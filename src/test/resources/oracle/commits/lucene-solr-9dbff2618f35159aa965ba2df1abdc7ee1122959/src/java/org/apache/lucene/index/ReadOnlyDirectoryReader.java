package org.apache.lucene.index;

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

import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.Map;

class ReadOnlyDirectoryReader extends DirectoryReader {
  ReadOnlyDirectoryReader(Directory directory, SegmentInfos sis, IndexDeletionPolicy deletionPolicy, boolean closeDirectory) throws IOException {
    super(directory, sis, deletionPolicy, closeDirectory, true);
  }

  ReadOnlyDirectoryReader(Directory directory, SegmentInfos infos, boolean closeDirectory, SegmentReader[] oldReaders, int[] oldStarts, Map oldNormsCache, boolean doClone) throws IOException {
    super(directory, infos, closeDirectory, oldReaders, oldStarts, oldNormsCache, true, doClone);
  }
  
  ReadOnlyDirectoryReader(IndexWriter writer, SegmentInfos infos) throws IOException {
    super(writer, infos);
  }
  
  protected void acquireWriteLock() {
    ReadOnlySegmentReader.noWrite();
  }
}
