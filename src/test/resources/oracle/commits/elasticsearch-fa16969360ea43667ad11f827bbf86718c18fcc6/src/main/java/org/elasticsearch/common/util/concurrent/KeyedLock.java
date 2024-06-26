/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.util.concurrent;

import org.elasticsearch.ElasticsearchIllegalStateException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class manages locks. Locks can be accessed with an identifier and are
 * created the first time they are acquired and removed if no thread hold the
 * lock. The latter is important to assure that the list of locks does not grow
 * infinitely.
 * 
 * A Thread can acquire a lock only once.
 * 
 * */

public class KeyedLock<T> {

    private final ConcurrentMap<T, KeyLock> map = new ConcurrentHashMap<T, KeyLock>();

    private final ThreadLocal<KeyLock> threadLocal = new ThreadLocal<KeyedLock.KeyLock>();

    public void acquire(T key) {
        while (true) {
            if (threadLocal.get() != null) {
                // if we are here, the thread already has the lock
                throw new ElasticsearchIllegalStateException("Lock already accquired in Thread" + Thread.currentThread().getId()
                        + " for key " + key);
            }
            KeyLock perNodeLock = map.get(key);
            if (perNodeLock == null) {
                KeyLock newLock = new KeyLock();
                perNodeLock = map.putIfAbsent(key, newLock);
                if (perNodeLock == null) {
                    newLock.lock();
                    threadLocal.set(newLock);
                    return;
                }
            }
            assert perNodeLock != null;
            int i = perNodeLock.count.get();
            if (i > 0 && perNodeLock.count.compareAndSet(i, i + 1)) {
                perNodeLock.lock();
                threadLocal.set(perNodeLock);
                return;
            }
        }
    }

    public void release(T key) {
        KeyLock lock = threadLocal.get();
        if (lock == null) {
            throw new ElasticsearchIllegalStateException("Lock not accquired");
        }
        assert lock.isHeldByCurrentThread();
        assert lock == map.get(key);
        lock.unlock();
        threadLocal.set(null);
        int decrementAndGet = lock.count.decrementAndGet();
        if (decrementAndGet == 0) {
            map.remove(key, lock);
        }
    }

    @SuppressWarnings("serial")
    private final static class KeyLock extends ReentrantLock {
        private final AtomicInteger count = new AtomicInteger(1);
    }

    public boolean hasLockedKeys() {
        return !map.isEmpty();
    }

}
