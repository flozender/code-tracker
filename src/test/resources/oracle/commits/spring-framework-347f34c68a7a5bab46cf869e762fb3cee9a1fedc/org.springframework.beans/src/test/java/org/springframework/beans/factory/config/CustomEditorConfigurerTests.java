/*
 * Copyright 2002-2008 the original author or authors.
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

import java.beans.PropertyEditorSupport;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.propertyeditors.CustomDateEditor;

/**
 * @author Juergen Hoeller
 * @since 31.07.2004
 */
public class CustomEditorConfigurerTests extends TestCase {

	public void testCustomEditorConfigurerWithPropertyEditorRegistrar() throws ParseException {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		CustomEditorConfigurer cec = new CustomEditorConfigurer();
		final DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.GERMAN);
		cec.setPropertyEditorRegistrars(new PropertyEditorRegistrar[] {
				new PropertyEditorRegistrar() {
					public void registerCustomEditors(PropertyEditorRegistry registry) {
						registry.registerCustomEditor(Date.class, new CustomDateEditor(df, true));
					}
				}});
		cec.postProcessBeanFactory(bf);

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("date", "2.12.1975");
		bf.registerBeanDefinition("tb1", new RootBeanDefinition(TestBean.class, pvs));
		pvs = new MutablePropertyValues();
		pvs.addPropertyValue("someMap[myKey]", new TypedStringValue("2.12.1975", Date.class));
		bf.registerBeanDefinition("tb2", new RootBeanDefinition(TestBean.class, pvs));

		TestBean tb1 = (TestBean) bf.getBean("tb1");
		assertEquals(df.parse("2.12.1975"), tb1.getDate());
		TestBean tb2 = (TestBean) bf.getBean("tb2");
		assertEquals(df.parse("2.12.1975"), tb2.getSomeMap().get("myKey"));
	}

	public void testCustomEditorConfigurerWithEditorClassName() throws ParseException {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		CustomEditorConfigurer cec = new CustomEditorConfigurer();
		Map<String, String> editors = new HashMap<String, String>();
		editors.put(Date.class.getName(), MyDateEditor.class.getName());
		cec.setCustomEditors(editors);
		cec.postProcessBeanFactory(bf);

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("date", "2.12.1975");
		bf.registerBeanDefinition("tb", new RootBeanDefinition(TestBean.class, pvs));

		TestBean tb = (TestBean) bf.getBean("tb");
		DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.GERMAN);
		assertEquals(df.parse("2.12.1975"), tb.getDate());
	}

	public void testCustomEditorConfigurerWithRequiredTypeArray() throws ParseException {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		CustomEditorConfigurer cec = new CustomEditorConfigurer();
		Map<String, String> editors = new HashMap<String, String>();
		editors.put("java.lang.String[]", MyTestEditor.class.getName());
		cec.setCustomEditors(editors);
		cec.postProcessBeanFactory(bf);

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("stringArray", "xxx");
		bf.registerBeanDefinition("tb", new RootBeanDefinition(TestBean.class, pvs));

		TestBean tb = (TestBean) bf.getBean("tb");
		assertTrue(tb.getStringArray() != null && tb.getStringArray().length == 1);
		assertEquals("test", tb.getStringArray()[0]);
	}

	public void testCustomEditorConfigurerWithUnresolvableEditor() throws ParseException {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		CustomEditorConfigurer cec = new CustomEditorConfigurer();
		Map<String, String> editors = new HashMap<String, String>();
		editors.put(Date.class.getName(), "MyNonExistingEditor");
		editors.put("MyNonExistingType", "MyNonExistingEditor");
		cec.setCustomEditors(editors);
		try {
			cec.postProcessBeanFactory(bf);
			fail("Should have thrown FatalBeanException");
		}
		catch (FatalBeanException ex) {
			assertTrue(ex.getCause() instanceof ClassNotFoundException);
		}
	}

	public void testCustomEditorConfigurerWithIgnoredUnresolvableEditor() throws ParseException {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		CustomEditorConfigurer cec = new CustomEditorConfigurer();
		Map<String, String> editors = new HashMap<String, String>();
		editors.put(Date.class.getName(), "MyNonExistingEditor");
		editors.put("MyNonExistingType", "MyNonExistingEditor");
		cec.setCustomEditors(editors);
		cec.setIgnoreUnresolvableEditors(true);
		cec.postProcessBeanFactory(bf);
	}


	public static class MyDateEditor extends CustomDateEditor {

		public MyDateEditor() {
			super(DateFormat.getDateInstance(DateFormat.SHORT, Locale.GERMAN), true);
		}
	}


	public static class MyTestEditor extends PropertyEditorSupport {

		public void setAsText(String text) {
			setValue(new String[] {"test"});
		}
	}

}
