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

package org.springframework.boot.autoconfigure.web;

import org.eclipse.jetty.servlets.GzipFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link GzipFilter}.
 *
 * @author Andy Wilkinson
 * @since 1.2.2
 */
@Configuration
@ConditionalOnClass({ GzipFilter.class, HttpMethod.class })
@EnableConfigurationProperties(GzipFilterProperties.class)
public class GzipFilterAutoConfiguration {

	@Autowired
	private GzipFilterProperties properties;

	@Bean
	@ConditionalOnProperty(prefix = "spring.http.gzip", name = "enabled", matchIfMissing = true)
	public FilterRegistrationBean gzipFilter() {
		FilterRegistrationBean registration = new FilterRegistrationBean(
				new GzipFilter());
		registration.addUrlPatterns("/*");
		registration.setInitParameters(this.properties.getAsInitParameters());

		return registration;
	}

}
