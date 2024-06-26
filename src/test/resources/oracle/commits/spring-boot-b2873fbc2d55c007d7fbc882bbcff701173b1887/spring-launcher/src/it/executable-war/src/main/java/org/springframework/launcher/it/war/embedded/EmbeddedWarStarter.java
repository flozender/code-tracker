/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.launcher.it.war.embedded;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.launcher.it.war.SpringInitializer;

/**
 * Starter to launch the embedded server. NOTE: Jetty annotation scanning is not
 * compatible with executable WARs so we must specify the {@link SpringInitializer}.
 *
 * @author Phillip Webb
 */
public final class EmbeddedWarStarter {

	public static void main(String[] args) throws Exception {
		Server server = new Server(8080);

		WebAppContext webAppContext = new WebAppContext();
		webAppContext.setContextPath("/");
		webAppContext.setConfigurations(new Configuration[] {
				new WebApplicationInitializersConfiguration(SpringInitializer.class) });

		webAppContext.setParentLoaderPriority(true);
		server.setHandler(webAppContext);
		server.start();

		server.join();
	}
}
