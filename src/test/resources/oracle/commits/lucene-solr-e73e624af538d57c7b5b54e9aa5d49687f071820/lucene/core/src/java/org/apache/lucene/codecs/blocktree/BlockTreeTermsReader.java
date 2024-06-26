package org.apache.lucene.codecs.blocktree;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.PostingsReaderBase;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.Terms;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IOUtils;

/** A block-based terms index and dictionary that assigns
 *  terms to variable length blocks according to how they
 *  share prefixes.  The terms index is a prefix trie
 *  whose leaves are term blocks.  The advantage of this
 *  approach is that seekExact is often able to
 *  determine a term cannot exist without doing any IO, and
 *  intersection with Automata is very fast.  Note that this
 *  terms dictionary has it's own fixed terms index (ie, it
 *  does not support a pluggable terms index
 *  implementation).
 *
 *  <p><b>NOTE</b>: this terms dictionary supports
 *  min/maxItemsPerBlock during indexing to control how
 *  much memory the terms index uses.</p>
 *
 *  <p>The data structure used by this implementation is very
 *  similar to a burst trie
 *  (http://citeseer.ist.psu.edu/viewdoc/summary?doi=10.1.1.18.3499),
 *  but with added logic to break up too-large blocks of all
 *  terms sharing a given prefix into smaller ones.</p>
 *
 *  <p>Use {@link org.apache.lucene.index.CheckIndex} with the <code>-verbose</code>
 *  option to see summary statistics on the blocks in the
 *  dictionary.
 *
 *  See {@link BlockTreeTermsWriter}.
 *
 * @lucene.experimental
 */

public final class BlockTreeTermsReader extends FieldsProducer {

  // Open input to the main terms dict file (_X.tib)
  final IndexInput in;

  //private static final boolean DEBUG = BlockTreeTermsWriter.DEBUG;

  // Reads the terms dict entries, to gather state to
  // produce DocsEnum on demand
  final PostingsReaderBase postingsReader;

  private final TreeMap<String,FieldReader> fields = new TreeMap<>();

  /** File offset where the directory starts in the terms file. */
  private long dirOffset;

  /** File offset where the directory starts in the index file. */
  private long indexDirOffset;

  final String segment;
  
  private final int version;

  /** Sole constructor. */
  public BlockTreeTermsReader(Directory dir, FieldInfos fieldInfos, SegmentInfo info,
                              PostingsReaderBase postingsReader, IOContext ioContext,
                              String segmentSuffix)
    throws IOException {
    
    this.postingsReader = postingsReader;

    this.segment = info.name;
    in = dir.openInput(IndexFileNames.segmentFileName(segment, segmentSuffix, BlockTreeTermsWriter.TERMS_EXTENSION),
                       ioContext);

    boolean success = false;
    IndexInput indexIn = null;

    try {
      version = readHeader(in);
      indexIn = dir.openInput(IndexFileNames.segmentFileName(segment, segmentSuffix, BlockTreeTermsWriter.TERMS_INDEX_EXTENSION),
                                ioContext);
      int indexVersion = readIndexHeader(indexIn);
      if (indexVersion != version) {
        throw new CorruptIndexException("mixmatched version files: " + in + "=" + version + "," + indexIn + "=" + indexVersion);
      }
      
      // verify
      if (version >= BlockTreeTermsWriter.VERSION_CHECKSUM) {
        CodecUtil.checksumEntireFile(indexIn);
      }

      // Have PostingsReader init itself
      postingsReader.init(in);
      
      
      // NOTE: data file is too costly to verify checksum against all the bytes on open,
      // but for now we at least verify proper structure of the checksum footer: which looks
      // for FOOTER_MAGIC + algorithmID. This is cheap and can detect some forms of corruption
      // such as file truncation.
      if (version >= BlockTreeTermsWriter.VERSION_CHECKSUM) {
        CodecUtil.retrieveChecksum(in);
      }

      // Read per-field details
      seekDir(in, dirOffset);
      seekDir(indexIn, indexDirOffset);

      final int numFields = in.readVInt();
      if (numFields < 0) {
        throw new CorruptIndexException("invalid numFields: " + numFields + " (resource=" + in + ")");
      }

      for(int i=0;i<numFields;i++) {
        final int field = in.readVInt();
        final long numTerms = in.readVLong();
        if (numTerms <= 0) {
          throw new CorruptIndexException("Illegal numTerms for field number: " + field + " (resource=" + in + ")");
        }
        final int numBytes = in.readVInt();
        if (numBytes < 0) {
          throw new CorruptIndexException("invalid rootCode for field number: " + field + ", numBytes=" + numBytes + " (resource=" + in + ")");
        }
        final BytesRef rootCode = new BytesRef(new byte[numBytes]);
        in.readBytes(rootCode.bytes, 0, numBytes);
        rootCode.length = numBytes;
        final FieldInfo fieldInfo = fieldInfos.fieldInfo(field);
        if (fieldInfo == null) {
          throw new CorruptIndexException("invalid field number: " + field + ", resource=" + in + ")");
        }
        final long sumTotalTermFreq = fieldInfo.getIndexOptions() == IndexOptions.DOCS_ONLY ? -1 : in.readVLong();
        final long sumDocFreq = in.readVLong();
        final int docCount = in.readVInt();
        final int longsSize = version >= BlockTreeTermsWriter.VERSION_META_ARRAY ? in.readVInt() : 0;
        if (longsSize < 0) {
          throw new CorruptIndexException("invalid longsSize for field: " + fieldInfo.name + ", longsSize=" + longsSize + " (resource=" + in + ")");
        }
        BytesRef minTerm, maxTerm;
        if (version >= BlockTreeTermsWriter.VERSION_MIN_MAX_TERMS) {
          minTerm = readBytesRef(in);
          maxTerm = readBytesRef(in);
        } else {
          minTerm = maxTerm = null;
        }
        if (docCount < 0 || docCount > info.getDocCount()) { // #docs with field must be <= #docs
          throw new CorruptIndexException("invalid docCount: " + docCount + " maxDoc: " + info.getDocCount() + " (resource=" + in + ")");
        }
        if (sumDocFreq < docCount) {  // #postings must be >= #docs with field
          throw new CorruptIndexException("invalid sumDocFreq: " + sumDocFreq + " docCount: " + docCount + " (resource=" + in + ")");
        }
        if (sumTotalTermFreq != -1 && sumTotalTermFreq < sumDocFreq) { // #positions must be >= #postings
          throw new CorruptIndexException("invalid sumTotalTermFreq: " + sumTotalTermFreq + " sumDocFreq: " + sumDocFreq + " (resource=" + in + ")");
        }
        final long indexStartFP = indexIn.readVLong();
        FieldReader previous = fields.put(fieldInfo.name,       
                                          new FieldReader(this, fieldInfo, numTerms, rootCode, sumTotalTermFreq, sumDocFreq, docCount,
                                                          indexStartFP, longsSize, indexIn, minTerm, maxTerm));
        if (previous != null) {
          throw new CorruptIndexException("duplicate field: " + fieldInfo.name + " (resource=" + in + ")");
        }
      }
      indexIn.close();

      success = true;
    } finally {
      if (!success) {
        // this.close() will close in:
        IOUtils.closeWhileHandlingException(indexIn, this);
      }
    }
  }

  private static BytesRef readBytesRef(IndexInput in) throws IOException {
    BytesRef bytes = new BytesRef();
    bytes.length = in.readVInt();
    bytes.bytes = new byte[bytes.length];
    in.readBytes(bytes.bytes, 0, bytes.length);
    return bytes;
  }

  /** Reads terms file header. */
  private int readHeader(IndexInput input) throws IOException {
    int version = CodecUtil.checkHeader(input, BlockTreeTermsWriter.TERMS_CODEC_NAME,
                          BlockTreeTermsWriter.VERSION_START,
                          BlockTreeTermsWriter.VERSION_CURRENT);
    if (version < BlockTreeTermsWriter.VERSION_APPEND_ONLY) {
      dirOffset = input.readLong();
    }
    return version;
  }

  /** Reads index file header. */
  private int readIndexHeader(IndexInput input) throws IOException {
    int version = CodecUtil.checkHeader(input, BlockTreeTermsWriter.TERMS_INDEX_CODEC_NAME,
                          BlockTreeTermsWriter.VERSION_START,
                          BlockTreeTermsWriter.VERSION_CURRENT);
    if (version < BlockTreeTermsWriter.VERSION_APPEND_ONLY) {
      indexDirOffset = input.readLong(); 
    }
    return version;
  }

  /** Seek {@code input} to the directory offset. */
  private void seekDir(IndexInput input, long dirOffset)
      throws IOException {
    if (version >= BlockTreeTermsWriter.VERSION_CHECKSUM) {
      input.seek(input.length() - CodecUtil.footerLength() - 8);
      dirOffset = input.readLong();
    } else if (version >= BlockTreeTermsWriter.VERSION_APPEND_ONLY) {
      input.seek(input.length() - 8);
      dirOffset = input.readLong();
    }
    input.seek(dirOffset);
  }

  // for debugging
  // private static String toHex(int v) {
  //   return "0x" + Integer.toHexString(v);
  // }

  @Override
  public void close() throws IOException {
    try {
      IOUtils.close(in, postingsReader);
    } finally { 
      // Clear so refs to terms index is GCable even if
      // app hangs onto us:
      fields.clear();
    }
  }

  @Override
  public Iterator<String> iterator() {
    return Collections.unmodifiableSet(fields.keySet()).iterator();
  }

  @Override
  public Terms terms(String field) throws IOException {
    assert field != null;
    return fields.get(field);
  }

  @Override
  public int size() {
    return fields.size();
  }

  // for debugging
  String brToString(BytesRef b) {
    if (b == null) {
      return "null";
    } else {
      try {
        return b.utf8ToString() + " " + b;
      } catch (Throwable t) {
        // If BytesRef isn't actually UTF8, or it's eg a
        // prefix of UTF8 that ends mid-unicode-char, we
        // fallback to hex:
        return b.toString();
      }
    }
  }

  @Override
  public long ramBytesUsed() {
    long sizeInBytes = postingsReader.ramBytesUsed();
    for(FieldReader reader : fields.values()) {
      sizeInBytes += reader.ramBytesUsed();
    }
    return sizeInBytes;
  }

  @Override
  public Iterable<? extends Accountable> getChildResources() {
    List<Accountable> resources = new ArrayList<>();
    resources.addAll(Accountables.namedAccountables("field", fields));
    resources.add(Accountables.namedAccountable("delegate", postingsReader));
    return Collections.unmodifiableList(resources);
  }

  @Override
  public void checkIntegrity() throws IOException {
    if (version >= BlockTreeTermsWriter.VERSION_CHECKSUM) {      
      // term dictionary
      CodecUtil.checksumEntireFile(in);
      
      // postings
      postingsReader.checkIntegrity();
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(fields=" + fields.size() + ",delegate=" + postingsReader + ")";
  }
}
