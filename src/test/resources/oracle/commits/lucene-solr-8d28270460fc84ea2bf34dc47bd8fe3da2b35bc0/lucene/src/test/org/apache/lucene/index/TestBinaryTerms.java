package org.apache.lucene.index;

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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.codecs.CodecProvider;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.LuceneTestCase;

/**
 * Test indexing and searching some byte[] terms
 */
public class TestBinaryTerms extends LuceneTestCase {
  public void testBinary() throws IOException {
    assumeFalse("PreFlex codec cannot work with binary terms!", 
        "PreFlex".equals(CodecProvider.getDefault().getDefaultFieldCodec()));
    
    Directory dir = newDirectory();
    RandomIndexWriter iw = new RandomIndexWriter(random, dir);
    BytesRef bytes = new BytesRef(2);
    BinaryTokenStream tokenStream = new BinaryTokenStream(bytes);
    
    for (int i = 0; i < 256; i++) {
      bytes.bytes[0] = (byte) i;
      bytes.bytes[1] = (byte) (255 - i);
      bytes.length = 2;
      Document doc = new Document();
      FieldType customType = new FieldType();
      customType.setStored(true);
      doc.add(new Field("id", "" + i, customType));
      doc.add(new TextField("bytes", tokenStream));
      iw.addDocument(doc);
    }
    
    IndexReader ir = iw.getReader();
    iw.close();
    
    IndexSearcher is = newSearcher(ir);
    
    for (int i = 0; i < 256; i++) {
      bytes.bytes[0] = (byte) i;
      bytes.bytes[1] = (byte) (255 - i);
      bytes.length = 2;
      TopDocs docs = is.search(new TermQuery(new Term("bytes", bytes)), 5);
      assertEquals(1, docs.totalHits);
      assertEquals("" + i, is.doc(docs.scoreDocs[0].doc).get("id"));
    }
    
    is.close();
    ir.close();
    dir.close();
  }
}
