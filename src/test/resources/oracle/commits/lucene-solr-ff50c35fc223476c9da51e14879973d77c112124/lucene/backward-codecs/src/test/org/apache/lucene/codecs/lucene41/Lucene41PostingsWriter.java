package org.apache.lucene.codecs.lucene41;

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

import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.PushPostingsWriterBase;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.packed.PackedInts;

import static org.apache.lucene.codecs.lucene41.ForUtil.MAX_DATA_SIZE;
import static org.apache.lucene.codecs.lucene41.ForUtil.MAX_ENCODED_SIZE;
import static org.apache.lucene.codecs.lucene41.Lucene41PostingsFormat.BLOCK_SIZE;

/**
 * Writes 4.1 postings for testing
 * @deprecated for test purposes only
 */
@Deprecated
public final class Lucene41PostingsWriter extends PushPostingsWriterBase {

  IndexOutput docOut;
  IndexOutput posOut;
  IndexOutput payOut;

  final static IntBlockTermState emptyState = new IntBlockTermState();
  IntBlockTermState lastState;

  // Holds starting file pointers for current term:
  private long docStartFP;
  private long posStartFP;
  private long payStartFP;

  final int[] docDeltaBuffer;
  final int[] freqBuffer;
  private int docBufferUpto;

  final int[] posDeltaBuffer;
  final int[] payloadLengthBuffer;
  final int[] offsetStartDeltaBuffer;
  final int[] offsetLengthBuffer;
  private int posBufferUpto;

  private byte[] payloadBytes;
  private int payloadByteUpto;

  private int lastBlockDocID;
  private long lastBlockPosFP;
  private long lastBlockPayFP;
  private int lastBlockPosBufferUpto;
  private int lastBlockPayloadByteUpto;

  private int lastDocID;
  private int lastPosition;
  private int lastStartOffset;
  private int docCount;

  final byte[] encoded;

  private final ForUtil forUtil;
  private final Lucene41SkipWriter skipWriter;
  
  /** Creates a postings writer with the specified PackedInts overhead ratio */
  // TODO: does this ctor even make sense?
  public Lucene41PostingsWriter(SegmentWriteState state, float acceptableOverheadRatio) throws IOException {
    super();

    docOut = state.directory.createOutput(IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, Lucene41PostingsFormat.DOC_EXTENSION),
                                                  state.context);
    IndexOutput posOut = null;
    IndexOutput payOut = null;
    boolean success = false;
    try {
      CodecUtil.writeHeader(docOut, Lucene41PostingsFormat.DOC_CODEC, Lucene41PostingsFormat.VERSION_CURRENT);
      forUtil = new ForUtil(acceptableOverheadRatio, docOut);
      if (state.fieldInfos.hasProx()) {
        posDeltaBuffer = new int[MAX_DATA_SIZE];
        posOut = state.directory.createOutput(IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, Lucene41PostingsFormat.POS_EXTENSION),
                                                      state.context);
        CodecUtil.writeHeader(posOut, Lucene41PostingsFormat.POS_CODEC, Lucene41PostingsFormat.VERSION_CURRENT);

        if (state.fieldInfos.hasPayloads()) {
          payloadBytes = new byte[128];
          payloadLengthBuffer = new int[MAX_DATA_SIZE];
        } else {
          payloadBytes = null;
          payloadLengthBuffer = null;
        }

        if (state.fieldInfos.hasOffsets()) {
          offsetStartDeltaBuffer = new int[MAX_DATA_SIZE];
          offsetLengthBuffer = new int[MAX_DATA_SIZE];
        } else {
          offsetStartDeltaBuffer = null;
          offsetLengthBuffer = null;
        }

        if (state.fieldInfos.hasPayloads() || state.fieldInfos.hasOffsets()) {
          payOut = state.directory.createOutput(IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, Lucene41PostingsFormat.PAY_EXTENSION),
                                                        state.context);
          CodecUtil.writeHeader(payOut, Lucene41PostingsFormat.PAY_CODEC, Lucene41PostingsFormat.VERSION_CURRENT);
        }
      } else {
        posDeltaBuffer = null;
        payloadLengthBuffer = null;
        offsetStartDeltaBuffer = null;
        offsetLengthBuffer = null;
        payloadBytes = null;
      }
      this.payOut = payOut;
      this.posOut = posOut;
      success = true;
    } finally {
      if (!success) {
        IOUtils.closeWhileHandlingException(docOut, posOut, payOut);
      }
    }

    docDeltaBuffer = new int[MAX_DATA_SIZE];
    freqBuffer = new int[MAX_DATA_SIZE];

    // TODO: should we try skipping every 2/4 blocks...?
    skipWriter = new Lucene41SkipWriter(Lucene41PostingsFormat.maxSkipLevels,
                                     BLOCK_SIZE, 
                                     state.segmentInfo.getDocCount(),
                                     docOut,
                                     posOut,
                                     payOut);

    encoded = new byte[MAX_ENCODED_SIZE];
  }

  /** Creates a postings writer with <code>PackedInts.COMPACT</code> */
  public Lucene41PostingsWriter(SegmentWriteState state) throws IOException {
    this(state, PackedInts.COMPACT);
  }

  @Override
  public IntBlockTermState newTermState() {
    return new IntBlockTermState();
  }

  @Override
  public void init(IndexOutput termsOut, SegmentWriteState state) throws IOException {
    CodecUtil.writeHeader(termsOut, Lucene41PostingsFormat.TERMS_CODEC, Lucene41PostingsFormat.VERSION_CURRENT);
    termsOut.writeVInt(BLOCK_SIZE);
  }

  @Override
  public int setField(FieldInfo fieldInfo) {
    super.setField(fieldInfo);
    skipWriter.setField(writePositions, writeOffsets, writePayloads);
    lastState = emptyState;
    if (writePositions) {
      if (writePayloads || writeOffsets) {
        return 3;  // doc + pos + pay FP
      } else {
        return 2;  // doc + pos FP
      }
    } else {
      return 1;    // doc FP
    }
  }

  @Override
  public void startTerm() {
    docStartFP = docOut.getFilePointer();
    if (writePositions) {
      posStartFP = posOut.getFilePointer();
      if (writePayloads || writeOffsets) {
        payStartFP = payOut.getFilePointer();
      }
    }
    lastDocID = 0;
    lastBlockDocID = -1;
    // if (DEBUG) {
    //   System.out.println("FPW.startTerm startFP=" + docStartFP);
    // }
    skipWriter.resetSkip();
  }

  @Override
  public void startDoc(int docID, int termDocFreq) throws IOException {
    // if (DEBUG) {
    //   System.out.println("FPW.startDoc docID["+docBufferUpto+"]=" + docID);
    // }
    // Have collected a block of docs, and get a new doc. 
    // Should write skip data as well as postings list for
    // current block.
    if (lastBlockDocID != -1 && docBufferUpto == 0) {
      // if (DEBUG) {
      //   System.out.println("  bufferSkip at writeBlock: lastDocID=" + lastBlockDocID + " docCount=" + (docCount-1));
      // }
      skipWriter.bufferSkip(lastBlockDocID, docCount, lastBlockPosFP, lastBlockPayFP, lastBlockPosBufferUpto, lastBlockPayloadByteUpto);
    }

    final int docDelta = docID - lastDocID;

    if (docID < 0 || (docCount > 0 && docDelta <= 0)) {
      throw new CorruptIndexException("docs out of order (" + docID + " <= " + lastDocID + " )", docOut.toString());
    }

    docDeltaBuffer[docBufferUpto] = docDelta;
    // if (DEBUG) {
    //   System.out.println("  docDeltaBuffer[" + docBufferUpto + "]=" + docDelta);
    // }
    if (writeFreqs) {
      freqBuffer[docBufferUpto] = termDocFreq;
    }
    docBufferUpto++;
    docCount++;

    if (docBufferUpto == BLOCK_SIZE) {
      // if (DEBUG) {
      //   System.out.println("  write docDelta block @ fp=" + docOut.getFilePointer());
      // }
      forUtil.writeBlock(docDeltaBuffer, encoded, docOut);
      if (writeFreqs) {
        // if (DEBUG) {
        //   System.out.println("  write freq block @ fp=" + docOut.getFilePointer());
        // }
        forUtil.writeBlock(freqBuffer, encoded, docOut);
      }
      // NOTE: don't set docBufferUpto back to 0 here;
      // finishDoc will do so (because it needs to see that
      // the block was filled so it can save skip data)
    }


    lastDocID = docID;
    lastPosition = 0;
    lastStartOffset = 0;
  }

  @Override
  public void addPosition(int position, BytesRef payload, int startOffset, int endOffset) throws IOException {
    // if (DEBUG) {
    //   System.out.println("FPW.addPosition pos=" + position + " posBufferUpto=" + posBufferUpto + (writePayloads ? " payloadByteUpto=" + payloadByteUpto: ""));
    // }
    posDeltaBuffer[posBufferUpto] = position - lastPosition;
    if (writePayloads) {
      if (payload == null || payload.length == 0) {
        // no payload
        payloadLengthBuffer[posBufferUpto] = 0;
      } else {
        payloadLengthBuffer[posBufferUpto] = payload.length;
        if (payloadByteUpto + payload.length > payloadBytes.length) {
          payloadBytes = ArrayUtil.grow(payloadBytes, payloadByteUpto + payload.length);
        }
        System.arraycopy(payload.bytes, payload.offset, payloadBytes, payloadByteUpto, payload.length);
        payloadByteUpto += payload.length;
      }
    }

    if (writeOffsets) {
      assert startOffset >= lastStartOffset;
      assert endOffset >= startOffset;
      offsetStartDeltaBuffer[posBufferUpto] = startOffset - lastStartOffset;
      offsetLengthBuffer[posBufferUpto] = endOffset - startOffset;
      lastStartOffset = startOffset;
    }
    
    posBufferUpto++;
    lastPosition = position;
    if (posBufferUpto == BLOCK_SIZE) {
      // if (DEBUG) {
      //   System.out.println("  write pos bulk block @ fp=" + posOut.getFilePointer());
      // }
      forUtil.writeBlock(posDeltaBuffer, encoded, posOut);

      if (writePayloads) {
        forUtil.writeBlock(payloadLengthBuffer, encoded, payOut);
        payOut.writeVInt(payloadByteUpto);
        payOut.writeBytes(payloadBytes, 0, payloadByteUpto);
        payloadByteUpto = 0;
      }
      if (writeOffsets) {
        forUtil.writeBlock(offsetStartDeltaBuffer, encoded, payOut);
        forUtil.writeBlock(offsetLengthBuffer, encoded, payOut);
      }
      posBufferUpto = 0;
    }
  }

  @Override
  public void finishDoc() throws IOException {
    // Since we don't know df for current term, we had to buffer
    // those skip data for each block, and when a new doc comes, 
    // write them to skip file.
    if (docBufferUpto == BLOCK_SIZE) {
      lastBlockDocID = lastDocID;
      if (posOut != null) {
        if (payOut != null) {
          lastBlockPayFP = payOut.getFilePointer();
        }
        lastBlockPosFP = posOut.getFilePointer();
        lastBlockPosBufferUpto = posBufferUpto;
        lastBlockPayloadByteUpto = payloadByteUpto;
      }
      // if (DEBUG) {
      //   System.out.println("  docBufferUpto="+docBufferUpto+" now get lastBlockDocID="+lastBlockDocID+" lastBlockPosFP=" + lastBlockPosFP + " lastBlockPosBufferUpto=" +  lastBlockPosBufferUpto + " lastBlockPayloadByteUpto=" + lastBlockPayloadByteUpto);
      // }
      docBufferUpto = 0;
    }
  }

  /** Called when we are done adding docs to this term */
  @Override
  public void finishTerm(BlockTermState _state) throws IOException {
    IntBlockTermState state = (IntBlockTermState) _state;
    assert state.docFreq > 0;

    // TODO: wasteful we are counting this (counting # docs
    // for this term) in two places?
    assert state.docFreq == docCount: state.docFreq + " vs " + docCount;

    // if (DEBUG) {
    //   System.out.println("FPW.finishTerm docFreq=" + state.docFreq);
    // }

    // if (DEBUG) {
    //   if (docBufferUpto > 0) {
    //     System.out.println("  write doc/freq vInt block (count=" + docBufferUpto + ") at fp=" + docOut.getFilePointer() + " docStartFP=" + docStartFP);
    //   }
    // }
    
    // docFreq == 1, don't write the single docid/freq to a separate file along with a pointer to it.
    final int singletonDocID;
    if (state.docFreq == 1) {
      // pulse the singleton docid into the term dictionary, freq is implicitly totalTermFreq
      singletonDocID = docDeltaBuffer[0];
    } else {
      singletonDocID = -1;
      // vInt encode the remaining doc deltas and freqs:
      for(int i=0;i<docBufferUpto;i++) {
        final int docDelta = docDeltaBuffer[i];
        final int freq = freqBuffer[i];
        if (!writeFreqs) {
          docOut.writeVInt(docDelta);
        } else if (freqBuffer[i] == 1) {
          docOut.writeVInt((docDelta<<1)|1);
        } else {
          docOut.writeVInt(docDelta<<1);
          docOut.writeVInt(freq);
        }
      }
    }

    final long lastPosBlockOffset;

    if (writePositions) {
      // if (DEBUG) {
      //   if (posBufferUpto > 0) {
      //     System.out.println("  write pos vInt block (count=" + posBufferUpto + ") at fp=" + posOut.getFilePointer() + " posStartFP=" + posStartFP + " hasPayloads=" + writePayloads + " hasOffsets=" + writeOffsets);
      //   }
      // }

      // totalTermFreq is just total number of positions(or payloads, or offsets)
      // associated with current term.
      assert state.totalTermFreq != -1;
      if (state.totalTermFreq > BLOCK_SIZE) {
        // record file offset for last pos in last block
        lastPosBlockOffset = posOut.getFilePointer() - posStartFP;
      } else {
        lastPosBlockOffset = -1;
      }
      if (posBufferUpto > 0) {       
        // TODO: should we send offsets/payloads to
        // .pay...?  seems wasteful (have to store extra
        // vLong for low (< BLOCK_SIZE) DF terms = vast vast
        // majority)

        // vInt encode the remaining positions/payloads/offsets:
        int lastPayloadLength = -1;  // force first payload length to be written
        int lastOffsetLength = -1;   // force first offset length to be written
        int payloadBytesReadUpto = 0;
        for(int i=0;i<posBufferUpto;i++) {
          final int posDelta = posDeltaBuffer[i];
          if (writePayloads) {
            final int payloadLength = payloadLengthBuffer[i];
            if (payloadLength != lastPayloadLength) {
              lastPayloadLength = payloadLength;
              posOut.writeVInt((posDelta<<1)|1);
              posOut.writeVInt(payloadLength);
            } else {
              posOut.writeVInt(posDelta<<1);
            }

            // if (DEBUG) {
            //   System.out.println("        i=" + i + " payloadLen=" + payloadLength);
            // }

            if (payloadLength != 0) {
              // if (DEBUG) {
              //   System.out.println("          write payload @ pos.fp=" + posOut.getFilePointer());
              // }
              posOut.writeBytes(payloadBytes, payloadBytesReadUpto, payloadLength);
              payloadBytesReadUpto += payloadLength;
            }
          } else {
            posOut.writeVInt(posDelta);
          }

          if (writeOffsets) {
            // if (DEBUG) {
            //   System.out.println("          write offset @ pos.fp=" + posOut.getFilePointer());
            // }
            int delta = offsetStartDeltaBuffer[i];
            int length = offsetLengthBuffer[i];
            if (length == lastOffsetLength) {
              posOut.writeVInt(delta << 1);
            } else {
              posOut.writeVInt(delta << 1 | 1);
              posOut.writeVInt(length);
              lastOffsetLength = length;
            }
          }
        }

        if (writePayloads) {
          assert payloadBytesReadUpto == payloadByteUpto;
          payloadByteUpto = 0;
        }
      }
      // if (DEBUG) {
      //   System.out.println("  totalTermFreq=" + state.totalTermFreq + " lastPosBlockOffset=" + lastPosBlockOffset);
      // }
    } else {
      lastPosBlockOffset = -1;
    }

    long skipOffset;
    if (docCount > BLOCK_SIZE) {
      skipOffset = skipWriter.writeSkip(docOut) - docStartFP;
      
      // if (DEBUG) {
      //   System.out.println("skip packet " + (docOut.getFilePointer() - (docStartFP + skipOffset)) + " bytes");
      // }
    } else {
      skipOffset = -1;
      // if (DEBUG) {
      //   System.out.println("  no skip: docCount=" + docCount);
      // }
    }
    // if (DEBUG) {
    //   System.out.println("  payStartFP=" + payStartFP);
    // }
    state.docStartFP = docStartFP;
    state.posStartFP = posStartFP;
    state.payStartFP = payStartFP;
    state.singletonDocID = singletonDocID;
    state.skipOffset = skipOffset;
    state.lastPosBlockOffset = lastPosBlockOffset;
    docBufferUpto = 0;
    posBufferUpto = 0;
    lastDocID = 0;
    docCount = 0;
  }
  
  @Override
  public void encodeTerm(long[] longs, DataOutput out, FieldInfo fieldInfo, BlockTermState _state, boolean absolute) throws IOException {
    IntBlockTermState state = (IntBlockTermState)_state;
    if (absolute) {
      lastState = emptyState;
    }
    longs[0] = state.docStartFP - lastState.docStartFP;
    if (writePositions) {
      longs[1] = state.posStartFP - lastState.posStartFP;
      if (writePayloads || writeOffsets) {
        longs[2] = state.payStartFP - lastState.payStartFP;
      }
    }
    if (state.singletonDocID != -1) {
      out.writeVInt(state.singletonDocID);
    }
    if (writePositions) {
      if (state.lastPosBlockOffset != -1) {
        out.writeVLong(state.lastPosBlockOffset);
      }
    }
    if (state.skipOffset != -1) {
      out.writeVLong(state.skipOffset);
    }
    lastState = state;
  }

  @Override
  public void close() throws IOException {
    // TODO: add a finish() at least to PushBase? DV too...?
    boolean success = false;
    try {
      if (docOut != null) {
        CodecUtil.writeFooter(docOut);
      }
      if (posOut != null) {
        CodecUtil.writeFooter(posOut);
      }
      if (payOut != null) {
        CodecUtil.writeFooter(payOut);
      }
      success = true;
    } finally {
      if (success) {
        IOUtils.close(docOut, posOut, payOut);
      } else {
        IOUtils.closeWhileHandlingException(docOut, posOut, payOut);
      }
      docOut = posOut = payOut = null;
    }
  }
}
