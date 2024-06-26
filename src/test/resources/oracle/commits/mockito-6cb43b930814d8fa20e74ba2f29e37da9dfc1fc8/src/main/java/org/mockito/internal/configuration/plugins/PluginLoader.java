/*
 * Copyright (c) 2016 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.configuration.plugins;

import org.mockito.Mockito;
import org.mockito.internal.util.collections.Iterables;
import org.mockito.plugins.MockitoPlugins;
import org.mockito.plugins.PluginSwitch;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

class PluginLoader {

    private final MockitoPlugins plugins = Mockito.framework().getPlugins();

    private final PluginSwitch pluginSwitch;

    private final Map<String, String> alias;

    public PluginLoader(PluginSwitch pluginSwitch) {
        this.pluginSwitch = pluginSwitch;
        this.alias = new HashMap<String, String>();
    }

    /**
     * Adds an alias for a class name to this plugin loader. Instead of the fully qualified type name,
     * the alias can be used as a convenience name for a known plugin.
     */
    PluginLoader withAlias(String name, String type) {
        alias.put(name, type);
        return this;
    }

    /**
     * Scans the classpath for given pluginType. If not found, default class is used.
     */
    @SuppressWarnings("unchecked")
    <T> T loadPlugin(final Class<T> pluginType) {
        try {
            T plugin = loadImpl(pluginType);
            if (plugin != null) {
                return plugin;
            }

            return plugins.getDefaultPlugin(pluginType);
        } catch (final Throwable t) {
            return (T) Proxy.newProxyInstance(pluginType.getClassLoader(),
                    new Class<?>[]{pluginType},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            throw new IllegalStateException("Could not initialize plugin: " + pluginType, t);
                        }
                    });
        }
    }

    /**
     * Equivalent to {@link java.util.ServiceLoader#load} but without requiring
     * Java 6 / Android 2.3 (Gingerbread).
     */
    private <T> T loadImpl(Class<T> service) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        Enumeration<URL> resources;
        try {
            resources = loader.getResources("mockito-extensions/" + service.getName());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + service, e);
        }

        try {
            String foundPluginClass = new PluginFinder(pluginSwitch).findPluginClass(Iterables.toIterable(resources));
            if (foundPluginClass != null) {
                String aliasType = alias.get(foundPluginClass);
                if (aliasType != null) {
                    foundPluginClass = aliasType;
                }
                Class<?> pluginClass = loader.loadClass(foundPluginClass);
                Object plugin = pluginClass.newInstance();
                return service.cast(plugin);
            }
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to load " + service + " implementation declared in " + resources, e);
        }
    }
}
