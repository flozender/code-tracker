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

import org.apache.lucene.codecs.Codec; // javadocs
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.LiveDocsFormat; // javadocs
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.BufferedIndexInput;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.DataOutput; // javadocs
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.StringHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Class for accessing a compound stream.
 * This class implements a directory, but is limited to only read operations.
 * Directory methods that would normally modify data throw an exception.
 * <p>
 * All files belonging to a segment have the same name with varying extensions.
 * The extensions correspond to the different file formats used by the {@link Codec}. 
 * When using the Compound File format these files are collapsed into a 
 * single <tt>.cfs</tt> file (except for the {@link LiveDocsFormat}, with a 
 * corresponding <tt>.cfe</tt> file indexing its sub-files.
 * <p>
 * Files:
 * <ul>
 *    <li><tt>.cfs</tt>: An optional "virtual" file consisting of all the other 
 *    index files for systems that frequently run out of file handles.
 *    <li><tt>.cfe</tt>: The "virtual" compound file's entry table holding all 
 *    entries in the corresponding .cfs file.
 * </ul>
 * <p>Description:</p>
 * <ul>
 *   <li>Compound (.cfs) --&gt; Header, FileData <sup>FileCount</sup>, Footer</li>
 *   <li>Compound Entry Table (.cfe) --&gt; Header, FileCount, &lt;FileName,
 *       DataOffset, DataLength&gt; <sup>FileCount</sup></li>
 *   <li>Header --&gt; {@link CodecUtil#writeSegmentHeader SegmentHeader}</li>
 *   <li>FileCount --&gt; {@link DataOutput#writeVInt VInt}</li>
 *   <li>DataOffset,DataLength,Checksum --&gt; {@link DataOutput#writeLong UInt64}</li>
 *   <li>FileName --&gt; {@link DataOutput#writeString String}</li>
 *   <li>FileData --&gt; raw file data</li>
 *   <li>Footer --&gt; {@link CodecUtil#writeFooter CodecFooter}</li>
 * </ul>
 * <p>Notes:</p>
 * <ul>
 *   <li>FileCount indicates how many files are contained in this compound file. 
 *       The entry table that follows has that many entries. 
 *   <li>Each directory entry contains a long pointer to the start of this file's data
 *       section, the files length, and a String with that file's name.
 * </ul>
 * 
 * @lucene.experimental
 */
final class CompoundFileDirectory extends BaseDirectory {
  
  /** Offset/Length for a slice inside of a compound file */
  public static final class FileEntry {
    long offset;
    long length;
  }
  
  private final Directory directory;
  private final String fileName;
  protected final int readBufferSize;  
  private final Map<String,FileEntry> entries;
  private final boolean openForWrite;
  private static final Map<String,FileEntry> SENTINEL = Collections.emptyMap();
  private final CompoundFileWriter writer;
  private final IndexInput handle;
  private int version;
  private final byte[] segmentID;
  
  /**
   * Create a new CompoundFileDirectory.
   */
  public CompoundFileDirectory(byte[] segmentID, Directory directory, String fileName, IOContext context, boolean openForWrite) throws IOException {
    this.directory = directory;
    this.segmentID = segmentID;
    this.fileName = fileName;
    this.readBufferSize = BufferedIndexInput.bufferSize(context);
    this.isOpen = false;
    this.openForWrite = openForWrite;
    if (!openForWrite) {
      boolean success = false;
      handle = directory.openInput(fileName, context);
      try {
        this.entries = readEntries(directory, fileName);
        if (version >= CompoundFileWriter.VERSION_CHECKSUM) {
          if (version >= CompoundFileWriter.VERSION_SEGMENTHEADER) {
            // nocommit: remove this null "hack", its because old rw test codecs cant properly impersonate
            if (segmentID == null) {
              CodecUtil.checkHeader(handle, CompoundFileWriter.DATA_CODEC, version, version);
              handle.skipBytes(StringHelper.ID_LENGTH);
            } else {
              CodecUtil.checkSegmentHeader(handle, CompoundFileWriter.DATA_CODEC, version, version, segmentID, "");
            }
          } else {
            CodecUtil.checkHeader(handle, CompoundFileWriter.DATA_CODEC, version, version);
          }
          // NOTE: data file is too costly to verify checksum against all the bytes on open,
          // but for now we at least verify proper structure of the checksum footer: which looks
          // for FOOTER_MAGIC + algorithmID. This is cheap and can detect some forms of corruption
          // such as file truncation.
          CodecUtil.retrieveChecksum(handle);
        }
        success = true;
      } finally {
        if (!success) {
          IOUtils.closeWhileHandlingException(handle);
        }
      }
      this.isOpen = true;
      writer = null;
    } else {
      assert !(directory instanceof CompoundFileDirectory) : "compound file inside of compound file: " + fileName;
      this.entries = SENTINEL;
      this.isOpen = true;
      writer = new CompoundFileWriter(segmentID, directory, fileName);
      handle = null;
    }
  }

  /** Helper method that reads CFS entries from an input stream */
  private final Map<String, FileEntry> readEntries(Directory dir, String name) throws IOException {
    ChecksumIndexInput entriesStream = null;
    Map<String,FileEntry> mapping = null;
    boolean success = false;
    try {
      final String entriesFileName = IndexFileNames.segmentFileName(
                                            IndexFileNames.stripExtension(name), "",
                                             IndexFileNames.COMPOUND_FILE_ENTRIES_EXTENSION);
      entriesStream = dir.openChecksumInput(entriesFileName, IOContext.READONCE);
      version = CodecUtil.checkHeader(entriesStream, CompoundFileWriter.ENTRY_CODEC, CompoundFileWriter.VERSION_START, CompoundFileWriter.VERSION_CURRENT);
      if (version >= CompoundFileWriter.VERSION_SEGMENTHEADER) {
        byte id[] = new byte[StringHelper.ID_LENGTH];
        entriesStream.readBytes(id, 0, id.length);
        // nocommit: remove this null "hack", its because old rw test codecs cant properly impersonate
        if (segmentID != null && !Arrays.equals(id, segmentID)) {
          throw new CorruptIndexException("file mismatch, expected segment id=" + StringHelper.idToString(segmentID) 
                                                                     + ", got=" + StringHelper.idToString(id), entriesStream);
        }
        byte suffixLength = entriesStream.readByte();
        if (suffixLength != 0) {
          throw new CorruptIndexException("unexpected segment suffix, expected zero-length, got=" + (suffixLength & 0xFF), entriesStream);
        }
      }
      final int numEntries = entriesStream.readVInt();
      mapping = new HashMap<>(numEntries);
      for (int i = 0; i < numEntries; i++) {
        final FileEntry fileEntry = new FileEntry();
        final String id = entriesStream.readString();
        FileEntry previous = mapping.put(id, fileEntry);
        if (previous != null) {
          throw new CorruptIndexException("Duplicate cfs entry id=" + id + " in CFS ", entriesStream);
        }
        fileEntry.offset = entriesStream.readLong();
        fileEntry.length = entriesStream.readLong();
      }
      if (version >= CompoundFileWriter.VERSION_CHECKSUM) {
        CodecUtil.checkFooter(entriesStream);
      } else {
        CodecUtil.checkEOF(entriesStream);
      }
      success = true;
    } finally {
      if (success) {
        IOUtils.close(entriesStream);
      } else {
        IOUtils.closeWhileHandlingException(entriesStream);
      }
    }
    return mapping;
  }
  
  public Directory getDirectory() {
    return directory;
  }
  
  public String getName() {
    return fileName;
  }
  
  @Override
  public synchronized void close() throws IOException {
    if (!isOpen) {
      // allow double close - usually to be consistent with other closeables
      return; // already closed
     }
    isOpen = false;
    if (writer != null) {
      assert openForWrite;
      writer.close();
    } else {
      IOUtils.close(handle);
    }
  }
  
  @Override
  public synchronized IndexInput openInput(String name, IOContext context) throws IOException {
    ensureOpen();
    assert !openForWrite;
    final String id = IndexFileNames.stripSegmentName(name);
    final FileEntry entry = entries.get(id);
    if (entry == null) {
      throw new FileNotFoundException("No sub-file with id " + id + " found (fileName=" + name + " files: " + entries.keySet() + ")");
    }
    return handle.slice(name, entry.offset, entry.length);
  }
  
  /** Returns an array of strings, one for each file in the directory. */
  @Override
  public String[] listAll() {
    ensureOpen();
    String[] res;
    if (writer != null) {
      res = writer.listAll(); 
    } else {
      res = entries.keySet().toArray(new String[entries.size()]);
      // Add the segment name
      String seg = IndexFileNames.parseSegmentName(fileName);
      for (int i = 0; i < res.length; i++) {
        res[i] = seg + res[i];
      }
    }
    return res;
  }
  
  /** Not implemented
   * @throws UnsupportedOperationException always: not supported by CFS */
  @Override
  public void deleteFile(String name) {
    throw new UnsupportedOperationException();
  }
  
  /** Not implemented
   * @throws UnsupportedOperationException always: not supported by CFS */
  public void renameFile(String from, String to) {
    throw new UnsupportedOperationException();
  }
  
  /** Returns the length of a file in the directory.
   * @throws IOException if the file does not exist */
  @Override
  public long fileLength(String name) throws IOException {
    ensureOpen();
    if (this.writer != null) {
      return writer.fileLength(name);
    }
    FileEntry e = entries.get(IndexFileNames.stripSegmentName(name));
    if (e == null)
      throw new FileNotFoundException(name);
    return e.length;
  }
  
  @Override
  public IndexOutput createOutput(String name, IOContext context) throws IOException {
    ensureOpen();
    return writer.createOutput(name, context);
  }
  
  @Override
  public void sync(Collection<String> names) {
    throw new UnsupportedOperationException();
  }
  
  /** Not implemented
   * @throws UnsupportedOperationException always: not supported by CFS */
  @Override
  public Lock makeLock(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return "CompoundFileDirectory(file=\"" + fileName + "\" in dir=" + directory + ")";
  }
}
