package org.apache.lucene.store;

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

import java.io.Closeable;
import java.io.IOException;

/** Abstract base class for input from a file in a {@link Directory}.  A
 * random-access input stream.  Used for all Lucene index input operations.
 * @see Directory
 */
public abstract class IndexInput extends DataInput implements Cloneable,Closeable {
  /** Closes the stream to further operations. */
  public abstract void close() throws IOException;

  /** Returns the current position in this file, where the next read will
   * occur.
   * @see #seek(long)
   */
  public abstract long getFilePointer();

  /** Sets current position in this file, where the next read will occur.
   * @see #getFilePointer()
   */
  public abstract void seek(long pos) throws IOException;

  /** The number of bytes in the file. */
  public abstract long length();
}