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
package org.springframework.web.server;


import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.Mono;
import reactor.rx.Streams;
import reactor.rx.stream.Signal;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ErrorHandlingHttpHandler;
import org.springframework.web.server.HttpExceptionHandler;
import org.springframework.web.server.InternalServerErrorExceptionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class ErrorHandlingHttpHandlerTests {

	private MockServerHttpRequest request;

	private MockServerHttpResponse response;


	@Before
	public void setUp() throws Exception {
		this.request = new MockServerHttpRequest(HttpMethod.GET, new URI("http://localhost:8080"));
		this.response = new MockServerHttpResponse();
	}


	@Test
	public void handleErrorSignal() throws Exception {
		HttpExceptionHandler exceptionHandler = new InternalServerErrorExceptionHandler();
		HttpHandler targetHandler = new TestHttpHandler(new IllegalStateException("boo"));
		HttpHandler handler = new ErrorHandlingHttpHandler(targetHandler, exceptionHandler);

		handler.handle(this.request, this.response).get();

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, this.response.getStatus());
	}

	@Test
	public void handleErrorSignalWithMultipleHttpErrorHandlers() throws Exception {
		HttpExceptionHandler[] exceptionHandlers = new HttpExceptionHandler[] {
				new UnresolvedExceptionHandler(),
				new UnresolvedExceptionHandler(),
				new InternalServerErrorExceptionHandler(),
				new UnresolvedExceptionHandler()
		};
		HttpHandler targetHandler = new TestHttpHandler(new IllegalStateException("boo"));
		HttpHandler httpHandler = new ErrorHandlingHttpHandler(targetHandler, exceptionHandlers);

		httpHandler.handle(this.request, this.response).get();

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, this.response.getStatus());
	}

	@Test
	public void unresolvedException() throws Exception {
		HttpExceptionHandler exceptionHandler = new UnresolvedExceptionHandler();
		HttpHandler targetHandler = new TestHttpHandler(new IllegalStateException("boo"));
		HttpHandler httpHandler = new ErrorHandlingHttpHandler(targetHandler, exceptionHandler);

		Publisher<Void> publisher = httpHandler.handle(this.request, this.response);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals("boo", ex.getMessage());
		assertNull(this.response.getStatus());
	}

	@Test
	public void thrownExceptionBecomesErrorSignal() throws Exception {
		HttpExceptionHandler exceptionHandler = new InternalServerErrorExceptionHandler();
		HttpHandler targetHandler = new TestHttpHandler(new IllegalStateException("boo"), true);
		HttpHandler handler = new ErrorHandlingHttpHandler(targetHandler, exceptionHandler);

		handler.handle(this.request, this.response).get();

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, this.response.getStatus());
	}


	private Throwable awaitErrorSignal(Publisher<?> publisher) throws Exception {
		Signal<?> signal = Streams.from(publisher).materialize().toList().get().get(0);
		assertEquals("Unexpected signal: " + signal, Signal.Type.ERROR, signal.getType());
		return signal.getThrowable();
	}


	private static class TestHttpHandler implements HttpHandler {

		private final RuntimeException exception;

		private final boolean raise;


		public TestHttpHandler(RuntimeException exception) {
			this(exception, false);
		}

		public TestHttpHandler(RuntimeException exception, boolean raise) {
			this.exception = exception;
			this.raise = raise;
		}

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			if (this.raise) {
				throw this.exception;
			}
			return Mono.error(this.exception);
		}
	}


	/** Leave the exception unresolved. */
	private static class UnresolvedExceptionHandler implements HttpExceptionHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response, Throwable ex) {
			return Mono.error(ex);
		}
	}

}
