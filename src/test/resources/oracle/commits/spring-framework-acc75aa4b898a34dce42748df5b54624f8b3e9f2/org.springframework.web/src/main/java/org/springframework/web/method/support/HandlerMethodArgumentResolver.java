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

package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Strategy interface for resolving method parameters into argument values from a given request.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public interface HandlerMethodArgumentResolver extends HandlerMethodProcessor {

	/**
	 * Indicates whether the given {@linkplain MethodParameter method parameter} is supported by this resolver.
	 * 
	 * @param parameter the method parameter to check
	 * @return {@code true} if this resolver supports the supplied parameter; {@code false} otherwise
	 */
	boolean supportsParameter(MethodParameter parameter);

	/**
	 * Resolves a method parameter into an argument value from a given request and a {@link ModelMap} providing
	 * the ability to both access and add new model attributes. A {@link WebDataBinderFactory} is also provided
	 * for creating a {@link WebDataBinder} instance to use for data binding and type conversion.
	 * 
	 * @param parameter the parameter to resolve to an argument. This parameter must have previously been passed to
	 * {@link #supportsParameter(org.springframework.core.MethodParameter)} and it must have returned {@code true}
	 * @param model the model for the current request
	 * @param webRequest the current request.
	 * @param binderFactory a factory in case the resolver needs to create a {@link WebDataBinder} instance
	 * @return the resolved argument value, or {@code null}.
	 * @throws Exception in case of errors with the preparation of argument values
	 */
	Object resolveArgument(MethodParameter parameter, 
						   ModelMap model, 
						   NativeWebRequest webRequest, 
						   WebDataBinderFactory binderFactory) throws Exception;

}