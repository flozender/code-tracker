package org.apache.lucene.util;

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

import java.io.File;
import java.io.IOException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.store.Directory;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Random;

public class _TestUtil {

  /** Returns temp dir, containing String arg in its name;
   *  does not create the directory. */
  public static File getTempDir(String desc) {
    String tempDir = System.getProperty("java.io.tmpdir");
    if (tempDir == null)
      throw new RuntimeException("java.io.tmpdir undefined, cannot run test");
    return new File(tempDir, desc + "." + new Random().nextLong());
  }

  public static void rmDir(File dir) throws IOException {
    if (dir.exists()) {
      File[] files = dir.listFiles();
      for (int i = 0; i < files.length; i++) {
        if (!files[i].delete()) {
          throw new IOException("could not delete " + files[i]);
        }
      }
      dir.delete();
    }
  }

  public static void rmDir(String dir) throws IOException {
    rmDir(new File(dir));
  }

  public static void syncConcurrentMerges(IndexWriter writer) {
    syncConcurrentMerges(writer.getConfig().getMergeScheduler());
  }

  public static void syncConcurrentMerges(MergeScheduler ms) {
    if (ms instanceof ConcurrentMergeScheduler)
      ((ConcurrentMergeScheduler) ms).sync();
  }

  /** This runs the CheckIndex tool on the index in.  If any
   *  issues are hit, a RuntimeException is thrown; else,
   *  true is returned. */
  public static boolean checkIndex(Directory dir) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

    CheckIndex checker = new CheckIndex(dir);
    checker.setInfoStream(new PrintStream(bos));
    CheckIndex.Status indexStatus = checker.checkIndex();
    if (indexStatus == null || indexStatus.clean == false) {
      System.out.println("CheckIndex failed");
      System.out.println(bos.toString());
      throw new RuntimeException("CheckIndex failed");
    } else
      return true;
  }

  /** Use only for testing.
   *  @deprecated -- in 3.0 we can use Arrays.toString
   *  instead */
  @Deprecated
  public static String arrayToString(int[] array) {
    StringBuilder buf = new StringBuilder();
    buf.append("[");
    for(int i=0;i<array.length;i++) {
      if (i > 0) {
        buf.append(" ");
      }
      buf.append(array[i]);
    }
    buf.append("]");
    return buf.toString();
  }

  /** Use only for testing.
   *  @deprecated -- in 3.0 we can use Arrays.toString
   *  instead */
  @Deprecated
  public static String arrayToString(Object[] array) {
    StringBuilder buf = new StringBuilder();
    buf.append("[");
    for(int i=0;i<array.length;i++) {
      if (i > 0) {
        buf.append(" ");
      }
      buf.append(array[i]);
    }
    buf.append("]");
    return buf.toString();
  }

  public static int getRandomSocketPort() {
    return 1024 + new Random().nextInt(64512);
  }

}
