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
package org.springframework.web.reactive.accept;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link CompositeContentTypeResolverBuilder}.
 *
 * @author Rossen Stoyanchev
 */
public class CompositeContentTypeResolverBuilderTests {

	@Test
	public void defaultSettings() throws Exception {
		CompositeContentTypeResolver resolver = new CompositeContentTypeResolverBuilder().build();

		ServerWebExchange exchange = createExchange("/flower.gif");

		assertEquals("Should be able to resolve file extensions by default",
				Collections.singletonList(MediaType.IMAGE_GIF), resolver.resolveMediaTypes(exchange));

		exchange = createExchange("/flower.xyz");

		assertEquals("Should ignore unknown extensions by default",
				Collections.<MediaType>emptyList(), resolver.resolveMediaTypes(exchange));

		exchange = createExchange("/flower");
		exchange.getRequest().getQueryParams().add("format", "gif");

		assertEquals("Should not resolve request parameters by default",
				Collections.<MediaType>emptyList(), resolver.resolveMediaTypes(exchange));

		exchange = createExchange("/flower");
		exchange.getRequest().getHeaders().setAccept(Collections.singletonList(MediaType.IMAGE_GIF));

		assertEquals("Should resolve Accept header by default",
				Collections.singletonList(MediaType.IMAGE_GIF), resolver.resolveMediaTypes(exchange));
	}

	@Test
	public void favorPath() throws Exception {
		CompositeContentTypeResolver resolver = new CompositeContentTypeResolverBuilder()
				.favorPathExtension(true)
				.mediaType("foo", new MediaType("application", "foo"))
				.mediaType("bar", new MediaType("application", "bar"))
				.build();

		ServerWebExchange exchange = createExchange("/flower.foo");
		assertEquals(Collections.singletonList(new MediaType("application", "foo")),
				resolver.resolveMediaTypes(exchange));

		exchange = createExchange("/flower.bar");
		assertEquals(Collections.singletonList(new MediaType("application", "bar")),
				resolver.resolveMediaTypes(exchange));

		exchange = createExchange("/flower.gif");
		assertEquals(Collections.singletonList(MediaType.IMAGE_GIF), resolver.resolveMediaTypes(exchange));
	}

	@Test
	public void favorPathWithJafTurnedOff() throws Exception {
		CompositeContentTypeResolver resolver = new CompositeContentTypeResolverBuilder()
				.favorPathExtension(true)
				.useJaf(false)
				.build();

		ServerWebExchange exchange = createExchange("/flower.foo");
		assertEquals(Collections.emptyList(), resolver.resolveMediaTypes(exchange));

		exchange = createExchange("/flower.gif");
		assertEquals(Collections.emptyList(), resolver.resolveMediaTypes(exchange));
	}

	@Test(expected = HttpMediaTypeNotAcceptableException.class) // SPR-10170
	public void favorPathWithIgnoreUnknownPathExtensionTurnedOff() throws Exception {
		CompositeContentTypeResolver resolver = new CompositeContentTypeResolverBuilder()
				.favorPathExtension(true)
				.ignoreUnknownPathExtensions(false)
				.build();

		ServerWebExchange exchange = createExchange("/flower.xyz");
		exchange.getRequest().getQueryParams().add("format", "json");

		resolver.resolveMediaTypes(exchange);
	}

	@Test
	public void favorParameter() throws Exception {
		CompositeContentTypeResolver resolver = new CompositeContentTypeResolverBuilder()
				.favorParameter(true)
				.mediaType("json", MediaType.APPLICATION_JSON)
				.build();

		ServerWebExchange exchange = createExchange("/flower");
		exchange.getRequest().getQueryParams().add("format", "json");

		assertEquals(Collections.singletonList(MediaType.APPLICATION_JSON),
				resolver.resolveMediaTypes(exchange));
	}

	@Test(expected = HttpMediaTypeNotAcceptableException.class) // SPR-10170
	public void favorParameterWithUnknownMediaType() throws Exception {
		CompositeContentTypeResolver resolver = new CompositeContentTypeResolverBuilder()
				.favorParameter(true)
				.build();

		ServerWebExchange exchange = createExchange("/flower");
		exchange.getRequest().getQueryParams().add("format", "xyz");

		resolver.resolveMediaTypes(exchange);
	}

	@Test
	public void ignoreAcceptHeader() throws Exception {
		CompositeContentTypeResolver resolver = new CompositeContentTypeResolverBuilder()
				.ignoreAcceptHeader(true)
				.build();

		ServerWebExchange exchange = createExchange("/flower");
		exchange.getRequest().getHeaders().setAccept(Collections.singletonList(MediaType.IMAGE_GIF));

		assertEquals(Collections.<MediaType>emptyList(), resolver.resolveMediaTypes(exchange));
	}

	@Test // SPR-10513
	public void setDefaultContentType() throws Exception {
		CompositeContentTypeResolver resolver = new CompositeContentTypeResolverBuilder()
				.defaultContentType(MediaType.APPLICATION_JSON)
				.build();

		ServerWebExchange exchange = createExchange("/");

		assertEquals(Collections.singletonList(MediaType.APPLICATION_JSON),
				resolver.resolveMediaTypes(exchange));

		exchange.getRequest().getHeaders().setAccept(Collections.singletonList(MediaType.ALL));

		assertEquals(Collections.singletonList(MediaType.APPLICATION_JSON),
				resolver.resolveMediaTypes(exchange));
	}

	@Test // SPR-12286
	public void setDefaultContentTypeWithStrategy() throws Exception {
		CompositeContentTypeResolver resolver = new CompositeContentTypeResolverBuilder()
				.defaultContentTypeResolver(new FixedContentTypeResolver(MediaType.APPLICATION_JSON))
				.build();

		ServerWebExchange exchange = createExchange("/");

		assertEquals(Collections.singletonList(MediaType.APPLICATION_JSON),
				resolver.resolveMediaTypes(exchange));

		exchange.getRequest().getHeaders().setAccept(Collections.singletonList(MediaType.ALL));
		assertEquals(Collections.singletonList(MediaType.APPLICATION_JSON),
				resolver.resolveMediaTypes(exchange));
	}


	private ServerWebExchange createExchange(String path) throws URISyntaxException {
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI(path));
		WebSessionManager sessionManager = mock(WebSessionManager.class);
		return new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);
	}
	
}
