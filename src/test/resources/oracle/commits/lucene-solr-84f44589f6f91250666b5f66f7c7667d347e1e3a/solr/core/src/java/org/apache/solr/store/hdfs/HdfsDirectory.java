package org.apache.solr.store.hdfs;

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
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.SafeModeAction;
import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.LockFactory;
import org.apache.solr.store.blockcache.CustomBufferedIndexInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HdfsDirectory extends BaseDirectory {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  public static final int BUFFER_SIZE = 8192;
  
  private static final String LF_EXT = ".lf";
  protected final Path hdfsDirPath;
  protected final Configuration configuration;
  
  private final FileSystem fileSystem;
  private final FileContext fileContext;
  
  public HdfsDirectory(Path hdfsDirPath, Configuration configuration) throws IOException {
    this(hdfsDirPath, HdfsLockFactory.INSTANCE, configuration);
  }

  public HdfsDirectory(Path hdfsDirPath, LockFactory lockFactory, Configuration configuration)
      throws IOException {
    super(lockFactory);
    this.hdfsDirPath = hdfsDirPath;
    this.configuration = configuration;
    fileSystem = FileSystem.get(hdfsDirPath.toUri(), configuration);
    fileContext = FileContext.getFileContext(hdfsDirPath.toUri(), configuration);
    
    if (fileSystem instanceof DistributedFileSystem) {
      // Make sure dfs is not in safe mode
      while (((DistributedFileSystem) fileSystem).setSafeMode(SafeModeAction.SAFEMODE_GET, true)) {
        LOG.warn("The NameNode is in SafeMode - Solr will wait 5 seconds and try again.");
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          Thread.interrupted();
          // continue
        }
      }
    }
    
    try {
      if (!fileSystem.exists(hdfsDirPath)) {
        boolean success = fileSystem.mkdirs(hdfsDirPath);
        if (!success) {
          throw new RuntimeException("Could not create directory: " + hdfsDirPath);
        }
      }
    } catch (Exception e) {
      org.apache.solr.common.util.IOUtils.closeQuietly(fileSystem);
      throw new RuntimeException("Problem creating directory: " + hdfsDirPath, e);
    }
  }
  
  @Override
  public void close() throws IOException {
    LOG.info("Closing hdfs directory {}", hdfsDirPath);
    fileSystem.close();
    isOpen = false;
  }

  /**
   * Check whether this directory is open or closed. This check may return stale results in the form of false negatives.
   * @return true if the directory is definitely closed, false if the directory is open or is pending closure
   */
  public boolean isClosed() {
    return !isOpen;
  }
  
  @Override
  public IndexOutput createOutput(String name, IOContext context) throws IOException {
    return new HdfsFileWriter(getFileSystem(), new Path(hdfsDirPath, name), name);
  }

  @Override
  public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
    throw new UnsupportedOperationException();
  }
  
  private String[] getNormalNames(List<String> files) {
    int size = files.size();
    for (int i = 0; i < size; i++) {
      String str = files.get(i);
      files.set(i, toNormalName(str));
    }
    return files.toArray(new String[] {});
  }
  
  private String toNormalName(String name) {
    if (name.endsWith(LF_EXT)) {
      return name.substring(0, name.length() - 3);
    }
    return name;
  }
  
  @Override
  public IndexInput openInput(String name, IOContext context)
      throws IOException {
    return openInput(name, BUFFER_SIZE);
  }
  
  private IndexInput openInput(String name, int bufferSize) throws IOException {
    return new HdfsIndexInput(name, getFileSystem(), new Path(
        hdfsDirPath, name), BUFFER_SIZE);
  }
  
  @Override
  public void deleteFiles(Collection<String> names) throws IOException {
    for(String name : names) {
      Path path = new Path(hdfsDirPath, name);
      LOG.debug("Deleting {}", path);
      getFileSystem().delete(path, false);
    }
  }
  
  @Override
  public void renameFile(String source, String dest) throws IOException {
    Path sourcePath = new Path(hdfsDirPath, source);
    Path destPath = new Path(hdfsDirPath, dest);
    fileContext.rename(sourcePath, destPath);
  }

  @Override
  public long fileLength(String name) throws IOException {
    return HdfsFileReader.getLength(getFileSystem(),
        new Path(hdfsDirPath, name));
  }
  
  public long fileModified(String name) throws IOException {
    FileStatus fileStatus = getFileSystem().getFileStatus(
        new Path(hdfsDirPath, name));
    return fileStatus.getModificationTime();
  }
  
  @Override
  public String[] listAll() throws IOException {
    FileStatus[] listStatus = getFileSystem().listStatus(hdfsDirPath);
    List<String> files = new ArrayList<>();
    if (listStatus == null) {
      return new String[] {};
    }
    for (FileStatus status : listStatus) {
      files.add(status.getPath().getName());
    }
    return getNormalNames(files);
  }

  public Path getHdfsDirPath() {
    return hdfsDirPath;
  }
  
  public FileSystem getFileSystem() {
    return fileSystem;
  }
  
  public Configuration getConfiguration() {
    return configuration;
  }
  
  static class HdfsIndexInput extends CustomBufferedIndexInput {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    private final Path path;
    private final FSDataInputStream inputStream;
    private final long length;
    private boolean clone = false;
    
    public HdfsIndexInput(String name, FileSystem fileSystem, Path path,
        int bufferSize) throws IOException {
      super(name);
      this.path = path;
      LOG.debug("Opening normal index input on {}", path);
      FileStatus fileStatus = fileSystem.getFileStatus(path);
      length = fileStatus.getLen();
      inputStream = fileSystem.open(path, bufferSize);
    }
    
    @Override
    protected void readInternal(byte[] b, int offset, int length)
        throws IOException {
      inputStream.readFully(getFilePointer(), b, offset, length);
    }
    
    @Override
    protected void seekInternal(long pos) throws IOException {

    }
    
    @Override
    protected void closeInternal() throws IOException {
      LOG.debug("Closing normal index input on {}", path);
      if (!clone) {
        inputStream.close();
      }
    }
    
    @Override
    public long length() {
      return length;
    }
    
    @Override
    public IndexInput clone() {
      HdfsIndexInput clone = (HdfsIndexInput) super.clone();
      clone.clone = true;
      return clone;
    }
  }
  
  @Override
  public void sync(Collection<String> names) throws IOException {
    LOG.debug("Sync called on {}", Arrays.toString(names.toArray()));
  }
  
  @Override
  public int hashCode() {
    return hdfsDirPath.hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof HdfsDirectory)) {
      return false;
    }
    return this.hdfsDirPath.equals(((HdfsDirectory) obj).hdfsDirPath);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "@" + hdfsDirPath + " lockFactory=" + lockFactory;
  }
}
