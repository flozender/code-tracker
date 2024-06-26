package org.apache.lucene.document;

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

import org.apache.lucene.index.DocValues;

/**
 * <p>
 * This class provides a {@link Field} that enables storing
 * of a per-document short value for scoring, sorting or value retrieval. Here's an
 * example usage:
 * 
 * <pre>
 *   document.add(new ShortDocValuesField(name, (short) 22));
 * </pre>
 * 
 * <p>
 * If you also need to store the value, you should add a
 * separate {@link StoredField} instance.
 * 
 * @see DocValues for further information
 * */

public class ShortDocValuesField extends StoredField {

  public static final FieldType TYPE = new FieldType();
  static {
    TYPE.setDocValueType(DocValues.Type.FIXED_INTS_16);
    TYPE.freeze();
  }

  public ShortDocValuesField(String name, short value) {
    super(name, TYPE);
    fieldsData = Short.valueOf(value);
  }
}
