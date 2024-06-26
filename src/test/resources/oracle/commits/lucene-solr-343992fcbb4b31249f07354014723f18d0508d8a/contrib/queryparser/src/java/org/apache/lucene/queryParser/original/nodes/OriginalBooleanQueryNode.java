package org.apache.lucene.queryParser.original.nodes;

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

import org.apache.lucene.queryParser.core.nodes.BooleanQueryNode;
import org.apache.lucene.queryParser.core.nodes.QueryNode;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Similarity;

/**
 * A {@link OriginalBooleanQueryNode} has the same behavior as
 * {@link BooleanQueryNode}. It only indicates if the coord should be enabled or
 * not for this boolean query. <br/>
 * 
 * @see Similarity#coord(int, int)
 * @see BooleanQuery
 */
public class OriginalBooleanQueryNode extends BooleanQueryNode {

  private static final long serialVersionUID = 1938287817191138787L;

  private boolean disableCoord;

  /**
   * @param clauses
   */
  public OriginalBooleanQueryNode(List<QueryNode> clauses, boolean disableCoord) {
    super(clauses);

    this.disableCoord = disableCoord;

  }

  public boolean isDisableCoord() {
    return this.disableCoord;
  }

}
