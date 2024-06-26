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

package org.springframework.autoconfigure;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.springframework.autoconfigure.MessageSourceAutoConfiguration;
import org.springframework.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link MessageSourceAutoConfiguration}.
 * 
 * @author Dave Syer
 */
public class MessageSourceAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Test
	public void testDefaultMessageSource() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(MessageSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertEquals("Foo message",
				this.context.getMessage("foo", null, "Foo message", Locale.UK));
	}

	@Test
	public void testMessageSourceCreated() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(MessageSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("spring.messages.basename", "test/messages");
		this.context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("test", map));
		this.context.refresh();
		assertEquals("bar",
				this.context.getMessage("foo", null, "Foo message", Locale.UK));
	}

}
