package org.apache.lucene.index;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.Iterator;

import org.apache.lucene.index.DocumentsWriterPerThreadPool.ThreadState;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.SetOnce;

/**
 * {@link FlushPolicy} controls when segments are flushed from a RAM resident
 * internal data-structure to the {@link IndexWriter}s {@link Directory}.
 * <p>
 * Segments are traditionally flushed by:
 * <ul>
 * <li>RAM consumption - configured via
 * {@link IndexWriterConfig#setRAMBufferSizeMB(double)}</li>
 * <li>Number of RAM resident documents - configured via
 * {@link IndexWriterConfig#setMaxBufferedDocs(int)}</li>
 * <li>Number of buffered delete terms - configured via
 * {@link IndexWriterConfig#setMaxBufferedDeleteTerms(int)}</li>
 * </ul>
 * 
 * The {@link IndexWriter} uses a provided {@link FlushPolicy} to control the
 * flushing process during indexing. The policy is informed for each added or
 * updated document as well as for each delete term. Based on the
 * {@link FlushPolicy} the information provided via {@link ThreadState} and
 * {@link DocumentsWriterFlushControl} the {@link FlushPolicy} can decide if a
 * {@link DocumentsWriterPerThread} needs flushing and can mark it as
 * flush-pending via
 * {@link DocumentsWriterFlushControl#setFlushPending(ThreadState)}.
 * 
 * @see ThreadState
 * @see DocumentsWriterFlushControl
 * @see DocumentsWriterPerThread
 * @see IndexWriterConfig#setFlushPolicy(FlushPolicy)
 */
public abstract class FlushPolicy {
  protected final SetOnce<DocumentsWriter> writer = new SetOnce<DocumentsWriter>();
  protected IndexWriterConfig indexWriterConfig;

  /**
   * Called for each delete term. If this is a delete triggered due to an update
   * the given {@link ThreadState} is non-null.
   * <p>
   * Note: This method is synchronized by the given
   * {@link DocumentsWriterFlushControl} and it is guaranteed that the calling
   * thread holds the lock on the given {@link ThreadState}
   */
  public abstract void onDelete(DocumentsWriterFlushControl control,
      ThreadState state);

  /**
   * Called for each document update on the given {@link ThreadState}s
   * {@link DocumentsWriterPerThread}.
   * <p>
   * Note: This method is synchronized by the given
   * {@link DocumentsWriterFlushControl} and it is guaranteed that the calling
   * thread holds the lock on the given {@link ThreadState}
   */
  public void onUpdate(DocumentsWriterFlushControl control, ThreadState state) {
    onInsert(control, state);
    if (!state.flushPending) {
      onDelete(control, state);
    }
  }

  /**
   * Called for each document addition on the given {@link ThreadState}s
   * {@link DocumentsWriterPerThread}.
   * <p>
   * Note: This method is synchronized by the given
   * {@link DocumentsWriterFlushControl} and it is guaranteed that the calling
   * thread holds the lock on the given {@link ThreadState}
   */
  public abstract void onInsert(DocumentsWriterFlushControl control,
      ThreadState state);

  /**
   * Called by {@link DocumentsWriter} to initialize the FlushPolicy
   */
  protected synchronized void init(DocumentsWriter docsWriter) {
    writer.set(docsWriter);
    indexWriterConfig = docsWriter.indexWriter.getConfig();
  }

  /**
   * Marks the most ram consuming active {@link DocumentsWriterPerThread} flush
   * pending
   */
  protected void markLargestWriterPending(DocumentsWriterFlushControl control,
      ThreadState perThreadState, final long currentBytesPerThread) {
    control
        .setFlushPending(findLargestNonPendingWriter(control, perThreadState));
  }

  /**
   * Returns the current most RAM consuming non-pending {@link ThreadState} with
   * at least one indexed document.
   * <p>
   * This method will never return <code>null</code>
   */
  protected ThreadState findLargestNonPendingWriter(
      DocumentsWriterFlushControl control, ThreadState perThreadState) {
    long maxRamSoFar = perThreadState.perThreadBytes;
    // the dwpt which needs to be flushed eventually
    ThreadState maxRamUsingThreadState = perThreadState;
    assert !perThreadState.flushPending : "DWPT should have flushed";
    Iterator<ThreadState> activePerThreadsIterator = control.allActiveThreads();
    while (activePerThreadsIterator.hasNext()) {
      ThreadState next = activePerThreadsIterator.next();
      if (!next.flushPending) {
        final long nextRam = next.perThreadBytes;
        if (nextRam > maxRamSoFar && next.perThread.getNumDocsInRAM() > 0) {
          maxRamSoFar = nextRam;
          maxRamUsingThreadState = next;
        }
      }
    }
    assert writer.get().message(
        "set largest ram consuming thread pending on lower watermark");
    return maxRamUsingThreadState;
  }

  /**
   * Returns the max net memory which marks the upper watermark for the
   * DocumentsWriter to be healthy. If all flushing and active
   * {@link DocumentsWriterPerThread} consume more memory than the upper
   * watermark all incoming threads should be stalled and blocked until the
   * memory drops below this.
   * <p>
   * Note: the upper watermark is only taken into account if this
   * {@link FlushPolicy} flushes by ram usage.
   * 
   * <p>
   * The default for the max net memory is set to 2 x
   * {@link IndexWriterConfig#getRAMBufferSizeMB()}
   * 
   */
  public long getMaxNetBytes() {
    if (!flushOnRAM()) {
      return -1;
    }
    final double ramBufferSizeMB = indexWriterConfig.getRAMBufferSizeMB();
    return (long) (ramBufferSizeMB * 1024.d * 1024.d * 2);
  }

  /**
   * Returns <code>true</code> if this {@link FlushPolicy} flushes on
   * {@link IndexWriterConfig#getMaxBufferedDocs()}, otherwise
   * <code>false</code>.
   */
  protected boolean flushOnDocCount() {
    return indexWriterConfig.getMaxBufferedDocs() != IndexWriterConfig.DISABLE_AUTO_FLUSH;
  }

  /**
   * Returns <code>true</code> if this {@link FlushPolicy} flushes on
   * {@link IndexWriterConfig#getMaxBufferedDeleteTerms()}, otherwise
   * <code>false</code>.
   */
  protected boolean flushOnDeleteTerms() {
    return indexWriterConfig.getMaxBufferedDeleteTerms() != IndexWriterConfig.DISABLE_AUTO_FLUSH;
  }

  /**
   * Returns <code>true</code> if this {@link FlushPolicy} flushes on
   * {@link IndexWriterConfig#getRAMBufferSizeMB()}, otherwise
   * <code>false</code>.
   */
  protected boolean flushOnRAM() {
    return indexWriterConfig.getRAMBufferSizeMB() != IndexWriterConfig.DISABLE_AUTO_FLUSH;
  }

}
