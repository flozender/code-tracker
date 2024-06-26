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

import org.apache.lucene.analysis.Analyzer; // javadocs
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.search.NumericRangeQuery; // javadocs
import org.apache.lucene.util.NumericUtils;

/**
 * Describes the properties of a field.
 */
public class FieldType implements IndexableFieldType {

  /** Data type of the numeric value
   * @since 3.2
   */
  public static enum NumericType {INT, LONG, FLOAT, DOUBLE}

  private boolean indexed;
  private boolean stored;
  private boolean tokenized = true;
  private boolean storeTermVectors;
  private boolean storeTermVectorOffsets;
  private boolean storeTermVectorPositions;
  private boolean storeTermVectorPayloads;
  private boolean omitNorms;
  private IndexOptions indexOptions = IndexOptions.DOCS_AND_FREQS_AND_POSITIONS;
  private DocValues.Type docValueType;
  private NumericType numericType;
  private boolean frozen;
  private int numericPrecisionStep = NumericUtils.PRECISION_STEP_DEFAULT;

  /**
   * Create a new mutable FieldType with all of the properties from <code>ref</code>
   */
  public FieldType(FieldType ref) {
    this.indexed = ref.indexed();
    this.stored = ref.stored();
    this.tokenized = ref.tokenized();
    this.storeTermVectors = ref.storeTermVectors();
    this.storeTermVectorOffsets = ref.storeTermVectorOffsets();
    this.storeTermVectorPositions = ref.storeTermVectorPositions();
    this.storeTermVectorPayloads = ref.storeTermVectorPayloads();
    this.omitNorms = ref.omitNorms();
    this.indexOptions = ref.indexOptions();
    this.docValueType = ref.docValueType();
    this.numericType = ref.numericType();
    // Do not copy frozen!
  }
  
  /**
   * Create a new FieldType with default properties.
   */
  public FieldType() {
  }

  private void checkIfFrozen() {
    if (frozen) {
      throw new IllegalStateException("this FieldType is already frozen and cannot be changed");
    }
  }

  /**
   * Prevents future changes. Note, it is recommended that this is called once
   * the FieldTypes's properties have been set, to prevent unintentional state
   * changes.
   */
  public void freeze() {
    this.frozen = true;
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * The default is <code>false</code>.
   * @see #setIndexed(boolean)
   */
  public boolean indexed() {
    return this.indexed;
  }
  
  /**
   * Set to <code>true</code> to index (invert) this field.
   * @see #indexed()
   */
  public void setIndexed(boolean value) {
    checkIfFrozen();
    this.indexed = value;
  }

  /**
   * {@inheritDoc}
   * <p>
   * The default is <code>false</code>.
   * @see #setStored(boolean)
   */
  public boolean stored() {
    return this.stored;
  }
  
  /**
   * Set to <code>true</code> to store this field.
   * @see #stored()
   */
  public void setStored(boolean value) {
    checkIfFrozen();
    this.stored = value;
  }

  /**
   * {@inheritDoc}
   * <p>
   * The default is <code>true</code>.
   * @see #setTokenized(boolean)
   */
  public boolean tokenized() {
    return this.tokenized;
  }
  
  /**
   * Set to <code>true</code> to tokenize this field's contents via the 
   * configured {@link Analyzer}.
   * @see #tokenized()
   */
  public void setTokenized(boolean value) {
    checkIfFrozen();
    this.tokenized = value;
  }

  /**
   * {@inheritDoc}
   * <p>
   * The default is <code>false</code>. 
   * @see #setStoreTermVectors(boolean)
   */
  public boolean storeTermVectors() {
    return this.storeTermVectors;
  }
  
  /**
   * Set to <code>true</code> if this field's indexed form should be also stored 
   * into term vectors.
   * @see #storeTermVectors()
   */
  public void setStoreTermVectors(boolean value) {
    checkIfFrozen();
    this.storeTermVectors = value;
  }

  /**
   * {@inheritDoc}
   * <p>
   * The default is <code>false</code>.
   * @see #setStoreTermVectorOffsets(boolean)
   */
  public boolean storeTermVectorOffsets() {
    return this.storeTermVectorOffsets;
  }
  
  /**
   * Set to <code>true</code> to also store token character offsets into the term
   * vector for this field.
   * @see #storeTermVectorOffsets()
   */
  public void setStoreTermVectorOffsets(boolean value) {
    checkIfFrozen();
    this.storeTermVectorOffsets = value;
  }

  /**
   * {@inheritDoc}
   * <p>
   * The default is <code>false</code>.
   * @see #setStoreTermVectorPositions(boolean)
   */
  public boolean storeTermVectorPositions() {
    return this.storeTermVectorPositions;
  }
  
  /**
   * Set to <code>true</code> to also store token positions into the term
   * vector for this field.
   * @see #storeTermVectorPositions()
   */
  public void setStoreTermVectorPositions(boolean value) {
    checkIfFrozen();
    this.storeTermVectorPositions = value;
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * The default is <code>false</code>.
   * @see #setStoreTermVectorPayloads(boolean) 
   */
  public boolean storeTermVectorPayloads() {
    return this.storeTermVectorPayloads;
  }
  
  /**
   * Set to <code>true</code> to also store token payloads into the term
   * vector for this field.
   * @see #storeTermVectorPayloads()
   */
  public void setStoreTermVectorPayloads(boolean value) {
    checkIfFrozen();
    this.storeTermVectorPayloads = value;
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * The default is <code>false</code>.
   * @see #setOmitNorms(boolean)
   */
  public boolean omitNorms() {
    return this.omitNorms;
  }
  
  /**
   * Set to <code>true</code> to omit normalization values for the field.
   * @see #omitNorms()
   */
  public void setOmitNorms(boolean value) {
    checkIfFrozen();
    this.omitNorms = value;
  }

  /**
   * {@inheritDoc}
   * <p>
   * The default is {@link IndexOptions#DOCS_AND_FREQS_AND_POSITIONS}.
   * @see #setIndexOptions(FieldInfo.IndexOptions)
   */
  public IndexOptions indexOptions() {
    return this.indexOptions;
  }
  
  /**
   * Sets the indexing options for the field:
   * @see #indexOptions()
   */
  public void setIndexOptions(IndexOptions value) {
    checkIfFrozen();
    this.indexOptions = value;
  }

  /**
   * Set's the field's DocValues.Type
   * @see #docValueType()
   */
  public void setDocValueType(DocValues.Type type) {
    checkIfFrozen();
    docValueType = type;
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * The default is <code>null</code> (no docValues) 
   * @see #setDocValueType(DocValues.Type)
   */
  @Override
  public DocValues.Type docValueType() {
    return docValueType;
  }

  /**
   * Specifies the field's numeric type.
   * @see #numericType()
   */
  public void setNumericType(NumericType type) {
    checkIfFrozen();
    numericType = type;
  }

  /** 
   * NumericType: if non-null then the field's value will be indexed
   * numerically so that {@link NumericRangeQuery} can be used at 
   * search time. 
   * <p>
   * The default is <code>null</code> (no numeric type) 
   * @see #setNumericType(NumericType)
   */
  public NumericType numericType() {
    return numericType;
  }

  /**
   * Sets the numeric precision step for the field.
   * @see #numericPrecisionStep()
   */
  public void setNumericPrecisionStep(int precisionStep) {
    checkIfFrozen();
    if (precisionStep < 1) {
      throw new IllegalArgumentException("precisionStep must be >= 1 (got " + precisionStep + ")");
    }
    this.numericPrecisionStep = precisionStep;
  }

  /** 
   * Precision step for numeric field. 
   * <p>
   * This has no effect if {@link #numericType()} returns null.
   * <p>
   * The default is {@link NumericUtils#PRECISION_STEP_DEFAULT}
   * @see #setNumericPrecisionStep(int)
   */
  public int numericPrecisionStep() {
    return numericPrecisionStep;
  }

  /** Prints a Field for human consumption. */
  @Override
  public final String toString() {
    StringBuilder result = new StringBuilder();
    if (stored()) {
      result.append("stored");
    }
    if (indexed()) {
      if (result.length() > 0)
        result.append(",");
      result.append("indexed");
      if (tokenized()) {
        result.append(",tokenized");
      }
      if (storeTermVectors()) {
        result.append(",termVector");
      }
      if (storeTermVectorOffsets()) {
        result.append(",termVectorOffsets");
      }
      if (storeTermVectorPositions()) {
        result.append(",termVectorPosition");
        if (storeTermVectorPayloads()) {
          result.append(",termVectorPayloads");
        }
      }
      if (omitNorms()) {
        result.append(",omitNorms");
      }
      if (indexOptions != IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) {
        result.append(",indexOptions=");
        result.append(indexOptions);
      }
      if (numericType != null) {
        result.append(",numericType=");
        result.append(numericType);
        result.append(",numericPrecisionStep=");
        result.append(numericPrecisionStep);
      }
    }
    if (docValueType != null) {
      if (result.length() > 0)
        result.append(",");
      result.append("docValueType=");
      result.append(docValueType);
    }
    
    return result.toString();
  }
}
