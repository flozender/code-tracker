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

import java.io.IOException;

import org.apache.lucene.index.values.DocValues;

/** Flex API for access to fields and terms
 *  @lucene.experimental */

public abstract class Fields {

  /** Returns an iterator that will step through all fields
   *  names.  This will not return null.  */
  public abstract FieldsEnum iterator() throws IOException;

  /** Get the {@link Terms} for this field.  This will return
   *  null if the field does not exist. */
  public abstract Terms terms(String field) throws IOException;
  
  /**
   * Returns {@link DocValues} for the current field.
   * 
   * @param field the field name
   * @return the {@link DocValues} for this field or <code>null</code> if not
   *         applicable.
   * @throws IOException
   */
  public abstract DocValues docValues(String field) throws IOException;

  public final static Fields[] EMPTY_ARRAY = new Fields[0];
}
