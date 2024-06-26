package org.apache.lucene.analysis.ngram;

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

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;

import java.io.StringReader;
import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * Tests {@link NGramTokenFilter} for correctness.
 * @author Otis Gospodnetic
 */
public class NGramTokenFilterTest extends TestCase {
    private TokenStream input;
    private ArrayList tokens = new ArrayList();
    
    public void setUp() {
        input = new WhitespaceTokenizer(new StringReader("abcde"));
    }

    public void testInvalidInput() throws Exception {
        boolean gotException = false;
        try {        
            new NGramTokenFilter(input, 2, 1);
        } catch (IllegalArgumentException e) {
            gotException = true;
        }
        assertTrue(gotException);
    }

    public void testInvalidInput2() throws Exception {
        boolean gotException = false;
        try {        
            new NGramTokenFilter(input, 0, 1);
        } catch (IllegalArgumentException e) {
            gotException = true;
        }
        assertTrue(gotException);
    }

    public void testUnigrams() throws Exception {
      NGramTokenFilter filter = new NGramTokenFilter(input, 1, 1);

      final Token reusableToken = new Token();
        for (Token nextToken = filter.next(reusableToken); nextToken != null; nextToken = filter.next(reusableToken)) {
            tokens.add(nextToken.toString());
//          System.out.println(token.term());
//          System.out.println(token);
//          Thread.sleep(1000);
        }

        assertEquals(5, tokens.size());
        ArrayList exp = new ArrayList();
        exp.add("(a,0,1)"); exp.add("(b,1,2)"); exp.add("(c,2,3)"); exp.add("(d,3,4)"); exp.add("(e,4,5)");
        assertEquals(exp, tokens);
    }

    public void testBigrams() throws Exception {
      NGramTokenFilter filter = new NGramTokenFilter(input, 2, 2);
      final Token reusableToken = new Token();
        for (Token nextToken = filter.next(reusableToken); nextToken != null; nextToken = filter.next(reusableToken)) {
            tokens.add(nextToken.toString());
//          System.out.println(token.term());
//          System.out.println(token);
//          Thread.sleep(1000);
        }

        assertEquals(4, tokens.size());
        ArrayList exp = new ArrayList();
        exp.add("(ab,0,2)"); exp.add("(bc,1,3)"); exp.add("(cd,2,4)"); exp.add("(de,3,5)");
        assertEquals(exp, tokens);
    }

    public void testNgrams() throws Exception {
      NGramTokenFilter filter = new NGramTokenFilter(input, 1, 3);
      final Token reusableToken = new Token();
        for (Token nextToken = filter.next(reusableToken); nextToken != null; nextToken = filter.next(reusableToken)) {
            tokens.add(nextToken.toString());
//          System.out.println(token.term());
//          System.out.println(token);
//          Thread.sleep(1000);
        }

        assertEquals(12, tokens.size());
        ArrayList exp = new ArrayList();
        exp.add("(a,0,1)"); exp.add("(b,1,2)"); exp.add("(c,2,3)"); exp.add("(d,3,4)"); exp.add("(e,4,5)");
        exp.add("(ab,0,2)"); exp.add("(bc,1,3)"); exp.add("(cd,2,4)"); exp.add("(de,3,5)");
        exp.add("(abc,0,3)"); exp.add("(bcd,1,4)"); exp.add("(cde,2,5)");
        assertEquals(exp, tokens);
    }

    public void testOversizedNgrams() throws Exception {
      NGramTokenFilter filter = new NGramTokenFilter(input, 6, 7);
      final Token reusableToken = new Token();
        for (Token nextToken = filter.next(reusableToken); nextToken != null; nextToken = filter.next(reusableToken)) {
            tokens.add(nextToken.toString());
//          System.out.println(token.term());
//          System.out.println(token);
//          Thread.sleep(1000);
        }

        assertTrue(tokens.isEmpty());
    }
}
