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

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.Versioning;
import org.hibernate.event.EventType;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PreInsertEvent;
import org.hibernate.event.PreInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.event.service.spi.EventListenerGroup;

public final class EntityInsertAction extends EntityAction {

	private Object[] state;
	private Object version;
	private Object cacheEntry;

	public EntityInsertAction(
	        Serializable id,
	        Object[] state,
	        Object instance,
	        Object version,
	        EntityPersister persister,
	        SessionImplementor session) throws HibernateException {
		super( session, id, instance, persister );
		this.state = state;
		this.version = version;
	}

	public Object[] getState() {
		return state;
	}

	@Override
	public void execute() throws HibernateException {
		EntityPersister persister = getPersister();
		SessionImplementor session = getSession();
		Object instance = getInstance();
		Serializable id = getId();

		boolean veto = preInsert();

		// Don't need to lock the cache here, since if someone
		// else inserted the same pk first, the insert would fail

		if ( !veto ) {
			
			persister.insert( id, state, instance, session );
		
			EntityEntry entry = session.getPersistenceContext().getEntry( instance );
			if ( entry == null ) {
				throw new AssertionFailure( "possible nonthreadsafe access to session" );
			}
			
			entry.postInsert();
	
			if ( persister.hasInsertGeneratedProperties() ) {
				persister.processInsertGeneratedProperties( id, instance, state, session );
				if ( persister.isVersionPropertyGenerated() ) {
					version = Versioning.getVersion(state, persister);
				}
				entry.postUpdate(instance, state, version);
			}

			getSession().getPersistenceContext().registerInsertedKey( getPersister(), getId() );
		}

		final SessionFactoryImplementor factory = getSession().getFactory();

		if ( isCachePutEnabled( persister, session ) ) {
			
			CacheEntry ce = new CacheEntry(
					state,
					persister, 
					persister.hasUninitializedLazyProperties( instance, session.getEntityMode() ),
					version,
					session,
					instance
				);
			
			cacheEntry = persister.getCacheEntryStructure().structure(ce);
			final CacheKey ck = session.generateCacheKey( id, persister.getIdentifierType(), persister.getRootEntityName() );
			boolean put = persister.getCacheAccessStrategy().insert( ck, cacheEntry, version );
			
			if ( put && factory.getStatistics().isStatisticsEnabled() ) {
				factory.getStatisticsImplementor().secondLevelCachePut( getPersister().getCacheAccessStrategy().getRegion().getName() );
			}
			
		}

		postInsert();

		if ( factory.getStatistics().isStatisticsEnabled() && !veto ) {
			factory.getStatisticsImplementor()
					.insertEntity( getPersister().getEntityName() );
		}

	}

	private void postInsert() {
		EventListenerGroup<PostInsertEventListener> listenerGroup = listenerGroup( EventType.POST_INSERT );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PostInsertEvent event = new PostInsertEvent(
				getInstance(),
				getId(),
				state,
				getPersister(),
				eventSource()
		);
		for ( PostInsertEventListener listener : listenerGroup.listeners() ) {
			listener.onPostInsert( event );
		}
	}

	private void postCommitInsert() {
		EventListenerGroup<PostInsertEventListener> listenerGroup = listenerGroup( EventType.POST_COMMIT_INSERT );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PostInsertEvent event = new PostInsertEvent(
				getInstance(),
				getId(),
				state,
				getPersister(),
				eventSource()
		);
		for ( PostInsertEventListener listener : listenerGroup.listeners() ) {
			listener.onPostInsert( event );
		}
	}

	private boolean preInsert() {
		boolean veto = false;

		EventListenerGroup<PreInsertEventListener> listenerGroup = listenerGroup( EventType.PRE_INSERT );
		if ( listenerGroup.isEmpty() ) {
			return veto;
		}
		final PreInsertEvent event = new PreInsertEvent( getInstance(), getId(), state, getPersister(), eventSource() );
		for ( PreInsertEventListener listener : listenerGroup.listeners() ) {
			veto |= listener.onPreInsert( event );
		}
		return veto;
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SessionImplementor session) throws HibernateException {
		EntityPersister persister = getPersister();
		if ( success && isCachePutEnabled( persister, getSession() ) ) {
			final CacheKey ck = getSession().generateCacheKey( getId(), persister.getIdentifierType(), persister.getRootEntityName() );
			boolean put = persister.getCacheAccessStrategy().afterInsert( ck, cacheEntry, version );
			
			if ( put && getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
				getSession().getFactory().getStatisticsImplementor()
						.secondLevelCachePut( getPersister().getCacheAccessStrategy().getRegion().getName() );
			}
		}
		postCommitInsert();
	}

	@Override
	protected boolean hasPostCommitEventListeners() {
		return ! listenerGroup( EventType.POST_COMMIT_INSERT ).isEmpty();
	}
	
	private boolean isCachePutEnabled(EntityPersister persister, SessionImplementor session) {
		return persister.hasCache()
				&& !persister.isCacheInvalidationRequired()
				&& session.getCacheMode().isPutEnabled();
	}

}
