package org.apache.lucene.util.cache;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

/**
 * Simple concurrent LRU cache, using a "double barrel"
 * approach where two ConcurrentHashMaps record entries.
 *
 * <p>At any given time, one hash is primary and the other
 * is secondary.  {@link #get} first checks primary, and if
 * that's a miss, checks secondary.  If secondary has the
 * entry, it's promoted to primary.  Once primary is full,
 * the secondary is cleared and the two are swapped.</p>
 *
 * <p>This is not as space efficient as other possible
 * concurrent approaches (see LUCENE-2075): to achieve
 * perfect LRU(N) it requires 2*N storage.  But, this
 * approach is relatively simple and seems in practice to
 * not grow unbounded in size when under hideously high
 * load.</p>
 *
 * <p>NOTE: this class is meant only to be used internally
 * by Lucene; it's only public so it can be shared across
 * packages.  This means the API is freely subject to
 * change, and, the class could be removed entirely, in any
 * Lucene release.  Use directly at your own risk!
 */

final public class DoubleBarrelLRUCache<K,V> extends Cache<K,V> {
  private final Map<K,V> cache1;
  private final Map<K,V> cache2;
  private final AtomicInteger countdown;
  private volatile boolean swapped;
  private final int maxSize;

  public DoubleBarrelLRUCache(int maxSize) {
    this.maxSize = maxSize;
    countdown = new AtomicInteger(maxSize);
    cache1 = new ConcurrentHashMap<K,V>();
    cache2 = new ConcurrentHashMap<K,V>();
  }

  @Override
  public boolean containsKey(Object k) {
    return false;
  }

  @Override
  public void close() {
  }

  @Override @SuppressWarnings("unchecked")
  public V get(Object key) {
    final Map<K,V> primary;
    final Map<K,V> secondary;
    if (swapped) {
      primary = cache2;
      secondary = cache1;
    } else {
      primary = cache1;
      secondary = cache2;
    }

    // Try primary frist
    V result = primary.get(key);
    if (result == null) {
      // Not found -- try secondary
      result = secondary.get(key);
      if (result != null) {
        // Promote to primary
        put((K) key, result);
      }
    }
    return result;
  }

  @Override
  public void put(K key, V value) {
    final Map<K,V> primary;
    final Map<K,V> secondary;
    if (swapped) {
      primary = cache2;
      secondary = cache1;
    } else {
      primary = cache1;
      secondary = cache2;
    }
    primary.put(key, value);

    if (countdown.decrementAndGet() == 0) {
      // Time to swap

      // NOTE: there is saturation risk here, that the
      // thread that's doing the clear() takes too long to
      // do so, while other threads continue to add to
      // primary, but in practice this seems not to be an
      // issue (see LUCENE-2075 for benchmark & details)

      // First, clear secondary
      secondary.clear();

      // Second, swap
      swapped = !swapped;

      // Third, reset countdown
      countdown.set(maxSize);
    }
  }
}
