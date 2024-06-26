package org.apache.lucene.index;

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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.store.CompoundFileDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.MockDirectoryWrapper;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.StringHelper;
import org.apache.lucene.util.TestUtil;

import java.io.IOException;
import java.nio.file.Path;

public class TestCompoundFile extends LuceneTestCase {
  private Directory dir;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    Path file = createTempDir("testIndex");
    dir = newFSDirectory(file);
  }
  
  @Override
  public void tearDown() throws Exception {
    dir.close();
    super.tearDown();
  }
  
  /** Creates a file of the specified size with random data. */
  private void createRandomFile(Directory dir, String name, int size) throws IOException {
    IndexOutput os = dir.createOutput(name, newIOContext(random()));
    for (int i=0; i<size; i++) {
      byte b = (byte) (Math.random() * 256);
      os.writeByte(b);
    }
    os.close();
  }
  
  /** Creates a file of the specified size with sequential data. The first
   *  byte is written as the start byte provided. All subsequent bytes are
   *  computed as start + offset where offset is the number of the byte.
   */
  static void createSequenceFile(Directory dir, String name, byte start, int size) throws IOException {
    IndexOutput os = dir.createOutput(name, newIOContext(random()));
    for (int i=0; i < size; i++) {
      os.writeByte(start);
      start ++;
    }
    os.close();
  }
  
  static void assertSameStreams(String msg, IndexInput expected, IndexInput test) throws IOException {
    assertNotNull(msg + " null expected", expected);
    assertNotNull(msg + " null test", test);
    assertEquals(msg + " length", expected.length(), test.length());
    assertEquals(msg + " position", expected.getFilePointer(), test.getFilePointer());
    
    byte expectedBuffer[] = new byte[512];
    byte testBuffer[] = new byte[expectedBuffer.length];
    
    long remainder = expected.length() - expected.getFilePointer();
    while (remainder > 0) {
      int readLen = (int) Math.min(remainder, expectedBuffer.length);
      expected.readBytes(expectedBuffer, 0, readLen);
      test.readBytes(testBuffer, 0, readLen);
      assertEqualArrays(msg + ", remainder " + remainder, expectedBuffer, testBuffer, 0, readLen);
      remainder -= readLen;
    }
  }
  
  static void assertSameStreams(String msg, IndexInput expected, IndexInput actual, long seekTo) throws IOException {
    if (seekTo >= 0 && seekTo < expected.length()) {
      expected.seek(seekTo);
      actual.seek(seekTo);
      assertSameStreams(msg + ", seek(mid)", expected, actual);
    }
  }
  
  static void assertSameSeekBehavior(String msg, IndexInput expected, IndexInput actual) throws IOException {
    // seek to 0
    long point = 0;
    assertSameStreams(msg + ", seek(0)", expected, actual, point);
    
    // seek to middle
    point = expected.length() / 2l;
    assertSameStreams(msg + ", seek(mid)", expected, actual, point);
    
    // seek to end - 2
    point = expected.length() - 2;
    assertSameStreams(msg + ", seek(end-2)", expected, actual, point);
    
    // seek to end - 1
    point = expected.length() - 1;
    assertSameStreams(msg + ", seek(end-1)", expected, actual, point);
    
    // seek to the end
    point = expected.length();
    assertSameStreams(msg + ", seek(end)", expected, actual, point);
    
    // seek past end
    point = expected.length() + 1;
    assertSameStreams(msg + ", seek(end+1)", expected, actual, point);
  }
  
  
  static void assertEqualArrays(String msg, byte[] expected, byte[] test, int start, int len) {
    assertNotNull(msg + " null expected", expected);
    assertNotNull(msg + " null test", test);
    
    for (int i=start; i<len; i++) {
      assertEquals(msg + " " + i, expected[i], test[i]);
    }
  }
  
  
  // ===========================================================
  //  Tests of the basic CompoundFile functionality
  // ===========================================================
  
  
  /** 
   * This test creates compound file based on a single file.
   * Files of different sizes are tested: 0, 1, 10, 100 bytes.
   */
  public void testSingleFile() throws IOException {
    int data[] = new int[] { 0, 1, 10, 100 };
    for (int i=0; i<data.length; i++) {
      byte id[] = StringHelper.randomId();
      String name = "t" + data[i];
      createSequenceFile(dir, name, (byte) 0, data[i]);
      CompoundFileDirectory csw = new CompoundFileDirectory(id, dir, name + ".cfs", newIOContext(random()), true);
      dir.copy(csw, name, name, newIOContext(random()));
      csw.close();
      
      CompoundFileDirectory csr = new CompoundFileDirectory(id, dir, name + ".cfs", newIOContext(random()), false);
      IndexInput expected = dir.openInput(name, newIOContext(random()));
      IndexInput actual = csr.openInput(name, newIOContext(random()));
      assertSameStreams(name, expected, actual);
      assertSameSeekBehavior(name, expected, actual);
      expected.close();
      actual.close();
      csr.close();
    }
  }
  
  /** 
   * This test creates compound file based on two files.
   */
  public void testTwoFiles() throws IOException {
    createSequenceFile(dir, "d1", (byte) 0, 15);
    createSequenceFile(dir, "d2", (byte) 0, 114);
    
    byte id[] = StringHelper.randomId();
    CompoundFileDirectory csw = new CompoundFileDirectory(id, dir, "d.cfs", newIOContext(random()), true);
    dir.copy(csw, "d1", "d1", newIOContext(random()));
    dir.copy(csw, "d2", "d2", newIOContext(random()));
    csw.close();
    
    CompoundFileDirectory csr = new CompoundFileDirectory(id, dir, "d.cfs", newIOContext(random()), false);
    IndexInput expected = dir.openInput("d1", newIOContext(random()));
    IndexInput actual = csr.openInput("d1", newIOContext(random()));
    assertSameStreams("d1", expected, actual);
    assertSameSeekBehavior("d1", expected, actual);
    expected.close();
    actual.close();
    
    expected = dir.openInput("d2", newIOContext(random()));
    actual = csr.openInput("d2", newIOContext(random()));
    assertSameStreams("d2", expected, actual);
    assertSameSeekBehavior("d2", expected, actual);
    expected.close();
    actual.close();
    csr.close();
  }
  
  /** 
   * This test creates a compound file based on a large number of files of
   * various length. The file content is generated randomly. The sizes range
   * from 0 to 1Mb. Some of the sizes are selected to test the buffering
   * logic in the file reading code. For this the chunk variable is set to
   * the length of the buffer used internally by the compound file logic.
   */
  public void testRandomFiles() throws IOException {
    // Setup the test segment
    String segment = "test";
    int chunk = 1024; // internal buffer size used by the stream
    createRandomFile(dir, segment + ".zero", 0);
    createRandomFile(dir, segment + ".one", 1);
    createRandomFile(dir, segment + ".ten", 10);
    createRandomFile(dir, segment + ".hundred", 100);
    createRandomFile(dir, segment + ".big1", chunk);
    createRandomFile(dir, segment + ".big2", chunk - 1);
    createRandomFile(dir, segment + ".big3", chunk + 1);
    createRandomFile(dir, segment + ".big4", 3 * chunk);
    createRandomFile(dir, segment + ".big5", 3 * chunk - 1);
    createRandomFile(dir, segment + ".big6", 3 * chunk + 1);
    createRandomFile(dir, segment + ".big7", 1000 * chunk);
    
    // Setup extraneous files
    createRandomFile(dir, "onetwothree", 100);
    createRandomFile(dir, segment + ".notIn", 50);
    createRandomFile(dir, segment + ".notIn2", 51);
    
    byte id[] = StringHelper.randomId();
    
    // Now test
    CompoundFileDirectory csw = new CompoundFileDirectory(id, dir, "test.cfs", newIOContext(random()), true);
    final String data[] = new String[] {
        ".zero", ".one", ".ten", ".hundred", ".big1", ".big2", ".big3",
        ".big4", ".big5", ".big6", ".big7"
    };
    for (int i=0; i<data.length; i++) {
      String fileName = segment + data[i];
      dir.copy(csw, fileName, fileName, newIOContext(random()));
    }
    csw.close();
    
    CompoundFileDirectory csr = new CompoundFileDirectory(id, dir, "test.cfs", newIOContext(random()), false);
    for (int i=0; i<data.length; i++) {
      IndexInput check = dir.openInput(segment + data[i], newIOContext(random()));
      IndexInput test = csr.openInput(segment + data[i], newIOContext(random()));
      assertSameStreams(data[i], check, test);
      assertSameSeekBehavior(data[i], check, test);
      test.close();
      check.close();
    }
    csr.close();
  }
  
  /** 
   * This test that writes larger than the size of the buffer output
   * will correctly increment the file pointer.
   */
  public void testLargeWrites() throws IOException {
    IndexOutput os = dir.createOutput("testBufferStart.txt", newIOContext(random()));
    
    byte[] largeBuf = new byte[2048];
    for (int i=0; i<largeBuf.length; i++) {
      largeBuf[i] = (byte) (Math.random() * 256);
    }
    
    long currentPos = os.getFilePointer();
    os.writeBytes(largeBuf, largeBuf.length);
    
    try {
      assertEquals(currentPos + largeBuf.length, os.getFilePointer());
    } finally {
      os.close();
    }
  }
  
  public void testAddExternalFile() throws IOException {
    createSequenceFile(dir, "d1", (byte) 0, 15);
    
    Directory newDir = newDirectory();
    byte id[] = StringHelper.randomId();
    CompoundFileDirectory csw = new CompoundFileDirectory(id, newDir, "d.cfs", newIOContext(random()), true);
    dir.copy(csw, "d1", "d1", newIOContext(random()));
    csw.close();
    
    CompoundFileDirectory csr = new CompoundFileDirectory(id, newDir, "d.cfs", newIOContext(random()), false);
    IndexInput expected = dir.openInput("d1", newIOContext(random()));
    IndexInput actual = csr.openInput("d1", newIOContext(random()));
    assertSameStreams("d1", expected, actual);
    assertSameSeekBehavior("d1", expected, actual);
    expected.close();
    actual.close();
    csr.close();
    
    newDir.close();
  }
  
  public void testAppend() throws IOException {
    Directory newDir = newDirectory();
    byte id[] = StringHelper.randomId();
    CompoundFileDirectory csw = new CompoundFileDirectory(id, newDir, "d.cfs", newIOContext(random()), true);
    int size = 5 + random().nextInt(128);
    for (int j = 0; j < 2; j++) {
      IndexOutput os = csw.createOutput("seg_" + j + "_foo.txt", newIOContext(random()));
      for (int i = 0; i < size; i++) {
        os.writeInt(i*j);
      }
      os.close();
      String[] listAll = newDir.listAll();
      assertEquals(1, listAll.length);
      assertEquals("d.cfs", listAll[0]);
    }
    createSequenceFile(dir, "d1", (byte) 0, 15);
    dir.copy(csw, "d1", "d1", newIOContext(random()));
    String[] listAll = newDir.listAll();
    assertEquals(1, listAll.length);
    assertEquals("d.cfs", listAll[0]);
    csw.close();
    CompoundFileDirectory csr = new CompoundFileDirectory(id, newDir, "d.cfs", newIOContext(random()), false);
    for (int j = 0; j < 2; j++) {
      IndexInput openInput = csr.openInput("seg_" + j + "_foo.txt", newIOContext(random()));
      assertEquals(size * 4, openInput.length());
      for (int i = 0; i < size; i++) {
        assertEquals(i*j, openInput.readInt());
      }
      
      openInput.close();
    }
    IndexInput expected = dir.openInput("d1", newIOContext(random()));
    IndexInput actual = csr.openInput("d1", newIOContext(random()));
    assertSameStreams("d1", expected, actual);
    assertSameSeekBehavior("d1", expected, actual);
    expected.close();
    actual.close();
    csr.close();
    newDir.close();
  }
  
  public void testAppendTwice() throws IOException {
    Directory newDir = newDirectory();
    byte id[] = StringHelper.randomId();
    CompoundFileDirectory csw = new CompoundFileDirectory(id, newDir, "d.cfs", newIOContext(random()), true);
    createSequenceFile(newDir, "d1", (byte) 0, 15);
    IndexOutput out = csw.createOutput("d.xyz", newIOContext(random()));
    out.writeInt(0);
    out.close();
    assertEquals(1, csw.listAll().length);
    assertEquals("d.xyz", csw.listAll()[0]);
    
    csw.close();
    
    CompoundFileDirectory cfr = new CompoundFileDirectory(id, newDir, "d.cfs", newIOContext(random()), false);
    assertEquals(1, cfr.listAll().length);
    assertEquals("d.xyz", cfr.listAll()[0]);
    cfr.close();
    newDir.close();
  }
  
  public void testEmptyCFS() throws IOException {
    Directory newDir = newDirectory();
    byte id[] = StringHelper.randomId();
    CompoundFileDirectory csw = new CompoundFileDirectory(id, newDir, "d.cfs", newIOContext(random()), true);
    csw.close();
    
    CompoundFileDirectory csr = new CompoundFileDirectory(id, newDir, "d.cfs", newIOContext(random()), false);
    assertEquals(0, csr.listAll().length);
    csr.close();
    
    newDir.close();
  }
  
  public void testReadNestedCFP() throws IOException {
    Directory newDir = newDirectory();
    // manually manipulates directory
    if (newDir instanceof MockDirectoryWrapper) {
      ((MockDirectoryWrapper)newDir).setEnableVirusScanner(false);
    }
    byte id[] = StringHelper.randomId();
    CompoundFileDirectory csw = new CompoundFileDirectory(id, newDir, "d.cfs", newIOContext(random()), true);
    CompoundFileDirectory nested = new CompoundFileDirectory(id, newDir, "b.cfs", newIOContext(random()), true);
    IndexOutput out = nested.createOutput("b.xyz", newIOContext(random()));
    IndexOutput out1 = nested.createOutput("b_1.xyz", newIOContext(random()));
    out.writeInt(0);
    out1.writeInt(1);
    out.close();
    out1.close();
    nested.close();
    newDir.copy(csw, "b.cfs", "b.cfs", newIOContext(random()));
    newDir.copy(csw, "b.cfe", "b.cfe", newIOContext(random()));
    newDir.deleteFile("b.cfs");
    newDir.deleteFile("b.cfe");
    csw.close();
    
    assertEquals(2, newDir.listAll().length);
    csw = new CompoundFileDirectory(id, newDir, "d.cfs", newIOContext(random()), false);
    
    assertEquals(2, csw.listAll().length);
    nested = new CompoundFileDirectory(id, csw, "b.cfs", newIOContext(random()), false);
    
    assertEquals(2, nested.listAll().length);
    IndexInput openInput = nested.openInput("b.xyz", newIOContext(random()));
    assertEquals(0, openInput.readInt());
    openInput.close();
    openInput = nested.openInput("b_1.xyz", newIOContext(random()));
    assertEquals(1, openInput.readInt());
    openInput.close();
    nested.close();
    csw.close();
    newDir.close();
  }
  
  public void testDoubleClose() throws IOException {
    Directory newDir = newDirectory();
    byte id[] = StringHelper.randomId();
    CompoundFileDirectory csw = new CompoundFileDirectory(id, newDir, "d.cfs", newIOContext(random()), true);
    IndexOutput out = csw.createOutput("d.xyz", newIOContext(random()));
    out.writeInt(0);
    out.close();
    
    csw.close();
    // close a second time - must have no effect according to Closeable
    csw.close();
    
    csw = new CompoundFileDirectory(id, newDir, "d.cfs", newIOContext(random()), false);
    IndexInput openInput = csw.openInput("d.xyz", newIOContext(random()));
    assertEquals(0, openInput.readInt());
    openInput.close();
    csw.close();
    // close a second time - must have no effect according to Closeable
    csw.close();
    
    newDir.close();
    
  }
  
  // Make sure we don't somehow use more than 1 descriptor
  // when reading a CFS with many subs:
  public void testManySubFiles() throws IOException {
    
    final Directory d = newFSDirectory(createTempDir("CFSManySubFiles"));
    byte id[] = StringHelper.randomId();
    
    final int FILE_COUNT = atLeast(500);
    
    for(int fileIdx=0;fileIdx<FILE_COUNT;fileIdx++) {
      IndexOutput out = d.createOutput("file." + fileIdx, newIOContext(random()));
      out.writeByte((byte) fileIdx);
      out.close();
    }
    
    final CompoundFileDirectory cfd = new CompoundFileDirectory(id, d, "c.cfs", newIOContext(random()), true);
    for(int fileIdx=0;fileIdx<FILE_COUNT;fileIdx++) {
      final String fileName = "file." + fileIdx;
      d.copy(cfd, fileName, fileName, newIOContext(random()));
    }
    cfd.close();
    
    final IndexInput[] ins = new IndexInput[FILE_COUNT];
    final CompoundFileDirectory cfr = new CompoundFileDirectory(id, d, "c.cfs", newIOContext(random()), false);
    for(int fileIdx=0;fileIdx<FILE_COUNT;fileIdx++) {
      ins[fileIdx] = cfr.openInput("file." + fileIdx, newIOContext(random()));
    }
    
    for(int fileIdx=0;fileIdx<FILE_COUNT;fileIdx++) {
      assertEquals((byte) fileIdx, ins[fileIdx].readByte());
    }
    
    for(int fileIdx=0;fileIdx<FILE_COUNT;fileIdx++) {
      ins[fileIdx].close();
    }
    cfr.close();
    d.close();
  }
  
  public void testListAll() throws Exception {
    Directory dir = newDirectory();
    if (dir instanceof MockDirectoryWrapper) {
      // test lists files manually and tries to verify every .cfs it finds,
      // but a virus scanner could leave some trash.
      ((MockDirectoryWrapper)dir).setEnableVirusScanner(false);
    }
    // riw should sometimes create docvalues fields, etc
    RandomIndexWriter riw = new RandomIndexWriter(random(), dir);
    Document doc = new Document();
    // these fields should sometimes get term vectors, etc
    Field idField = newStringField("id", "", Field.Store.NO);
    Field bodyField = newTextField("body", "", Field.Store.NO);
    doc.add(idField);
    doc.add(bodyField);
    for (int i = 0; i < 100; i++) {
      idField.setStringValue(Integer.toString(i));
      bodyField.setStringValue(TestUtil.randomUnicodeString(random()));
      riw.addDocument(doc);
      if (random().nextInt(7) == 0) {
        riw.commit();
      }
    }
    riw.close();
    SegmentInfos infos = new SegmentInfos();
    infos.read(dir);
    for (String file : infos.files(dir, true)) {
      try (IndexInput in = dir.openInput(file, IOContext.DEFAULT)) {}
      if (file.endsWith(IndexFileNames.COMPOUND_FILE_EXTENSION)) {
        String segment = IndexFileNames.parseSegmentName(file);
        // warning: N^2
        boolean found = false;
        for (SegmentCommitInfo si : infos) {
          if (si.info.name.equals(segment)) {
            found = true;
            try (CompoundFileDirectory cfs = new CompoundFileDirectory(si.info.getId(), dir, file, IOContext.DEFAULT, false)) {
              for (String cfsFile : cfs.listAll()) {
                try (IndexInput cfsIn = cfs.openInput(cfsFile, IOContext.DEFAULT)) {}
              }
            }
          }
        }
        assertTrue(found);
      }
    }
    dir.close();
  }
}
