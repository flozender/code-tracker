/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.search.facets.histogram;

import org.apache.lucene.index.IndexReader;
import org.elasticsearch.index.cache.field.data.FieldDataCache;
import org.elasticsearch.index.field.data.FieldData;
import org.elasticsearch.index.field.data.NumericFieldData;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.search.facets.Facet;
import org.elasticsearch.search.facets.FacetPhaseExecutionException;
import org.elasticsearch.search.facets.support.AbstractFacetCollector;
import org.elasticsearch.util.gnu.trove.TLongDoubleHashMap;
import org.elasticsearch.util.gnu.trove.TLongLongHashMap;

import java.io.IOException;

import static org.elasticsearch.index.field.data.FieldDataOptions.*;

/**
 * A histogram facet collector that uses different fields for the key and the value.
 *
 * @author kimchy (shay.banon)
 */
public class KeyValueHistogramFacetCollector extends AbstractFacetCollector {

    private final String keyFieldName;
    private final String keyIndexFieldName;

    private final String valueFieldName;
    private final String valueIndexFieldName;

    private final long interval;

    private final HistogramFacet.ComparatorType comparatorType;

    private final FieldDataCache fieldDataCache;

    private final FieldData.Type keyFieldDataType;
    private NumericFieldData keyFieldData;

    private final FieldData.Type valueFieldDataType;
    private NumericFieldData valueFieldData;

    private final TLongLongHashMap counts = new TLongLongHashMap();
    private final TLongDoubleHashMap totals = new TLongDoubleHashMap();

    public KeyValueHistogramFacetCollector(String facetName, String keyFieldName, String valueFieldName, long interval, HistogramFacet.ComparatorType comparatorType, FieldDataCache fieldDataCache, MapperService mapperService) {
        super(facetName);
        this.keyFieldName = keyFieldName;
        this.valueFieldName = valueFieldName;
        this.interval = interval;
        this.comparatorType = comparatorType;
        this.fieldDataCache = fieldDataCache;

        FieldMapper mapper = mapperService.smartNameFieldMapper(keyFieldName);
        if (mapper == null) {
            throw new FacetPhaseExecutionException(facetName, "No mapping found for key_field [" + keyFieldName + "]");
        }
        keyIndexFieldName = mapper.names().indexName();
        keyFieldDataType = mapper.fieldDataType();

        mapper = mapperService.smartNameFieldMapper(valueFieldName);
        if (mapper == null) {
            throw new FacetPhaseExecutionException(facetName, "No mapping found for value_field [" + valueFieldName + "]");
        }
        valueIndexFieldName = mapper.names().indexName();
        valueFieldDataType = mapper.fieldDataType();
    }

    @Override protected void doCollect(int doc) throws IOException {
        if (keyFieldData.multiValued()) {
            if (valueFieldData.multiValued()) {
                // both multi valued, intersect based on the minimum size
                double[] keys = keyFieldData.doubleValues(doc);
                double[] values = valueFieldData.doubleValues(doc);
                int size = Math.min(keys.length, values.length);
                for (int i = 0; i < size; i++) {
                    long bucket = HistogramFacetCollector.bucket(keys[i], interval);
                    counts.adjustOrPutValue(bucket, 1, 1);
                    totals.adjustOrPutValue(bucket, values[i], values[i]);
                }
            } else {
                // key multi valued, value is a single value
                double value = valueFieldData.doubleValue(doc);
                for (double key : keyFieldData.doubleValues(doc)) {
                    long bucket = HistogramFacetCollector.bucket(key, interval);
                    counts.adjustOrPutValue(bucket, 1, 1);
                    totals.adjustOrPutValue(bucket, value, value);
                }
            }
        } else {
            // single key value, compute the bucket once
            long bucket = HistogramFacetCollector.bucket(keyFieldData.doubleValue(doc), interval);
            if (valueFieldData.multiValued()) {
                for (double value : valueFieldData.doubleValues(doc)) {
                    counts.adjustOrPutValue(bucket, 1, 1);
                    totals.adjustOrPutValue(bucket, value, value);
                }
            } else {
                // both key and value are not multi valued
                double value = valueFieldData.doubleValue(doc);
                counts.adjustOrPutValue(bucket, 1, 1);
                totals.adjustOrPutValue(bucket, value, value);
            }
        }
    }

    @Override protected void doSetNextReader(IndexReader reader, int docBase) throws IOException {
        keyFieldData = (NumericFieldData) fieldDataCache.cache(keyFieldDataType, reader, keyIndexFieldName, fieldDataOptions().withFreqs(false));
        valueFieldData = (NumericFieldData) fieldDataCache.cache(valueFieldDataType, reader, valueIndexFieldName, fieldDataOptions().withFreqs(false));
    }

    @Override public Facet facet() {
        return new InternalHistogramFacet(facetName, keyFieldName, valueFieldName, interval, comparatorType, counts, totals);
    }
}