package org.apache.lucene.codecs.lucene40;

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

import org.apache.lucene.codecs.SegmentInfoFormat;
import org.apache.lucene.codecs.SegmentInfoReader;
import org.apache.lucene.codecs.SegmentInfoWriter;
import org.apache.lucene.index.SegmentInfo;

/**
 * Lucene 4.0 Segment info format.
 * @deprecated Only for reading old 4.0-4.5 segments
 */
@Deprecated
public class Lucene40SegmentInfoFormat extends SegmentInfoFormat {
  private final SegmentInfoReader reader = new Lucene40SegmentInfoReader();

  /** Sole constructor. */
  public Lucene40SegmentInfoFormat() {
  }
  
  @Override
  public final SegmentInfoReader getSegmentInfoReader() {
    return reader;
  }

  @Override
  public SegmentInfoWriter getSegmentInfoWriter() {
    throw new UnsupportedOperationException("this codec can only be used for reading");
  }

  /** File extension used to store {@link SegmentInfo}. */
  static final String SI_EXTENSION = "si";
  static final String CODEC_NAME = "Lucene40SegmentInfo";
  static final int VERSION_START = 0;
  static final int VERSION_CURRENT = VERSION_START;
}
