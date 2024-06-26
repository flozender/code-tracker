package org.apache.lucene.document;

import org.apache.lucene.util.BytesRef;

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

public final class BinaryField extends Field {

  public static final FieldType TYPE_STORED = new FieldType();
  static {
    TYPE_STORED.setStored(true);
    TYPE_STORED.freeze();
  }

  public BinaryField(String name, byte[] value) {
    super(name, BinaryField.TYPE_STORED, value);
  }
  
  public BinaryField(String name, byte[] value, int offset, int length) {
    super(name, BinaryField.TYPE_STORED, value, offset, length);
  }

  public BinaryField(String name, BytesRef bytes) {
    super(name, BinaryField.TYPE_STORED, bytes);
  }
  
  public BinaryField(String name, FieldType custom, byte[] value) {
    super(name, custom, value);
  }
    
  public boolean isNumeric() {
    return false;
  }  
}
