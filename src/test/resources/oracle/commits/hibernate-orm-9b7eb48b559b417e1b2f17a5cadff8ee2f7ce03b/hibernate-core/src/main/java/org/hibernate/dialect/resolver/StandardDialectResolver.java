/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.dialect.resolver;

import static org.jboss.logging.Logger.Level.WARN;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.InformixDialect;
import org.hibernate.dialect.Ingres10Dialect;
import org.hibernate.dialect.Ingres9Dialect;
import org.hibernate.dialect.IngresDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.SybaseAnywhereDialect;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * The standard Hibernate resolver.
 *
 * @author Steve Ebersole
 */
public class StandardDialectResolver extends AbstractDialectResolver{

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                StandardDialectResolver.class.getPackage().getName());

	@Override
    protected Dialect resolveDialectInternal(DatabaseMetaData metaData) throws SQLException {
		String databaseName = metaData.getDatabaseProductName();
		int databaseMajorVersion = metaData.getDatabaseMajorVersion();

		if ( "HSQL Database Engine".equals( databaseName ) ) {
			return new HSQLDialect();
		}

		if ( "H2".equals( databaseName ) ) {
			return new H2Dialect();
		}

		if ( "MySQL".equals( databaseName ) ) {
			return new MySQLDialect();
		}

		if ( "PostgreSQL".equals( databaseName ) ) {
			return new PostgreSQLDialect();
		}

		if ( "Apache Derby".equals( databaseName ) ) {
			return new DerbyDialect();
		}

		if ( "ingres".equalsIgnoreCase( databaseName ) ) {
            switch( databaseMajorVersion ) {
                case 9:
                    int databaseMinorVersion = metaData.getDatabaseMinorVersion();
                    if (databaseMinorVersion > 2) {
                        return new Ingres9Dialect();
                    }
                    return new IngresDialect();
                case 10:
                    LOG.unsupportedIngresVersion();
                    return new Ingres10Dialect();
                default:
                    LOG.unknownIngresVersion(databaseMajorVersion);
            }
			return new IngresDialect();
		}

		if ( databaseName.startsWith( "Microsoft SQL Server" ) ) {
			return new SQLServerDialect();
		}

		if ( "Sybase SQL Server".equals( databaseName ) || "Adaptive Server Enterprise".equals( databaseName ) ) {
			return new SybaseASE15Dialect();
		}

		if ( databaseName.startsWith( "Adaptive Server Anywhere" ) ) {
			return new SybaseAnywhereDialect();
		}

		if ( "Informix Dynamic Server".equals( databaseName ) ) {
			return new InformixDialect();
		}

		if ( databaseName.startsWith( "DB2/" ) ) {
			return new DB2Dialect();
		}

		if ( "Oracle".equals( databaseName ) ) {
			switch ( databaseMajorVersion ) {
				case 11:
                    LOG.unsupportedOracleVersion();
					return new Oracle10gDialect();
				case 10:
					return new Oracle10gDialect();
				case 9:
					return new Oracle9iDialect();
				case 8:
					return new Oracle8iDialect();
				default:
                    LOG.unknownOracleVersion(databaseMajorVersion);
			}
		}

		return null;
	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = WARN )
        @Message( value = "Unknown Ingres major version [%s]; using Ingres 9.2 dialect" )
        void unknownIngresVersion( int databaseMajorVersion );

        @LogMessage( level = WARN )
        @Message( value = "Unknown Oracle major version [%s]" )
        void unknownOracleVersion( int databaseMajorVersion );

        @LogMessage( level = WARN )
        @Message( value = "Ingres 10 is not yet fully supported; using Ingres 9.3 dialect" )
        void unsupportedIngresVersion();

        @LogMessage( level = WARN )
        @Message( value = "Oracle 11g is not yet fully supported; using Oracle 10g dialect" )
        void unsupportedOracleVersion();
    }
}
