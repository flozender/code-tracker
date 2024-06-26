package org.apache.lucene.search;

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

import org.apache.lucene.index.IndexReader;

/**
 * <p>Expert: Collectors are primarily meant to be used to
 * gather raw results from a search, and implement sorting
 * or custom result filtering, collation, etc. </p>
 *
 * <p>As of 2.9, this class replaces the deprecated
 * HitCollector, and offers an API for efficient collection
 * of hits across sequential {@link IndexReader}s. {@link
 * IndexSearcher} advances the collector through each of the
 * sub readers, in an arbitrary order. This results in a
 * higher performance means of collection.</p>
 *
 * <p>Lucene's core collectors are derived from Collector.
 * Likely your application can use one of these classes, or
 * subclass {@link TopDocsCollector}, instead of
 * implementing Collector directly:
 *
 * <ul>
 *      
 *   <li>{@link TopDocsCollector} is an abstract base class
 *   that assumes you will retrieve the top N docs,
 *   according to some criteria, after collection is
 *   done.  </li>
 *
 *   <li>{@link TopScoreDocCollector} is a concrete subclass
 *   {@link TopDocsCollector} and sorts according to score +
 *   docID.  This is used internally by the {@link
 *   IndexSearcher} search methods that do not take an
 *   explicit {@link Sort}. It is likely the most frequently
 *   used collector.</li>
 *
 *   <li>{@link TopFieldCollector} subclasses {@link
 *   TopDocsCollector} and sorts according to a specified
 *   {@link Sort} object (sort by field).  This is used
 *   internally by the {@link IndexSearcher} search methods
 *   that take an explicit {@link Sort}.
 *
 *   <li>{@link TimeLimitingCollector}, which wraps any other
 *   Collector and aborts the search if it's taken too much
 *   time, will subclass Collector in 3.0 (presently it
 *   subclasses the deprecated HitCollector).</li>
 *
 *   <li>{@link PositiveScoresOnlyCollector} wraps any other
 *   Collector and prevents collection of hits whose score
 *   is &lt;= 0.0</li>
 *
 * </ul>
 *
 * <p>Collector decouples the score from the collected doc:
 * the score computation is skipped entirely if it's not
 * needed.  Collectors that do need the score should
 * implement the {@link #setScorer} method, to hold onto the
 * passed {@link Scorer} instance, and call {@link
 * Scorer#score()} within the collect method to compute the
 * current hit's score.  If your collector may request the
 * score for a single hit multiple times, you should use
 * {@link ScoreCachingWrappingScorer}. </p>
 * 
 * <p><b>NOTE:</b> The doc that is passed to the collect
 * method is relative to the current reader. If your
 * collector needs to resolve this to the docID space of the
 * Multi*Reader, you must re-base it by recording the
 * docBase from the most recent setNextReader call.  Here's
 * a simple example showing how to collect docIDs into a
 * BitSet:</p>
 * 
 * <pre>
 * Searcher searcher = new IndexSearcher(indexReader);
 * final BitSet bits = new BitSet(indexReader.maxDoc());
 * searcher.search(query, new Collector() {
 *   private int docBase;
 * 
 *   // ignore scorer
 *   public void setScorer(Scorer scorer) {
 *   }
 * 
 *   public void collect(int doc) {
 *     bits.set(doc + docBase);
 *   }
 * 
 *   public void setNextReader(IndexReader reader, int docBase) {
 *     this.docBase = docBase;
 *   }
 * });
 * </pre>
 *
 * <p>Not all collectors will need to rebase the docID.  For
 * example, a collector that simply counts the total number
 * of hits would skip it.</p>
 * 
 * <p><b>NOTE:</b> Prior to 2.9, Lucene silently filtered
 * out hits with score <= 0.  As of 2.9, the core Collectors
 * no longer do that.  It's very unusual to have such hits
 * (a negative query boost, or function query returning
 * negative custom scores, could cause it to happen).  If
 * you need that behavior, use {@link
 * PositiveScoresOnlyCollector}.</p>
 *
 * <p><b>NOTE:</b> This API is experimental and might change
 * in incompatible ways in the next release.</p>
 * 
 * @since 2.9
 */
public abstract class Collector {
  
  /**
   * Called before successive calls to {@link #collect(int)}. Implementations
   * that need the score of the current document (passed-in to
   * {@link #collect(int)}), should save the passed-in Scorer and call
   * scorer.score() when needed.
   */
  public abstract void setScorer(Scorer scorer) throws IOException;
  
  /**
   * Called once for every document matching a query, with the unbased document
   * number.
   * 
   * <p>
   * Note: This is called in an inner search loop. For good search performance,
   * implementations of this method should not call {@link Searcher#doc(int)} or
   * {@link org.apache.lucene.index.IndexReader#document(int)} on every hit.
   * Doing so can slow searches by an order of magnitude or more.
   */
  public abstract void collect(int doc) throws IOException;

  /**
   * Called before collecting from each IndexReader. All doc ids in
   * {@link #collect(int)} will correspond to reader.
   * 
   * Add docBase to the current IndexReaders internal document id to re-base ids
   * in {@link #collect(int)}.
   * 
   * @param reader
   *          next IndexReader
   * @param docBase
   */
  public abstract void setNextReader(IndexReader reader, int docBase) throws IOException;

  /**
   * Returns true iff this {@link Collector} can accept documents given to
   * {@link #collect(int)} out of order.
   * <p>
   * NOTE: some collectors can work in either mode, with a more efficient
   * implementation for in-order docs collection. If your collector can work in
   * either mode, it is recommended that you create two variants of it, since
   * some queries work much faster if out-of-order collection is supported by a
   * {@link Collector}.
   */
  public abstract boolean acceptsDocsOutOfOrder();
  
}
