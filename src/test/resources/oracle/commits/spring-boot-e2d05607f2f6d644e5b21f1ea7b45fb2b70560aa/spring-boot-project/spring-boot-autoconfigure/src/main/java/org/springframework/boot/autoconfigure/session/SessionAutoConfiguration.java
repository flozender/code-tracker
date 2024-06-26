/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Session.
 *
 * @author Andy Wilkinson
 * @author Tommy Ludwig
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @since 1.4.0
 */
@Configuration
@ConditionalOnClass(Session.class)
@ConditionalOnWebApplication
@EnableConfigurationProperties(SessionProperties.class)
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, HazelcastAutoConfiguration.class,
		JdbcTemplateAutoConfiguration.class, MongoDataAutoConfiguration.class,
		MongoReactiveDataAutoConfiguration.class, RedisAutoConfiguration.class,
		RedisReactiveAutoConfiguration.class })
@AutoConfigureBefore(HttpHandlerAutoConfiguration.class)
public class SessionAutoConfiguration {

	@Configuration
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@Import({ ServletSessionRepositoryValidator.class,
			SessionRepositoryFilterConfiguration.class })
	static class ServletSessionConfiguration {

		@Configuration
		@ConditionalOnMissingBean(SessionRepository.class)
		@Import({ ServletSessionRepositoryImplementationValidator.class,
				ServletSessionConfigurationImportSelector.class })
		static class ServletSessionRepositoryConfiguration {

		}

	}

	@Configuration
	@ConditionalOnWebApplication(type = Type.REACTIVE)
	@Import(ReactiveSessionRepositoryValidator.class)
	static class ReactiveSessionConfiguration {

		@Configuration
		@ConditionalOnMissingBean(ReactiveSessionRepository.class)
		@Import({ ReactiveSessionRepositoryImplementationValidator.class,
				ReactiveSessionConfigurationImportSelector.class })
		static class ReactiveSessionRepositoryConfiguration {

		}

	}

	/**
	 * {@link ImportSelector} base class to add {@link StoreType} configuration classes.
	 */
	abstract static class SessionConfigurationImportSelector implements ImportSelector {

		protected final String[] selectImports(AnnotationMetadata importingClassMetadata,
				WebApplicationType webApplicationType) {
			List<String> imports = new ArrayList<>();
			StoreType[] types = StoreType.values();
			for (int i = 0; i < types.length; i++) {
				imports.add(SessionStoreMappings.getConfigurationClass(webApplicationType,
						types[i]));
			}
			return imports.toArray(new String[imports.size()]);
		}

	}

	/**
	 * {@link ImportSelector} to add {@link StoreType} configuration classes for reactive
	 * web applications.
	 */
	static class ReactiveSessionConfigurationImportSelector
			extends SessionConfigurationImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return super.selectImports(importingClassMetadata,
					WebApplicationType.REACTIVE);
		}

	}

	/**
	 * {@link ImportSelector} to add {@link StoreType} configuration classes for Servlet
	 * web applications.
	 */
	static class ServletSessionConfigurationImportSelector
			extends SessionConfigurationImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return super.selectImports(importingClassMetadata,
					WebApplicationType.SERVLET);
		}

	}

	/**
	 * Base class for beans used to validate that only one supported implementation is
	 * available in the classpath when the store-type property is not set.
	 */
	abstract static class AbstractSessionRepositoryImplementationValidator {

		private final List<String> candidates;

		private final ClassLoader classLoader;

		private final SessionProperties sessionProperties;

		AbstractSessionRepositoryImplementationValidator(
				ApplicationContext applicationContext,
				SessionProperties sessionProperties, List<String> candidates) {
			this.classLoader = applicationContext.getClassLoader();
			this.sessionProperties = sessionProperties;
			this.candidates = candidates;
		}

		@PostConstruct
		public void checkAvailableImplementations() {
			List<Class<?>> availableCandidates = new ArrayList<>();
			for (String candidate : this.candidates) {
				addCandidateIfAvailable(availableCandidates, candidate);
			}
			StoreType storeType = this.sessionProperties.getStoreType();
			if (availableCandidates.size() > 1 && storeType == null) {
				throw new NonUniqueSessionRepositoryException(availableCandidates);
			}
		}

		private void addCandidateIfAvailable(List<Class<?>> candidates, String type) {
			try {
				Class<?> candidate = this.classLoader.loadClass(type);
				if (candidate != null) {
					candidates.add(candidate);
				}
			}
			catch (Throwable ex) {
				// Ignore
			}
		}
	}

	/**
	 * Bean used to validate that only one supported implementation is available in the
	 * classpath when the store-type property is not set.
	 */
	static class ServletSessionRepositoryImplementationValidator
			extends AbstractSessionRepositoryImplementationValidator {

		ServletSessionRepositoryImplementationValidator(
				ApplicationContext applicationContext,
				SessionProperties sessionProperties) {
			super(applicationContext, sessionProperties, Arrays.asList(
					"org.springframework.session.hazelcast.HazelcastSessionRepository",
					"org.springframework.session.jdbc.JdbcOperationsSessionRepository",
					"org.springframework.session.data.mongo.MongoOperationsSessionRepository",
					"org.springframework.session.data.redis.RedisOperationsSessionRepository"));
		}

	}

	/**
	 * Bean used to validate that only one supported implementation is available in the
	 * classpath when the store-type property is not set.
	 */
	static class ReactiveSessionRepositoryImplementationValidator
			extends AbstractSessionRepositoryImplementationValidator {

		ReactiveSessionRepositoryImplementationValidator(
				ApplicationContext applicationContext,
				SessionProperties sessionProperties) {
			super(applicationContext, sessionProperties, Arrays.asList(
					"org.springframework.session.data.redis.ReactiveRedisOperationsSessionRepository",
					"org.springframework.session.data.mongo.ReactiveMongoOperationsSessionRepository"));
		}

	}

	/**
	 * Base class for validating that a (reactive) session repository bean exists.
	 */
	abstract static class AbstractSessionRepositoryValidator {

		private final SessionProperties sessionProperties;

		private final ObjectProvider<?> sessionRepositoryProvider;

		protected AbstractSessionRepositoryValidator(SessionProperties sessionProperties,
				ObjectProvider<?> sessionRepositoryProvider) {
			this.sessionProperties = sessionProperties;
			this.sessionRepositoryProvider = sessionRepositoryProvider;
		}

		@PostConstruct
		public void checkSessionRepository() {
			StoreType storeType = this.sessionProperties.getStoreType();
			if (storeType != StoreType.NONE
					&& this.sessionRepositoryProvider.getIfAvailable() == null) {
				if (storeType != null) {
					throw new SessionRepositoryUnavailableException("No session "
							+ "repository could be auto-configured, check your "
							+ "configuration (session store type is '"
							+ storeType.name().toLowerCase() + "')", storeType);
				}
			}
		}

	}

	/**
	 * Bean used to validate that a {@link SessionRepository} exists and provide a
	 * meaningful message if that's not the case.
	 */
	static class ServletSessionRepositoryValidator
			extends AbstractSessionRepositoryValidator {

		ServletSessionRepositoryValidator(SessionProperties sessionProperties,
				ObjectProvider<SessionRepository<?>> sessionRepositoryProvider) {
			super(sessionProperties, sessionRepositoryProvider);
		}

	}

	/**
	 * Bean used to validate that a {@link ReactiveSessionRepository} exists and provide a
	 * meaningful message if that's not the case.
	 */
	static class ReactiveSessionRepositoryValidator
			extends AbstractSessionRepositoryValidator {

		ReactiveSessionRepositoryValidator(SessionProperties sessionProperties,
				ObjectProvider<ReactiveSessionRepository<?>> sessionRepositoryProvider) {
			super(sessionProperties, sessionRepositoryProvider);
		}

	}

}
