/*
 * Copyright 2008 the original author or authors.
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

package org.springframework.web.servlet.tags;

import java.math.BigDecimal;

import javax.servlet.jsp.tagext.Tag;

import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.annotation.NumberFormat.Style;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockPageContext;

public class EvalTagTests extends AbstractTagTests {

	private EvalTag tag;

	private MockPageContext context;

	protected void setUp() throws Exception {
		context = createPageContext();
		context.getRequest().setAttribute("bean", new Bean());
		tag = new EvalTag();
		tag.setPageContext(context);
	}

	public void testEndTagPrintScopedAttributeResult() throws Exception {
		tag.setExpression("bean.method()");
		int action = tag.doStartTag();
		assertEquals(Tag.EVAL_BODY_INCLUDE, action);
		action = tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("foo", ((MockHttpServletResponse)context.getResponse()).getContentAsString());
	}
	
	public void testEndTagPrintFormattedScopedAttributeResult() throws Exception {
		tag.setExpression("bean.formattable");
		int action = tag.doStartTag();
		assertEquals(Tag.EVAL_BODY_INCLUDE, action);
		action = tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, action);
		// TODO - fails because EL does not consider annotations on getter/setter method or field for properties (just annotations on method parameters)
		//assertEquals("25%", ((MockHttpServletResponse)context.getResponse()).getContentAsString());
	}

	public static class Bean {
		
		public String method() {
			return "foo";
		}
		
		@NumberFormat(style=Style.PERCENT)
		public BigDecimal getFormattable() {
			return new BigDecimal(".25");
		}
	}
}
