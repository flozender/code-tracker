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
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.util.ToStringUtils;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.BasicAutomata;
import org.apache.lucene.util.automaton.BasicOperations;
import org.apache.lucene.util.automaton.MinimizationOperations;
import org.apache.lucene.util.automaton.SpecialOperations;

/**
 * A {@link Query} that will match terms against a finite-state machine.
 * <p>
 * This query will match documents that contain terms accepted by a given
 * finite-state machine. The automaton can be constructed with the
 * {@link org.apache.lucene.util.automaton} API. Alternatively, it can be
 * created from a regular expression with {@link RegexpQuery} or from
 * the standard Lucene wildcard syntax with {@link WildcardQuery}.
 * </p>
 * <p>
 * When the query is executed, it will create an equivalent minimal DFA of the
 * finite-state machine, and will enumerate the term dictionary in an
 * intelligent way to reduce the number of comparisons. For example: the regular
 * expression of <code>[dl]og?</code> will make approximately four comparisons:
 * do, dog, lo, and log.
 * </p>
 * @lucene.experimental
 */
public class AutomatonQuery extends MultiTermQuery {
  /** the automaton to match index terms against */
  protected Automaton automaton;
  /** term containing the field, and possibly some pattern structure */
  protected Term term;

  /**
   * Create a new AutomatonQuery from an {@link Automaton}.
   * 
   * @param term Term containing field and possibly some pattern structure. The
   *        term text is ignored.
   * @param automaton Automaton to run, terms that are accepted are considered a
   *        match.
   */
  public AutomatonQuery(Term term, Automaton automaton) {
    super(term.field());
    this.term = term;
    this.automaton = automaton;
    MinimizationOperations.minimize(automaton);
  }

  @Override
  protected TermsEnum getTermsEnum(IndexReader reader) throws IOException {
    // matches nothing
    if (BasicOperations.isEmpty(automaton)) {
      return TermsEnum.EMPTY;
    }
    
    // matches all possible strings
    if (BasicOperations.isTotal(automaton)) {
      return MultiFields.getTerms(reader, getField()).iterator();
    }
    
    // matches a fixed string in singleton representation
    String singleton = automaton.getSingleton();
    if (singleton != null)
      return new SingleTermsEnum(reader, term.createTerm(singleton));
  
    // matches a fixed string in expanded representation
    String commonPrefix = SpecialOperations.getCommonPrefix(automaton);
    if (automaton.equals(BasicAutomata.makeString(commonPrefix))) {
      return new SingleTermsEnum(reader, term.createTerm(commonPrefix));
    }
    
    // matches a constant prefix
    Automaton prefixAutomaton = BasicOperations.concatenate(BasicAutomata
        .makeString(commonPrefix), BasicAutomata.makeAnyString());
    if (automaton.equals(prefixAutomaton)) {
      return new PrefixTermsEnum(reader, term.createTerm(commonPrefix));
    }
    
    return new AutomatonTermsEnum(automaton, term, reader);
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((automaton == null) ? 0 : automaton.hashCode());
    result = prime * result + ((term == null) ? 0 : term.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    AutomatonQuery other = (AutomatonQuery) obj;
    if (automaton == null) {
      if (other.automaton != null)
        return false;
    } else if (!automaton.equals(other.automaton))
      return false;
    if (term == null) {
      if (other.term != null)
        return false;
    } else if (!term.equals(other.term))
      return false;
    return true;
  }

  @Override
  public String toString(String field) {
    StringBuilder buffer = new StringBuilder();
    if (!term.field().equals(field)) {
      buffer.append(term.field());
      buffer.append(":");
    }
    buffer.append(getClass().getSimpleName());
    buffer.append(" {");
    buffer.append('\n');
    buffer.append(automaton.toString());
    buffer.append("}");
    buffer.append(ToStringUtils.boost(getBoost()));
    return buffer.toString();
  }
}
