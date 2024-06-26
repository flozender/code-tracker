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

package org.springframework.core.env;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.springframework.core.env.AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME;
import static org.springframework.core.env.AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME;

import java.lang.reflect.Field;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.springframework.mock.env.MockPropertySource;


/**
 * Unit tests for {@link DefaultEnvironment}.
 *
 * @author Chris Beams
 */
public class EnvironmentTests {

	private static final String ALLOWED_PROPERTY_NAME = "theanswer";
	private static final String ALLOWED_PROPERTY_VALUE = "42";

	private static final String DISALLOWED_PROPERTY_NAME = "verboten";
	private static final String DISALLOWED_PROPERTY_VALUE = "secret";

	private static final String STRING_PROPERTY_NAME = "stringPropName";
	private static final String STRING_PROPERTY_VALUE = "stringPropValue";
	private static final Object NON_STRING_PROPERTY_NAME = new Object();
	private static final Object NON_STRING_PROPERTY_VALUE = new Object();

	private ConfigurableEnvironment environment = new DefaultEnvironment();

	@Test
	public void activeProfiles() {
		assertThat(environment.getActiveProfiles().length, is(0));
		environment.setActiveProfiles("local", "embedded");
		String[] activeProfiles = environment.getActiveProfiles();
		assertThat(Arrays.asList(activeProfiles), hasItems("local", "embedded"));
		assertThat(activeProfiles.length, is(2));
	}

	@Test
	public void getActiveProfiles_systemPropertiesEmpty() {
		assertThat(environment.getActiveProfiles().length, is(0));
		System.setProperty(ACTIVE_PROFILES_PROPERTY_NAME, "");
		assertThat(environment.getActiveProfiles().length, is(0));
		System.getProperties().remove(ACTIVE_PROFILES_PROPERTY_NAME);
	}

	@Test
	public void getActiveProfiles_fromSystemProperties() {
		assertThat(environment.getActiveProfiles().length, is(0));
		System.setProperty(ACTIVE_PROFILES_PROPERTY_NAME, "foo");
		assertThat(Arrays.asList(environment.getActiveProfiles()), hasItem("foo"));
		System.getProperties().remove(ACTIVE_PROFILES_PROPERTY_NAME);
	}

	@Test
	public void getActiveProfiles_fromSystemProperties_withMultipleProfiles() {
		assertThat(environment.getActiveProfiles().length, is(0));
		System.setProperty(ACTIVE_PROFILES_PROPERTY_NAME, "foo,bar");
		assertThat(Arrays.asList(environment.getActiveProfiles()), hasItems("foo", "bar"));
		System.getProperties().remove(ACTIVE_PROFILES_PROPERTY_NAME);
	}

	@Test
	public void getActiveProfiles_fromSystemProperties_withMulitpleProfiles_withWhitespace() {
		assertThat(environment.getActiveProfiles().length, is(0));
		System.setProperty(ACTIVE_PROFILES_PROPERTY_NAME, " bar , baz "); // notice whitespace
		assertThat(Arrays.asList(environment.getActiveProfiles()), hasItems("bar", "baz"));
		System.getProperties().remove(ACTIVE_PROFILES_PROPERTY_NAME);
	}

	@Test
	public void getDefaultProfiles() {
		assertThat(environment.getDefaultProfiles().length, is(0));
		environment.getPropertySources().addFirst(new MockPropertySource().withProperty(DEFAULT_PROFILES_PROPERTY_NAME, "pd1"));
		assertThat(environment.getDefaultProfiles().length, is(1));
		assertThat(Arrays.asList(environment.getDefaultProfiles()), hasItem("pd1"));
	}

	@Test
	public void setDefaultProfiles() {
		environment.setDefaultProfiles();
		assertThat(environment.getDefaultProfiles().length, is(0));
		environment.setDefaultProfiles("pd1");
		assertThat(Arrays.asList(environment.getDefaultProfiles()), hasItem("pd1"));
		environment.setDefaultProfiles("pd2", "pd3");
		assertThat(Arrays.asList(environment.getDefaultProfiles()), not(hasItem("pd1")));
		assertThat(Arrays.asList(environment.getDefaultProfiles()), hasItems("pd2", "pd3"));
	}

	@Test(expected=IllegalArgumentException.class)
	public void acceptsProfiles_mustSpecifyAtLeastOne() {
		environment.acceptsProfiles();
	}

	@Test
	public void acceptsProfiles_activeProfileSetProgrammatically() {
		assertThat(environment.acceptsProfiles("p1", "p2"), is(false));
		environment.setActiveProfiles("p1");
		assertThat(environment.acceptsProfiles("p1", "p2"), is(true));
		environment.setActiveProfiles("p2");
		assertThat(environment.acceptsProfiles("p1", "p2"), is(true));
		environment.setActiveProfiles("p1", "p2");
		assertThat(environment.acceptsProfiles("p1", "p2"), is(true));
	}

	@Test
	public void acceptsProfiles_activeProfileSetViaProperty() {
		assertThat(environment.acceptsProfiles("p1"), is(false));
		environment.getPropertySources().addFirst(new MockPropertySource().withProperty(ACTIVE_PROFILES_PROPERTY_NAME, "p1"));
		assertThat(environment.acceptsProfiles("p1"), is(true));
	}

	@Test
	public void acceptsProfiles_defaultProfile() {
		assertThat(environment.acceptsProfiles("pd"), is(false));
		environment.setDefaultProfiles("pd");
		assertThat(environment.acceptsProfiles("pd"), is(true));
		environment.setActiveProfiles("p1");
		assertThat(environment.acceptsProfiles("pd"), is(false));
		assertThat(environment.acceptsProfiles("p1"), is(true));
	}

	@Test
	public void getSystemProperties_withAndWithoutSecurityManager() {
		System.setProperty(ALLOWED_PROPERTY_NAME, ALLOWED_PROPERTY_VALUE);
		System.setProperty(DISALLOWED_PROPERTY_NAME, DISALLOWED_PROPERTY_VALUE);
		System.getProperties().put(STRING_PROPERTY_NAME, NON_STRING_PROPERTY_VALUE);
		System.getProperties().put(NON_STRING_PROPERTY_NAME, STRING_PROPERTY_VALUE);

		{
			Map<?, ?> systemProperties = environment.getSystemProperties();
			assertThat(systemProperties, notNullValue());
			assertSame(systemProperties, System.getProperties());
			assertThat(systemProperties.get(ALLOWED_PROPERTY_NAME), equalTo((Object)ALLOWED_PROPERTY_VALUE));
			assertThat(systemProperties.get(DISALLOWED_PROPERTY_NAME), equalTo((Object)DISALLOWED_PROPERTY_VALUE));

			// non-string keys and values work fine... until the security manager is introduced below
			assertThat(systemProperties.get(STRING_PROPERTY_NAME), equalTo(NON_STRING_PROPERTY_VALUE));
			assertThat(systemProperties.get(NON_STRING_PROPERTY_NAME), equalTo((Object)STRING_PROPERTY_VALUE));
		}

		SecurityManager oldSecurityManager = System.getSecurityManager();
		SecurityManager securityManager = new SecurityManager() {
			@Override
			public void checkPropertiesAccess() {
				// see http://download.oracle.com/javase/1.5.0/docs/api/java/lang/System.html#getProperties()
				throw new AccessControlException("Accessing the system properties is disallowed");
			}
			@Override
			public void checkPropertyAccess(String key) {
				// see http://download.oracle.com/javase/1.5.0/docs/api/java/lang/System.html#getProperty(java.lang.String)
				if (DISALLOWED_PROPERTY_NAME.equals(key)) {
					throw new AccessControlException(
							format("Accessing the system property [%s] is disallowed", DISALLOWED_PROPERTY_NAME));
				}
			}
			@Override
			public void checkPermission(Permission perm) {
				// allow everything else
			}
		};
		System.setSecurityManager(securityManager);

		{
			Map<?, ?> systemProperties = environment.getSystemProperties();
			assertThat(systemProperties, notNullValue());
			assertThat(systemProperties, instanceOf(ReadOnlySystemAttributesMap.class));
			assertThat((String)systemProperties.get(ALLOWED_PROPERTY_NAME), equalTo(ALLOWED_PROPERTY_VALUE));
			assertThat(systemProperties.get(DISALLOWED_PROPERTY_NAME), equalTo(null));

			// nothing we can do here in terms of warning the user that there was
			// actually a (non-string) value available. By this point, we only
			// have access to calling System.getProperty(), which itself returns null
			// if the value is non-string.  So we're stuck with returning a potentially
			// misleading null.
			assertThat(systemProperties.get(STRING_PROPERTY_NAME), nullValue());

			// in the case of a non-string *key*, however, we can do better.  Alert
			// the user that under these very special conditions (non-object key +
			// SecurityManager that disallows access to system properties), they
			// cannot do what they're attempting.
			try {
				systemProperties.get(NON_STRING_PROPERTY_NAME);
				fail("Expected IllegalArgumentException when searching with non-string key against ReadOnlySystemAttributesMap");
			} catch (IllegalArgumentException ex) {
				// expected
			}
		}

		System.setSecurityManager(oldSecurityManager);
		System.clearProperty(ALLOWED_PROPERTY_NAME);
		System.clearProperty(DISALLOWED_PROPERTY_NAME);
		System.getProperties().remove(STRING_PROPERTY_NAME);
		System.getProperties().remove(NON_STRING_PROPERTY_NAME);
	}

	@Test
	public void getSystemEnvironment_withAndWithoutSecurityManager() throws Exception {
		getModifiableSystemEnvironment().put(ALLOWED_PROPERTY_NAME, ALLOWED_PROPERTY_VALUE);
		getModifiableSystemEnvironment().put(DISALLOWED_PROPERTY_NAME, DISALLOWED_PROPERTY_VALUE);

		{
			Map<String, String> systemEnvironment = environment.getSystemEnvironment();
			assertThat(systemEnvironment, notNullValue());
			assertSame(systemEnvironment, System.getenv());
		}

		SecurityManager oldSecurityManager = System.getSecurityManager();
		SecurityManager securityManager = new SecurityManager() {
			@Override
			public void checkPermission(Permission perm) {
				//see http://download.oracle.com/javase/1.5.0/docs/api/java/lang/System.html#getenv()
				if ("getenv.*".equals(perm.getName())) {
					throw new AccessControlException("Accessing the system environment is disallowed");
				}
				//see http://download.oracle.com/javase/1.5.0/docs/api/java/lang/System.html#getenv(java.lang.String)
				if (("getenv."+DISALLOWED_PROPERTY_NAME).equals(perm.getName())) {
					throw new AccessControlException(
							format("Accessing the system environment variable [%s] is disallowed", DISALLOWED_PROPERTY_NAME));
				}
			}
		};
		System.setSecurityManager(securityManager);

		{
			Map<String, String> systemEnvironment = environment.getSystemEnvironment();
			assertThat(systemEnvironment, notNullValue());
			assertThat(systemEnvironment, instanceOf(ReadOnlySystemAttributesMap.class));
			assertThat(systemEnvironment.get(ALLOWED_PROPERTY_NAME), equalTo(ALLOWED_PROPERTY_VALUE));
			assertThat(systemEnvironment.get(DISALLOWED_PROPERTY_NAME), nullValue());
		}

		System.setSecurityManager(oldSecurityManager);
		getModifiableSystemEnvironment().remove(ALLOWED_PROPERTY_NAME);
		getModifiableSystemEnvironment().remove(DISALLOWED_PROPERTY_NAME);
	}

	// TODO SPR-7508: duplicated from EnvironmentPropertyResolutionSearchTests
	@SuppressWarnings("unchecked")
	private static Map<String, String> getModifiableSystemEnvironment() throws Exception {
		Class<?>[] classes = Collections.class.getDeclaredClasses();
		Map<String, String> systemEnv = System.getenv();
		for (Class<?> cl : classes) {
			if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
				Field field = cl.getDeclaredField("m");
				field.setAccessible(true);
				Object obj = field.get(systemEnv);
				return (Map<String, String>) obj;
			}
		}
		throw new IllegalStateException();
	}
}
