package org.apache.lucene.search;

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
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;


/**
 * A query that applies a filter to the results of another query.
 *
 * <p>Note: the bits are retrieved from the filter each time this
 * query is used in a search - use a CachingWrapperFilter to avoid
 * regenerating the bits every time.
 * @since   1.4
 * @see     CachingWrapperFilter
 */
public class FilteredQuery extends Query {

  private final Query query;
  private final Filter filter;
  private final FilterStrategy strategy;

  /**
   * Constructs a new query which applies a filter to the results of the original query.
   * {@link Filter#getDocIdSet} will be called every time this query is used in a search.
   * @param query  Query to be filtered, cannot be <code>null</code>.
   * @param filter Filter to apply to query results, cannot be <code>null</code>.
   */
  public FilteredQuery(Query query, Filter filter) {
    this(query, filter, RANDOM_ACCESS_FILTER_STRATEGY);
  }
  
  /**
   * Expert: Constructs a new query which applies a filter to the results of the original query.
   * {@link Filter#getDocIdSet} will be called every time this query is used in a search.
   * @param query  Query to be filtered, cannot be <code>null</code>.
   * @param filter Filter to apply to query results, cannot be <code>null</code>.
   * @param strategy a filter strategy used to create a filtered scorer. 
   * 
   * @see FilterStrategy
   */
  public FilteredQuery(Query query, Filter filter, FilterStrategy strategy) {
    if (query == null || filter == null)
      throw new IllegalArgumentException("Query and filter cannot be null.");
    if (strategy == null)
      throw new IllegalArgumentException("FilterStrategy can not be null");
    this.strategy = strategy;
    this.query = query;
    this.filter = filter;
  }
  
  /**
   * Returns a Weight that applies the filter to the enclosed query's Weight.
   * This is accomplished by overriding the Scorer returned by the Weight.
   */
  @Override
  public Weight createWeight(final IndexSearcher searcher) throws IOException {
    final Weight weight = query.createWeight (searcher);
    return new Weight() {

      @Override
      public float getValueForNormalization() throws IOException { 
        return weight.getValueForNormalization() * getBoost() * getBoost(); // boost sub-weight
      }

      @Override
      public void normalize(float norm, float topLevelBoost) { 
        weight.normalize(norm, topLevelBoost * getBoost()); // incorporate boost
      }

      @Override
      public Explanation explain(LeafReaderContext ir, int i) throws IOException {
        Explanation inner = weight.explain (ir, i);
        Filter f = FilteredQuery.this.filter;
        DocIdSet docIdSet = f.getDocIdSet(ir, ir.reader().getLiveDocs());
        DocIdSetIterator docIdSetIterator = docIdSet == null ? DocIdSetIterator.empty() : docIdSet.iterator();
        if (docIdSetIterator == null) {
          docIdSetIterator = DocIdSetIterator.empty();
        }
        if (docIdSetIterator.advance(i) == i) {
          return inner;
        } else {
          Explanation result = new Explanation
            (0.0f, "failure to match filter: " + f.toString());
          result.addDetail(inner);
          return result;
        }
      }

      // return this query
      @Override
      public Query getQuery() {
        return FilteredQuery.this;
      }

      // return a filtering scorer
      @Override
      public Scorer scorer(LeafReaderContext context, Bits acceptDocs, boolean needsScores) throws IOException {
        assert filter != null;

        DocIdSet filterDocIdSet = filter.getDocIdSet(context, acceptDocs);
        if (filterDocIdSet == null) {
          // this means the filter does not accept any documents.
          return null;
        }

        return strategy.filteredScorer(context, weight, filterDocIdSet, needsScores);
      }

      // return a filtering top scorer
      @Override
      public BulkScorer bulkScorer(LeafReaderContext context, Bits acceptDocs, boolean needsScores) throws IOException {
        assert filter != null;

        DocIdSet filterDocIdSet = filter.getDocIdSet(context, acceptDocs);
        if (filterDocIdSet == null) {
          // this means the filter does not accept any documents.
          return null;
        }

        return strategy.filteredBulkScorer(context, weight, filterDocIdSet, needsScores);

      }
    };
  }
  
  /**
   * A scorer that consults the filter iff a document was matched by the
   * delegate scorer. This is useful if the filter computation is more expensive
   * than document scoring or if the filter has a linear running time to compute
   * the next matching doc like exact geo distances.
   */
  private static final class QueryFirstScorer extends FilterScorer {
    private final Scorer scorer;
    private int scorerDoc = -1;
    private final Bits filterBits;

    protected QueryFirstScorer(Weight weight, Bits filterBits, Scorer other) {
      super(other, weight);
      this.scorer = other;
      this.filterBits = filterBits;
    }

    @Override
    public int nextDoc() throws IOException {
      int doc;
      for(;;) {
        doc = scorer.nextDoc();
        if (doc == Scorer.NO_MORE_DOCS || filterBits.get(doc)) {
          return scorerDoc = doc;
        }
      } 
    }
    
    @Override
    public int advance(int target) throws IOException {
      int doc = scorer.advance(target);
      if (doc != Scorer.NO_MORE_DOCS && !filterBits.get(doc)) {
        return scorerDoc = nextDoc();
      } else {
        return scorerDoc = doc;
      }
    }
    @Override
    public int docID() {
      return scorerDoc;
    }

    @Override
    public Collection<ChildScorer> getChildren() {
      return Collections.singleton(new ChildScorer(scorer, "FILTERED"));
    }

  }

  private static class QueryFirstBulkScorer extends BulkScorer {

    private final Scorer scorer;
    private final Bits filterBits;

    public QueryFirstBulkScorer(Scorer scorer, Bits filterBits) {
      this.scorer = scorer;
      this.filterBits = filterBits;
    }

    @Override
    public long cost() {
      return scorer.cost();
    }

    @Override
    public int score(LeafCollector collector, int min, int maxDoc) throws IOException {
      // the normalization trick already applies the boost of this query,
      // so we can use the wrapped scorer directly:
      collector.setScorer(scorer);
      if (scorer.docID() < min) {
        scorer.advance(min);
      }
      while (true) {
        final int scorerDoc = scorer.docID();
        if (scorerDoc < maxDoc) {
          if (filterBits.get(scorerDoc)) {
            collector.collect(scorerDoc);
          }
          scorer.nextDoc();
        } else {
          break;
        }
      }

      return scorer.docID();
    }
  }
  
  /**
   * A Scorer that uses a "leap-frog" approach (also called "zig-zag join"). The scorer and the filter
   * take turns trying to advance to each other's next matching document, often
   * jumping past the target document. When both land on the same document, it's
   * collected.
   */
  private static final class LeapFrogScorer extends FilterScorer {
    private final DocIdSetIterator secondary;
    private final DocIdSetIterator primary;
    private final Scorer scorer;
    private int primaryDoc = -1;
    private int secondaryDoc = -1;

    protected LeapFrogScorer(Weight weight, DocIdSetIterator primary, DocIdSetIterator secondary, Scorer scorer) {
      super(scorer, weight);
      this.primary = primary;
      this.secondary = secondary;
      this.scorer = scorer;
    }

    private final int advanceToNextCommonDoc() throws IOException {
      for (;;) {
        if (secondaryDoc < primaryDoc) {
          secondaryDoc = secondary.advance(primaryDoc);
        } else if (secondaryDoc == primaryDoc) {
          return primaryDoc;
        } else {
          primaryDoc = primary.advance(secondaryDoc);
        }
      }
    }

    @Override
    public final int nextDoc() throws IOException {
      primaryDoc = primaryNext();
      return advanceToNextCommonDoc();
    }
    
    protected int primaryNext() throws IOException {
      return primary.nextDoc();
    }
    
    @Override
    public final int advance(int target) throws IOException {
      if (target > primaryDoc) {
        primaryDoc = primary.advance(target);
      }
      return advanceToNextCommonDoc();
    }

    @Override
    public final int docID() {
      return secondaryDoc;
    }

    @Override
    public final Collection<ChildScorer> getChildren() {
      return Collections.singleton(new ChildScorer(scorer, "FILTERED"));
    }

    @Override
    public long cost() {
      return Math.min(primary.cost(), secondary.cost());
    }
  }
  
  /** Rewrites the query. If the wrapped is an instance of
   * {@link MatchAllDocsQuery} it returns a {@link ConstantScoreQuery}. Otherwise
   * it returns a new {@code FilteredQuery} wrapping the rewritten query. */
  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    final Query queryRewritten = query.rewrite(reader);
    
    if (queryRewritten != query) {
      // rewrite to a new FilteredQuery wrapping the rewritten query
      final Query rewritten = new FilteredQuery(queryRewritten, filter, strategy);
      rewritten.setBoost(this.getBoost());
      return rewritten;
    } else {
      // nothing to rewrite, we are done!
      return this;
    }
  }

  /** Returns this FilteredQuery's (unfiltered) Query */
  public final Query getQuery() {
    return query;
  }

  /** Returns this FilteredQuery's filter */
  public final Filter getFilter() {
    return filter;
  }
  
  /** Returns this FilteredQuery's {@link FilterStrategy} */
  public FilterStrategy getFilterStrategy() {
    return this.strategy;
  }

  // inherit javadoc
  @Override
  public void extractTerms(Set<Term> terms) {
    getQuery().extractTerms(terms);
  }

  /** Prints a user-readable version of this query. */
  @Override
  public String toString (String s) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("filtered(");
    buffer.append(query.toString(s));
    buffer.append(")->");
    buffer.append(filter);
    buffer.append(ToStringUtils.boost(getBoost()));
    return buffer.toString();
  }

  /** Returns true iff <code>o</code> is equal to this. */
  @Override
  public boolean equals(Object o) {
    if (o == this)
      return true;
    if (!super.equals(o))
      return false;
    assert o instanceof FilteredQuery;
    final FilteredQuery fq = (FilteredQuery) o;
    return fq.query.equals(this.query) && fq.filter.equals(this.filter) && fq.strategy.equals(this.strategy);
  }

  /** Returns a hash code value for this object. */
  @Override
  public int hashCode() {
    int hash = super.hashCode();
    hash = hash * 31 + strategy.hashCode();
    hash = hash * 31 + query.hashCode();
    hash = hash * 31 + filter.hashCode();
    return hash;
  }
  
  /**
   * A {@link FilterStrategy} that conditionally uses a random access filter if
   * the given {@link DocIdSet} supports random access (returns a non-null value
   * from {@link DocIdSet#bits()}) and
   * {@link RandomAccessFilterStrategy#useRandomAccess(Bits, long)} returns
   * <code>true</code>. Otherwise this strategy falls back to a "zig-zag join" (
   * {@link FilteredQuery#LEAP_FROG_FILTER_FIRST_STRATEGY}) strategy.
   * 
   * <p>
   * Note: this strategy is the default strategy in {@link FilteredQuery}
   * </p>
   */
  public static final FilterStrategy RANDOM_ACCESS_FILTER_STRATEGY = new RandomAccessFilterStrategy();
  
  /**
   * A filter strategy that uses a "leap-frog" approach (also called "zig-zag join"). 
   * The scorer and the filter
   * take turns trying to advance to each other's next matching document, often
   * jumping past the target document. When both land on the same document, it's
   * collected.
   * <p>
   * Note: This strategy uses the filter to lead the iteration.
   * </p> 
   */
  public static final FilterStrategy LEAP_FROG_FILTER_FIRST_STRATEGY = new LeapFrogFilterStrategy(false);
  
  /**
   * A filter strategy that uses a "leap-frog" approach (also called "zig-zag join"). 
   * The scorer and the filter
   * take turns trying to advance to each other's next matching document, often
   * jumping past the target document. When both land on the same document, it's
   * collected.
   * <p>
   * Note: This strategy uses the query to lead the iteration.
   * </p> 
   */
  public static final FilterStrategy LEAP_FROG_QUERY_FIRST_STRATEGY = new LeapFrogFilterStrategy(true);
  
  /**
   * A filter strategy that advances the Query or rather its {@link Scorer} first and consults the
   * filter {@link DocIdSet} for each matched document.
   * <p>
   * Note: this strategy requires a {@link DocIdSet#bits()} to return a non-null value. Otherwise
   * this strategy falls back to {@link FilteredQuery#LEAP_FROG_QUERY_FIRST_STRATEGY}
   * </p>
   * <p>
   * Use this strategy if the filter computation is more expensive than document
   * scoring or if the filter has a linear running time to compute the next
   * matching doc like exact geo distances.
   * </p>
   */
  public static final FilterStrategy QUERY_FIRST_FILTER_STRATEGY = new QueryFirstFilterStrategy();
  
  /** Abstract class that defines how the filter ({@link DocIdSet}) applied during document collection. */
  public static abstract class FilterStrategy {
    
    /**
     * Returns a filtered {@link Scorer} based on this strategy.
     * 
     * @param context
     *          the {@link org.apache.lucene.index.LeafReaderContext} for which to return the {@link Scorer}.
     * @param weight the {@link FilteredQuery} {@link Weight} to create the filtered scorer.
     * @param docIdSet the filter {@link DocIdSet} to apply
     * @return a filtered scorer
     * 
     * @throws IOException if an {@link IOException} occurs
     */
    public abstract Scorer filteredScorer(LeafReaderContext context,
        Weight weight, DocIdSet docIdSet, boolean needsScores) throws IOException;

    /**
     * Returns a filtered {@link BulkScorer} based on this
     * strategy.  This is an optional method: the default
     * implementation just calls {@link #filteredScorer} and
     * wraps that into a BulkScorer.
     *
     * @param context
     *          the {@link org.apache.lucene.index.LeafReaderContext} for which to return the {@link Scorer}.
     * @param weight the {@link FilteredQuery} {@link Weight} to create the filtered scorer.
     * @param docIdSet the filter {@link DocIdSet} to apply
     * @return a filtered top scorer
     */
    public BulkScorer filteredBulkScorer(LeafReaderContext context,
        Weight weight, DocIdSet docIdSet, boolean needsScores) throws IOException {
      Scorer scorer = filteredScorer(context, weight, docIdSet, needsScores);
      if (scorer == null) {
        return null;
      }
      // This impl always scores docs in order, so we can
      // ignore scoreDocsInOrder:
      return new Weight.DefaultBulkScorer(scorer);
    }

  }
  
  /**
   * A {@link FilterStrategy} that conditionally uses a random access filter if
   * the given {@link DocIdSet} supports random access (returns a non-null value
   * from {@link DocIdSet#bits()}) and
   * {@link RandomAccessFilterStrategy#useRandomAccess(Bits, long)} returns
   * <code>true</code>. Otherwise this strategy falls back to a "zig-zag join" (
   * {@link FilteredQuery#LEAP_FROG_FILTER_FIRST_STRATEGY}) strategy .
   */
  public static class RandomAccessFilterStrategy extends FilterStrategy {

    @Override
    public Scorer filteredScorer(LeafReaderContext context, Weight weight, DocIdSet docIdSet, boolean needsScores) throws IOException {
      final DocIdSetIterator filterIter = docIdSet.iterator();
      if (filterIter == null) {
        // this means the filter does not accept any documents.
        return null;
      }  
      
      final Bits filterAcceptDocs = docIdSet.bits();
      // force if RA is requested
      final boolean useRandomAccess = filterAcceptDocs != null && useRandomAccess(filterAcceptDocs, filterIter.cost());
      if (useRandomAccess) {
        // if we are using random access, we return the inner scorer, just with other acceptDocs
        return weight.scorer(context, filterAcceptDocs, needsScores);
      } else {
        // we are gonna advance() this scorer, so we set inorder=true/toplevel=false
        // we pass null as acceptDocs, as our filter has already respected acceptDocs, no need to do twice
        final Scorer scorer = weight.scorer(context, null, needsScores);
        return (scorer == null) ? null : new LeapFrogScorer(weight, filterIter, scorer, scorer);
      }
    }
    
    /**
     * Expert: decides if a filter should be executed as "random-access" or not.
     * random-access means the filter "filters" in a similar way as deleted docs are filtered
     * in Lucene. This is faster when the filter accepts many documents.
     * However, when the filter is very sparse, it can be faster to execute the query+filter
     * as a conjunction in some cases.
     * 
     * The default implementation returns <code>true</code> if the filter matches more than 1%
     * of documents
     * 
     * @lucene.internal
     */
    protected boolean useRandomAccess(Bits bits, long filterCost) {
      // if the filter matches more than 1% of documents, we use random-access
      return filterCost * 100 > bits.length();
    }
  }
  
  private static final class LeapFrogFilterStrategy extends FilterStrategy {
    
    private final boolean scorerFirst;
    
    private LeapFrogFilterStrategy(boolean scorerFirst) {
      this.scorerFirst = scorerFirst;
    }

    @Override
    public Scorer filteredScorer(LeafReaderContext context,
        Weight weight, DocIdSet docIdSet, boolean needsScores) throws IOException {
      final DocIdSetIterator filterIter = docIdSet.iterator();
      if (filterIter == null) {
        // this means the filter does not accept any documents.
        return null;
      }
      // we pass null as acceptDocs, as our filter has already respected acceptDocs, no need to do twice
      final Scorer scorer = weight.scorer(context, null, needsScores);
      if (scorer == null) {
        return null;
      }

      if (scorerFirst) {
        return new LeapFrogScorer(weight, scorer, filterIter, scorer);  
      } else {
        return new LeapFrogScorer(weight, filterIter, scorer, scorer);  
      }
    }
  }
  
  /**
   * A filter strategy that advances the {@link Scorer} first and consults the
   * {@link DocIdSet} for each matched document.
   * <p>
   * Note: this strategy requires a {@link DocIdSet#bits()} to return a non-null value. Otherwise
   * this strategy falls back to {@link FilteredQuery#LEAP_FROG_QUERY_FIRST_STRATEGY}
   * </p>
   * <p>
   * Use this strategy if the filter computation is more expensive than document
   * scoring or if the filter has a linear running time to compute the next
   * matching doc like exact geo distances.
   * </p>
   */
  private static final class QueryFirstFilterStrategy extends FilterStrategy {
    @Override
    public Scorer filteredScorer(final LeafReaderContext context,
        Weight weight, DocIdSet docIdSet, boolean needsScores) throws IOException {
      Bits filterAcceptDocs = docIdSet.bits();
      if (filterAcceptDocs == null) {
        // Filter does not provide random-access Bits; we
        // must fallback to leapfrog:
        return LEAP_FROG_QUERY_FIRST_STRATEGY.filteredScorer(context, weight, docIdSet, needsScores);
      }
      final Scorer scorer = weight.scorer(context, null, needsScores);
      return scorer == null ? null : new QueryFirstScorer(weight, filterAcceptDocs, scorer);
    }

    @Override
    public BulkScorer filteredBulkScorer(final LeafReaderContext context,
        Weight weight, DocIdSet docIdSet, boolean needsScores) throws IOException {
      Bits filterAcceptDocs = docIdSet.bits();
      if (filterAcceptDocs == null) {
        // Filter does not provide random-access Bits; we
        // must fallback to leapfrog:
        return LEAP_FROG_QUERY_FIRST_STRATEGY.filteredBulkScorer(context, weight, docIdSet, needsScores);
      }
      final Scorer scorer = weight.scorer(context, null, needsScores);
      return scorer == null ? null : new QueryFirstBulkScorer(scorer, filterAcceptDocs);
    }
  }
  
}
