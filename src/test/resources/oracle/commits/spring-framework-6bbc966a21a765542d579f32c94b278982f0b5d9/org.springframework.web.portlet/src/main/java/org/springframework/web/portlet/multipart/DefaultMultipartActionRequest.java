/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.web.portlet.multipart;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.portlet.ActionRequest;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.portlet.util.ActionRequestWrapper;

/**
 * Default implementation of the {@link MultipartActionRequest} interface.
 * Provides management of pre-generated parameter values.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see PortletMultipartResolver
 */
public class DefaultMultipartActionRequest extends ActionRequestWrapper implements MultipartActionRequest {

	private Map multipartFiles;

	private Map multipartParameters;


	/**
	 * Wrap the given Portlet ActionRequest in a MultipartActionRequest.
	 * @param request the request to wrap
	 * @param multipartFiles a map of the multipart files
	 * @param multipartParameters a map of the parameters to expose,
	 * with Strings as keys and String arrays as values
	 */
	public DefaultMultipartActionRequest(ActionRequest request, Map multipartFiles, Map multipartParameters) {
		super(request);
		setMultipartFiles(multipartFiles);
		setMultipartParameters(multipartParameters);
	}

	/**
	 * Wrap the given Portlet ActionRequest in a MultipartActionRequest.
	 * @param request the request to wrap
	 */
	protected DefaultMultipartActionRequest(ActionRequest request) {
		super(request);
	}


	public Iterator getFileNames() {
		return getMultipartFiles().keySet().iterator();
	}

	public MultipartFile getFile(String name) {
		return (MultipartFile) getMultipartFiles().get(name);
	}

	public Map getFileMap() {
		return getMultipartFiles();
	}


	@Override
	public Enumeration getParameterNames() {
		Set paramNames = new HashSet();
		Enumeration paramEnum = super.getParameterNames();
		while (paramEnum.hasMoreElements()) {
			paramNames.add(paramEnum.nextElement());
		}
		paramNames.addAll(getMultipartParameters().keySet());
		return Collections.enumeration(paramNames);
	}

	@Override
	public String getParameter(String name) {
		String[] values = (String[]) getMultipartParameters().get(name);
		if (values != null) {
			return (values.length > 0 ? values[0] : null);
		}
		return super.getParameter(name);
	}

	@Override
	public String[] getParameterValues(String name) {
		String[] values = (String[]) getMultipartParameters().get(name);
		if (values != null) {
			return values;
		}
		return super.getParameterValues(name);
	}

	@Override
	public Map getParameterMap() {
		Map paramMap = new HashMap();
		paramMap.putAll(super.getParameterMap());
		paramMap.putAll(getMultipartParameters());
		return paramMap;
	}


	/**
	 * Set a Map with parameter names as keys and MultipartFile objects as values.
	 * To be invoked by subclasses on initialization.
	 */
	protected final void setMultipartFiles(Map multipartFiles) {
		this.multipartFiles = Collections.unmodifiableMap(multipartFiles);
	}

	/**
	 * Obtain the MultipartFile Map for retrieval,
	 * lazily initializing it if necessary.
	 * @see #initializeMultipart()
	 */
	protected Map getMultipartFiles() {
		if (this.multipartFiles == null) {
			initializeMultipart();
		}
		return this.multipartFiles;
	}

	/**
	 * Set a Map with parameter names as keys and String array objects as values.
	 * To be invoked by subclasses on initialization.
	 */
	protected final void setMultipartParameters(Map multipartParameters) {
		this.multipartParameters = multipartParameters;
	}

	/**
	 * Obtain the multipart parameter Map for retrieval,
	 * lazily initializing it if necessary.
	 * @see #initializeMultipart()
	 */
	protected Map getMultipartParameters() {
		if (this.multipartParameters == null) {
			initializeMultipart();
		}
		return this.multipartParameters;
	}

	/**
	 * Lazily initialize the multipart request, if possible.
	 * Only called if not already eagerly initialized.
	 */
	protected void initializeMultipart() {
		throw new IllegalStateException("Multipart request not initialized");
	}

}
