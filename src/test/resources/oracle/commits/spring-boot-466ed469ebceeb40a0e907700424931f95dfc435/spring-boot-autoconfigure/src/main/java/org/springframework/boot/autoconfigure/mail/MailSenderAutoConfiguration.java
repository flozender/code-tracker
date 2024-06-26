/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.mail;

import java.util.Map;
import java.util.Properties;
import javax.activation.MimeType;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * {@link EnableAutoConfiguration Auto configuration} for email support.
 *
 * @author Oliver Gierke
 * @author Stephane Nicoll
 */
@Configuration
@ConditionalOnClass({MimeMessage.class, MimeType.class})
@ConditionalOnProperty(prefix = "spring.mail", value = "host")
@ConditionalOnMissingBean(MailSender.class)
@EnableConfigurationProperties(MailProperties.class)
public class MailSenderAutoConfiguration {

	@Autowired MailProperties properties;

	@Bean
	public JavaMailSender mailSender() {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(this.properties.getHost());
		if (this.properties.getPort() != null) {
			sender.setPort(this.properties.getPort());
		}
		sender.setUsername(this.properties.getUsername());
		sender.setPassword(this.properties.getPassword());
		sender.setDefaultEncoding(this.properties.getDefaultEncoding());
		Map<String,String> properties = this.properties.getProperties();
		if (!properties.isEmpty()) {
			Properties javaMailProperties= new Properties();
			for (Map.Entry<String, String> entry : properties.entrySet()) {
				javaMailProperties.setProperty(entry.getKey(), entry.getValue());
			}
			sender.setJavaMailProperties(javaMailProperties);
		}
		return sender;
	}

}