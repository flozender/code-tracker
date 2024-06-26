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

package org.springframework.boot.autoconfigure.amqp;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rabbitmq.client.Channel;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RabbitTemplate}.
 * 
 * @author Greg Turnquist
 */
@Configuration
@ConditionalOnClass({ RabbitTemplate.class, Channel.class })
@EnableConfigurationProperties(RabbitProperties.class)
public class RabbitAutoConfiguration {

	@Bean
	@ConditionalOnExpression("${spring.rabbitmq.dynamic:true}")
	@ConditionalOnMissingBean(AmqpAdmin.class)
	public AmqpAdmin amqpAdmin(CachingConnectionFactory connectionFactory) {
		return new RabbitAdmin(connectionFactory);
	}

	@Autowired
	private ConnectionFactory connectionFactory;

	@Bean
	@ConditionalOnMissingBean(RabbitTemplate.class)
	public RabbitTemplate rabbitTemplate() {
		return new RabbitTemplate(this.connectionFactory);
	}

	@Configuration
	@ConditionalOnMissingBean(ConnectionFactory.class)
	protected static class RabbitConnectionFactoryCreator {

		@Bean
		public ConnectionFactory rabbitConnectionFactory(
				RabbitProperties config) {
			CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
					config.getHost());
			connectionFactory.setPort(config.getPort());
			if (config.getUsername() != null) {
				connectionFactory.setUsername(config.getUsername());
			}
			if (config.getPassword() != null) {
				connectionFactory.setPassword(config.getPassword());
			}
			if (config.getVirtualHost() != null) {
				connectionFactory.setVirtualHost(config.getVirtualHost());
			}
			return connectionFactory;
		}
	}
}
