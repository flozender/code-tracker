/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.action.internal;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.cache.CacheException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.EventType;
import org.hibernate.event.PostCollectionRecreateEvent;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PreCollectionRecreateEvent;
import org.hibernate.event.PreCollectionRecreateEventListener;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.event.service.spi.EventListenerGroup;

public final class CollectionRecreateAction extends CollectionAction {

	public CollectionRecreateAction(
			final PersistentCollection collection,
			final CollectionPersister persister,
			final Serializable id,
			final SessionImplementor session) throws CacheException {
		super( persister, collection, id, session );
	}

	@Override
	public void execute() throws HibernateException {
		// this method is called when a new non-null collection is persisted
		// or when an existing (non-null) collection is moved to a new owner
		final PersistentCollection collection = getCollection();
		
		preRecreate();

		getPersister().recreate( collection, getKey(), getSession() );
		
		getSession().getPersistenceContext()
				.getCollectionEntry(collection)
				.afterAction(collection);
		
		evict();

		postRecreate();

		if ( getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
			getSession().getFactory().getStatisticsImplementor()
					.recreateCollection( getPersister().getRole() );
		}
	}

	private void preRecreate() {
		EventListenerGroup<PreCollectionRecreateEventListener> listenerGroup = listenerGroup( EventType.PRE_COLLECTION_RECREATE );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PreCollectionRecreateEvent event = new PreCollectionRecreateEvent( getPersister(), getCollection(), eventSource() );
		for ( PreCollectionRecreateEventListener listener : listenerGroup.listeners() ) {
			listener.onPreRecreateCollection( event );
		}
	}

	private void postRecreate() {
		EventListenerGroup<PostCollectionRecreateEventListener> listenerGroup = listenerGroup( EventType.POST_COLLECTION_RECREATE );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PostCollectionRecreateEvent event = new PostCollectionRecreateEvent( getPersister(), getCollection(), eventSource() );
		for ( PostCollectionRecreateEventListener listener : listenerGroup.listeners() ) {
			listener.onPostRecreateCollection( event );
		}
	}
}







