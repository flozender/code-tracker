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

package org.springframework.boot.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AbstractTransactionManagementConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DataSourceTransactionManagerAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class DataSourceTransactionManagerAutoConfigurationTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testDataSourceExists() throws Exception {
		this.context.register(EmbeddedDataSourceConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
		assertNotNull(this.context.getBean(DataSourceTransactionManager.class));
		assertNotNull(
				this.context.getBean(AbstractTransactionManagementConfiguration.class));
	}

	@Test
	public void testNoDataSourceExists() throws Exception {
		this.context.register(DataSourceTransactionManagerAutoConfiguration.class);
		this.context.refresh();
		assertEquals(0, this.context.getBeanNamesForType(DataSource.class).length);
		assertEquals(0, this.context
				.getBeanNamesForType(DataSourceTransactionManager.class).length);
	}

	@Test
	public void testManualConfiguration() throws Exception {
		this.context.register(SwitchTransactionsOn.class,
				EmbeddedDataSourceConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
		assertNotNull(this.context.getBean(DataSourceTransactionManager.class));
	}

	@Test
	public void testExistingTransactionManager() {
		this.context.register(SwitchTransactionsOn.class,
				TransactionManagerConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class);
		this.context.refresh();
		assertEquals("No transaction manager should be been created", 1, this.context
				.getBeansOfType(PlatformTransactionManager.class).size());
		assertEquals("Wrong transaction manager",
				this.context.getBean("myTransactionManager"),
				this.context.getBean(PlatformTransactionManager.class));
	}

	@EnableTransactionManagement
	protected static class SwitchTransactionsOn {

	}

	@Configuration
	protected static class TransactionManagerConfiguration {

		@Bean
		public PlatformTransactionManager myTransactionManager() {
			return mock(PlatformTransactionManager.class);
		}

	}

}
