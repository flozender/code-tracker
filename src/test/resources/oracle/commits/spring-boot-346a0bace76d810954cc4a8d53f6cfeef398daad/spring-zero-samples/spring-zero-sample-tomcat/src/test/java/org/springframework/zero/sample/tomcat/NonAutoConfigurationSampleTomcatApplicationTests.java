package org.springframework.zero.sample.tomcat;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.zero.SpringApplication;
import org.springframework.zero.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.zero.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.zero.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.zero.sample.tomcat.SampleTomcatApplication;
import org.springframework.zero.sample.tomcat.service.HelloWorldService;
import org.springframework.zero.sample.tomcat.web.SampleController;

import static org.junit.Assert.assertEquals;

/**
 * Basic integration tests for demo application.
 * 
 * @author Dave Syer
 */
public class NonAutoConfigurationSampleTomcatApplicationTests {

	private static ConfigurableApplicationContext context;

	@Configuration
	@Import({ EmbeddedServletContainerAutoConfiguration.class,
			WebMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	@ComponentScan(basePackageClasses = { SampleController.class, HelloWorldService.class })
	public static class NonAutoConfigurationSampleTomcatApplication {

		public static void main(String[] args) throws Exception {
			SpringApplication.run(SampleTomcatApplication.class, args);
		}

	}

	@BeforeClass
	public static void start() throws Exception {
		Future<ConfigurableApplicationContext> future = Executors
				.newSingleThreadExecutor().submit(
						new Callable<ConfigurableApplicationContext>() {
							@Override
							public ConfigurableApplicationContext call() throws Exception {
								return (ConfigurableApplicationContext) SpringApplication
										.run(NonAutoConfigurationSampleTomcatApplication.class);
							}
						});
		context = future.get(10, TimeUnit.SECONDS);
	}

	@AfterClass
	public static void stop() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void testHome() throws Exception {
		ResponseEntity<String> entity = getRestTemplate().getForEntity(
				"http://localhost:8080", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("Hello World", entity.getBody());
	}

	private RestTemplate getRestTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
			}
		});
		return restTemplate;

	}

}
