/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.internal.strategy;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.internal.regions.EhcacheNaturalIdRegion;
import org.hibernate.cache.spi.NaturalIdCacheKey;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * Ehcache specific read-only NaturalId region access strategy
 *
 * @author Chris Dennis
 * @author Alex Snaps
 */
public class ReadOnlyEhcacheNaturalIdRegionAccessStrategy
		extends AbstractEhcacheAccessStrategy<EhcacheNaturalIdRegion,NaturalIdCacheKey>
		implements NaturalIdRegionAccessStrategy {

	/**
	 * Create a read-only access strategy accessing the given NaturalId region.
	 *
	 * @param region THe wrapped region
	 * @param settings The Hibermate settings
	 */
	public ReadOnlyEhcacheNaturalIdRegionAccessStrategy(EhcacheNaturalIdRegion region, SessionFactoryOptions settings) {
		super( region, settings );
	}

	@Override
	public NaturalIdRegion getRegion() {
		return region();
	}

	@Override
	public Object get(NaturalIdCacheKey key, long txTimestamp) throws CacheException {
		return region().get( key );
	}

	@Override
	public boolean putFromLoad(NaturalIdCacheKey key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException {
		if ( minimalPutOverride && region().contains( key ) ) {
			return false;
		}
		else {
			region().put( key, value );
			return true;
		}
	}

	@Override
	public SoftLock lockItem(NaturalIdCacheKey key, Object version) throws UnsupportedOperationException {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * A no-op since this cache is read-only
	 */
	@Override
	public void unlockItem(NaturalIdCacheKey key, SoftLock lock) throws CacheException {
		region().remove( key );
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * This cache is asynchronous hence a no-op
	 */
	@Override
	public boolean insert(NaturalIdCacheKey key, Object value) throws CacheException {
		return false;
	}

	@Override
	public boolean afterInsert(NaturalIdCacheKey key, Object value) throws CacheException {
		region().put( key, value );
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Throws UnsupportedOperationException since this cache is read-only
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public boolean update(NaturalIdCacheKey key, Object value) throws UnsupportedOperationException {
		throw new UnsupportedOperationException( "Can't write to a readonly object" );
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Throws UnsupportedOperationException since this cache is read-only
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public boolean afterUpdate(NaturalIdCacheKey key, Object value, SoftLock lock) throws UnsupportedOperationException {
		throw new UnsupportedOperationException( "Can't write to a readonly object" );
	}
}
