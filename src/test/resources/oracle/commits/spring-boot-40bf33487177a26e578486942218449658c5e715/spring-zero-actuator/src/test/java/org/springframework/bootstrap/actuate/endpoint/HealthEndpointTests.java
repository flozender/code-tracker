/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.actuate.endpoint;

import org.junit.Test;
import org.springframework.bootstrap.actuate.health.HealthIndicator;
import org.springframework.bootstrap.context.annotation.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link HealthEndpoint}.
 * 
 * @author Phillip Webb
 */
public class HealthEndpointTests extends AbstractEndpointTests<HealthEndpoint<String>> {

	public HealthEndpointTests() {
		super(Config.class, HealthEndpoint.class, "/health", false, "endpoints.health");
	}

	@Test
	public void invoke() throws Exception {
		assertThat(getEndpointBean().invoke(), equalTo("fine"));
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Config {

		@Bean
		public HealthEndpoint<String> endpoint() {
			return new HealthEndpoint<String>(new HealthIndicator<String>() {
				@Override
				public String health() {
					return "fine";
				}
			});
		}

	}
}
