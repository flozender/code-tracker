package org.apache.lucene.queryParser;

/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Reader;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 * Test QueryParser's ability to deal with Analyzers that return more
 * than one token per position.
 * 
 * @author Daniel Naber
 */
public class TestMultiAnalyzer extends TestCase {

  private static int multiToken = 0;

  public void testMultiAnalyzer() throws ParseException {
    
    QueryParser qp = new QueryParser("", new MultiAnalyzer());

    // trivial, no multiple tokens:
    assertEquals("foo", qp.parse("foo").toString());
    assertEquals("foo foobar", qp.parse("foo foobar").toString());

    // two tokens at the same position:
    assertEquals("(multi multi2) foo", qp.parse("multi foo").toString());
    assertEquals("foo (multi multi2)", qp.parse("foo multi").toString());
    assertEquals("(multi multi2) (multi multi2)", qp.parse("multi multi").toString());
    assertEquals("+(foo (multi multi2)) +(bar (multi multi2))",
        qp.parse("+(foo multi) +(bar multi)").toString());
    assertEquals("+(foo (multi multi2)) field:\"bar (multi multi2)\"",
        qp.parse("+(foo multi) field:\"bar multi\"").toString());

    // phrases:
    assertEquals("\"(multi multi2) foo\"", qp.parse("\"multi foo\"").toString());
    assertEquals("\"foo (multi multi2)\"", qp.parse("\"foo multi\"").toString());
    assertEquals("\"foo (multi multi2) foobar (multi multi2)\"",
        qp.parse("\"foo multi foobar multi\"").toString());

    // fields:
    assertEquals("(field:multi field:multi2) field:foo", qp.parse("field:multi field:foo").toString());
    assertEquals("field:\"(multi multi2) foo\"", qp.parse("field:\"multi foo\"").toString());

    // three tokens at one position:
    assertEquals("triplemulti multi3 multi2", qp.parse("triplemulti").toString());
    assertEquals("foo (triplemulti multi3 multi2) foobar",
        qp.parse("foo triplemulti foobar").toString());

    // phrase with non-default slop:
    assertEquals("\"(multi multi2) foo\"~10", qp.parse("\"multi foo\"~10").toString());

    // phrase with non-default boost:
    assertEquals("\"(multi multi2) foo\"^2.0", qp.parse("\"multi foo\"^2").toString());

    // non-default operator:
    qp.setDefaultOperator(QueryParser.AND_OPERATOR);
    assertEquals("+(multi multi2) +foo", qp.parse("multi foo").toString());

  }

  public void testPosIncrementAnalyzer() throws ParseException {
    QueryParser qp = new QueryParser("", new PosIncrementAnalyzer());
    assertEquals("quick brown", qp.parse("the quick brown").toString());
    assertEquals("\"quick brown\"", qp.parse("\"the quick brown\"").toString());
    assertEquals("quick brown fox", qp.parse("the quick brown fox").toString());
    assertEquals("\"quick brown fox\"", qp.parse("\"the quick brown fox\"").toString());
  }
  
  /**
   * Expands "multi" to "multi" and "multi2", both at the same position,
   * and expands "triplemulti" to "triplemulti", "multi3", and "multi2".  
   */
  private class MultiAnalyzer extends Analyzer {

    public MultiAnalyzer() {
    }

    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = new StandardTokenizer(reader);
      result = new TestFilter(result);
      result = new LowerCaseFilter(result);
      return result;
    }
  }

  private final class TestFilter extends TokenFilter {
    
    private org.apache.lucene.analysis.Token prevToken;
    
    public TestFilter(TokenStream in) {
      super(in);
    }

    public final org.apache.lucene.analysis.Token next() throws java.io.IOException {
      if (multiToken > 0) {
        org.apache.lucene.analysis.Token token = 
          new org.apache.lucene.analysis.Token("multi"+(multiToken+1), prevToken.startOffset(),
          prevToken.endOffset(), prevToken.type());
        token.setPositionIncrement(0);
        multiToken--;
        return token;
      } else {
        org.apache.lucene.analysis.Token t = input.next();
        prevToken = t;
        if (t == null)
          return null;
        String text = t.termText();
        if (text.equals("triplemulti")) {
          multiToken = 2;
          return t;
        } else if (text.equals("multi")) {
          multiToken = 1;
          return t;
        } else {
          return t;
        }
      }
    }
  }

  /**
   * Analyzes "the quick brown" as: quick(incr=2) brown(incr=1).
   * Does not work correctly for input other than "the quick brown ...".
   */
  private class PosIncrementAnalyzer extends Analyzer {

    public PosIncrementAnalyzer() {
    }

    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = new StandardTokenizer(reader);
      result = new TestPosIncrementFilter(result);
      result = new LowerCaseFilter(result);
      return result;
    }
  }

  private final class TestPosIncrementFilter extends TokenFilter {
    
    public TestPosIncrementFilter(TokenStream in) {
      super(in);
    }

    public final org.apache.lucene.analysis.Token next() throws java.io.IOException {
      for (Token t = input.next(); t != null; t = input.next()) {
        if (t.termText().equals("the")) {
          // stopword, do nothing
        } else if (t.termText().equals("quick")) {
          org.apache.lucene.analysis.Token token = 
            new org.apache.lucene.analysis.Token(t.termText(), t.startOffset(),
                t.endOffset(), t.type());
          token.setPositionIncrement(2);
          return token;
        } else {
          org.apache.lucene.analysis.Token token = 
            new org.apache.lucene.analysis.Token(t.termText(), t.startOffset(),
                t.endOffset(), t.type());
          token.setPositionIncrement(1);
          return token;
        }
      }
      return null;
    }
  }

}
