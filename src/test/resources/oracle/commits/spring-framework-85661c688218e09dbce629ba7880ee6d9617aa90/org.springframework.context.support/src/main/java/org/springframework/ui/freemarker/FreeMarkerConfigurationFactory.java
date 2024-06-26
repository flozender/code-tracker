/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ui.freemarker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.CollectionUtils;

/**
 * Factory that configures a FreeMarker Configuration. Can be used standalone, but
 * typically you will either use FreeMarkerConfigurationFactoryBean for preparing a
 * Configuration as bean reference, or FreeMarkerConfigurer for web views.
 *
 * <p>The optional "configLocation" property sets the location of a FreeMarker
 * properties file, within the current application. FreeMarker properties can be
 * overridden via "freemarkerSettings". All of these properties will be set by
 * calling FreeMarker's <code>Configuration.setSettings()</code> method and are
 * subject to constraints set by FreeMarker.
 *
 * <p>The "freemarkerVariables" property can be used to specify a Map of
 * shared variables that will be applied to the Configuration via the
 * <code>setAllSharedVariables()</code> method. Like <code>setSettings()</code>,
 * these entries are subject to FreeMarker constraints.
 *
 * <p>The simplest way to use this class is to specify a "templateLoaderPath";
 * FreeMarker does not need any further configuration then.
 *
 * <p>Note: Spring's FreeMarker support requires FreeMarker 2.3 or higher.
 *
 * @author Darren Davison
 * @author Juergen Hoeller
 * @since 03.03.2004
 * @see #setConfigLocation
 * @see #setFreemarkerSettings
 * @see #setFreemarkerVariables
 * @see #setTemplateLoaderPath
 * @see #createConfiguration
 * @see FreeMarkerConfigurationFactoryBean
 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer
 * @see freemarker.template.Configuration
 */
public class FreeMarkerConfigurationFactory {

	protected final Log logger = LogFactory.getLog(getClass());

	private Resource configLocation;

	private Properties freemarkerSettings;

	private Map<String, Object> freemarkerVariables;

	private String defaultEncoding;

	private final List<TemplateLoader> templateLoaders = new ArrayList<TemplateLoader>();

	private List<TemplateLoader> preTemplateLoaders;

	private List<TemplateLoader> postTemplateLoaders;

	private String[] templateLoaderPaths;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	private boolean preferFileSystemAccess = true;


	/**
	 * Set the location of the FreeMarker config file.
	 * Alternatively, you can specify all setting locally.
	 * @see #setFreemarkerSettings
	 * @see #setTemplateLoaderPath
	 */
	public void setConfigLocation(Resource resource) {
		configLocation = resource;
	}

	/**
	 * Set properties that contain well-known FreeMarker keys which will be
	 * passed to FreeMarker's <code>Configuration.setSettings</code> method.
	 * @see freemarker.template.Configuration#setSettings
	 */
	public void setFreemarkerSettings(Properties settings) {
		this.freemarkerSettings = settings;
	}

	/**
	 * Set a Map that contains well-known FreeMarker objects which will be passed
	 * to FreeMarker's <code>Configuration.setAllSharedVariables()</code> method.
	 * @see freemarker.template.Configuration#setAllSharedVariables
	 */
	public void setFreemarkerVariables(Map<String, Object> variables) {
		this.freemarkerVariables = variables;
	}

	/**
	 * Set the default encoding for the FreeMarker configuration.
	 * If not specified, FreeMarker will use the platform file encoding.
	 * <p>Used for template rendering unless there is an explicit encoding specified
	 * for the rendering process (for example, on Spring's FreeMarkerView).
	 * @see freemarker.template.Configuration#setDefaultEncoding
	 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerView#setEncoding
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * Set a List of <code>TemplateLoader<code>s that will be used to search
	 * for templates. For example, one or more custom loaders such as database
	 * loaders could be configured and injected here.
	 * @deprecated as of Spring 2.0.1, in favor of the "preTemplateLoaders"
	 * and "postTemplateLoaders" properties
	 * @see #setPreTemplateLoaders
	 * @see #setPostTemplateLoaders
	 */
	@Deprecated
	public void setTemplateLoaders(TemplateLoader[] templateLoaders) {
		if (templateLoaders != null) {
			this.templateLoaders.addAll(Arrays.asList(templateLoaders));
		}
	}

	/**
	 * Set a List of <code>TemplateLoader<code>s that will be used to search
	 * for templates. For example, one or more custom loaders such as database
	 * loaders could be configured and injected here.
	 * <p>The {@link TemplateLoader TemplateLoaders} specified here will be
	 * registered <i>before</i> the default template loaders that this factory
	 * registers (such as loaders for specified "templateLoaderPaths" or any
	 * loaders registered in {@link #postProcessTemplateLoaders}).
	 * @see #setTemplateLoaderPaths
	 * @see #postProcessTemplateLoaders
	 */
	public void setPreTemplateLoaders(TemplateLoader[] preTemplateLoaders) {
		this.preTemplateLoaders = Arrays.asList(preTemplateLoaders);
	}

	/**
	 * Set a List of <code>TemplateLoader<code>s that will be used to search
	 * for templates. For example, one or more custom loaders such as database
	 * loaders can be configured.
	 * <p>The {@link TemplateLoader TemplateLoaders} specified here will be
	 * registered <i>after</i> the default template loaders that this factory
	 * registers (such as loaders for specified "templateLoaderPaths" or any
	 * loaders registered in {@link #postProcessTemplateLoaders}).
	 * @see #setTemplateLoaderPaths
	 * @see #postProcessTemplateLoaders
	 */
	public void setPostTemplateLoaders(TemplateLoader[] postTemplateLoaders) {
		this.postTemplateLoaders = Arrays.asList(postTemplateLoaders);
	}

	/**
	 * Set the Freemarker template loader path via a Spring resource location.
	 * See the "templateLoaderPaths" property for details on path handling.
	 * @see #setTemplateLoaderPaths
	 */
	public void setTemplateLoaderPath(String templateLoaderPath) {
		this.templateLoaderPaths = new String[] {templateLoaderPath};
	}

	/**
	 * Set multiple Freemarker template loader paths via Spring resource locations.
	 * <p>When populated via a String, standard URLs like "file:" and "classpath:"
	 * pseudo URLs are supported, as understood by ResourceEditor. Allows for
	 * relative paths when running in an ApplicationContext.
	 * <p>Will define a path for the default FreeMarker template loader.
	 * If a specified resource cannot be resolved to a <code>java.io.File</code>,
	 * a generic SpringTemplateLoader will be used, without modification detection.
	 * <p>To enforce the use of SpringTemplateLoader, i.e. to not resolve a path
	 * as file system resource in any case, turn off the "preferFileSystemAccess"
	 * flag. See the latter's javadoc for details.
	 * <p>If you wish to specify your own list of TemplateLoaders, do not set this
	 * property and instead use <code>setTemplateLoaders(List templateLoaders)</code>
	 * @see org.springframework.core.io.ResourceEditor
	 * @see org.springframework.context.ApplicationContext#getResource
	 * @see freemarker.template.Configuration#setDirectoryForTemplateLoading
	 * @see SpringTemplateLoader
	 * @see #setTemplateLoaders
	 */
	public void setTemplateLoaderPaths(String[] templateLoaderPaths) {
		this.templateLoaderPaths = templateLoaderPaths;
	}

	/**
	 * Set the Spring ResourceLoader to use for loading FreeMarker template files.
	 * The default is DefaultResourceLoader. Will get overridden by the
	 * ApplicationContext if running in a context.
	 * @see org.springframework.core.io.DefaultResourceLoader
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Return the Spring ResourceLoader to use for loading FreeMarker template files.
	 */
	protected ResourceLoader getResourceLoader() {
		return resourceLoader;
	}

	/**
	 * Set whether to prefer file system access for template loading.
	 * File system access enables hot detection of template changes.
	 * <p>If this is enabled, FreeMarkerConfigurationFactory will try to resolve
	 * the specified "templateLoaderPath" as file system resource (which will work
	 * for expanded class path resources and ServletContext resources too).
	 * <p>Default is "true". Turn this off to always load via SpringTemplateLoader
	 * (i.e. as stream, without hot detection of template changes), which might
	 * be necessary if some of your templates reside in an expanded classes
	 * directory while others reside in jar files.
	 * @see #setTemplateLoaderPath
	 */
	public void setPreferFileSystemAccess(boolean preferFileSystemAccess) {
		this.preferFileSystemAccess = preferFileSystemAccess;
	}

	/**
	 * Return whether to prefer file system access for template loading.
	 */
	protected boolean isPreferFileSystemAccess() {
		return preferFileSystemAccess;
	}


	/**
	 * Prepare the FreeMarker Configuration and return it.
	 * @return the FreeMarker Configuration object
	 * @throws IOException if the config file wasn't found
	 * @throws TemplateException on FreeMarker initialization failure
	 */
	public Configuration createConfiguration() throws IOException, TemplateException {
		Configuration config = newConfiguration();
		Properties props = new Properties();

		// Load config file if specified.
		if (this.configLocation != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Loading FreeMarker configuration from " + this.configLocation);
			}
			PropertiesLoaderUtils.fillProperties(props, this.configLocation);
		}

		// Merge local properties if specified.
		if (this.freemarkerSettings != null) {
			props.putAll(this.freemarkerSettings);
		}

		// FreeMarker will only accept known keys in its setSettings and
		// setAllSharedVariables methods.
		if (!props.isEmpty()) {
			config.setSettings(props);
		}

		if (!CollectionUtils.isEmpty(this.freemarkerVariables)) {
			config.setAllSharedVariables(new SimpleHash(this.freemarkerVariables));
		}

		if (this.defaultEncoding != null) {
			config.setDefaultEncoding(this.defaultEncoding);
		}

		// Register template loaders that are supposed to kick in early.
		if (this.preTemplateLoaders != null) {
			this.templateLoaders.addAll(this.preTemplateLoaders);
		}

		// Register default template loaders.
		if (this.templateLoaderPaths != null) {
			for (String path : this.templateLoaderPaths) {
				this.templateLoaders.add(getTemplateLoaderForPath(path));
			}
		}
		postProcessTemplateLoaders(this.templateLoaders);

		// Register template loaders that are supposed to kick in late.
		if (this.postTemplateLoaders != null) {
			this.templateLoaders.addAll(this.postTemplateLoaders);
		}

		TemplateLoader loader = getAggregateTemplateLoader(this.templateLoaders);
		if (loader != null) {
			config.setTemplateLoader(loader);
		}

		postProcessConfiguration(config);
		return config;
	}

	/**
	 * Return a new Configuration object. Subclasses can override this for
	 * custom initialization, or for using a mock object for testing.
	 * <p>Called by <code>createConfiguration()</code>.
	 * @return the Configuration object
	 * @throws IOException if a config file wasn't found
	 * @throws TemplateException on FreeMarker initialization failure
	 * @see #createConfiguration()
	 */
	protected Configuration newConfiguration() throws IOException, TemplateException {
		return new Configuration();
	}

	/**
	 * Determine a FreeMarker TemplateLoader for the given path.
	 * <p>Default implementation creates either a FileTemplateLoader or
	 * a SpringTemplateLoader.
	 * @param templateLoaderPath the path to load templates from
	 * @return an appropriate TemplateLoader
	 * @see freemarker.cache.FileTemplateLoader
	 * @see SpringTemplateLoader
	 */
	protected TemplateLoader getTemplateLoaderForPath(String templateLoaderPath) {
		if (isPreferFileSystemAccess()) {
			// Try to load via the file system, fall back to SpringTemplateLoader
			// (for hot detection of template changes, if possible).
			try {
				Resource path = getResourceLoader().getResource(templateLoaderPath);
				File file = path.getFile();  // will fail if not resolvable in the file system
				if (logger.isDebugEnabled()) {
					logger.debug(
							"Template loader path [" + path + "] resolved to file path [" + file.getAbsolutePath() + "]");
				}
				return new FileTemplateLoader(file);
			}
			catch (IOException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot resolve template loader path [" + templateLoaderPath +
							"] to [java.io.File]: using SpringTemplateLoader as fallback", ex);
				}
				return new SpringTemplateLoader(getResourceLoader(), templateLoaderPath);
			}
		}
		else {
			// Always load via SpringTemplateLoader (without hot detection of template changes).
			logger.debug("File system access not preferred: using SpringTemplateLoader");
			return new SpringTemplateLoader(getResourceLoader(), templateLoaderPath);
		}
	}

	/**
	 * To be overridden by subclasses that want to to register custom
	 * TemplateLoader instances after this factory created its default
	 * template loaders.
	 * <p>Called by <code>createConfiguration()</code>. Note that specified
	 * "postTemplateLoaders" will be registered <i>after</i> any loaders
	 * registered by this callback; as a consequence, they are are <i>not</i>
	 * included in the given List.
	 * @param templateLoaders the current List of TemplateLoader instances,
	 * to be modified by a subclass
	 * @see #createConfiguration()
	 * @see #setPostTemplateLoaders
	 */
	protected void postProcessTemplateLoaders(List templateLoaders) {
	}

	/**
	 * Return a TemplateLoader based on the given TemplateLoader list.
	 * If more than one TemplateLoader has been registered, a FreeMarker
	 * MultiTemplateLoader needs to be created.
	 * @param templateLoaders the final List of TemplateLoader instances
	 * @return the aggregate TemplateLoader
	 */
	protected TemplateLoader getAggregateTemplateLoader(List<TemplateLoader> templateLoaders) {
		int loaderCount = templateLoaders.size();
		switch (loaderCount) {
			case 0:
				logger.info("No FreeMarker TemplateLoaders specified");
				return null;
			case 1:
				return templateLoaders.get(0);
			default:
				TemplateLoader[] loaders = templateLoaders.toArray(new TemplateLoader[loaderCount]);
				return new MultiTemplateLoader(loaders);
		}
	}

	/**
	 * To be overridden by subclasses that want to to perform custom
	 * post-processing of the Configuration object after this factory
	 * performed its default initialization.
	 * <p>Called by <code>createConfiguration()</code>.
	 * @param config the current Configuration object
	 * @throws IOException if a config file wasn't found
	 * @throws TemplateException on FreeMarker initialization failure
	 * @see #createConfiguration()
	 */
	protected void postProcessConfiguration(Configuration config) throws IOException, TemplateException {
	}

}
