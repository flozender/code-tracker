package org.apache.lucene.queryParser.standard.processors;

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

import org.apache.lucene.queryParser.core.nodes.ParametricRangeQueryNode;
import org.apache.lucene.queryParser.core.nodes.QueryNode;
import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorImpl;
import org.apache.lucene.queryParser.standard.config.MultiTermRewriteMethodAttribute;
import org.apache.lucene.queryParser.standard.nodes.WildcardQueryNode;
import org.apache.lucene.search.MultiTermQuery;

/**
 * This processor instates the default
 * {@link org.apache.lucene.search.MultiTermQuery.RewriteMethod},
 * {@link MultiTermQuery#CONSTANT_SCORE_AUTO_REWRITE_DEFAULT}, for multi-term
 * query nodes.
 */
public class MultiTermRewriteMethodProcessor extends QueryNodeProcessorImpl {

  protected QueryNode postProcessNode(QueryNode node) {

    // set setMultiTermRewriteMethod for WildcardQueryNode and
    // PrefixWildcardQueryNode
    if (node instanceof WildcardQueryNode
        || node instanceof ParametricRangeQueryNode) {

      if (!getQueryConfigHandler().hasAttribute(
          MultiTermRewriteMethodAttribute.class)) {
        // This should not happen, this attribute is created in the
        // StandardQueryConfigHandler
        throw new IllegalArgumentException(
            "MultiTermRewriteMethodAttribute should be set on the QueryConfigHandler");
      }

      // read the attribute value and use a TAG to take the value to the Builder
      MultiTermQuery.RewriteMethod rewriteMethod = getQueryConfigHandler()
          .getAttribute(MultiTermRewriteMethodAttribute.class)
          .getMultiTermRewriteMethod();

      node.setTag(MultiTermRewriteMethodAttribute.TAG_ID, rewriteMethod);

    }

    return node;
  }

  protected QueryNode preProcessNode(QueryNode node) {
    return node;
  }

  protected List<QueryNode> setChildrenOrder(List<QueryNode> children) {
    return children;
  }
}
