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

package org.springframework.ui.format;

import java.lang.annotation.Annotation;

import org.springframework.core.convert.TypeDescriptor;

/**
 * A shared registry of Formatters.
 *
 * @author Keith Donald
 * @since 3.0
 */
public interface FormatterRegistry {

	/**
	 * Adds a Formatter to this registry indexed by &lt;T&gt;.
	 * Calling getFormatter(&lt;T&gt;.class) returns <code>formatter</code>.
	 * @param formatter the formatter
	 * @param <T> the type of object the formatter formats
	 */
	<T> void add(Formatter<T> formatter);

	/**
	 * Adds a Formatter to this registry indexed by type.
	 * Use this add method when type differs from &lt;T&gt;.
	 * Calling getFormatter(type) returns a decorator that wraps the targetFormatter.
	 * On format, the decorator first coerses the instance of type to &lt;T&gt;, then delegates to <code>targetFormatter</code> to format the value.
	 * On parse, the decorator first delegates to the formatter to parse a &lt;T&gt;, then coerses the parsed value to type.
	 * @param type the object type
	 * @param targetFormatter the target formatter
	 */
	void add(Class<?> type, Formatter<?> targetFormatter);

	/**
	 * Adds a AnnotationFormatterFactory that returns the Formatter for properties annotated with a specific annotation.
	 * @param factory the annotation formatter factory
	 */
	void add(AnnotationFormatterFactory<?, ?> factory);

	/**
	 * Get the Formatter for the type descriptor.
	 * @return the Formatter, or <code>null</code> if no suitable one is registered
	 */
	Formatter<Object> getFormatter(TypeDescriptor type);

}
