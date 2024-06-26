package org.apache.lucene.search.spell;

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

import junit.framework.Assert;

import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.spell.WordBreakSpellChecker.BreakSuggestionSortMethod;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.English;
import org.apache.lucene.util.LuceneTestCase;

public class TestWordBreakSpellChecker extends LuceneTestCase {
  private Directory dir = null;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    dir = newDirectory();
    RandomIndexWriter writer = new RandomIndexWriter(random(), dir, new MockAnalyzer(random(), MockTokenizer.WHITESPACE, true));

    for (int i = 900; i < 1112; i++) {
      Document doc = new Document();
      String num = English.intToEnglish(i).replaceAll("[-]", " ").replaceAll("[,]", "");
      doc.add(newTextField("numbers", num, Field.Store.NO));
      writer.addDocument(doc);
    }
    
    {
      Document doc = new Document();
      doc.add(newTextField("numbers", "thou hast sand betwixt thy toes", Field.Store.NO));
      writer.addDocument(doc);
    }
    {
      Document doc = new Document();
      doc.add(newTextField("numbers", "hundredeight eightyeight yeight", Field.Store.NO));
      writer.addDocument(doc);
    }
    {
      Document doc = new Document();
      doc.add(newTextField("numbers", "tres y cinco", Field.Store.NO));
      writer.addDocument(doc);
    }
    
    writer.commit();
    writer.close();
  }
  
  @Override
  public void tearDown() throws Exception {
    if(dir!=null) {
      dir.close();
      dir = null;
    }
    super.tearDown();
  } 
  public void testCombiningWords() throws Exception {
    IndexReader ir = null;
    try {
      ir = DirectoryReader.open(dir);
      WordBreakSpellChecker wbsp = new WordBreakSpellChecker();
      
      {        
        Term[] terms = { 
            new Term("numbers", "one"),
            new Term("numbers", "hun"),
            new Term("numbers", "dred"),
            new Term("numbers", "eight"),
            new Term("numbers", "y"),
            new Term("numbers", "eight"),
        };
        wbsp.setMaxChanges(3);
        wbsp.setMaxCombineWordLength(20);
        wbsp.setMinSuggestionFrequency(1);
        CombineSuggestion[] cs = wbsp.suggestWordCombinations(terms, 10, ir, SuggestMode.SUGGEST_ALWAYS);
        Assert.assertTrue(cs.length==5);
        
        Assert.assertTrue(cs[0].originalTermIndexes.length==2);
        Assert.assertTrue(cs[0].originalTermIndexes[0]==1);
        Assert.assertTrue(cs[0].originalTermIndexes[1]==2);
        Assert.assertTrue(cs[0].suggestion.string.equals("hundred"));
        Assert.assertTrue(cs[0].suggestion.score==1);
        
        Assert.assertTrue(cs[1].originalTermIndexes.length==2);
        Assert.assertTrue(cs[1].originalTermIndexes[0]==3);
        Assert.assertTrue(cs[1].originalTermIndexes[1]==4);
        Assert.assertTrue(cs[1].suggestion.string.equals("eighty"));
        Assert.assertTrue(cs[1].suggestion.score==1);        
        
        Assert.assertTrue(cs[2].originalTermIndexes.length==2);
        Assert.assertTrue(cs[2].originalTermIndexes[0]==4);
        Assert.assertTrue(cs[2].originalTermIndexes[1]==5);
        Assert.assertTrue(cs[2].suggestion.string.equals("yeight"));
        Assert.assertTrue(cs[2].suggestion.score==1);
        
        for(int i=3 ; i<5 ; i++) {
          Assert.assertTrue(cs[i].originalTermIndexes.length==3);
          Assert.assertTrue(cs[i].suggestion.score==2);
          Assert.assertTrue(
              (cs[i].originalTermIndexes[0]==1 && 
               cs[i].originalTermIndexes[1]==2 && 
               cs[i].originalTermIndexes[2]==3 && 
               cs[i].suggestion.string.equals("hundredeight")) ||
              (cs[i].originalTermIndexes[0]==3 &&
               cs[i].originalTermIndexes[1]==4 &&
               cs[i].originalTermIndexes[2]==5 &&
               cs[i].suggestion.string.equals("eightyeight"))
         );
        }     
        
        cs = wbsp.suggestWordCombinations(terms, 5, ir, SuggestMode.SUGGEST_WHEN_NOT_IN_INDEX);
        Assert.assertTrue(cs.length==2);
        Assert.assertTrue(cs[0].originalTermIndexes.length==2);
        Assert.assertTrue(cs[0].suggestion.score==1);
        Assert.assertTrue(cs[0].originalTermIndexes[0]==1);
        Assert.assertTrue(cs[0].originalTermIndexes[1]==2);
        Assert.assertTrue(cs[0].suggestion.string.equals("hundred"));
        Assert.assertTrue(cs[0].suggestion.score==1);
        
        Assert.assertTrue(cs[1].originalTermIndexes.length==3);
        Assert.assertTrue(cs[1].suggestion.score==2);
        Assert.assertTrue(cs[1].originalTermIndexes[0] == 1);
        Assert.assertTrue(cs[1].originalTermIndexes[1] == 2);
        Assert.assertTrue(cs[1].originalTermIndexes[2] == 3);
        Assert.assertTrue(cs[1].suggestion.string.equals("hundredeight"));
      }
    } catch(Exception e) {
      throw e;
    } finally {
      try { ir.close(); } catch(Exception e1) { }
    }    
  }
  
  public void testBreakingWords() throws Exception {
    IndexReader ir = null;
    try {
      ir = DirectoryReader.open(dir);
      WordBreakSpellChecker wbsp = new WordBreakSpellChecker();
      
      {
        Term term = new Term("numbers", "ninetynine");
        wbsp.setMaxChanges(1);
        wbsp.setMinBreakWordLength(1);
        wbsp.setMinSuggestionFrequency(1);
        SuggestWord[][] sw = wbsp.suggestWordBreaks(term, 5, ir, SuggestMode.SUGGEST_WHEN_NOT_IN_INDEX, BreakSuggestionSortMethod.NUM_CHANGES_THEN_MAX_FREQUENCY);
        Assert.assertTrue(sw.length==1);
        Assert.assertTrue(sw[0].length==2);
        Assert.assertTrue(sw[0][0].string.equals("ninety"));
        Assert.assertTrue(sw[0][1].string.equals("nine"));
        Assert.assertTrue(sw[0][0].score == 1);
        Assert.assertTrue(sw[0][1].score == 1);
      }
      {
        Term term = new Term("numbers", "onethousand");
        wbsp.setMaxChanges(1);
        wbsp.setMinBreakWordLength(1);
        wbsp.setMinSuggestionFrequency(1);
        SuggestWord[][] sw = wbsp.suggestWordBreaks(term, 2, ir, SuggestMode.SUGGEST_WHEN_NOT_IN_INDEX, BreakSuggestionSortMethod.NUM_CHANGES_THEN_MAX_FREQUENCY);
        Assert.assertTrue(sw.length==1);
        Assert.assertTrue(sw[0].length==2);
        Assert.assertTrue(sw[0][0].string.equals("one"));
        Assert.assertTrue(sw[0][1].string.equals("thousand"));
        Assert.assertTrue(sw[0][0].score == 1);
        Assert.assertTrue(sw[0][1].score == 1);
        
        wbsp.setMaxChanges(2);
        wbsp.setMinSuggestionFrequency(1);
        sw = wbsp.suggestWordBreaks(term, 1, ir, SuggestMode.SUGGEST_WHEN_NOT_IN_INDEX, BreakSuggestionSortMethod.NUM_CHANGES_THEN_MAX_FREQUENCY);
        Assert.assertTrue(sw.length==1);
        Assert.assertTrue(sw[0].length==2);
        
        wbsp.setMaxChanges(2);
        wbsp.setMinSuggestionFrequency(2);
        sw = wbsp.suggestWordBreaks(term, 2, ir, SuggestMode.SUGGEST_WHEN_NOT_IN_INDEX, BreakSuggestionSortMethod.NUM_CHANGES_THEN_MAX_FREQUENCY);
        Assert.assertTrue(sw.length==1);
        Assert.assertTrue(sw[0].length==2);
        
        wbsp.setMaxChanges(2);
        wbsp.setMinSuggestionFrequency(1);
        sw = wbsp.suggestWordBreaks(term, 2, ir, SuggestMode.SUGGEST_WHEN_NOT_IN_INDEX, BreakSuggestionSortMethod.NUM_CHANGES_THEN_MAX_FREQUENCY);
        Assert.assertTrue(sw.length==2);
        Assert.assertTrue(sw[0].length==2);
        Assert.assertTrue(sw[0][0].string.equals("one"));
        Assert.assertTrue(sw[0][1].string.equals("thousand"));
        Assert.assertTrue(sw[0][0].score == 1);
        Assert.assertTrue(sw[0][1].score == 1);
        Assert.assertTrue(sw[0][1].freq>1);
        Assert.assertTrue(sw[0][0].freq>sw[0][1].freq);
        Assert.assertTrue(sw[1].length==3);
        Assert.assertTrue(sw[1][0].string.equals("one"));
        Assert.assertTrue(sw[1][1].string.equals("thou"));
        Assert.assertTrue(sw[1][2].string.equals("sand"));
        Assert.assertTrue(sw[1][0].score == 2);
        Assert.assertTrue(sw[1][1].score == 2);
        Assert.assertTrue(sw[1][2].score == 2);
        Assert.assertTrue(sw[1][0].freq>1);
        Assert.assertTrue(sw[1][1].freq==1);
        Assert.assertTrue(sw[1][2].freq==1);
      }
      {
        Term term = new Term("numbers", "onethousandonehundredeleven");
        wbsp.setMaxChanges(3);
        wbsp.setMinBreakWordLength(1);
        wbsp.setMinSuggestionFrequency(1);
        SuggestWord[][] sw = wbsp.suggestWordBreaks(term, 5, ir, SuggestMode.SUGGEST_WHEN_NOT_IN_INDEX, BreakSuggestionSortMethod.NUM_CHANGES_THEN_MAX_FREQUENCY);
        Assert.assertTrue(sw.length==0);
        
        wbsp.setMaxChanges(4);
        sw = wbsp.suggestWordBreaks(term, 5, ir, SuggestMode.SUGGEST_WHEN_NOT_IN_INDEX, BreakSuggestionSortMethod.NUM_CHANGES_THEN_MAX_FREQUENCY);
        Assert.assertTrue(sw.length==1);
        Assert.assertTrue(sw[0].length==5);
        
        wbsp.setMaxChanges(5);
        sw = wbsp.suggestWordBreaks(term, 5, ir, SuggestMode.SUGGEST_WHEN_NOT_IN_INDEX, BreakSuggestionSortMethod.NUM_CHANGES_THEN_MAX_FREQUENCY);
        Assert.assertTrue(sw.length==2);
        Assert.assertTrue(sw[0].length==5);
        Assert.assertTrue(sw[0][1].string.equals("thousand"));
        Assert.assertTrue(sw[1].length==6);
        Assert.assertTrue(sw[1][1].string.equals("thou"));
        Assert.assertTrue(sw[1][2].string.equals("sand"));
      }
      
    } catch(Exception e) {
      throw e;
    } finally {
      try { ir.close(); } catch(Exception e1) { }
    }    
  }
 }
