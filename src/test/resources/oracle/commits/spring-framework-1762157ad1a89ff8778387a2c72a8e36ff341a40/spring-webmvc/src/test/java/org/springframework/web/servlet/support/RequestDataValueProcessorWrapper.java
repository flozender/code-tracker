/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.support;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.support.RequestDataValueProcessor;

public class RequestDataValueProcessorWrapper implements RequestDataValueProcessor {

	private RequestDataValueProcessor processor;

	public void setRequestDataValueProcessor(RequestDataValueProcessor processor) {
		this.processor = processor;
	}

	public String processUrl(HttpServletRequest request, String url) {
		return (this.processor != null) ? this.processor.processUrl(request, url) : url;
	}

	public String processFormFieldValue(HttpServletRequest request, String name, String value, String type) {
		return (this.processor != null) ? this.processor.processFormFieldValue(request, name, value, type) : value;
	}

	public String processAction(HttpServletRequest request, String action) {
		return (this.processor != null) ? this.processor.processAction(request, action) : action;
	}

	public Map<String, String> getExtraHiddenFields(HttpServletRequest request) {
		return (this.processor != null) ? this.processor.getExtraHiddenFields(request) : null;
	}

}