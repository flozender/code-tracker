package org.apache.lucene.store;

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

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.TestUtil;

/**
 * Tests MMapDirectory's MultiMMapIndexInput
 * <p>
 * Because Java's ByteBuffer uses an int to address the
 * values, it's necessary to access a file >
 * Integer.MAX_VALUE in size using multiple byte buffers.
 */
public class TestMultiMMap extends BaseDirectoryTestCase {
  File workDir;

  @Override
  protected Directory getDirectory(File path) throws IOException {
    return new MMapDirectory(path, null, 1<<TestUtil.nextInt(random(), 10, 28));
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    assumeTrue("test requires a jre that supports unmapping", MMapDirectory.UNMAP_SUPPORTED);
  }
  
  public void testCloneSafety() throws Exception {
    MMapDirectory mmapDir = new MMapDirectory(createTempDir("testCloneSafety"));
    IndexOutput io = mmapDir.createOutput("bytes", newIOContext(random()));
    io.writeVInt(5);
    io.close();
    IndexInput one = mmapDir.openInput("bytes", IOContext.DEFAULT);
    IndexInput two = one.clone();
    IndexInput three = two.clone(); // clone of clone
    one.close();
    try {
      one.readVInt();
      fail("Must throw AlreadyClosedException");
    } catch (AlreadyClosedException ignore) {
      // pass
    }
    try {
      two.readVInt();
      fail("Must throw AlreadyClosedException");
    } catch (AlreadyClosedException ignore) {
      // pass
    }
    try {
      three.readVInt();
      fail("Must throw AlreadyClosedException");
    } catch (AlreadyClosedException ignore) {
      // pass
    }
    two.close();
    three.close();
    // test double close of master:
    one.close();
    mmapDir.close();
  }
  
  public void testCloneClose() throws Exception {
    MMapDirectory mmapDir = new MMapDirectory(createTempDir("testCloneClose"));
    IndexOutput io = mmapDir.createOutput("bytes", newIOContext(random()));
    io.writeVInt(5);
    io.close();
    IndexInput one = mmapDir.openInput("bytes", IOContext.DEFAULT);
    IndexInput two = one.clone();
    IndexInput three = two.clone(); // clone of clone
    two.close();
    assertEquals(5, one.readVInt());
    try {
      two.readVInt();
      fail("Must throw AlreadyClosedException");
    } catch (AlreadyClosedException ignore) {
      // pass
    }
    assertEquals(5, three.readVInt());
    one.close();
    three.close();
    mmapDir.close();
  }
  
  public void testCloneSliceSafety() throws Exception {
    MMapDirectory mmapDir = new MMapDirectory(createTempDir("testCloneSliceSafety"));
    IndexOutput io = mmapDir.createOutput("bytes", newIOContext(random()));
    io.writeInt(1);
    io.writeInt(2);
    io.close();
    IndexInput slicer = mmapDir.openInput("bytes", newIOContext(random()));
    IndexInput one = slicer.slice("first int", 0, 4);
    IndexInput two = slicer.slice("second int", 4, 4);
    IndexInput three = one.clone(); // clone of clone
    IndexInput four = two.clone(); // clone of clone
    slicer.close();
    try {
      one.readInt();
      fail("Must throw AlreadyClosedException");
    } catch (AlreadyClosedException ignore) {
      // pass
    }
    try {
      two.readInt();
      fail("Must throw AlreadyClosedException");
    } catch (AlreadyClosedException ignore) {
      // pass
    }
    try {
      three.readInt();
      fail("Must throw AlreadyClosedException");
    } catch (AlreadyClosedException ignore) {
      // pass
    }
    try {
      four.readInt();
      fail("Must throw AlreadyClosedException");
    } catch (AlreadyClosedException ignore) {
      // pass
    }
    one.close();
    two.close();
    three.close();
    four.close();
    // test double-close of slicer:
    slicer.close();
    mmapDir.close();
  }

  public void testCloneSliceClose() throws Exception {
    MMapDirectory mmapDir = new MMapDirectory(createTempDir("testCloneSliceClose"));
    IndexOutput io = mmapDir.createOutput("bytes", newIOContext(random()));
    io.writeInt(1);
    io.writeInt(2);
    io.close();
    IndexInput slicer = mmapDir.openInput("bytes", newIOContext(random()));
    IndexInput one = slicer.slice("first int", 0, 4);
    IndexInput two = slicer.slice("second int", 4, 4);
    one.close();
    try {
      one.readInt();
      fail("Must throw AlreadyClosedException");
    } catch (AlreadyClosedException ignore) {
      // pass
    }
    assertEquals(2, two.readInt());
    // reopen a new slice "one":
    one = slicer.slice("first int", 0, 4);
    assertEquals(1, one.readInt());
    one.close();
    two.close();
    slicer.close();
    mmapDir.close();
  }

  public void testSeekZero() throws Exception {
    for (int i = 0; i < 31; i++) {
      MMapDirectory mmapDir = new MMapDirectory(createTempDir("testSeekZero"), null, 1<<i);
      IndexOutput io = mmapDir.createOutput("zeroBytes", newIOContext(random()));
      io.close();
      IndexInput ii = mmapDir.openInput("zeroBytes", newIOContext(random()));
      ii.seek(0L);
      ii.close();
      mmapDir.close();
    }
  }
  
  public void testSeekSliceZero() throws Exception {
    for (int i = 0; i < 31; i++) {
      MMapDirectory mmapDir = new MMapDirectory(createTempDir("testSeekSliceZero"), null, 1<<i);
      IndexOutput io = mmapDir.createOutput("zeroBytes", newIOContext(random()));
      io.close();
      IndexInput slicer = mmapDir.openInput("zeroBytes", newIOContext(random()));
      IndexInput ii = slicer.slice("zero-length slice", 0, 0);
      ii.seek(0L);
      ii.close();
      slicer.close();
      mmapDir.close();
    }
  }
  
  public void testSeekEnd() throws Exception {
    for (int i = 0; i < 17; i++) {
      MMapDirectory mmapDir = new MMapDirectory(createTempDir("testSeekEnd"), null, 1<<i);
      IndexOutput io = mmapDir.createOutput("bytes", newIOContext(random()));
      byte bytes[] = new byte[1<<i];
      random().nextBytes(bytes);
      io.writeBytes(bytes, bytes.length);
      io.close();
      IndexInput ii = mmapDir.openInput("bytes", newIOContext(random()));
      byte actual[] = new byte[1<<i];
      ii.readBytes(actual, 0, actual.length);
      assertEquals(new BytesRef(bytes), new BytesRef(actual));
      ii.seek(1<<i);
      ii.close();
      mmapDir.close();
    }
  }
  
  public void testSeekSliceEnd() throws Exception {
    for (int i = 0; i < 17; i++) {
      MMapDirectory mmapDir = new MMapDirectory(createTempDir("testSeekSliceEnd"), null, 1<<i);
      IndexOutput io = mmapDir.createOutput("bytes", newIOContext(random()));
      byte bytes[] = new byte[1<<i];
      random().nextBytes(bytes);
      io.writeBytes(bytes, bytes.length);
      io.close();
      IndexInput slicer = mmapDir.openInput("bytes", newIOContext(random()));
      IndexInput ii = slicer.slice("full slice", 0, bytes.length);
      byte actual[] = new byte[1<<i];
      ii.readBytes(actual, 0, actual.length);
      assertEquals(new BytesRef(bytes), new BytesRef(actual));
      ii.seek(1<<i);
      ii.close();
      slicer.close();
      mmapDir.close();
    }
  }
  
  public void testSeeking() throws Exception {
    for (int i = 0; i < 10; i++) {
      MMapDirectory mmapDir = new MMapDirectory(createTempDir("testSeeking"), null, 1<<i);
      IndexOutput io = mmapDir.createOutput("bytes", newIOContext(random()));
      byte bytes[] = new byte[1<<(i+1)]; // make sure we switch buffers
      random().nextBytes(bytes);
      io.writeBytes(bytes, bytes.length);
      io.close();
      IndexInput ii = mmapDir.openInput("bytes", newIOContext(random()));
      byte actual[] = new byte[1<<(i+1)]; // first read all bytes
      ii.readBytes(actual, 0, actual.length);
      assertEquals(new BytesRef(bytes), new BytesRef(actual));
      for (int sliceStart = 0; sliceStart < bytes.length; sliceStart++) {
        for (int sliceLength = 0; sliceLength < bytes.length - sliceStart; sliceLength++) {
          byte slice[] = new byte[sliceLength];
          ii.seek(sliceStart);
          ii.readBytes(slice, 0, slice.length);
          assertEquals(new BytesRef(bytes, sliceStart, sliceLength), new BytesRef(slice));
        }
      }
      ii.close();
      mmapDir.close();
    }
  }
  
  // note instead of seeking to offset and reading length, this opens slices at the 
  // the various offset+length and just does readBytes.
  public void testSlicedSeeking() throws Exception {
    for (int i = 0; i < 10; i++) {
      MMapDirectory mmapDir = new MMapDirectory(createTempDir("testSlicedSeeking"), null, 1<<i);
      IndexOutput io = mmapDir.createOutput("bytes", newIOContext(random()));
      byte bytes[] = new byte[1<<(i+1)]; // make sure we switch buffers
      random().nextBytes(bytes);
      io.writeBytes(bytes, bytes.length);
      io.close();
      IndexInput ii = mmapDir.openInput("bytes", newIOContext(random()));
      byte actual[] = new byte[1<<(i+1)]; // first read all bytes
      ii.readBytes(actual, 0, actual.length);
      ii.close();
      assertEquals(new BytesRef(bytes), new BytesRef(actual));
      IndexInput slicer = mmapDir.openInput("bytes", newIOContext(random()));
      for (int sliceStart = 0; sliceStart < bytes.length; sliceStart++) {
        for (int sliceLength = 0; sliceLength < bytes.length - sliceStart; sliceLength++) {
          assertSlice(bytes, slicer, 0, sliceStart, sliceLength);
        }
      }
      slicer.close();
      mmapDir.close();
    }
  }

  public void testSliceOfSlice() throws Exception {
    for (int i = 0; i < 10; i++) {
      MMapDirectory mmapDir = new MMapDirectory(createTempDir("testSliceOfSlice"), null, 1<<i);
      IndexOutput io = mmapDir.createOutput("bytes", newIOContext(random()));
      byte bytes[] = new byte[1<<(i+1)]; // make sure we switch buffers
      random().nextBytes(bytes);
      io.writeBytes(bytes, bytes.length);
      io.close();
      IndexInput ii = mmapDir.openInput("bytes", newIOContext(random()));
      byte actual[] = new byte[1<<(i+1)]; // first read all bytes
      ii.readBytes(actual, 0, actual.length);
      ii.close();
      assertEquals(new BytesRef(bytes), new BytesRef(actual));
      IndexInput outerSlicer = mmapDir.openInput("bytes", newIOContext(random()));
      final int outerSliceStart = random().nextInt(bytes.length / 2);
      final int outerSliceLength = random().nextInt(bytes.length - outerSliceStart);
      IndexInput innerSlicer = outerSlicer.slice("parentBytesSlice", outerSliceStart, outerSliceLength);
      for (int sliceStart = 0; sliceStart < outerSliceLength; sliceStart++) {
        for (int sliceLength = 0; sliceLength < outerSliceLength - sliceStart; sliceLength++) {
          assertSlice(bytes, innerSlicer, outerSliceStart, sliceStart, sliceLength);
        }
      }
      innerSlicer.close();
      outerSlicer.close();
      mmapDir.close();
    }    
  }
  
  private void assertSlice(byte[] bytes, IndexInput slicer, int outerSliceStart, int sliceStart, int sliceLength) throws IOException {
    byte slice[] = new byte[sliceLength];
    IndexInput input = slicer.slice("bytesSlice", sliceStart, slice.length);
    input.readBytes(slice, 0, slice.length);
    input.close();
    assertEquals(new BytesRef(bytes, outerSliceStart + sliceStart, sliceLength), new BytesRef(slice));
  }
  
  public void testRandomChunkSizes() throws Exception {
    int num = atLeast(10);
    for (int i = 0; i < num; i++)
      assertChunking(random(), TestUtil.nextInt(random(), 20, 100));
  }
  
  private void assertChunking(Random random, int chunkSize) throws Exception {
    File path = createTempDir("mmap" + chunkSize);
    MMapDirectory mmapDir = new MMapDirectory(path, null, chunkSize);
    // we will map a lot, try to turn on the unmap hack
    if (MMapDirectory.UNMAP_SUPPORTED)
      mmapDir.setUseUnmap(true);
    MockDirectoryWrapper dir = new MockDirectoryWrapper(random, mmapDir);
    RandomIndexWriter writer = new RandomIndexWriter(random, dir, newIndexWriterConfig(new MockAnalyzer(random)).setMergePolicy(newLogMergePolicy()));
    Document doc = new Document();
    Field docid = newStringField("docid", "0", Field.Store.YES);
    Field junk = newStringField("junk", "", Field.Store.YES);
    doc.add(docid);
    doc.add(junk);
    
    int numDocs = 100;
    for (int i = 0; i < numDocs; i++) {
      docid.setStringValue("" + i);
      junk.setStringValue(TestUtil.randomUnicodeString(random));
      writer.addDocument(doc);
    }
    IndexReader reader = writer.getReader();
    writer.shutdown();
    
    int numAsserts = atLeast(100);
    for (int i = 0; i < numAsserts; i++) {
      int docID = random.nextInt(numDocs);
      assertEquals("" + docID, reader.document(docID).get("docid"));
    }
    reader.close();
    writer.close();
    dir.close();
  }
  
  public void testImplementations() throws Exception {
    for (int i = 2; i < 12; i++) {
      final int chunkSize = 1<<i;
      MMapDirectory mmapDir = new MMapDirectory(createTempDir("testImplementations"), null, chunkSize);
      IndexOutput io = mmapDir.createOutput("bytes", newIOContext(random()));
      int size = random().nextInt(chunkSize * 2) + 3; // add some buffer of 3 for slice tests
      byte bytes[] = new byte[size];
      random().nextBytes(bytes);
      io.writeBytes(bytes, bytes.length);
      io.close();
      IndexInput ii = mmapDir.openInput("bytes", newIOContext(random()));
      byte actual[] = new byte[size]; // first read all bytes
      ii.readBytes(actual, 0, actual.length);
      assertEquals(new BytesRef(bytes), new BytesRef(actual));
      // reinit:
      ii.seek(0L);
      
      // check impl (we must check size < chunksize: currently, if size==chunkSize, we get 2 buffers, the second one empty:
      assertTrue((size < chunkSize) ? (ii instanceof ByteBufferIndexInput.SingleBufferImpl) : (ii instanceof ByteBufferIndexInput.DefaultImpl));
      
      // clone tests:
      assertSame(ii.getClass(), ii.clone().getClass());
      
      // slice test (offset 0)
      int sliceSize = random().nextInt(size);
      IndexInput slice = ii.slice("slice", 0, sliceSize);
      assertTrue((sliceSize < chunkSize) ? (slice instanceof ByteBufferIndexInput.SingleBufferImpl) : (slice instanceof ByteBufferIndexInput.DefaultImpl));

      // slice test (offset > 0 )
      int offset = random().nextInt(size - 1) + 1;
      sliceSize = random().nextInt(size - offset + 1);
      slice = ii.slice("slice", offset, sliceSize);
      //System.out.println(offset + "/" + sliceSize + " chunkSize=" + chunkSize + " " + slice.getClass());
      if (offset % chunkSize + sliceSize < chunkSize) {
        assertTrue(slice instanceof ByteBufferIndexInput.SingleBufferImpl);
      } else if (offset % chunkSize == 0) {
        assertTrue(slice instanceof ByteBufferIndexInput.DefaultImpl);
      } else {
        assertTrue(slice instanceof ByteBufferIndexInput.WithOffsetImpl);
      }

      ii.close();
      mmapDir.close();
    }    
  }
}
