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

package org.springframework.beans.factory.xml;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.internal.matchers.TypeSafeMatcher;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.DefaultEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * TODO SPR-7508: document
 *
 * @author Chris Beams
 */
public class EnvironmentBeansTests {

	private static final String PROD_ELIGIBLE_XML = "environmentBeans-prodProfile.xml";
	private static final String DEV_ELIGIBLE_XML = "environmentBeans-devProfile.xml";
	private static final String ALL_ELIGIBLE_XML = "environmentBeans-noProfile.xml";
	private static final String MULTI_ELIGIBLE_XML = "environmentBeans-multiProfile.xml";
	private static final String UNKOWN_ELIGIBLE_XML = "environmentBeans-unknownProfile.xml";

	private static final String PROD_ACTIVE = "prod";
	private static final String DEV_ACTIVE = "dev";
	private static final String NULL_ACTIVE = null;
	private static final String UNKNOWN_ACTIVE = "unknown";
	private static final String[] NONE_ACTIVE = new String[0];
	private static final String[] MULTI_ACTIVE = new String[] { PROD_ACTIVE, DEV_ACTIVE };

	private static final String TARGET_BEAN = "foo";

	@Test
	public void test() {
		assertThat(beanFactoryFor(PROD_ELIGIBLE_XML, NONE_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(PROD_ELIGIBLE_XML, NULL_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(PROD_ELIGIBLE_XML, DEV_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(PROD_ELIGIBLE_XML, PROD_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(PROD_ELIGIBLE_XML, MULTI_ACTIVE), containsTargetBean());

		assertThat(beanFactoryFor(DEV_ELIGIBLE_XML, NONE_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(DEV_ELIGIBLE_XML, NULL_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(DEV_ELIGIBLE_XML, PROD_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(DEV_ELIGIBLE_XML, DEV_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(DEV_ELIGIBLE_XML, MULTI_ACTIVE), containsTargetBean());

		assertThat(beanFactoryFor(ALL_ELIGIBLE_XML, NONE_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(ALL_ELIGIBLE_XML, NULL_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(ALL_ELIGIBLE_XML, DEV_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(ALL_ELIGIBLE_XML, PROD_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(ALL_ELIGIBLE_XML, MULTI_ACTIVE), containsTargetBean());

		assertThat(beanFactoryFor(MULTI_ELIGIBLE_XML, NONE_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(MULTI_ELIGIBLE_XML, NULL_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(MULTI_ELIGIBLE_XML, UNKNOWN_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(MULTI_ELIGIBLE_XML, DEV_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(MULTI_ELIGIBLE_XML, PROD_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(MULTI_ELIGIBLE_XML, MULTI_ACTIVE), containsTargetBean());

		assertThat(beanFactoryFor(UNKOWN_ELIGIBLE_XML, MULTI_ACTIVE), not(containsTargetBean()));
	}

	private BeanDefinitionRegistry beanFactoryFor(String xmlName, String... activeProfileNames) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
		DefaultEnvironment env = new DefaultEnvironment();
		env.setActiveProfiles(activeProfileNames);
		reader.setEnvironment(env);
		reader.loadBeanDefinitions(new ClassPathResource(xmlName, getClass()));
		return beanFactory;
	}


	private static Matcher<BeanDefinitionRegistry> containsBeanDefinition(final String beanName) {
		return new TypeSafeMatcher<BeanDefinitionRegistry>() {

			public void describeTo(Description desc) {
				desc.appendText("a BeanDefinitionRegistry containing bean named ")
					.appendValue(beanName);
			}

			@Override
			public boolean matchesSafely(BeanDefinitionRegistry beanFactory) {
				return beanFactory.containsBeanDefinition(beanName);
			}

		};
	}

	private static Matcher<BeanDefinitionRegistry> containsTargetBean() {
		return containsBeanDefinition(TARGET_BEAN);
	}
}
