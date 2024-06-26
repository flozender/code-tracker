/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Defines the strategy for access to entity or collection data in a Infinispan instance.
 * <p/>
 * The intent of this class is to encapsulate common code and serve as a delegate for
 * {@link org.hibernate.cache.spi.access.EntityRegionAccessStrategy}
 * and {@link org.hibernate.cache.spi.access.CollectionRegionAccessStrategy} implementations.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public interface AccessDelegate {
	Object get(SessionImplementor session, Object key, long txTimestamp) throws CacheException;

	/**
	 * Attempt to cache an object, afterQuery loading from the database.
	 *
	 * @param session Current session
	 * @param key The item key
	 * @param value The item
	 * @param txTimestamp a timestamp prior to the transaction start time
	 * @param version the item version number
	 * @return <tt>true</tt> if the object was successfully cached
	 */
	boolean putFromLoad(SessionImplementor session, Object key, Object value, long txTimestamp, Object version);

	/**
	 * Attempt to cache an object, afterQuery loading from the database, explicitly
	 * specifying the minimalPut behavior.
	 *
	 * @param session Current session.
	 * @param key The item key
	 * @param value The item
	 * @param txTimestamp a timestamp prior to the transaction start time
	 * @param version the item version number
	 * @param minimalPutOverride Explicit minimalPut flag
	 * @return <tt>true</tt> if the object was successfully cached
	 * @throws org.hibernate.cache.CacheException Propogated from underlying {@link org.hibernate.cache.spi.Region}
	 */
	boolean putFromLoad(SessionImplementor session, Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException;

	/**
	 * Called afterQuery an item has been inserted (beforeQuery the transaction completes),
	 * instead of calling evict().
	 *
	 * @param session Current session
	 * @param key The item key
	 * @param value The item
	 * @param version The item's version value
	 * @return Were the contents of the cache actual changed by this operation?
	 * @throws CacheException if the insert fails
	 */
	boolean insert(SessionImplementor session, Object key, Object value, Object version) throws CacheException;

	/**
	 * Called afterQuery an item has been updated (beforeQuery the transaction completes),
	 * instead of calling evict().
	 *
	 * @param session Current session
	 * @param key The item key
	 * @param value The item
	 * @param currentVersion The item's current version value
	 * @param previousVersion The item's previous version value
	 * @return Whether the contents of the cache actual changed by this operation
	 * @throws CacheException if the update fails
	 */
	boolean update(SessionImplementor session, Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException;

	/**
	 * Called afterQuery an item has become stale (beforeQuery the transaction completes).
	 *
	 * @param session Current session
	 * @param key The key of the item to remove
	 * @throws CacheException if removing the cached item fails
	 */
	void remove(SessionImplementor session, Object key) throws CacheException;

	/**
	 * Called to evict data from the entire region
	 *
	 * @throws CacheException if eviction the region fails
	 */
	void removeAll() throws CacheException;

	/**
	 * Forcibly evict an item from the cache immediately without regard for transaction
	 * isolation.
	 *
	 * @param key The key of the item to remove
	 * @throws CacheException if evicting the item fails
	 */
	void evict(Object key) throws CacheException;

	/**
	 * Forcibly evict all items from the cache immediately without regard for transaction
	 * isolation.
	 *
	 * @throws CacheException if evicting items fails
	 */
	void evictAll() throws CacheException;

	/**
	 * Called when we have finished the attempted update/delete (which may or
	 * may not have been successful), afterQuery transaction completion.  This method
	 * is used by "asynchronous" concurrency strategies.
	 *
	 *
	 * @param session
	 * @param key The item key
	 * @throws org.hibernate.cache.CacheException Propogated from underlying {@link org.hibernate.cache.spi.Region}
	 */
	void unlockItem(SessionImplementor session, Object key) throws CacheException;

	/**
	 * Called afterQuery an item has been inserted (afterQuery the transaction completes),
	 * instead of calling release().
	 * This method is used by "asynchronous" concurrency strategies.
	 *
	 *
	 * @param session
	 * @param key The item key
	 * @param value The item
	 * @param version The item's version value
	 * @return Were the contents of the cache actual changed by this operation?
	 * @throws CacheException Propagated from underlying {@link org.hibernate.cache.spi.Region}
	 */
	boolean afterInsert(SessionImplementor session, Object key, Object value, Object version);

	/**
	 * Called afterQuery an item has been updated (afterQuery the transaction completes),
	 * instead of calling release().  This method is used by "asynchronous"
	 * concurrency strategies.
	 *
	 *
	 * @param session
	 * @param key The item key
	 * @param value The item
	 * @param currentVersion The item's current version value
	 * @param previousVersion The item's previous version value
	 * @param lock The lock previously obtained from {@link #lockItem}
	 * @return Were the contents of the cache actual changed by this operation?
	 * @throws CacheException Propagated from underlying {@link org.hibernate.cache.spi.Region}
	 */
	boolean afterUpdate(SessionImplementor session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock);
}
