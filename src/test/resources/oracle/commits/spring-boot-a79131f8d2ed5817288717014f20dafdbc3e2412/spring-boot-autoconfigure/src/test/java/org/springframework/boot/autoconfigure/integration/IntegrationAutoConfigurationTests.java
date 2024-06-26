/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.integration;

import org.junit.Test;

import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.integration.support.channel.HeaderChannelRegistry;

import static org.junit.Assert.assertNotNull;

/**
 * @author Artem Bilan
 * @since 1.1
 */
public class IntegrationAutoConfigurationTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void integrationIsAvailable() {
		this.context.register(IntegrationAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(HeaderChannelRegistry.class));
		this.context.close();
	}

	@Test
	public void addJmxAuto() {
		this.context.register(JmxAutoConfiguration.class,
				IntegrationAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(HeaderChannelRegistry.class));
		this.context.close();
	}

	@Test
	public void parentContext() {
		this.context.register(IntegrationAutoConfiguration.class);
		this.context.refresh();
		AnnotationConfigApplicationContext parent = this.context;
		this.context = new AnnotationConfigApplicationContext();
		this.context.setParent(parent);
		this.context.register(IntegrationAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(HeaderChannelRegistry.class));
		((ConfigurableApplicationContext) this.context.getParent()).close();
		this.context.close();
	}

}
