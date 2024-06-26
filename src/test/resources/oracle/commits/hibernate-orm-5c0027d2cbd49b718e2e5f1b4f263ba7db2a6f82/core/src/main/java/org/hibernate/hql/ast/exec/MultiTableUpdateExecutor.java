// $Id: MultiTableUpdateExecutor.java 11288 2007-03-15 11:38:45Z steve.ebersole@jboss.com $
package org.hibernate.hql.ast.exec;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.hql.ast.HqlSqlWalker;
import org.hibernate.hql.ast.tree.AssignmentSpecification;
import org.hibernate.hql.ast.tree.FromElement;
import org.hibernate.hql.ast.tree.UpdateStatement;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.Update;
import org.hibernate.util.StringHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of MultiTableUpdateExecutor.
 *
 * @author Steve Ebersole
 */
public class MultiTableUpdateExecutor extends AbstractStatementExecutor {
	private static final Logger log = LoggerFactory.getLogger( MultiTableUpdateExecutor.class );

	private final Queryable persister;
	private final String idInsertSelect;
	private final String[] updates;
	private final ParameterSpecification[][] hqlParameters;

	public MultiTableUpdateExecutor(HqlSqlWalker walker) {
		super( walker, log );

		if ( !walker.getSessionFactoryHelper().getFactory().getDialect().supportsTemporaryTables() ) {
			throw new HibernateException( "cannot perform multi-table updates using dialect not supporting temp tables" );
		}

		UpdateStatement updateStatement = ( UpdateStatement ) walker.getAST();
		FromElement fromElement = updateStatement.getFromClause().getFromElement();
		String bulkTargetAlias = fromElement.getTableAlias();
		this.persister = fromElement.getQueryable();

		this.idInsertSelect = generateIdInsertSelect( persister, bulkTargetAlias, updateStatement.getWhereClause() );
		log.trace( "Generated ID-INSERT-SELECT SQL (multi-table update) : " +  idInsertSelect );

		String[] tableNames = persister.getConstraintOrderedTableNameClosure();
		String[][] columnNames = persister.getContraintOrderedTableKeyColumnClosure();

		String idSubselect = generateIdSubselect( persister );
		List assignmentSpecifications = walker.getAssignmentSpecifications();

		updates = new String[tableNames.length];
		hqlParameters = new ParameterSpecification[tableNames.length][];
		for ( int tableIndex = 0; tableIndex < tableNames.length; tableIndex++ ) {
			boolean affected = false;
			List parameterList = new ArrayList();
			Update update = new Update( getFactory().getDialect() )
					.setTableName( tableNames[tableIndex] )
					.setWhere( "(" + StringHelper.join( ", ", columnNames[tableIndex] ) + ") IN (" + idSubselect + ")" );
			if ( getFactory().getSettings().isCommentsEnabled() ) {
				update.setComment( "bulk update" );
			}
			final Iterator itr = assignmentSpecifications.iterator();
			while ( itr.hasNext() ) {
				final AssignmentSpecification specification = ( AssignmentSpecification ) itr.next();
				if ( specification.affectsTable( tableNames[tableIndex] ) ) {
					affected = true;
					update.appendAssignmentFragment( specification.getSqlAssignmentFragment() );
					if ( specification.getParameters() != null ) {
						for ( int paramIndex = 0; paramIndex < specification.getParameters().length; paramIndex++ ) {
							parameterList.add( specification.getParameters()[paramIndex] );
						}
					}
				}
			}
			if ( affected ) {
				updates[tableIndex] = update.toStatementString();
				hqlParameters[tableIndex] = ( ParameterSpecification[] ) parameterList.toArray( new ParameterSpecification[0] );
			}
		}
	}

	public Queryable getAffectedQueryable() {
		return persister;
	}

	public String[] getSqlStatements() {
		return updates;
	}

	public int execute(QueryParameters parameters, SessionImplementor session) throws HibernateException {
		coordinateSharedCacheCleanup( session );

		createTemporaryTableIfNecessary( persister, session );

		try {
			// First, save off the pertinent ids, as the return value
			PreparedStatement ps = null;
			int resultCount = 0;
			try {
				try {
					ps = session.getBatcher().prepareStatement( idInsertSelect );
					int parameterStart = getWalker().getNumberOfParametersInSetClause();
					List allParams = getWalker().getParameters();
					Iterator whereParams = allParams.subList( parameterStart, allParams.size() ).iterator();
					int sum = 1; // jdbc params are 1-based
					while ( whereParams.hasNext() ) {
						sum += ( ( ParameterSpecification ) whereParams.next() ).bind( ps, parameters, session, sum );
					}
					resultCount = ps.executeUpdate();
				}
				finally {
					if ( ps != null ) {
						session.getBatcher().closeStatement( ps );
					}
				}
			}
			catch( SQLException e ) {
				throw JDBCExceptionHelper.convert(
						getFactory().getSQLExceptionConverter(),
				        e,
				        "could not insert/select ids for bulk update",
				        idInsertSelect
					);
			}

			// Start performing the updates
			for ( int i = 0; i < updates.length; i++ ) {
				if ( updates[i] == null ) {
					continue;
				}
				try {
					try {
						ps = session.getBatcher().prepareStatement( updates[i] );
						if ( hqlParameters[i] != null ) {
							int position = 1; // jdbc params are 1-based
							for ( int x = 0; x < hqlParameters[i].length; x++ ) {
								position += hqlParameters[i][x].bind( ps, parameters, session, position );
							}
						}
						ps.executeUpdate();
					}
					finally {
						if ( ps != null ) {
							session.getBatcher().closeStatement( ps );
						}
					}
				}
				catch( SQLException e ) {
					throw JDBCExceptionHelper.convert(
							getFactory().getSQLExceptionConverter(),
					        e,
					        "error performing bulk update",
					        updates[i]
						);
				}
			}

			return resultCount;
		}
		finally {
			dropTemporaryTableIfNecessary( persister, session );
		}
	}

	protected Queryable[] getAffectedQueryables() {
		return new Queryable[] { persister };
	}
}
