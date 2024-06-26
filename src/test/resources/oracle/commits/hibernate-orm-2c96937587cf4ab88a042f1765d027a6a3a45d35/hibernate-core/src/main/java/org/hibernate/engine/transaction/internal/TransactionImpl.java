/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.internal;

import javax.transaction.Synchronization;

import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.jboss.logging.Logger;

import static org.hibernate.resource.transaction.spi.TransactionCoordinator.TransactionDriver;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class TransactionImpl implements TransactionImplementor {
	private static final Logger LOG = CoreLogging.logger( TransactionImpl.class );

	private final TransactionCoordinator transactionCoordinator;
	private TransactionDriver transactionDriverControl;

	public TransactionImpl(TransactionCoordinator transactionCoordinator) {
		this.transactionCoordinator = transactionCoordinator;
	}

	@Override
	public void begin() {
		if ( !transactionCoordinator.isActive() ) {
			throw new TransactionException( "Cannot begin Transaction on closed Session/EntityManager" );
		}

		if ( transactionDriverControl == null ) {
			transactionDriverControl = transactionCoordinator.getTransactionDriverControl();
		}
		// per-JPA
		if ( isActive() ) {
			throw new IllegalStateException( "Transaction already active" );
		}

		LOG.debug( "begin" );
		this.transactionDriverControl.begin();
	}

	@Override
	public void commit() {
		if ( !isActive() ) {
			// allow MARKED_ROLLBACK to propagate through to transactionDriverControl
			throw new IllegalStateException( "Transaction not successfully started" );
		}

		LOG.debug( "committing" );

		try {
			internalGetTransactionDriverControl().commit();
		}
		finally {
//			invalidate();
		}
	}

	public TransactionDriver internalGetTransactionDriverControl() {
		// NOTE here to help be a more descriptive NullPointerException
		if ( this.transactionDriverControl == null ) {
			throw new IllegalStateException( "Transaction was not properly begun/started" );
		}
		return this.transactionDriverControl;
	}

	@Override
	public void rollback() {
		// todo : may need a "JPA compliant" flag here

		TransactionStatus status = getStatus();
		if ( status == TransactionStatus.ROLLED_BACK || status == TransactionStatus.NOT_ACTIVE ) {
			// Allow rollback() calls on completed transactions, just no-op.
			LOG.debug( "rollback() called on an inactive transaction" );
//			invalidate();
			return;
		}

		if ( !status.canRollback() ) {
			throw new TransactionException( "Cannot rollback transaction in current status [" + status.name() + "]" );
		}

		LOG.debug( "rolling back" );
		if ( status != TransactionStatus.FAILED_COMMIT || allowFailedCommitToPhysicallyRollback() ) {
			try {
				internalGetTransactionDriverControl().rollback();
			}
			finally {
				invalidate();
			}
		}
	}

	@Override
	public boolean isActive() {
		return getStatus() == TransactionStatus.ACTIVE || getStatus() == TransactionStatus.MARKED_ROLLBACK;
	}

	@Override
	public TransactionStatus getStatus() {
		// Allow looking at STATUS on closed Session/Transaction
		return transactionDriverControl == null
				? TransactionStatus.NOT_ACTIVE
				: transactionDriverControl.getStatus();
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) throws HibernateException {
		this.transactionCoordinator.getLocalSynchronizations().registerSynchronization( synchronization );
	}

	@Override
	public void setTimeout(int seconds) {
		this.transactionCoordinator.setTimeOut( seconds );
	}

	@Override
	public int getTimeout() {
		return this.transactionCoordinator.getTimeOut();
	}

	@Override
	public void setRollbackOnly() {
		internalGetTransactionDriverControl().markRollbackOnly();
	}

	@Override
	public boolean getRollbackOnly() {
		return getStatus() == TransactionStatus.MARKED_ROLLBACK;
	}

	protected boolean allowFailedCommitToPhysicallyRollback() {
		return false;
	}
}
