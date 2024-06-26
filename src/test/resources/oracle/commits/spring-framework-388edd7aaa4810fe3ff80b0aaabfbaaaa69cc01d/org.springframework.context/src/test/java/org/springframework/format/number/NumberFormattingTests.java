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

package org.springframework.format.number;

import java.math.BigDecimal;
import java.util.Locale;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.annotation.NumberFormat.Style;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.validation.DataBinder;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
public class NumberFormattingTests {

	private FormattingConversionService conversionService = new FormattingConversionService();

	private DataBinder binder;

	@Before
	public void setUp() {
		ConversionServiceFactory.addDefaultConverters(conversionService);
		conversionService.addFormatterForFieldType(Number.class, new NumberFormatter());
		conversionService.addFormatterForFieldAnnotation(new NumberFormatAnnotationFormatterFactory());
		LocaleContextHolder.setLocale(Locale.US);
		binder = new DataBinder(new TestBean());
		binder.setConversionService(conversionService);
	}

	@After
	public void tearDown() {
		LocaleContextHolder.setLocale(null);
	}

	@Test
	public void testDefaultNumberFormatting() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("numberDefault", "3,339.12");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("3,339", binder.getBindingResult().getFieldValue("numberDefault"));		
	}

	@Test
	public void testDefaultNumberFormattingAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("numberDefaultAnnotated", "3,339.12");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("3,339.12", binder.getBindingResult().getFieldValue("numberDefaultAnnotated"));		
	}

	@Test
	public void testCurrencyFormatting() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("currency", "$3,339.12");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("$3,339.12", binder.getBindingResult().getFieldValue("currency"));		
	}

	@Test
	public void testPercentFormatting() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("percent", "53%");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("53%", binder.getBindingResult().getFieldValue("percent"));
	}

	@Test
	public void testPatternFormatting() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("pattern", "1,25.00");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("1,25.00", binder.getBindingResult().getFieldValue("pattern"));
	}

	@SuppressWarnings("unused")
	private static class TestBean {
		
		private Integer numberDefault;
		
		@NumberFormat
		private Double numberDefaultAnnotated;

		@NumberFormat(style=Style.CURRENCY)
		private BigDecimal currency;

		@NumberFormat(style=Style.PERCENT)
		private BigDecimal percent;

		@NumberFormat(pattern="#,##.00")
		private BigDecimal pattern;

		public Integer getNumberDefault() {
			return numberDefault;
		}

		public void setNumberDefault(Integer numberDefault) {
			this.numberDefault = numberDefault;
		}

		public Double getNumberDefaultAnnotated() {
			return numberDefaultAnnotated;
		}

		public void setNumberDefaultAnnotated(Double numberDefaultAnnotated) {
			this.numberDefaultAnnotated = numberDefaultAnnotated;
		}

		public BigDecimal getCurrency() {
			return currency;
		}

		public void setCurrency(BigDecimal currency) {
			this.currency = currency;
		}

		public BigDecimal getPercent() {
			return percent;
		}

		public void setPercent(BigDecimal percent) {
			this.percent = percent;
		}

		public BigDecimal getPattern() {
			return pattern;
		}

		public void setPattern(BigDecimal pattern) {
			this.pattern = pattern;
		}

		
	}
}