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
import java.util.Set;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation.IDFExplanation;
import org.apache.lucene.util.ToStringUtils;

/** A Query that matches documents containing a term.
  This may be combined with other terms with a {@link BooleanQuery}.
  */
public class TermQuery extends Query {
  private Term term;

  private class TermWeight extends Weight {
    private final Similarity similarity;
    private float value;
    private float idf;
    private float queryNorm;
    private float queryWeight;
    private IDFExplanation idfExp;

    public TermWeight(Searcher searcher)
      throws IOException {
      this.similarity = getSimilarity(searcher);
      idfExp = similarity.idfExplain(term, searcher);
      idf = idfExp.getIdf();
    }

    @Override
    public String toString() { return "weight(" + TermQuery.this + ")"; }

    @Override
    public Query getQuery() { return TermQuery.this; }

    @Override
    public float getValue() { return value; }

    @Override
    public float sumOfSquaredWeights() {
      queryWeight = idf * getBoost();             // compute query weight
      return queryWeight * queryWeight;           // square it
    }

    @Override
    public void normalize(float queryNorm) {
      this.queryNorm = queryNorm;
      queryWeight *= queryNorm;                   // normalize query weight
      value = queryWeight * idf;                  // idf for document
    }

    @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      // NOTE: debateably, the caller should never pass in a
      // multi reader...
      DocsEnum docs = MultiFields.getTermDocsEnum(reader, MultiFields.getDeletedDocs(reader), term.field(), term.bytes());
      if (docs == null) {
        return null;
      }

      return new TermScorer(this, docs, similarity, reader.norms(term.field()));
    }

    @Override
    public Explanation explain(IndexReader reader, int doc)
      throws IOException {

      ComplexExplanation result = new ComplexExplanation();
      result.setDescription("weight("+getQuery()+" in "+doc+"), product of:");

      Explanation expl = new Explanation(idf, idfExp.explain());

      // explain query weight
      Explanation queryExpl = new Explanation();
      queryExpl.setDescription("queryWeight(" + getQuery() + "), product of:");

      Explanation boostExpl = new Explanation(getBoost(), "boost");
      if (getBoost() != 1.0f)
        queryExpl.addDetail(boostExpl);
      queryExpl.addDetail(expl);

      Explanation queryNormExpl = new Explanation(queryNorm,"queryNorm");
      queryExpl.addDetail(queryNormExpl);

      queryExpl.setValue(boostExpl.getValue() *
                         expl.getValue() *
                         queryNormExpl.getValue());

      result.addDetail(queryExpl);

      // explain field weight
      String field = term.field();
      ComplexExplanation fieldExpl = new ComplexExplanation();
      fieldExpl.setDescription("fieldWeight("+term+" in "+doc+
                               "), product of:");

      Explanation tfExplanation = new Explanation();
      int tf = 0;
      DocsEnum docs = reader.termDocsEnum(MultiFields.getDeletedDocs(reader), term.field(), term.bytes());
      if (docs != null) {
          int newDoc = docs.advance(doc);
          if (newDoc == doc) {
            tf = docs.freq();
          }
        tfExplanation.setValue(similarity.tf(tf));
        tfExplanation.setDescription("tf(termFreq("+term+")="+tf+")");
      } else {
        tfExplanation.setValue(0.0f);
        tfExplanation.setDescription("no matching term");
      }
      fieldExpl.addDetail(tfExplanation);
      fieldExpl.addDetail(expl);

      Explanation fieldNormExpl = new Explanation();
      byte[] fieldNorms = reader.norms(field);
      float fieldNorm =
        fieldNorms!=null ? similarity.decodeNormValue(fieldNorms[doc]) : 1.0f;
      fieldNormExpl.setValue(fieldNorm);
      fieldNormExpl.setDescription("fieldNorm(field="+field+", doc="+doc+")");
      fieldExpl.addDetail(fieldNormExpl);
      
      fieldExpl.setMatch(Boolean.valueOf(tfExplanation.isMatch()));
      fieldExpl.setValue(tfExplanation.getValue() *
                         expl.getValue() *
                         fieldNormExpl.getValue());

      result.addDetail(fieldExpl);
      result.setMatch(fieldExpl.getMatch());
      
      // combine them
      result.setValue(queryExpl.getValue() * fieldExpl.getValue());

      if (queryExpl.getValue() == 1.0f)
        return fieldExpl;

      return result;
    }
  }

  /** Constructs a query for the term <code>t</code>. */
  public TermQuery(Term t) {
    term = t;
  }

  /** Returns the term of this query. */
  public Term getTerm() { return term; }

  @Override
  public Weight createWeight(Searcher searcher) throws IOException {
    return new TermWeight(searcher);
  }

  @Override
  public void extractTerms(Set<Term> terms) {
    terms.add(getTerm());
  }

  /** Prints a user-readable version of this query. */
  @Override
  public String toString(String field) {
    StringBuilder buffer = new StringBuilder();
    if (!term.field().equals(field)) {
      buffer.append(term.field());
      buffer.append(":");
    }
    buffer.append(term.text());
    buffer.append(ToStringUtils.boost(getBoost()));
    return buffer.toString();
  }

  /** Returns true iff <code>o</code> is equal to this. */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TermQuery))
      return false;
    TermQuery other = (TermQuery)o;
    return (this.getBoost() == other.getBoost())
      && this.term.equals(other.term);
  }

  /** Returns a hash code value for this object.*/
  @Override
  public int hashCode() {
    return Float.floatToIntBits(getBoost()) ^ term.hashCode();
  }

}
