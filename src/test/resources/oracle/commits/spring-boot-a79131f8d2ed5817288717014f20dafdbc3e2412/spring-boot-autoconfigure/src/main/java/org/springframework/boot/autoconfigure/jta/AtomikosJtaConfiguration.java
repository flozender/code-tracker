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

package org.springframework.boot.autoconfigure.jta;

import java.io.File;
import java.util.Properties;

import javax.jms.Message;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationHome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jta.XAConnectionFactoryWrapper;
import org.springframework.boot.jta.XADataSourceWrapper;
import org.springframework.boot.jta.atomikos.AtomikosDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.jta.atomikos.AtomikosProperties;
import org.springframework.boot.jta.atomikos.AtomikosXAConnectionFactoryWrapper;
import org.springframework.boot.jta.atomikos.AtomikosXADataSourceWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.StringUtils;

/**
 * JTA Configuration for <A href="http://www.atomikos.com/">Atomikos</a>.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.2.0
 */
@Configuration
@ConditionalOnClass({ JtaTransactionManager.class, UserTransactionManager.class })
@ConditionalOnMissingBean(PlatformTransactionManager.class)
class AtomikosJtaConfiguration {

	@Autowired
	private JtaProperties jtaProperties;

	@Bean
	@ConditionalOnMissingBean
	@ConfigurationProperties(prefix = JtaProperties.PREFIX)
	public AtomikosProperties atomikosProperties() {
		return new AtomikosProperties();
	}

	@Bean(initMethod = "init", destroyMethod = "shutdownForce")
	@ConditionalOnMissingBean
	public UserTransactionService userTransactionService(
			AtomikosProperties atomikosProperties) {
		Properties properties = new Properties();
		if (StringUtils.hasText(this.jtaProperties.getTransactionManagerId())) {
			properties.setProperty("com.atomikos.icatch.tm_unique_name",
					this.jtaProperties.getTransactionManagerId());
		}
		properties.setProperty("com.atomikos.icatch.log_base_dir", getLogBaseDir());
		properties.putAll(atomikosProperties.asProperties());
		return new UserTransactionServiceImp(properties);
	}

	private String getLogBaseDir() {
		if (StringUtils.hasLength(this.jtaProperties.getLogDir())) {
			return this.jtaProperties.getLogDir();
		}
		File home = new ApplicationHome().getDir();
		return new File(home, "transaction-logs").getAbsolutePath();
	}

	@Bean(initMethod = "init", destroyMethod = "close")
	@ConditionalOnMissingBean
	public UserTransactionManager atomikosTransactionManager(
			UserTransactionService userTransactionService) throws Exception {
		UserTransactionManager manager = new UserTransactionManager();
		manager.setStartupTransactionService(false);
		manager.setForceShutdown(true);
		return manager;
	}

	@Bean
	@ConditionalOnMissingBean
	public XADataSourceWrapper xaDataSourceWrapper() {
		return new AtomikosXADataSourceWrapper();
	}

	@Bean
	@ConditionalOnMissingBean
	public static AtomikosDependsOnBeanFactoryPostProcessor atomikosDependsOnBeanFactoryPostProcessor() {
		return new AtomikosDependsOnBeanFactoryPostProcessor();
	}

	@Bean
	public JtaTransactionManager transactionManager(UserTransaction userTransaction,
			TransactionManager transactionManager) {
		return new JtaTransactionManager(userTransaction, transactionManager);
	}

	@Configuration
	@ConditionalOnClass(Message.class)
	static class AtomikosJtaJmsConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public XAConnectionFactoryWrapper xaConnectionFactoryWrapper() {
			return new AtomikosXAConnectionFactoryWrapper();
		}

	}

}
