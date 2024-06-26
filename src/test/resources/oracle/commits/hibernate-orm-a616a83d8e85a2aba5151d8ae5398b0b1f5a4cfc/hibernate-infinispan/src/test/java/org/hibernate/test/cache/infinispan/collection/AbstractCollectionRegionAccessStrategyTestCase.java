/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
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
package org.hibernate.test.cache.infinispan.collection;

import javax.transaction.TransactionManager;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.infinispan.transaction.tm.BatchModeTransactionManager;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.impl.CacheDataDescriptionImpl;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.cache.infinispan.access.TransactionalAccessDelegate;
import org.hibernate.cache.infinispan.collection.CollectionRegionImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.compare.ComparableComparator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import junit.framework.AssertionFailedError;

import org.hibernate.test.cache.infinispan.AbstractNonFunctionalTestCase;
import org.hibernate.test.cache.infinispan.NodeEnvironment;
import org.hibernate.test.cache.infinispan.functional.cluster.DualNodeJtaTransactionManagerImpl;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;

import static org.hibernate.testing.TestLogger.LOG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Base class for tests of CollectionRegionAccessStrategy impls.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractCollectionRegionAccessStrategyTestCase extends AbstractNonFunctionalTestCase {

	public static final String REGION_NAME = "test/com.foo.test";
	public static final String KEY_BASE = "KEY";
	public static final String VALUE1 = "VALUE1";
	public static final String VALUE2 = "VALUE2";

	protected static int testCount;

	protected NodeEnvironment localEnvironment;
	protected CollectionRegionImpl localCollectionRegion;
	protected CollectionRegionAccessStrategy localAccessStrategy;

	protected NodeEnvironment remoteEnvironment;
	protected CollectionRegionImpl remoteCollectionRegion;
	protected CollectionRegionAccessStrategy remoteAccessStrategy;

	protected boolean invalidation;
	protected boolean synchronous;

	protected Exception node1Exception;
	protected Exception node2Exception;

	protected AssertionFailedError node1Failure;
	protected AssertionFailedError node2Failure;

	protected abstract AccessType getAccessType();

	@Before
	public void prepareResources() throws Exception {
		// to mimic exactly the old code results, both environments here are exactly the same...
		Configuration cfg = createConfiguration( getConfigurationName() );
		localEnvironment = new NodeEnvironment( cfg );
		localEnvironment.prepare();

		localCollectionRegion = localEnvironment.getCollectionRegion( REGION_NAME, getCacheDataDescription() );
		localAccessStrategy = localCollectionRegion.buildAccessStrategy( getAccessType() );

		invalidation = localCollectionRegion.getCacheAdapter().isClusteredInvalidation();
		synchronous = localCollectionRegion.getCacheAdapter().isSynchronous();

		// Sleep a bit to avoid concurrent FLUSH problem
		avoidConcurrentFlush();

		remoteEnvironment = new NodeEnvironment( cfg );
		remoteEnvironment.prepare();

		remoteCollectionRegion = remoteEnvironment.getCollectionRegion( REGION_NAME, getCacheDataDescription() );
		remoteAccessStrategy = remoteCollectionRegion.buildAccessStrategy( getAccessType() );
	}

	protected abstract String getConfigurationName();

	protected static Configuration createConfiguration(String configName) {
		Configuration cfg = CacheTestUtil.buildConfiguration(
				REGION_PREFIX, InfinispanRegionFactory.class, true, false
		);
		cfg.setProperty( InfinispanRegionFactory.ENTITY_CACHE_RESOURCE_PROP, configName );
		return cfg;
	}

	protected CacheDataDescription getCacheDataDescription() {
		return new CacheDataDescriptionImpl( true, true, ComparableComparator.INSTANCE );
	}

	@After
	public void releaseResources() throws Exception {
		if ( localEnvironment != null ) {
			localEnvironment.release();
		}
		if ( remoteEnvironment != null ) {
			remoteEnvironment.release();
		}
	}

	protected boolean isUsingInvalidation() {
		return invalidation;
	}

	protected boolean isSynchronous() {
		return synchronous;
	}

	@Test
	public abstract void testCacheConfiguration();

	@Test
	public void testGetRegion() {
		assertEquals( "Correct region", localCollectionRegion, localAccessStrategy.getRegion() );
	}

	@Test
	public void testPutFromLoadRemoveDoesNotProduceStaleData() throws Exception {
		final CountDownLatch pferLatch = new CountDownLatch( 1 );
		final CountDownLatch removeLatch = new CountDownLatch( 1 );
		TransactionManager tm = DualNodeJtaTransactionManagerImpl.getInstance( "test1234" );
		PutFromLoadValidator validator = new PutFromLoadValidator( tm ) {
			@Override
			public boolean acquirePutFromLoadLock(Object key) {
				boolean acquired = super.acquirePutFromLoadLock( key );
				try {
					removeLatch.countDown();
					pferLatch.await( 2, TimeUnit.SECONDS );
				}
				catch (InterruptedException e) {
					LOG.debug( "Interrupted" );
					Thread.currentThread().interrupt();
				}
				catch (Exception e) {
					LOG.error( "Error", e );
					throw new RuntimeException( "Error", e );
				}
				return acquired;
			}
		};
		final TransactionalAccessDelegate delegate = new TransactionalAccessDelegate(
				(CollectionRegionImpl) localCollectionRegion, validator
		);

		Callable<Void> pferCallable = new Callable<Void>() {
			public Void call() throws Exception {
				delegate.putFromLoad( "k1", "v1", 0, null );
				return null;
			}
		};

		Callable<Void> removeCallable = new Callable<Void>() {
			public Void call() throws Exception {
				removeLatch.await();
				delegate.remove( "k1" );
				pferLatch.countDown();
				return null;
			}
		};

		ExecutorService executorService = Executors.newCachedThreadPool();
		Future<Void> pferFuture = executorService.submit( pferCallable );
		Future<Void> removeFuture = executorService.submit( removeCallable );

		pferFuture.get();
		removeFuture.get();

		assertFalse( localCollectionRegion.getCacheAdapter().containsKey( "k1" ) );
	}

	@Test
	public void testPutFromLoad() throws Exception {
		putFromLoadTest( false );
	}

	@Test
	public void testPutFromLoadMinimal() throws Exception {
		putFromLoadTest( true );
	}

	private void putFromLoadTest(final boolean useMinimalAPI) throws Exception {

		final String KEY = KEY_BASE + testCount++;

		final CountDownLatch writeLatch1 = new CountDownLatch( 1 );
		final CountDownLatch writeLatch2 = new CountDownLatch( 1 );
		final CountDownLatch completionLatch = new CountDownLatch( 2 );

		Thread node1 = new Thread() {

			public void run() {

				try {
					long txTimestamp = System.currentTimeMillis();
					BatchModeTransactionManager.getInstance().begin();

					assertEquals( "node1 starts clean", null, localAccessStrategy.get( KEY, txTimestamp ) );

					writeLatch1.await();

					if ( useMinimalAPI ) {
						localAccessStrategy.putFromLoad( KEY, VALUE2, txTimestamp, new Integer( 2 ), true );
					}
					else {
						localAccessStrategy.putFromLoad( KEY, VALUE2, txTimestamp, new Integer( 2 ) );
					}

					BatchModeTransactionManager.getInstance().commit();
				}
				catch (Exception e) {
					LOG.error( "node1 caught exception", e );
					node1Exception = e;
					rollback();
				}
				catch (AssertionFailedError e) {
					node1Failure = e;
					rollback();
				}
				finally {
					// Let node2 write
					writeLatch2.countDown();
					completionLatch.countDown();
				}
			}
		};

		Thread node2 = new Thread() {

			public void run() {

				try {
					long txTimestamp = System.currentTimeMillis();
					BatchModeTransactionManager.getInstance().begin();

					assertNull( "node2 starts clean", remoteAccessStrategy.get( KEY, txTimestamp ) );

					// Let node1 write
					writeLatch1.countDown();
					// Wait for node1 to finish
					writeLatch2.await();

					// Let the first PFER propagate
					sleep( 200 );

					if ( useMinimalAPI ) {
						remoteAccessStrategy.putFromLoad( KEY, VALUE1, txTimestamp, new Integer( 1 ), true );
					}
					else {
						remoteAccessStrategy.putFromLoad( KEY, VALUE1, txTimestamp, new Integer( 1 ) );
					}

					BatchModeTransactionManager.getInstance().commit();
				}
				catch (Exception e) {
					LOG.error( "node2 caught exception", e );
					node2Exception = e;
					rollback();
				}
				catch (AssertionFailedError e) {
					node2Failure = e;
					rollback();
				}
				finally {
					completionLatch.countDown();
				}
			}
		};

		node1.setDaemon( true );
		node2.setDaemon( true );

		node1.start();
		node2.start();

		assertTrue( "Threads completed", completionLatch.await( 2, TimeUnit.SECONDS ) );

		if ( node1Failure != null ) {
			throw node1Failure;
		}
		if ( node2Failure != null ) {
			throw node2Failure;
		}

		assertEquals( "node1 saw no exceptions", null, node1Exception );
		assertEquals( "node2 saw no exceptions", null, node2Exception );

		// let the final PFER propagate
		sleep( 100 );

		long txTimestamp = System.currentTimeMillis();
		String msg1 = "Correct node1 value";
		String msg2 = "Correct node2 value";
		Object expected1 = null;
		Object expected2 = null;
		if ( isUsingInvalidation() ) {
			// PFER does not generate any invalidation, so each node should
			// succeed. We count on database locking and Hibernate removing
			// the collection on any update to prevent the situation we have
			// here where the caches have inconsistent data
			expected1 = VALUE2;
			expected2 = VALUE1;
		}
		else {
			// the initial VALUE2 should prevent the node2 put
			expected1 = VALUE2;
			expected2 = VALUE2;
		}

		assertEquals( msg1, expected1, localAccessStrategy.get( KEY, txTimestamp ) );
		assertEquals( msg2, expected2, remoteAccessStrategy.get( KEY, txTimestamp ) );
	}

	@Test
	public void testRemove() {
		evictOrRemoveTest( false );
	}

	@Test
	public void testRemoveAll() {
		evictOrRemoveAllTest( false );
	}

	@Test
	public void testEvict() {
		evictOrRemoveTest( true );
	}

	@Test
	public void testEvictAll() {
		evictOrRemoveAllTest( true );
	}

	private void evictOrRemoveTest(boolean evict) {

		final String KEY = KEY_BASE + testCount++;

		assertNull( "local is clean", localAccessStrategy.get( KEY, System.currentTimeMillis() ) );
		assertNull( "remote is clean", remoteAccessStrategy.get( KEY, System.currentTimeMillis() ) );

		localAccessStrategy.putFromLoad( KEY, VALUE1, System.currentTimeMillis(), new Integer( 1 ) );
		assertEquals( VALUE1, localAccessStrategy.get( KEY, System.currentTimeMillis() ) );
		remoteAccessStrategy.putFromLoad( KEY, VALUE1, System.currentTimeMillis(), new Integer( 1 ) );
		assertEquals( VALUE1, remoteAccessStrategy.get( KEY, System.currentTimeMillis() ) );

		// Wait for async propagation
		sleep( 250 );

		if ( evict ) {
			localAccessStrategy.evict( KEY );
		}
		else {
			localAccessStrategy.remove( KEY );
		}

		assertEquals( null, localAccessStrategy.get( KEY, System.currentTimeMillis() ) );

		assertEquals( null, remoteAccessStrategy.get( KEY, System.currentTimeMillis() ) );
	}

	private void evictOrRemoveAllTest(boolean evict) {

		final String KEY = KEY_BASE + testCount++;

		assertEquals( 0, getValidKeyCount( localCollectionRegion.getCacheAdapter().keySet() ) );

		assertEquals( 0, getValidKeyCount( remoteCollectionRegion.getCacheAdapter().keySet() ) );

		assertNull( "local is clean", localAccessStrategy.get( KEY, System.currentTimeMillis() ) );
		assertNull( "remote is clean", remoteAccessStrategy.get( KEY, System.currentTimeMillis() ) );

		localAccessStrategy.putFromLoad( KEY, VALUE1, System.currentTimeMillis(), new Integer( 1 ) );
		assertEquals( VALUE1, localAccessStrategy.get( KEY, System.currentTimeMillis() ) );
		remoteAccessStrategy.putFromLoad( KEY, VALUE1, System.currentTimeMillis(), new Integer( 1 ) );
		assertEquals( VALUE1, remoteAccessStrategy.get( KEY, System.currentTimeMillis() ) );

		// Wait for async propagation
		sleep( 250 );

		if ( evict ) {
			localAccessStrategy.evictAll();
		}
		else {
			localAccessStrategy.removeAll();
		}

		// This should re-establish the region root node
		assertNull( localAccessStrategy.get( KEY, System.currentTimeMillis() ) );

		assertEquals( 0, getValidKeyCount( localCollectionRegion.getCacheAdapter().keySet() ) );

		// Re-establishing the region root on the local node doesn't
		// propagate it to other nodes. Do a get on the remote node to re-establish
		assertEquals( null, remoteAccessStrategy.get( KEY, System.currentTimeMillis() ) );

		assertEquals( 0, getValidKeyCount( remoteCollectionRegion.getCacheAdapter().keySet() ) );

		// Test whether the get above messes up the optimistic version
		remoteAccessStrategy.putFromLoad( KEY, VALUE1, System.currentTimeMillis(), new Integer( 1 ) );
		assertEquals( VALUE1, remoteAccessStrategy.get( KEY, System.currentTimeMillis() ) );

		assertEquals( 1, getValidKeyCount( remoteCollectionRegion.getCacheAdapter().keySet() ) );

		// Wait for async propagation of the putFromLoad
		sleep( 250 );

		assertEquals(
				"local is correct", (isUsingInvalidation() ? null : VALUE1), localAccessStrategy.get(
				KEY, System
				.currentTimeMillis()
		)
		);
		assertEquals( "remote is correct", VALUE1, remoteAccessStrategy.get( KEY, System.currentTimeMillis() ) );
	}

	private void rollback() {
		try {
			BatchModeTransactionManager.getInstance().rollback();
		}
		catch (Exception e) {
			LOG.error( e.getMessage(), e );
		}

	}

}
