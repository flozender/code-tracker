/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.plugins;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.node.info.PluginsInfo;
import org.elasticsearch.bootstrap.Bootstrap;
import org.elasticsearch.bootstrap.JarHell;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.io.FileSystemUtils;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.elasticsearch.common.io.FileSystemUtils.isAccessibleDirectory;

/**
 *
 */
public class PluginsService extends AbstractComponent {

    /**
     * We keep around a list of plugins
     */
    private final ImmutableList<Tuple<PluginInfo, Plugin>> plugins;
    private final PluginsInfo info;

    private final ImmutableMap<Plugin, List<OnModuleReference>> onModuleReferences;

    static class OnModuleReference {
        public final Class<? extends Module> moduleClass;
        public final Method onModuleMethod;

        OnModuleReference(Class<? extends Module> moduleClass, Method onModuleMethod) {
            this.moduleClass = moduleClass;
            this.onModuleMethod = onModuleMethod;
        }
    }

    /**
     * Constructs a new PluginService
     * @param settings The settings of the system
     * @param environment The environment of the system
     */
    public PluginsService(Settings settings, Environment environment) {
        super(settings);

        ImmutableList.Builder<Tuple<PluginInfo, Plugin>> tupleBuilder = ImmutableList.builder();

        // first we load specified plugins via 'plugin.types' settings parameter.
        // this is a hack for what is between unit and integration tests...
        String[] defaultPluginsClasses = settings.getAsArray("plugin.types");
        for (String pluginClass : defaultPluginsClasses) {
            Plugin plugin = loadPlugin(pluginClass, settings, getClass().getClassLoader());
            PluginInfo pluginInfo = new PluginInfo(plugin.name(), plugin.description(), false, "NA", true, pluginClass, false);
            if (logger.isTraceEnabled()) {
                logger.trace("plugin loaded from settings [{}]", pluginInfo);
            }
            tupleBuilder.add(new Tuple<>(pluginInfo, plugin));
        }

        // now, find all the ones that are in plugins/
        try {
          List<Bundle> bundles = getPluginBundles(environment);
          tupleBuilder.addAll(loadBundles(bundles));
        } catch (IOException ex) {
          throw new IllegalStateException("Unable to initialize plugins", ex);
        }

        plugins = tupleBuilder.build();
        info = new PluginsInfo();
        for (Tuple<PluginInfo, Plugin> tuple : plugins) {
            info.add(tuple.v1());
        }

        // We need to build a List of jvm and site plugins for checking mandatory plugins
        Map<String, Plugin> jvmPlugins = new HashMap<>();
        List<String> sitePlugins = new ArrayList<>();

        for (Tuple<PluginInfo, Plugin> tuple : plugins) {
            PluginInfo info = tuple.v1();
            if (info.isJvm()) {
                jvmPlugins.put(tuple.v2().name(), tuple.v2());
            }
            if (info.isSite()) {
                sitePlugins.add(info.getName());
            }
        }

        // Checking expected plugins
        String[] mandatoryPlugins = settings.getAsArray("plugin.mandatory", null);
        if (mandatoryPlugins != null) {
            Set<String> missingPlugins = new HashSet<>();
            for (String mandatoryPlugin : mandatoryPlugins) {
                if (!jvmPlugins.containsKey(mandatoryPlugin) && !sitePlugins.contains(mandatoryPlugin) && !missingPlugins.contains(mandatoryPlugin)) {
                    missingPlugins.add(mandatoryPlugin);
                }
            }
            if (!missingPlugins.isEmpty()) {
                throw new ElasticsearchException("Missing mandatory plugins [" + Strings.collectionToDelimitedString(missingPlugins, ", ") + "]");
            }
        }

        logger.info("loaded {}, sites {}", jvmPlugins.keySet(), sitePlugins);

        MapBuilder<Plugin, List<OnModuleReference>> onModuleReferences = MapBuilder.newMapBuilder();
        for (Plugin plugin : jvmPlugins.values()) {
            List<OnModuleReference> list = new ArrayList<>();
            for (Method method : plugin.getClass().getMethods()) {
                if (!method.getName().equals("onModule")) {
                    continue;
                }
                if (method.getParameterTypes().length == 0 || method.getParameterTypes().length > 1) {
                    logger.warn("Plugin: {} implementing onModule with no parameters or more than one parameter", plugin.name());
                    continue;
                }
                Class moduleClass = method.getParameterTypes()[0];
                if (!Module.class.isAssignableFrom(moduleClass)) {
                    logger.warn("Plugin: {} implementing onModule by the type is not of Module type {}", plugin.name(), moduleClass);
                    continue;
                }
                list.add(new OnModuleReference(moduleClass, method));
            }
            if (!list.isEmpty()) {
                onModuleReferences.put(plugin, list);
            }
        }
        this.onModuleReferences = onModuleReferences.immutableMap();
    }

    public ImmutableList<Tuple<PluginInfo, Plugin>> plugins() {
        return plugins;
    }

    public void processModules(Iterable<Module> modules) {
        for (Module module : modules) {
            processModule(module);
        }
    }

    public void processModule(Module module) {
        for (Tuple<PluginInfo, Plugin> plugin : plugins()) {
            // see if there are onModule references
            List<OnModuleReference> references = onModuleReferences.get(plugin.v2());
            if (references != null) {
                for (OnModuleReference reference : references) {
                    if (reference.moduleClass.isAssignableFrom(module.getClass())) {
                        try {
                            reference.onModuleMethod.invoke(plugin.v2(), module);
                        } catch (Exception e) {
                            logger.warn("plugin {}, failed to invoke custom onModule method", e, plugin.v2().name());
                        }
                    }
                }
            }
        }
    }

    public Settings updatedSettings() {
        Map<String, String> foundSettings = new HashMap<>();
        final Settings.Builder builder = Settings.settingsBuilder();
        for (Tuple<PluginInfo, Plugin> plugin : plugins) {
            Settings settings = plugin.v2().additionalSettings();
            for (String setting : settings.getAsMap().keySet()) {
                String oldPlugin = foundSettings.put(setting, plugin.v1().getName());
                if (oldPlugin != null) {
                    throw new IllegalArgumentException("Cannot have additional setting [" + setting + "] " +
                        "in plugin [" + plugin.v1().getName() + "], already added in plugin [" + oldPlugin + "]");
                }
            }
            builder.put(settings);
        }
        return builder.put(this.settings).build();
    }

    public Collection<Module> nodeModules() {
        List<Module> modules = new ArrayList<>();
        for (Tuple<PluginInfo, Plugin> plugin : plugins) {
            modules.addAll(plugin.v2().nodeModules());
        }
        return modules;
    }

    public Collection<Class<? extends LifecycleComponent>> nodeServices() {
        List<Class<? extends LifecycleComponent>> services = new ArrayList<>();
        for (Tuple<PluginInfo, Plugin> plugin : plugins) {
            services.addAll(plugin.v2().nodeServices());
        }
        return services;
    }

    public Collection<Module> indexModules(Settings indexSettings) {
        List<Module> modules = new ArrayList<>();
        for (Tuple<PluginInfo, Plugin> plugin : plugins) {
            modules.addAll(plugin.v2().indexModules(indexSettings));
        }
        return modules;
    }

    public Collection<Class<? extends Closeable>> indexServices() {
        List<Class<? extends Closeable>> services = new ArrayList<>();
        for (Tuple<PluginInfo, Plugin> plugin : plugins) {
            services.addAll(plugin.v2().indexServices());
        }
        return services;
    }

    public Collection<Module> shardModules(Settings indexSettings) {
        List<Module> modules = new ArrayList<>();
        for (Tuple<PluginInfo, Plugin> plugin : plugins) {
            modules.addAll(plugin.v2().shardModules(indexSettings));
        }
        return modules;
    }

    public Collection<Class<? extends Closeable>> shardServices() {
        List<Class<? extends Closeable>> services = new ArrayList<>();
        for (Tuple<PluginInfo, Plugin> plugin : plugins) {
            services.addAll(plugin.v2().shardServices());
        }
        return services;
    }

    /**
     * Get information about plugins (jvm and site plugins).
     */
    public PluginsInfo info() {
        return info;
    }
    
    // a "bundle" is a group of plugins in a single classloader
    // really should be 1-1, but we are not so fortunate
    static class Bundle {
        List<PluginInfo> plugins = new ArrayList<>();
        List<URL> urls = new ArrayList<>();
    }

    static List<Bundle> getPluginBundles(Environment environment) throws IOException {
        ESLogger logger = Loggers.getLogger(PluginsService.class);

        Path pluginsDirectory = environment.pluginsFile();
        // TODO: remove this leniency, but tests bogusly rely on it
        if (!isAccessibleDirectory(pluginsDirectory, logger)) {
            return Collections.emptyList();
        }
        
        List<Bundle> bundles = new ArrayList<>();
        // a special purgatory for plugins that directly depend on each other
        bundles.add(new Bundle());

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDirectory)) {
            for (Path plugin : stream) {
                if (FileSystemUtils.isHidden(plugin)) {
                    logger.trace("--- skip hidden plugin file[{}]", plugin.toAbsolutePath());
                    continue;
                }
                logger.trace("--- adding plugin [{}]", plugin.toAbsolutePath());
                PluginInfo info = PluginInfo.readFromProperties(plugin);
                List<URL> urls = new ArrayList<>();
                if (info.isJvm()) {
                    // a jvm plugin: gather urls for jar files
                    try (DirectoryStream<Path> jarStream = Files.newDirectoryStream(plugin, "*.jar")) {
                        for (Path jar : jarStream) {
                            urls.add(jar.toUri().toURL());
                        }
                    }
                }
                final Bundle bundle;
                if (info.isJvm() && info.isIsolated() == false) {
                    bundle = bundles.get(0); // purgatory
                } else {
                    bundle = new Bundle();
                    bundles.add(bundle);
                }
                bundle.plugins.add(info);
                bundle.urls.addAll(urls);
            }
        }
        
        return bundles;
    }

    private List<Tuple<PluginInfo,Plugin>> loadBundles(List<Bundle> bundles) {
        ImmutableList.Builder<Tuple<PluginInfo, Plugin>> plugins = ImmutableList.builder();

        for (Bundle bundle : bundles) {
            // jar-hell check the bundle against the parent classloader
            // pluginmanager does it, but we do it again, in case lusers mess with jar files manually
            try {
                final List<URL> jars = new ArrayList<>();
                ClassLoader parentLoader = getClass().getClassLoader();
                if (parentLoader instanceof URLClassLoader) {
                    for (URL url : ((URLClassLoader) parentLoader).getURLs()) {
                        jars.add(url);
                    }
                }
                jars.addAll(bundle.urls);
                JarHell.checkJarHell(jars.toArray(new URL[0]));
            } catch (Exception e) {
                throw new IllegalStateException("failed to load bundle " + bundle.urls + " due to jar hell", e);
            }
            
            // create a child to load the plugins in this bundle
            ClassLoader loader = URLClassLoader.newInstance(bundle.urls.toArray(new URL[0]), getClass().getClassLoader());
            for (PluginInfo pluginInfo : bundle.plugins) {
                final Plugin plugin;
                if (pluginInfo.isJvm()) {
                    plugin = loadPlugin(pluginInfo.getClassname(), settings, loader);
                } else {
                    plugin = new SitePlugin(pluginInfo.getName(), pluginInfo.getDescription());
                }
                plugins.add(new Tuple<>(pluginInfo, plugin));
            }
        }

        return plugins.build();
    }

    private Plugin loadPlugin(String className, Settings settings, ClassLoader loader) {
        try {
            Class<? extends Plugin> pluginClass = loader.loadClass(className).asSubclass(Plugin.class);

            try {
                return pluginClass.getConstructor(Settings.class).newInstance(settings);
            } catch (NoSuchMethodException e) {
                try {
                    return pluginClass.getConstructor().newInstance();
                } catch (NoSuchMethodException e1) {
                    throw new ElasticsearchException("No constructor for [" + pluginClass + "]. A plugin class must " +
                            "have either an empty default constructor or a single argument constructor accepting a " +
                            "Settings instance");
                }
            }

        } catch (Throwable e) {
            throw new ElasticsearchException("Failed to load plugin class [" + className + "]", e);
        }
    }
}
