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

import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.index.AtomicReader.AtomicReaderContext;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util._TestUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

public class TestJoinUtil extends LuceneTestCase {

  public void testSimple() throws Exception {
    final String idField = "id";
    final String toField = "productId";

    Directory dir = newDirectory();
    RandomIndexWriter w = new RandomIndexWriter(
        random,
        dir,
        newIndexWriterConfig(TEST_VERSION_CURRENT,
            new MockAnalyzer(random)).setMergePolicy(newLogMergePolicy()));

    // 0
    Document doc = new Document();
    doc.add(new Field("description", "random text", TextField.TYPE_STORED));
    doc.add(new Field("name", "name1", TextField.TYPE_STORED));
    doc.add(new Field(idField, "1", TextField.TYPE_STORED));
    w.addDocument(doc);

    // 1
    doc = new Document();
    doc.add(new Field("price", "10.0", TextField.TYPE_STORED));
    doc.add(new Field(idField, "2", TextField.TYPE_STORED));
    doc.add(new Field(toField, "1", TextField.TYPE_STORED));
    w.addDocument(doc);

    // 2
    doc = new Document();
    doc.add(new Field("price", "20.0", TextField.TYPE_STORED));
    doc.add(new Field(idField, "3", TextField.TYPE_STORED));
    doc.add(new Field(toField, "1", TextField.TYPE_STORED));
    w.addDocument(doc);

    // 3
    doc = new Document();
    doc.add(new Field("description", "more random text", TextField.TYPE_STORED));
    doc.add(new Field("name", "name2", TextField.TYPE_STORED));
    doc.add(new Field(idField, "4", TextField.TYPE_STORED));
    w.addDocument(doc);
    w.commit();

    // 4
    doc = new Document();
    doc.add(new Field("price", "10.0", TextField.TYPE_STORED));
    doc.add(new Field(idField, "5", TextField.TYPE_STORED));
    doc.add(new Field(toField, "4", TextField.TYPE_STORED));
    w.addDocument(doc);

    // 5
    doc = new Document();
    doc.add(new Field("price", "20.0", TextField.TYPE_STORED));
    doc.add(new Field(idField, "6", TextField.TYPE_STORED));
    doc.add(new Field(toField, "4", TextField.TYPE_STORED));
    w.addDocument(doc);

    IndexSearcher indexSearcher = new IndexSearcher(w.getReader());
    w.close();

    // Search for product
    Query joinQuery =
        JoinUtil.createJoinQuery(idField, false, toField, new TermQuery(new Term("name", "name2")), indexSearcher);

    TopDocs result = indexSearcher.search(joinQuery, 10);
    assertEquals(2, result.totalHits);
    assertEquals(4, result.scoreDocs[0].doc);
    assertEquals(5, result.scoreDocs[1].doc);

    joinQuery = JoinUtil.createJoinQuery(idField, false, toField, new TermQuery(new Term("name", "name1")), indexSearcher);
    result = indexSearcher.search(joinQuery, 10);
    assertEquals(2, result.totalHits);
    assertEquals(1, result.scoreDocs[0].doc);
    assertEquals(2, result.scoreDocs[1].doc);

    // Search for offer
    joinQuery = JoinUtil.createJoinQuery(toField, false, idField, new TermQuery(new Term("id", "5")), indexSearcher);
    result = indexSearcher.search(joinQuery, 10);
    assertEquals(1, result.totalHits);
    assertEquals(3, result.scoreDocs[0].doc);

    indexSearcher.getIndexReader().close();
    dir.close();
  }

  @Test
  public void testSingleValueRandomJoin() throws Exception {
    int maxIndexIter = _TestUtil.nextInt(random, 6, 12);
    int maxSearchIter = _TestUtil.nextInt(random, 13, 26);
    executeRandomJoin(false, maxIndexIter, maxSearchIter);
  }

  @Test
  // This test really takes more time, that is why the number of iterations are smaller.
  public void testMultiValueRandomJoin() throws Exception {
    int maxIndexIter = _TestUtil.nextInt(random, 3, 6);
    int maxSearchIter = _TestUtil.nextInt(random, 6, 12);
    executeRandomJoin(true, maxIndexIter, maxSearchIter);
  }

  private void executeRandomJoin(boolean multipleValuesPerDocument, int maxIndexIter, int maxSearchIter) throws Exception {
    for (int indexIter = 1; indexIter <= maxIndexIter; indexIter++) {
      if (VERBOSE) {
        System.out.println("indexIter=" + indexIter);
      }
      Directory dir = newDirectory();
      RandomIndexWriter w = new RandomIndexWriter(
          random,
          dir,
          newIndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random, MockTokenizer.KEYWORD, false)).setMergePolicy(newLogMergePolicy())
      );
      int numberOfDocumentsToIndex = _TestUtil.nextInt(random, 87, 764);
      IndexIterationContext context = createContext(numberOfDocumentsToIndex, w, multipleValuesPerDocument);

      IndexReader topLevelReader = w.getReader();
      w.close();
      for (int searchIter = 1; searchIter <= maxSearchIter; searchIter++) {
        if (VERBOSE) {
          System.out.println("searchIter=" + searchIter);
        }
        IndexSearcher indexSearcher = newSearcher(topLevelReader);

        int r = random.nextInt(context.randomUniqueValues.length);
        boolean from = context.randomFrom[r];
        String randomValue = context.randomUniqueValues[r];
        FixedBitSet expectedResult = createExpectedResult(randomValue, from, indexSearcher.getIndexReader(), context);

        Query actualQuery = new TermQuery(new Term("value", randomValue));
        if (VERBOSE) {
          System.out.println("actualQuery=" + actualQuery);
        }
        Query joinQuery;
        if (from) {
          joinQuery = JoinUtil.createJoinQuery("from", multipleValuesPerDocument, "to", actualQuery, indexSearcher);
        } else {
          joinQuery = JoinUtil.createJoinQuery("to", multipleValuesPerDocument, "from", actualQuery, indexSearcher);
        }
        if (VERBOSE) {
          System.out.println("joinQuery=" + joinQuery);
        }

        // Need to know all documents that have matches. TopDocs doesn't give me that and then I'd be also testing TopDocsCollector...
        final FixedBitSet actualResult = new FixedBitSet(indexSearcher.getIndexReader().maxDoc());
        indexSearcher.search(joinQuery, new Collector() {

          int docBase;

          public void collect(int doc) throws IOException {
            actualResult.set(doc + docBase);
          }

          public void setNextReader(AtomicReaderContext context) throws IOException {
            docBase = context.docBase;
          }

          public void setScorer(Scorer scorer) throws IOException {
          }

          public boolean acceptsDocsOutOfOrder() {
            return true;
          }
        });

        if (VERBOSE) {
          System.out.println("expected cardinality:" + expectedResult.cardinality());
          DocIdSetIterator iterator = expectedResult.iterator();
          for (int doc = iterator.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = iterator.nextDoc()) {
            System.out.println(String.format("Expected doc[%d] with id value %s", doc, indexSearcher.doc(doc).get("id")));
          }
          System.out.println("actual cardinality:" + actualResult.cardinality());
          iterator = actualResult.iterator();
          for (int doc = iterator.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = iterator.nextDoc()) {
            System.out.println(String.format("Actual doc[%d] with id value %s", doc, indexSearcher.doc(doc).get("id")));
          }
        }

        assertEquals(expectedResult, actualResult);
      }
      topLevelReader.close();
      dir.close();
    }
  }

  private IndexIterationContext createContext(int nDocs, RandomIndexWriter writer, boolean multipleValuesPerDocument) throws IOException {
    return createContext(nDocs, writer, writer, multipleValuesPerDocument);
  }

  private IndexIterationContext createContext(int nDocs, RandomIndexWriter fromWriter, RandomIndexWriter toWriter, boolean multipleValuesPerDocument) throws IOException {
    IndexIterationContext context = new IndexIterationContext();
    int numRandomValues = nDocs / 2;
    context.randomUniqueValues = new String[numRandomValues];
    Set<String> trackSet = new HashSet<String>();
    context.randomFrom = new boolean[numRandomValues];
    for (int i = 0; i < numRandomValues; i++) {
      String uniqueRandomValue;
      do {
        uniqueRandomValue = _TestUtil.randomRealisticUnicodeString(random);
//        uniqueRandomValue = _TestUtil.randomSimpleString(random);
      } while ("".equals(uniqueRandomValue) || trackSet.contains(uniqueRandomValue));
      // Generate unique values and empty strings aren't allowed.
      trackSet.add(uniqueRandomValue);
      context.randomFrom[i] = random.nextBoolean();
      context.randomUniqueValues[i] = uniqueRandomValue;
    }

    for (int i = 0; i < nDocs; i++) {
      String id = Integer.toString(i);
      int randomI = random.nextInt(context.randomUniqueValues.length);
      String value = context.randomUniqueValues[randomI];
      Document document = new Document();
      document.add(newField(random, "id", id, TextField.TYPE_STORED));
      document.add(newField(random, "value", value, TextField.TYPE_STORED));

      boolean from = context.randomFrom[randomI];
      int numberOfLinkValues = multipleValuesPerDocument ? 2 + random.nextInt(10) : 1;
      RandomDoc doc = new RandomDoc(id, numberOfLinkValues, value);
      for (int j = 0; j < numberOfLinkValues; j++) {
        String linkValue = context.randomUniqueValues[random.nextInt(context.randomUniqueValues.length)];
        doc.linkValues.add(linkValue);
        if (from) {
          if (!context.fromDocuments.containsKey(linkValue)) {
            context.fromDocuments.put(linkValue, new ArrayList<RandomDoc>());
          }
          if (!context.randomValueFromDocs.containsKey(value)) {
            context.randomValueFromDocs.put(value, new ArrayList<RandomDoc>());
          }

          context.fromDocuments.get(linkValue).add(doc);
          context.randomValueFromDocs.get(value).add(doc);
          document.add(newField(random, "from", linkValue, TextField.TYPE_STORED));
        } else {
          if (!context.toDocuments.containsKey(linkValue)) {
            context.toDocuments.put(linkValue, new ArrayList<RandomDoc>());
          }
          if (!context.randomValueToDocs.containsKey(value)) {
            context.randomValueToDocs.put(value, new ArrayList<RandomDoc>());
          }

          context.toDocuments.get(linkValue).add(doc);
          context.randomValueToDocs.get(value).add(doc);
          document.add(newField(random, "to", linkValue, TextField.TYPE_STORED));
        }
      }

      final RandomIndexWriter w;
      if (from) {
        w = fromWriter;
      } else {
        w = toWriter;
      }

      w.addDocument(document);
      if (random.nextInt(10) == 4) {
        w.commit();
      }
      if (VERBOSE) {
        System.out.println("Added document[" + i + "]: " + document);
      }
    }
    return context;
  }

  private FixedBitSet createExpectedResult(String queryValue, boolean from, IndexReader topLevelReader, IndexIterationContext context) throws IOException {
    final Map<String, List<RandomDoc>> randomValueDocs;
    final Map<String, List<RandomDoc>> linkValueDocuments;
    if (from) {
      randomValueDocs = context.randomValueFromDocs;
      linkValueDocuments = context.toDocuments;
    } else {
      randomValueDocs = context.randomValueToDocs;
      linkValueDocuments = context.fromDocuments;
    }

    FixedBitSet expectedResult = new FixedBitSet(topLevelReader.maxDoc());
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
          DocsEnum docsEnum = MultiFields.getTermDocsEnum(topLevelReader, MultiFields.getLiveDocs(topLevelReader), "id", new BytesRef(otherSideDoc.id), false);
          assert docsEnum != null;
          int doc = docsEnum.nextDoc();
          expectedResult.set(doc);
        }
      }
    }
    return expectedResult;
  }

  private static class IndexIterationContext {

    String[] randomUniqueValues;
    boolean[] randomFrom;
    Map<String, List<RandomDoc>> fromDocuments = new HashMap<String, List<RandomDoc>>();
    Map<String, List<RandomDoc>> toDocuments = new HashMap<String, List<RandomDoc>>();
    Map<String, List<RandomDoc>> randomValueFromDocs = new HashMap<String, List<RandomDoc>>();
    Map<String, List<RandomDoc>> randomValueToDocs = new HashMap<String, List<RandomDoc>>();

  }

  private static class RandomDoc {

    final String id;
    final List<String> linkValues;
    final String value;

    private RandomDoc(String id, int numberOfLinkValues, String value) {
      this.id = id;
      linkValues = new ArrayList<String>(numberOfLinkValues);
      this.value = value;
    }
  }

}
