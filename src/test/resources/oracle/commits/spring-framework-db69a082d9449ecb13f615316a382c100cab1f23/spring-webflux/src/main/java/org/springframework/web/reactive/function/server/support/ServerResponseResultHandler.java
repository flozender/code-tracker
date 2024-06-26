/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.function.server.support;

import reactor.core.publisher.Mono;

import org.springframework.util.Assert;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@code HandlerResultHandler} implementation that supports {@link ServerResponse}s.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ServerResponseResultHandler implements HandlerResultHandler {

	private final HandlerStrategies strategies;


	/**
	 * Create a {@code ServerResponseResultHandler} with default strategies.
	 */
	public ServerResponseResultHandler() {
		this(HandlerStrategies.builder().build());
	}

	/**
	 * Create a {@code ServerResponseResultHandler} with the given strategies.
	 */
	public ServerResponseResultHandler(HandlerStrategies strategies) {
		Assert.notNull(strategies, "HandlerStrategies must not be null");
		this.strategies = strategies;
	}


	@Override
	public boolean supports(HandlerResult result) {
		return (result.getReturnValue() instanceof ServerResponse);
	}

	@Override
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		ServerResponse response = (ServerResponse) result.getReturnValue();
		Assert.state(response != null, "No ServerResponse");
		return response.writeTo(exchange, this.strategies);
	}

}
