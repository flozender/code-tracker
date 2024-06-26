package org.apache.lucene.index;

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

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.index.CompositeIndexReader.CompositeReaderContext;
import org.apache.lucene.index.AtomicIndexReader.AtomicReaderContext;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.search.SearcherManager; // javadocs
import org.apache.lucene.store.*;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.ReaderUtil;         // for javadocs

/** IndexReader is an abstract class, providing an interface for accessing an
 index.  Search of an index is done entirely through this abstract interface,
 so that any subclass which implements it is searchable.

 <p> Concrete subclasses of IndexReader are usually constructed with a call to
 one of the static <code>open()</code> methods, e.g. {@link
 #open(Directory)}.

 <p> For efficiency, in this API documents are often referred to via
 <i>document numbers</i>, non-negative integers which each name a unique
 document in the index.  These document numbers are ephemeral--they may change
 as documents are added to and deleted from an index.  Clients should thus not
 rely on a given document having the same number between sessions.

 <p>
 <b>NOTE</b>: for backwards API compatibility, several methods are not listed 
 as abstract, but have no useful implementations in this base class and 
 instead always throw UnsupportedOperationException.  Subclasses are 
 strongly encouraged to override these methods, but in many cases may not 
 need to.
 </p>

 <p>

 <a name="thread-safety"></a><p><b>NOTE</b>: {@link
 IndexReader} instances are completely thread
 safe, meaning multiple threads can call any of its methods,
 concurrently.  If your application requires external
 synchronization, you should <b>not</b> synchronize on the
 <code>IndexReader</code> instance; use your own
 (non-Lucene) objects instead.
*/
public abstract class IndexReader implements Closeable {

  /**
   * A custom listener that's invoked when the IndexReader
   * is closed.
   *
   * @lucene.experimental
   */
  public static interface ReaderClosedListener {
    public void onClose(IndexReader reader);
  }

  private final Set<ReaderClosedListener> readerClosedListeners = 
      Collections.synchronizedSet(new LinkedHashSet<ReaderClosedListener>());

  /** Expert: adds a {@link ReaderClosedListener}.  The
   * provided listener will be invoked when this reader is closed.
   *
   * @lucene.experimental */
  public final void addReaderClosedListener(ReaderClosedListener listener) {
    ensureOpen();
    readerClosedListeners.add(listener);
  }

  /** Expert: remove a previously added {@link ReaderClosedListener}.
   *
   * @lucene.experimental */
  public final void removeReaderClosedListener(ReaderClosedListener listener) {
    ensureOpen();
    readerClosedListeners.remove(listener);
  }

  private final void notifyReaderClosedListeners() {
    synchronized(readerClosedListeners) {
      for(ReaderClosedListener listener : readerClosedListeners) {
        listener.onClose(this);
      }
    }
  }

  private volatile boolean closed;
  
  private final AtomicInteger refCount = new AtomicInteger();

  static int DEFAULT_TERMS_INDEX_DIVISOR = 1;

  /** Expert: returns the current refCount for this reader */
  public final int getRefCount() {
    // NOTE: don't ensureOpen, so that callers can see
    // refCount is 0 (reader is closed)
    return refCount.get();
  }
  
  /**
   * Expert: increments the refCount of this IndexReader
   * instance.  RefCounts are used to determine when a
   * reader can be closed safely, i.e. as soon as there are
   * no more references.  Be sure to always call a
   * corresponding {@link #decRef}, in a finally clause;
   * otherwise the reader may never be closed.  Note that
   * {@link #close} simply calls decRef(), which means that
   * the IndexReader will not really be closed until {@link
   * #decRef} has been called for all outstanding
   * references.
   *
   * @see #decRef
   * @see #tryIncRef
   */
  public final void incRef() {
    ensureOpen();
    refCount.incrementAndGet();
  }
  
  /**
   * Expert: increments the refCount of this IndexReader
   * instance only if the IndexReader has not been closed yet
   * and returns <code>true</code> iff the refCount was
   * successfully incremented, otherwise <code>false</code>.
   * If this method returns <code>false</code> the reader is either
   * already closed or is currently been closed. Either way this
   * reader instance shouldn't be used by an application unless
   * <code>true</code> is returned.
   * <p>
   * RefCounts are used to determine when a
   * reader can be closed safely, i.e. as soon as there are
   * no more references.  Be sure to always call a
   * corresponding {@link #decRef}, in a finally clause;
   * otherwise the reader may never be closed.  Note that
   * {@link #close} simply calls decRef(), which means that
   * the IndexReader will not really be closed until {@link
   * #decRef} has been called for all outstanding
   * references.
   *
   * @see #decRef
   * @see #incRef
   */
  public final boolean tryIncRef() {
    int count;
    while ((count = refCount.get()) > 0) {
      if (refCount.compareAndSet(count, count+1)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Expert: decreases the refCount of this IndexReader
   * instance.  If the refCount drops to 0, then this
   * reader is closed.  If an exception is hit, the refCount
   * is unchanged.
   *
   * @throws IOException in case an IOException occurs in  doClose()
   *
   * @see #incRef
   */
  public final void decRef() throws IOException {
    ensureOpen();
    final int rc = refCount.decrementAndGet();
    if (rc == 0) {
      boolean success = false;
      try {
        doClose();
        success = true;
      } finally {
        if (!success) {
          // Put reference back on failure
          refCount.incrementAndGet();
        }
      }
      notifyReaderClosedListeners();
    } else if (rc < 0) {
      throw new IllegalStateException("too many decRef calls: refCount is " + rc + " after decrement");
    }
  }
  
  protected IndexReader() { 
    refCount.set(1);
  }
  
  /**
   * @throws AlreadyClosedException if this IndexReader is closed
   */
  protected final void ensureOpen() throws AlreadyClosedException {
    if (refCount.get() <= 0) {
      throw new AlreadyClosedException("this IndexReader is closed");
    }
  }
  
  /** Returns a IndexReader reading the index in the given
   *  Directory
   * @param directory the index directory
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   */
  public static DirectoryReader open(final Directory directory) throws CorruptIndexException, IOException {
    return DirectoryReader.open(directory, null, DEFAULT_TERMS_INDEX_DIVISOR);
  }
  
  /** Expert: Returns a IndexReader reading the index in the given
   *  Directory with the given termInfosIndexDivisor.
   * @param directory the index directory
   * @param termInfosIndexDivisor Subsamples which indexed
   *  terms are loaded into RAM. This has the same effect as {@link
   *  IndexWriterConfig#setTermIndexInterval} except that setting
   *  must be done at indexing time while this setting can be
   *  set per reader.  When set to N, then one in every
   *  N*termIndexInterval terms in the index is loaded into
   *  memory.  By setting this to a value > 1 you can reduce
   *  memory usage, at the expense of higher latency when
   *  loading a TermInfo.  The default value is 1.  Set this
   *  to -1 to skip loading the terms index entirely.
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   */
  public static DirectoryReader open(final Directory directory, int termInfosIndexDivisor) throws CorruptIndexException, IOException {
    return DirectoryReader.open(directory, null, termInfosIndexDivisor);
  }
  
  /**
   * Open a near real time IndexReader from the {@link org.apache.lucene.index.IndexWriter}.
   *
   * @param writer The IndexWriter to open from
   * @param applyAllDeletes If true, all buffered deletes will
   * be applied (made visible) in the returned reader.  If
   * false, the deletes are not applied but remain buffered
   * (in IndexWriter) so that they will be applied in the
   * future.  Applying deletes can be costly, so if your app
   * can tolerate deleted documents being returned you might
   * gain some performance by passing false.
   * @return The new IndexReader
   * @throws CorruptIndexException
   * @throws IOException if there is a low-level IO error
   *
   * @see #openIfChanged(IndexReader,IndexWriter,boolean)
   *
   * @lucene.experimental
   */
  public static DirectoryReader open(final IndexWriter writer, boolean applyAllDeletes) throws CorruptIndexException, IOException {
    return writer.getReader(applyAllDeletes);
  }

  /** Expert: returns an IndexReader reading the index in the given
   *  {@link IndexCommit}.
   * @param commit the commit point to open
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   */
  public static DirectoryReader open(final IndexCommit commit) throws CorruptIndexException, IOException {
    return DirectoryReader.open(commit.getDirectory(), commit, DEFAULT_TERMS_INDEX_DIVISOR);
  }


  /** Expert: returns an IndexReader reading the index in the given
   *  {@link IndexCommit} and termInfosIndexDivisor.
   * @param commit the commit point to open
   * @param termInfosIndexDivisor Subsamples which indexed
   *  terms are loaded into RAM. This has the same effect as {@link
   *  IndexWriterConfig#setTermIndexInterval} except that setting
   *  must be done at indexing time while this setting can be
   *  set per reader.  When set to N, then one in every
   *  N*termIndexInterval terms in the index is loaded into
   *  memory.  By setting this to a value > 1 you can reduce
   *  memory usage, at the expense of higher latency when
   *  loading a TermInfo.  The default value is 1.  Set this
   *  to -1 to skip loading the terms index entirely.
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   */
  public static DirectoryReader open(final IndexCommit commit, int termInfosIndexDivisor) throws CorruptIndexException, IOException {
    return DirectoryReader.open(commit.getDirectory(), commit, termInfosIndexDivisor);
  }

  /**
   * If the index has changed since the provided reader was
   * opened, open and return a new reader; else, return
   * null.  The new reader, if not null, will be the same
   * type of reader as the previous one, ie an NRT reader
   * will open a new NRT reader, a MultiReader will open a
   * new MultiReader,  etc.
   *
   * <p>This method is typically far less costly than opening a
   * fully new <code>IndexReader</code> as it shares
   * resources (for example sub-readers) with the provided
   * <code>IndexReader</code>, when possible.
   *
   * <p>The provided reader is not closed (you are responsible
   * for doing so); if a new reader is returned you also
   * must eventually close it.  Be sure to never close a
   * reader while other threads are still using it; see
   * {@link SearcherManager} to simplify managing this.
   *
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   * @return null if there are no changes; else, a new
   * IndexReader instance which you must eventually close
   */  
  public static CompositeIndexReader openIfChanged(CompositeIndexReader oldReader) throws IOException {
    final CompositeIndexReader newReader = oldReader.doOpenIfChanged();
    assert newReader != oldReader;
    return newReader;
  }

  /**
   * If the IndexCommit differs from what the
   * provided reader is searching, open and return a new
   * reader; else, return null.
   *
   * @see #openIfChanged(IndexReader)
   */
  public static CompositeIndexReader openIfChanged(CompositeIndexReader oldReader, IndexCommit commit) throws IOException {
    final CompositeIndexReader newReader = oldReader.doOpenIfChanged(commit);
    assert newReader != oldReader;
    return newReader;
  }

  /**
   * Expert: If there changes (committed or not) in the
   * {@link IndexWriter} versus what the provided reader is
   * searching, then open and return a new
   * IndexReader searching both committed and uncommitted
   * changes from the writer; else, return null (though, the
   * current implementation never returns null).
   *
   * <p>This provides "near real-time" searching, in that
   * changes made during an {@link IndexWriter} session can be
   * quickly made available for searching without closing
   * the writer nor calling {@link IndexWriter#commit}.
   *
   * <p>It's <i>near</i> real-time because there is no hard
   * guarantee on how quickly you can get a new reader after
   * making changes with IndexWriter.  You'll have to
   * experiment in your situation to determine if it's
   * fast enough.  As this is a new and experimental
   * feature, please report back on your findings so we can
   * learn, improve and iterate.</p>
   *
   * <p>The very first time this method is called, this
   * writer instance will make every effort to pool the
   * readers that it opens for doing merges, applying
   * deletes, etc.  This means additional resources (RAM,
   * file descriptors, CPU time) will be consumed.</p>
   *
   * <p>For lower latency on reopening a reader, you should
   * call {@link IndexWriterConfig#setMergedSegmentWarmer} to
   * pre-warm a newly merged segment before it's committed
   * to the index.  This is important for minimizing
   * index-to-search delay after a large merge.  </p>
   *
   * <p>If an addIndexes* call is running in another thread,
   * then this reader will only search those segments from
   * the foreign index that have been successfully copied
   * over, so far.</p>
   *
   * <p><b>NOTE</b>: Once the writer is closed, any
   * outstanding readers may continue to be used.  However,
   * if you attempt to reopen any of those readers, you'll
   * hit an {@link AlreadyClosedException}.</p>
   *
   * @return IndexReader that covers entire index plus all
   * changes made so far by this IndexWriter instance, or
   * null if there are no new changes
   *
   * @param writer The IndexWriter to open from
   *
   * @param applyAllDeletes If true, all buffered deletes will
   * be applied (made visible) in the returned reader.  If
   * false, the deletes are not applied but remain buffered
   * (in IndexWriter) so that they will be applied in the
   * future.  Applying deletes can be costly, so if your app
   * can tolerate deleted documents being returned you might
   * gain some performance by passing false.
   *
   * @throws IOException
   *
   * @lucene.experimental
   */
  public static CompositeIndexReader openIfChanged(CompositeIndexReader oldReader, IndexWriter writer, boolean applyAllDeletes) throws IOException {
    final CompositeIndexReader newReader = oldReader.doOpenIfChanged(writer, applyAllDeletes);
    assert newReader != oldReader;
    return newReader;
  }

  /**
   * Returns the directory associated with this index.  The Default 
   * implementation returns the directory specified by subclasses when 
   * delegating to the IndexReader(Directory) constructor, or throws an 
   * UnsupportedOperationException if one was not specified.
   * @throws UnsupportedOperationException if no directory
   */
  public Directory directory() {
    ensureOpen();
    throw new UnsupportedOperationException("This reader does not support this method.");  
  }

  /**
   * Returns the time the index in the named directory was last modified. 
   * Do not use this to check whether the reader is still up-to-date, use
   * {@link #isCurrent()} instead. 
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   */
  public static long lastModified(final Directory directory2) throws CorruptIndexException, IOException {
    return ((Long) new SegmentInfos.FindSegmentsFile(directory2) {
        @Override
        public Object doBody(String segmentFileName) throws IOException {
          return Long.valueOf(directory2.fileModified(segmentFileName));
        }
      }.run()).longValue();
  }
  
  /**
   * Reads version number from segments files. The version number is
   * initialized with a timestamp and then increased by one for each change of
   * the index.
   * 
   * @param directory where the index resides.
   * @return version number.
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   */
  public static long getCurrentVersion(Directory directory) throws CorruptIndexException, IOException {
    return SegmentInfos.readCurrentVersion(directory);
  }
  
  /**
   * Reads commitUserData, previously passed to {@link
   * IndexWriter#commit(Map)}, from current index
   * segments file.  This will return null if {@link
   * IndexWriter#commit(Map)} has never been called for
   * this index.
   * 
   * @param directory where the index resides.
   * @return commit userData.
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   *
   * @see #getCommitUserData()
   */
  public static Map<String, String> getCommitUserData(Directory directory) throws CorruptIndexException, IOException {
    return SegmentInfos.readCurrentUserData(directory);
  }

  /** Retrieve term vectors for this document, or null if
   *  term vectors were not indexed.  The returned Fields
   *  instance acts like a single-document inverted index
   *  (the docID will be 0). */
  public abstract Fields getTermVectors(int docID)
          throws IOException;

  /** Retrieve term vector for this document and field, or
   *  null if term vectors were not indexed.  The returned
   *  Fields instance acts like a single-document inverted
   *  index (the docID will be 0). */
  public final Terms getTermVector(int docID, String field)
    throws IOException {
    Fields vectors = getTermVectors(docID);
    if (vectors == null) {
      return null;
    }
    return vectors.terms(field);
  }

  /**
   * Returns <code>true</code> if an index exists at the specified directory.
   * @param  directory the directory to check for an index
   * @return <code>true</code> if an index exists; <code>false</code> otherwise
   * @throws IOException if there is a problem with accessing the index
   */
  public static boolean indexExists(Directory directory) throws IOException {
    try {
      new SegmentInfos().read(directory);
      return true;
    } catch (IOException ioe) {
      return false;
    }
  }

  /** Returns the number of documents in this index. */
  public abstract int numDocs();

  /** Returns one greater than the largest possible document number.
   * This may be used to, e.g., determine how big to allocate an array which
   * will have an element for every document number in an index.
   */
  public abstract int maxDoc();

  /** Returns the number of deleted documents. */
  public final int numDeletedDocs() {
    return maxDoc() - numDocs();
  }

  /** Expert: visits the fields of a stored document, for
   *  custom processing/loading of each field.  If you
   *  simply want to load all fields, use {@link
   *  #document(int)}.  If you want to load a subset, use
   *  {@link DocumentStoredFieldVisitor}.  */
  public abstract void document(int docID, StoredFieldVisitor visitor) throws CorruptIndexException, IOException;
  
  /**
   * Returns the stored fields of the <code>n</code><sup>th</sup>
   * <code>Document</code> in this index.  This is just
   * sugar for using {@link DocumentStoredFieldVisitor}.
   * <p>
   * <b>NOTE:</b> for performance reasons, this method does not check if the
   * requested document is deleted, and therefore asking for a deleted document
   * may yield unspecified results. Usually this is not required, however you
   * can test if the doc is deleted by checking the {@link
   * Bits} returned from {@link MultiFields#getLiveDocs}.
   *
   * <b>NOTE:</b> only the content of a field is returned,
   * if that field was stored during indexing.  Metadata
   * like boost, omitNorm, IndexOptions, tokenized, etc.,
   * are not preserved.
   * 
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   */
  // TODO: we need a separate StoredField, so that the
  // Document returned here contains that class not
  // IndexableField
  public final Document document(int docID) throws CorruptIndexException, IOException {
    ensureOpen();
    final DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor();
    document(docID, visitor);
    return visitor.getDocument();
  }

  /**
   * Like {@link #document(int)} but only loads the specified
   * fields.  Note that this is simply sugar for {@link
   * DocumentStoredFieldVisitor#DocumentStoredFieldVisitor(Set)}.
   */
  public final Document document(int docID, Set<String> fieldsToLoad) throws CorruptIndexException, IOException {
    final DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor(fieldsToLoad);
    document(docID, visitor);
    return visitor.getDocument();
  }

  /** Returns true if any documents have been deleted */
  public abstract boolean hasDeletions();

  /**
   * Closes files associated with this index.
   * Also saves any new deletions to disk.
   * No other methods should be called after this has been called.
   * @throws IOException if there is a low-level IO error
   */
  public final synchronized void close() throws IOException {
    if (!closed) {
      decRef();
      closed = true;
    }
  }
  
  /** Implements close. */
  protected abstract void doClose() throws IOException;

  /** Returns all commit points that exist in the Directory.
   *  Normally, because the default is {@link
   *  KeepOnlyLastCommitDeletionPolicy}, there would be only
   *  one commit point.  But if you're using a custom {@link
   *  IndexDeletionPolicy} then there could be many commits.
   *  Once you have a given commit, you can open a reader on
   *  it by calling {@link IndexReader#open(IndexCommit)}
   *  There must be at least one commit in
   *  the Directory, else this method throws {@link
   *  IndexNotFoundException}.  Note that if a commit is in
   *  progress while this method is running, that commit
   *  may or may not be returned.
   *  
   *  @return a sorted list of {@link IndexCommit}s, from oldest 
   *  to latest. */
  public static List<IndexCommit> listCommits(Directory dir) throws IOException {
    return DirectoryReader.listCommits(dir);
  }

  /**
   * Expert: Returns a the root {@link ReaderContext} for this
   * {@link IndexReader}'s sub-reader tree. Iff this reader is composed of sub
   * readers ,ie. this reader being a composite reader, this method returns a
   * {@link CompositeReaderContext} holding the reader's direct children as well as a
   * view of the reader tree's atomic leaf contexts. All sub-
   * {@link ReaderContext} instances referenced from this readers top-level
   * context are private to this reader and are not shared with another context
   * tree. For example, IndexSearcher uses this API to drive searching by one
   * atomic leaf reader at a time. If this reader is not composed of child
   * readers, this method returns an {@link AtomicReaderContext}.
   * <p>
   * Note: Any of the sub-{@link CompositeReaderContext} instances reference from this
   * top-level context holds a <code>null</code> {@link CompositeReaderContext#leaves}
   * reference. Only the top-level context maintains the convenience leaf-view
   * for performance reasons.
   * 
   * @lucene.experimental
   */
  public abstract ReaderContext getTopReaderContext();

  /** Expert: Returns a key for this IndexReader, so FieldCache/CachingWrapperFilter can find
   * it again.
   * This key must not have equals()/hashCode() methods, so &quot;equals&quot; means &quot;identical&quot;. */
  public Object getCoreCacheKey() {
    // Don't can ensureOpen since FC calls this (to evict)
    // on close
    return this;
  }

  /** Expert: Returns a key for this IndexReader that also includes deletions,
   * so FieldCache/CachingWrapperFilter can find it again.
   * This key must not have equals()/hashCode() methods, so &quot;equals&quot; means &quot;identical&quot;. */
  public Object getCombinedCoreAndDeletesKey() {
    // Don't can ensureOpen since FC calls this (to evict)
    // on close
    return this;
  }

  /** For IndexReader implementations that use
   *  TermInfosReader to read terms, this returns the
   *  current indexDivisor as specified when the reader was
   *  opened.
   */
  public int getTermInfosIndexDivisor() {
    throw new UnsupportedOperationException("This reader does not support this method.");
  }
  
  // nocommit: remove generics and add a typed (overloaded) getter method instead instance fields with "R reader"
  /**
   * A struct like class that represents a hierarchical relationship between
   * {@link IndexReader} instances. 
   * @lucene.experimental
   */
  public static abstract class ReaderContext {
    /** The reader context for this reader's immediate parent, or null if none */
    public final CompositeReaderContext parent;
    /** <code>true</code> if this context struct represents the top level reader within the hierarchical context */
    public final boolean isTopLevel;
    /** the doc base for this reader in the parent, <tt>0</tt> if parent is null */
    public final int docBaseInParent;
    /** the ord for this reader in the parent, <tt>0</tt> if parent is null */
    public final int ordInParent;
    
    ReaderContext(CompositeReaderContext parent, int ordInParent, int docBaseInParent) {
      this.parent = parent;
      this.docBaseInParent = docBaseInParent;
      this.ordInParent = ordInParent;
      this.isTopLevel = parent==null;
    }
    
    public abstract IndexReader reader();
    
    /**
     * Returns the context's leaves if this context is a top-level context
     * otherwise <code>null</code>.
     * <p>
     * Note: this is convenience method since leaves can always be obtained by
     * walking the context tree.
     */
    public abstract AtomicReaderContext[] leaves();
    
    /**
     * Returns the context's children iff this context is a composite context
     * otherwise <code>null</code>.
     * <p>
     * Note: this method is a convenience method to prevent
     * <code>instanceof</code> checks and type-casts to
     * {@link CompositeReaderContext}.
     */
    public abstract ReaderContext[] children();
  }
}
