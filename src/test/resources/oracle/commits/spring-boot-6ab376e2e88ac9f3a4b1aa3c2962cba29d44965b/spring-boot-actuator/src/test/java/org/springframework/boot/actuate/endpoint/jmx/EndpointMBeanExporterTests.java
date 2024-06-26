/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.jmx;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.ObjectUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link EndpointMBeanExporter}
 *
 * @author Christian Dupuis
 */
public class EndpointMBeanExporterTests {

	GenericApplicationContext context = null;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testRegistrationOfOneEndpoint() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class));
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(TestEndpoint.class));
		this.context.refresh();

		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);

		MBeanInfo mbeanInfo = mbeanExporter.getServer()
				.getMBeanInfo(getObjectName("endpoint1", this.context));
		assertNotNull(mbeanInfo);
		assertEquals(3, mbeanInfo.getOperations().length);
		assertEquals(3, mbeanInfo.getAttributes().length);
	}

	@Test
	public void testSkipRegistrationOfDisabledEndpoint() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class));
		MutablePropertyValues mvp = new MutablePropertyValues();
		mvp.add("enabled", Boolean.FALSE);
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(TestEndpoint.class, null, mvp));
		this.context.refresh();
		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);
		assertFalse(mbeanExporter.getServer()
				.isRegistered(getObjectName("endpoint1", this.context)));
	}

	@Test
	public void testRegistrationOfEnabledEndpoint() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class));
		MutablePropertyValues mvp = new MutablePropertyValues();
		mvp.add("enabled", Boolean.TRUE);
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(TestEndpoint.class, null, mvp));
		this.context.refresh();
		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);
		assertTrue(mbeanExporter.getServer()
				.isRegistered(getObjectName("endpoint1", this.context)));
	}

	@Test
	public void testRegistrationTwoEndpoints() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class));
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(TestEndpoint.class));
		this.context.registerBeanDefinition("endpoint2",
				new RootBeanDefinition(TestEndpoint.class));
		this.context.refresh();

		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);

		assertNotNull(mbeanExporter.getServer()
				.getMBeanInfo(getObjectName("endpoint1", this.context)));
		assertNotNull(mbeanExporter.getServer()
				.getMBeanInfo(getObjectName("endpoint2", this.context)));
	}

	@Test
	public void testRegistrationWithDifferentDomain() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class, null,
						new MutablePropertyValues(
								Collections.singletonMap("domain", "test-domain"))));
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(TestEndpoint.class));
		this.context.refresh();

		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);

		assertNotNull(mbeanExporter.getServer().getMBeanInfo(
				getObjectName("test-domain", "endpoint1", false, this.context)));
	}

	@Test
	public void testRegistrationWithDifferentDomainAndIdentity() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("domain", "test-domain");
		properties.put("ensureUniqueRuntimeObjectNames", true);
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class, null,
						new MutablePropertyValues(properties)));
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(TestEndpoint.class));
		this.context.refresh();

		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);

		assertNotNull(mbeanExporter.getServer().getMBeanInfo(
				getObjectName("test-domain", "endpoint1", true, this.context)));
	}

	@Test
	public void testRegistrationWithDifferentDomainAndIdentityAndStaticNames()
			throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("domain", "test-domain");
		properties.put("ensureUniqueRuntimeObjectNames", true);
		Properties staticNames = new Properties();
		staticNames.put("key1", "value1");
		staticNames.put("key2", "value2");
		properties.put("objectNameStaticProperties", staticNames);
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class, null,
						new MutablePropertyValues(properties)));
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(TestEndpoint.class));
		this.context.refresh();

		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);

		assertNotNull(mbeanExporter.getServer()
				.getMBeanInfo(ObjectNameManager.getInstance(
						getObjectName("test-domain", "endpoint1", true, this.context)
								.toString() + ",key1=value1,key2=value2")));
	}

	@Test
	public void testRegistrationWithParentContext() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class));
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(TestEndpoint.class));
		GenericApplicationContext parent = new GenericApplicationContext();

		this.context.setParent(parent);
		parent.refresh();
		this.context.refresh();

		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);

		assertNotNull(mbeanExporter.getServer()
				.getMBeanInfo(getObjectName("endpoint1", this.context)));

		parent.close();
	}

	private ObjectName getObjectName(String beanKey, GenericApplicationContext context)
			throws MalformedObjectNameException {
		return getObjectName("org.springframework.boot", beanKey, false, context);
	}

	private ObjectName getObjectName(String domain, String beanKey,
			boolean includeIdentity, ApplicationContext applicationContext)
					throws MalformedObjectNameException {
		if (includeIdentity) {
			return ObjectNameManager.getInstance(String.format(
					"%s:type=Endpoint,name=%s,identity=%s", domain, beanKey, ObjectUtils
							.getIdentityHexString(applicationContext.getBean(beanKey))));
		}
		else {
			return ObjectNameManager.getInstance(
					String.format("%s:type=Endpoint,name=%s", domain, beanKey));
		}
	}

	public static class TestEndpoint extends AbstractEndpoint<String> {

		public TestEndpoint() {
			super("test");
		}

		@Override
		public String invoke() {
			return "hello world";
		}
	}

}
