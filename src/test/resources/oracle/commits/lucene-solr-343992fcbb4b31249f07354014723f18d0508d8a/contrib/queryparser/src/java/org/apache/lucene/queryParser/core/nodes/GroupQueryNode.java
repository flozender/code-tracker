package org.apache.lucene.queryParser.core.nodes;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.messages.MessageImpl;
import org.apache.lucene.queryParser.core.QueryNodeError;
import org.apache.lucene.queryParser.core.messages.QueryParserMessages;
import org.apache.lucene.queryParser.core.parser.EscapeQuerySyntax;

/**
 * A {@link GroupQueryNode} represents a location where the original user typed
 * real parenthesis on the query string. This class is useful for queries like:
 * a) a AND b OR c b) ( a AND b) OR c
 * 
 * Parenthesis might be used to define the boolean operation precedence.
 */
public class GroupQueryNode extends QueryNodeImpl {

  private static final long serialVersionUID = -9204673493869114999L;

  /**
   * This QueryNode is used to identify parenthesis on the original query string
   */
  public GroupQueryNode(QueryNode query) {
    if (query == null) {
      throw new QueryNodeError(new MessageImpl(
          QueryParserMessages.PARAMETER_VALUE_NOT_SUPPORTED, "query", "null"));
    }

    allocate();
    setLeaf(false);
    add(query);
  }

  public QueryNode getChild() {
    return getChildren().get(0);
  }

  public String toString() {
    return "<group>" + "\n" + getChild().toString() + "\n</group>";
  }

  public CharSequence toQueryString(EscapeQuerySyntax escapeSyntaxParser) {
    if (getChild() == null)
      return "";

    return "( " + getChild().toQueryString(escapeSyntaxParser) + " )";
  }

  public QueryNode cloneTree() throws CloneNotSupportedException {
    GroupQueryNode clone = (GroupQueryNode) super.cloneTree();

    return clone;
  }

  /**
   * @param child
   */
  public void setChild(QueryNode child) {
    List<QueryNode> list = new ArrayList<QueryNode>();
    list.add(child);
    this.set(list);
  }

}
