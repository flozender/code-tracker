package org.apache.lucene.search;

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

import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util._TestUtil;

public class TestCachingSpanFilter extends LuceneTestCase {

  public void testEnforceDeletions() throws Exception {
    Directory dir = newDirectory();
    RandomIndexWriter writer = new RandomIndexWriter(
        random,
        dir,
        newIndexWriterConfig(random, TEST_VERSION_CURRENT, new MockAnalyzer(random)).
            setMergeScheduler(new SerialMergeScheduler()).
            // asserts below requires no unexpected merges:
            setMergePolicy(newLogMergePolicy(10))
    );

    // NOTE: cannot use writer.getReader because RIW (on
    // flipping a coin) may give us a newly opened reader,
    // but we use .reopen on this reader below and expect to
    // (must) get an NRT reader:
    IndexReader reader = IndexReader.open(writer.w, true);
    // same reason we don't wrap?
    IndexSearcher searcher = newSearcher(reader, false);

    // add a doc, refresh the reader, and check that its there
    Document doc = new Document();
    FieldType customType = new FieldType(TextField.TYPE_STORED);
    customType.setTokenized(false);
    doc.add(newField("id", "1", customType));
    writer.addDocument(doc);

    reader = refreshReader(reader);
    searcher.close();
    searcher = newSearcher(reader, false);

    TopDocs docs = searcher.search(new MatchAllDocsQuery(), 1);
    assertEquals("Should find a hit...", 1, docs.totalHits);

    final SpanFilter startFilter = new SpanQueryFilter(new SpanTermQuery(new Term("id", "1")));

    CachingSpanFilter filter = new CachingSpanFilter(startFilter);

    docs = searcher.search(new MatchAllDocsQuery(), filter, 1);
    assertEquals("[query + filter] Should find a hit...", 1, docs.totalHits);
    int missCount = filter.missCount;
    assertTrue(missCount > 0);
    Query constantScore = new ConstantScoreQuery(filter);
    docs = searcher.search(constantScore, 1);
    assertEquals("[just filter] Should find a hit...", 1, docs.totalHits);
    assertEquals(missCount, filter.missCount);

    // NOTE: important to hold ref here so GC doesn't clear
    // the cache entry!  Else the assert below may sometimes
    // fail:
    IndexReader oldReader = reader;

    writer.addDocument(doc);
    reader = refreshReader(reader);
    searcher.close();
    searcher = newSearcher(reader, false);
        
    docs = searcher.search(new MatchAllDocsQuery(), filter, 1);
    assertEquals("[query + filter] Should find 2 hits...", 2, docs.totalHits);
    assertTrue(filter.missCount > missCount);
    missCount = filter.missCount;

    constantScore = new ConstantScoreQuery(filter);
    docs = searcher.search(constantScore, 1);
    assertEquals("[just filter] Should find a hit...", 2, docs.totalHits);
    assertEquals(missCount, filter.missCount);

    // NOTE: important to hold ref here so GC doesn't clear
    // the cache entry!  Else the assert below may sometimes
    // fail:
    IndexReader oldReader2 = reader;

    // now delete the doc, refresh the reader, and see that it's not there
    writer.deleteDocuments(new Term("id", "1"));

    reader = refreshReader(reader);
    searcher.close();
    searcher = newSearcher(reader, false);

    docs = searcher.search(new MatchAllDocsQuery(), filter, 1);
    assertEquals("[query + filter] Should *not* find a hit...", 0, docs.totalHits);
    assertEquals(missCount, filter.missCount);

    docs = searcher.search(constantScore, 1);
    assertEquals("[just filter] Should *not* find a hit...", 0, docs.totalHits);
    assertEquals(missCount, filter.missCount);

    // NOTE: silliness to make sure JRE does not optimize
    // away our holding onto oldReader to prevent
    // CachingWrapperFilter's WeakHashMap from dropping the
    // entry:
    assertTrue(oldReader != null);
    assertTrue(oldReader2 != null);

    searcher.close();
    writer.close();
    reader.close();
    dir.close();
  }

  private static IndexReader refreshReader(IndexReader reader) throws IOException {
    IndexReader oldReader = reader;
    reader = IndexReader.openIfChanged(reader);
    if (reader != null) {
      oldReader.close();
      return reader;
    } else {
      return oldReader;
    }
  }
  
}
