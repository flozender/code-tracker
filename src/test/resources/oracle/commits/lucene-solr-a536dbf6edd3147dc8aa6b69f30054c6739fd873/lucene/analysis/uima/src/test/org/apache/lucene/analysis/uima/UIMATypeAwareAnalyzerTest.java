package org.apache.lucene.analysis.uima;

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

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.TokenStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.StringReader;

/**
 * Testcase for {@link UIMATypeAwareAnalyzer}
 */
public class UIMATypeAwareAnalyzerTest extends BaseTokenStreamTestCase {

  private UIMATypeAwareAnalyzer analyzer;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    analyzer = new UIMATypeAwareAnalyzer("/uima/AggregateSentenceAE.xml",
        "org.apache.uima.TokenAnnotation", "posTag");
  }

  @After
  public void tearDown() throws Exception {
    analyzer.close();
    super.tearDown();
  }

  @Test
  public void baseUIMATypeAwareAnalyzerStreamTest() throws Exception {

    // create a token stream
    TokenStream ts = analyzer.tokenStream("text", new StringReader("the big brown fox jumped on the wood"));

    // check that 'the big brown fox jumped on the wood' tokens have the expected PoS types
    assertTokenStreamContents(ts,
        new String[]{"the", "big", "brown", "fox", "jumped", "on", "the", "wood"},
        new String[]{"at", "jj", "jj", "nn", "vbd", "in", "at", "nn"});

  }

  @Test
  @Ignore("Where is TestAggregatedSentenceAE.xml")
  public void testRandomStrings() throws Exception {
    checkRandomData(random(), new UIMATypeAwareAnalyzer("/uima/TestAggregateSentenceAE.xml",
        "org.apache.lucene.uima.ts.TokenAnnotation", "pos"), 1000 * RANDOM_MULTIPLIER);
  }

}
