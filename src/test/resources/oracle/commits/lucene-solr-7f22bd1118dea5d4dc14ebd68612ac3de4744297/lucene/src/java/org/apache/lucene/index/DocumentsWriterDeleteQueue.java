package org.apache.lucene.index;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.search.Query;

/**
 * {@link DocumentsWriterDeleteQueue} is a non-blocking linked pending deletes
 * queue. In contrast to other queue implementation we only maintain only the
 * tail of the queue. A delete queue is always used in a context of a set of
 * DWPT and a global delete pool. Each of the DWPT and the global pool need to
 * maintain their 'own' head of the queue. The difference between the DWPT and
 * the global pool is that the DWPT starts maintaining a head once it has added
 * its first document since for its segments private deletes only the deletes
 * after that document are relevant. The global pool instead starts maintaining
 * the head once this instance is created by taking the sentinel instance as its
 * initial head.
 * <p>
 * Since each {@link DeleteSlice} maintains its own head and the list is only
 * single linked the garbage collector takes care of pruning the list for us.
 * All nodes in the list that are still relevant should be either directly or
 * indirectly referenced by one of the DWPT's private {@link DeleteSlice} or by
 * the global {@link BufferedDeletes} slice.
 * <p>
 * Each DWPT as well as the global delete pool maintain their private
 * DeleteSlice instance. In the DWPT case updating a slice is equivalent to
 * atomically finishing the document. The slice update guarantees a happens
 * before relationship to all other updates in the same indexing session. When a
 * DWPT updates a document it
 * 
 * <ol>
 * <li>consumes a document finishes its processing</li>
 * <li>updates its private {@link DeleteSlice} either by calling
 * {@link #updateSlice(DeleteSlice)} or {@link #add(Term, DeleteSlice)} (if the
 * document has a delTerm)</li>
 * <li>applies all deletes in the slice to its private {@link BufferedDeletes}
 * and resets it</li>
 * <li>increments its internal document id</li>
 * </ol>
 * 
 * The DWPT also doesn't apply its current documents delete term until it has
 * updated its delete slice which ensures the consistency of the update. if the
 * update fails before the DeleteSlice could have been updated the deleteTerm
 * will also not be added to its private deletes neither to the global deletes.
 * 
 */
final class DocumentsWriterDeleteQueue {

  private volatile Node tail;

  private static final AtomicReferenceFieldUpdater<DocumentsWriterDeleteQueue, Node> tailUpdater = AtomicReferenceFieldUpdater
      .newUpdater(DocumentsWriterDeleteQueue.class, Node.class, "tail");

  private final DeleteSlice globalSlice;
  private final BufferedDeletes globalBufferedDeletes;
  /* only acquired to update the global deletes */
  private final ReentrantLock globalBufferLock = new ReentrantLock();
  
  DocumentsWriterDeleteQueue() {
    this(new BufferedDeletes(false));
  }

  DocumentsWriterDeleteQueue(BufferedDeletes globalBufferedDeletes) {
    this.globalBufferedDeletes = globalBufferedDeletes;
    /*
     * we use a sentinel instance as our initial tail. No slice will ever try to
     * apply this tail since the head is always omitted.
     */
    tail = new Node(null); // sentinel
    globalSlice = new DeleteSlice(tail);
  }

  void addDelete(Query... queries) {
    add(new QueryArrayNode(queries));
    tryApplyGlobalSlice();
  }

  void addDelete(Term... terms) {
    add(new TermArrayNode(terms));
    tryApplyGlobalSlice();
  }

  /**
   * invariant for document update
   */
  void add(Term term, DeleteSlice slice) {
    final TermNode termNode = new TermNode(term);
    add(termNode);
    /*
     * this is an update request where the term is the updated documents
     * delTerm. in that case we need to guarantee that this insert is atomic
     * with regards to the given delete slice. This means if two threads try to
     * update the same document with in turn the same delTerm one of them must
     * win. By taking the node we have created for our del term as the new tail
     * it is guaranteed that if another thread adds the same right after us we
     * will apply this delete next time we update our slice and one of the two
     * competing updates wins!
     */
    slice.sliceTail = termNode;
    assert slice.sliceHead != slice.sliceTail : "slice head and tail must differ after add";
    tryApplyGlobalSlice(); // TODO doing this each time is not necessary maybe
    // we can do it just every n times or so?
  }

  void add(Node item) {
    /*
     * this non-blocking / 'wait-free' linked list add was inspired by Apache
     * Harmony's ConcurrentLinkedQueue Implementation.
     */
    while (true) {
      final Node currentTail = this.tail;
      final Node tailNext = currentTail.next;
      if (tail == currentTail) {
        if (tailNext != null) {
          /*
           * we are in intermediate state here. the tails next pointer has been
           * advanced but the tail itself might not be updated yet. help to
           * advance the tail and try again updating it.
           */
          tailUpdater.compareAndSet(this, currentTail, tailNext); // can fail
        } else {
          /*
           * we are in quiescent state and can try to insert the item to the
           * current tail if we fail to insert we just retry the operation since
           * somebody else has already added its item
           */
          if (currentTail.casNext(null, item)) {
            /*
             * now that we are done we need to advance the tail while another
             * thread could have advanced it already so we can ignore the return
             * type of this CAS call
             */
            tailUpdater.compareAndSet(this, currentTail, item);
            return;
          }
        }
      }
    }
  }

  boolean anyChanges() {
    globalBufferLock.lock();
    try {
      return !globalSlice.isEmpty() || globalBufferedDeletes.any();
    } finally {
      globalBufferLock.unlock();
    }
  }

  void tryApplyGlobalSlice() {
    if (globalBufferLock.tryLock()) {
      /*
       * the global buffer must be locked but we don't need to upate them if
       * there is an update going on right now. It is sufficient to apply the
       * deletes that have been added after the current in-flight global slices
       * tail the next time we can get the lock!
       */
      try {
        if (updateSlice(globalSlice)) {
          globalSlice.apply(globalBufferedDeletes, BufferedDeletes.MAX_INT);

        }
      } finally {
        globalBufferLock.unlock();
      }
    }
  }

  FrozenBufferedDeletes freezeGlobalBuffer(DeleteSlice callerSlice) {
    globalBufferLock.lock();
    /*
     * here we are freezing the global buffer so we need to lock it, apply all
     * deletes in the queue and reset the global slice to let the GC prune the
     * queue.
     */
    final Node currentTail = tail; // take the current tail make this local any
    // changes after this call are applied later
    // and not relevant here
    if (callerSlice != null) {
      // update the callers slices so we are on the same page
      callerSlice.sliceTail = currentTail;
    }
    try {
      if (globalSlice.sliceTail != currentTail) {
        globalSlice.sliceTail = currentTail;
        globalSlice.apply(globalBufferedDeletes, BufferedDeletes.MAX_INT);
      }

      final FrozenBufferedDeletes packet = new FrozenBufferedDeletes(
          globalBufferedDeletes, false);
      globalBufferedDeletes.clear();
      return packet;
    } finally {
      globalBufferLock.unlock();
    }
  }

  DeleteSlice newSlice() {
    return new DeleteSlice(tail);
  }

  boolean updateSlice(DeleteSlice slice) {
    if (slice.sliceTail != tail) { // if we are the same just
      slice.sliceTail = tail;
      return true;
    }
    return false;
  }

  static class DeleteSlice {
    // no need to be volatile, slices are only access by one thread!
    Node sliceHead; // we don't apply this one
    Node sliceTail;

    DeleteSlice(Node currentTail) {
      assert currentTail != null;
      /*
       * Initially this is a 0 length slice pointing to the 'current' tail of
       * the queue. Once we update the slice we only need to assign the tail and
       * have a new slice
       */
      sliceHead = sliceTail = currentTail;
    }

    void apply(BufferedDeletes del, int docIDUpto) {
      if (sliceHead == sliceTail) {
        // 0 length slice
        return;
      }
      /*
       * when we apply a slice we take the head and get its next as our first
       * item to apply and continue until we applied the tail. If the head and
       * tail in this slice are not equal then there will be at least one more
       * non-null node in the slice!
       */
      Node current = sliceHead;
      do {
        current = current.next;
        assert current != null : "slice property violated between the head on the tail must not be a null node";
        current.apply(del, docIDUpto);
      } while (current != sliceTail);
      reset();
    }

    void reset() {
      // resetting to a 0 length slice
      sliceHead = sliceTail;
    }

    /**
     * Returns <code>true</code> iff the given item is identical to the item
     * hold by the slices tail, otherwise <code>false</code>.
     */
    boolean isTailItem(Object item) {
      return sliceTail.item == item;
    }

    boolean isEmpty() {
      return sliceHead == sliceTail;
    }
  }

  public int numGlobalTermDeletes() {
    return globalBufferedDeletes.numTermDeletes.get();
  }

  void clear() {
    globalBufferLock.lock();
    try {
      final Node currentTail = tail;
      globalSlice.sliceHead = globalSlice.sliceTail = currentTail;
      globalBufferedDeletes.clear();
    } finally {
      globalBufferLock.unlock();
    }
  }

  private static class Node {
    volatile Node next;
    final Object item;

    private Node(Object item) {
      this.item = item;
    }

    static final AtomicReferenceFieldUpdater<Node, Node> nextUpdater = AtomicReferenceFieldUpdater
        .newUpdater(Node.class, Node.class, "next");

    void apply(BufferedDeletes bufferedDeletes, int docIDUpto) {
      assert false : "sentinel item must never be applied";
    }

    boolean casNext(Node cmp, Node val) {
      return nextUpdater.compareAndSet(this, cmp, val);
    }
  }

  private static final class TermNode extends Node {

    TermNode(Term term) {
      super(term);
    }

    @Override
    void apply(BufferedDeletes bufferedDeletes, int docIDUpto) {
      bufferedDeletes.addTerm((Term) item, docIDUpto);
    }

  }

  private static final class QueryArrayNode extends Node {
    QueryArrayNode(Query[] query) {
      super(query);
    }

    @Override
    void apply(BufferedDeletes bufferedDeletes, int docIDUpto) {
      final Query[] queries = (Query[]) item;
      for (Query query : queries) {
        bufferedDeletes.addQuery(query, docIDUpto);  
      }
    }
  }
  
  private static final class TermArrayNode extends Node {
    TermArrayNode(Term[] term) {
      super(term);
    }

    @Override
    void apply(BufferedDeletes bufferedDeletes, int docIDUpto) {
      final Term[] terms = (Term[]) item;
      for (Term term : terms) {
        bufferedDeletes.addTerm(term, docIDUpto);  
      }
    }
  }


  private boolean forceApplyGlobalSlice() {
    globalBufferLock.lock();
    final Node currentTail = tail;
    try {
      if (globalSlice.sliceTail != currentTail) {
        globalSlice.sliceTail = currentTail;
        globalSlice.apply(globalBufferedDeletes, BufferedDeletes.MAX_INT);
      }
      return globalBufferedDeletes.any();
    } finally {
      globalBufferLock.unlock();
    }
  }

  public int getBufferedDeleteTermsSize() {
    globalBufferLock.lock();
    try {
      forceApplyGlobalSlice();
      return globalBufferedDeletes.terms.size();
    } finally {
      globalBufferLock.unlock();
    }

  }
}
