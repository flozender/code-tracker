package org.apache.lucene.index.values;

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

/** Base class for specific Bytes Reader/Writer implementations */
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.values.DocValues.SortedSource;
import org.apache.lucene.index.values.DocValues.Source;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.ByteBlockPool;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CodecUtil;

/**
 * Provides concrete Writer/Reader impls for byte[] value per document. There
 * are 6 package-private impls of this, for all combinations of
 * STRAIGHT/DEREF/SORTED X fixed/not fixed.
 * 
 * <p>
 * NOTE: The total amount of byte[] data stored (across a single segment) cannot
 * exceed 2GB.
 * </p>
 * <p>
 * NOTE: Each byte[] must be <= 32768 bytes in length
 * </p>
 */
//TODO - add bulk copy where possible
public final class Bytes {

  // don't instantiate!
  private Bytes() {
  }

  public static enum Mode {
    STRAIGHT, DEREF, SORTED
  };

  
  // nocommit -- i shouldn't have to specify fixed? can
  // track itself & do the write thing at write time?
  public static Writer getWriter(Directory dir, String id, Mode mode,
      Comparator<BytesRef> comp, boolean fixedSize) throws IOException {

    if (comp == null) {
      comp = BytesRef.getUTF8SortedAsUnicodeComparator();
    }

    if (fixedSize) {
      if (mode == Mode.STRAIGHT) {
        return new FixedStraightBytesImpl.Writer(dir, id);
      } else if (mode == Mode.DEREF) {
        return new FixedDerefBytesImpl.Writer(dir, id);
      } else if (mode == Mode.SORTED) {
        return new FixedSortedBytesImpl.Writer(dir, id, comp);
      }
    } else {
      if (mode == Mode.STRAIGHT) {
        return new VarStraightBytesImpl.Writer(dir, id);
      } else if (mode == Mode.DEREF) {
        return new VarDerefBytesImpl.Writer(dir, id);
      } else if (mode == Mode.SORTED) {
        return new VarSortedBytesImpl.Writer(dir, id, comp);
      }
    }

    throw new IllegalArgumentException("");
  }

  // nocommit -- I can peek @ header to determing fixed/mode?
  public static DocValues getValues(Directory dir, String id, Mode mode,
      boolean fixedSize, int maxDoc) throws IOException {
    if (fixedSize) {
      if (mode == Mode.STRAIGHT) {
        try {
          return new FixedStraightBytesImpl.Reader(dir, id, maxDoc);
        } catch (IOException e) {
          throw e;
        }
      } else if (mode == Mode.DEREF) {
        try {
          return new FixedDerefBytesImpl.Reader(dir, id, maxDoc);
        } catch (IOException e) {
          throw e;
        }
      } else if (mode == Mode.SORTED) {
        return new FixedSortedBytesImpl.Reader(dir, id, maxDoc);
      }
    } else {
      if (mode == Mode.STRAIGHT) {
        return new VarStraightBytesImpl.Reader(dir, id, maxDoc);
      } else if (mode == Mode.DEREF) {
        return new VarDerefBytesImpl.Reader(dir, id, maxDoc);
      } else if (mode == Mode.SORTED) {
        return new VarSortedBytesImpl.Reader(dir, id, maxDoc);
      }
    }

    throw new IllegalArgumentException("");
  }

  static abstract class BytesBaseSource extends Source {
    protected final IndexInput datIn;
    protected final IndexInput idxIn;
    protected final BytesRef defaultValue = new BytesRef();

    protected BytesBaseSource(IndexInput datIn, IndexInput idxIn) {
      this.datIn = datIn;
      this.idxIn = idxIn;
    }

    public void close() throws IOException {
      if (datIn != null)
        datIn.close();
      if (idxIn != null) // if straight
        idxIn.close();

    }
  }

  static abstract class BytesBaseSortedSource extends SortedSource {
    protected final IndexInput datIn;
    protected final IndexInput idxIn;
    protected final BytesRef defaultValue = new BytesRef();

    protected BytesBaseSortedSource(IndexInput datIn, IndexInput idxIn) {
      this.datIn = datIn;
      this.idxIn = idxIn;
    }

    public void close() throws IOException {
      if (datIn != null)
        datIn.close();
      if (idxIn != null) // if straight
        idxIn.close();

    }
  }

  static abstract class BytesWriterBase extends Writer {


    private final Directory dir;
    private final String id;
    protected IndexOutput idxOut;
    protected IndexOutput datOut;
    protected BytesRef bytesRef;
    private String codecName;
    private int version;
    protected final ByteBlockPool pool;
    protected final AtomicLong bytesUsed;

    protected BytesWriterBase(Directory dir, String id, String codecName,
        int version, boolean initIndex, boolean initData, ByteBlockPool pool, AtomicLong bytesUsed) throws IOException {
      this.dir = dir;
      this.id = id;
      this.codecName = codecName;
      this.version = version;
      this.pool = pool;
      this.bytesUsed = bytesUsed;
      if (initData)
        initDataOut();
      if (initIndex)
        initIndexOut();
    }

    protected void initDataOut() throws IOException {
      datOut = dir.createOutput(IndexFileNames.segmentFileName(id, "",
          IndexFileNames.CSF_DATA_EXTENSION));
      CodecUtil.writeHeader(datOut, codecName, version);
    }

    protected void initIndexOut() throws IOException {
      idxOut = dir.createOutput(IndexFileNames.segmentFileName(id, "",
          IndexFileNames.CSF_INDEX_EXTENSION));
      CodecUtil.writeHeader(idxOut, codecName, version);
    }

    public long ramBytesUsed() {
      return bytesUsed.get();
    }

    /**
     * Must be called only with increasing docIDs. It's OK for some docIDs to be
     * skipped; they will be filled with 0 bytes.
     */
    @Override
    public abstract void add(int docID, BytesRef bytes) throws IOException;

    @Override
    public synchronized void finish(int docCount) throws IOException {
      if (datOut != null)
        datOut.close();
      if (idxOut != null)
        idxOut.close();
      if(pool != null)
        pool.reset();
    }

    @Override
    protected void add(int docID) throws IOException {
      add(docID, bytesRef);
    }

    @Override
    protected void setNextAttribute(ValuesAttribute attr) {
      bytesRef = attr.bytes();
      assert bytesRef != null;
    }
    
    @Override
    public void add(int docID, ValuesAttribute attr) throws IOException {
      final BytesRef ref;
      if((ref = attr.bytes()) != null) {
        add(docID, ref);
      }
    }

    @Override
    public void files(Collection<String> files) throws IOException {
      files.add(IndexFileNames.segmentFileName(id, "",
          IndexFileNames.CSF_DATA_EXTENSION));
      final String idxFile = IndexFileNames.segmentFileName(id, "",
          IndexFileNames.CSF_INDEX_EXTENSION);
      if (dir.fileExists(idxFile)) { // TODO is this correct? could be initialized lazy
        files.add(idxFile);
      }
    }
  }

  /**
   * Opens all necessary files, but does not read any data in until you call
   * {@link #load}.
   */
   static abstract class BytesReaderBase extends DocValues {
    protected final IndexInput idxIn;
    protected final IndexInput datIn;
    protected final int version;
    protected final String id;

    protected BytesReaderBase(Directory dir, String id, String codecName,
        int maxVersion, boolean doIndex) throws IOException {
      this.id = id;
      datIn = dir.openInput(IndexFileNames.segmentFileName(id, "",
          IndexFileNames.CSF_DATA_EXTENSION));
      version = CodecUtil.checkHeader(datIn, codecName, maxVersion, maxVersion);

      if (doIndex) {
        idxIn = dir.openInput(IndexFileNames.segmentFileName(id, "",
            IndexFileNames.CSF_INDEX_EXTENSION));
        final int version2 = CodecUtil.checkHeader(idxIn, codecName,
            maxVersion, maxVersion);
        assert version == version2;
      } else {
        idxIn = null;
      }
    }

    protected final IndexInput cloneData() {
      // is never NULL
      return (IndexInput) datIn.clone();
    }

    protected final IndexInput cloneIndex() {
      return idxIn == null ? null : (IndexInput) idxIn.clone();
    }

    public void close() throws IOException {
      if (datIn != null) {
        datIn.close();
      }
      if (idxIn != null) {
        idxIn.close();
      }
    }
  }

}