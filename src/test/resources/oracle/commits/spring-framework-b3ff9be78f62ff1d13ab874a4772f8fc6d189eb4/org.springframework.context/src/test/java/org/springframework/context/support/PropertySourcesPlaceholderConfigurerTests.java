/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.context.support;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

import java.util.Properties;

import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.env.MockPropertySource;

import test.beans.TestBean;

/**
 * Unit tests for {@link PropertySourcesPlaceholderConfigurer}.
 * 
 * @author Chris Beams
 * @since 3.1
 */
public class PropertySourcesPlaceholderConfigurerTests {

	@Test
	public void replacementFromEnvironmentProperties() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		MockEnvironment env = new MockEnvironment();
		env.setProperty("my.name", "myValue");

		PropertySourcesPlaceholderConfigurer ppc =
			new PropertySourcesPlaceholderConfigurer();
		ppc.setEnvironment(env);
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName(), equalTo("myValue"));
	}

	@Test
	public void localPropertiesViaResource() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		PropertySourcesPlaceholderConfigurer pc = new PropertySourcesPlaceholderConfigurer();
		Resource resource = new ClassPathResource("PropertySourcesPlaceholderConfigurerTests.properties", this.getClass());
		pc.setLocation(resource);
		pc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName(), equalTo("foo"));
	}

	@Test
	public void localPropertiesOverrideFalse() {
		localPropertiesOverride(false);
	}

	@Test
	public void localPropertiesOverrideTrue() {
		localPropertiesOverride(true);
	}

	@Test
	public void explicitPropertySources() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addLast(new MockPropertySource().withProperty("my.name", "foo"));

		PropertySourcesPlaceholderConfigurer pc = new PropertySourcesPlaceholderConfigurer();
		pc.setPropertySources(propertySources);
		pc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName(), equalTo("foo"));
	}

	@Test
	public void explicitPropertySourcesExcludesEnvironment() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addLast(new MockPropertySource());

		PropertySourcesPlaceholderConfigurer pc = new PropertySourcesPlaceholderConfigurer();
		pc.setPropertySources(propertySources);
		pc.setEnvironment(new MockEnvironment().withProperty("my.name", "env"));
		pc.setIgnoreUnresolvablePlaceholders(true);
		pc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName(), equalTo("${my.name}"));
	}

	@Test
	@SuppressWarnings("serial")
	public void explicitPropertySourcesExcludesLocalProperties() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addLast(new MockPropertySource());

		PropertySourcesPlaceholderConfigurer pc = new PropertySourcesPlaceholderConfigurer();
		pc.setPropertySources(propertySources);
		pc.setProperties(new Properties() {{ put("my.name", "local"); }});
		pc.setIgnoreUnresolvablePlaceholders(true);
		pc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName(), equalTo("${my.name}"));
	}

	@SuppressWarnings("serial")
	private void localPropertiesOverride(boolean override) {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${foo}")
					.getBeanDefinition());

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();

		ppc.setLocalOverride(override);
		ppc.setProperties(new Properties() {{ setProperty("foo", "local"); }});
		ppc.setEnvironment(new MockEnvironment().withProperty("foo", "enclosing"));
		ppc.postProcessBeanFactory(bf);
		if (override) {
			assertThat(bf.getBean(TestBean.class).getName(), equalTo("local"));
		} else {
			assertThat(bf.getBean(TestBean.class).getName(), equalTo("enclosing"));
		}
	}

}
