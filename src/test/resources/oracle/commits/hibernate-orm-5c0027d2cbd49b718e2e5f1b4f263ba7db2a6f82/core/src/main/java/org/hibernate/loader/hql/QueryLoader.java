// $Id: QueryLoader.java 11116 2007-01-30 14:30:55Z steve.ebersole@jboss.com $
package org.hibernate.loader.hql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.QueryException;
import org.hibernate.ScrollableResults;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.EventSource;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.hql.HolderInstantiator;
import org.hibernate.hql.ast.QueryTranslatorImpl;
import org.hibernate.hql.ast.tree.FromElement;
import org.hibernate.hql.ast.tree.SelectClause;
import org.hibernate.hql.ast.tree.QueryNode;
import org.hibernate.impl.IteratorImpl;
import org.hibernate.loader.BasicLoader;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;

/**
 * A delegate that implements the Loader part of QueryTranslator.
 *
 * @author josh
 */
public class QueryLoader extends BasicLoader {

	/**
	 * The query translator that is delegating to this object.
	 */
	private QueryTranslatorImpl queryTranslator;

	private Queryable[] entityPersisters;
	private String[] entityAliases;
	private String[] sqlAliases;
	private String[] sqlAliasSuffixes;
	private boolean[] includeInSelect;

	private String[] collectionSuffixes;

	private boolean hasScalars;
	private String[][] scalarColumnNames;
	//private Type[] sqlResultTypes;
	private Type[] queryReturnTypes;

	private final Map sqlAliasByEntityAlias = new HashMap(8);

	private EntityType[] ownerAssociationTypes;
	private int[] owners;
	private boolean[] entityEagerPropertyFetches;

	private int[] collectionOwners;
	private QueryableCollection[] collectionPersisters;

	private int selectLength;

	private ResultTransformer selectNewTransformer;
	private String[] queryReturnAliases;

	private LockMode[] defaultLockModes;


	/**
	 * Creates a new Loader implementation.
	 *
	 * @param queryTranslator The query translator that is the delegator.
	 * @param factory The factory from which this loader is being created.
	 * @param selectClause The AST representing the select clause for loading.
	 */
	public QueryLoader(
			final QueryTranslatorImpl queryTranslator,
	        final SessionFactoryImplementor factory,
	        final SelectClause selectClause) {
		super( factory );
		this.queryTranslator = queryTranslator;
		initialize( selectClause );
		postInstantiate();
	}

	private void initialize(SelectClause selectClause) {

		List fromElementList = selectClause.getFromElementsForLoad();

		hasScalars = selectClause.isScalarSelect();
		scalarColumnNames = selectClause.getColumnNames();
		//sqlResultTypes = selectClause.getSqlResultTypes();
		queryReturnTypes = selectClause.getQueryReturnTypes();

		selectNewTransformer = HolderInstantiator.createSelectNewTransformer(
				selectClause.getConstructor(),
				selectClause.isMap(),
				selectClause.isList());
		queryReturnAliases = selectClause.getQueryReturnAliases();

		List collectionFromElements = selectClause.getCollectionFromElements();
		if ( collectionFromElements != null && collectionFromElements.size()!=0 ) {
			int length = collectionFromElements.size();
			collectionPersisters = new QueryableCollection[length];
			collectionOwners = new int[length];
			collectionSuffixes = new String[length];
			for ( int i=0; i<length; i++ ) {
				FromElement collectionFromElement = (FromElement) collectionFromElements.get(i);
				collectionPersisters[i] = collectionFromElement.getQueryableCollection();
				collectionOwners[i] = fromElementList.indexOf( collectionFromElement.getOrigin() );
//				collectionSuffixes[i] = collectionFromElement.getColumnAliasSuffix();
//				collectionSuffixes[i] = Integer.toString( i ) + "_";
				collectionSuffixes[i] = collectionFromElement.getCollectionSuffix();
			}
		}

		int size = fromElementList.size();
		entityPersisters = new Queryable[size];
		entityEagerPropertyFetches = new boolean[size];
		entityAliases = new String[size];
		sqlAliases = new String[size];
		sqlAliasSuffixes = new String[size];
		includeInSelect = new boolean[size];
		owners = new int[size];
		ownerAssociationTypes = new EntityType[size];

		for ( int i = 0; i < size; i++ ) {
			final FromElement element = ( FromElement ) fromElementList.get( i );
			entityPersisters[i] = ( Queryable ) element.getEntityPersister();

			if ( entityPersisters[i] == null ) {
				throw new IllegalStateException( "No entity persister for " + element.toString() );
			}

			entityEagerPropertyFetches[i] = element.isAllPropertyFetch();
			sqlAliases[i] = element.getTableAlias();
			entityAliases[i] = element.getClassAlias();
			sqlAliasByEntityAlias.put( entityAliases[i], sqlAliases[i] );
			// TODO should we just collect these like with the collections above?
			sqlAliasSuffixes[i] = ( size == 1 ) ? "" : Integer.toString( i ) + "_";
//			sqlAliasSuffixes[i] = element.getColumnAliasSuffix();
			includeInSelect[i] = !element.isFetch();
			if ( includeInSelect[i] ) {
				selectLength++;
			}

			owners[i] = -1; //by default
			if ( element.isFetch() ) {
				if ( element.isCollectionJoin() || element.getQueryableCollection() != null ) {
					// This is now handled earlier in this method.
				}
				else if ( element.getDataType().isEntityType() ) {
					EntityType entityType = ( EntityType ) element.getDataType();
					if ( entityType.isOneToOne() ) {
						owners[i] = fromElementList.indexOf( element.getOrigin() );
					}
					ownerAssociationTypes[i] = entityType;
				}
			}
		}

		//NONE, because its the requested lock mode, not the actual! 
		defaultLockModes = ArrayHelper.fillArray(LockMode.NONE, size);

	}

	// -- Loader implementation --

	public final void validateScrollability() throws HibernateException {
		queryTranslator.validateScrollability();
	}

	protected boolean needsFetchingScroll() {
		return queryTranslator.containsCollectionFetches();
	}

	public Loadable[] getEntityPersisters() {
		return entityPersisters;
	}

	public String[] getAliases() {
		return sqlAliases;
	}

	public String[] getSqlAliasSuffixes() {
		return sqlAliasSuffixes;
	}

	public String[] getSuffixes() {
		return getSqlAliasSuffixes();
	}

	public String[] getCollectionSuffixes() {
		return collectionSuffixes;
	}

	protected String getQueryIdentifier() {
		return queryTranslator.getQueryIdentifier();
	}

	/**
	 * The SQL query string to be called.
	 */
	protected String getSQLString() {
		return queryTranslator.getSQLString();
	}

	/**
	 * An (optional) persister for a collection to be initialized; only collection loaders
	 * return a non-null value
	 */
	protected CollectionPersister[] getCollectionPersisters() {
		return collectionPersisters;
	}

	protected int[] getCollectionOwners() {
		return collectionOwners;
	}

	protected boolean[] getEntityEagerPropertyFetches() {
		return entityEagerPropertyFetches;
	}

	/**
	 * An array of indexes of the entity that owns a one-to-one association
	 * to the entity at the given index (-1 if there is no "owner")
	 */
	protected int[] getOwners() {
		return owners;
	}

	protected EntityType[] getOwnerAssociationTypes() {
		return ownerAssociationTypes;
	}

	// -- Loader overrides --

	protected boolean isSubselectLoadingEnabled() {
		return hasSubselectLoadableCollections();
	}

	/**
	 * @param lockModes a collection of lock modes specified dynamically via the Query interface
	 */
	protected LockMode[] getLockModes(Map lockModes) {

		if ( lockModes==null || lockModes.size()==0 ) {
			return defaultLockModes;
		}
		else {
			// unfortunately this stuff can't be cached because
			// it is per-invocation, not constant for the
			// QueryTranslator instance

			LockMode[] lockModeArray = new LockMode[entityAliases.length];
			for ( int i = 0; i < entityAliases.length; i++ ) {
				LockMode lockMode = (LockMode) lockModes.get( entityAliases[i] );
				if ( lockMode == null ) {
					//NONE, because its the requested lock mode, not the actual! 
					lockMode = LockMode.NONE;
				}
				lockModeArray[i] = lockMode;
			}
			return lockModeArray;
		}
	}

	protected String applyLocks(String sql, Map lockModes, Dialect dialect) throws QueryException {
		if ( lockModes == null || lockModes.size() == 0 ) {
			return sql;
		}

		// can't cache this stuff either (per-invocation)
		// we are given a map of user-alias -> lock mode
		// create a new map of sql-alias -> lock mode
		final Map aliasedLockModes = new HashMap();
		final Map keyColumnNames = dialect.forUpdateOfColumns() ? new HashMap() : null;
		final Iterator iter = lockModes.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = ( Map.Entry ) iter.next();
			final String userAlias = ( String ) me.getKey();
			final String drivingSqlAlias = ( String ) sqlAliasByEntityAlias.get( userAlias );
			if ( drivingSqlAlias == null ) {
				throw new IllegalArgumentException( "could not locate alias to apply lock mode : " + userAlias );
			}
			// at this point we have (drivingSqlAlias) the SQL alias of the driving table
			// corresponding to the given user alias.  However, the driving table is not
			// (necessarily) the table against which we want to apply locks.  Mainly,
			// the exception case here is joined-subclass hierarchies where we instead
			// want to apply the lock against the root table (for all other strategies,
			// it just happens that driving and root are the same).
			final QueryNode select = ( QueryNode ) queryTranslator.getSqlAST();
			final Lockable drivingPersister = ( Lockable ) select.getFromClause().getFromElement( userAlias ).getQueryable();
			final String sqlAlias = drivingPersister.getRootTableAlias( drivingSqlAlias );
			aliasedLockModes.put( sqlAlias, me.getValue() );
			if ( keyColumnNames != null ) {
				keyColumnNames.put( sqlAlias, drivingPersister.getRootTableIdentifierColumnNames() );
			}
		}
		return dialect.applyLocksToSql( sql, aliasedLockModes, keyColumnNames );
	}

	protected boolean upgradeLocks() {
		return true;
	}

	private boolean hasSelectNew() {
		return selectNewTransformer!=null;
	}

	protected Object getResultColumnOrRow(Object[] row, ResultTransformer transformer, ResultSet rs, SessionImplementor session)
			throws SQLException, HibernateException {

		row = toResultRow( row );
		boolean hasTransform = hasSelectNew() || transformer!=null;
		if ( hasScalars ) {
			String[][] scalarColumns = scalarColumnNames;
			int queryCols = queryReturnTypes.length;
			if ( !hasTransform && queryCols == 1 ) {
				return queryReturnTypes[0].nullSafeGet( rs, scalarColumns[0], session, null );
			}
			else {
				row = new Object[queryCols];
				for ( int i = 0; i < queryCols; i++ ) {
					row[i] = queryReturnTypes[i].nullSafeGet( rs, scalarColumns[i], session, null );
				}
				return row;
			}
		}
		else if ( !hasTransform ) {
			return row.length == 1 ? row[0] : row;
		}
		else {
			return row;
		}

	}

	protected List getResultList(List results, ResultTransformer resultTransformer) throws QueryException {
		// meant to handle dynamic instantiation queries...
		HolderInstantiator holderInstantiator = HolderInstantiator.getHolderInstantiator(selectNewTransformer, resultTransformer, queryReturnAliases);
		if ( holderInstantiator.isRequired() ) {
			for ( int i = 0; i < results.size(); i++ ) {
				Object[] row = ( Object[] ) results.get( i );
				Object result = holderInstantiator.instantiate(row);
				results.set( i, result );
			}

			if(!hasSelectNew() && resultTransformer!=null) {
				return resultTransformer.transformList(results);
			} else {
				return results;
			}
		} else {
			return results;
		}
	}

	// --- Query translator methods ---

	public List list(
			SessionImplementor session,
			QueryParameters queryParameters) throws HibernateException {
		checkQuery( queryParameters );
		return list( session, queryParameters, queryTranslator.getQuerySpaces(), queryReturnTypes );
	}

	private void checkQuery(QueryParameters queryParameters) {
		if ( hasSelectNew() && queryParameters.getResultTransformer() != null ) {
			throw new QueryException( "ResultTransformer is not allowed for 'select new' queries." );
		}
	}

	public Iterator iterate(
			QueryParameters queryParameters,
			EventSource session) throws HibernateException {
		checkQuery( queryParameters );
		final boolean stats = session.getFactory().getStatistics().isStatisticsEnabled();
		long startTime = 0;
		if ( stats ) {
			startTime = System.currentTimeMillis();
		}

		try {

			final PreparedStatement st = prepareQueryStatement( queryParameters, false, session );

			if(queryParameters.isCallable()) {
				throw new QueryException("iterate() not supported for callable statements");
			}
			final ResultSet rs = getResultSet(st, queryParameters.hasAutoDiscoverScalarTypes(), false, queryParameters.getRowSelection(), session);
			final Iterator result = new IteratorImpl(
					rs,
			        st,
			        session,
			        queryReturnTypes,
			        queryTranslator.getColumnNames(),
			        HolderInstantiator.getHolderInstantiator(selectNewTransformer, queryParameters.getResultTransformer(), queryReturnAliases)
				);

			if ( stats ) {
				session.getFactory().getStatisticsImplementor().queryExecuted(
//						"HQL: " + queryTranslator.getQueryString(),
						getQueryIdentifier(),
						0,
						System.currentTimeMillis() - startTime
				);
			}

			return result;

		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
					getFactory().getSQLExceptionConverter(),
			        sqle,
			        "could not execute query using iterate",
			        getSQLString()
				);
		}

	}

	public ScrollableResults scroll(
			final QueryParameters queryParameters,
	        final SessionImplementor session) throws HibernateException {
		checkQuery( queryParameters );
		return scroll( queryParameters, queryReturnTypes, HolderInstantiator.getHolderInstantiator(selectNewTransformer, queryParameters.getResultTransformer(), queryReturnAliases), session );
	}

	// -- Implementation private methods --

	private Object[] toResultRow(Object[] row) {
		if ( selectLength == row.length ) {
			return row;
		}
		else {
			Object[] result = new Object[selectLength];
			int j = 0;
			for ( int i = 0; i < row.length; i++ ) {
				if ( includeInSelect[i] ) {
					result[j++] = row[i];
				}
			}
			return result;
		}
	}

	/**
	 * Returns the locations of all occurrences of the named parameter.
	 */
	public int[] getNamedParameterLocs(String name) throws QueryException {
		return queryTranslator.getParameterTranslations().getNamedParameterSqlLocations( name );
	}

	/**
	 * We specifically override this method here, because in general we know much more
	 * about the parameters and their appropriate bind positions here then we do in
	 * our super because we track them explciitly here through the ParameterSpecification
	 * interface.
	 *
	 * @param queryParameters The encapsulation of the parameter values to be bound.
	 * @param startIndex The position from which to start binding parameter values.
	 * @param session The originating session.
	 * @return The number of JDBC bind positions actually bound during this method execution.
	 * @throws SQLException Indicates problems performing the binding.
	 */
	protected int bindParameterValues(
			final PreparedStatement statement,
			final QueryParameters queryParameters,
			final int startIndex,
			final SessionImplementor session) throws SQLException {
		int position = bindFilterParameterValues( statement, queryParameters, startIndex, session );
		List parameterSpecs = queryTranslator.getSqlAST().getWalker().getParameters();
		Iterator itr = parameterSpecs.iterator();
		while ( itr.hasNext() ) {
			ParameterSpecification spec = ( ParameterSpecification ) itr.next();
			position += spec.bind( statement, queryParameters, session, position );
		}
		return position - startIndex;
	}

	private int bindFilterParameterValues(
			PreparedStatement st,
			QueryParameters queryParameters,
			int position,
			SessionImplementor session) throws SQLException {
		// todo : better to handle dynamic filters through implicit DynamicFilterParameterSpecification
		// see the discussion there in DynamicFilterParameterSpecification's javadocs as to why
		// it is currently not done that way.
		int filteredParamCount = queryParameters.getFilteredPositionalParameterTypes() == null
				? 0
				: queryParameters.getFilteredPositionalParameterTypes().length;
		int nonfilteredParamCount = queryParameters.getPositionalParameterTypes() == null
				? 0
				: queryParameters.getPositionalParameterTypes().length;
		int filterParamCount = filteredParamCount - nonfilteredParamCount;
		for ( int i = 0; i < filterParamCount; i++ ) {
			Type type = queryParameters.getFilteredPositionalParameterTypes()[i];
			Object value = queryParameters.getFilteredPositionalParameterValues()[i];
			type.nullSafeSet( st, value, position, session );
			position += type.getColumnSpan( getFactory() );
		}

		return position;
	}
}
