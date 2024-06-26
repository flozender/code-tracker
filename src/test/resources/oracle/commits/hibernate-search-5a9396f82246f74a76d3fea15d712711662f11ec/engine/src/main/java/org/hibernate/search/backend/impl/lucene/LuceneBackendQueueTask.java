/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.exception.impl.ErrorContextBuilder;

/**
 * Apply the operations to Lucene directories.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 * @author Sanne Grinovero
 */
final class LuceneBackendQueueTask implements Runnable {

	private static final Log log = LoggerFactory.make();

	private final Lock modificationLock;
	private final LuceneBackendResources resources;
	private final List<LuceneWork> workList;
	private final IndexingMonitor monitor;

	LuceneBackendQueueTask(List<LuceneWork> workList, LuceneBackendResources resources, IndexingMonitor monitor) {
		this.workList = workList;
		this.resources = resources;
		this.monitor = monitor;
		this.modificationLock = resources.getParallelModificationLock();
	}

	@Override
	public void run() {
		modificationLock.lock();
		try {
			applyUpdates();
		}
		catch (InterruptedException e) {
			log.interruptedWhileWaitingForIndexActivity( e );
			Thread.currentThread().interrupt();
			handleException( e );
		}
		catch (Exception e) {
			log.backendError( e );
			handleException( e );
		}
		finally {
			modificationLock.unlock();
		}
	}

	private void handleException(Exception e) {
		ErrorContextBuilder builder = new ErrorContextBuilder();
		builder.allWorkToBeDone( workList );
		builder.errorThatOccurred( e );
		resources.getErrorHandler().handle( builder.createErrorContext() );
	}

	/**
	 * Applies all modifications to the index in parallel using the workers executor
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void applyUpdates() throws InterruptedException, ExecutionException {
		AbstractWorkspaceImpl workspace = resources.getWorkspace();

		ErrorContextBuilder errorContextBuilder = new ErrorContextBuilder();
		errorContextBuilder.allWorkToBeDone( workList );

		IndexWriter indexWriter = workspace.getIndexWriter( errorContextBuilder );
		if ( indexWriter == null ) {
			log.cannotOpenIndexWriterCausePreviousError();
			return;
		}

		boolean taskExecutionSuccessful = true;

		try {
			if ( workList.size() == 1 ) {
				taskExecutionSuccessful = runSingleTask( workList.get( 0 ), indexWriter, errorContextBuilder );
			}
			else {
				taskExecutionSuccessful = runMultipleTasks( indexWriter, errorContextBuilder );
			}
			if ( !taskExecutionSuccessful ) {
				resources.getErrorHandler().handle( errorContextBuilder.createErrorContext() );
			}
			else {
				workspace.optimizerPhase();
			}
		}
		finally {
			workspace.afterTransactionApplied( !taskExecutionSuccessful, false );
		}
	}

	/**
	 * Applies each modification in parallel using the backend workers pool
	 * @throws InterruptedException
	 */
	private boolean runMultipleTasks(final IndexWriter indexWriter, final ErrorContextBuilder errorContextBuilder) throws InterruptedException {
		final int queueSize = workList.size();
		final ExecutorService executor = resources.getWorkersExecutor();
		final Future<LuceneWork>[] submittedTasks = new Future[ queueSize ];

		for ( int i = 0; i < queueSize; i++ ) {
			LuceneWork luceneWork = workList.get( i );
			SingleTaskRunnable task = new SingleTaskRunnable( luceneWork, resources, indexWriter, monitor );
			submittedTasks[i] = executor.submit( task, luceneWork );
		}

		boolean allTasksSuccessful = true;

		// now wait for all tasks being completed before releasing our lock
		// (this thread waits even in async backend mode)
		for ( int i = 0; i < queueSize; i++ ) {
			Future<LuceneWork> task = submittedTasks[i];
			try {
				LuceneWork work = task.get();
				errorContextBuilder.workCompleted( work );
			}
			catch (ExecutionException e) {
				errorContextBuilder.addWorkThatFailed( workList.get( i ) );
				errorContextBuilder.errorThatOccurred( e.getCause() );
				allTasksSuccessful = false;
			}
		}

		return allTasksSuccessful;
	}

	/**
	 * Applies a single modification using the caller's thread to avoid pointless context
	 * switching.
	 */
	private boolean runSingleTask(final LuceneWork luceneWork, final IndexWriter indexWriter, final ErrorContextBuilder errorContextBuilder) {
		try {
			SingleTaskRunnable.performWork( luceneWork, resources, indexWriter, monitor );
			return true;
		}
		catch (RuntimeException re) {
			errorContextBuilder.errorThatOccurred( re );
			errorContextBuilder.addWorkThatFailed( luceneWork );
			return false;
		}
	}

}
