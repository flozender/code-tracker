package org.apache.lucene.queryParser.original.builders;

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

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.core.QueryNodeException;
import org.apache.lucene.queryParser.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryParser.core.nodes.FieldQueryNode;
import org.apache.lucene.queryParser.core.nodes.QueryNode;
import org.apache.lucene.queryParser.original.nodes.MultiPhraseQueryNode;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.TermQuery;

/**
 * Builds a {@link MultiPhraseQuery} object from a {@link MultiPhraseQueryNode}
 * object.
 */
public class MultiPhraseQueryNodeBuilder implements OriginalQueryBuilder {

  public MultiPhraseQueryNodeBuilder() {
    // empty constructor
  }

  public MultiPhraseQuery build(QueryNode queryNode) throws QueryNodeException {
    MultiPhraseQueryNode phraseNode = (MultiPhraseQueryNode) queryNode;

    MultiPhraseQuery phraseQuery = new MultiPhraseQuery();

    List<QueryNode> children = phraseNode.getChildren();

    if (children != null) {
      TreeMap<Integer, List<Term>> positionTermMap = new TreeMap<Integer, List<Term>>();

      for (QueryNode child : children) {
        FieldQueryNode termNode = (FieldQueryNode) child;
        TermQuery termQuery = (TermQuery) termNode
            .getTag(QueryTreeBuilder.QUERY_TREE_BUILDER_TAGID);
        List<Term> termList = positionTermMap.get(termNode
            .getPositionIncrement());

        if (termList == null) {
          termList = new LinkedList<Term>();
          positionTermMap.put(termNode.getPositionIncrement(), termList);

        }

        termList.add(termQuery.getTerm());

      }

      for (int positionIncrement : positionTermMap.keySet()) {
        List<Term> termList = positionTermMap.get(positionIncrement);

        phraseQuery.add(termList.toArray(new Term[termList.size()]),
            positionIncrement);

      }

    }

    return phraseQuery;

  }

}
