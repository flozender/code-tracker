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
package org.springframework.ui.format.jodatime;

import java.util.Locale;

import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.ui.format.Printer;

/**
 * Prints Joda Time {@link ReadableInstant} instances using a {@link DateTimeFormatter}.
 * @author Keith Donald
 */
public final class ReadableInstantPrinter implements Printer<ReadableInstant> {

	private final DateTimeFormatter formatter;

	/**
	 * Creates a new ReadableInstantPrinter.
	 * @param formatter the Joda DateTimeFormatter instance
	 */
	public ReadableInstantPrinter(DateTimeFormatter formatter) {
		this.formatter = formatter;
	}

	public String print(ReadableInstant instant, Locale locale) {
		return JodaTimeContextHolder.getFormatter(this.formatter, locale).print(instant);
	}
}
