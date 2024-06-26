package org.apache.lucene.analysis.sinks;
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

import org.apache.lucene.analysis.SinkTokenizer;
import org.apache.lucene.analysis.Token;

import java.util.List;


/**
 * If the {@link org.apache.lucene.analysis.Token#type()} matches the passed in <code>typeToMatch</code> then
 * add it to the sink
 *
 **/
public class TokenTypeSinkTokenizer extends SinkTokenizer {

  private String typeToMatch;

  public TokenTypeSinkTokenizer(String typeToMatch) {
    this.typeToMatch = typeToMatch;
  }

  public TokenTypeSinkTokenizer(int initCap, String typeToMatch) {
    super(initCap);
    this.typeToMatch = typeToMatch;
  }

  public TokenTypeSinkTokenizer(List/*<Token>*/ input, String typeToMatch) {
    super(input);
    this.typeToMatch = typeToMatch;
  }

  public void add(Token t) {
    //check to see if this is a Category
    if (t != null && typeToMatch.equals(t.type())){
      lst.add(t.clone());
    }
  }
}
