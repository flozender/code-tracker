package org.apache.lucene.facet.range;

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

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.queries.function.valuesource.LongFieldSource;

/** Base class for range faceting.
 *
 *  @lucene.experimental */
abstract class RangeFacetCounts extends Facets {
  protected final Range[] ranges;
  protected final int[] counts;
  protected final String field;
  protected int totCount;

  /** Create {@code RangeFacetCounts}, using {@link
   *  LongFieldSource} from the specified field. */
  protected RangeFacetCounts(String field, Range[] ranges) throws IOException {
    this.field = field;
    this.ranges = ranges;
    counts = new int[ranges.length];
  }

  @Override
  public FacetResult getTopChildren(int topN, String dim, String... path) {
    if (dim.equals(field) == false) {
      throw new IllegalArgumentException("invalid dim \"" + dim + "\"; should be \"" + field + "\"");
    }
    if (path.length != 0) {
      throw new IllegalArgumentException("path.length should be 0");
    }
    LabelAndValue[] labelValues = new LabelAndValue[counts.length];
    for(int i=0;i<counts.length;i++) {
      labelValues[i] = new LabelAndValue(ranges[i].label, counts[i]);
    }
    return new FacetResult(dim, path, totCount, labelValues, labelValues.length);
  }

  @Override
  public Number getSpecificValue(String dim, String... path) throws IOException {
    // TODO: should we impl this?
    throw new UnsupportedOperationException();
  }

  @Override
  public List<FacetResult> getAllDims(int topN) throws IOException {
    return Collections.singletonList(getTopChildren(topN, null));
  }
}
