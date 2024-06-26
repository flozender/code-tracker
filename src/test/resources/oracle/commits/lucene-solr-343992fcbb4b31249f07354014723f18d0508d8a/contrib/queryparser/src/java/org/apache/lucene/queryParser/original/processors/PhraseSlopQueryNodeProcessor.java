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
import org.apache.lucene.queryParser.core.nodes.QueryNode;
import org.apache.lucene.queryParser.core.nodes.SlopQueryNode;
import org.apache.lucene.queryParser.core.nodes.TokenizedPhraseQueryNode;
import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorImpl;
import org.apache.lucene.queryParser.original.nodes.MultiPhraseQueryNode;

/**
 * This processor removes invalid {@link SlopQueryNode} objects in the query
 * node tree. A {@link SlopQueryNode} is invalid if its child is neither a
 * {@link TokenizedPhraseQueryNode} nor a {@link MultiPhraseQueryNode}. <br/>
 * 
 * @see SlopQueryNode
 */
public class PhraseSlopQueryNodeProcessor extends QueryNodeProcessorImpl {

  public PhraseSlopQueryNodeProcessor() {
    // empty constructor
  }

  protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {

    if (node instanceof SlopQueryNode) {
      SlopQueryNode phraseSlopNode = (SlopQueryNode) node;

      if (!(phraseSlopNode.getChild() instanceof TokenizedPhraseQueryNode)
          && !(phraseSlopNode.getChild() instanceof MultiPhraseQueryNode)) {
        return phraseSlopNode.getChild();
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
