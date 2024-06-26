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
package org.springframework.web.server.adapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Processors;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.server.WebServerExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.session.WebSessionManager;

/**
 * Default implementation of {@link WebServerExchange}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultWebServerExchange implements WebServerExchange {

	private final ServerHttpRequest request;

	private final ServerHttpResponse response;

	private final WebSessionManager sessionManager;


	private final Map<String, Object> attributes = new ConcurrentHashMap<>();

	private final Object createSessionLock = new Object();

	private Mono<WebSession> sessionMono;



	public DefaultWebServerExchange(ServerHttpRequest request, ServerHttpResponse response,
			WebSessionManager sessionManager) {

		Assert.notNull(request, "'request' is required.");
		Assert.notNull(response, "'response' is required.");
		Assert.notNull(response, "'sessionManager' is required.");
		this.request = request;
		this.response = response;
		this.sessionManager = sessionManager;
	}


	@Override
	public ServerHttpRequest getRequest() {
		return this.request;
	}

	@Override
	public ServerHttpResponse getResponse() {
		return this.response;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	@Override
	public Mono<WebSession> getSession() {
		if (this.sessionMono == null) {
			synchronized (this.createSessionLock) {
				if (this.sessionMono == null) {
					FluxProcessor<WebSession, WebSession> replay = Processors.replay(1);
					this.sessionMono = this.sessionManager.getSession(this).subscribeWith(replay).next();
				}
			}
		}
		return this.sessionMono;
	}

}
