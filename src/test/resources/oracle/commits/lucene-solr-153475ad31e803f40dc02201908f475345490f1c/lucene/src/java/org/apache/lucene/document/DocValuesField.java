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
import java.util.Comparator;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.values.PerDocFieldValues;
import org.apache.lucene.index.values.ValueType;
import org.apache.lucene.util.BytesRef;

/**
 * <p>
 * This class provides a {@link AbstractField} that enables storing of typed
 * per-document values for scoring, sorting or value retrieval. Here's an
 * example usage, adding an int value:
 * 
 * <pre>
 * document.add(new DocValuesField(name).setInt(value));
 * </pre>
 * 
 * For optimal performance, re-use the <code>DocValuesField</code> and
 * {@link Document} instance for more than one document:
 * 
 * <pre>
 *  DocValuesField field = new DocValuesField(name);
 *  Document document = new Document();
 *  document.add(field);
 * 
 *  for(all documents) {
 *    ...
 *    field.setInt(value)
 *    writer.addDocument(document);
 *    ...
 *  }
 * </pre>
 * 
 * <p>
 * If doc values are stored in addition to an indexed ({@link Index}) or stored
 * ({@link Store}) value it's recommended to use the {@link DocValuesField}'s
 * {@link #set(AbstractField)} API:
 * 
 * <pre>
 *  DocValuesField field = new DocValuesField(name);
 *  Field indexedField = new Field(name, stringValue, Stored.NO, Indexed.ANALYZED);
 *  Document document = new Document();
 *  document.add(indexedField);
 *  field.set(indexedField);
 *  for(all documents) {
 *    ...
 *    field.setInt(value)
 *    writer.addDocument(document);
 *    ...
 *  }
 * </pre>
 * 
 * */
public class DocValuesField extends AbstractField implements PerDocFieldValues {

  protected BytesRef bytes;
  protected double doubleValue;
  protected long longValue;
  protected ValueType type;
  protected Comparator<BytesRef> bytesComparator;

  /**
   * Creates a new {@link DocValuesField} with the given name.
   */
  public DocValuesField(String name) {
    super(name, Store.NO, Index.NO, TermVector.NO);
    setDocValues(this);
  }

  /**
   * Creates a {@link DocValuesField} prototype
   */
  DocValuesField() {
    this("");
  }

  /**
   * Sets the given <code>long</code> value and sets the field's {@link ValueType} to
   * {@link ValueType#INTS} unless already set. If you want to change the
   * default type use {@link #setType(ValueType)}.
   */
  public void setInt(long value) {
    if (type == null) {
      type = ValueType.INTS;
    }
    longValue = value;
  }

  /**
   * Sets the given <code>float</code> value and sets the field's {@link ValueType}
   * to {@link ValueType#FLOAT_32} unless already set. If you want to
   * change the type use {@link #setType(ValueType)}.
   */
  public void setFloat(float value) {
    if (type == null) {
      type = ValueType.FLOAT_32;
    }
    doubleValue = value;
  }

  /**
   * Sets the given <code>double</code> value and sets the field's {@link ValueType}
   * to {@link ValueType#FLOAT_64} unless already set. If you want to
   * change the default type use {@link #setType(ValueType)}.
   */
  public void setFloat(double value) {
    if (type == null) {
      type = ValueType.FLOAT_64;
    }
    doubleValue = value;
  }

  /**
   * Sets the given {@link BytesRef} value and the field's {@link ValueType}. The
   * comparator for this field is set to <code>null</code>. If a
   * <code>null</code> comparator is set the default comparator for the given
   * {@link ValueType} is used.
   */
  public void setBytes(BytesRef value, ValueType type) {
    setBytes(value, type, null);
  }

  /**
   * Sets the given {@link BytesRef} value, the field's {@link ValueType} and the
   * field's comparator. If the {@link Comparator} is set to <code>null</code>
   * the default for the given {@link ValueType} is used instead.
   * 
   * @throws IllegalArgumentException
   *           if the value or the type are null
   */
  public void setBytes(BytesRef value, ValueType type, Comparator<BytesRef> comp) {
    if (value == null) {
      throw new IllegalArgumentException("value must not be null");
    }
    setType(type);
    if (bytes == null) {
      bytes = new BytesRef(value);
    } else {
      bytes.copy(value);
    }
    bytesComparator = comp;
  }

  /**
   * Returns the set {@link BytesRef} or <code>null</code> if not set.
   */
  public BytesRef getBytes() {
    return bytes;
  }

  /**
   * Returns the set {@link BytesRef} comparator or <code>null</code> if not set
   */
  public Comparator<BytesRef> bytesComparator() {
    return bytesComparator;
  }

  /**
   * Returns the set floating point value or <code>0.0d</code> if not set.
   */
  public double getFloat() {
    return doubleValue;
  }

  /**
   * Returns the set <code>long</code> value of <code>0</code> if not set.
   */
  public long getInt() {
    return longValue;
  }

  /**
   * Sets the {@link BytesRef} comparator for this field. If the field has a
   * numeric {@link ValueType} the comparator will be ignored.
   */
  public void setBytesComparator(Comparator<BytesRef> comp) {
    this.bytesComparator = comp;
  }

  /**
   * Sets the {@link ValueType} for this field.
   */
  public void setType(ValueType type) {
    if (type == null) {
      throw new IllegalArgumentException("Type must not be null");
    }
    this.type = type;
  }

  /**
   * Returns the field's {@link ValueType}
   */
  public ValueType type() {
    return type;
  }

  /**
   * Returns always <code>null</code>
   */
  public Reader readerValue() {
    return null;
  }

  /**
   * Returns always <code>null</code>
   */
  public String stringValue() {
    return null;
  }

  /**
   * Returns always <code>null</code>
   */
  public TokenStream tokenStreamValue() {
    return null;
  }

  /**
   * Sets this {@link DocValuesField} to the given {@link AbstractField} and
   * returns the given field. Any modifications to this instance will be visible
   * to the given field.
   */
  public <T extends AbstractField> T set(T field) {
    field.setDocValues(this);
    return field;
  }

  /**
   * Sets a new {@link PerDocFieldValues} instance on the given field with the
   * given type and returns it.
   * 
   */
  public static <T extends AbstractField> T set(T field, ValueType type) {
    if (field instanceof DocValuesField)
      return field;
    final DocValuesField valField = new DocValuesField();
    switch (type) {
    case BYTES_FIXED_DEREF:
    case BYTES_FIXED_SORTED:
    case BYTES_FIXED_STRAIGHT:
    case BYTES_VAR_DEREF:
    case BYTES_VAR_SORTED:
    case BYTES_VAR_STRAIGHT:
      BytesRef ref = field.isBinary() ? new BytesRef(field.getBinaryValue(),
          field.getBinaryOffset(), field.getBinaryLength()) : new BytesRef(
          field.stringValue());
      valField.setBytes(ref, type);
      break;
    case INTS:
      valField.setInt(Long.parseLong(field.stringValue()));
      break;
    case FLOAT_32:
      valField.setFloat(Float.parseFloat(field.stringValue()));
      break;
    case FLOAT_64:
      valField.setFloat(Double.parseDouble(field.stringValue()));
      break;
    default:
      throw new IllegalArgumentException("unknown type: " + type);
    }
    return valField.set(field);
  }

}
