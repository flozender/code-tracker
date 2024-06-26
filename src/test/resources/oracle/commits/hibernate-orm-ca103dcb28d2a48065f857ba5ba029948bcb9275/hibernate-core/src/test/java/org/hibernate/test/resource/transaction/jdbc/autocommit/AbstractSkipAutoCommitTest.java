/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.resource.transaction.jdbc.autocommit;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.test.util.ReflectionUtil;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractSkipAutoCommitTest extends BaseEntityManagerFunctionalTestCase {

	private PreparedStatementSpyConnectionProvider connectionProvider =
		new PreparedStatementSpyConnectionProvider() {
			@Override
			protected Connection actualConnection() throws SQLException {
				Connection connection = super.actualConnection();
				connection.setAutoCommit( false );
				return connection;
			}
		};

	@Override
	protected Map getConfig() {
		Map config = super.getConfig();

		config.put( AvailableSettings.DATASOURCE, dataSource() );
		config.put( AvailableSettings.SKIP_AUTOCOMMIT_CHECK, Boolean.TRUE );
		config.put( AvailableSettings.CONNECTION_PROVIDER, connectionProvider );

		return config;
	}

	protected abstract DataSource dataSource();

	@Override
	public void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			City.class,
		};
	}

	@Test
	public void test() {
		connectionProvider.clear();
		doInJPA( this::entityManagerFactory, entityManager -> {
			City city = new City();
			city.setId( 1L );
			city.setName( "Cluj-Napoca" );
			entityManager.persist( city );

			assertTrue( connectionProvider.getAcquiredConnections().isEmpty() );
			assertTrue( connectionProvider.getReleasedConnections().isEmpty() );
		} );
		verifyConnections();

		connectionProvider.clear();
		doInJPA( this::entityManagerFactory, entityManager -> {
			City city = entityManager.find( City.class, 1L );
			assertEquals( "Cluj-Napoca", city.getName() );
		} );
		verifyConnections();
	}

	private void verifyConnections() {
		assertTrue( connectionProvider.getAcquiredConnections().isEmpty() );

		List<Connection> connections = connectionProvider.getReleasedConnections();
		assertEquals( 1, connections.size() );
		Connection connection = connections.get( 0 );
		try {
			verify(connection, never()).setAutoCommit( false );
		}
		catch (SQLException e) {
			fail(e.getMessage());
		}
	}

	@Entity(name = "City" )
	public static class City {

		@Id
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
