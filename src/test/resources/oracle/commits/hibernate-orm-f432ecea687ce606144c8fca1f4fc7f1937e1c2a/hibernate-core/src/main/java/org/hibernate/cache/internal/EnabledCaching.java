/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.internal;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.PersistenceException;

import org.hibernate.HibernateException;
import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.QueryResultRegionAccess;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.TimestampsRegionAccess;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;

/**
 * @author Steve Ebersole
 * @author Strong Liu
 */
public class EnabledCaching implements CacheImplementor, DomainDataRegionBuildingContext {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( EnabledCaching.class );

	private final SessionFactoryImplementor sessionFactory;
	private final RegionFactory regionFactory;

	private final Map<String,Region> regionsByName = new ConcurrentHashMap<>();

	private final Map<NavigableRole,EntityDataAccess> entityAccessMap = new ConcurrentHashMap<>();
	private final Map<NavigableRole,NaturalIdDataAccess> naturalIdAccessMap = new ConcurrentHashMap<>();
	private final Map<NavigableRole,CollectionDataAccess> collectionAccessMap = new ConcurrentHashMap<>();

	private final TimestampsRegionAccess timestampsRegionAccess;

	private final QueryResultRegionAccess defaultQueryResultsRegionAccess;
	private final Map<String, QueryResultRegionAccess> namedQueryResultsRegionAccess = new ConcurrentHashMap<>();


	private final Set<String> legacySecondLevelCacheNames = new LinkedHashSet<>();
	private final Map<String,Set<NaturalIdDataAccess>> legacyNaturalIdAccessesForRegion = new ConcurrentHashMap<>();

	public EnabledCaching(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;

		this.regionFactory = getSessionFactory().getSessionFactoryOptions().getServiceRegistry().getService( RegionFactory.class );

		if ( getSessionFactory().getSessionFactoryOptions().isQueryCacheEnabled() ) {
			final TimestampsRegion timestampsRegion = regionFactory.buildTimestampsRegion(
					TimestampsRegion.class.getName(),
					sessionFactory
			);
			timestampsRegionAccess = sessionFactory.getSessionFactoryOptions()
					.getTimestampsRegionAccessFactory()
					.buildTimestampsRegionAccess( this, timestampsRegion );

			final QueryResultsRegion queryResultsRegion = regionFactory.buildQueryResultsRegion(
					QueryResultRegionAccessImpl.class.getName(),
					sessionFactory
			);
			regionsByName.put( queryResultsRegion.getName(), queryResultsRegion );
			defaultQueryResultsRegionAccess = new QueryResultRegionAccessImpl(
					queryResultsRegion,
					timestampsRegionAccess
			);
		}
		else {
			timestampsRegionAccess = new TimestampsRegionAccessDisabledImpl();
			defaultQueryResultsRegionAccess = null;
		}
	}

	@Override
	public void prime(Set<DomainDataRegionConfig> cacheRegionConfigs) {
		for ( DomainDataRegionConfig regionConfig : cacheRegionConfigs ) {
			final DomainDataRegion region = getRegionFactory().buildDomainDataRegion( regionConfig, this );
			regionsByName.put( region.getName(), region );

			if ( !StringTypeDescriptor.INSTANCE.areEqual( region.getName(), regionConfig.getRegionName() ) ) {
				throw new HibernateException(
						String.format(
								Locale.ROOT,
								"Region returned from RegionFactory was named differently than requested name.  Expecting `%s`, but found `%s`",
								regionConfig.getRegionName(),
								region.getName()
						)
				);
			}

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Entity caching

			for ( EntityDataCachingConfig entityAccessConfig : regionConfig.getEntityCaching() ) {
				final EntityDataAccess entityDataAccess = entityAccessMap.put(
						entityAccessConfig.getNavigableRole(),
						region.getEntityDataAccess( entityAccessConfig.getNavigableRole() )
				);

				legacySecondLevelCacheNames.add(
						StringHelper.qualifyConditionally(
								getSessionFactory().getSessionFactoryOptions().getCacheRegionPrefix(),
								region.getName()
						)
				);
			}


			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Natural-id caching

			if ( regionConfig.getNaturalIdCaching().isEmpty() ) {
				legacyNaturalIdAccessesForRegion.put( region.getName(), Collections.emptySet() );
			}
			else {
				final HashSet<NaturalIdDataAccess> accesses = new HashSet<>();

				for ( NaturalIdDataCachingConfig naturalIdAccessConfig : regionConfig.getNaturalIdCaching() ) {
					final NaturalIdDataAccess naturalIdDataAccess = naturalIdAccessMap.put(
							naturalIdAccessConfig.getNavigableRole(),
							region.getNaturalIdDataAccess( naturalIdAccessConfig.getNavigableRole() )
					);
					accesses.add( naturalIdDataAccess );
				}

				legacyNaturalIdAccessesForRegion.put( region.getName(), accesses );
			}


			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Collection caching

			for ( CollectionDataCachingConfig collectionAccessConfig : regionConfig.getCollectionCaching() ) {
				final CollectionDataAccess collectionDataAccess = collectionAccessMap.put(
						collectionAccessConfig.getNavigableRole(),
						region.getCollectionDataAccess( collectionAccessConfig.getNavigableRole() )
				);

				legacySecondLevelCacheNames.add(
						StringHelper.qualifyConditionally(
								getSessionFactory().getSessionFactoryOptions().getCacheRegionPrefix(),
								region.getName()
						)
				);
			}
		}

	}

	@Override
	public CacheKeysFactory getEnforcedCacheKeysFactory() {
		// todo (6.0) : allow configuration of this
		return null;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public RegionFactory getRegionFactory() {
		return regionFactory;
	}

	@Override
	public TimestampsRegionAccess getTimestampsRegionAccess() {
		return timestampsRegionAccess;
	}


	@Override
	public Region getRegion(String regionName) {
		return regionsByName.get( regionName );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Entity data

	@Override
	public boolean containsEntity(Class entityClass, Serializable identifier) {
		return containsEntity( entityClass.getName(), identifier );
	}

	@Override
	public boolean containsEntity(String entityName, Serializable identifier) {
		final EntityPersister entityDescriptor = sessionFactory.getMetamodel().entityPersister( entityName );
		final EntityDataAccess cacheAccess = entityDescriptor.getCacheAccessStrategy();
		if ( cacheAccess == null ) {
			return false;
		}

		final Object key = cacheAccess.generateCacheKey( identifier, entityDescriptor, sessionFactory, null );
		return cacheAccess.contains( key );
	}

	@Override
	public void evictEntityData(Class entityClass, Serializable identifier) {
		evictEntityData( entityClass.getName(), identifier );
	}

	@Override
	public void evictEntityData(String entityName, Serializable identifier) {
		final EntityPersister entityDescriptor = sessionFactory.getMetamodel().entityPersister( entityName );
		final EntityDataAccess cacheAccess = entityDescriptor.getCacheAccessStrategy();
		if ( cacheAccess == null ) {
			return;
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Evicting second-level cache: %s",
					MessageHelper.infoString( entityDescriptor, identifier, sessionFactory )
			);
		}

		final Object key = cacheAccess.generateCacheKey( identifier, entityDescriptor, sessionFactory, null );
		cacheAccess.evict( key );
	}

	@Override
	public void evictEntityData(Class entityClass) {
		evictEntityData( entityClass.getName() );
	}

	@Override
	public void evictEntityData(String entityName) {
		evictEntityData( getSessionFactory().getMetamodel().entityPersister( entityName ) );
	}

	protected void evictEntityData(EntityPersister entityDescriptor) {
		EntityPersister rootEntityDescriptor = entityDescriptor;
		if ( entityDescriptor.isInherited()
				&& ! entityDescriptor.getEntityName().equals( entityDescriptor.getRootEntityName() ) ) {
			rootEntityDescriptor = getSessionFactory().getMetamodel().entityPersister( entityDescriptor.getRootEntityName() );
		}

		evictEntityData(
				rootEntityDescriptor.getNavigableRole(),
				rootEntityDescriptor.getCacheAccessStrategy()
		);
	}

	private void evictEntityData(NavigableRole navigableRole, EntityDataAccess cacheAccess) {
		if ( cacheAccess == null ) {
			return;
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Evicting entity cache: %s", navigableRole.getFullPath() );
		}

		cacheAccess.evictAll();
	}

	@Override
	public void evictEntityData() {
		sessionFactory.getMetamodel().entityPersisters().values().forEach( this::evictEntityData );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Natural-id data

	@Override
	public void evictNaturalIdData(Class entityClass) {
		evictNaturalIdData( entityClass.getName() );
	}

	@Override
	public void evictNaturalIdData(String entityName) {
		evictNaturalIdData(
				sessionFactory.getMetamodel().entityPersister( entityName )
		);
	}

	private void evictNaturalIdData(EntityPersister rootEntityDescriptor) {
		evictNaturalIdData( rootEntityDescriptor.getNavigableRole(), rootEntityDescriptor.getNaturalIdCacheAccessStrategy() );
	}

	@Override
	public void evictNaturalIdData() {
		naturalIdAccessMap.forEach( this::evictNaturalIdData );
	}

	private void evictNaturalIdData(NavigableRole rootEntityRole, NaturalIdDataAccess cacheAccess) {
		if ( cacheAccess == null ) {
			return;
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Evicting natural-id cache: %s", rootEntityRole.getFullPath() );
		}

		cacheAccess.evictAll();
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Collection data

	@Override
	public boolean containsCollection(String role, Serializable ownerIdentifier) {
		final CollectionPersister collectionDescriptor = sessionFactory.getMetamodel()
				.collectionPersister( role );

		final CollectionDataAccess cacheAccess = collectionDescriptor.getCacheAccessStrategy();
		if ( cacheAccess == null ) {
			return false;
		}

		final Object key = cacheAccess.generateCacheKey( ownerIdentifier, collectionDescriptor, sessionFactory, null );
		return cacheAccess.contains( key );
	}

	@Override
	public void evictCollectionData(String role, Serializable ownerIdentifier) {
		final CollectionPersister collectionDescriptor = sessionFactory.getMetamodel()
				.collectionPersister( role );

		final CollectionDataAccess cacheAccess = collectionDescriptor.getCacheAccessStrategy();
		if ( cacheAccess == null ) {
			return;
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Evicting second-level cache: %s",
					MessageHelper.collectionInfoString( collectionDescriptor, ownerIdentifier, sessionFactory )
			);
		}

		final Object key = cacheAccess.generateCacheKey( ownerIdentifier, collectionDescriptor, sessionFactory, null );
		cacheAccess.evict( key );
	}

	@Override
	public void evictCollectionData(String role) {
		final CollectionPersister collectionDescriptor = sessionFactory.getMetamodel()
				.collectionPersister( role );

		evictCollectionData( collectionDescriptor );
	}

	private void evictCollectionData(CollectionPersister collectionDescriptor) {
		evictCollectionData(
				collectionDescriptor.getNavigableRole(),
				collectionDescriptor.getCacheAccessStrategy()
		);
	}

	private void evictCollectionData(NavigableRole navigableRole, CollectionDataAccess cacheAccess) {
		if ( cacheAccess == null ) {
			return;
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Evicting second-level cache: %s", navigableRole.getFullPath() );
		}
		cacheAccess.evictAll();

	}

	@Override
	public void evictCollectionData() {
		collectionAccessMap.forEach( this::evictCollectionData );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query-results data

	@Override
	public boolean containsQuery(String regionName) {
		final QueryResultRegionAccess cacheAccess = getQueryResultsRegionAccessStrictly( regionName );
		return cacheAccess != null;
	}

	@Override
	public void evictDefaultQueryRegion() {
		evictQueryResultRegion( defaultQueryResultsRegionAccess );
	}

	@Override
	public void evictQueryRegion(String regionName) {
		final QueryResultRegionAccess cacheAccess = getQueryResultsRegionAccess( regionName );
		if ( cacheAccess == null ) {
			return;
		}

		evictQueryResultRegion( cacheAccess );
	}

	private void evictQueryResultRegion(QueryResultRegionAccess cacheAccess) {
		if ( cacheAccess == null ) {
			return;
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Evicting query cache, region: %s", cacheAccess.getRegion().getName() );
		}

		cacheAccess.clear();
	}

	@Override
	public void evictQueryRegions() {
		if ( LOG.isDebugEnabled() ) {
			LOG.debug( "Evicting cache of all query regions." );
		}

		evictQueryResultRegion( defaultQueryResultsRegionAccess );

		for ( QueryResultRegionAccess cacheAccess : namedQueryResultsRegionAccess.values() ) {
			evictQueryResultRegion( cacheAccess );
		}
	}

	@Override
	public QueryResultRegionAccess getDefaultQueryResultsRegionAccess() {
		return defaultQueryResultsRegionAccess;
	}

	@Override
	public QueryResultRegionAccess getQueryResultsRegionAccess(String regionName) throws HibernateException {
		if ( !getSessionFactory().getSessionFactoryOptions().isQueryCacheEnabled() ) {
			return null;
		}


		if ( regionName == null || regionName.equals( getDefaultQueryResultsRegionAccess().getRegion().getName() ) ) {
			return getDefaultQueryResultsRegionAccess();
		}

		final QueryResultRegionAccess existing = namedQueryResultsRegionAccess.get( regionName );
		if ( existing != null ) {
			return existing;
		}

		return makeQueryResultsRegionAccess( regionName );
	}

	@Override
	public QueryResultRegionAccess getQueryResultsRegionAccessStrictly(String regionName) {
		if ( !getSessionFactory().getSessionFactoryOptions().isQueryCacheEnabled() ) {
			return null;
		}

		return namedQueryResultsRegionAccess.get( regionName );
	}

	protected QueryResultRegionAccess makeQueryResultsRegionAccess(String regionName) {
		final QueryResultsRegion region = (QueryResultsRegion) regionsByName.computeIfAbsent(
				regionName,
				this::makeQueryResultsRegion
		);
		final QueryResultRegionAccessImpl regionAccess = new QueryResultRegionAccessImpl(
				region,
				timestampsRegionAccess
		);
		namedQueryResultsRegionAccess.put( regionName, regionAccess );
		return regionAccess;
	}

	protected QueryResultsRegion makeQueryResultsRegion(String regionName) {
		// make sure there is not an existing domain-data region with that name..
		final Region existing = regionsByName.get( regionName );
		if ( existing != null ) {
			if ( !QueryResultsRegion.class.isInstance( existing ) ) {
				throw new IllegalStateException( "Cannot store both domain-data and query-result-data in the same region [" + regionName );
			}

			throw new IllegalStateException( "Illegal call to create QueryResultsRegion - one already existed" );
		}

		return regionFactory.buildQueryResultsRegion( regionName, getSessionFactory() );
	}


	@Override
	public Set<String> getCacheRegionNames() {
		return regionsByName.keySet();
	}

	@Override
	public void evictRegion(String regionName) {
		getRegion( regionName ).clear();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cls) {
		if ( org.hibernate.Cache.class.isAssignableFrom( cls ) ) {
			return (T) this;
		}

		if ( RegionFactory.class.isAssignableFrom( cls ) ) {
			return (T) regionFactory;
		}

		throw new PersistenceException( "Hibernate cannot unwrap Cache as " + cls.getName() );
	}

	@Override
	public void close() {
		for ( Region region : regionsByName.values() ) {
			region.destroy();
		}
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA-defined methods

	@Override
	public boolean contains(Class cls, Object primaryKey) {
		// JPA
		return containsEntity( cls, (Serializable) primaryKey );
	}

	@Override
	public void evict(Class cls, Object primaryKey) {
		// JPA call
		evictEntityData( cls, (Serializable) primaryKey );
	}

	@Override
	public void evict(Class cls) {
		// JPA
		evictEntityData( cls );
	}




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations

	@Override
	public EntityDataAccess getEntityRegionAccess(NavigableRole rootEntityName) {
		return entityAccessMap.get( rootEntityName );
	}

	@Override
	public NaturalIdDataAccess getNaturalIdRegionAccess(NavigableRole rootEntityName) {
		return naturalIdAccessMap.get( rootEntityName );
	}

	@Override
	public CollectionDataAccess getCollectionRegionAccess(NavigableRole collectionRole) {
		return collectionAccessMap.get( collectionRole );
	}

	@Override
	public String[] getSecondLevelCacheRegionNames() {
		return ArrayHelper.toStringArray( legacySecondLevelCacheNames );
	}


	@Override
	public Set<NaturalIdDataAccess> getNaturalIdAccessesInRegion(String regionName) {
		return legacyNaturalIdAccessesForRegion.get( regionName );
	}
}
