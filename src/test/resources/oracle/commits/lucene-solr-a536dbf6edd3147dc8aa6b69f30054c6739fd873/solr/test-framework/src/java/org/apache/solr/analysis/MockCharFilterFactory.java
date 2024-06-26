package org.apache.solr.analysis;

/*
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

import java.util.Map;

import org.apache.lucene.analysis.CharStream;
import org.apache.lucene.analysis.MockCharFilter;
import org.apache.lucene.analysis.util.CharFilterFactory;

/**
 * Factory for {@link MockCharFilter} for testing purposes.
 */
public class MockCharFilterFactory extends CharFilterFactory {
  int remainder;

  @Override
  public void init(Map<String,String> args) {
    super.init(args);
    String sval = args.get("remainder");
    if (sval == null) {
      throw new IllegalArgumentException("remainder is mandatory");
    }
    remainder = Integer.parseInt(sval);
  }

  @Override
  public CharStream create(CharStream input) {
    return new MockCharFilter(input, remainder);
  }
}
