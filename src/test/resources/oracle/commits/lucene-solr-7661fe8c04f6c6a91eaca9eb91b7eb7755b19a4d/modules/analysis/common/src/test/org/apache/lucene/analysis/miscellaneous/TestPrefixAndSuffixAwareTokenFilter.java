package org.apache.lucene.analysis.miscellaneous;

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

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;

import java.io.IOException;
import java.io.StringReader;

public class TestPrefixAndSuffixAwareTokenFilter extends BaseTokenStreamTestCase {

  public void test() throws IOException {

    PrefixAndSuffixAwareTokenFilter ts = new PrefixAndSuffixAwareTokenFilter(
        new SingleTokenTokenStream(createToken("^", 0, 0)),
        new WhitespaceTokenizer(TEST_VERSION_CURRENT, new StringReader("hello world")),
        new SingleTokenTokenStream(createToken("$", 0, 0)));

    assertTokenStreamContents(ts,
        new String[] { "^", "hello", "world", "$" },
        new int[] { 0, 0, 6, 11 },
        new int[] { 0, 5, 11, 11 });
  }

  private static Token createToken(String term, int start, int offset)
  {
    Token token = new Token(start, offset);
    token.setTermBuffer(term);
    return token;
  }
}
