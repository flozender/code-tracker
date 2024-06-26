/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.multitenancy.schema;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.internal.BasicServiceRegistryImpl;
import org.hibernate.service.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.service.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.hbm2ddl.ConnectionHelper;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Steve Ebersole
 */
public class SchemaBasedMultiTenancyTest extends BaseUnitTestCase {
	private DriverManagerConnectionProviderImpl acmeProvider;
	private DriverManagerConnectionProviderImpl jbossProvider;

	private ServiceRegistryImplementor serviceRegistry;

	private SessionFactory sessionFactory;

	@Before
	public void setUp() {
		acmeProvider = ConnectionProviderBuilder.buildConnectionProvider( "acme" );
		jbossProvider = ConnectionProviderBuilder.buildConnectionProvider( "jboss" );
		AbstractMultiTenantConnectionProvider multiTenantConnectionProvider = new AbstractMultiTenantConnectionProvider() {
			@Override
			protected ConnectionProvider getAnyConnectionProvider() {
				return acmeProvider;
			}

			@Override
			protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
				if ( "acme".equals( tenantIdentifier ) ) {
					return acmeProvider;
				}
				else if ( "jboss".equals( tenantIdentifier ) ) {
					return jbossProvider;
				}
				throw new HibernateException( "Unknown tenant identifier" );
			}
		};

		Configuration cfg = new Configuration();
		cfg.getProperties().put( Environment.MULTI_TENANT, MultiTenancyStrategy.DATABASE );
		cfg.addAnnotatedClass( Customer.class );

		cfg.buildMappings();

		// do the acme export
		new SchemaExport(
				new ConnectionHelper() {
					private Connection connection;
					@Override
					public void prepare(boolean needsAutoCommit) throws SQLException {
						connection = acmeProvider.getConnection();
					}

					@Override
					public Connection getConnection() throws SQLException {
						return connection;
					}

					@Override
					public void release() throws SQLException {
						acmeProvider.closeConnection( connection );
					}
				},
				cfg.generateDropSchemaScript( ConnectionProviderBuilder.getCorrespondingDialect() ),
				cfg.generateSchemaCreationScript( ConnectionProviderBuilder.getCorrespondingDialect() )
		).execute( 		// so stupid...
				false, 	// do not script the export (write it to file)
				true, 	// do run it against the database
				false, 	// do not *just* perform the drop
				false	// do not *just* perform the create
		);

		// do the jboss export
		new SchemaExport(
				new ConnectionHelper() {
					private Connection connection;
					@Override
					public void prepare(boolean needsAutoCommit) throws SQLException {
						connection = jbossProvider.getConnection();
					}

					@Override
					public Connection getConnection() throws SQLException {
						return connection;
					}

					@Override
					public void release() throws SQLException {
						jbossProvider.closeConnection( connection );
					}
				},
				cfg.generateDropSchemaScript( ConnectionProviderBuilder.getCorrespondingDialect() ),
				cfg.generateSchemaCreationScript( ConnectionProviderBuilder.getCorrespondingDialect() )
		).execute( 		// so stupid...
				false, 	// do not script the export (write it to file)
				true, 	// do run it against the database
				false, 	// do not *just* perform the drop
				false	// do not *just* perform the create
		);

		serviceRegistry = new BasicServiceRegistryImpl( cfg.getProperties() );
		serviceRegistry.registerService( MultiTenantConnectionProvider.class, multiTenantConnectionProvider );

		sessionFactory = cfg.buildSessionFactory( serviceRegistry );
	}

	@After
	public void tearDown() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
		if ( serviceRegistry != null ) {
			serviceRegistry.destroy();
		}
		if ( jbossProvider != null ) {
			jbossProvider.stop();
		}
		if ( acmeProvider != null ) {
			acmeProvider.stop();
		}
	}

	private Session openSession() {
		return sessionFactory.openSession();
	}

	@Test
	public void testBasicExpectedBehavior() {
		Session session = openSession();
		session.setTenantIdentifier( "jboss" );
		session.beginTransaction();
		Customer steve = new Customer( "steve" );
		session.save( steve );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		try {
			session.setTenantIdentifier( "acme" );
			session.beginTransaction();
			Customer check = (Customer) session.get( Customer.class, steve.getId() );
			Assert.assertNull( "tenancy not properly isolated", check );
		}
		finally {
			session.getTransaction().commit();
			session.close();
		}

		session = openSession();
		session.setTenantIdentifier( "jboss" );
		session.beginTransaction();
		session.delete( steve );
		session.getTransaction().commit();
		session.close();
	}

}
