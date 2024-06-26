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
import java.io.IOException;

import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.CodecUtil;
import org.apache.lucene.util.LongsRef;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.packed.PackedInts;

/** Stores ints packed with fixed-bit precision. */
class PackedIntsImpl {

  private static final String CODEC_NAME = "PackedInts";

  static final int VERSION_START = 0;
  static final int VERSION_CURRENT = VERSION_START;

  static class IntsWriter extends Writer {
    // nocommit - can we bulkcopy this on a merge?
    private LongsRef intsRef;
    private long[] docToValue;
    private long minValue;
    private long maxValue;
    private boolean started;
    private final Directory dir;
    private final String id;
    private int maxDocID;
    private int minDocID;

    protected IntsWriter(Directory dir, String id) throws IOException {
      this.dir = dir;
      this.id = id;
      docToValue = new long[1];
    }

    @Override
    synchronized public void add(int docID, long v) throws IOException {

      if (!started) {
        minValue = maxValue = v;
        minDocID = maxDocID = docID;
        started = true;

      } else {
        if (v < minValue) {
          minValue = v;
        } else if (v > maxValue) {
          maxValue = v;
        }
        if (docID < minDocID) {
          minDocID = docID;
        } else if (docID > maxDocID) {
          maxDocID = docID;
        }
      }
      if (docID >= docToValue.length) {
        docToValue = ArrayUtil.grow(docToValue, 1 + docID);
      }
      docToValue[docID] = v;
    }

    @Override
    synchronized public void finish(int docCount) throws IOException {
      if(!started)
        return;
      final IndexOutput datOut = dir.createOutput(IndexFileNames
          .segmentFileName(id, "", IndexFileNames.CSF_DATA_EXTENSION));
      CodecUtil.writeHeader(datOut, CODEC_NAME, VERSION_CURRENT);

      // nocommit -- long can't work right since it's signed
      datOut.writeLong(minValue);
      // write a default value to recognize docs without a value for that field
      final long defaultValue = ++maxValue - minValue;
      datOut.writeLong(defaultValue);
      PackedInts.Writer w = PackedInts.getWriter(datOut, docCount, PackedInts.bitsRequired(maxValue-minValue));
         
      final int limit = maxDocID + 1;
      for (int i = 0; i < minDocID; i++) {
        w.add(defaultValue);
      }
      for (int i = minDocID; i < limit; i++) {
        w.add(docToValue[i] - minValue);
      }
      for (int i = limit; i < docCount; i++) {
        w.add(defaultValue);
      }
      w.finish();

      datOut.close();
    }

    public long ramBytesUsed() {
      return RamUsageEstimator.NUM_BYTES_ARRAY_HEADER + docToValue.length
          * RamUsageEstimator.NUM_BYTES_LONG;
    }

    @Override
    protected void add(int docID) throws IOException {
      add(docID, intsRef.get());
    }

    @Override
    protected void setNextAttribute(ValuesAttribute attr) {
      intsRef = attr.ints();
    }
  }

  /**
   * Opens all necessary files, but does not read any data in until you call
   * {@link #load}.
   */
  static class IntsReader extends Reader {
    private final IndexInput datIn;

    protected IntsReader(Directory dir, String id) throws IOException {
      datIn = dir.openInput(IndexFileNames.segmentFileName(id, "",
          IndexFileNames.CSF_DATA_EXTENSION));
      CodecUtil.checkHeader(datIn, CODEC_NAME, VERSION_START, VERSION_START);
    }

    /**
     * Loads the actual values. You may call this more than once, eg if you
     * already previously loaded but then discarded the Source.
     */
    @Override
    public Source load() throws IOException {
      return new IntsSource((IndexInput) datIn.clone());
    }

    private static class IntsSource extends Source {
      private final long minValue;
      private final long defaultValue;
      private final PackedInts.Reader values;

      public IntsSource(IndexInput dataIn) throws IOException {
        dataIn.seek(CodecUtil.headerLength(CODEC_NAME));
        minValue = dataIn.readLong();
        defaultValue = dataIn.readLong();
        values = PackedInts.getReader(dataIn);
      }

      @Override
      public long ints(int docID) {
        // nocommit -- can we somehow avoid 2X method calls
        // on each get? must push minValue down, and make
        // PackedInts implement Ints.Source
        final long val = values.get(docID);
        // docs not having a value for that field must return a default value
        return val == defaultValue ? 0 : minValue + val;
      }

      public long ramBytesUsed() {
        // TODO(simonw): move that to PackedInts?
        return RamUsageEstimator.NUM_BYTES_ARRAY_HEADER
            + values.getBitsPerValue() * values.size();
      }
    }

    public void close() throws IOException {
      datIn.close();
    }

    @Override
    public ValuesEnum getEnum(AttributeSource source) throws IOException {
      return new IntsEnumImpl(source, (IndexInput) datIn.clone());
    }

  }

  private static final class IntsEnumImpl extends ValuesEnum {
    private final PackedInts.ReaderIterator ints;
    private long minValue;
    private final IndexInput dataIn;
    private final long defaultValue;
    private LongsRef ref;
    private final int maxDoc;
    private int pos = -1;

    private IntsEnumImpl(AttributeSource source, IndexInput dataIn)
        throws IOException {
      super(source, Values.PACKED_INTS);
      this.ref = attr.ints();
      this.ref.offset = 0;
      this.dataIn = dataIn;
      dataIn.seek(CodecUtil.headerLength(CODEC_NAME));
      minValue = dataIn.readLong();
      defaultValue = dataIn.readLong();
      this.ints = PackedInts.getReaderIterator(dataIn);
      maxDoc = ints.size();
    }

    @Override
    public void close() throws IOException {
      ints.close();
      dataIn.close();
    }

    @Override
    public int advance(int target) throws IOException {
      if (target >= maxDoc)
        return pos = NO_MORE_DOCS;
      final long val = ints.advance(target);
      ref.ints[0] = val == defaultValue? 0:minValue + val;
      ref.offset = 0; // can we skip this?
      return pos = target;
    }

    @Override
    public int docID() {
      return pos;
    }

    @Override
    public int nextDoc() throws IOException {
      return advance(pos+1);
    }
  }
}