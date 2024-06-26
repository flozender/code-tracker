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

package org.springframework.web.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.context.support.RequestHandledEvent;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import org.springframework.web.servlet.handler.SimpleServletHandlerAdapter;
import org.springframework.web.servlet.handler.SimpleServletPostProcessor;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.handler.UserRoleAuthorizationInterceptor;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.theme.SessionThemeResolver;
import org.springframework.web.servlet.theme.ThemeChangeInterceptor;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.ResourceBundleViewResolver;

/**
 * @author Juergen Hoeller
 * @since 21.05.2003
 */
public class ComplexWebApplicationContext extends StaticWebApplicationContext {

	public void refresh() throws BeansException {
		registerSingleton(DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME, SessionLocaleResolver.class);
		registerSingleton(DispatcherServlet.THEME_RESOLVER_BEAN_NAME, SessionThemeResolver.class);

		LocaleChangeInterceptor interceptor1 = new LocaleChangeInterceptor();
		LocaleChangeInterceptor interceptor2 = new LocaleChangeInterceptor();
		interceptor2.setParamName("locale2");
		ThemeChangeInterceptor interceptor3 = new ThemeChangeInterceptor();
		ThemeChangeInterceptor interceptor4 = new ThemeChangeInterceptor();
		interceptor4.setParamName("theme2");
		UserRoleAuthorizationInterceptor interceptor5 = new UserRoleAuthorizationInterceptor();
		interceptor5.setAuthorizedRoles(new String[] {"role1", "role2"});

		List interceptors = new ArrayList();
		interceptors.add(interceptor5);
		interceptors.add(interceptor1);
		interceptors.add(interceptor2);
		interceptors.add(interceptor3);
		interceptors.add(interceptor4);
		interceptors.add(new MyHandlerInterceptor1());
		interceptors.add(new MyHandlerInterceptor2());
		interceptors.add(new MyWebRequestInterceptor());

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add(
				"mappings", "/view.do=viewHandler\n/locale.do=localeHandler\nloc.do=anotherLocaleHandler");
		pvs.add("interceptors", interceptors);
		registerSingleton("myUrlMapping1", SimpleUrlHandlerMapping.class, pvs);

		pvs = new MutablePropertyValues();
		pvs.add(
				"mappings", "/form.do=localeHandler\n/unknown.do=unknownHandler\nservlet.do=myServlet");
		pvs.add("order", "2");
		registerSingleton("myUrlMapping2", SimpleUrlHandlerMapping.class, pvs);

		pvs = new MutablePropertyValues();
		pvs.add(
				"mappings", "/form.do=formHandler\n/head.do=headController\n" +
				"body.do=bodyController\n/noview*=noviewController\n/noview/simple*=noviewController");
		pvs.add("order", "1");
		registerSingleton("handlerMapping", SimpleUrlHandlerMapping.class, pvs);

		registerSingleton("myDummyAdapter", MyDummyAdapter.class);
		registerSingleton("myHandlerAdapter", MyHandlerAdapter.class);
		registerSingleton("standardHandlerAdapter", SimpleControllerHandlerAdapter.class);
		registerSingleton("noviewController", NoViewController.class);

		pvs = new MutablePropertyValues();
		pvs.add("order", new Integer(0));
		pvs.add("basename", "org.springframework.web.servlet.complexviews");
		registerSingleton("viewResolver", ResourceBundleViewResolver.class, pvs);

		pvs = new MutablePropertyValues();
		pvs.add("suffix", ".jsp");
		registerSingleton("viewResolver2", InternalResourceViewResolver.class, pvs);

		pvs = new MutablePropertyValues();
		pvs.add("commandClass", "org.springframework.beans.TestBean");
		pvs.add("formView", "form");
		registerSingleton("formHandler", SimpleFormController.class, pvs);

		pvs = new MutablePropertyValues();
		pvs.add("viewName", "form");
		registerSingleton("viewHandler", ParameterizableViewController.class, pvs);

		registerSingleton("localeHandler", ComplexLocaleChecker.class);
		registerSingleton("anotherLocaleHandler", ComplexLocaleChecker.class);
		registerSingleton("unknownHandler", Object.class);

		registerSingleton("headController", HeadController.class);
		registerSingleton("bodyController", BodyController.class);

		registerSingleton("servletPostProcessor", SimpleServletPostProcessor.class);
		registerSingleton("handlerAdapter", SimpleServletHandlerAdapter.class);
		registerSingleton("myServlet", MyServlet.class);

		pvs = new MutablePropertyValues();
		pvs.add("order", "1");
		pvs.add("exceptionMappings",
			"java.lang.IllegalAccessException=failed2\n" +
			"ServletRequestBindingException=failed3");
		pvs.add("defaultErrorView", "failed0");
		registerSingleton("exceptionResolver1", SimpleMappingExceptionResolver.class, pvs);

		pvs = new MutablePropertyValues();
		pvs.add("order", "0");
		pvs.add("exceptionMappings", "java.lang.Exception=failed1");
		List mappedHandlers = new ManagedList();
		mappedHandlers.add(new RuntimeBeanReference("anotherLocaleHandler"));
		pvs.add("mappedHandlers", mappedHandlers);
		pvs.add("defaultStatusCode", "500");
		pvs.add("defaultErrorView", "failed2");
		registerSingleton("handlerExceptionResolver", SimpleMappingExceptionResolver.class, pvs);

		registerSingleton("multipartResolver", MockMultipartResolver.class);
		registerSingleton("testListener", TestApplicationListener.class);

		addMessage("test", Locale.ENGLISH, "test message");
		addMessage("test", Locale.CANADA, "Canadian & test message");

		super.refresh();
	}


	public static class HeadController implements Controller {

		public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
			if ("HEAD".equals(request.getMethod())) {
				response.setContentLength(5);
			}
			return null;
		}
	}


	public static class BodyController implements Controller {

		public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
			response.getOutputStream().write("body".getBytes());
			return null;
		}
	}


	public static class NoViewController implements Controller {

		public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
			return new ModelAndView();
		}
	}


	public static class MyServlet implements Servlet {

		private ServletConfig servletConfig;

		public void init(ServletConfig servletConfig) throws ServletException {
			this.servletConfig = servletConfig;
		}

		public ServletConfig getServletConfig() {
			return servletConfig;
		}

		public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException {
			servletResponse.getOutputStream().write("body".getBytes());
		}

		public String getServletInfo() {
			return null;
		}

		public void destroy() {
			this.servletConfig = null;
		}
	}


	public static interface MyHandler {

		public void doSomething(HttpServletRequest request) throws ServletException, IllegalAccessException;

		public long lastModified();
	}


	public static class MyHandlerAdapter extends ApplicationObjectSupport implements HandlerAdapter, Ordered {

		public int getOrder() {
			return 99;
		}

		public boolean supports(Object handler) {
			return handler != null && MyHandler.class.isAssignableFrom(handler.getClass());
		}

		public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object delegate)
			throws ServletException, IllegalAccessException {
			((MyHandler) delegate).doSomething(request);
			return null;
		}

		public long getLastModified(HttpServletRequest request, Object delegate) {
			return ((MyHandler) delegate).lastModified();
		}
	}


	public static class MyDummyAdapter extends ApplicationObjectSupport implements HandlerAdapter {

		public boolean supports(Object handler) {
			return handler != null && MyHandler.class.isAssignableFrom(handler.getClass());
		}

		public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object delegate)
			throws IOException, ServletException {
			throw new ServletException("dummy");
		}

		public long getLastModified(HttpServletRequest request, Object delegate) {
			return -1;
		}
	}


	public static class MyHandlerInterceptor1 implements HandlerInterceptor {

		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException {
			if (request.getAttribute("test2") != null) {
				throw new ServletException("Wrong interceptor order");
			}
			request.setAttribute("test1", "test1");
			request.setAttribute("test1x", "test1x");
			request.setAttribute("test1y", "test1y");
			return true;
		}

		public void postHandle(
				HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
				throws ServletException {
			if (request.getAttribute("test2x") != null) {
				throw new ServletException("Wrong interceptor order");
			}
			if (!"test1x".equals(request.getAttribute("test1x"))) {
				throw new ServletException("Incorrect request attribute");
			}
			request.removeAttribute("test1x");
		}

		public void afterCompletion(
				HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
				throws ServletException {
			if (request.getAttribute("test2y") != null) {
				throw new ServletException("Wrong interceptor order");
			}
			request.removeAttribute("test1y");
		}
	}


	public static class MyHandlerInterceptor2 implements HandlerInterceptor {

		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException {
			if (request.getAttribute("test1x") == null) {
				throw new ServletException("Wrong interceptor order");
			}
			if (request.getParameter("abort") != null) {
				return false;
			}
			request.setAttribute("test2", "test2");
			request.setAttribute("test2x", "test2x");
			request.setAttribute("test2y", "test2y");
			return true;
		}

		public void postHandle(
				HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
				throws ServletException {
			if (request.getParameter("noView") != null) {
				modelAndView.clear();
			}
			if (request.getAttribute("test1x") == null) {
				throw new ServletException("Wrong interceptor order");
			}
			if (!"test2x".equals(request.getAttribute("test2x"))) {
				throw new ServletException("Incorrect request attribute");
			}
			request.removeAttribute("test2x");
		}

		public void afterCompletion(
				HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
				throws Exception {
			if (request.getAttribute("test1y") == null) {
				throw new ServletException("Wrong interceptor order");
			}
			request.removeAttribute("test2y");
		}
	}


	public static class MyWebRequestInterceptor implements WebRequestInterceptor {

		public void preHandle(WebRequest request) throws Exception {
			request.setAttribute("test3", "test3", WebRequest.SCOPE_REQUEST);
		}

		public void postHandle(WebRequest request, ModelMap model) throws Exception {
			request.setAttribute("test3x", "test3x", WebRequest.SCOPE_REQUEST);
		}

		public void afterCompletion(WebRequest request, Exception ex) throws Exception {
			request.setAttribute("test3y", "test3y", WebRequest.SCOPE_REQUEST);
		}
	}


	public static class ComplexLocaleChecker implements MyHandler {

		public void doSomething(HttpServletRequest request) throws ServletException, IllegalAccessException {
			WebApplicationContext wac = RequestContextUtils.getWebApplicationContext(request);
			if (!(wac instanceof ComplexWebApplicationContext)) {
				throw new ServletException("Incorrect WebApplicationContext");
			}
			if (!(request instanceof MultipartHttpServletRequest)) {
				throw new ServletException("Not in a MultipartHttpServletRequest");
			}
			if (!(RequestContextUtils.getLocaleResolver(request) instanceof SessionLocaleResolver)) {
				throw new ServletException("Incorrect LocaleResolver");
			}
			if (!Locale.CANADA.equals(RequestContextUtils.getLocale(request))) {
				throw new ServletException("Incorrect Locale");
			}
			if (!Locale.CANADA.equals(LocaleContextHolder.getLocale())) {
				throw new ServletException("Incorrect Locale");
			}
			if (!(RequestContextUtils.getThemeResolver(request) instanceof SessionThemeResolver)) {
				throw new ServletException("Incorrect ThemeResolver");
			}
			if (!"theme".equals(RequestContextUtils.getThemeResolver(request).resolveThemeName(request))) {
				throw new ServletException("Incorrect theme name");
			}
			if (request.getParameter("fail") != null) {
				throw new ModelAndViewDefiningException(new ModelAndView("failed1"));
			}
			if (request.getParameter("access") != null) {
				throw new IllegalAccessException("illegal access");
			}
			if (request.getParameter("servlet") != null) {
				throw new ServletRequestBindingException("servlet");
			}
			if (request.getParameter("exception") != null) {
				throw new RuntimeException("servlet");
			}
		}

		public long lastModified() {
			return 99;
		}
	}


	public static class MockMultipartResolver implements MultipartResolver {

		public boolean isMultipart(HttpServletRequest request) {
			return true;
		}

		public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
			if (request.getAttribute("fail") != null) {
				throw new MaxUploadSizeExceededException(1000);
			}
			if (request instanceof MultipartHttpServletRequest) {
				throw new IllegalStateException("Already a multipart request");
			}
			if (request.getAttribute("resolved") != null) {
				throw new IllegalStateException("Already resolved");
			}
			request.setAttribute("resolved", Boolean.TRUE);
			return new AbstractMultipartHttpServletRequest(request) {
				public HttpHeaders getMultipartHeaders(String paramOrFileName) {
					return null;
				}
				public String getMultipartContentType(String paramOrFileName) {
					return null;
				}
			};
		}

		public void cleanupMultipart(MultipartHttpServletRequest request) {
			if (request.getAttribute("cleanedUp") != null) {
				throw new IllegalStateException("Already cleaned up");
			}
			request.setAttribute("cleanedUp", Boolean.TRUE);
		}
	}


	public static class TestApplicationListener implements ApplicationListener {

		public int counter = 0;

		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof RequestHandledEvent) {
				this.counter++;
			}
		}
	}

}
