/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.test.context.junit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.Pet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.GenericPropertiesContextLoader;

/**
 * <p>
 * JUnit 4 based test class, which verifies the expected functionality of
 * {@link SpringJUnit4ClassRunner} in conjunction with support for application
 * contexts loaded from Java {@link Properties} files. Specifically, the
 * {@link ContextConfiguration#loader() loaderClass} and
 * {@link ContextConfiguration#resourceSuffix() resourceSuffix} attributes of
 * &#064;ContextConfiguration are tested.
 * </p>
 * <p>
 * Since no {@link ContextConfiguration#locations() locations} are explicitly
 * defined, the {@link ContextConfiguration#resourceSuffix() resourceSuffix} is
 * set to &quot;-context.properties&quot;, and
 * {@link ContextConfiguration#generateDefaultLocations() generateDefaultLocations}
 * is left set to its default value of {@code true}, this test class's
 * dependencies will be injected via
 * {@link Autowired annotation-based autowiring} from beans defined in the
 * {@link ApplicationContext} loaded from the default classpath resource: &quot;{@code /org/springframework/test/junit4/PropertiesBasedSpringJUnit4ClassRunnerAppCtxTests-context.properties}&quot;.
 * </p>
 *
 * @author Sam Brannen
 * @since 2.5
 * @see GenericPropertiesContextLoader
 * @see SpringJUnit4ClassRunnerAppCtxTests
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = GenericPropertiesContextLoader.class)
public class PropertiesBasedSpringJUnit4ClassRunnerAppCtxTests {

	@Autowired
	private Pet cat;

	@Autowired
	private String testString;


	@Test
	public void verifyAnnotationAutowiredFields() {
		assertNotNull("The cat field should have been autowired.", this.cat);
		assertEquals("Garfield", this.cat.getName());

		assertNotNull("The testString field should have been autowired.", this.testString);
		assertEquals("Test String", this.testString);
	}

}
