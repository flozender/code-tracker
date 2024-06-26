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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.search.FieldCache; // not great (circular); used only to purge FieldCache entry on close
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Comparator;

/**  A <code>FilterIndexReader</code> contains another IndexReader, which it
 * uses as its basic source of data, possibly transforming the data along the
 * way or providing additional functionality. The class
 * <code>FilterIndexReader</code> itself simply implements all abstract methods
 * of <code>IndexReader</code> with versions that pass all requests to the
 * contained index reader. Subclasses of <code>FilterIndexReader</code> may
 * further override some of these methods and may also provide additional
 * methods and fields.
 */
public class FilterIndexReader extends IndexReader {

  /** Base class for filtering {@link Fields}
   *  implementations. */
  public static class FilterFields extends Fields {
    protected Fields in;

    public FilterFields(Fields in) {
      this.in = in;
    }

    @Override
    public FieldsEnum iterator() throws IOException {
      return in.iterator();
    }

    @Override
    public Terms terms(String field) throws IOException {
      return in.terms(field);
    }
  }

  /** Base class for filtering {@link Terms}
   *  implementations. */
  public static class FilterTerms extends Terms {
    protected Terms in;

    public FilterTerms(Terms in) {
      this.in = in;
    }

    @Override
    public TermsEnum iterator() throws IOException {
      return in.iterator();
    }

    @Override
    public Comparator<BytesRef> getComparator() throws IOException {
      return in.getComparator();
    }

    @Override
    public int docFreq(BytesRef text) throws IOException {
      return in.docFreq(text);
    }

    @Override
    public DocsEnum docs(Bits skipDocs, BytesRef text, DocsEnum reuse) throws IOException {
      return in.docs(skipDocs, text, reuse);
    }

    @Override
    public DocsAndPositionsEnum docsAndPositions(Bits skipDocs, BytesRef text, DocsAndPositionsEnum reuse) throws IOException {
      return in.docsAndPositions(skipDocs, text, reuse);
    }

    @Override
    public long getUniqueTermCount() throws IOException {
      return in.getUniqueTermCount();
    }
  }

  /** Base class for filtering {@link TermsEnum} implementations. */
  public static class FilterFieldsEnum extends FieldsEnum {
    protected FieldsEnum in;
    public FilterFieldsEnum(FieldsEnum in) {
      this.in = in;
    }

    @Override
    public String next() throws IOException {
      return in.next();
    }

    @Override
    public TermsEnum terms() throws IOException {
      return in.terms();
    }
  }

  /** Base class for filtering {@link TermsEnum} implementations. */
  public static class FilterTermsEnum extends TermsEnum {
    protected TermsEnum in;

    public FilterTermsEnum(TermsEnum in) { this.in = in; }

    @Override
    public SeekStatus seek(BytesRef text, boolean useCache) throws IOException {
      return in.seek(text, useCache);
    }

    @Override
    public SeekStatus seek(long ord) throws IOException {
      return in.seek(ord);
    }

    @Override
    public BytesRef next() throws IOException {
      return in.next();
    }

    @Override
    public BytesRef term() throws IOException {
      return in.term();
    }

    @Override
    public long ord() throws IOException {
      return in.ord();
    }

    @Override
    public int docFreq() {
      return in.docFreq();
    }

    @Override
      public DocsEnum docs(Bits skipDocs, DocsEnum reuse) throws IOException {
      return in.docs(skipDocs, reuse);
    }

    @Override
    public DocsAndPositionsEnum docsAndPositions(Bits skipDocs, DocsAndPositionsEnum reuse) throws IOException {
      return in.docsAndPositions(skipDocs, reuse);
    }

    @Override
    public Comparator<BytesRef> getComparator() throws IOException {
      return in.getComparator();
    }
  }

  /** Base class for filtering {@link DocsEnum} implementations. */
  public static class FilterDocsEnum extends DocsEnum {
    protected DocsEnum in;

    public FilterDocsEnum(DocsEnum in) {
      this.in = in;
    }

    @Override
    public int docID() {
      return in.docID();
    }

    @Override
    public int freq() {
      return in.freq();
    }

    @Override
    public int nextDoc() throws IOException {
      return in.nextDoc();
    }

    @Override
    public int advance(int target) throws IOException {
      return in.advance(target);
    }

    @Override
    public BulkReadResult getBulkResult() {
      return in.getBulkResult();
    }

    @Override
    public int read() throws IOException {
      return in.read();
    }
  }

  /** Base class for filtering {@link DocsAndPositionsEnum} implementations. */
  public static class FilterDocsAndPositionsEnum extends DocsAndPositionsEnum {
    protected DocsAndPositionsEnum in;

    public FilterDocsAndPositionsEnum(DocsAndPositionsEnum in) {
      this.in = in;
    }

    @Override
    public int docID() {
      return in.docID();
    }

    @Override
    public int freq() {
      return in.freq();
    }

    @Override
    public int nextDoc() throws IOException {
      return in.nextDoc();
    }

    @Override
    public int advance(int target) throws IOException {
      return in.advance(target);
    }

    @Override
    public int nextPosition() throws IOException {
      return in.nextPosition();
    }

    @Override
    public BytesRef getPayload() throws IOException {
      return in.getPayload();
    }

    @Override
    public boolean hasPayload() {
      return in.hasPayload();
    }
  }

  protected IndexReader in;

  /**
   * <p>Construct a FilterIndexReader based on the specified base reader.
   * Directory locking for delete, undeleteAll, and setNorm operations is
   * left to the base reader.</p>
   * <p>Note that base reader is closed if this FilterIndexReader is closed.</p>
   * @param in specified base reader.
   */
  public FilterIndexReader(IndexReader in) {
    super();
    this.in = in;
  }

  @Override
  public Directory directory() {
    return in.directory();
  }
  
  @Override
  public Bits getDeletedDocs() throws IOException {
    return MultiFields.getDeletedDocs(in);
  }
  
  @Override
  public TermFreqVector[] getTermFreqVectors(int docNumber)
          throws IOException {
    ensureOpen();
    return in.getTermFreqVectors(docNumber);
  }

  @Override
  public TermFreqVector getTermFreqVector(int docNumber, String field)
          throws IOException {
    ensureOpen();
    return in.getTermFreqVector(docNumber, field);
  }


  @Override
  public void getTermFreqVector(int docNumber, String field, TermVectorMapper mapper) throws IOException {
    ensureOpen();
    in.getTermFreqVector(docNumber, field, mapper);

  }

  @Override
  public void getTermFreqVector(int docNumber, TermVectorMapper mapper) throws IOException {
    ensureOpen();
    in.getTermFreqVector(docNumber, mapper);
  }

  @Override
  public int numDocs() {
    // Don't call ensureOpen() here (it could affect performance)
    return in.numDocs();
  }

  @Override
  public int maxDoc() {
    // Don't call ensureOpen() here (it could affect performance)
    return in.maxDoc();
  }

  @Override
  public Document document(int n, FieldSelector fieldSelector) throws CorruptIndexException, IOException {
    ensureOpen();
    return in.document(n, fieldSelector);
  }

  @Override
  public boolean isDeleted(int n) {
    // Don't call ensureOpen() here (it could affect performance)
    return in.isDeleted(n);
  }

  @Override
  public boolean hasDeletions() {
    // Don't call ensureOpen() here (it could affect performance)
    return in.hasDeletions();
  }

  @Override
  protected void doUndeleteAll() throws CorruptIndexException, IOException {in.undeleteAll();}

  @Override
  public boolean hasNorms(String field) throws IOException {
    ensureOpen();
    return in.hasNorms(field);
  }

  @Override
  public byte[] norms(String f) throws IOException {
    ensureOpen();
    return in.norms(f);
  }

  @Override
  public void norms(String f, byte[] bytes, int offset) throws IOException {
    ensureOpen();
    in.norms(f, bytes, offset);
  }

  @Override
  protected void doSetNorm(int d, String f, byte b) throws CorruptIndexException, IOException {
    in.setNorm(d, f, b);
  }

  // final to force subclass to impl flex APIs, instead
  @Override
  public final TermEnum terms() throws IOException {
    ensureOpen();
    return in.terms();
  }

  // final to force subclass to impl flex APIs, instead
  @Override
  public final TermEnum terms(Term t) throws IOException {
    ensureOpen();
    return in.terms(t);
  }

  @Override
  public int docFreq(Term t) throws IOException {
    ensureOpen();
    return in.docFreq(t);
  }

  @Override
  public int docFreq(String field, BytesRef t) throws IOException {
    ensureOpen();
    return in.docFreq(field, t);
  }

  // final to force subclass to impl flex APIs, instead
  @Override
  public final TermDocs termDocs() throws IOException {
    ensureOpen();
    return in.termDocs();
  }

  // final to force subclass to impl flex APIs, instead
  @Override
  public final TermDocs termDocs(Term term) throws IOException {
    ensureOpen();
    return in.termDocs(term);
  }

  // final to force subclass to impl flex APIs, instead
  @Override
  public final TermPositions termPositions() throws IOException {
    ensureOpen();
    return in.termPositions();
  }

  @Override
  protected void doDelete(int n) throws  CorruptIndexException, IOException { in.deleteDocument(n); }
  
  @Override
  protected void doCommit(Map<String,String> commitUserData) throws IOException { in.commit(commitUserData); }
  
  @Override
  protected void doClose() throws IOException {
    in.close();

    // NOTE: only needed in case someone had asked for
    // FieldCache for top-level reader (which is generally
    // not a good idea):
    FieldCache.DEFAULT.purge(this);
  }


  @Override
  public Collection<String> getFieldNames(IndexReader.FieldOption fieldNames) {
    ensureOpen();
    return in.getFieldNames(fieldNames);
  }

  @Override
  public long getVersion() {
    ensureOpen();
    return in.getVersion();
  }

  @Override
  public boolean isCurrent() throws CorruptIndexException, IOException {
    ensureOpen();
    return in.isCurrent();
  }
  
  @Override
  public boolean isOptimized() {
    ensureOpen();
    return in.isOptimized();
  }
  
  @Override
  public IndexReader[] getSequentialSubReaders() {
    return null;
  }

  @Override
  public Fields fields() throws IOException {
    return MultiFields.getFields(in);
  }

  /** If the subclass of FilteredIndexReader modifies the
   *  contents of the FieldCache, you must override this
   *  method to provide a different key */
  @Override
  public Object getCoreCacheKey() {
    return in.getCoreCacheKey();
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    final StringBuilder buffer = new StringBuilder("FilterReader(");
    buffer.append(in);
    buffer.append(')');
    return buffer.toString();
  }
}