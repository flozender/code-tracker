/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.dialect.lock;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.SimpleSelect;

/**
 * A locking strategy where the locks are obtained through select statements.
 * <p/>
 * For non-read locks, this is achieved through the Dialect's specific
 * SELECT ... FOR UPDATE syntax.
 *
 * @see org.hibernate.dialect.Dialect#getForUpdateString(org.hibernate.LockMode)
 * @see org.hibernate.dialect.Dialect#appendLockHint(org.hibernate.LockMode, String)
 * @since 3.2
 *
 * @author Steve Ebersole
 */
public class SelectLockingStrategy extends AbstractSelectLockingStrategy {
	/**
	 * Construct a locking strategy based on SQL SELECT statements.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indictates the type of lock to be acquired.
	 */
	public SelectLockingStrategy(Lockable lockable, LockMode lockMode) {
		super( lockable, lockMode );
	}

	/**
	 * @see LockingStrategy#lock
	 */
	public void lock(
	        Serializable id,
	        Object version,
	        Object object,
	        int timeout, 
	        SessionImplementor session) throws StaleObjectStateException, JDBCException {
		final String sql = determineSql( timeout );
		SessionFactoryImplementor factory = session.getFactory();
		try {
			PreparedStatement st = session.getJDBCContext().getConnectionManager().prepareSelectStatement( sql );
			try {
				getLockable().getIdentifierType().nullSafeSet( st, id, 1, session );
				if ( getLockable().isVersioned() ) {
					getLockable().getVersionType().nullSafeSet(
							st,
							version,
							getLockable().getIdentifierType().getColumnSpan( factory ) + 1,
							session
					);
				}

				ResultSet rs = st.executeQuery();
				try {
					if ( !rs.next() ) {
						if ( factory.getStatistics().isStatisticsEnabled() ) {
							factory.getStatisticsImplementor()
									.optimisticFailure( getLockable().getEntityName() );
						}
						throw new StaleObjectStateException( getLockable().getEntityName(), id );
					}
				}
				finally {
					rs.close();
				}
			}
			finally {
				st.close();
			}

		}
		catch ( SQLException sqle ) {
			throw session.getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not lock: " + MessageHelper.infoString( getLockable(), id, session.getFactory() ),
					sql
				);
		}
	}

	protected String generateLockString(int timeout) {
		SessionFactoryImplementor factory = getLockable().getFactory();
		LockOptions lockOptions = new LockOptions( getLockMode() );
		lockOptions.setTimeOut( timeout );
		SimpleSelect select = new SimpleSelect( factory.getDialect() )
				.setLockOptions( lockOptions )
				.setTableName( getLockable().getRootTableName() )
				.addColumn( getLockable().getRootTableIdentifierColumnNames()[0] )
				.addCondition( getLockable().getRootTableIdentifierColumnNames(), "=?" );
		if ( getLockable().isVersioned() ) {
			select.addCondition( getLockable().getVersionColumnName(), "=?" );
		}
		if ( factory.getSettings().isCommentsEnabled() ) {
			select.setComment( getLockMode() + " lock " + getLockable().getEntityName() );
		}
		return select.toStatementString();
	}
}
