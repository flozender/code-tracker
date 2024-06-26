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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;


/**
 * A query that applies a filter to the results of another query.
 *
 * <p>Note: the bits are retrieved from the filter each time this
 * query is used in a search - use a CachingWrapperFilter to avoid
 * regenerating the bits every time.
 * @since   1.4
 * @see     CachingWrapperQuery
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
    this.strategy = Objects.requireNonNull(strategy, "FilterStrategy must not be null");
    this.query = Objects.requireNonNull(query, "Query must not be null");
    this.filter = Objects.requireNonNull(filter, "Filter must not be null");
  }
  
  /**
   * Returns a Weight that applies the filter to the enclosed query's Weight.
   * This is accomplished by overriding the Scorer returned by the Weight.
   */
  @Override
  public Weight createWeight(final IndexSearcher searcher, boolean needsScores) throws IOException {
    final Weight weight = query.createWeight (searcher, needsScores);
    return new Weight(FilteredQuery.this) {

      @Override
      public void extractTerms(Set<Term> terms) {
        weight.extractTerms(terms);
      }

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
          return Explanation.noMatch("failure to match filter: " + f.toString(), inner);
        }
      }

      // return a filtering scorer
      @Override
      public Scorer scorer(LeafReaderContext context, Bits acceptDocs) throws IOException {
        assert filter != null;

        DocIdSet filterDocIdSet = filter.getDocIdSet(context, acceptDocs);
        if (filterDocIdSet == null) {
          // this means the filter does not accept any documents.
          return null;
        }

        return strategy.filteredScorer(context, weight, filterDocIdSet);
      }

      // return a filtering top scorer
      @Override
      public BulkScorer bulkScorer(LeafReaderContext context, Bits acceptDocs) throws IOException {
        assert filter != null;

        DocIdSet filterDocIdSet = filter.getDocIdSet(context, acceptDocs);
        if (filterDocIdSet == null) {
          // this means the filter does not accept any documents.
          return null;
        }

        return strategy.filteredBulkScorer(context, weight, filterDocIdSet);

      }
    };
  }
  
  /**
   * A scorer that consults the filter iff a document was matched by the
   * delegate scorer. This is useful if the filter computation is more expensive
   * than document scoring or if the filter has a linear running time to compute
   * the next matching doc like exact geo distances.
   */
  private static final class QueryFirstScorer extends Scorer {
    private final Scorer scorer;
    private final Bits filterBits;

    protected QueryFirstScorer(Weight weight, Bits filterBits, Scorer other) {
      super(weight);
      this.scorer = other;
      this.filterBits = filterBits;
    }

    @Override
    public int nextDoc() throws IOException {
      int doc;
      for(;;) {
        doc = scorer.nextDoc();
        if (doc == DocIdSetIterator.NO_MORE_DOCS || filterBits.get(doc)) {
          return doc;
        }
      } 
    }
    
    @Override
    public int advance(int target) throws IOException {
      int doc = scorer.advance(target);
      if (doc != DocIdSetIterator.NO_MORE_DOCS && !filterBits.get(doc)) {
        return nextDoc();
      } else {
        return doc;
      }
    }
    
    @Override
    public int docID() {
      return scorer.docID();
    }

    @Override
    public float score() throws IOException {
      return scorer.score();
    }

    @Override
    public int freq() throws IOException {
      return scorer.freq();
    }

    @Override
    public long cost() {
      return scorer.cost();
    }

    @Override
    public Collection<ChildScorer> getChildren() {
      return Collections.singleton(new ChildScorer(scorer, "FILTERED"));
    }

    @Override
    public TwoPhaseIterator asTwoPhaseIterator() {    
      TwoPhaseIterator inner = scorer.asTwoPhaseIterator();
      if (inner != null) {
        // we are like a simplified conjunction here, handle the nested case:
        return new TwoPhaseIterator(inner.approximation()) {
          @Override
          public boolean matches() throws IOException {
            // check the approximation matches first, then check bits last.
            return inner.matches() && filterBits.get(scorer.docID());
          }
        };
      } else {
        // scorer doesnt have an approximation, just use it, to force bits applied last.
        return new TwoPhaseIterator(scorer) {
          @Override
          public boolean matches() throws IOException {
            return filterBits.get(scorer.docID());
          }
        };
      }
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
  private static final class LeapFrogScorer extends Scorer {
    private final ConjunctionDISI conjunction;
    private final Scorer scorer;

    protected LeapFrogScorer(Weight weight, DocIdSetIterator primary, DocIdSetIterator secondary, Scorer scorer) {
      super(weight);
      conjunction = ConjunctionDISI.intersect(Arrays.asList(primary, secondary));
      this.scorer = scorer;
    }

    @Override
    public int nextDoc() throws IOException {
      return conjunction.nextDoc();
    }
    
    @Override
    public final int advance(int target) throws IOException {
      return conjunction.advance(target);
    }

    @Override
    public final int docID() {
      return conjunction.docID();
    }

    @Override
    public final Collection<ChildScorer> getChildren() {
      return Collections.singleton(new ChildScorer(scorer, "FILTERED"));
    }

    @Override
    public long cost() {
      return conjunction.cost();
    }

    @Override
    public float score() throws IOException {
      return scorer.score();
    }

    @Override
    public int freq() throws IOException {
      return scorer.freq();
    }

    @Override
    public TwoPhaseIterator asTwoPhaseIterator() {
      return conjunction.asTwoPhaseIterator();
    }
  }
  
  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    if (filter instanceof QueryWrapperFilter) {
      // In that case the filter does not implement random-access anyway so
      // we want to take advantage of approximations
      BooleanQuery rewritten = new BooleanQuery();
      rewritten.add(query, Occur.MUST);
      rewritten.add(((QueryWrapperFilter) filter).getQuery(), Occur.FILTER);
      rewritten.setBoost(getBoost());
      return rewritten;
    }

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
        Weight weight, DocIdSet docIdSet) throws IOException;

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
        Weight weight, DocIdSet docIdSet) throws IOException {
      Scorer scorer = filteredScorer(context, weight, docIdSet);
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
    public Scorer filteredScorer(LeafReaderContext context, Weight weight, DocIdSet docIdSet) throws IOException {
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
        return weight.scorer(context, filterAcceptDocs);
      } else {
        // we are gonna advance() this scorer, so we set inorder=true/toplevel=false
        // we pass null as acceptDocs, as our filter has already respected acceptDocs, no need to do twice
        final Scorer scorer = weight.scorer(context, null);
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
        Weight weight, DocIdSet docIdSet) throws IOException {
      final DocIdSetIterator filterIter = docIdSet.iterator();
      if (filterIter == null) {
        // this means the filter does not accept any documents.
        return null;
      }
      // we pass null as acceptDocs, as our filter has already respected acceptDocs, no need to do twice
      final Scorer scorer = weight.scorer(context, null);
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
        Weight weight, DocIdSet docIdSet) throws IOException {
      Bits filterAcceptDocs = docIdSet.bits();
      if (filterAcceptDocs == null) {
        // Filter does not provide random-access Bits; we
        // must fallback to leapfrog:
        return LEAP_FROG_QUERY_FIRST_STRATEGY.filteredScorer(context, weight, docIdSet);
      }
      final Scorer scorer = weight.scorer(context, null);
      return scorer == null ? null : new QueryFirstScorer(weight, filterAcceptDocs, scorer);
    }

    @Override
    public BulkScorer filteredBulkScorer(final LeafReaderContext context,
        Weight weight, DocIdSet docIdSet) throws IOException {
      Bits filterAcceptDocs = docIdSet.bits();
      if (filterAcceptDocs == null) {
        // Filter does not provide random-access Bits; we
        // must fallback to leapfrog:
        return LEAP_FROG_QUERY_FIRST_STRATEGY.filteredBulkScorer(context, weight, docIdSet);
      }
      final Scorer scorer = weight.scorer(context, null);
      return scorer == null ? null : new QueryFirstBulkScorer(scorer, filterAcceptDocs);
    }
  }
  
}
