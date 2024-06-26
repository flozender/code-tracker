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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.http.converter.xml.XmlAwareFormHttpMessageConverter;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.ModelMap;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.HandlerMethodSelector;
import org.springframework.web.method.annotation.ExceptionMethodMapping;
import org.springframework.web.method.annotation.support.ModelAttributeMethodProcessor;
import org.springframework.web.method.annotation.support.ModelMethodProcessor;
import org.springframework.web.method.annotation.support.WebArgumentResolverAdapter;
import org.springframework.web.method.support.HandlerMethodArgumentResolverContainer;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerMethodExceptionResolver;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;
import org.springframework.web.servlet.mvc.method.annotation.support.DefaultMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.support.HttpEntityMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.support.ModelAndViewMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.support.RequestResponseBodyMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.support.ServletRequestMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.support.ServletResponseMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.support.ViewMethodReturnValueHandler;

/**
 * An extension of {@link AbstractHandlerMethodExceptionResolver} that matches thrown exceptions to
 * {@link ExceptionHandler @ExceptionHandler} methods in the handler. If a match is found the
 * exception-handling method is invoked to process the request.
 * 
 * <p>See {@link ExceptionHandler} for information on supported method arguments and return values
 * for exception-handling methods. You can customize method argument resolution and return value 
 * processing through the various bean properties in this class.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see #setCustomArgumentResolvers(WebArgumentResolver[])
 * @see #setCustomModelAndViewResolvers(ModelAndViewResolver[])
 * @see #setMessageConverters(HttpMessageConverter[])
 */
public class RequestMappingHandlerMethodExceptionResolver extends AbstractHandlerMethodExceptionResolver implements
		InitializingBean {

	private WebArgumentResolver[] customArgumentResolvers;

	private HttpMessageConverter<?>[] messageConverters;

	private ModelAndViewResolver[] customModelAndViewResolvers;

	private final Map<Class<?>, ExceptionMethodMapping> exceptionMethodMappingCache = 
		new ConcurrentHashMap<Class<?>, ExceptionMethodMapping>();

	private final HandlerMethodArgumentResolverContainer argumentResolvers = new HandlerMethodArgumentResolverContainer();
	
	private final HandlerMethodReturnValueHandlerContainer returnValueHandlers = new HandlerMethodReturnValueHandlerContainer();

	/**
	 * Creates an instance of {@link RequestMappingHandlerMethodExceptionResolver}.
	 */
	public RequestMappingHandlerMethodExceptionResolver() {
		
		StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
		stringHttpMessageConverter.setWriteAcceptCharset(false); // See SPR-7316
		
		this.messageConverters = new HttpMessageConverter[] { new ByteArrayHttpMessageConverter(),
				stringHttpMessageConverter, new SourceHttpMessageConverter<Source>(),
				new XmlAwareFormHttpMessageConverter() };
	}

	/**
	 * Set a custom ArgumentResolvers to use for special method parameter types.
	 * <p>Such a custom ArgumentResolver will kick in first, having a chance to resolve
	 * an argument value before the standard argument handling kicks in.
	 */
	public void setCustomArgumentResolver(WebArgumentResolver argumentResolver) {
		this.customArgumentResolvers = new WebArgumentResolver[]{argumentResolver};
	}

	/**
	 * Set one or more custom ArgumentResolvers to use for special method parameter types.
	 * <p>Any such custom ArgumentResolver will kick in first, having a chance to resolve
	 * an argument value before the standard argument handling kicks in.
	 */
	public void setCustomArgumentResolvers(WebArgumentResolver[] argumentResolvers) {
		this.customArgumentResolvers = argumentResolvers;
	}

	/**
	 * Set the message body converters to use.
	 * <p>These converters are used to convert from and to HTTP requests and responses.
	 */
	public void setMessageConverters(HttpMessageConverter<?>[] messageConverters) {
		this.messageConverters = messageConverters;
	}

	/**
	 * Set a custom ModelAndViewResolvers to use for special method return types.
	 * <p>Such a custom ModelAndViewResolver will kick in first, having a chance to resolve
	 * a return value before the standard ModelAndView handling kicks in.
	 */
	public void setCustomModelAndViewResolver(ModelAndViewResolver customModelAndViewResolver) {
		this.customModelAndViewResolvers = new ModelAndViewResolver[] {customModelAndViewResolver};
	}

	/**
	 * Set one or more custom ModelAndViewResolvers to use for special method return types.
	 * <p>Any such custom ModelAndViewResolver will kick in first, having a chance to resolve
	 * a return value before the standard ModelAndView handling kicks in.
	 */
	public void setCustomModelAndViewResolvers(ModelAndViewResolver[] customModelAndViewResolvers) {
		this.customModelAndViewResolvers = customModelAndViewResolvers;
	}

	public void afterPropertiesSet() throws Exception {
		if (customArgumentResolvers != null) {
			for (WebArgumentResolver customResolver : customArgumentResolvers) {
				argumentResolvers.registerArgumentResolver(new WebArgumentResolverAdapter(customResolver));
			}
		}	

		argumentResolvers.registerArgumentResolver(new ServletRequestMethodArgumentResolver());
		argumentResolvers.registerArgumentResolver(new ServletResponseMethodArgumentResolver());

		returnValueHandlers.registerReturnValueHandler(new RequestResponseBodyMethodProcessor(messageConverters));
		returnValueHandlers.registerReturnValueHandler(new ModelAttributeMethodProcessor(false));
		returnValueHandlers.registerReturnValueHandler(new ModelAndViewMethodReturnValueHandler());
		returnValueHandlers.registerReturnValueHandler(new ModelMethodProcessor());
		returnValueHandlers.registerReturnValueHandler(new ViewMethodReturnValueHandler());
		returnValueHandlers.registerReturnValueHandler(new HttpEntityMethodProcessor(messageConverters));
		returnValueHandlers.registerReturnValueHandler(new DefaultMethodReturnValueHandler(customModelAndViewResolvers));
	}

	@Override
	protected ModelAndView doResolveHandlerMethodException(HttpServletRequest request, 
														   HttpServletResponse response, 
														   HandlerMethod handlerMethod, 
														   Exception ex) {
		if (handlerMethod != null) {
			ExceptionMethodMapping mapping = getExceptionMethodMapping(handlerMethod);
			Method method = mapping.getMethod(ex);

			if (method != null) {
				Object handler = handlerMethod.getBean();
				ServletInvocableHandlerMethod exceptionHandler = new ServletInvocableHandlerMethod(handler, method);
				exceptionHandler.setArgumentResolverContainer(argumentResolvers);
				exceptionHandler.setReturnValueHandlers(returnValueHandlers);
				
				ServletWebRequest webRequest = new ServletWebRequest(request, response);
				ModelMap model = new ExtendedModelMap();
				try {
					if (logger.isDebugEnabled()) {
						logger.debug("Invoking exception-handling method: " + exceptionHandler);
					}
					ModelAndView mav = exceptionHandler.invokeAndHandle(webRequest , model , ex);
					return (mav != null) ? mav : new ModelAndView();
				}
				catch (Exception invocationEx) {
					logger.error("Invoking exception-handling method resulted in exception : " + 
							exceptionHandler, invocationEx);
				}
			}
		}
		
		return null;
	}

	private ExceptionMethodMapping getExceptionMethodMapping(HandlerMethod handlerMethod) {
		Class<?> handlerType = handlerMethod.getBeanType();
		ExceptionMethodMapping mapping = exceptionMethodMappingCache.get(handlerType);
		if (mapping == null) {
			Set<Method> methods = HandlerMethodSelector.selectMethods(handlerType, EXCEPTION_HANDLER_METHODS);
			mapping = new ExceptionMethodMapping(methods);
			exceptionMethodMappingCache.put(handlerType, mapping);
		}
		return mapping;
	}

	/**
	 * Pre-built MethodFilter that matches {@link ExceptionHandler @ExceptionHandler} methods.
	 */
	public static MethodFilter EXCEPTION_HANDLER_METHODS = new MethodFilter() {

		public boolean matches(Method method) {
			return AnnotationUtils.findAnnotation(method, ExceptionHandler.class) != null;
		}
	};

}
