/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context.embedded;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.web.servlet.ServletContextInitializer;

/**
 * Simple interface that represents customizations to an
 * {@link EmbeddedServletContainerFactory}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Brian Clozel
 * @see EmbeddedServletContainerFactory
 * @see EmbeddedServletContainerCustomizer
 */
public interface ConfigurableEmbeddedServletContainer
		extends ConfigurableEmbeddedWebServer {

	/**
	 * Sets the context path for the embedded servlet container. The context should start
	 * with a "/" character but not end with a "/" character. The default context path can
	 * be specified using an empty string.
	 * @param contextPath the contextPath to set
	 */
	void setContextPath(String contextPath);

	/**
	 * Sets the display name of the application deployed in the embedded servlet
	 * container.
	 * @param displayName the displayName to set
	 * @since 1.3.0
	 */
	void setDisplayName(String displayName);

	/**
	 * The session timeout in seconds (default 30 minutes). If 0 or negative then sessions
	 * never expire.
	 * @param sessionTimeout the session timeout
	 */
	void setSessionTimeout(int sessionTimeout);

	/**
	 * The session timeout in the specified {@link TimeUnit} (default 30 minutes). If 0 or
	 * negative then sessions never expire.
	 * @param sessionTimeout the session timeout
	 * @param timeUnit the time unit
	 */
	void setSessionTimeout(int sessionTimeout, TimeUnit timeUnit);

	/**
	 * Sets if session data should be persisted between restarts.
	 * @param persistSession {@code true} if session data should be persisted
	 */
	void setPersistSession(boolean persistSession);

	/**
	 * Set the directory used to store serialized session data.
	 * @param sessionStoreDir the directory or {@code null} to use a default location.
	 */
	void setSessionStoreDir(File sessionStoreDir);

	/**
	 * Set if the DefaultServlet should be registered. Defaults to {@code true} so that
	 * files from the {@link #setDocumentRoot(File) document root} will be served.
	 * @param registerDefaultServlet if the default servlet should be registered
	 */
	void setRegisterDefaultServlet(boolean registerDefaultServlet);

	/**
	 * Sets the mime-type mappings.
	 * @param mimeMappings the mime type mappings (defaults to
	 * {@link MimeMappings#DEFAULT})
	 */
	void setMimeMappings(MimeMappings mimeMappings);

	/**
	 * Sets the document root directory which will be used by the web context to serve
	 * static files.
	 * @param documentRoot the document root or {@code null} if not required
	 */
	void setDocumentRoot(File documentRoot);

	/**
	 * Sets {@link ServletContextInitializer} that should be applied in addition to
	 * {@link EmbeddedServletContainerFactory#getEmbeddedServletContainer(ServletContextInitializer...)}
	 * parameters. This method will replace any previously set or added initializers.
	 * @param initializers the initializers to set
	 * @see #addInitializers
	 */
	void setInitializers(List<? extends ServletContextInitializer> initializers);

	/**
	 * Add {@link ServletContextInitializer}s to those that should be applied in addition
	 * to
	 * {@link EmbeddedServletContainerFactory#getEmbeddedServletContainer(ServletContextInitializer...)}
	 * parameters.
	 * @param initializers the initializers to add
	 * @see #setInitializers
	 */
	void addInitializers(ServletContextInitializer... initializers);

	/**
	 * Sets the configuration that will be applied to the container's JSP servlet.
	 * @param jsp the JSP servlet configuration
	 */
	void setJsp(Jsp jsp);

	/**
	 * Sets the Locale to Charset mappings.
	 * @param localeCharsetMappings the Locale to Charset mappings
	 */
	void setLocaleCharsetMappings(Map<Locale, Charset> localeCharsetMappings);

}
