/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Valve;
import org.apache.catalina.valves.AccessLogValve;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.mvc.ManagementErrorEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.hateoas.HypermediaHttpMessageConverterConfiguration;
import org.springframework.boot.autoconfigure.web.DefaultServletWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Configuration triggered from {@link EndpointWebMvcAutoConfiguration} when a new
 * {@link WebServer} running on a different port is required.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @see EndpointWebMvcAutoConfiguration
 */
@Configuration
@EnableWebMvc
@Import(ManagementContextConfigurationsImportSelector.class)
public class EndpointWebMvcChildContextConfiguration {

	@Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
	public DispatcherServlet dispatcherServlet() {
		DispatcherServlet dispatcherServlet = new DispatcherServlet();
		// Ensure the parent configuration does not leak down to us
		dispatcherServlet.setDetectAllHandlerAdapters(false);
		dispatcherServlet.setDetectAllHandlerExceptionResolvers(false);
		dispatcherServlet.setDetectAllHandlerMappings(false);
		dispatcherServlet.setDetectAllViewResolvers(false);
		return dispatcherServlet;
	}

	@Bean(name = DispatcherServlet.HANDLER_MAPPING_BEAN_NAME)
	public CompositeHandlerMapping compositeHandlerMapping() {
		return new CompositeHandlerMapping();
	}

	@Bean(name = DispatcherServlet.HANDLER_ADAPTER_BEAN_NAME)
	public CompositeHandlerAdapter compositeHandlerAdapter() {
		return new CompositeHandlerAdapter();
	}

	@Bean(name = DispatcherServlet.HANDLER_EXCEPTION_RESOLVER_BEAN_NAME)
	public CompositeHandlerExceptionResolver compositeHandlerExceptionResolver() {
		return new CompositeHandlerExceptionResolver();
	}

	@Bean
	public ServerFactoryCustomization serverCustomization() {
		return new ServerFactoryCustomization();
	}

	@Bean
	public UndertowAccessLogCustomizer undertowAccessLogCustomizer() {
		return new UndertowAccessLogCustomizer();
	}

	@Bean
	@ConditionalOnClass(name = "org.apache.catalina.valves.AccessLogValve")
	public TomcatAccessLogCustomizer tomcatAccessLogCustomizer() {
		return new TomcatAccessLogCustomizer();
	}

	/*
	 * The error controller is present but not mapped as an endpoint in this context
	 * because of the DispatcherServlet having had its HandlerMapping explicitly disabled.
	 * So we expose the same feature but only for machine endpoints.
	 */
	@Bean
	@ConditionalOnBean(ErrorAttributes.class)
	public ManagementErrorEndpoint errorEndpoint(ErrorAttributes errorAttributes) {
		return new ManagementErrorEndpoint(errorAttributes);
	}

	/**
	 * Configuration to add {@link HandlerMapping} for {@link MvcEndpoint}s.
	 */
	@Configuration
	protected static class EndpointHandlerMappingConfiguration {

		@Autowired
		public void handlerMapping(MvcEndpoints endpoints,
				ListableBeanFactory beanFactory, EndpointHandlerMapping mapping) {
			// In a child context we definitely want to see the parent endpoints
			mapping.setDetectHandlerMethodsInAncestorContexts(true);
		}

	}

	@Configuration
	@ConditionalOnClass({ EnableWebSecurity.class, Filter.class })
	@ConditionalOnBean(name = "springSecurityFilterChain", search = SearchStrategy.ANCESTORS)
	public static class EndpointWebMvcChildContextSecurityConfiguration {

		@Bean
		public Filter springSecurityFilterChain(HierarchicalBeanFactory beanFactory) {
			BeanFactory parent = beanFactory.getParentBeanFactory();
			return parent.getBean("springSecurityFilterChain", Filter.class);
		}

	}

	@Configuration
	@ConditionalOnClass({ LinkDiscoverer.class })
	@Import(HypermediaHttpMessageConverterConfiguration.class)
	@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
	static class HypermediaConfiguration {

	}

	static class ServerFactoryCustomization implements
			WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>, Ordered {

		@Autowired
		private ListableBeanFactory beanFactory;

		// This needs to be lazily initialized because web server customizer
		// instances get their callback very early in the context lifecycle.
		private ManagementServerProperties managementServerProperties;

		private ServerProperties server;

		private DefaultServletWebServerFactoryCustomizer serverCustomizer;

		@Override
		public int getOrder() {
			return 0;
		}

		@Override
		public void customize(ConfigurableServletWebServerFactory webServerFactory) {
			if (this.managementServerProperties == null) {
				this.managementServerProperties = BeanFactoryUtils
						.beanOfTypeIncludingAncestors(this.beanFactory,
								ManagementServerProperties.class);
				this.server = BeanFactoryUtils.beanOfTypeIncludingAncestors(
						this.beanFactory, ServerProperties.class);
				this.serverCustomizer = BeanFactoryUtils.beanOfTypeIncludingAncestors(
						this.beanFactory, DefaultServletWebServerFactoryCustomizer.class);
			}
			// Customize as per the parent context first (so e.g. the access logs go to
			// the same place)
			this.serverCustomizer.customize(webServerFactory);
			// Then reset the error pages
			webServerFactory.setErrorPages(Collections.<ErrorPage>emptySet());
			// and the context path
			webServerFactory.setContextPath("");
			// and add the management-specific bits
			webServerFactory.setPort(this.managementServerProperties.getPort());
			if (this.managementServerProperties.getSsl() != null) {
				webServerFactory.setSsl(this.managementServerProperties.getSsl());
			}
			webServerFactory.setServerHeader(this.server.getServerHeader());
			webServerFactory.setAddress(this.managementServerProperties.getAddress());
			webServerFactory
					.addErrorPages(new ErrorPage(this.server.getError().getPath()));
		}

	}

	static class CompositeHandlerMapping implements HandlerMapping {

		@Autowired
		private ListableBeanFactory beanFactory;

		private List<HandlerMapping> mappings;

		@Override
		public HandlerExecutionChain getHandler(HttpServletRequest request)
				throws Exception {
			if (this.mappings == null) {
				this.mappings = extractMappings();
			}
			for (HandlerMapping mapping : this.mappings) {
				HandlerExecutionChain handler = mapping.getHandler(request);
				if (handler != null) {
					return handler;
				}
			}
			return null;
		}

		private List<HandlerMapping> extractMappings() {
			List<HandlerMapping> list = new ArrayList<>();
			list.addAll(this.beanFactory.getBeansOfType(HandlerMapping.class).values());
			list.remove(this);
			AnnotationAwareOrderComparator.sort(list);
			return list;
		}

	}

	static class CompositeHandlerAdapter implements HandlerAdapter {

		@Autowired
		private ListableBeanFactory beanFactory;

		private List<HandlerAdapter> adapters;

		private List<HandlerAdapter> extractAdapters() {
			List<HandlerAdapter> list = new ArrayList<>();
			list.addAll(this.beanFactory.getBeansOfType(HandlerAdapter.class).values());
			list.remove(this);
			AnnotationAwareOrderComparator.sort(list);
			return list;
		}

		@Override
		public boolean supports(Object handler) {
			if (this.adapters == null) {
				this.adapters = extractAdapters();
			}
			for (HandlerAdapter mapping : this.adapters) {
				if (mapping.supports(handler)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public ModelAndView handle(HttpServletRequest request,
				HttpServletResponse response, Object handler) throws Exception {
			if (this.adapters == null) {
				this.adapters = extractAdapters();
			}
			for (HandlerAdapter mapping : this.adapters) {
				if (mapping.supports(handler)) {
					return mapping.handle(request, response, handler);
				}
			}
			return null;
		}

		@Override
		public long getLastModified(HttpServletRequest request, Object handler) {
			if (this.adapters == null) {
				this.adapters = extractAdapters();
			}
			for (HandlerAdapter mapping : this.adapters) {
				if (mapping.supports(handler)) {
					return mapping.getLastModified(request, handler);
				}
			}
			return 0;
		}

	}

	static class CompositeHandlerExceptionResolver implements HandlerExceptionResolver {

		@Autowired
		private ListableBeanFactory beanFactory;

		private List<HandlerExceptionResolver> resolvers;

		private List<HandlerExceptionResolver> extractResolvers() {
			List<HandlerExceptionResolver> list = new ArrayList<>();
			list.addAll(this.beanFactory.getBeansOfType(HandlerExceptionResolver.class)
					.values());
			list.remove(this);
			AnnotationAwareOrderComparator.sort(list);
			return list;
		}

		@Override
		public ModelAndView resolveException(HttpServletRequest request,
				HttpServletResponse response, Object handler, Exception ex) {
			if (this.resolvers == null) {
				this.resolvers = extractResolvers();
			}
			for (HandlerExceptionResolver mapping : this.resolvers) {
				ModelAndView mav = mapping.resolveException(request, response, handler,
						ex);
				if (mav != null) {
					return mav;
				}
			}
			return null;
		}

	}

	static abstract class AccessLogCustomizer implements Ordered {

		protected String customizePrefix(String prefix) {
			return "management_" + prefix;
		}

		@Override
		public int getOrder() {
			return 1;
		}

	}

	static class TomcatAccessLogCustomizer extends AccessLogCustomizer
			implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

		@Override
		public void customize(TomcatServletWebServerFactory serverFactory) {
			AccessLogValve accessLogValve = findAccessLogValve(serverFactory);
			if (accessLogValve == null) {
				return;
			}
			accessLogValve.setPrefix(customizePrefix(accessLogValve.getPrefix()));
		}

		private AccessLogValve findAccessLogValve(
				TomcatServletWebServerFactory serverFactory) {
			for (Valve engineValve : serverFactory.getEngineValves()) {
				if (engineValve instanceof AccessLogValve) {
					return (AccessLogValve) engineValve;
				}
			}
			return null;
		}

	}

	static class UndertowAccessLogCustomizer extends AccessLogCustomizer
			implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {

		@Override
		public void customize(UndertowServletWebServerFactory serverFactory) {
			serverFactory.setAccessLogPrefix(
					customizePrefix(serverFactory.getAccessLogPrefix()));
		}

	}

}
