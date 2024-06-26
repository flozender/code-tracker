/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import java.io.Serializable;

import org.hibernate.cache.spi.CollectionCacheKey;
import org.hibernate.cache.spi.EntityCacheKey;
import org.hibernate.cache.spi.NaturalIdCacheKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Second level cache providers now have the option to use custom key implementations.
 * This was done as the default key implementation is very generic and is quite
 * a large object to allocate in large quantities at runtime.
 * In some extreme cases, for example when the hit ratio is very low, this was making the efficiency
 * penalty vs its benefits tradeoff questionable.
 * <p/>
 * Depending on configuration settings there might be opportunities to
 * use simpler key implementations, for example when multi-tenancy is not being used to
 * avoid the tenant identifier, or when a cache instance is entirely dedicated to a single type
 * to use the primary id only, skipping the role or entity name.
 * <p/>
 * Even with multiple types sharing the same cache, their identifiers could be of the same
 * {@link org.hibernate.type.Type}; in this case the cache container could
 * use a single type reference to implement a custom equality function without having
 * to look it up on each equality check: that's a small optimisation but the
 * equality function is often invoked extremely frequently.
 * <p/>
 * Another reason is to make it more convenient to implement custom serialization protocols when the
 * implementation supports clustering.
 *
 * @see org.hibernate.type.Type#getHashCode(Object, SessionFactoryImplementor)
 * @see org.hibernate.type.Type#isEqual(Object, Object)
 * @author Sanne Grinovero
 * @since 5.0
 */
public class DefaultCacheKeysFactory {

	public static CollectionCacheKey createCollectionKey(Serializable id, CollectionPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return new OldCacheKeyImplementation( id, persister.getKeyType(), persister.getRole(), tenantIdentifier, factory );
	}

	public static EntityCacheKey createEntityKey(Serializable id, EntityPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return new OldCacheKeyImplementation( id, persister.getIdentifierType(), persister.getRootEntityName(), tenantIdentifier, factory );
	}

	public static NaturalIdCacheKey createNaturalIdKey(Object[] naturalIdValues, EntityPersister persister, SessionImplementor session) {
		return new OldNaturalIdCacheKey( naturalIdValues, persister, session );
	}

}
