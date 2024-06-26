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
 * A {@link ModifierQueryNode} indicates the modifier value (+,-,?,NONE) for
 * each term on the query string. For example "+t1 -t2 t3" will have a tree of:
 * <blockquote>
 * &lt;BooleanQueryNode&gt; &lt;ModifierQueryNode modifier="MOD_REQ"&gt; &lt;t1/&gt;
 * &lt;/ModifierQueryNode&gt; &lt;ModifierQueryNode modifier="MOD_NOT"&gt; &lt;t2/&gt;
 * &lt;/ModifierQueryNode&gt; &lt;t3/&gt; &lt;/BooleanQueryNode&gt;
 * </blockquote>
 */
public class ModifierQueryNode extends QueryNodeImpl {

  private static final long serialVersionUID = -391209837953928169L;

  public enum Modifier {
    MOD_NONE, MOD_NOT, MOD_REQ;

    public String toString() {
      switch (this) {
      case MOD_NONE:
        return "MOD_NONE";
      case MOD_NOT:
        return "MOD_NOT";
      case MOD_REQ:
        return "MOD_REQ";
      }
      // this code is never executed
      return "MOD_DEFAULT";
    }

    public String toDigitString() {
      switch (this) {
      case MOD_NONE:
        return "";
      case MOD_NOT:
        return "-";
      case MOD_REQ:
        return "+";
      }
      // this code is never executed
      return "";
    }

    public String toLargeString() {
      switch (this) {
      case MOD_NONE:
        return "";
      case MOD_NOT:
        return "NOT ";
      case MOD_REQ:
        return "+";
      }
      // this code is never executed
      return "";
    }
  }

  private Modifier modifier = Modifier.MOD_NONE;

  /**
   * Used to store the modifier value on the original query string
   * 
   * @param query
   *          - QueryNode subtree
   * @param mod
   *          - Modifier Value
   */
  public ModifierQueryNode(QueryNode query, Modifier mod) {
    if (query == null) {
      throw new QueryNodeError(new MessageImpl(
          QueryParserMessages.PARAMETER_VALUE_NOT_SUPPORTED, "query", "null"));
    }

    allocate();
    setLeaf(false);
    add(query);
    this.modifier = mod;
  }

  public QueryNode getChild() {
    return getChildren().get(0);
  }

  public Modifier getModifier() {
    return this.modifier;
  }

  public String toString() {
    return "<modifier operation='" + this.modifier.toString() + "'>" + "\n"
        + getChild().toString() + "\n</modifier>";
  }

  public CharSequence toQueryString(EscapeQuerySyntax escapeSyntaxParser) {
    if (getChild() == null)
      return "";

    String leftParenthensis = "";
    String rightParenthensis = "";

    if (getChild() != null && getChild() instanceof ModifierQueryNode) {
      leftParenthensis = "(";
      rightParenthensis = ")";
    }

    if (getChild() instanceof BooleanQueryNode) {
      return this.modifier.toLargeString() + leftParenthensis
          + getChild().toQueryString(escapeSyntaxParser) + rightParenthensis;
    } else {
      return this.modifier.toDigitString() + leftParenthensis
          + getChild().toQueryString(escapeSyntaxParser) + rightParenthensis;
    }
  }

  public QueryNode cloneTree() throws CloneNotSupportedException {
    ModifierQueryNode clone = (ModifierQueryNode) super.cloneTree();

    clone.modifier = this.modifier;

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
