/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.persister.collection;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import org.hibernate.MappingException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.LoadQueryInfluencers;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.SubselectFetch;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.Expectations;
import org.hibernate.loader.collection.BatchingCollectionInitializer;
import org.hibernate.loader.collection.CollectionInitializer;
import org.hibernate.loader.collection.SubselectOneToManyLoader;
import org.hibernate.loader.entity.CollectionElementLoader;
import org.hibernate.mapping.Collection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.Update;

/**
 * Collection persister for one-to-many associations.
 *
 * @author Gavin King
 */
public class OneToManyPersister extends AbstractCollectionPersister {

	private final boolean cascadeDeleteEnabled;
	private final boolean keyIsNullable;
	private final boolean keyIsUpdateable;

	@Override
    protected boolean isRowDeleteEnabled() {
		return keyIsUpdateable && keyIsNullable;
	}

	@Override
    protected boolean isRowInsertEnabled() {
		return keyIsUpdateable;
	}

	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	public OneToManyPersister(
			Collection collection,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			Configuration cfg,
			SessionFactoryImplementor factory) throws MappingException, CacheException {
		super( collection, cacheAccessStrategy, cfg, factory );
		cascadeDeleteEnabled = collection.getKey().isCascadeDeleteEnabled() &&
				factory.getDialect().supportsCascadeDelete();
		keyIsNullable = collection.getKey().isNullable();
		keyIsUpdateable = collection.getKey().isUpdateable();
	}

	/**
	 * Generate the SQL UPDATE that updates all the foreign keys to null
	 */
	@Override
    protected String generateDeleteString() {
		
		Update update = new Update( getDialect() )
				.setTableName( qualifiedTableName )
				.addColumns( keyColumnNames, "null" )
				.addPrimaryKeyColumns( keyColumnNames );
		
		if ( hasIndex && !indexContainsFormula ) update.addColumns( indexColumnNames, "null" );
		
		if ( hasWhere ) update.setWhere( sqlWhereString );
		
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			update.setComment( "delete one-to-many " + getRole() );
		}
		
		return update.toStatementString();
	}

	/**
	 * Generate the SQL UPDATE that updates a foreign key to a value
	 */
	@Override
    protected String generateInsertRowString() {
		
		Update update = new Update( getDialect() )
				.setTableName( qualifiedTableName )
				.addColumns( keyColumnNames );
		
		if ( hasIndex && !indexContainsFormula ) update.addColumns( indexColumnNames );
		
		//identifier collections not supported for 1-to-many
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			update.setComment( "create one-to-many row " + getRole() );
		}
		
		return update.addPrimaryKeyColumns( elementColumnNames, elementColumnWriters )
				.toStatementString();
	}

	/**
	 * Not needed for one-to-many association
	 */
	@Override
    protected String generateUpdateRowString() {
		return null;
	}

	/**
	 * Generate the SQL UPDATE that updates a particular row's foreign
	 * key to null
	 */
	@Override
    protected String generateDeleteRowString() {
		
		Update update = new Update( getDialect() )
				.setTableName( qualifiedTableName )
				.addColumns( keyColumnNames, "null" );
		
		if ( hasIndex && !indexContainsFormula ) update.addColumns( indexColumnNames, "null" );
		
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			update.setComment( "delete one-to-many row " + getRole() );
		}
		
		//use a combination of foreign key columns and pk columns, since
		//the ordering of removal and addition is not guaranteed when
		//a child moves from one parent to another
		String[] rowSelectColumnNames = ArrayHelper.join( keyColumnNames, elementColumnNames );
		return update.addPrimaryKeyColumns( rowSelectColumnNames )
				.toStatementString();
	}

	public boolean consumesEntityAlias() {
		return true;
	}
	public boolean consumesCollectionAlias() {
		return true;
	}

	public boolean isOneToMany() {
		return true;
	}

	@Override
    public boolean isManyToMany() {
		return false;
	}

	private BasicBatchKey deleteRowBatchKey;
	private BasicBatchKey insertRowBatchKey;

	@Override
    protected int doUpdateRows(Serializable id, PersistentCollection collection, SessionImplementor session) {

		// we finish all the "removes" first to take care of possible unique
		// constraints and so that we can take better advantage of batching
		
		try {
			int count = 0;
			if ( isRowDeleteEnabled() ) {
				final Expectation deleteExpectation = Expectations.appropriateExpectation( getDeleteCheckStyle() );
				final boolean useBatch = deleteExpectation.canBeBatched();
				if ( useBatch && deleteRowBatchKey == null ) {
					deleteRowBatchKey = new BasicBatchKey(
							getRole() + "#DELETEROW",
							deleteExpectation
					);
				}
				final String sql = getSQLDeleteRowString();

				PreparedStatement st = null;
				// update removed rows fks to null
				try {
					int i = 0;
					Iterator entries = collection.entries( this );
					int offset = 1;
					while ( entries.hasNext() ) {
						Object entry = entries.next();
						if ( collection.needsUpdating( entry, i, elementType ) ) {  // will still be issued when it used to be null
							if ( useBatch ) {
								st = session.getTransactionCoordinator()
										.getJdbcCoordinator()
										.getBatch( deleteRowBatchKey )
										.getBatchStatement( sql, isDeleteCallable() );
							}
							else {
								st = session.getTransactionCoordinator()
										.getJdbcCoordinator()
										.getStatementPreparer()
										.prepareStatement( sql, isDeleteCallable() );
							}
							int loc = writeKey( st, id, offset, session );
							writeElementToWhere( st, collection.getSnapshotElement(entry, i), loc, session );
							if ( useBatch ) {
								session.getTransactionCoordinator()
										.getJdbcCoordinator()
										.getBatch( deleteRowBatchKey )
										.addToBatch();
							}
							else {
								deleteExpectation.verifyOutcome( st.executeUpdate(), st, -1 );
							}
							count++;
						}
						i++;
					}
				}
				catch ( SQLException e ) {
					if ( useBatch ) {
						session.getTransactionCoordinator().getJdbcCoordinator().abortBatch();
					}
					throw e;
				}
				finally {
					if ( !useBatch ) {
						st.close();
					}
				}
			}
			
			if ( isRowInsertEnabled() ) {
				final Expectation insertExpectation = Expectations.appropriateExpectation( getInsertCheckStyle() );
				boolean useBatch = insertExpectation.canBeBatched();
				boolean callable = isInsertCallable();
				if ( useBatch && insertRowBatchKey == null ) {
					insertRowBatchKey = new BasicBatchKey(
							getRole() + "#INSERTROW",
							insertExpectation
					);
				}
				final String sql = getSQLInsertRowString();

				PreparedStatement st = null;
				// now update all changed or added rows fks
				try {
					int i = 0;
					Iterator entries = collection.entries( this );
					while ( entries.hasNext() ) {
						Object entry = entries.next();
						int offset = 1;
						if ( collection.needsUpdating( entry, i, elementType ) ) {
							if ( useBatch ) {
								st = session.getTransactionCoordinator()
										.getJdbcCoordinator()
										.getBatch( insertRowBatchKey )
										.getBatchStatement( sql, callable );
							}
							else {
								st = session.getTransactionCoordinator()
										.getJdbcCoordinator()
										.getStatementPreparer()
										.prepareStatement( sql, callable );
							}

							offset += insertExpectation.prepare( st );

							int loc = writeKey( st, id, offset, session );
							if ( hasIndex && !indexContainsFormula ) {
								loc = writeIndexToWhere( st, collection.getIndex( entry, i, this ), loc, session );
							}

							writeElementToWhere( st, collection.getElement( entry ), loc, session );

							if ( useBatch ) {
								session.getTransactionCoordinator().getJdbcCoordinator().getBatch( insertRowBatchKey ).addToBatch();
							}
							else {
								insertExpectation.verifyOutcome( st.executeUpdate(), st, -1 );
							}
							count++;
						}
						i++;
					}
				}
				catch ( SQLException sqle ) {
					if ( useBatch ) {
						session.getTransactionCoordinator().getJdbcCoordinator().abortBatch();
					}
					throw sqle;
				}
				finally {
					if ( !useBatch ) {
						st.close();
					}
				}
			}

			return count;
		}
		catch ( SQLException sqle ) {
			throw getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not update collection rows: " + 
					MessageHelper.collectionInfoString( this, id, getFactory() ),
					getSQLInsertRowString()
			);
		}
	}

	public String selectFragment(
	        Joinable rhs,
	        String rhsAlias,
	        String lhsAlias,
	        String entitySuffix,
	        String collectionSuffix,
	        boolean includeCollectionColumns) {
		StringBuffer buf = new StringBuffer();
		if ( includeCollectionColumns ) {
//			buf.append( selectFragment( lhsAlias, "" ) )//ignore suffix for collection columns!
			buf.append( selectFragment( lhsAlias, collectionSuffix ) )
					.append( ", " );
		}
		OuterJoinLoadable ojl = ( OuterJoinLoadable ) getElementPersister();
		return buf.append( ojl.selectFragment( lhsAlias, entitySuffix ) )//use suffix for the entity columns
				.toString();
	}

	/**
	 * Create the <tt>OneToManyLoader</tt>
	 *
	 * @see org.hibernate.loader.collection.OneToManyLoader
	 */
	@Override
    protected CollectionInitializer createCollectionInitializer(LoadQueryInfluencers loadQueryInfluencers) 
			throws MappingException {
		return BatchingCollectionInitializer.createBatchingOneToManyInitializer( this, batchSize, getFactory(), loadQueryInfluencers );
	}

	public String fromJoinFragment(String alias,
								   boolean innerJoin,
								   boolean includeSubclasses) {
		return ( ( Joinable ) getElementPersister() ).fromJoinFragment( alias, innerJoin, includeSubclasses );
	}

	public String whereJoinFragment(String alias,
									boolean innerJoin,
									boolean includeSubclasses) {
		return ( ( Joinable ) getElementPersister() ).whereJoinFragment( alias, innerJoin, includeSubclasses );
	}

	@Override
    public String getTableName() {
		return ( ( Joinable ) getElementPersister() ).getTableName();
	}

	@Override
    public String filterFragment(String alias) throws MappingException {
		String result = super.filterFragment( alias );
		if ( getElementPersister() instanceof Joinable ) {
			result += ( ( Joinable ) getElementPersister() ).oneToManyFilterFragment( alias );
		}
		return result;

	}

	@Override
    protected CollectionInitializer createSubselectInitializer(SubselectFetch subselect, SessionImplementor session) {
		return new SubselectOneToManyLoader( 
				this,
				subselect.toSubselectString( getCollectionType().getLHSPropertyName() ),
				subselect.getResult(),
				subselect.getQueryParameters(),
				subselect.getNamedParameterLocMap(),
				session.getFactory(),
				session.getLoadQueryInfluencers()
			);
	}

	@Override
    public Object getElementByIndex(Serializable key, Object index, SessionImplementor session, Object owner) {
		return new CollectionElementLoader( this, getFactory(), session.getLoadQueryInfluencers() )
				.loadElement( session, key, incrementIndexByBase(index) );
	}

}
