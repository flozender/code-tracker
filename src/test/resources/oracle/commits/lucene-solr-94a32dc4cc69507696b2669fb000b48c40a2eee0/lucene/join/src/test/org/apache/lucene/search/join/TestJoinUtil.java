package org.apache.lucene.search.join;

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

import com.carrotsearch.randomizedtesting.generators.RandomInts;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.NoMergePolicy;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.BitSetIterator;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.TestUtil;
import org.apache.lucene.util.packed.PackedInts;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class TestJoinUtil extends LuceneTestCase {

  public void testSimple() throws Exception {
    final String idField = "id";
    final String toField = "productId";

    Directory dir = newDirectory();
    RandomIndexWriter w = new RandomIndexWriter(
        random(),
        dir,
        newIndexWriterConfig(new MockAnalyzer(random())).setMergePolicy(newLogMergePolicy()));

    // 0
    Document doc = new Document();
    doc.add(new TextField("description", "random text", Field.Store.NO));
    doc.add(new TextField("name", "name1", Field.Store.NO));
    doc.add(new TextField(idField, "1", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("1")));
    w.addDocument(doc);

    // 1
    doc = new Document();
    doc.add(new TextField("price", "10.0", Field.Store.NO));
    doc.add(new TextField(idField, "2", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("2")));
    doc.add(new TextField(toField, "1", Field.Store.NO));
    doc.add(new SortedDocValuesField(toField, new BytesRef("1")));
    w.addDocument(doc);

    // 2
    doc = new Document();
    doc.add(new TextField("price", "20.0", Field.Store.NO));
    doc.add(new TextField(idField, "3", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("3")));
    doc.add(new TextField(toField, "1", Field.Store.NO));
    doc.add(new SortedDocValuesField(toField, new BytesRef("1")));
    w.addDocument(doc);

    // 3
    doc = new Document();
    doc.add(new TextField("description", "more random text", Field.Store.NO));
    doc.add(new TextField("name", "name2", Field.Store.NO));
    doc.add(new TextField(idField, "4", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("4")));
    w.addDocument(doc);
    w.commit();

    // 4
    doc = new Document();
    doc.add(new TextField("price", "10.0", Field.Store.NO));
    doc.add(new TextField(idField, "5", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("5")));
    doc.add(new TextField(toField, "4", Field.Store.NO));
    doc.add(new SortedDocValuesField(toField, new BytesRef("4")));
    w.addDocument(doc);

    // 5
    doc = new Document();
    doc.add(new TextField("price", "20.0", Field.Store.NO));
    doc.add(new TextField(idField, "6", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("6")));
    doc.add(new TextField(toField, "4", Field.Store.NO));
    doc.add(new SortedDocValuesField(toField, new BytesRef("4")));
    w.addDocument(doc);

    IndexSearcher indexSearcher = new IndexSearcher(w.getReader());
    w.close();

    // Search for product
    Query joinQuery =
        JoinUtil.createJoinQuery(idField, false, toField, new TermQuery(new Term("name", "name2")), indexSearcher, ScoreMode.None);

    TopDocs result = indexSearcher.search(joinQuery, 10);
    assertEquals(2, result.totalHits);
    assertEquals(4, result.scoreDocs[0].doc);
    assertEquals(5, result.scoreDocs[1].doc);

    joinQuery = JoinUtil.createJoinQuery(idField, false, toField, new TermQuery(new Term("name", "name1")), indexSearcher, ScoreMode.None);
    result = indexSearcher.search(joinQuery, 10);
    assertEquals(2, result.totalHits);
    assertEquals(1, result.scoreDocs[0].doc);
    assertEquals(2, result.scoreDocs[1].doc);

    // Search for offer
    joinQuery = JoinUtil.createJoinQuery(toField, false, idField, new TermQuery(new Term("id", "5")), indexSearcher, ScoreMode.None);
    result = indexSearcher.search(joinQuery, 10);
    assertEquals(1, result.totalHits);
    assertEquals(3, result.scoreDocs[0].doc);

    indexSearcher.getIndexReader().close();
    dir.close();
  }

  public void testSimpleOrdinalsJoin() throws Exception {
    final String idField = "id";
    final String productIdField = "productId";
    // A field indicating to what type a document belongs, which is then used to distinques between documents during joining.
    final String typeField = "type";
    // A single sorted doc values field that holds the join values for all document types.
    // Typically during indexing a schema will automatically create this field with the values
    final String joinField = idField + productIdField;

    Directory dir = newDirectory();
    RandomIndexWriter w = new RandomIndexWriter(
        random(),
        dir,
        newIndexWriterConfig(new MockAnalyzer(random())).setMergePolicy(NoMergePolicy.INSTANCE));

    // 0
    Document doc = new Document();
    doc.add(new TextField(idField, "1", Field.Store.NO));
    doc.add(new TextField(typeField, "product", Field.Store.NO));
    doc.add(new TextField("description", "random text", Field.Store.NO));
    doc.add(new TextField("name", "name1", Field.Store.NO));
    doc.add(new SortedDocValuesField(joinField, new BytesRef("1")));
    w.addDocument(doc);

    // 1
    doc = new Document();
    doc.add(new TextField(productIdField, "1", Field.Store.NO));
    doc.add(new TextField(typeField, "price", Field.Store.NO));
    doc.add(new TextField("price", "10.0", Field.Store.NO));
    doc.add(new SortedDocValuesField(joinField, new BytesRef("1")));
    w.addDocument(doc);

    // 2
    doc = new Document();
    doc.add(new TextField(productIdField, "1", Field.Store.NO));
    doc.add(new TextField(typeField, "price", Field.Store.NO));
    doc.add(new TextField("price", "20.0", Field.Store.NO));
    doc.add(new SortedDocValuesField(joinField, new BytesRef("1")));
    w.addDocument(doc);

    // 3
    doc = new Document();
    doc.add(new TextField(idField, "2", Field.Store.NO));
    doc.add(new TextField(typeField, "product", Field.Store.NO));
    doc.add(new TextField("description", "more random text", Field.Store.NO));
    doc.add(new TextField("name", "name2", Field.Store.NO));
    doc.add(new SortedDocValuesField(joinField, new BytesRef("2")));
    w.addDocument(doc);
    w.commit();

    // 4
    doc = new Document();
    doc.add(new TextField(productIdField, "2", Field.Store.NO));
    doc.add(new TextField(typeField, "price", Field.Store.NO));
    doc.add(new TextField("price", "10.0", Field.Store.NO));
    doc.add(new SortedDocValuesField(joinField, new BytesRef("2")));
    w.addDocument(doc);

    // 5
    doc = new Document();
    doc.add(new TextField(productIdField, "2", Field.Store.NO));
    doc.add(new TextField(typeField, "price", Field.Store.NO));
    doc.add(new TextField("price", "20.0", Field.Store.NO));
    doc.add(new SortedDocValuesField(joinField, new BytesRef("2")));
    w.addDocument(doc);

    IndexSearcher indexSearcher = new IndexSearcher(w.getReader());
    w.close();

    IndexReader r = indexSearcher.getIndexReader();
    SortedDocValues[] values = new SortedDocValues[r.leaves().size()];
    for (int i = 0; i < values.length; i++) {
      LeafReader leafReader =  r.leaves().get(i).reader();
      values[i] = DocValues.getSorted(leafReader, joinField);
    }
    MultiDocValues.OrdinalMap ordinalMap = MultiDocValues.OrdinalMap.build(
        r.getCoreCacheKey(), values, PackedInts.DEFAULT
    );

    Query toQuery = new TermQuery(new Term(typeField, "price"));
    Query fromQuery = new TermQuery(new Term("name", "name2"));
    // Search for product and return prices
    Query joinQuery = JoinUtil.createJoinQuery(joinField, fromQuery, toQuery, indexSearcher, ScoreMode.None, ordinalMap);
    TopDocs result = indexSearcher.search(joinQuery, 10);
    assertEquals(2, result.totalHits);
    assertEquals(4, result.scoreDocs[0].doc);
    assertEquals(5, result.scoreDocs[1].doc);

    fromQuery = new TermQuery(new Term("name", "name1"));
    joinQuery = JoinUtil.createJoinQuery(joinField, fromQuery, toQuery, indexSearcher, ScoreMode.None, ordinalMap);
    result = indexSearcher.search(joinQuery, 10);
    assertEquals(2, result.totalHits);
    assertEquals(1, result.scoreDocs[0].doc);
    assertEquals(2, result.scoreDocs[1].doc);

    // Search for prices and return products
    fromQuery = new TermQuery(new Term("price", "20.0"));
    toQuery = new TermQuery(new Term(typeField, "product"));
    joinQuery = JoinUtil.createJoinQuery(joinField, fromQuery, toQuery, indexSearcher, ScoreMode.None, ordinalMap);
    result = indexSearcher.search(joinQuery, 10);
    assertEquals(2, result.totalHits);
    assertEquals(0, result.scoreDocs[0].doc);
    assertEquals(3, result.scoreDocs[1].doc);

    indexSearcher.getIndexReader().close();
    dir.close();
  }

  public void testRandomOrdinalsJoin() throws Exception {
    IndexIterationContext context = createContext(512, false, true);
    int searchIters = 10;
    IndexSearcher indexSearcher = context.searcher;
    for (int i = 0; i < searchIters; i++) {
      if (VERBOSE) {
        System.out.println("search iter=" + i);
      }
      int r = random().nextInt(context.randomUniqueValues.length);
      boolean from = context.randomFrom[r];
      String randomValue = context.randomUniqueValues[r];
      BitSet expectedResult = createExpectedResult(randomValue, from, indexSearcher.getIndexReader(), context);

      final Query actualQuery = new TermQuery(new Term("value", randomValue));
      if (VERBOSE) {
        System.out.println("actualQuery=" + actualQuery);
      }
      final ScoreMode scoreMode = ScoreMode.values()[random().nextInt(ScoreMode.values().length)];
      if (VERBOSE) {
        System.out.println("scoreMode=" + scoreMode);
      }

      final Query joinQuery;
      if (from) {
        BooleanQuery fromQuery = new BooleanQuery();
        fromQuery.add(new TermQuery(new Term("type", "from")), BooleanClause.Occur.FILTER);
        fromQuery.add(actualQuery, BooleanClause.Occur.MUST);
        Query toQuery = new TermQuery(new Term("type", "to"));
        joinQuery = JoinUtil.createJoinQuery("join_field", fromQuery, toQuery, indexSearcher, scoreMode, context.ordinalMap);
      } else {
        BooleanQuery fromQuery = new BooleanQuery();
        fromQuery.add(new TermQuery(new Term("type", "to")), BooleanClause.Occur.FILTER);
        fromQuery.add(actualQuery, BooleanClause.Occur.MUST);
        Query toQuery = new TermQuery(new Term("type", "from"));
        joinQuery = JoinUtil.createJoinQuery("join_field", fromQuery, toQuery, indexSearcher, scoreMode, context.ordinalMap);
      }
      if (VERBOSE) {
        System.out.println("joinQuery=" + joinQuery);
      }

      final BitSet actualResult = new FixedBitSet(indexSearcher.getIndexReader().maxDoc());
      final TopScoreDocCollector topScoreDocCollector = TopScoreDocCollector.create(10);
      indexSearcher.search(joinQuery, MultiCollector.wrap(new BitSetCollector(actualResult), topScoreDocCollector));
      assertBitSet(expectedResult, actualResult, indexSearcher);
      TopDocs expectedTopDocs = createExpectedTopDocs(randomValue, from, scoreMode, context);
      TopDocs actualTopDocs = topScoreDocCollector.topDocs();
      assertTopDocs(expectedTopDocs, actualTopDocs, scoreMode, indexSearcher, joinQuery);
    }
    context.close();
  }

  // TermsWithScoreCollector.MV.Avg forgets to grow beyond TermsWithScoreCollector.INITIAL_ARRAY_SIZE
  public void testOverflowTermsWithScoreCollector() throws Exception {
    test300spartans(true, ScoreMode.Avg);
  }

  public void testOverflowTermsWithScoreCollectorRandom() throws Exception {
    test300spartans(random().nextBoolean(), ScoreMode.values()[random().nextInt(ScoreMode.values().length)]);
  }

  void test300spartans(boolean multipleValues, ScoreMode scoreMode) throws Exception {
    final String idField = "id";
    final String toField = "productId";

    Directory dir = newDirectory();
    RandomIndexWriter w = new RandomIndexWriter(
        random(),
        dir,
        newIndexWriterConfig(new MockAnalyzer(random())).setMergePolicy(newLogMergePolicy()));

    // 0
    Document doc = new Document();
    doc.add(new TextField("description", "random text", Field.Store.NO));
    doc.add(new TextField("name", "name1", Field.Store.NO));
    doc.add(new TextField(idField, "0", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("0")));
    w.addDocument(doc);

    doc = new Document();
    doc.add(new TextField("price", "10.0", Field.Store.NO));

    if (multipleValues) {
      for(int i=0;i<300;i++) {
        doc.add(new SortedSetDocValuesField(toField, new BytesRef(""+i)));
      }
    } else {
      doc.add(new SortedDocValuesField(toField, new BytesRef("0")));
    }
    w.addDocument(doc);

    IndexSearcher indexSearcher = new IndexSearcher(w.getReader());
    w.close();

    // Search for product
    Query joinQuery =
        JoinUtil.createJoinQuery(toField, multipleValues, idField, new TermQuery(new Term("price", "10.0")), indexSearcher, scoreMode);

    TopDocs result = indexSearcher.search(joinQuery, 10);
    assertEquals(1, result.totalHits);
    assertEquals(0, result.scoreDocs[0].doc);


    indexSearcher.getIndexReader().close();
    dir.close();
  }

  /** LUCENE-5487: verify a join query inside a SHOULD BQ
   *  will still use the join query's optimized BulkScorers */
  public void testInsideBooleanQuery() throws Exception {
    final String idField = "id";
    final String toField = "productId";

    Directory dir = newDirectory();
    RandomIndexWriter w = new RandomIndexWriter(
        random(),
        dir,
        newIndexWriterConfig(new MockAnalyzer(random())).setMergePolicy(newLogMergePolicy()));

    // 0
    Document doc = new Document();
    doc.add(new TextField("description", "random text", Field.Store.NO));
    doc.add(new TextField("name", "name1", Field.Store.NO));
    doc.add(new TextField(idField, "7", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("7")));
    w.addDocument(doc);

    // 1
    doc = new Document();
    doc.add(new TextField("price", "10.0", Field.Store.NO));
    doc.add(new TextField(idField, "2", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("2")));
    doc.add(new TextField(toField, "7", Field.Store.NO));
    w.addDocument(doc);

    // 2
    doc = new Document();
    doc.add(new TextField("price", "20.0", Field.Store.NO));
    doc.add(new TextField(idField, "3", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("3")));
    doc.add(new TextField(toField, "7", Field.Store.NO));
    w.addDocument(doc);

    // 3
    doc = new Document();
    doc.add(new TextField("description", "more random text", Field.Store.NO));
    doc.add(new TextField("name", "name2", Field.Store.NO));
    doc.add(new TextField(idField, "0", Field.Store.NO));
    w.addDocument(doc);
    w.commit();

    // 4
    doc = new Document();
    doc.add(new TextField("price", "10.0", Field.Store.NO));
    doc.add(new TextField(idField, "5", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("5")));
    doc.add(new TextField(toField, "0", Field.Store.NO));
    w.addDocument(doc);

    // 5
    doc = new Document();
    doc.add(new TextField("price", "20.0", Field.Store.NO));
    doc.add(new TextField(idField, "6", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("6")));
    doc.add(new TextField(toField, "0", Field.Store.NO));
    w.addDocument(doc);

    w.forceMerge(1);

    IndexSearcher indexSearcher = new IndexSearcher(w.getReader());
    w.close();

    // Search for product
    Query joinQuery =
        JoinUtil.createJoinQuery(idField, false, toField, new TermQuery(new Term("description", "random")), indexSearcher, ScoreMode.Avg);

    BooleanQuery bq = new BooleanQuery();
    bq.add(joinQuery, BooleanClause.Occur.SHOULD);
    bq.add(new TermQuery(new Term("id", "3")), BooleanClause.Occur.SHOULD);

    indexSearcher.search(bq, new SimpleCollector() {
        boolean sawFive;
        @Override
        public void collect(int docID) {
          // Hairy / evil (depends on how BooleanScorer
          // stores temporarily collected docIDs by
          // appending to head of linked list):
          if (docID == 5) {
            sawFive = true;
          } else if (docID == 1) {
            assertFalse("optimized bulkScorer was not used for join query embedded in boolean query!", sawFive);
          }
        }

        @Override
        public boolean needsScores() {
          return false;
        }
      });

    indexSearcher.getIndexReader().close();
    dir.close();
  }

  public void testSimpleWithScoring() throws Exception {
    final String idField = "id";
    final String toField = "movieId";

    Directory dir = newDirectory();
    RandomIndexWriter w = new RandomIndexWriter(
        random(),
        dir,
        newIndexWriterConfig(new MockAnalyzer(random())).setMergePolicy(newLogMergePolicy()));

    // 0
    Document doc = new Document();
    doc.add(new TextField("description", "A random movie", Field.Store.NO));
    doc.add(new TextField("name", "Movie 1", Field.Store.NO));
    doc.add(new TextField(idField, "1", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("1")));
    w.addDocument(doc);

    // 1
    doc = new Document();
    doc.add(new TextField("subtitle", "The first subtitle of this movie", Field.Store.NO));
    doc.add(new TextField(idField, "2", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("2")));
    doc.add(new TextField(toField, "1", Field.Store.NO));
    doc.add(new SortedDocValuesField(toField, new BytesRef("1")));
    w.addDocument(doc);

    // 2
    doc = new Document();
    doc.add(new TextField("subtitle", "random subtitle; random event movie", Field.Store.NO));
    doc.add(new TextField(idField, "3", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("3")));
    doc.add(new TextField(toField, "1", Field.Store.NO));
    doc.add(new SortedDocValuesField(toField, new BytesRef("1")));
    w.addDocument(doc);

    // 3
    doc = new Document();
    doc.add(new TextField("description", "A second random movie", Field.Store.NO));
    doc.add(new TextField("name", "Movie 2", Field.Store.NO));
    doc.add(new TextField(idField, "4", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("4")));
    w.addDocument(doc);
    w.commit();

    // 4
    doc = new Document();
    doc.add(new TextField("subtitle", "a very random event happened during christmas night", Field.Store.NO));
    doc.add(new TextField(idField, "5", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("5")));
    doc.add(new TextField(toField, "4", Field.Store.NO));
    doc.add(new SortedDocValuesField(toField, new BytesRef("4")));
    w.addDocument(doc);

    // 5
    doc = new Document();
    doc.add(new TextField("subtitle", "movie end movie test 123 test 123 random", Field.Store.NO));
    doc.add(new TextField(idField, "6", Field.Store.NO));
    doc.add(new SortedDocValuesField(idField, new BytesRef("6")));
    doc.add(new TextField(toField, "4", Field.Store.NO));
    doc.add(new SortedDocValuesField(toField, new BytesRef("4")));
    w.addDocument(doc);

    IndexSearcher indexSearcher = new IndexSearcher(w.getReader());
    w.close();

    // Search for movie via subtitle
    Query joinQuery =
        JoinUtil.createJoinQuery(toField, false, idField, new TermQuery(new Term("subtitle", "random")), indexSearcher, ScoreMode.Max);
    TopDocs result = indexSearcher.search(joinQuery, 10);
    assertEquals(2, result.totalHits);
    assertEquals(0, result.scoreDocs[0].doc);
    assertEquals(3, result.scoreDocs[1].doc);

    // Score mode max.
    joinQuery = JoinUtil.createJoinQuery(toField, false, idField, new TermQuery(new Term("subtitle", "movie")), indexSearcher, ScoreMode.Max);
    result = indexSearcher.search(joinQuery, 10);
    assertEquals(2, result.totalHits);
    assertEquals(3, result.scoreDocs[0].doc);
    assertEquals(0, result.scoreDocs[1].doc);

    // Score mode total
    joinQuery = JoinUtil.createJoinQuery(toField, false, idField, new TermQuery(new Term("subtitle", "movie")), indexSearcher, ScoreMode.Total);
    result = indexSearcher.search(joinQuery, 10);
    assertEquals(2, result.totalHits);
    assertEquals(0, result.scoreDocs[0].doc);
    assertEquals(3, result.scoreDocs[1].doc);

    //Score mode avg
    joinQuery = JoinUtil.createJoinQuery(toField, false, idField, new TermQuery(new Term("subtitle", "movie")), indexSearcher, ScoreMode.Avg);
    result = indexSearcher.search(joinQuery, 10);
    assertEquals(2, result.totalHits);
    assertEquals(3, result.scoreDocs[0].doc);
    assertEquals(0, result.scoreDocs[1].doc);

    indexSearcher.getIndexReader().close();
    dir.close();
  }

  @Test
  @Slow
  public void testSingleValueRandomJoin() throws Exception {
    int maxIndexIter = TestUtil.nextInt(random(), 6, 12);
    int maxSearchIter = TestUtil.nextInt(random(), 13, 26);
    executeRandomJoin(false, maxIndexIter, maxSearchIter, TestUtil.nextInt(random(), 87, 764));
  }

  @Test
  @Slow
  // This test really takes more time, that is why the number of iterations are smaller.
  public void testMultiValueRandomJoin() throws Exception {
    int maxIndexIter = TestUtil.nextInt(random(), 3, 6);
    int maxSearchIter = TestUtil.nextInt(random(), 6, 12);
    executeRandomJoin(true, maxIndexIter, maxSearchIter, TestUtil.nextInt(random(), 11, 57));
  }

  private void executeRandomJoin(boolean multipleValuesPerDocument, int maxIndexIter, int maxSearchIter, int numberOfDocumentsToIndex) throws Exception {
    for (int indexIter = 1; indexIter <= maxIndexIter; indexIter++) {
      if (VERBOSE) {
        System.out.println("indexIter=" + indexIter);
      }
      IndexIterationContext context = createContext(numberOfDocumentsToIndex, multipleValuesPerDocument, false);
      IndexSearcher indexSearcher = context.searcher;
      for (int searchIter = 1; searchIter <= maxSearchIter; searchIter++) {
        if (VERBOSE) {
          System.out.println("searchIter=" + searchIter);
        }

        int r = random().nextInt(context.randomUniqueValues.length);
        boolean from = context.randomFrom[r];
        String randomValue = context.randomUniqueValues[r];
        BitSet expectedResult = createExpectedResult(randomValue, from, indexSearcher.getIndexReader(), context);

        final Query actualQuery = new TermQuery(new Term("value", randomValue));
        if (VERBOSE) {
          System.out.println("actualQuery=" + actualQuery);
        }
        final ScoreMode scoreMode = ScoreMode.values()[random().nextInt(ScoreMode.values().length)];
        if (VERBOSE) {
          System.out.println("scoreMode=" + scoreMode);
        }

        final Query joinQuery;
        if (from) {
          joinQuery = JoinUtil.createJoinQuery("from", multipleValuesPerDocument, "to", actualQuery, indexSearcher, scoreMode);
        } else {
          joinQuery = JoinUtil.createJoinQuery("to", multipleValuesPerDocument, "from", actualQuery, indexSearcher, scoreMode);
        }
        if (VERBOSE) {
          System.out.println("joinQuery=" + joinQuery);
        }

        // Need to know all documents that have matches. TopDocs doesn't give me that and then I'd be also testing TopDocsCollector...
        final BitSet actualResult = new FixedBitSet(indexSearcher.getIndexReader().maxDoc());
        final TopScoreDocCollector topScoreDocCollector = TopScoreDocCollector.create(10);
        indexSearcher.search(joinQuery, MultiCollector.wrap(new BitSetCollector(actualResult), topScoreDocCollector));
        // Asserting bit set...
        assertBitSet(expectedResult, actualResult, indexSearcher);
        // Asserting TopDocs...
        TopDocs expectedTopDocs = createExpectedTopDocs(randomValue, from, scoreMode, context);
        TopDocs actualTopDocs = topScoreDocCollector.topDocs();
        assertTopDocs(expectedTopDocs, actualTopDocs, scoreMode, indexSearcher, joinQuery);
      }
      context.close();
    }
  }

  private void assertBitSet(BitSet expectedResult, BitSet actualResult, IndexSearcher indexSearcher) throws IOException {
    if (VERBOSE) {
      System.out.println("expected cardinality:" + expectedResult.cardinality());
      DocIdSetIterator iterator = new BitSetIterator(expectedResult, expectedResult.cardinality());
      for (int doc = iterator.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = iterator.nextDoc()) {
        System.out.println(String.format(Locale.ROOT, "Expected doc[%d] with id value %s", doc, indexSearcher.doc(doc).get("id")));
      }
      System.out.println("actual cardinality:" + actualResult.cardinality());
      iterator = new BitSetIterator(actualResult, actualResult.cardinality());
      for (int doc = iterator.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = iterator.nextDoc()) {
        System.out.println(String.format(Locale.ROOT, "Actual doc[%d] with id value %s", doc, indexSearcher.doc(doc).get("id")));
      }
    }
    assertEquals(expectedResult, actualResult);
  }

  private void assertTopDocs(TopDocs expectedTopDocs, TopDocs actualTopDocs, ScoreMode scoreMode, IndexSearcher indexSearcher, Query joinQuery) throws IOException {
    assertEquals(expectedTopDocs.totalHits, actualTopDocs.totalHits);
    assertEquals(expectedTopDocs.scoreDocs.length, actualTopDocs.scoreDocs.length);
    if (scoreMode == ScoreMode.None) {
      return;
    }

    assertEquals(expectedTopDocs.getMaxScore(), actualTopDocs.getMaxScore(), 0.0f);
    if (VERBOSE) {
      for (int i = 0; i < expectedTopDocs.scoreDocs.length; i++) {
        System.out.printf(Locale.ENGLISH, "Expected doc: %d | Actual doc: %d\n", expectedTopDocs.scoreDocs[i].doc, actualTopDocs.scoreDocs[i].doc);
        System.out.printf(Locale.ENGLISH, "Expected score: %f | Actual score: %f\n", expectedTopDocs.scoreDocs[i].score, actualTopDocs.scoreDocs[i].score);
      }
    }

    for (int i = 0; i < expectedTopDocs.scoreDocs.length; i++) {
      assertEquals(expectedTopDocs.scoreDocs[i].doc, actualTopDocs.scoreDocs[i].doc);
      assertEquals(expectedTopDocs.scoreDocs[i].score, actualTopDocs.scoreDocs[i].score, 0.0f);
      Explanation explanation = indexSearcher.explain(joinQuery, expectedTopDocs.scoreDocs[i].doc);
      assertEquals(expectedTopDocs.scoreDocs[i].score, explanation.getValue(), 0.0f);
    }
  }

  private IndexIterationContext createContext(int nDocs, boolean multipleValuesPerDocument, boolean globalOrdinalJoin) throws IOException {
    if (globalOrdinalJoin) {
      assertFalse("ordinal join doesn't support multiple join values per document", multipleValuesPerDocument);
    }

    Directory dir = newDirectory();
    RandomIndexWriter w = new RandomIndexWriter(
        random(),
        dir,
        newIndexWriterConfig(new MockAnalyzer(random(), MockTokenizer.KEYWORD, false))
    );

    IndexIterationContext context = new IndexIterationContext();
    int numRandomValues = nDocs / RandomInts.randomIntBetween(random(), 2, 10);
    context.randomUniqueValues = new String[numRandomValues];
    Set<String> trackSet = new HashSet<>();
    context.randomFrom = new boolean[numRandomValues];
    for (int i = 0; i < numRandomValues; i++) {
      String uniqueRandomValue;
      do {
//        uniqueRandomValue = TestUtil.randomRealisticUnicodeString(random());
        uniqueRandomValue = TestUtil.randomSimpleString(random());
      } while ("".equals(uniqueRandomValue) || trackSet.contains(uniqueRandomValue));
      // Generate unique values and empty strings aren't allowed.
      trackSet.add(uniqueRandomValue);
      context.randomFrom[i] = random().nextBoolean();
      context.randomUniqueValues[i] = uniqueRandomValue;
    }

    RandomDoc[] docs = new RandomDoc[nDocs];
    for (int i = 0; i < nDocs; i++) {
      String id = Integer.toString(i);
      int randomI = random().nextInt(context.randomUniqueValues.length);
      String value = context.randomUniqueValues[randomI];
      Document document = new Document();
      document.add(newTextField(random(), "id", id, Field.Store.YES));
      document.add(newTextField(random(), "value", value, Field.Store.NO));

      boolean from = context.randomFrom[randomI];
      int numberOfLinkValues = multipleValuesPerDocument ? 2 + random().nextInt(10) : 1;
      docs[i] = new RandomDoc(id, numberOfLinkValues, value, from);
      if (globalOrdinalJoin) {
        document.add(newStringField("type", from ? "from" : "to", Field.Store.NO));
      }
      for (int j = 0; j < numberOfLinkValues; j++) {
        String linkValue = context.randomUniqueValues[random().nextInt(context.randomUniqueValues.length)];
        docs[i].linkValues.add(linkValue);
        if (from) {
          if (!context.fromDocuments.containsKey(linkValue)) {
            context.fromDocuments.put(linkValue, new ArrayList<>());
          }
          if (!context.randomValueFromDocs.containsKey(value)) {
            context.randomValueFromDocs.put(value, new ArrayList<>());
          }

          context.fromDocuments.get(linkValue).add(docs[i]);
          context.randomValueFromDocs.get(value).add(docs[i]);
          document.add(newTextField(random(), "from", linkValue, Field.Store.NO));
          if (multipleValuesPerDocument) {
            document.add(new SortedSetDocValuesField("from", new BytesRef(linkValue)));
          } else {
            document.add(new SortedDocValuesField("from", new BytesRef(linkValue)));
          }
          if (globalOrdinalJoin) {
            document.add(new SortedDocValuesField("join_field", new BytesRef(linkValue)));
          }
        } else {
          if (!context.toDocuments.containsKey(linkValue)) {
            context.toDocuments.put(linkValue, new ArrayList<>());
          }
          if (!context.randomValueToDocs.containsKey(value)) {
            context.randomValueToDocs.put(value, new ArrayList<>());
          }

          context.toDocuments.get(linkValue).add(docs[i]);
          context.randomValueToDocs.get(value).add(docs[i]);
          document.add(newTextField(random(), "to", linkValue, Field.Store.NO));
          if (multipleValuesPerDocument) {
            document.add(new SortedSetDocValuesField("to", new BytesRef(linkValue)));
          } else {
            document.add(new SortedDocValuesField("to", new BytesRef(linkValue)));
          }
          if (globalOrdinalJoin) {
            document.add(new SortedDocValuesField("join_field", new BytesRef(linkValue)));
          }
        }
      }

      w.addDocument(document);
      if (random().nextInt(10) == 4) {
        w.commit();
      }
      if (VERBOSE) {
        System.out.println("Added document[" + docs[i].id + "]: " + document);
      }
    }

    if (random().nextBoolean()) {
      w.forceMerge(1);
    }
    w.close();

    // Pre-compute all possible hits for all unique random values. On top of this also compute all possible score for
    // any ScoreMode.
    DirectoryReader topLevelReader = DirectoryReader.open(dir);
    IndexSearcher searcher = newSearcher(topLevelReader);
    for (int i = 0; i < context.randomUniqueValues.length; i++) {
      String uniqueRandomValue = context.randomUniqueValues[i];
      final String fromField;
      final String toField;
      final Map<String, Map<Integer, JoinScore>> queryVals;
      if (context.randomFrom[i]) {
        fromField = "from";
        toField = "to";
        queryVals = context.fromHitsToJoinScore;
      } else {
        fromField = "to";
        toField = "from";
        queryVals = context.toHitsToJoinScore;
      }
      final Map<BytesRef, JoinScore> joinValueToJoinScores = new HashMap<>();
      if (multipleValuesPerDocument) {
        searcher.search(new TermQuery(new Term("value", uniqueRandomValue)), new SimpleCollector() {

          private Scorer scorer;
          private SortedSetDocValues docTermOrds;

          @Override
          public void collect(int doc) throws IOException {
            docTermOrds.setDocument(doc);
            long ord;
            while ((ord = docTermOrds.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS) {
              final BytesRef joinValue = docTermOrds.lookupOrd(ord);
              JoinScore joinScore = joinValueToJoinScores.get(joinValue);
              if (joinScore == null) {
                joinValueToJoinScores.put(BytesRef.deepCopyOf(joinValue), joinScore = new JoinScore());
              }
              joinScore.addScore(scorer.score());
            }
          }

          @Override
          protected void doSetNextReader(LeafReaderContext context) throws IOException {
            docTermOrds = DocValues.getSortedSet(context.reader(), fromField);
          }

          @Override
          public void setScorer(Scorer scorer) {
            this.scorer = scorer;
          }

          @Override
          public boolean needsScores() {
            return true;
          }
        });
      } else {
        searcher.search(new TermQuery(new Term("value", uniqueRandomValue)), new SimpleCollector() {

          private Scorer scorer;
          private BinaryDocValues terms;
          private Bits docsWithField;

          @Override
          public void collect(int doc) throws IOException {
            final BytesRef joinValue = terms.get(doc);
            if (joinValue.length == 0 && !docsWithField.get(doc)) {
              return;
            }

            JoinScore joinScore = joinValueToJoinScores.get(joinValue);
            if (joinScore == null) {
              joinValueToJoinScores.put(BytesRef.deepCopyOf(joinValue), joinScore = new JoinScore());
            }
            if (VERBOSE) {
              System.out.println("expected val=" + joinValue.utf8ToString() + " expected score=" + scorer.score());
            }
            joinScore.addScore(scorer.score());
          }

          @Override
          protected void doSetNextReader(LeafReaderContext context) throws IOException {
            terms = DocValues.getBinary(context.reader(), fromField);
            docsWithField = DocValues.getDocsWithField(context.reader(), fromField);
          }

          @Override
          public void setScorer(Scorer scorer) {
            this.scorer = scorer;
          }

          @Override
          public boolean needsScores() {
            return true;
          }
        });
      }

      final Map<Integer, JoinScore> docToJoinScore = new HashMap<>();
      if (multipleValuesPerDocument) {
        LeafReader slowCompositeReader = SlowCompositeReaderWrapper.wrap(topLevelReader);
        Terms terms = slowCompositeReader.terms(toField);
        if (terms != null) {
          PostingsEnum postingsEnum = null;
          TermsEnum termsEnum = null;
          SortedSet<BytesRef> joinValues = new TreeSet<>(BytesRef.getUTF8SortedAsUnicodeComparator());
          joinValues.addAll(joinValueToJoinScores.keySet());
          for (BytesRef joinValue : joinValues) {
            termsEnum = terms.iterator(termsEnum);
            if (termsEnum.seekExact(joinValue)) {
              postingsEnum = termsEnum.postings(slowCompositeReader.getLiveDocs(), postingsEnum, PostingsEnum.NONE);
              JoinScore joinScore = joinValueToJoinScores.get(joinValue);

              for (int doc = postingsEnum.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = postingsEnum.nextDoc()) {
                // First encountered join value determines the score.
                // Something to keep in mind for many-to-many relations.
                if (!docToJoinScore.containsKey(doc)) {
                  docToJoinScore.put(doc, joinScore);
                }
              }
            }
          }
        }
      } else {
        searcher.search(new MatchAllDocsQuery(), new SimpleCollector() {

          private BinaryDocValues terms;
          private int docBase;

          @Override
          public void collect(int doc) {
            final BytesRef joinValue = terms.get(doc);
            JoinScore joinScore = joinValueToJoinScores.get(joinValue);
            if (joinScore == null) {
              return;
            }
            docToJoinScore.put(docBase + doc, joinScore);
          }

          @Override
          protected void doSetNextReader(LeafReaderContext context) throws IOException {
            terms = DocValues.getBinary(context.reader(), toField);
            docBase = context.docBase;
          }

          @Override
          public void setScorer(Scorer scorer) {
          }

          @Override
          public boolean needsScores() {
            return false;
          }
        });
      }
      queryVals.put(uniqueRandomValue, docToJoinScore);
    }

    if (globalOrdinalJoin) {
      SortedDocValues[] values = new SortedDocValues[topLevelReader.leaves().size()];
      for (LeafReaderContext leadContext : topLevelReader.leaves()) {
        values[leadContext.ord] = DocValues.getSorted(leadContext.reader(), "join_field");
      }
      context.ordinalMap = MultiDocValues.OrdinalMap.build(
          topLevelReader.getCoreCacheKey(), values, PackedInts.DEFAULT
      );
    }

    context.searcher = searcher;
    context.dir = dir;
    return context;
  }

  private TopDocs createExpectedTopDocs(String queryValue,
                                        final boolean from,
                                        final ScoreMode scoreMode,
                                        IndexIterationContext context) {

    Map<Integer, JoinScore> hitsToJoinScores;
    if (from) {
      hitsToJoinScores = context.fromHitsToJoinScore.get(queryValue);
    } else {
      hitsToJoinScores = context.toHitsToJoinScore.get(queryValue);
    }
    List<Map.Entry<Integer,JoinScore>> hits = new ArrayList<>(hitsToJoinScores.entrySet());
    Collections.sort(hits, new Comparator<Map.Entry<Integer, JoinScore>>() {

      @Override
      public int compare(Map.Entry<Integer, JoinScore> hit1, Map.Entry<Integer, JoinScore> hit2) {
        float score1 = hit1.getValue().score(scoreMode);
        float score2 = hit2.getValue().score(scoreMode);

        int cmp = Float.compare(score2, score1);
        if (cmp != 0) {
          return cmp;
        }
        return hit1.getKey() - hit2.getKey();
      }

    });
    ScoreDoc[] scoreDocs = new ScoreDoc[Math.min(10, hits.size())];
    for (int i = 0; i < scoreDocs.length; i++) {
      Map.Entry<Integer,JoinScore> hit = hits.get(i);
      scoreDocs[i] = new ScoreDoc(hit.getKey(), hit.getValue().score(scoreMode));
    }
    return new TopDocs(hits.size(), scoreDocs, hits.isEmpty() ? Float.NaN : hits.get(0).getValue().score(scoreMode));
  }

  private BitSet createExpectedResult(String queryValue, boolean from, IndexReader topLevelReader, IndexIterationContext context) throws IOException {
    final Map<String, List<RandomDoc>> randomValueDocs;
    final Map<String, List<RandomDoc>> linkValueDocuments;
    if (from) {
      randomValueDocs = context.randomValueFromDocs;
      linkValueDocuments = context.toDocuments;
    } else {
      randomValueDocs = context.randomValueToDocs;
      linkValueDocuments = context.fromDocuments;
    }

    BitSet expectedResult = new FixedBitSet(topLevelReader.maxDoc());
    List<RandomDoc> matchingDocs = randomValueDocs.get(queryValue);
    if (matchingDocs == null) {
      return new FixedBitSet(topLevelReader.maxDoc());
    }

    for (RandomDoc matchingDoc : matchingDocs) {
      for (String linkValue : matchingDoc.linkValues) {
        List<RandomDoc> otherMatchingDocs = linkValueDocuments.get(linkValue);
        if (otherMatchingDocs == null) {
          continue;
        }

        for (RandomDoc otherSideDoc : otherMatchingDocs) {
          PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(topLevelReader, MultiFields.getLiveDocs(topLevelReader), "id", new BytesRef(otherSideDoc.id), 0);
          assert postingsEnum != null;
          int doc = postingsEnum.nextDoc();
          expectedResult.set(doc);
        }
      }
    }
    return expectedResult;
  }

  private static class IndexIterationContext {

    String[] randomUniqueValues;
    boolean[] randomFrom;
    Map<String, List<RandomDoc>> fromDocuments = new HashMap<>();
    Map<String, List<RandomDoc>> toDocuments = new HashMap<>();
    Map<String, List<RandomDoc>> randomValueFromDocs = new HashMap<>();
    Map<String, List<RandomDoc>> randomValueToDocs = new HashMap<>();

    Map<String, Map<Integer, JoinScore>> fromHitsToJoinScore = new HashMap<>();
    Map<String, Map<Integer, JoinScore>> toHitsToJoinScore = new HashMap<>();

    MultiDocValues.OrdinalMap ordinalMap;

    Directory dir;
    IndexSearcher searcher;

    void close() throws IOException {
      searcher.getIndexReader().close();
      dir.close();
    }

  }

  private static class RandomDoc {

    final String id;
    final List<String> linkValues;
    final String value;
    final boolean from;

    private RandomDoc(String id, int numberOfLinkValues, String value, boolean from) {
      this.id = id;
      this.from = from;
      linkValues = new ArrayList<>(numberOfLinkValues);
      this.value = value;
    }
  }

  private static class JoinScore {

    float maxScore;
    float total;
    int count;

    void addScore(float score) {
      total += score;
      if (score > maxScore) {
        maxScore = score;
      }
      count++;
    }

    float score(ScoreMode mode) {
      switch (mode) {
        case None:
          return 1.0f;
        case Total:
          return total;
        case Avg:
          return total / count;
        case Max:
          return maxScore;
      }
      throw new IllegalArgumentException("Unsupported ScoreMode: " + mode);
    }

  }

  private static class BitSetCollector extends SimpleCollector {

    private final BitSet bitSet;
    private int docBase;

    private BitSetCollector(BitSet bitSet) {
      this.bitSet = bitSet;
    }

    @Override
    public void collect(int doc) throws IOException {
      bitSet.set(docBase + doc);
    }

    @Override
    protected void doSetNextReader(LeafReaderContext context) throws IOException {
      docBase = context.docBase;
    }

    @Override
    public boolean needsScores() {
      return false;
    }
  }

}
