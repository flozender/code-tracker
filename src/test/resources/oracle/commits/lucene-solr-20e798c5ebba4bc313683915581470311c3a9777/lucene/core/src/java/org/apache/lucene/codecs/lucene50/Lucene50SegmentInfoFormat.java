package org.apache.lucene.codecs.lucene50;

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
import java.util.Map;
import java.util.Set;

import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.SegmentInfoFormat;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.IndexWriter; // javadocs
import org.apache.lucene.index.SegmentInfo; // javadocs
import org.apache.lucene.index.SegmentInfos; // javadocs
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.DataOutput; // javadocs
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.StringHelper;
import org.apache.lucene.util.Version;

/**
 * Lucene 5.0 Segment info format.
 * <p>
 * Files:
 * <ul>
 *   <li><tt>.si</tt>: Header, SegVersion, SegSize, IsCompoundFile, Diagnostics, Files, Footer
 * </ul>
 * </p>
 * Data types:
 * <p>
 * <ul>
 *   <li>Header --&gt; {@link CodecUtil#writeSegmentHeader SegmentHeader}</li>
 *   <li>SegSize --&gt; {@link DataOutput#writeInt Int32}</li>
 *   <li>SegVersion --&gt; {@link DataOutput#writeString String}</li>
 *   <li>Files --&gt; {@link DataOutput#writeStringSet Set&lt;String&gt;}</li>
 *   <li>Diagnostics --&gt; {@link DataOutput#writeStringStringMap Map&lt;String,String&gt;}</li>
 *   <li>IsCompoundFile --&gt; {@link DataOutput#writeByte Int8}</li>
 *   <li>Footer --&gt; {@link CodecUtil#writeFooter CodecFooter}</li>
 * </ul>
 * </p>
 * Field Descriptions:
 * <p>
 * <ul>
 *   <li>SegVersion is the code version that created the segment.</li>
 *   <li>SegSize is the number of documents contained in the segment index.</li>
 *   <li>IsCompoundFile records whether the segment is written as a compound file or
 *       not. If this is -1, the segment is not a compound file. If it is 1, the segment
 *       is a compound file.</li>
 *   <li>The Diagnostics Map is privately written by {@link IndexWriter}, as a debugging aid,
 *       for each segment it creates. It includes metadata like the current Lucene
 *       version, OS, Java version, why the segment was created (merge, flush,
 *       addIndexes), etc.</li>
 *   <li>Files is a list of files referred to by this segment.</li>
 * </ul>
 * </p>
 * 
 * @see SegmentInfos
 * @lucene.experimental
 */
public class Lucene50SegmentInfoFormat extends SegmentInfoFormat {

  /** Sole constructor. */
  public Lucene50SegmentInfoFormat() {
  }
  
  @Override
  public SegmentInfo read(Directory dir, String segment, IOContext context) throws IOException {
    final String fileName = IndexFileNames.segmentFileName(segment, "", Lucene50SegmentInfoFormat.SI_EXTENSION);
    try (ChecksumIndexInput input = dir.openChecksumInput(fileName, context)) {
      Throwable priorE = null;
      SegmentInfo si = null;
      try {
        CodecUtil.checkHeader(input, Lucene50SegmentInfoFormat.CODEC_NAME,
                                     Lucene50SegmentInfoFormat.VERSION_START,
                                     Lucene50SegmentInfoFormat.VERSION_CURRENT);
        byte id[] = new byte[StringHelper.ID_LENGTH];
        input.readBytes(id, 0, id.length);
        String suffix = input.readString();
        if (!suffix.isEmpty()) {
          throw new CorruptIndexException("invalid codec header: got unexpected suffix: " + suffix, input);
        }
        final Version version = Version.fromBits(input.readInt(), input.readInt(), input.readInt());
        
        final int docCount = input.readInt();
        if (docCount < 0) {
          throw new CorruptIndexException("invalid docCount: " + docCount, input);
        }
        final boolean isCompoundFile = input.readByte() == SegmentInfo.YES;
        final Map<String,String> diagnostics = input.readStringStringMap();
        final Set<String> files = input.readStringSet();
        
        si = new SegmentInfo(dir, version, segment, docCount, isCompoundFile, null, diagnostics, id);
        si.setFiles(files);
      } catch (Throwable exception) {
        priorE = exception;
      } finally {
        CodecUtil.checkFooter(input, priorE);
      }
      return si;
    }
  }

  @Override
  public void write(Directory dir, SegmentInfo si, IOContext ioContext) throws IOException {
    final String fileName = IndexFileNames.segmentFileName(si.name, "", Lucene50SegmentInfoFormat.SI_EXTENSION);
    si.addFile(fileName);

    boolean success = false;
    try (IndexOutput output = dir.createOutput(fileName, ioContext)) {
      // NOTE: we encode ID in the segment header, for format consistency with all other per-segment files
      CodecUtil.writeSegmentHeader(output, 
                                   Lucene50SegmentInfoFormat.CODEC_NAME, 
                                   Lucene50SegmentInfoFormat.VERSION_CURRENT,
                                   si.getId(),
                                   "");
      Version version = si.getVersion();
      if (version.major < 5) {
        throw new IllegalArgumentException("invalid major version: should be >= 5 but got: " + version.major + " segment=" + si);
      }
      // Write the Lucene version that created this segment, since 3.1
      output.writeInt(version.major);
      output.writeInt(version.minor);
      output.writeInt(version.bugfix);
      assert version.prerelease == 0;
      output.writeInt(si.getDocCount());

      output.writeByte((byte) (si.getUseCompoundFile() ? SegmentInfo.YES : SegmentInfo.NO));
      output.writeStringStringMap(si.getDiagnostics());
      Set<String> files = si.files();
      for (String file : files) {
        if (!IndexFileNames.parseSegmentName(file).equals(si.name)) {
          throw new IllegalArgumentException("invalid files: expected segment=" + si.name + ", got=" + files);
        }
      }
      output.writeStringSet(files);
      CodecUtil.writeFooter(output);
      success = true;
    } finally {
      if (!success) {
        // TODO: are we doing this outside of the tracking wrapper? why must SIWriter cleanup like this?
        IOUtils.deleteFilesIgnoringExceptions(si.dir, fileName);
      }
    }
  }

  /** File extension used to store {@link SegmentInfo}. */
  public final static String SI_EXTENSION = "si";
  static final String CODEC_NAME = "Lucene50SegmentInfo";
  static final int VERSION_START = 0;
  static final int VERSION_CURRENT = VERSION_START;
}
