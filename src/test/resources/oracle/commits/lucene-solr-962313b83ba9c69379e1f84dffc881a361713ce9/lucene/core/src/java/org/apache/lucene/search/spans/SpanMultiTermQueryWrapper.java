package org.apache.lucene.search.spans;

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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoringRewrite;
import org.apache.lucene.search.TopTermsRewrite;

import java.io.IOException;
import java.util.Objects;

/**
 * Wraps any {@link MultiTermQuery} as a {@link SpanQuery}, 
 * so it can be nested within other SpanQuery classes.
 * <p>
 * The query is rewritten by default to a {@link SpanOrQuery} containing
 * the expanded terms, but this can be customized. 
 * <p>
 * Example:
 * <blockquote><pre class="prettyprint">
 * {@code
 * WildcardQuery wildcard = new WildcardQuery(new Term("field", "bro?n"));
 * SpanQuery spanWildcard = new SpanMultiTermQueryWrapper<WildcardQuery>(wildcard);
 * // do something with spanWildcard, such as use it in a SpanFirstQuery
 * }
 * </pre></blockquote>
 */
public class SpanMultiTermQueryWrapper<Q extends MultiTermQuery> extends SpanQuery {
  protected final Q query;

  /**
   * Create a new SpanMultiTermQueryWrapper. 
   * 
   * @param query Query to wrap.
   * <p>
   * NOTE: This will call {@link MultiTermQuery#setRewriteMethod(MultiTermQuery.RewriteMethod)}
   * on the wrapped <code>query</code>, changing its rewrite method to a suitable one for spans.
   * Be sure to not change the rewrite method on the wrapped query afterwards! Doing so will
   * throw {@link UnsupportedOperationException} on rewriting this query!
   */
  @SuppressWarnings({"rawtypes","unchecked"})
  public SpanMultiTermQueryWrapper(Q query) {
    this.query = Objects.requireNonNull(query);
    
    MultiTermQuery.RewriteMethod method = query.getRewriteMethod();
    if (method instanceof TopTermsRewrite) {
      final int pqsize = ((TopTermsRewrite) method).getSize();
      setRewriteMethod(new TopTermsSpanBooleanQueryRewrite(pqsize));
    } else {
      setRewriteMethod(SCORING_SPAN_QUERY_REWRITE); 
    }
  }

  /**
   * Expert: returns the rewriteMethod
   */
  public final SpanRewriteMethod getRewriteMethod() {
    final MultiTermQuery.RewriteMethod m = query.getRewriteMethod();
    if (!(m instanceof SpanRewriteMethod))
      throw new UnsupportedOperationException("You can only use SpanMultiTermQueryWrapper with a suitable SpanRewriteMethod.");
    return (SpanRewriteMethod) m;
  }

  /**
   * Expert: sets the rewrite method. This only makes sense
   * to be a span rewrite method.
   */
  public final void setRewriteMethod(SpanRewriteMethod rewriteMethod) {
    query.setRewriteMethod(rewriteMethod);
  }

  @Override
  public String getField() {
    return query.getField();
  }

  @Override
  public SpanWeight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
    throw new IllegalArgumentException("Rewrite first!");
  }

  /** Returns the wrapped query */
  public Query getWrappedQuery() {
    return query;
  }

  @Override
  public String toString(String field) {
    StringBuilder builder = new StringBuilder();
    builder.append("SpanMultiTermQueryWrapper(");
    // NOTE: query.toString must be placed in a temp local to avoid compile errors on Java 8u20
    // see https://bugs.openjdk.java.net/browse/JDK-8056984?page=com.atlassian.streams.streams-jira-plugin:activity-stream-issue-tab
    String queryStr = query.toString(field);
    builder.append(queryStr);
    builder.append(")");
    return builder.toString();
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    final Query q = query.rewrite(reader);
    if (!(q instanceof SpanQuery))
      throw new UnsupportedOperationException("You can only use SpanMultiTermQueryWrapper with a suitable SpanRewriteMethod.");
    return q;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + query.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (! super.equals(obj)) {
      return false;
    }
    SpanMultiTermQueryWrapper<?> other = (SpanMultiTermQueryWrapper<?>) obj;
    return query.equals(other.query);
  }

  /** Abstract class that defines how the query is rewritten. */
  public static abstract class SpanRewriteMethod extends MultiTermQuery.RewriteMethod {
    @Override
    public abstract SpanQuery rewrite(IndexReader reader, MultiTermQuery query) throws IOException;
  }

  /**
   * A rewrite method that first translates each term into a SpanTermQuery in a
   * {@link Occur#SHOULD} clause in a BooleanQuery, and keeps the
   * scores as computed by the query.
   * 
   * @see #setRewriteMethod
   */
  public final static SpanRewriteMethod SCORING_SPAN_QUERY_REWRITE = new SpanRewriteMethod() {
    private final ScoringRewrite<SpanOrQuery> delegate = new ScoringRewrite<SpanOrQuery>() {
      @Override
      protected SpanOrQuery getTopLevelBuilder() {
        return new SpanOrQuery();
      }

      protected Query build(SpanOrQuery builder) {
        return builder;
      }

      @Override
      protected void checkMaxClauseCount(int count) {
        // we accept all terms as SpanOrQuery has no limits
      }
    
      @Override
      protected void addClause(SpanOrQuery topLevel, Term term, int docCount, float boost, TermContext states) {
        final SpanTermQuery q = new SpanTermQuery(term, states);
        topLevel.addClause(q);
      }
    };
    
    @Override
    public SpanQuery rewrite(IndexReader reader, MultiTermQuery query) throws IOException {
      return (SpanQuery) delegate.rewrite(reader, query);
    }
  };
  
  /**
   * A rewrite method that first translates each term into a SpanTermQuery in a
   * {@link Occur#SHOULD} clause in a BooleanQuery, and keeps the
   * scores as computed by the query.
   * 
   * <p>
   * This rewrite method only uses the top scoring terms so it will not overflow
   * the boolean max clause count.
   * 
   * @see #setRewriteMethod
   */
  public static final class TopTermsSpanBooleanQueryRewrite extends SpanRewriteMethod  {
    private final TopTermsRewrite<SpanOrQuery> delegate;
  
    /** 
     * Create a TopTermsSpanBooleanQueryRewrite for 
     * at most <code>size</code> terms.
     */
    public TopTermsSpanBooleanQueryRewrite(int size) {
      delegate = new TopTermsRewrite<SpanOrQuery>(size) {
        @Override
        protected int getMaxSize() {
          return Integer.MAX_VALUE;
        }
    
        @Override
        protected SpanOrQuery getTopLevelBuilder() {
          return new SpanOrQuery();
        }

        @Override
        protected Query build(SpanOrQuery builder) {
          return builder;
        }

        @Override
        protected void addClause(SpanOrQuery topLevel, Term term, int docFreq, float boost, TermContext states) {
          final SpanTermQuery q = new SpanTermQuery(term, states);
          topLevel.addClause(q);
        }
      };
    }
    
    /** return the maximum priority queue size */
    public int getSize() {
      return delegate.getSize();
    }

    @Override
    public SpanQuery rewrite(IndexReader reader, MultiTermQuery query) throws IOException {
      return (SpanQuery) delegate.rewrite(reader, query);
    }
  
    @Override
    public int hashCode() {
      return 31 * delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      final TopTermsSpanBooleanQueryRewrite other = (TopTermsSpanBooleanQueryRewrite) obj;
      return delegate.equals(other.delegate);
    }
    
  }
  
}
