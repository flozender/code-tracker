/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.reactive;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.WebServerExchange;

/**
 * Central dispatcher for HTTP request handlers/controllers. Dispatches to registered
 * handlers for processing a web request, providing convenient mapping facilities.
 *
 * <p>It can use any {@link HandlerMapping} implementation to control the routing of
 * requests to handler objects. HandlerMapping objects can be defined as beans in
 * the application context.
 *
 * <p>It can use any {@link HandlerAdapter}; this allows for using any handler interface.
 * HandlerAdapter objects can be added as beans in the application context.
 *
 * <p>It can use any {@link HandlerResultHandler}; this allows to process the result of
 * the request handling. HandlerResultHandler objects can be added as beans in the
 * application context.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class DispatcherHandler implements WebHandler, ApplicationContextAware {

	private static final Log logger = LogFactory.getLog(DispatcherHandler.class);


	private List<HandlerMapping> handlerMappings;

	private List<HandlerAdapter> handlerAdapters;

	private List<HandlerResultHandler> resultHandlers;

	private Function<Throwable, Throwable> errorMapper = new DispatcherHandlerExceptionMapper();


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		initStrategies(applicationContext);
	}

	/**
	 * Configure a function to map error signals from the {@code DispatcherHandler}.
	 * <p>By default this is set to {@link DispatcherHandlerExceptionMapper}.
	 * @param errorMapper the function
	 */
	public void setErrorMapper(Function<Throwable, Throwable> errorMapper) {
		this.errorMapper = errorMapper;
	}

	/**
	 * Return the configured function for mapping exceptions.
	 */
	public Function<Throwable, Throwable> getErrorMapper() {
		return this.errorMapper;
	}

	protected void initStrategies(ApplicationContext context) {

		Map<String, HandlerMapping> mappingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerMapping.class, true, false);

		this.handlerMappings = new ArrayList<>(mappingBeans.values());
		this.handlerMappings.add(new NotFoundHandlerMapping());
		AnnotationAwareOrderComparator.sort(this.handlerMappings);

		Map<String, HandlerAdapter> adapterBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerAdapter.class, true, false);

		this.handlerAdapters = new ArrayList<>(adapterBeans.values());
		AnnotationAwareOrderComparator.sort(this.handlerAdapters);

		Map<String, HandlerResultHandler> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerResultHandler.class, true, false);

		this.resultHandlers = new ArrayList<>(beans.values());
		AnnotationAwareOrderComparator.sort(this.resultHandlers);
	}


	@Override
	public Mono<Void> handle(WebServerExchange exchange) {
		if (logger.isDebugEnabled()) {
			ServerHttpRequest request = exchange.getRequest();
			logger.debug("Processing " + request.getMethod() + " request for [" + request.getURI() + "]");
		}
		return Flux.fromIterable(this.handlerMappings)
				.concatMap(mapping -> mapping.getHandler(exchange))
				.next()
				.then(handler -> invokeHandler(exchange, handler))
				.then(result -> handleResult(exchange, result))
				.otherwise(ex -> Mono.error(this.errorMapper.apply(ex)));
	}

	private Mono<HandlerResult> invokeHandler(WebServerExchange exchange, Object handler) {
		for (HandlerAdapter handlerAdapter : this.handlerAdapters) {
			if (handlerAdapter.supports(handler)) {
				return handlerAdapter.handle(exchange, handler);
			}
		}
		return Mono.error(new IllegalStateException("No HandlerAdapter: " + handler));
	}

	private Mono<Void> handleResult(WebServerExchange exchange, HandlerResult result) {
		return getResultHandler(result).handleResult(exchange, result)
				.otherwise(ex -> result.applyExceptionHandler(ex).then(exceptionResult ->
						getResultHandler(result).handleResult(exchange, exceptionResult)));
	}

	private HandlerResultHandler getResultHandler(HandlerResult handlerResult) {
		for (HandlerResultHandler resultHandler : resultHandlers) {
			if (resultHandler.supports(handlerResult)) {
				return resultHandler;
			}
		}
		throw new IllegalStateException("No HandlerResultHandler for " + handlerResult.getResult());
	}


	private static class NotFoundHandlerMapping implements HandlerMapping {

		@SuppressWarnings("ThrowableInstanceNeverThrown")
		private static final Exception HANDLER_NOT_FOUND_EXCEPTION = new HandlerNotFoundException();


		@Override
		public Mono<Object> getHandler(WebServerExchange exchange) {
			return Mono.error(HANDLER_NOT_FOUND_EXCEPTION);
		}
	}

}