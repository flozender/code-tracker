/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot;

import org.springframework.context.ApplicationEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Event published as early when a {@link SpringApplication} is starting up and the
 * {@link Environment} is first available for inspection and modification.
 * 
 * @author Dave Syer
 */
public class SpringApplicationNewEnvironmentEvent extends ApplicationEvent {

	private ConfigurableEnvironment environment;
	private String[] args;

	/**
	 * @param springApplication the current application
	 * @param environment the environment that was just created
	 * @param args the argumemts the application is running with
	 */
	public SpringApplicationNewEnvironmentEvent(SpringApplication springApplication,
			ConfigurableEnvironment environment, String[] args) {
		super(springApplication);
		this.environment = environment;
		this.args = args;
	}

	/**
	 * @return the springApplication
	 */
	public SpringApplication getSpringApplication() {
		return (SpringApplication) getSource();
	}

	/**
	 * @return the args
	 */
	public String[] getArgs() {
		return this.args;
	}

	/**
	 * @return the environment
	 */
	public ConfigurableEnvironment getEnvironment() {
		return this.environment;
	}

}
