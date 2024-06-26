package org.apache.lucene.mockfile;

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
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;

/**  
 * A {@code FilterPath} contains another 
 * {@code Path}, which it uses as its basic 
 * source of data, possibly transforming the data along the 
 * way or providing additional functionality. 
 */
public class FilterPath implements Path {
  
  /** 
   * The underlying {@code Path} instance. 
   */
  protected final Path delegate;
  
  /** 
   * The parent {@code FileSystem} for this path. 
   */
  protected final FileSystem fileSystem;
  
  /**
   * Construct a {@code FilterPath} with parent
   * {@code fileSystem}, based on the specified base path.
   * @param delegate specified base path.
   * @param fileSystem parent fileSystem.
   */
  public FilterPath(Path delegate, FileSystem fileSystem) {
    this.delegate = delegate;
    this.fileSystem = fileSystem;
  }
  
  /** 
   * Get the underlying wrapped path.
   * @return wrapped path.
   */
  public Path getDelegate() {
    return delegate;
  }

  @Override
  public FileSystem getFileSystem() {
    return fileSystem;
  }

  @Override
  public boolean isAbsolute() {
    return delegate.isAbsolute();
  }

  @Override
  public Path getRoot() {
    Path root = delegate.getRoot();
    if (root == null) {
      return null;
    }
    return new FilterPath(root, fileSystem);
  }

  @Override
  public Path getFileName() {
    Path fileName = delegate.getFileName();
    if (fileName == null) {
      return null;
    }
    return new FilterPath(fileName, fileSystem);
  }

  @Override
  public Path getParent() {
    Path parent = delegate.getParent();
    if (parent == null) {
      return null;
    }
    return new FilterPath(parent, fileSystem);
  }

  @Override
  public int getNameCount() {
    return delegate.getNameCount();
  }

  @Override
  public Path getName(int index) {
    return new FilterPath(delegate.getName(index), fileSystem);
  }

  @Override
  public Path subpath(int beginIndex, int endIndex) {
    return new FilterPath(delegate.subpath(beginIndex, endIndex), fileSystem);
  }

  @Override
  public boolean startsWith(Path other) {
    if (other instanceof FilterPath) {
      FilterPath f = (FilterPath) other;
      return fileSystem == f.fileSystem && delegate.startsWith(f.delegate);
    }
    return false;
  }

  @Override
  public boolean startsWith(String other) {
    return delegate.startsWith(other);
  }

  @Override
  public boolean endsWith(Path other) {
    if (other instanceof FilterPath) {
      FilterPath f = (FilterPath) other;
      return fileSystem == f.fileSystem && delegate.endsWith(f.delegate);
    }
    return false;
  }

  @Override
  public boolean endsWith(String other) {
    return delegate.startsWith(other);
  }

  @Override
  public Path normalize() {
    return new FilterPath(delegate.normalize(), fileSystem);
  }

  @Override
  public Path resolve(Path other) {
    if (other instanceof FilterPath) {
      other = ((FilterPath)other).delegate;
    }
    return new FilterPath(delegate.resolve(other), fileSystem);
  }

  @Override
  public Path resolve(String other) {
    return new FilterPath(delegate.resolve(other), fileSystem);
  }

  @Override
  public Path resolveSibling(Path other) {
    if (other instanceof FilterPath) {
      other = ((FilterPath)other).delegate;
    }
    return new FilterPath(delegate.resolveSibling(other), fileSystem);
  }

  @Override
  public Path resolveSibling(String other) {
    return new FilterPath(delegate.resolveSibling(other), fileSystem);
  }

  @Override
  public Path relativize(Path other) {
    if (other instanceof FilterPath) {
      other = ((FilterPath)other).delegate;
    }
    return new FilterPath(delegate.relativize(other), fileSystem);
  }

  // TODO: should these methods not expose delegate result directly?
  // it could allow code to "escape" the sandbox... 

  @Override
  public URI toUri() {
    return delegate.toUri();
  }
  
  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  public Path toAbsolutePath() {
    return new FilterPath(delegate.toAbsolutePath(), fileSystem);
  }

  @Override
  public Path toRealPath(LinkOption... options) throws IOException {
    return new FilterPath(delegate.toRealPath(options), fileSystem);
  }

  @Override
  public File toFile() {
    // TODO: should we throw exception here?
    return delegate.toFile();
  }

  @Override
  public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
    return delegate.register(watcher, events, modifiers);
  }

  @Override
  public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
    return delegate.register(watcher, events);
  }

  @Override
  public Iterator<Path> iterator() {
    final Iterator<Path> iterator = delegate.iterator();
    return new Iterator<Path>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Path next() {
        return new FilterPath(iterator.next(), fileSystem);
      }

      @Override
      public void remove() {
        iterator.remove();
      }
    };
  }

  @Override
  public int compareTo(Path other) {
    if (other instanceof FilterPath) {
      other = ((FilterPath)other).delegate;
    }
    return delegate.compareTo(other);
  }
  
  /**
   * Unwraps all {@code FilterPath}s, returning
   * the innermost {@code Path}.
   * <p>
   * WARNING: this is exposed for testing only!
   * @param path specified path.
   * @return innermost Path instance
   */
  public static Path unwrap(Path path) {
    while (path instanceof FilterPath) {
      path = ((FilterPath)path).delegate;
    }
    return path;
  }
}
