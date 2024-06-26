/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultRegionAccess;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.QuerySpacesHelper;
import org.hibernate.cache.spi.TimestampsRegionAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.type.Type;
import org.hibernate.type.TypeHelper;

/**
 * The standard implementation of the Hibernate QueryCache interface.  Works
 * hind-in-hand with {@link TimestampsRegionAccess} to help in recognizing
 * stale query results.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class QueryResultRegionAccessImpl implements QueryResultRegionAccess {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( QueryResultRegionAccessImpl.class );

	private static final boolean DEBUGGING = LOG.isDebugEnabled();
	private static final boolean TRACING = LOG.isTraceEnabled();

	private final QueryResultsRegion cacheRegion;
	private final TimestampsRegionAccess timestampsCache;

	QueryResultRegionAccessImpl(
			QueryResultsRegion cacheRegion,
			TimestampsRegionAccess timestampsCache) {
		this.cacheRegion = cacheRegion;
		this.timestampsCache = timestampsCache;
	}

	@Override
	public QueryResultsRegion getRegion() {
		return cacheRegion;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public boolean put(
			final QueryKey key,
			final List results,
			final Type[] returnTypes,
			final SharedSessionContractImplementor session) throws HibernateException {
		if ( DEBUGGING ) {
			LOG.debugf( "Caching query results in region: %s; timestamp=%s", cacheRegion.getName(), session.getTransactionStartTimestamp() );
		}

		final List resultsCopy = CollectionHelper.arrayList( results.size() );

		final boolean isSingleResult = returnTypes.length == 1;
		for ( Object aResult : results ) {
			final Serializable resultRowForCache;
			if ( isSingleResult ) {
				resultRowForCache = returnTypes[0].disassemble( aResult, session, null );
			}
			else {
				resultRowForCache = TypeHelper.disassemble( (Object[]) aResult, returnTypes, null, session, null );
			}
			resultsCopy.add( resultRowForCache );
			if ( TRACING ) {
				logCachedResultRowDetails( returnTypes, aResult );
			}
		}

		if ( TRACING ) {
			logCachedResultDetails( key, null, returnTypes, resultsCopy );
		}

		final CacheItem cacheItem = new CacheItem(
				session.getTransactionStartTimestamp(),
				resultsCopy
		);

		try {
			session.getEventListenerManager().cachePutStart();
			cacheRegion.getAccess().addToCache( key, cacheItem );
		}
		finally {
			session.getEventListenerManager().cachePutEnd();
		}

		return true;
	}

	private static void logCachedResultDetails(QueryKey key, Set querySpaces, Type[] returnTypes, List result) {
		if ( !TRACING ) {
			return;
		}
		LOG.trace( "key.hashCode=" + key.hashCode() );
		LOG.trace( "querySpaces=" + querySpaces );
		if ( returnTypes == null || returnTypes.length == 0 ) {
			LOG.trace(
					"Unexpected returnTypes is "
							+ ( returnTypes == null ? "null" : "empty" ) + "! result"
							+ ( result == null ? " is null" : ".size()=" + result.size() )
			);
		}
		else {
			final StringBuilder returnTypeInfo = new StringBuilder();
			for ( Type returnType : returnTypes ) {
				returnTypeInfo.append( "typename=" )
						.append( returnType.getName() )
						.append( " class=" )
						.append( returnType.getReturnedClass().getName() )
						.append( ' ' );
			}
			LOG.trace( "unexpected returnTypes is " + returnTypeInfo.toString() + "! result" );
		}
	}

	@Override
	public List get(
			QueryKey key,
			Set<Serializable> spaces,
			final Type[] returnTypes,
			SharedSessionContractImplementor session) {
		return get(
				key,
				QuerySpacesHelper.INSTANCE.toStringArray( spaces ),
				returnTypes,
				session
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public List get(
			final QueryKey key,
			final String[] spaces,
			final Type[] returnTypes,
			final SharedSessionContractImplementor session) {
		if ( DEBUGGING ) {
			LOG.debugf( "Checking cached query results in region: %s", cacheRegion.getName() );
		}

		final CacheItem cacheItem = getCachedData( key, session );
		if ( cacheItem == null ) {
			if ( DEBUGGING ) {
				LOG.debug( "Query results were not found in cache" );
			}
			return null;
		}

		if ( !timestampsCache.isUpToDate( spaces, cacheItem.timestamp, session ) ) {
			if ( DEBUGGING ) {
				LOG.debug( "Cached query results were not up-to-date" );
			}
			return null;
		}

		if ( DEBUGGING ) {
			LOG.debug( "Returning cached query results" );
		}

		final boolean singleResult = returnTypes.length == 1;
		for ( int i = 0; i < cacheItem.results.size(); i++ ) {
			if ( singleResult ) {
				returnTypes[0].beforeAssemble( (Serializable) cacheItem.results.get( i ), session );
			}
			else {
				TypeHelper.beforeAssemble( (Serializable[]) cacheItem.results.get( i ), returnTypes, session );
			}
		}

		return assembleCachedResult( key, cacheItem.results, singleResult, returnTypes, session );
	}

	private CacheItem getCachedData(QueryKey key, SharedSessionContractImplementor session) {
		CacheItem cachedItem = null;
		try {
			session.getEventListenerManager().cacheGetStart();
			cachedItem = (CacheItem) cacheRegion.getAccess().getFromCache( key );
		}
		finally {
			session.getEventListenerManager().cacheGetEnd( cachedItem != null );
		}
		return cachedItem;
	}

	@SuppressWarnings("unchecked")
	private List assembleCachedResult(
			final QueryKey key,
			final List cached,
			boolean singleResult,
			final Type[] returnTypes,
			final SharedSessionContractImplementor session) throws HibernateException {

		final List result = new ArrayList( cached.size() );
		if ( singleResult ) {
			for ( Object aCached : cached ) {
				result.add( returnTypes[0].assemble( (Serializable) aCached, session, null ) );
			}
		}
		else {
			for ( int i = 0; i < cached.size(); i++ ) {
				result.add(
						TypeHelper.assemble( (Serializable[]) cached.get( i ), returnTypes, session, null )
				);
				if ( TRACING ) {
					logCachedResultRowDetails( returnTypes, result.get( i ) );
				}
			}
		}
		return result;
	}

	private static void logCachedResultRowDetails(Type[] returnTypes, Object result) {
		logCachedResultRowDetails(
				returnTypes,
				( result instanceof Object[] ? (Object[]) result : new Object[] { result } )
		);
	}

	private static void logCachedResultRowDetails(Type[] returnTypes, Object[] tuple) {
		if ( !TRACING ) {
			return;
		}
		if ( tuple == null ) {
			LOG.tracef(
					"tuple is null; returnTypes is %s",
					returnTypes == null ? "null" : "Type[" + returnTypes.length + "]"
			);
			if ( returnTypes != null && returnTypes.length > 1 ) {
				LOG.trace(
						"Unexpected result tuple! tuple is null; should be Object["
								+ returnTypes.length + "]!"
				);
			}
		}
		else {
			if ( returnTypes == null || returnTypes.length == 0 ) {
				LOG.trace(
						"Unexpected result tuple! tuple is null; returnTypes is "
								+ ( returnTypes == null ? "null" : "empty" )
				);
			}
			LOG.tracef(
					"tuple is Object[%s]; returnTypes is %s",
					tuple.length,
					returnTypes == null ? "null" : "Type[" + returnTypes.length + "]"
			);
			if ( returnTypes != null && tuple.length != returnTypes.length ) {
				LOG.trace(
						"Unexpected tuple length! transformer= expected="
								+ returnTypes.length + " got=" + tuple.length
				);
			}
			else {
				for ( int j = 0; j < tuple.length; j++ ) {
					if ( tuple[j] != null && returnTypes != null
							&& ! returnTypes[j].getReturnedClass().isInstance( tuple[j] ) ) {
						LOG.trace(
								"Unexpected tuple value type! transformer= expected="
										+ returnTypes[j].getReturnedClass().getName()
										+ " got="
										+ tuple[j].getClass().getName()
						);
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		return "QueryResultsCache(" + cacheRegion.getName() + ')';
	}

	@Override
	public void clear() throws CacheException {
		cacheRegion.getAccess().clearCache();
	}

	public static class CacheItem implements Serializable {
		private final long timestamp;
		private final List results;

		CacheItem(long timestamp, List results) {
			this.timestamp = timestamp;
			this.results = results;
		}
	}
}
