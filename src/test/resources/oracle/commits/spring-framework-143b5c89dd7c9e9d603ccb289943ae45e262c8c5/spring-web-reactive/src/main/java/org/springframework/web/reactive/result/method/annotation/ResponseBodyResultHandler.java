/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.util.List;
import java.util.Optional;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.ServerWebExchange;


/**
 * {@code HandlerResultHandler} that handles return values from methods annotated
 * with {@code @ResponseBody} writing to the body of the request or response with
 * an {@link HttpMessageWriter}.
 *
 * <p>By default the order for this result handler is set to 100. As it detects
 * the presence of {@code @ResponseBody} it should be ordered after result
 * handlers that look for a specific return type. Note however that this handler
 * does recognize and explicitly ignores the {@code ResponseEntity} return type.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ResponseBodyResultHandler extends AbstractMessageWriterResultHandler
		implements HandlerResultHandler {


	/**
	 * Constructor with {@link HttpMessageWriter}s and a
	 * {@code RequestedContentTypeResolver}.
	 *
	 * @param messageWriters writers for serializing to the response body stream
	 * @param contentTypeResolver for resolving the requested content type
	 */
	public ResponseBodyResultHandler(List<HttpMessageWriter<?>> messageWriters,
			RequestedContentTypeResolver contentTypeResolver) {

		this(messageWriters, contentTypeResolver, new ReactiveAdapterRegistry());
	}

	/**
	 * Constructor with an additional {@link ReactiveAdapterRegistry}.
	 *
	 * @param messageWriters writers for serializing to the response body stream
	 * @param contentTypeResolver for resolving the requested content type
	 * @param adapterRegistry for adapting other reactive types (e.g. rx.Observable,
	 * rx.Single, etc.) to Flux or Mono
	 */
	public ResponseBodyResultHandler(List<HttpMessageWriter<?>> messageWriters,
			RequestedContentTypeResolver contentTypeResolver,
			ReactiveAdapterRegistry adapterRegistry) {

		super(messageWriters, contentTypeResolver, adapterRegistry);
		setOrder(100);
	}


	@Override
	public boolean supports(HandlerResult result) {
		MethodParameter parameter = result.getReturnTypeSource();
		return hasResponseBodyAnnotation(parameter) && !isHttpEntityType(result);
	}

	private boolean hasResponseBodyAnnotation(MethodParameter parameter) {
		Class<?> containingClass = parameter.getContainingClass();
		return (AnnotationUtils.findAnnotation(containingClass, ResponseBody.class) != null ||
				parameter.getMethodAnnotation(ResponseBody.class) != null);
	}

	private boolean isHttpEntityType(HandlerResult result) {
		Class<?> rawClass = result.getReturnType().getRawClass();
		if (HttpEntity.class.isAssignableFrom(rawClass)) {
			return true;
		}
		else {
			Optional<Object> optional = result.getReturnValue();
			ReactiveAdapter adapter = getReactiveAdapterRegistry().getAdapterFrom(rawClass, optional);
			if (adapter != null && !adapter.getDescriptor().isNoValue()) {
				ResolvableType genericType = result.getReturnType().getGeneric(0);
				if (HttpEntity.class.isAssignableFrom(genericType.getRawClass())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		Object body = result.getReturnValue().orElse(null);
		MethodParameter bodyTypeParameter = result.getReturnTypeSource();
		return writeBody(body, bodyTypeParameter, exchange);
	}

}
