/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.internal;

import java.io.Serializable;

import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegionAccess;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.jboss.logging.Logger;

/**
 * Standard implementation of QuerySpaceStalenessStrategy
 *
 * @author Steve Ebersole
 */
public class TimestampsRegionAccessEnabledImpl implements TimestampsRegionAccess {
	private static final Logger log = Logger.getLogger( TimestampsRegionAccessEnabledImpl.class );
	private static final boolean DEBUG_ENABLED = log.isDebugEnabled();

	private final TimestampsRegion timestampsRegion;

	public TimestampsRegionAccessEnabledImpl(TimestampsRegion timestampsRegion) {
		this.timestampsRegion = timestampsRegion;
	}

	@Override
	public TimestampsRegion getRegion() {
		return timestampsRegion;
	}

	@Override
	public void preInvalidate(
			String[] spaces,
			SharedSessionContractImplementor session) {
		final SessionFactoryImplementor factory = session.getFactory();
		final RegionFactory regionFactory = factory.getCache().getRegionFactory();

		final boolean stats = factory.getStatistics().isStatisticsEnabled();

		final Long ts = regionFactory.nextTimestamp() + regionFactory.getTimeout();

		for ( Serializable space : spaces ) {
			if ( DEBUG_ENABLED ) {
				log.debugf( "Pre-invalidating space [%s], timestamp: %s", space, ts );
			}

			try {
				session.getEventListenerManager().cachePutStart();

				//put() has nowait semantics, is this really appropriate?
				//note that it needs to be async replication, never local or sync
				timestampsRegion.getAccess().addToCache( space, ts );
			}
			finally {
				session.getEventListenerManager().cachePutEnd();
			}

			if ( stats ) {
				factory.getStatistics().updateTimestampsCachePut();
			}
		}
	}

	@Override
	public void invalidate(
			String[] spaces,
			SharedSessionContractImplementor session) {
		final boolean stats = session.getFactory().getStatistics().isStatisticsEnabled();

		final Long ts = session.getFactory().getCache().getRegionFactory().nextTimestamp();

		for (Serializable space : spaces) {
			if ( DEBUG_ENABLED ) {
				log.debugf( "Invalidating space [%s], timestamp: %s", space, ts );
			}

			try {
				session.getEventListenerManager().cachePutStart();
				timestampsRegion.getAccess().addToCache( space, ts );
			}
			finally {
				session.getEventListenerManager().cachePutEnd();

				if ( stats ) {
					session.getFactory().getStatistics().updateTimestampsCachePut();
				}
			}
		}
	}

	@Override
	public boolean isUpToDate(
			String[] spaces,
			Long timestamp,
			SharedSessionContractImplementor session) {
		final boolean stats = session.getFactory().getStatistics().isStatisticsEnabled();

		for ( Serializable space : spaces ) {
			final Long lastUpdate = getLastUpdateTimestampForSpace( space, session );
			if ( lastUpdate == null ) {
				// the last update timestamp for the given space was evicted from the
				// cache or there have been no writes to it since startup
				if ( stats ) {
					session.getFactory().getStatistics().updateTimestampsCacheMiss();
				}
			}
			else {
				if ( DEBUG_ENABLED ) {
					log.debugf(
							"[%s] last update timestamp: %s",
							space,
							lastUpdate + ", result set timestamp: " + timestamp
					);
				}
				if ( stats ) {
					session.getFactory().getStatistics().updateTimestampsCacheHit();
				}
				if ( lastUpdate >= timestamp ) {
					return false;
				}
			}
		}
		return true;
	}

	private Long getLastUpdateTimestampForSpace(Serializable space, SharedSessionContractImplementor session) {
		Long ts = null;
		try {
			session.getEventListenerManager().cacheGetStart();
			ts = (Long) timestampsRegion.getAccess().getFromCache( space );
		}
		finally {
			session.getEventListenerManager().cacheGetEnd( ts != null );
		}
		return ts;
	}

}
