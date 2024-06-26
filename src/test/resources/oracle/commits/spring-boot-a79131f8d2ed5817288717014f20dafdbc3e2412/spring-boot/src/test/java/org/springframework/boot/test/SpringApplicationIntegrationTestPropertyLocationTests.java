/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.test;

import javax.annotation.PostConstruct;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationIntegrationTestPropertyLocationTests.MoreConfig;
import org.springframework.boot.test.SpringApplicationIntegrationTestTests.Config;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link IntegrationTest} with {@link TestPropertySource} locations.
 *
 * @author Phillip Webb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { Config.class, MoreConfig.class })
@WebAppConfiguration
@IntegrationTest({ "server.port=0", "value1=123" })
@TestPropertySource(properties = "value2=456", locations = "classpath:/test-property-source-annotation.properties")
public class SpringApplicationIntegrationTestPropertyLocationTests {

	@Autowired
	private Environment environment;

	@Test
	public void loadedProperties() throws Exception {
		assertThat(this.environment.getProperty("value1"), equalTo("123"));
		assertThat(this.environment.getProperty("value2"), equalTo("456"));
		assertThat(this.environment.getProperty("annotation-referenced"),
				equalTo("fromfile"));
	}

	@Configuration
	static class MoreConfig {

		@Value("${value1}")
		private String value1;

		@Value("${value2}")
		private String value2;

		@Value("${annotation-referenced}")
		private String annotationReferenced;

		@PostConstruct
		void checkValues() {
			assertThat(this.value1, equalTo("123"));
			assertThat(this.value2, equalTo("456"));
			assertThat(this.annotationReferenced, equalTo("fromfile"));
		}

	}

}
