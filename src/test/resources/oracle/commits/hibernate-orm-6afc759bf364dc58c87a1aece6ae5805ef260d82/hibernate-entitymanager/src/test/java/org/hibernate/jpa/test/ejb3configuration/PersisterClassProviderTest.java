/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.ejb3configuration;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.engine.internal.MutableEntityEntryFactory;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.ValueInclusion;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.jpa.test.SettingsGenerator;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.internal.PersisterClassResolverInitiator;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.EntityIdentifierDefinition;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.tuple.entity.BytecodeEnhancementMetadataNonPojoImpl;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class PersisterClassProviderTest {
	@Test
	@SuppressWarnings("unchecked")
	public void testPersisterClassProvider() {
		Map settings = SettingsGenerator.generateSettings(
				PersisterClassResolverInitiator.IMPL_NAME, GoofyPersisterClassProvider.class,
				AvailableSettings.LOADED_CLASSES, Arrays.asList( Bell.class )
		);
		try {
			EntityManagerFactory entityManagerFactory = Bootstrap.getEntityManagerFactoryBuilder(
					new PersistenceUnitDescriptorAdapter(),
					settings
			).build();
			entityManagerFactory.close();
		}
		catch ( PersistenceException e ) {
            Assert.assertNotNull( e.getCause() );
			Assert.assertNotNull( e.getCause().getCause() );
			Assert.assertEquals( GoofyException.class, e.getCause().getCause().getClass() );

		}
	}

	public static class GoofyPersisterClassProvider implements PersisterClassResolver {
		@Override
		public Class<? extends EntityPersister> getEntityPersisterClass(PersistentClass metadata) {
			return GoofyProvider.class;
		}

		@Override
		public Class<? extends CollectionPersister> getCollectionPersisterClass(Collection metadata) {
			return null;
		}
	}

	public static class GoofyProvider implements EntityPersister {

		@SuppressWarnings( {"UnusedParameters"})
		public GoofyProvider(
				org.hibernate.mapping.PersistentClass persistentClass,
				org.hibernate.cache.spi.access.EntityRegionAccessStrategy strategy,
				NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
				PersisterCreationContext creationContext) {
			throw new GoofyException();
		}

		@Override
		public EntityMode getEntityMode() {
			return null;
		}

		@Override
		public EntityTuplizer getEntityTuplizer() {
			return null;
		}

		@Override
		public BytecodeEnhancementMetadata getInstrumentationMetadata() {
			return new BytecodeEnhancementMetadataNonPojoImpl( getEntityName() );
		}

		@Override
		public void generateEntityDefinition() {
		}

		@Override
		public void postInstantiate() throws MappingException {

		}

		@Override
		public SessionFactoryImplementor getFactory() {
			return null;
		}

		@Override
		public EntityEntryFactory getEntityEntryFactory() {
			return MutableEntityEntryFactory.INSTANCE;
		}

		@Override
		public String getRootEntityName() {
			return null;
		}

		@Override
		public String getEntityName() {
			return null;
		}

		@Override
		public EntityMetamodel getEntityMetamodel() {
			return null;
		}

		@Override
		public boolean isSubclassEntityName(String entityName) {
			return false;
		}

		@Override
		public Serializable[] getPropertySpaces() {
			return new Serializable[0];
		}

		@Override
		public Serializable[] getQuerySpaces() {
			return new Serializable[0];
		}

		@Override
		public boolean hasProxy() {
			return false;
		}

		@Override
		public boolean hasCollections() {
			return false;
		}

		@Override
		public boolean hasMutableProperties() {
			return false;
		}

		@Override
		public boolean hasSubselectLoadableCollections() {
			return false;
		}

		@Override
		public boolean hasCascades() {
			return false;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public boolean isInherited() {
			return false;
		}

		@Override
		public boolean isIdentifierAssignedByInsert() {
			return false;
		}

		@Override
		public Type getPropertyType(String propertyName) throws MappingException {
			return null;
		}

		@Override
		public int[] findDirty(Object[] currentState, Object[] previousState, Object owner, SessionImplementor session) {
			return new int[0];
		}

		@Override
		public int[] findModified(Object[] old, Object[] current, Object object, SessionImplementor session) {
			return new int[0];
		}

		@Override
		public boolean hasIdentifierProperty() {
			return false;
		}

		@Override
		public boolean canExtractIdOutOfEntity() {
			return false;
		}

		@Override
		public boolean isVersioned() {
			return false;
		}

		@Override
		public Comparator getVersionComparator() {
			return null;
		}

		@Override
		public VersionType getVersionType() {
			return null;
		}

		@Override
		public int getVersionProperty() {
			return 0;
		}

		@Override
		public boolean hasNaturalIdentifier() {
			return false;
		}
		
        @Override
		public int[] getNaturalIdentifierProperties() {
			return new int[0];
		}

		@Override
		public Object[] getNaturalIdentifierSnapshot(Serializable id, SessionImplementor session) {
			return new Object[0];
		}

		@Override
		public Serializable loadEntityIdByNaturalId(Object[] naturalIdValues, LockOptions lockOptions,
				SessionImplementor session) {
			return null;
		}
		
		@Override
        public boolean hasNaturalIdCache() {
            return false;
        }

        @Override
        public NaturalIdRegionAccessStrategy getNaturalIdCacheAccessStrategy() {
            return null;
        }

        @Override
		public IdentifierGenerator getIdentifierGenerator() {
			return null;
		}

		@Override
		public boolean hasLazyProperties() {
			return false;
		}

		@Override
		public Object load(Serializable id, Object optionalObject, LockMode lockMode, SessionImplementor session) {
			return null;
		}

		@Override
		public Object load(Serializable id, Object optionalObject, LockOptions lockOptions, SessionImplementor session) {
			return null;
		}

		@Override
		public void lock(Serializable id, Object version, Object object, LockMode lockMode, SessionImplementor session) {
		}

		@Override
		public void lock(Serializable id, Object version, Object object, LockOptions lockOptions, SessionImplementor session) {
		}

		@Override
		public void insert(Serializable id, Object[] fields, Object object, SessionImplementor session) {
		}

		@Override
		public Serializable insert(Object[] fields, Object object, SessionImplementor session) {
			return null;
		}

		@Override
		public void delete(Serializable id, Object version, Object object, SessionImplementor session) {
		}

		@Override
		public void update(Serializable id, Object[] fields, int[] dirtyFields, boolean hasDirtyCollection, Object[] oldFields, Object oldVersion, Object object, Object rowId, SessionImplementor session) {
		}

		@Override
		public Type[] getPropertyTypes() {
			return new Type[0];
		}

		@Override
		public String[] getPropertyNames() {
			return new String[0];
		}

		@Override
		public boolean[] getPropertyInsertability() {
			return new boolean[0];
		}

		@Override
		public ValueInclusion[] getPropertyInsertGenerationInclusions() {
			return new ValueInclusion[0];
		}

		@Override
		public ValueInclusion[] getPropertyUpdateGenerationInclusions() {
			return new ValueInclusion[0];
		}

		@Override
		public boolean[] getPropertyUpdateability() {
			return new boolean[0];
		}

		@Override
		public boolean[] getPropertyCheckability() {
			return new boolean[0];
		}

		@Override
		public boolean[] getPropertyNullability() {
			return new boolean[0];
		}

		@Override
		public boolean[] getPropertyVersionability() {
			return new boolean[0];
		}

		@Override
		public boolean[] getPropertyLaziness() {
			return new boolean[0];
		}

		@Override
		public CascadeStyle[] getPropertyCascadeStyles() {
			return new CascadeStyle[0];
		}

		@Override
		public Type getIdentifierType() {
			return null;
		}

		@Override
		public String getIdentifierPropertyName() {
			return null;
		}

		@Override
		public boolean isCacheInvalidationRequired() {
			return false;
		}

		@Override
		public boolean isLazyPropertiesCacheable() {
			return false;
		}

		@Override
		public boolean hasCache() {
			return false;
		}

		@Override
		public EntityRegionAccessStrategy getCacheAccessStrategy() {
			return null;
		}

		@Override
		public CacheEntryStructure getCacheEntryStructure() {
			return null;
		}

		@Override
		public ClassMetadata getClassMetadata() {
			return null;
		}

		@Override
		public boolean isBatchLoadable() {
			return false;
		}

		@Override
		public boolean isSelectBeforeUpdateRequired() {
			return false;
		}

		@Override
		public Object[] getDatabaseSnapshot(Serializable id, SessionImplementor session) throws HibernateException {
			return new Object[0];
		}

		@Override
		public Serializable getIdByUniqueKey(Serializable key, String uniquePropertyName, SessionImplementor session) {
			throw new UnsupportedOperationException( "Not supported" );
		}

		@Override
		public Object getCurrentVersion(Serializable id, SessionImplementor session) throws HibernateException {
			return null;
		}

		@Override
		public Object forceVersionIncrement(Serializable id, Object currentVersion, SessionImplementor session) {
			return null;
		}

		@Override
		public boolean isInstrumented() {
			return false;
		}

		@Override
		public boolean hasInsertGeneratedProperties() {
			return false;
		}

		@Override
		public boolean hasUpdateGeneratedProperties() {
			return false;
		}

		@Override
		public boolean isVersionPropertyGenerated() {
			return false;
		}

		@Override
		public void afterInitialize(Object entity, boolean lazyPropertiesAreUnfetched, SessionImplementor session) {
		}

		@Override
		public void afterReassociate(Object entity, SessionImplementor session) {
		}

		@Override
		public Object createProxy(Serializable id, SessionImplementor session) throws HibernateException {
			return null;
		}

		@Override
		public Boolean isTransient(Object object, SessionImplementor session) throws HibernateException {
			return null;
		}

		@Override
		public Object[] getPropertyValuesToInsert(Object object, Map mergeMap, SessionImplementor session) {
			return new Object[0];
		}

		@Override
		public void processInsertGeneratedProperties(Serializable id, Object entity, Object[] state, SessionImplementor session) {
		}

		@Override
		public void processUpdateGeneratedProperties(Serializable id, Object entity, Object[] state, SessionImplementor session) {
		}

		@Override
		public Class getMappedClass() {
			return null;
		}

		@Override
		public boolean implementsLifecycle() {
			return false;
		}

		@Override
		public Class getConcreteProxyClass() {
			return null;
		}

		@Override
		public void setPropertyValues(Object object, Object[] values) {
		}

		@Override
		public void setPropertyValue(Object object, int i, Object value) {
		}

		@Override
		public Object[] getPropertyValues(Object object) {
			return new Object[0];
		}

		@Override
		public Object getPropertyValue(Object object, int i) {
			return null;
		}

		@Override
		public Object getPropertyValue(Object object, String propertyName) {
			return null;
		}

		@Override
		public Serializable getIdentifier(Object object) {
			return null;
		}

		@Override
		public Serializable getIdentifier(Object entity, SessionImplementor session) {
			return null;
		}

		@Override
		public void setIdentifier(Object entity, Serializable id, SessionImplementor session) {
		}

		@Override
		public Object getVersion(Object object) {
			return null;
		}

		@Override
		public Object instantiate(Serializable id, SessionImplementor session) {
			return null;
		}

		@Override
		public boolean isInstance(Object object) {
			return false;
		}

		@Override
		public boolean hasUninitializedLazyProperties(Object object) {
			return false;
		}

		@Override
		public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion, SessionImplementor session) {
		}

		@Override
		public EntityPersister getSubclassEntityPersister(Object instance, SessionFactoryImplementor factory) {
			return null;
		}

		@Override
		public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
			return null;
		}

		@Override
		public int[] resolveAttributeIndexes(String[] attributeNames) {
			return new int[0];
		}

		@Override
		public boolean canUseReferenceCacheEntries() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public CacheEntry buildCacheEntry(Object entity, Object[] state, Object version, SessionImplementor session) {
			return null;
		}

		@Override
		public EntityPersister getEntityPersister() {
			return this;
		}

		@Override
		public EntityIdentifierDefinition getEntityKeyDefinition() {
			return null;
		}

		@Override
		public Iterable<AttributeDefinition> getAttributes() {
			return null;
		}
	}

	public static class GoofyException extends RuntimeException {

	}
}
