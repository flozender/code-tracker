package org.apache.lucene.queryParser.original.processors;

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

import java.util.List;

import org.apache.lucene.queryParser.core.QueryNodeException;
import org.apache.lucene.queryParser.core.nodes.MatchAllDocsQueryNode;
import org.apache.lucene.queryParser.core.nodes.QueryNode;
import org.apache.lucene.queryParser.core.nodes.WildcardQueryNode;
import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorImpl;
import org.apache.lucene.search.MatchAllDocsQuery;

/**
 * This processor converts every {@link WildcardQueryNode} that is "*:*" to
 * {@link MatchAllDocsQueryNode}.
 * 
 * @see MatchAllDocsQueryNode
 * @see MatchAllDocsQuery
 */
public class MatchAllDocsQueryNodeProcessor extends QueryNodeProcessorImpl {

  public MatchAllDocsQueryNodeProcessor() {
    // empty constructor
  }

  protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {

    if (node instanceof WildcardQueryNode) {
      WildcardQueryNode wildcardNode = (WildcardQueryNode) node;

      if (wildcardNode.getField().toString().equals("*")
          && wildcardNode.getText().toString().equals("*")) {

        return new MatchAllDocsQueryNode();

      }

    }

    return node;

  }

  protected QueryNode preProcessNode(QueryNode node) throws QueryNodeException {

    return node;

  }

  protected List<QueryNode> setChildrenOrder(List<QueryNode> children)
      throws QueryNodeException {

    return children;

  }

}
