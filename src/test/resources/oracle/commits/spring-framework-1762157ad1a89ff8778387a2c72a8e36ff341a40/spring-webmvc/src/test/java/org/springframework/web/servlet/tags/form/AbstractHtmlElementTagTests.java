/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.tags.form;

import static org.easymock.EasyMock.createMock;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockPageContext;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.support.RequestDataValueProcessorWrapper;
import org.springframework.web.servlet.support.JspAwareRequestContext;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.servlet.tags.AbstractTagTests;
import org.springframework.web.servlet.tags.RequestContextAwareTag;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public abstract class AbstractHtmlElementTagTests extends AbstractTagTests {

	public static final String COMMAND_NAME = "testBean";

	private StringWriter writer;

	private MockPageContext pageContext;


	protected final void setUp() throws Exception {
		// set up a writer for the tag content to be written to
		this.writer = new StringWriter();

		// configure the page context
		this.pageContext = createAndPopulatePageContext();

		onSetUp();
	}

	protected MockPageContext createAndPopulatePageContext() throws JspException {
		MockPageContext pageContext = createPageContext();
		MockHttpServletRequest request = (MockHttpServletRequest) pageContext.getRequest();
		StaticWebApplicationContext wac = (StaticWebApplicationContext) RequestContextUtils.getWebApplicationContext(request);
		wac.registerSingleton("requestDataValueProcessor", RequestDataValueProcessorWrapper.class);
		extendRequest(request);
		extendPageContext(pageContext);
		RequestContext requestContext = new JspAwareRequestContext(pageContext);
		pageContext.setAttribute(RequestContextAwareTag.REQUEST_CONTEXT_PAGE_ATTRIBUTE, requestContext);
		return pageContext;
	}

	protected void extendPageContext(MockPageContext pageContext) throws JspException {
	}

	protected void extendRequest(MockHttpServletRequest request) {
	}

	protected void onSetUp() {
	}

	protected MockPageContext getPageContext() {
		return this.pageContext;
	}

	protected Writer getWriter() {
		return this.writer;
	}

	protected String getOutput() {
		return this.writer.toString();
	}

	protected final RequestContext getRequestContext() {
		return (RequestContext) getPageContext().getAttribute(RequestContextAwareTag.REQUEST_CONTEXT_PAGE_ATTRIBUTE);
	}

	protected RequestDataValueProcessor getMockRequestDataValueProcessor() {
		RequestDataValueProcessor mockProcessor = createMock(RequestDataValueProcessor.class);
		ServletRequest request = getPageContext().getRequest();
		StaticWebApplicationContext wac = (StaticWebApplicationContext) RequestContextUtils.getWebApplicationContext(request);
		wac.getBean(RequestDataValueProcessorWrapper.class).setRequestDataValueProcessor(mockProcessor);
		return mockProcessor;
	}

	protected void exposeBindingResult(Errors errors) {
		// wrap errors in a Model
		Map model = new HashMap();
		model.put(BindingResult.MODEL_KEY_PREFIX + COMMAND_NAME, errors);

		// replace the request context with one containing the errors
		MockPageContext pageContext = getPageContext();
		RequestContext context = new RequestContext((HttpServletRequest) pageContext.getRequest(), model);
		pageContext.setAttribute(RequestContextAwareTag.REQUEST_CONTEXT_PAGE_ATTRIBUTE, context);
	}

	protected final void assertContainsAttribute(String output, String attributeName, String attributeValue) {
		String attributeString = attributeName + "=\"" + attributeValue + "\"";
		assertTrue("Expected to find attribute '" + attributeName +
				"' with value '" + attributeValue +
				"' in output + '" + output + "'",
				output.indexOf(attributeString) > -1);
	}

	protected final void assertAttributeNotPresent(String output, String attributeName) {
		assertTrue("Unexpected attribute '" + attributeName + "' in output '" + output + "'.",
				output.indexOf(attributeName + "=\"") < 0);
	}

	protected final void assertBlockTagContains(String output, String desiredContents) {
		String contents = output.substring(output.indexOf(">") + 1, output.lastIndexOf("<"));
		assertTrue("Expected to find '" + desiredContents + "' in the contents of block tag '" + output + "'",
				contents.indexOf(desiredContents) > -1);
	}

}
