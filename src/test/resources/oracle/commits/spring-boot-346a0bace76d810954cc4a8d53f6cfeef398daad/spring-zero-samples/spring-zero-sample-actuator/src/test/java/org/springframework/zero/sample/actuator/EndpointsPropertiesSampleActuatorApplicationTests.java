package org.springframework.zero.sample.actuator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.zero.SpringApplication;
import org.springframework.zero.sample.actuator.SampleActuatorApplication;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests for endpoints configuration.
 * 
 * @author Dave Syer
 */
public class EndpointsPropertiesSampleActuatorApplicationTests {

	private ConfigurableApplicationContext context;

	private void start(final Class<?> configuration, final String... args)
			throws Exception {
		Future<ConfigurableApplicationContext> future = Executors
				.newSingleThreadExecutor().submit(
						new Callable<ConfigurableApplicationContext>() {
							@Override
							public ConfigurableApplicationContext call() throws Exception {
								return (ConfigurableApplicationContext) SpringApplication
										.run(configuration, args);
							}
						});
		this.context = future.get(10, TimeUnit.SECONDS);
	}

	@After
	public void stop() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testCustomErrorPath() throws Exception {
		start(SampleActuatorApplication.class, "--error.path=/oops");
		testError();
	}

	private void testError() {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = getRestTemplate("user", "password").getForEntity(
				"http://localhost:8080/oops", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertEquals("None", body.get("error"));
		assertEquals(999, body.get("status"));
	}

	private RestTemplate getRestTemplate(final String username, final String password) {

		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();

		if (username != null) {

			interceptors.add(new ClientHttpRequestInterceptor() {

				@Override
				public ClientHttpResponse intercept(HttpRequest request, byte[] body,
						ClientHttpRequestExecution execution) throws IOException {
					request.getHeaders().add(
							"Authorization",
							"Basic "
									+ new String(Base64
											.encode((username + ":" + password)
													.getBytes())));
					return execution.execute(request, body);
				}
			});
		}

		RestTemplate restTemplate = new RestTemplate(
				new InterceptingClientHttpRequestFactory(
						new SimpleClientHttpRequestFactory(), interceptors));
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
			}
		});
		return restTemplate;

	}

}
