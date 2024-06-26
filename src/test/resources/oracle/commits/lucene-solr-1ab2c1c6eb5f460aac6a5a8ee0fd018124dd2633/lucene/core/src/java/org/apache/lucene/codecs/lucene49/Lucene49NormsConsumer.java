package org.apache.lucene.codecs.lucene49;

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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.DocValuesConsumer;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.packed.BlockPackedWriter;
import org.apache.lucene.util.packed.PackedInts;

import static org.apache.lucene.codecs.lucene49.Lucene49NormsFormat.VERSION_CURRENT;

/**
 * Writer for {@link Lucene49NormsFormat}
 */
class Lucene49NormsConsumer extends DocValuesConsumer { 
  static final byte DELTA_COMPRESSED = 0;
  static final byte TABLE_COMPRESSED = 1;
  static final byte CONST_COMPRESSED = 2;
  static final byte UNCOMPRESSED = 3;
  static final int BLOCK_SIZE = 16384;

  IndexOutput data, meta;
  final int maxDoc;
  
  Lucene49NormsConsumer(SegmentWriteState state, String dataCodec, String dataExtension, String metaCodec, String metaExtension) throws IOException {
    maxDoc = state.segmentInfo.getDocCount();
    boolean success = false;
    try {
      String dataName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, dataExtension);
      data = state.directory.createOutput(dataName, state.context);
      CodecUtil.writeHeader(data, dataCodec, VERSION_CURRENT);
      String metaName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, metaExtension);
      meta = state.directory.createOutput(metaName, state.context);
      CodecUtil.writeHeader(meta, metaCodec, VERSION_CURRENT);
      success = true;
    } finally {
      if (!success) {
        IOUtils.closeWhileHandlingException(this);
      }
    }
  }
  
  // we explicitly use only certain bits per value and a specified format, so we statically check this will work
  static {
    assert PackedInts.Format.PACKED_SINGLE_BLOCK.isSupported(1);
    assert PackedInts.Format.PACKED_SINGLE_BLOCK.isSupported(2);
    assert PackedInts.Format.PACKED_SINGLE_BLOCK.isSupported(4);
  }

  @Override
  public void addNumericField(FieldInfo field, Iterable<Number> values) throws IOException {
    meta.writeVInt(field.number);
    long minValue = Long.MAX_VALUE;
    long maxValue = Long.MIN_VALUE;
    // TODO: more efficient?
    HashSet<Long> uniqueValues = null;
    uniqueValues = new HashSet<>();
    
    long count = 0;
    for (Number nv : values) {
      if (nv == null) {
        throw new IllegalStateException("illegal norms data for field " + field.name + ", got null for value: " + count);
      }
      final long v = nv.longValue();
      
      minValue = Math.min(minValue, v);
      maxValue = Math.max(maxValue, v);
      
      if (uniqueValues != null) {
        if (uniqueValues.add(v)) {
          if (uniqueValues.size() > 256) {
            uniqueValues = null;
          }
        }
      }
      ++count;
    }
    
    if (count != maxDoc) {
      throw new IllegalStateException("illegal norms data for field " + field.name + ", expected " + maxDoc + " values, got " + count);
    }
    
    if (uniqueValues != null && uniqueValues.size() == 1) {
      // 0 bpv
      meta.writeByte(CONST_COMPRESSED);
      meta.writeLong(minValue);
    } else if (uniqueValues != null) {
      // small number of unique values: this is the typical case:
      // we only use bpv=1,2,4,8     
      PackedInts.Format format = PackedInts.Format.PACKED_SINGLE_BLOCK;
      int bitsPerValue = PackedInts.bitsRequired(uniqueValues.size()-1);
      if (bitsPerValue == 3) {
        bitsPerValue = 4;
      } else if (bitsPerValue > 4) {
        bitsPerValue = 8;
      }
      
      if (bitsPerValue == 8 && minValue >= Byte.MIN_VALUE && maxValue <= Byte.MAX_VALUE) {
        meta.writeByte(UNCOMPRESSED); // uncompressed byte[]
        meta.writeLong(data.getFilePointer());
        for (Number nv : values) {
          data.writeByte(nv == null ? 0 : (byte) nv.longValue());
        }
      } else {
        meta.writeByte(TABLE_COMPRESSED); // table-compressed
        meta.writeLong(data.getFilePointer());
        data.writeVInt(PackedInts.VERSION_CURRENT);
        
        Long[] decode = uniqueValues.toArray(new Long[uniqueValues.size()]);
        Arrays.sort(decode);
        final HashMap<Long,Integer> encode = new HashMap<>();
        // upgrade to power of two sized array
        int size = 1 << bitsPerValue;
        data.writeVInt(size);
        for (int i = 0; i < decode.length; i++) {
          data.writeLong(decode[i]);
          encode.put(decode[i], i);
        }
        for (int i = decode.length; i < size; i++) {
          data.writeLong(0);
        }

        data.writeVInt(format.getId());
        data.writeVInt(bitsPerValue);

        final PackedInts.Writer writer = PackedInts.getWriterNoHeader(data, format, maxDoc, bitsPerValue, PackedInts.DEFAULT_BUFFER_SIZE);
        for(Number nv : values) {
          writer.add(encode.get(nv.longValue()));
        }
        writer.finish();
      }
    } else {
      meta.writeByte(DELTA_COMPRESSED); // delta-compressed
      meta.writeLong(data.getFilePointer());
      data.writeVInt(PackedInts.VERSION_CURRENT);
      data.writeVInt(BLOCK_SIZE);

      final BlockPackedWriter writer = new BlockPackedWriter(data, BLOCK_SIZE);
      for (Number nv : values) {
        writer.add(nv.longValue());
      }
      writer.finish();
    }
  }
  
  @Override
  public void close() throws IOException {
    boolean success = false;
    try {
      if (meta != null) {
        meta.writeVInt(-1); // write EOF marker
        CodecUtil.writeFooter(meta); // write checksum
      }
      if (data != null) {
        CodecUtil.writeFooter(data); // write checksum
      }
      success = true;
    } finally {
      if (success) {
        IOUtils.close(data, meta);
      } else {
        IOUtils.closeWhileHandlingException(data, meta);
      }
      meta = data = null;
    }
  }

  @Override
  public void addBinaryField(FieldInfo field, final Iterable<BytesRef> values) throws IOException {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public void addSortedField(FieldInfo field, Iterable<BytesRef> values, Iterable<Number> docToOrd) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addSortedSetField(FieldInfo field, Iterable<BytesRef> values, final Iterable<Number> docToOrdCount, final Iterable<Number> ords) throws IOException {
    throw new UnsupportedOperationException();
  }
}
