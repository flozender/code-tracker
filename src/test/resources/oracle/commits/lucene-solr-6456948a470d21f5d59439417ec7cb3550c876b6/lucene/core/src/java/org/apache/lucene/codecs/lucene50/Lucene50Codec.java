package org.apache.lucene.codecs.lucene50;

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

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.DocValuesFormat;
import org.apache.lucene.codecs.FieldInfosFormat;
import org.apache.lucene.codecs.FilterCodec;
import org.apache.lucene.codecs.LiveDocsFormat;
import org.apache.lucene.codecs.NormsFormat;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.SegmentInfoFormat;
import org.apache.lucene.codecs.StoredFieldsFormat;
import org.apache.lucene.codecs.TermVectorsFormat;
import org.apache.lucene.codecs.perfield.PerFieldDocValuesFormat;
import org.apache.lucene.codecs.perfield.PerFieldPostingsFormat;

/**
 * Implements the Lucene 5.0 index format, with configurable per-field postings
 * and docvalues formats.
 * <p>
 * If you want to reuse functionality of this codec in another codec, extend
 * {@link FilterCodec}.
 *
 * @see org.apache.lucene.codecs.lucene50 package documentation for file format details.
 * @lucene.experimental
 */
public class Lucene50Codec extends Codec {
  private final StoredFieldsFormat fieldsFormat = new Lucene50StoredFieldsFormat();
  private final TermVectorsFormat vectorsFormat = new Lucene50TermVectorsFormat();
  private final FieldInfosFormat fieldInfosFormat = new Lucene50FieldInfosFormat();
  private final SegmentInfoFormat segmentInfosFormat = new Lucene50SegmentInfoFormat();
  private final LiveDocsFormat liveDocsFormat = new Lucene50LiveDocsFormat();
  
  private final PostingsFormat postingsFormat = new PerFieldPostingsFormat() {
    @Override
    public PostingsFormat getPostingsFormatForField(String field) {
      return Lucene50Codec.this.getPostingsFormatForField(field);
    }
  };
  
  private final DocValuesFormat docValuesFormat = new PerFieldDocValuesFormat() {
    @Override
    public DocValuesFormat getDocValuesFormatForField(String field) {
      return Lucene50Codec.this.getDocValuesFormatForField(field);
    }
  };

  /** Sole constructor. */
  public Lucene50Codec() {
    super("Lucene50");
  }
  
  @Override
  public final StoredFieldsFormat storedFieldsFormat() {
    return fieldsFormat;
  }
  
  @Override
  public final TermVectorsFormat termVectorsFormat() {
    return vectorsFormat;
  }

  @Override
  public final PostingsFormat postingsFormat() {
    return postingsFormat;
  }
  
  @Override
  public final FieldInfosFormat fieldInfosFormat() {
    return fieldInfosFormat;
  }
  
  @Override
  public final SegmentInfoFormat segmentInfoFormat() {
    return segmentInfosFormat;
  }
  
  @Override
  public final LiveDocsFormat liveDocsFormat() {
    return liveDocsFormat;
  }

  /** Returns the postings format that should be used for writing 
   *  new segments of <code>field</code>.
   *  
   *  The default implementation always returns "Lucene41"
   */
  public PostingsFormat getPostingsFormatForField(String field) {
    return defaultFormat;
  }
  
  /** Returns the docvalues format that should be used for writing 
   *  new segments of <code>field</code>.
   *  
   *  The default implementation always returns "Lucene410"
   */
  public DocValuesFormat getDocValuesFormatForField(String field) {
    return defaultDVFormat;
  }
  
  @Override
  public final DocValuesFormat docValuesFormat() {
    return docValuesFormat;
  }

  private final PostingsFormat defaultFormat = PostingsFormat.forName("Lucene41");
  private final DocValuesFormat defaultDVFormat = DocValuesFormat.forName("Lucene410");

  private final NormsFormat normsFormat = new Lucene50NormsFormat();

  @Override
  public final NormsFormat normsFormat() {
    return normsFormat;
  }
}
