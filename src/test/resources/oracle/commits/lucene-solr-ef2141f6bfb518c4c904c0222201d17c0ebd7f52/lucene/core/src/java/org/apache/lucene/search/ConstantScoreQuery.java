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
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.ToStringUtils;

/**
 * A query that wraps another query or a filter and simply returns a constant score equal to the
 * query boost for every document that matches the filter or query.
 * For queries it therefore simply strips of all scores and returns a constant one.
 */
public class ConstantScoreQuery extends Query {
  protected final Filter filter;
  protected final Query query;

  /** Strips off scores from the passed in Query. The hits will get a constant score
   * dependent on the boost factor of this query. */
  public ConstantScoreQuery(Query query) {
    if (query == null)
      throw new NullPointerException("Query may not be null");
    this.filter = null;
    this.query = query;
  }

  /** Wraps a Filter as a Query. The hits will get a constant score
   * dependent on the boost factor of this query.
   * If you simply want to strip off scores from a Query, no longer use
   * {@code new ConstantScoreQuery(new QueryWrapperFilter(query))}, instead
   * use {@link #ConstantScoreQuery(Query)}!
   */
  public ConstantScoreQuery(Filter filter) {
    if (filter == null)
      throw new NullPointerException("Filter may not be null");
    this.filter = filter;
    this.query = null;
  }

  /** Returns the encapsulated filter, returns {@code null} if a query is wrapped. */
  public Filter getFilter() {
    return filter;
  }

  /** Returns the encapsulated query, returns {@code null} if a filter is wrapped. */
  public Query getQuery() {
    return query;
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    if (query != null) {
      Query rewritten = query.rewrite(reader);
      if (rewritten != query) {
        rewritten = new ConstantScoreQuery(rewritten);
        rewritten.setBoost(this.getBoost());
        return rewritten;
      }
    } else {
      assert filter != null;
      // Fix outdated usage pattern from Lucene 2.x/early-3.x:
      // because ConstantScoreQuery only accepted filters,
      // QueryWrapperFilter was used to wrap queries.
      if (filter instanceof QueryWrapperFilter) {
        final QueryWrapperFilter qwf = (QueryWrapperFilter) filter;
        final Query rewritten = new ConstantScoreQuery(qwf.getQuery().rewrite(reader));
        rewritten.setBoost(this.getBoost());
        return rewritten;
      }
    }
    return this;
  }

  @Override
  public void extractTerms(Set<Term> terms) {
    // TODO: OK to not add any terms when wrapped a filter
    // and used with MultiSearcher, but may not be OK for
    // highlighting.
    // If a query was wrapped, we delegate to query.
    if (query != null)
      query.extractTerms(terms);
  }

  protected class ConstantWeight extends Weight {
    private final Weight innerWeight;
    private float queryNorm;
    private float queryWeight;
    
    public ConstantWeight(IndexSearcher searcher) throws IOException {
      super(ConstantScoreQuery.this);
      this.innerWeight = (query == null) ? null : query.createWeight(searcher, PostingsEnum.FLAG_NONE);
    }

    @Override
    public float getValueForNormalization() throws IOException {
      // we calculate sumOfSquaredWeights of the inner weight, but ignore it (just to initialize everything)
      if (innerWeight != null) innerWeight.getValueForNormalization();
      queryWeight = getBoost();
      return queryWeight * queryWeight;
    }

    @Override
    public void normalize(float norm, float topLevelBoost) {
      this.queryNorm = norm * topLevelBoost;
      queryWeight *= this.queryNorm;
      // we normalize the inner weight, but ignore it (just to initialize everything)
      if (innerWeight != null) innerWeight.normalize(norm, topLevelBoost);
    }

    @Override
    public BulkScorer bulkScorer(LeafReaderContext context, Bits acceptDocs) throws IOException {
      if (filter != null) {
        assert query == null;
        return super.bulkScorer(context, acceptDocs);
      } else {
        assert query != null && innerWeight != null;
        BulkScorer bulkScorer = innerWeight.bulkScorer(context, acceptDocs);
        if (bulkScorer == null) {
          return null;
        }
        return new ConstantBulkScorer(bulkScorer, this, queryWeight);
      }
    }

    @Override
    public Scorer scorer(LeafReaderContext context, Bits acceptDocs) throws IOException {
      if (filter != null) {
        assert query == null;
        final DocIdSet dis = filter.getDocIdSet(context, acceptDocs);
        if (dis == null) {
          return null;
        }
        final DocIdSetIterator disi = dis.iterator();
        if (disi == null)
          return null;
        return new ConstantDocIdSetIteratorScorer(disi, this, queryWeight);
      } else {
        assert query != null && innerWeight != null;
        Scorer scorer = innerWeight.scorer(context, acceptDocs);
        if (scorer == null) {
          return null;
        }
        return new ConstantScoreScorer(scorer, queryWeight);
      }


    }

    @Override
    public Explanation explain(LeafReaderContext context, int doc) throws IOException {
      final Scorer cs = scorer(context, context.reader().getLiveDocs());
      final boolean exists = (cs != null && cs.advance(doc) == doc);

      final ComplexExplanation result = new ComplexExplanation();
      if (exists) {
        result.setDescription(ConstantScoreQuery.this.toString() + ", product of:");
        result.setValue(queryWeight);
        result.setMatch(Boolean.TRUE);
        result.addDetail(new Explanation(getBoost(), "boost"));
        result.addDetail(new Explanation(queryNorm, "queryNorm"));
      } else {
        result.setDescription(ConstantScoreQuery.this.toString() + " doesn't match id " + doc);
        result.setValue(0);
        result.setMatch(Boolean.FALSE);
      }
      return result;
    }
  }

  /** We return this as our {@link BulkScorer} so that if the CSQ
   *  wraps a query with its own optimized top-level
   *  scorer (e.g. BooleanScorer) we can use that
   *  top-level scorer. */
  protected class ConstantBulkScorer extends BulkScorer {
    final BulkScorer bulkScorer;
    final Weight weight;
    final float theScore;

    public ConstantBulkScorer(BulkScorer bulkScorer, Weight weight, float theScore) {
      this.bulkScorer = bulkScorer;
      this.weight = weight;
      this.theScore = theScore;
    }

    @Override
    public int score(LeafCollector collector, int min, int max) throws IOException {
      return bulkScorer.score(wrapCollector(collector), min, max);
    }

    private LeafCollector wrapCollector(LeafCollector collector) {
      return new FilterLeafCollector(collector) {
        @Override
        public void setScorer(Scorer scorer) throws IOException {
          // we must wrap again here, but using the scorer passed in as parameter:
          in.setScorer(new ConstantScoreScorer(scorer, theScore));
        }
      };
    }

    @Override
    public long cost() {
      return bulkScorer.cost();
    }
  }

  protected class ConstantScoreScorer extends FilterScorer {

    private final float score;

    public ConstantScoreScorer(Scorer wrapped, float score) {
      super(wrapped);
      this.score = score;
    }

    @Override
    public int freq() throws IOException {
      return 1;
    }

    @Override
    public float score() throws IOException {
      return score;
    }

    @Override
    public Collection<ChildScorer> getChildren() {
      if (query != null) {
        return Collections.singletonList(new ChildScorer(in, "constant"));
      } else {
        return Collections.emptyList();
      }
    }
  }

  protected class ConstantDocIdSetIteratorScorer extends Scorer {
    final DocIdSetIterator docIdSetIterator;
    final float theScore;

    public ConstantDocIdSetIteratorScorer(DocIdSetIterator docIdSetIterator, Weight w, float theScore) {
      super(w);
      this.theScore = theScore;
      this.docIdSetIterator = docIdSetIterator;
    }

    @Override
    public int nextDoc() throws IOException {
      return docIdSetIterator.nextDoc();
    }
    
    @Override
    public int docID() {
      return docIdSetIterator.docID();
    }

    @Override
    public float score() throws IOException {
      assert docIdSetIterator.docID() != NO_MORE_DOCS;
      return theScore;
    }

    @Override
    public int freq() throws IOException {
      return 1;
    }

    @Override
    public int nextPosition() throws IOException {
      return -1;
    }

    @Override
    public int startOffset() throws IOException {
      return -1;
    }

    @Override
    public int endOffset() throws IOException {
      return -1;
    }

    @Override
    public BytesRef getPayload() throws IOException {
      return null;
    }

    @Override
    public int advance(int target) throws IOException {
      return docIdSetIterator.advance(target);
    }

    @Override
    public long cost() {
      return docIdSetIterator.cost();
    }

    @Override
    public Collection<ChildScorer> getChildren() {
      if (query != null) {
        return Collections.singletonList(new ChildScorer((Scorer) docIdSetIterator, "constant"));
      } else {
        return Collections.emptyList();
      }
    }
  }

  @Override
  public Weight createWeight(IndexSearcher searcher, int postingsFlags) throws IOException {
    return new ConstantScoreQuery.ConstantWeight(searcher);
  }

  @Override
  public String toString(String field) {
    return new StringBuilder("ConstantScore(")
      .append((query == null) ? filter.toString() : query.toString(field))
      .append(')')
      .append(ToStringUtils.boost(getBoost()))
      .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!super.equals(o))
      return false;
    if (o instanceof ConstantScoreQuery) {
      final ConstantScoreQuery other = (ConstantScoreQuery) o;
      return 
        ((this.filter == null) ? other.filter == null : this.filter.equals(other.filter)) &&
        ((this.query == null) ? other.query == null : this.query.equals(other.query));
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() +
      ((query == null) ? filter : query).hashCode();
  }

}
