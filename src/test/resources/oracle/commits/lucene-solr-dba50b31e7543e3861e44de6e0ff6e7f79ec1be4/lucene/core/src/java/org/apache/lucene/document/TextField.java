package org.apache.lucene.document;

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

import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;

/** A field that is indexed and tokenized, without term
 *  vectors.  For example this would be used on a 'body'
 *  field, that contains the bulk of a document's text. */

public final class TextField extends Field {

  /* Indexed, tokenized, not stored. */
  public static final FieldType TYPE_NOT_STORED = new FieldType();

  /* Indexed, tokenized, stored. */
  public static final FieldType TYPE_STORED = new FieldType();

  static {
    TYPE_NOT_STORED.setIndexed(true);
    TYPE_NOT_STORED.setTokenized(true);
    TYPE_NOT_STORED.freeze();

    TYPE_STORED.setIndexed(true);
    TYPE_STORED.setTokenized(true);
    TYPE_STORED.setStored(true);
    TYPE_STORED.freeze();
  }

  // TODO: add sugar for term vectors...?

  /** Creates a new TextField with Reader value. */
  public TextField(String name, Reader reader, Store store) {
    super(name, reader, store == Store.YES ? TYPE_STORED : TYPE_NOT_STORED);
  }

  /** Creates a new TextField with String value. */
  public TextField(String name, String value, Store store) {
    super(name, value, store == Store.YES ? TYPE_STORED : TYPE_NOT_STORED);
  }
  
  /** Creates a new un-stored TextField with TokenStream value. */
  public TextField(String name, TokenStream stream) {
    super(name, stream, TYPE_NOT_STORED);
  }
}
