/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded;

/**
 * Simple interface that represents a fully configured embedded servlet container (for
 * example Tomcat or Jetty). Allows the container to be {@link #stop() stopped}.
 * 
 * <p>
 * Instances of this class are usually obtained via a
 * {@link EmbeddedServletContainerFactory}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 * @see EmbeddedServletContainerFactory
 */
public interface EmbeddedServletContainer {

	/**
	 * An empty {@link EmbeddedServletContainer} that does nothing.
	 */
	public static final EmbeddedServletContainer NONE = new EmbeddedServletContainer() {

		@Override
		public void start() throws EmbeddedServletContainerException {
			// Do nothing
		};

		@Override
		public void stop() throws EmbeddedServletContainerException {
			// Do nothing
		}

		public int getPort() {
			return 0;
		}
	};

	/**
	 * Starts the embedded servlet container. Calling this method on an already started
	 * container has no effect.
	 * @throws EmbeddedServletContainerException of the container cannot be stopped
	 */
	void start() throws EmbeddedServletContainerException;

	/**
	 * Stops the embedded servlet container. Calling this method on an already stopped
	 * container has no effect.
	 * @throws EmbeddedServletContainerException of the container cannot be stopped
	 */
	void stop() throws EmbeddedServletContainerException;

	/**
	 * @return the port this server is listening on (or zero if none)
	 */
	int getPort();

}
