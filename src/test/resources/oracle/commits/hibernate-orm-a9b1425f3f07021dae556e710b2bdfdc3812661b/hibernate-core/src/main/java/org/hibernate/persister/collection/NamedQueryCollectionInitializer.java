/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.persister.collection;

import static org.jboss.logging.Logger.Level.DEBUG;
import java.io.Serializable;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.impl.AbstractQueryImpl;
import org.hibernate.loader.collection.CollectionInitializer;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * A wrapper around a named query.
 * @author Gavin King
 */
public final class NamedQueryCollectionInitializer implements CollectionInitializer {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                NamedQueryCollectionInitializer.class.getPackage().getName());

    private final String queryName;
	private final CollectionPersister persister;

	public NamedQueryCollectionInitializer(String queryName, CollectionPersister persister) {
		super();
		this.queryName = queryName;
		this.persister = persister;
	}

	public void initialize(Serializable key, SessionImplementor session)
	throws HibernateException {

        LOG.initializingCollection(persister.getRole(), queryName);

		//TODO: is there a more elegant way than downcasting?
		AbstractQueryImpl query = (AbstractQueryImpl) session.getNamedSQLQuery(queryName);
		if ( query.getNamedParameters().length>0 ) {
			query.setParameter(
					query.getNamedParameters()[0],
					key,
					persister.getKeyType()
				);
		}
		else {
			query.setParameter( 0, key, persister.getKeyType() );
		}
		query.setCollectionKey( key )
				.setFlushMode( FlushMode.MANUAL )
				.list();

	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = DEBUG )
        @Message( value = "Initializing collection: %s using named query: %s" )
        void initializingCollection( String role,
                                     String queryName );
    }
}