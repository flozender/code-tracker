package org.apache.lucene.facet;

/*
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
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.FixedBitSet;

/** Collects hits for subsequent faceting.  Once you've run
 *  a search and collect hits into this, instantiate one of
 *  the {@link Facets} subclasses to do the facet
 *  counting.  Use the {@code search} utility methods to
 *  perform an "ordinary" search but also collect into a
 *  {@link Collector}. */
public class FacetsCollector extends SimpleCollector {

  private LeafReaderContext context;
  private Scorer scorer;
  private int totalHits;
  private float[] scores;
  private final boolean keepScores;
  private final List<MatchingDocs> matchingDocs = new ArrayList<>();
  private Docs docs;
  
  /**
   * Used during collection to record matching docs and then return a
   * {@link DocIdSet} that contains them.
   */
  protected static abstract class Docs {
    
    /** Solr constructor. */
    public Docs() {}
    
    /** Record the given document. */
    public abstract void addDoc(int docId) throws IOException;
    
    /** Return the {@link DocIdSet} which contains all the recorded docs. */
    public abstract DocIdSet getDocIdSet();
  }

  /**
   * Holds the documents that were matched in the {@link org.apache.lucene.index.LeafReaderContext}.
   * If scores were required, then {@code scores} is not null.
   */
  public final static class MatchingDocs {
    
    /** Context for this segment. */
    public final LeafReaderContext context;

    /** Which documents were seen. */
    public final DocIdSet bits;

    /** Non-sparse scores array. */
    public final float[] scores;
    
    /** Total number of hits */
    public final int totalHits;

    /** Sole constructor. */
    public MatchingDocs(LeafReaderContext context, DocIdSet bits, int totalHits, float[] scores) {
      this.context = context;
      this.bits = bits;
      this.scores = scores;
      this.totalHits = totalHits;
    }
  }

  /** Default constructor */
  public FacetsCollector() {
    this(false);
  }

  /** Create this; if {@code keepScores} is true then a
   *  float[] is allocated to hold score of all hits. */
  public FacetsCollector(boolean keepScores) {
    this.keepScores = keepScores;
  }
  
  /**
   * Creates a {@link Docs} to record hits. The default uses {@link FixedBitSet}
   * to record hits and you can override to e.g. record the docs in your own
   * {@link DocIdSet}.
   */
  protected Docs createDocs(final int maxDoc) {
    return new Docs() {
      private final FixedBitSet bits = new FixedBitSet(maxDoc);
      
      @Override
      public void addDoc(int docId) throws IOException {
        bits.set(docId);
      }
      
      @Override
      public DocIdSet getDocIdSet() {
        return new BitDocIdSet(bits);
      }
    };
  }

  /** True if scores were saved. */
  public final boolean getKeepScores() {
    return keepScores;
  }
  
  /**
   * Returns the documents matched by the query, one {@link MatchingDocs} per
   * visited segment.
   */
  public List<MatchingDocs> getMatchingDocs() {
    if (docs != null) {
      matchingDocs.add(new MatchingDocs(this.context, docs.getDocIdSet(), totalHits, scores));
      docs = null;
      scores = null;
      context = null;
    }

    return matchingDocs;
  }

  @Override
  public final void collect(int doc) throws IOException {
    docs.addDoc(doc);
    if (keepScores) {
      if (totalHits >= scores.length) {
        float[] newScores = new float[ArrayUtil.oversize(totalHits + 1, 4)];
        System.arraycopy(scores, 0, newScores, 0, totalHits);
        scores = newScores;
      }
      scores[totalHits] = scorer.score();
    }
    totalHits++;
  }

  @Override
  public boolean needsScores() {
    return true;
  }

  @Override
  public final void setScorer(Scorer scorer) throws IOException {
    this.scorer = scorer;
  }
    
  @Override
  protected void doSetNextReader(LeafReaderContext context) throws IOException {
    if (docs != null) {
      matchingDocs.add(new MatchingDocs(this.context, docs.getDocIdSet(), totalHits, scores));
    }
    docs = createDocs(context.reader().maxDoc());
    totalHits = 0;
    if (keepScores) {
      scores = new float[64]; // some initial size
    }
    this.context = context;
  }

  /** Utility method, to search and also collect all hits
   *  into the provided {@link Collector}. */
  public static TopDocs search(IndexSearcher searcher, Query q, int n, Collector fc) throws IOException {
    return doSearch(searcher, null, q, null, n, null, false, false, fc);
  }

  /** Utility method, to search and also collect all hits
   *  into the provided {@link Collector}. */
  public static TopDocs search(IndexSearcher searcher, Query q, Filter filter, int n, Collector fc) throws IOException {
    return doSearch(searcher, null, q, filter, n, null, false, false, fc);
  }

  /** Utility method, to search and also collect all hits
   *  into the provided {@link Collector}. */
  public static TopFieldDocs search(IndexSearcher searcher, Query q, Filter filter, int n, Sort sort, Collector fc) throws IOException {
    if (sort == null) {
      throw new IllegalArgumentException("sort must not be null");
    }
    return (TopFieldDocs) doSearch(searcher, null, q, filter, n, sort, false, false, fc);
  }

  /** Utility method, to search and also collect all hits
   *  into the provided {@link Collector}. */
  public static TopFieldDocs search(IndexSearcher searcher, Query q, Filter filter, int n, Sort sort, boolean doDocScores, boolean doMaxScore, Collector fc) throws IOException {
    if (sort == null) {
      throw new IllegalArgumentException("sort must not be null");
    }
    return (TopFieldDocs) doSearch(searcher, null, q, filter, n, sort, doDocScores, doMaxScore, fc);
  }

  /** Utility method, to search and also collect all hits
   *  into the provided {@link Collector}. */
  public TopDocs searchAfter(IndexSearcher searcher, ScoreDoc after, Query q, int n, Collector fc) throws IOException {
    return doSearch(searcher, after, q, null, n, null, false, false, fc);
  }

  /** Utility method, to search and also collect all hits
   *  into the provided {@link Collector}. */
  public static TopDocs searchAfter(IndexSearcher searcher, ScoreDoc after, Query q, Filter filter, int n, Collector fc) throws IOException {
    return doSearch(searcher, after, q, filter, n, null, false, false, fc);
  }

  /** Utility method, to search and also collect all hits
   *  into the provided {@link Collector}. */
  public static TopDocs searchAfter(IndexSearcher searcher, ScoreDoc after, Query q, Filter filter, int n, Sort sort, Collector fc) throws IOException {
    if (sort == null) {
      throw new IllegalArgumentException("sort must not be null");
    }
    return doSearch(searcher, after, q, filter, n, sort, false, false, fc);
  }

  /** Utility method, to search and also collect all hits
   *  into the provided {@link Collector}. */
  public static TopDocs searchAfter(IndexSearcher searcher, ScoreDoc after, Query q, Filter filter, int n, Sort sort, boolean doDocScores, boolean doMaxScore, Collector fc) throws IOException {
    if (sort == null) {
      throw new IllegalArgumentException("sort must not be null");
    }
    return doSearch(searcher, after, q, filter, n, sort, doDocScores, doMaxScore, fc);
  }

  private static TopDocs doSearch(IndexSearcher searcher, ScoreDoc after, Query q, Filter filter, int n, Sort sort,
                                  boolean doDocScores, boolean doMaxScore, Collector fc) throws IOException {

    if (filter != null) {
      q = new FilteredQuery(q, filter);
    }

    int limit = searcher.getIndexReader().maxDoc();
    if (limit == 0) {
      limit = 1;
    }
    n = Math.min(n, limit);

    if (after != null && after.doc >= limit) {
      throw new IllegalArgumentException("after.doc exceeds the number of documents in the reader: after.doc="
                                         + after.doc + " limit=" + limit);
    }

    TopDocsCollector<?> hitsCollector;
    if (sort != null) {
      if (after != null && !(after instanceof FieldDoc)) {
        // TODO: if we fix type safety of TopFieldDocs we can
        // remove this
        throw new IllegalArgumentException("after must be a FieldDoc; got " + after);
      }
      boolean fillFields = true;
      hitsCollector = TopFieldCollector.create(sort, n,
                                               (FieldDoc) after,
                                               fillFields,
                                               doDocScores,
                                               doMaxScore);
    } else {
      hitsCollector = TopScoreDocCollector.create(n, after);
    }
    searcher.search(q, MultiCollector.wrap(hitsCollector, fc));
    return hitsCollector.topDocs();
  }
}
