/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.context.annotation.configuration;

import static org.junit.Assert.*;
import org.junit.Test;
import test.beans.ITestBean;
import test.beans.TestBean;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.StandardScopes;

/**
 * Miscellaneous system tests covering {@link Bean} naming, aliases, scoping and error
 * handling within {@link Configuration} class definitions.
 *
 * @author Chris Beams
 */
public class ConfigurationClassProcessingTests {

	/**
	 * Creates a new {@link BeanFactory}, populates it with a {@link BeanDefinition} for
	 * each of the given {@link Configuration} <var>configClasses</var>, and then
	 * post-processes the factory using JavaConfig's {@link ConfigurationClassPostProcessor}.
	 * When complete, the factory is ready to service requests for any {@link Bean} methods
	 * declared by <var>configClasses</var>.
	 */
	private BeanFactory initBeanFactory(Class<?>... configClasses) {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		for (Class<?> configClass : configClasses) {
			String configBeanName = configClass.getName();
			factory.registerBeanDefinition(configBeanName, new RootBeanDefinition(configClass));
		}
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(factory);
		factory.addBeanPostProcessor(new AutowiredAnnotationBeanPostProcessor());
		return factory;
	}


	@Test
	public void customBeanNameIsRespected() {
		BeanFactory factory = initBeanFactory(ConfigWithBeanWithCustomName.class);
		assertSame(factory.getBean("customName"), ConfigWithBeanWithCustomName.testBean);

		// method name should not be registered
		try {
			factory.getBean("methodName");
			fail("bean should not have been registered with 'methodName'");
		} catch (NoSuchBeanDefinitionException ex) { /* expected */ }
	}

	@Test
	public void aliasesAreRespected() {
		BeanFactory factory = initBeanFactory(ConfigWithBeanWithAliases.class);
		assertSame(factory.getBean("name1"), ConfigWithBeanWithAliases.testBean);
		String[] aliases = factory.getAliases("name1");
		for(String alias : aliases)
			assertSame(factory.getBean(alias), ConfigWithBeanWithAliases.testBean);

		// method name should not be registered
		try {
			factory.getBean("methodName");
			fail("bean should not have been registered with 'methodName'");
		} catch (NoSuchBeanDefinitionException ex) { /* expected */ }
	}

	@Test(expected=BeanDefinitionParsingException.class)
	public void testFinalBeanMethod() {
		initBeanFactory(ConfigWithFinalBean.class);
	}

	@Test
	public void simplestPossibleConfiguration() {
		BeanFactory factory = initBeanFactory(SimplestPossibleConfig.class);
		String stringBean = factory.getBean("stringBean", String.class);
		assertEquals(stringBean, "foo");
	}

	@Test
	public void configurationWithPrototypeScopedBeans() {
		BeanFactory factory = initBeanFactory(ConfigWithPrototypeBean.class);

		TestBean foo = factory.getBean("foo", TestBean.class);
		ITestBean bar = factory.getBean("bar", ITestBean.class);
		ITestBean baz = factory.getBean("baz", ITestBean.class);

		assertSame(foo.getSpouse(), bar);
		assertNotSame(bar.getSpouse(), baz);
	}


	@Configuration
	static class ConfigWithBeanWithCustomName {
		static TestBean testBean = new TestBean();
		@Bean(name="customName")
		public TestBean methodName() {
			return testBean;
		}
	}


	@Configuration
	static class ConfigWithFinalBean {
		public final @Bean TestBean testBean() {
			return new TestBean();
		}
	}


	@Configuration
	static class SimplestPossibleConfig {
		public @Bean String stringBean() {
			return "foo";
		}
	}


	@Configuration
	static class ConfigWithBeanWithAliases {
		static TestBean testBean = new TestBean();
		@Bean(name={"name1", "alias1", "alias2", "alias3"})
		public TestBean methodName() {
			return testBean;
		}
	}


	@Configuration
	static class ConfigWithPrototypeBean {

		public @Bean TestBean foo() {
			TestBean foo = new TestBean("foo");
			foo.setSpouse(bar());
			return foo;
		}

		public @Bean TestBean bar() {
			TestBean bar = new TestBean("bar");
			bar.setSpouse(baz());
			return bar;
		}

		@Bean @Scope(StandardScopes.PROTOTYPE)
		public TestBean baz() {
			return new TestBean("bar");
		}
	}

}
