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
package org.hibernate.event.def;

import java.io.Serializable;

import org.jboss.logging.Logger;

import org.hibernate.HibernateLogger;
import org.hibernate.LockMode;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.Status;
import org.hibernate.engine.Versioning;
import org.hibernate.event.AbstractEvent;
import org.hibernate.event.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.TypeHelper;

/**
 * A convenience base class for listeners that respond to requests to reassociate an entity
 * to a session ( such as through lock() or update() ).
 *
 * @author Gavin King
 */
public class AbstractReassociateEventListener implements Serializable {

    private static final HibernateLogger LOG = Logger.getMessageLogger(HibernateLogger.class,
                                                                       AbstractReassociateEventListener.class.getName());

	/**
	 * Associates a given entity (either transient or associated with another session) to
	 * the given session.
	 *
	 * @param event The event triggering the re-association
	 * @param object The entity to be associated
	 * @param id The id of the entity.
	 * @param persister The entity's persister instance.
	 *
	 * @return An EntityEntry representing the entity within this session.
	 */
	protected final EntityEntry reassociate(AbstractEvent event, Object object, Serializable id, EntityPersister persister) {

        if (LOG.isTraceEnabled()) LOG.trace("Reassociating transient instance: "
                                            + MessageHelper.infoString(persister, id, event.getSession().getFactory()));

		final EventSource source = event.getSession();
		final EntityKey key = source.generateEntityKey( id, persister );

		source.getPersistenceContext().checkUniqueness( key, object );

		//get a snapshot
		Object[] values = persister.getPropertyValues( object, source.getEntityMode() );
		TypeHelper.deepCopy(
				values,
				persister.getPropertyTypes(),
				persister.getPropertyUpdateability(),
				values,
				source
		);
		Object version = Versioning.getVersion( values, persister );

		EntityEntry newEntry = source.getPersistenceContext().addEntity(
				object,
				( persister.isMutable() ? Status.MANAGED : Status.READ_ONLY ),
				values,
				key,
				version,
				LockMode.NONE,
				true,
				persister,
				false,
				true //will be ignored, using the existing Entry instead
		);

		new OnLockVisitor( source, id, object ).process( object, persister );

		persister.afterReassociate( object, source );

		return newEntry;

	}
}
