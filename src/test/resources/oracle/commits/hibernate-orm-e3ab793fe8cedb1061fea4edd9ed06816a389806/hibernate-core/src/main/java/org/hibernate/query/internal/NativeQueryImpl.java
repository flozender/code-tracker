/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryConstructorReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.NativeQueryImplementor;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class NativeQueryImpl extends AbstractProducedQuery<Object> implements NativeQueryImplementor {
	private final String sqlString;
	private List<NativeSQLQueryReturn> queryReturns;
	private List<NativeQueryReturnBuilder> queryReturnBuilders;
	private boolean autoDiscoverTypes;

	private Collection<String> querySpaces;

	private final boolean callable;
	private final LockOptions lockOptions = new LockOptions();
	private Serializable collectionKey;

	/**
	 * Constructs a NativeQueryImpl given a sql query defined in the mappings.
	 *
	 * @param queryDef The representation of the defined <sql-query/>.
	 * @param session The session to which this NativeQuery belongs.
	 * @param parameterMetadata Metadata about parameters found in the query.
	 */
	public NativeQueryImpl(
			NamedSQLQueryDefinition queryDef,
			SharedSessionContractImplementor session,
			ParameterMetadata parameterMetadata) {
		super( session, parameterMetadata );

		this.sqlString = queryDef.getQueryString();
		this.callable = queryDef.isCallable();
		this.querySpaces = queryDef.getQuerySpaces();

		if ( queryDef.getResultSetRef() != null ) {
			ResultSetMappingDefinition definition = session.getFactory()
					.getNamedQueryRepository()
					.getResultSetMappingDefinition( queryDef.getResultSetRef() );
			if ( definition == null ) {
				throw new MappingException(
						"Unable to find resultset-ref definition: " +
								queryDef.getResultSetRef()
				);
			}
			this.queryReturns = new ArrayList<>( Arrays.asList( definition.getQueryReturns() ) );
		}
		else if ( queryDef.getQueryReturns() != null && queryDef.getQueryReturns().length > 0 ) {
			this.queryReturns = new ArrayList<>( Arrays.asList( queryDef.getQueryReturns() ) );
		}
		else {
			this.queryReturns = new ArrayList<>();
		}
	}

	public NativeQueryImpl(
			String sqlString,
			boolean callable,
			SharedSessionContractImplementor session,
			ParameterMetadata sqlParameterMetadata) {
		super( session, sqlParameterMetadata );

		this.queryReturns = new ArrayList<>();
		this.sqlString = sqlString;
		this.callable = callable;
		this.querySpaces = new ArrayList<>();
	}

	@Override
	public NativeQuery setResultSetMapping(String name) {
		ResultSetMappingDefinition mapping = getProducer().getFactory().getNamedQueryRepository().getResultSetMappingDefinition( name );
		if ( mapping == null ) {
			throw new MappingException( "Unknown SqlResultSetMapping [" + name + "]" );
		}
		NativeSQLQueryReturn[] returns = mapping.getQueryReturns();
		queryReturns.addAll( Arrays.asList( returns ) );
		return this;
	}

	@Override
	public String getQueryString() {
		return sqlString;
	}

	@Override
	public boolean isCallable() {
		return callable;
	}

	@Override
	public List<NativeSQLQueryReturn> getQueryReturns() {
		prepareQueryReturnsIfNecessary();
		return queryReturns;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<Object> doList() {
		return getProducer().list(
				generateQuerySpecification(),
				getQueryParameters()
		);
	}

	private NativeSQLQuerySpecification generateQuerySpecification() {
		return new NativeSQLQuerySpecification(
				getQueryParameterBindings().expandListValuedParameters( getQueryString(), getProducer() ),
				queryReturns.toArray( new NativeSQLQueryReturn[queryReturns.size()] ),
				querySpaces
		);
	}

	@Override
	public QueryParameters getQueryParameters() {
		final QueryParameters queryParameters = super.getQueryParameters();
		queryParameters.setCallable( callable );
		queryParameters.setAutoDiscoverScalarTypes( autoDiscoverTypes );
		if ( collectionKey != null ) {
			queryParameters.setCollectionKeys( new Serializable[] {collectionKey} );
		}
		return queryParameters;
	}

	private void prepareQueryReturnsIfNecessary() {
		if ( queryReturnBuilders != null ) {
			if ( !queryReturnBuilders.isEmpty() ) {
				if ( queryReturns != null ) {
					queryReturns.clear();
					queryReturns = null;
				}
				queryReturns = new ArrayList<>();
				for ( NativeQueryReturnBuilder builder : queryReturnBuilders ) {
					queryReturns.add( builder.buildReturn() );
				}
				queryReturnBuilders.clear();
			}
			queryReturnBuilders = null;
		}
	}

	@Override
	protected void beforeQuery() {
		prepareQueryReturnsIfNecessary();
		boolean noReturns = queryReturns == null || queryReturns.isEmpty();
		if ( noReturns ) {
			this.autoDiscoverTypes = true;
		}
		else {
			for ( NativeSQLQueryReturn queryReturn : queryReturns ) {
				if ( queryReturn instanceof NativeSQLQueryScalarReturn ) {
					NativeSQLQueryScalarReturn scalar = (NativeSQLQueryScalarReturn) queryReturn;
					if ( scalar.getType() == null ) {
						autoDiscoverTypes = true;
						break;
					}
				}
				else if ( NativeSQLQueryConstructorReturn.class.isInstance( queryReturn ) ) {
					autoDiscoverTypes = true;
					break;
				}
			}
		}

		super.beforeQuery();

		if ( getSynchronizedQuerySpaces() != null && !getSynchronizedQuerySpaces().isEmpty() ) {
			// The application defined query spaces on the Hibernate native SQLQuery which means the query will already
			// perform a partial flush according to the defined query spaces, no need to do a full flush.
			return;
		}

		// otherwise we need to flush.  the query itself is not required to execute in a transaction; if there is
		// no transaction, the flush would throw a TransactionRequiredException which would potentially break existing
		// apps, so we only do the flush if a transaction is in progress.
		//
		// NOTE : this was added for JPA initially.  Perhaps we want to only do this from JPA usage?
		if ( getProducer().isTransactionInProgress() ) {
			getProducer().flush();
		}

	}

	protected int doExecuteUpdate() {
		return getProducer().executeNativeUpdate(
				generateQuerySpecification(),
				getQueryParameters()
		);
	}

	@Override
	public NativeQueryImplementor setCollectionKey(Serializable key) {
		this.collectionKey = key;
		return this;
	}

	@Override
	public NativeQueryImplementor addScalar(String columnAlias) {
		return addScalar( columnAlias, null );
	}

	@Override
	public NativeQueryImplementor addScalar(String columnAlias, Type type) {
		addReturnBuilder(
				new NativeQueryReturnBuilder() {
					public NativeSQLQueryReturn buildReturn() {
						return new NativeSQLQueryScalarReturn( columnAlias, type );
					}
				}
		);
		return this;
	}

	protected void addReturnBuilder(NativeQueryReturnBuilder builder) {
		if ( queryReturnBuilders == null ) {
			queryReturnBuilders = new ArrayList<>();
		}

		queryReturnBuilders.add( builder );
	}

	@Override
	public RootReturn addRoot(String tableAlias, String entityName) {
		NativeQueryReturnBuilderRootImpl builder = new NativeQueryReturnBuilderRootImpl( tableAlias, entityName );
		addReturnBuilder( builder );
		return builder;
	}

	@Override
	public RootReturn addRoot(String tableAlias, Class entityType) {
		return addRoot( tableAlias, entityType.getName() );
	}

	@Override
	public NativeQueryImplementor addEntity(String entityName) {
		return addEntity( StringHelper.unqualify( entityName ), entityName );
	}

	@Override
	public NativeQueryImplementor addEntity(String tableAlias, String entityName) {
		addRoot( tableAlias, entityName );
		return this;
	}

	@Override
	public NativeQueryImplementor addEntity(String tableAlias, String entityName, LockMode lockMode) {
		addRoot( tableAlias, entityName ).setLockMode( lockMode );
		return this;
	}

	@Override
	public NativeQueryImplementor addEntity(Class entityType) {
		return addEntity( entityType.getName() );
	}

	@Override
	public NativeQueryImplementor addEntity(String tableAlias, Class entityClass) {
		return addEntity( tableAlias, entityClass.getName() );
	}

	@Override
	public NativeQueryImplementor addEntity(String tableAlias, Class entityClass, LockMode lockMode) {
		return addEntity( tableAlias, entityClass.getName(), lockMode );
	}

	@Override
	public FetchReturn addFetch(String tableAlias, String ownerTableAlias, String joinPropertyName) {
		NativeQueryReturnBuilderFetchImpl builder = new NativeQueryReturnBuilderFetchImpl( tableAlias, ownerTableAlias, joinPropertyName );
		addReturnBuilder( builder );
		return builder;
	}

	@Override
	public NativeQueryImplementor addJoin(String tableAlias, String path) {
		createFetchJoin( tableAlias, path );
		return this;
	}

	private FetchReturn createFetchJoin(String tableAlias, String path) {
		int loc = path.indexOf( '.' );
		if ( loc < 0 ) {
			throw new QueryException( "not a property path: " + path );
		}
		final String ownerTableAlias = path.substring( 0, loc );
		final String joinedPropertyName = path.substring( loc + 1 );
		return addFetch( tableAlias, ownerTableAlias, joinedPropertyName );
	}

	@Override
	public NativeQueryImplementor addJoin(String tableAlias, String ownerTableAlias, String joinPropertyName) {
		addFetch( tableAlias, ownerTableAlias, joinPropertyName );
		return this;
	}

	@Override
	public NativeQueryImplementor addJoin(String tableAlias, String path, LockMode lockMode) {
		createFetchJoin( tableAlias, path ).setLockMode( lockMode );
		return this;
	}

	@Override
	public String[] getReturnAliases() {
		throw new UnsupportedOperationException( "Native (SQL) queries do not support returning aliases" );
	}

	@Override
	public Type[] getReturnTypes() {
		throw new UnsupportedOperationException( "Native (SQL) queries do not support returning 'return types'" );
	}

	@Override
	public NativeQuery setEntity(int position, Object val) {
		setParameter( position, val, getProducer().getFactory().getTypeHelper().entity( resolveEntityName( val ) ) );
		return this;
	}

	@Override
	public NativeQuery setEntity(String name, Object val) {
		setParameter( name, val, getProducer().getFactory().getTypeHelper().entity( resolveEntityName( val ) ) );
		return this;
	}

	@Override
	public Collection<String> getSynchronizedQuerySpaces() {
		return querySpaces;
	}

	@Override
	public NativeQueryImplementor addSynchronizedQuerySpace(String querySpace) {
		addQuerySpaces( querySpace );
		return this;
	}

	protected void addQuerySpaces(String... spaces) {
		if ( spaces != null ) {
			if ( querySpaces == null ) {
				querySpaces = new ArrayList<>();
			}
			querySpaces.addAll( Arrays.asList( (String[]) spaces ) );
		}
	}

	protected void addQuerySpaces(Serializable... spaces) {
		if ( spaces != null ) {
			if ( querySpaces == null ) {
				querySpaces = new ArrayList<>();
			}
			querySpaces.addAll( Arrays.asList( (String[]) spaces ) );
		}
	}

	@Override
	public NativeQueryImplementor addSynchronizedEntityName(String entityName) throws MappingException {
		addQuerySpaces( getProducer().getFactory().getMetamodel().entityPersister( entityName ).getQuerySpaces() );
		return this;
	}

	@Override
	public NativeQueryImplementor addSynchronizedEntityClass(Class entityClass) throws MappingException {
		addQuerySpaces( getProducer().getFactory().getMetamodel().entityPersister( entityClass.getName() ).getQuerySpaces() );
		return this;
	}

	@Override
	protected boolean isNativeQuery() {
		return true;
	}

	@Override
	public NativeQueryImplementor setHibernateFlushMode(FlushMode flushMode) {
		super.setHibernateFlushMode( flushMode );
		return this;
	}

	@Override
	public NativeQueryImplementor setFlushMode(FlushMode flushMode) {
		super.setFlushMode( flushMode );
		return this;
	}

	@Override
	public NativeQueryImplementor setFlushMode(FlushModeType flushModeType) {
		super.setFlushMode( flushModeType );
		return this;
	}

	@Override
	public NativeQueryImplementor setCacheMode(CacheMode cacheMode) {
		super.setCacheMode( cacheMode );
		return this;
	}

	@Override
	public NativeQueryImplementor setCacheable(boolean cacheable) {
		super.setCacheable( cacheable );
		return this;
	}

	@Override
	public NativeQueryImplementor setCacheRegion(String cacheRegion) {
		super.setCacheRegion( cacheRegion );
		return this;
	}

	@Override
	public NativeQueryImplementor setTimeout(int timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	public NativeQueryImplementor setFetchSize(int fetchSize) {
		super.setFetchSize( fetchSize );
		return this;
	}

	@Override
	public NativeQueryImplementor setReadOnly(boolean readOnly) {
		super.setReadOnly( readOnly );
		return this;
	}

	@Override
	public NativeQueryImplementor setLockOptions(LockOptions lockOptions) {
		super.setLockOptions( lockOptions );
		return this;
	}

	@Override
	public NativeQueryImplementor setLockMode(String alias, LockMode lockMode) {
		super.setLockMode( alias, lockMode );
		return this;
	}

	@Override
	public NativeQueryImplementor setLockMode(LockModeType lockModeType) {
		super.setLockMode( lockModeType );
		return this;
	}

	@Override
	public NativeQueryImplementor setComment(String comment) {
		super.setComment( comment );
		return this;
	}

	@Override
	public NativeQueryImplementor addQueryHint(String hint) {
		super.addQueryHint( hint );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameter(QueryParameter parameter, Object value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameter(Parameter parameter, Object value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameter(String name, Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameter(int position, Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameter(QueryParameter parameter, Object value, Type type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameter(String name, Object value, Type type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameter(int position, Object value, Type type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameter(QueryParameter parameter, Object value, TemporalType temporalType) {
		super.setParameter( parameter, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameter(String name, Object value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameter(int position, Object value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameterList(QueryParameter parameter, Collection values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameterList(String name, Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameterList(String name, Collection values, Type type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameterList(String name, Object[] values, Type type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameterList(String name, Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameter(Parameter param, Calendar value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameter(Parameter param, Date value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameter(String name, Calendar value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameter(String name, Date value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameter(int position, Calendar value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor setParameter(int position, Date value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor setResultTransformer(ResultTransformer transformer) {
		super.setResultTransformer( transformer );
		return this;
	}

	@Override
	public NativeQueryImplementor setProperties(Map map) {
		super.setProperties( map );
		return this;
	}

	@Override
	public NativeQueryImplementor setProperties(Object bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	public NativeQueryImplementor setMaxResults(int maxResult) {
		super.setMaxResults( maxResult );
		return this;
	}

	@Override
	public NativeQueryImplementor setFirstResult(int startPosition) {
		super.setFirstResult( startPosition );
		return this;
	}

	@Override
	public NativeQueryImplementor setHint(String hintName, Object value) {
		super.setHint( hintName, value );
		return this;
	}
}
