package org.apache.lucene.index.values;

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

import java.io.IOException;
import java.util.Comparator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.values.DocValues.SortedSource;
import org.apache.lucene.index.values.DocValues.Source;
import org.apache.lucene.util.BytesRef;

/**
 * Abstract base class for {@link DocValues} {@link Source} /
 * {@link SortedSource} cache.
 * <p>
 * {@link Source} and {@link SortedSource} instances loaded via
 * {@link DocValues#load()} and {@link DocValues#loadSorted(Comparator)} are
 * entirely memory resident and need to be maintained by the caller. Each call
 * to {@link DocValues#load()} or {@link DocValues#loadSorted(Comparator)} will
 * cause an entire reload of the underlying data. Source and
 * {@link SortedSource} instances obtained from {@link DocValues#getSource()}
 * and {@link DocValues#getSource()} respectively are maintained by a
 * {@link SourceCache} that is closed ({@link #close(DocValues)}) once the
 * {@link IndexReader} that created the {@link DocValues} instance is closed.
 * <p>
 * Unless {@link Source} and {@link SortedSource} instances are managed by
 * another entity it is recommended to use the cached variants to obtain a
 * source instance.
 * <p>
 * Implementation of this API must be thread-safe.
 * 
 * @see DocValues#setCache(SourceCache)
 * @see DocValues#getSource()
 * @see DocValues#getSortedSorted(Comparator)
 * 
 * @lucene.experimental
 */
public abstract class SourceCache {

  /**
   * Atomically loads a {@link Source} into the cache from the given
   * {@link DocValues} and returns it iff no other {@link Source} has already
   * been cached. Otherwise the cached source is returned.
   * <p>
   * This method will not return <code>null</code>
   */
  public abstract Source load(DocValues values) throws IOException;

  /**
   * Atomically loads a {@link SortedSource} into the cache from the given
   * {@link DocValues} and returns it iff no other {@link SortedSource} has
   * already been cached. Otherwise the cached source is returned.
   * <p>
   * This method will not return <code>null</code>
   */
  public abstract SortedSource loadSorted(DocValues values,
      Comparator<BytesRef> comp) throws IOException;

  /**
   * Atomically invalidates the cached {@link Source} and {@link SortedSource}
   * instances if any and empties the cache.
   */
  public abstract void invalidate(DocValues values);

  /**
   * Atomically closes the cache and frees all resources.
   */
  public synchronized void close(DocValues values) {
    invalidate(values);
  }

  /**
   * Simple per {@link DocValues} instance cache implementation that holds a
   * {@link Source} and {@link SortedSource} reference as a member variable.
   * <p>
   * If a {@link DirectSourceCache} instance is closed or invalidated the cached
   * reference are simply set to <code>null</code>
   */
  public static final class DirectSourceCache extends SourceCache {
    private Source ref;
    private SortedSource sortedRef;

    public synchronized Source load(DocValues values) throws IOException {
      if (ref == null) {
        ref = values.load();
      }
      return ref;
    }

    public synchronized SortedSource loadSorted(DocValues values,
        Comparator<BytesRef> comp) throws IOException {
      if (sortedRef == null) {
        sortedRef = values.loadSorted(comp);
      }
      return sortedRef;
    }

    public synchronized void invalidate(DocValues values) {
      ref = null;
      sortedRef = null;
    }
  }

}
