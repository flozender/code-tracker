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

package org.springframework.beans.factory.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.springframework.beans.factory.support.BeanDefinitionReaderUtils.registerWithGeneratedName;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import test.beans.TestBean;


/**
 * Unit tests for {@link EnvironmentAwarePropertyPlaceholderConfigurer}.
 *
 * @see PropertyResourceConfigurerTests
 * @author Chris Beams
 */
public class PropertyPlaceholderConfigurerTests {
	private static final String P1 = "p1";
	private static final String P1_LOCAL_PROPS_VAL = "p1LocalPropsVal";
	private static final String P1_SYSTEM_PROPS_VAL = "p1SystemPropsVal";
	private static final String P1_SYSTEM_ENV_VAL = "p1SystemEnvVal";

	private DefaultListableBeanFactory bf;
	private AbstractPropertyPlaceholderConfigurer ppc;
	private Properties ppcProperties;

	private AbstractBeanDefinition p1BeanDef;

	@Before
	public void setUp() {
		p1BeanDef = rootBeanDefinition(TestBean.class)
			.addPropertyValue("name", "${"+P1+"}")
			.getBeanDefinition();

		bf = new DefaultListableBeanFactory();

		ppcProperties = new Properties();
		ppcProperties.setProperty(P1, P1_LOCAL_PROPS_VAL);
		System.setProperty(P1, P1_SYSTEM_PROPS_VAL);
		getModifiableSystemEnvironment().put(P1, P1_SYSTEM_ENV_VAL);
		ppc = new PropertyPlaceholderConfigurer();
		ppc.setProperties(ppcProperties);

	}

	@After
	public void tearDown() {
		System.clearProperty(P1);
		getModifiableSystemEnvironment().remove(P1);
	}


	// -------------------------------------------------------------------------
	// Tests to ensure backward-compatibility for Environment refactoring
	// -------------------------------------------------------------------------

	@Test
	public void resolveFromSystemProperties() {
		getModifiableSystemEnvironment().put("otherKey", "systemValue");
		p1BeanDef = rootBeanDefinition(TestBean.class)
			.addPropertyValue("name", "${"+P1+"}")
			.addPropertyValue("sex", "${otherKey}")
			.getBeanDefinition();
		registerWithGeneratedName(p1BeanDef, bf);
		ppc.postProcessBeanFactory(bf);
		TestBean bean = bf.getBean(TestBean.class);
		assertThat(bean.getName(), equalTo(P1_LOCAL_PROPS_VAL));
		assertThat(bean.getSex(), equalTo("systemValue"));
		getModifiableSystemEnvironment().remove("otherKey");
	}

	@Test
	public void resolveFromLocalProperties() {
		tearDown(); // eliminate entries from system props/environment
		registerWithGeneratedName(p1BeanDef, bf);
		ppc.postProcessBeanFactory(bf);
		TestBean bean = bf.getBean(TestBean.class);
		assertThat(bean.getName(), equalTo(P1_LOCAL_PROPS_VAL));
	}

	@Test
	public void setSystemPropertiesMode_defaultIsFallback() {
		registerWithGeneratedName(p1BeanDef, bf);
		ppc.postProcessBeanFactory(bf);
		TestBean bean = bf.getBean(TestBean.class);
		assertThat(bean.getName(), equalTo(P1_LOCAL_PROPS_VAL));
	}

	/*
	@Test
	public void setSystemSystemPropertiesMode_toOverride_andResolveFromSystemProperties() {
		registerWithGeneratedName(p1BeanDef, bf);
		ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
		ppc.postProcessBeanFactory(bf);
		TestBean bean = bf.getBean(TestBean.class);
		assertThat(bean.getName(), equalTo(P1_SYSTEM_PROPS_VAL));
	}

	@Test
	public void setSystemSystemPropertiesMode_toOverride_andResolveFromSystemEnvironment() {
		registerWithGeneratedName(p1BeanDef, bf);
		System.clearProperty(P1); // will now fall all the way back to system environment
		ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
		ppc.postProcessBeanFactory(bf);
		TestBean bean = bf.getBean(TestBean.class);
		assertThat(bean.getName(), equalTo(P1_SYSTEM_ENV_VAL));
	}

	@Test
	public void setSystemSystemPropertiesMode_toOverride_andSetSearchSystemEnvironment_toFalse() {
		registerWithGeneratedName(p1BeanDef, bf);
		System.clearProperty(P1); // will now fall all the way back to system environment
		ppc.setSearchSystemEnvironment(false);
		ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
		ppc.postProcessBeanFactory(bf);
		TestBean bean = bf.getBean(TestBean.class);
		assertThat(bean.getName(), equalTo(P1_LOCAL_PROPS_VAL)); // has to resort to local props
	}
	*/

	/**
	 * Creates a scenario in which two PPCs are configured, each with different
	 * settings regarding resolving properties from the environment.
	 */
	@Test
	public void twoPlacholderConfigurers_withConflictingSettings() {
		String P2 = "p2";
		String P2_LOCAL_PROPS_VAL = "p2LocalPropsVal";
		String P2_SYSTEM_PROPS_VAL = "p2SystemPropsVal";
		String P2_SYSTEM_ENV_VAL = "p2SystemEnvVal";

		AbstractBeanDefinition p2BeanDef = rootBeanDefinition(TestBean.class)
			.addPropertyValue("name", "${"+P1+"}")
			.addPropertyValue("country", "${"+P2+"}")
			.getBeanDefinition();

		bf.registerBeanDefinition("p1Bean", p1BeanDef);
		bf.registerBeanDefinition("p2Bean", p2BeanDef);

		ppc.setIgnoreUnresolvablePlaceholders(true);
		ppc.postProcessBeanFactory(bf);

		System.setProperty(P2, P2_SYSTEM_PROPS_VAL);
		getModifiableSystemEnvironment().put(P2, P2_SYSTEM_ENV_VAL);
		Properties ppc2Properties = new Properties();
		ppc2Properties.put(P2, P2_LOCAL_PROPS_VAL);

		PropertyPlaceholderConfigurer ppc2 = new PropertyPlaceholderConfigurer();
		ppc2.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
		ppc2.setProperties(ppc2Properties);

		ppc2Properties = new Properties();
		ppc2Properties.setProperty(P2, P2_LOCAL_PROPS_VAL);
		ppc2.postProcessBeanFactory(bf);

		TestBean p1Bean = bf.getBean("p1Bean", TestBean.class);
		assertThat(p1Bean.getName(), equalTo(P1_LOCAL_PROPS_VAL));

		TestBean p2Bean = bf.getBean("p2Bean", TestBean.class);
		assertThat(p2Bean.getName(), equalTo(P1_LOCAL_PROPS_VAL));
		assertThat(p2Bean.getCountry(), equalTo(P2_SYSTEM_PROPS_VAL));

		System.clearProperty(P2);
		getModifiableSystemEnvironment().remove(P2);
	}

	@Test
	public void customPlaceholderPrefixAndSuffix() {
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setPlaceholderPrefix("@<");
		ppc.setPlaceholderSuffix(">");

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "@<key1>")
				.addPropertyValue("sex", "${key2}")
				.getBeanDefinition());

		System.setProperty("key1", "systemKey1Value");
		System.setProperty("key2", "systemKey2Value");
		ppc.postProcessBeanFactory(bf);
		System.clearProperty("key1");
		System.clearProperty("key2");

		assertThat(bf.getBean(TestBean.class).getName(), is("systemKey1Value"));
		assertThat(bf.getBean(TestBean.class).getSex(), is("${key2}"));
	}

	@Test
	public void nullValueIsPreserved() {
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setNullValue("customNull");
		getModifiableSystemEnvironment().put("my.name", "customNull");
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${my.name}")
				.getBeanDefinition());
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName(), nullValue());
		getModifiableSystemEnvironment().remove("my.name");
	}


	// -------------------------------------------------------------------------
	// Tests for functionality not possible prior to Environment refactoring
	// -------------------------------------------------------------------------

	/**
	 * Tests that properties against a BeanFactory's Environment are used by
	 * PropertyPlaceholderConfigurer during placeholder resolution.
	@Test @SuppressWarnings({ "unchecked", "rawtypes", "serial" })
	public void replacePlaceholdersFromBeanFactoryEnvironmentPropertySources() {
		System.setProperty("key1", "systemValue");

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.getEnvironment().addPropertySource("psCustom", new HashMap() {{ put("key1", "customValue"); }});
		bf.registerBeanDefinition("testBean",
				rootBeanDefinition(TestBean.class).addPropertyValue("name", "${key1}").getBeanDefinition());

		new PropertyPlaceholderConfigurer().postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName(), is("customValue"));

		System.clearProperty("key1");
	}
	 */


	// -------------------------------------------------------------------------
	// Utilities
	// -------------------------------------------------------------------------

	// TODO SPR-7508: duplicated from EnvironmentPropertyResolutionSearchTests
	@SuppressWarnings("unchecked")
	private static Map<String, String> getModifiableSystemEnvironment() {
		Class<?>[] classes = Collections.class.getDeclaredClasses();
		Map<String, String> env = System.getenv();
		for (Class<?> cl : classes) {
			if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
				try {
					Field field = cl.getDeclaredField("m");
					field.setAccessible(true);
					Object obj = field.get(env);
					return (Map<String, String>) obj;
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		}
		throw new IllegalStateException();
	}
}
