package org.apache.lucene.store;

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

import java.util.Collections;
import java.util.Random;
import java.io.File;

import org.apache.lucene.util.LuceneTestCase;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;

public class TestWindowsMMap extends LuceneTestCase {
  
  private final static String alphabet = "abcdefghijklmnopqrstuvwzyz";
  private Random random;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    random = newRandom();
  }
  
  private String randomToken() {
    int tl = 1 + random.nextInt(7);
    StringBuilder sb = new StringBuilder();
    for(int cx = 0; cx < tl; cx ++) {
      int c = random.nextInt(25);
      sb.append(alphabet.substring(c, c+1));
    }
    return sb.toString();
  }
  
  private String randomField() {
    int fl = 1 + random.nextInt(3);
    StringBuilder fb = new StringBuilder();
    for(int fx = 0; fx < fl; fx ++) {
      fb.append(randomToken());
      fb.append(" ");
    }
    return fb.toString();
  }
  
  private final static String storePathname = 
    new File(System.getProperty("tempDir"),"testLuceneMmap").getAbsolutePath();

  public void testMmapIndex() throws Exception {
    FSDirectory storeDirectory;
    storeDirectory = new MMapDirectory(new File(storePathname), null);

    // plan to add a set of useful stopwords, consider changing some of the
    // interior filters.
    StandardAnalyzer analyzer = new StandardAnalyzer(TEST_VERSION_CURRENT, Collections.emptySet());
    // TODO: something about lock timeouts and leftover locks.
    IndexWriter writer = new IndexWriter(storeDirectory, new IndexWriterConfig(
        TEST_VERSION_CURRENT, analyzer)
        .setOpenMode(OpenMode.CREATE));
    IndexSearcher searcher = new IndexSearcher(storeDirectory, true);
    
    for(int dx = 0; dx < 1000; dx ++) {
      String f = randomField();
      Document doc = new Document();
      doc.add(new Field("data", f, Field.Store.YES, Field.Index.ANALYZED));	
      writer.addDocument(doc);
    }
    
    searcher.close();
    writer.close();
                rmDir(new File(storePathname));
  }

        private void rmDir(File dir) {
          File[] files = dir.listFiles();
          for (int i = 0; i < files.length; i++) {
            files[i].delete();
          }
          dir.delete();
        }
}
