package org.apache.lucene.analysis.ar;

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

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.KeywordMarkerTokenFilter;
import org.apache.lucene.util.Version;

/**
 * Test the Arabic Normalization Filter
 *
 */
public class TestArabicStemFilter extends BaseTokenStreamTestCase {
  
  public void testAlPrefix() throws IOException {
    check("الحسن", "حسن");
  }    

  public void testWalPrefix() throws IOException {
    check("والحسن", "حسن");
  }    
  
  public void testBalPrefix() throws IOException {
    check("بالحسن", "حسن");
  }    
  
  public void testKalPrefix() throws IOException {
    check("كالحسن", "حسن");
  }    
  
  public void testFalPrefix() throws IOException {
    check("فالحسن", "حسن");
  }    

  public void testLlPrefix() throws IOException {
    check("للاخر", "اخر"); 
  }
  
  public void testWaPrefix() throws IOException {
    check("وحسن", "حسن");
  } 
  
  public void testAhSuffix() throws IOException {
    check("زوجها", "زوج");
  } 
  
  public void testAnSuffix() throws IOException {
    check("ساهدان", "ساهد");
  } 
  
  public void testAtSuffix() throws IOException {
    check("ساهدات", "ساهد");
  } 
  
  public void testWnSuffix() throws IOException {
    check("ساهدون", "ساهد");
  } 
  
  public void testYnSuffix() throws IOException {
    check("ساهدين", "ساهد");
  } 
  
  public void testYhSuffix() throws IOException {
    check("ساهديه", "ساهد");
  } 

  public void testYpSuffix() throws IOException {
    check("ساهدية", "ساهد");
  } 
  
  public void testHSuffix() throws IOException {
    check("ساهده", "ساهد");
  } 
  
  public void testPSuffix() throws IOException {
    check("ساهدة", "ساهد");
  }
  
  public void testYSuffix() throws IOException {
    check("ساهدي", "ساهد");
  }
  
  public void testComboPrefSuf() throws IOException {
    check("وساهدون", "ساهد");
  }
  
  public void testComboSuf() throws IOException {
    check("ساهدهات", "ساهد");
  }
  
  public void testShouldntStem() throws IOException {
    check("الو", "الو");
  }

  public void testNonArabic() throws IOException {
    check("English", "English");
  }
  
  public void testWithKeywordAttribute() throws IOException {
    CharArraySet set = new CharArraySet(Version.LUCENE_CURRENT, 1, true);
    set.add("ساهدهات");
    ArabicLetterTokenizer tokenStream  = new ArabicLetterTokenizer(Version.LUCENE_CURRENT, new StringReader("ساهدهات"));

    ArabicStemFilter filter = new ArabicStemFilter(new KeywordMarkerTokenFilter(tokenStream, set));
    assertTokenStreamContents(filter, new String[]{"ساهدهات"});
  }

  private void check(final String input, final String expected) throws IOException {
    ArabicLetterTokenizer tokenStream  = new ArabicLetterTokenizer(Version.LUCENE_CURRENT, new StringReader(input));
    ArabicStemFilter filter = new ArabicStemFilter(tokenStream);
    assertTokenStreamContents(filter, new String[]{expected});
  }
}
