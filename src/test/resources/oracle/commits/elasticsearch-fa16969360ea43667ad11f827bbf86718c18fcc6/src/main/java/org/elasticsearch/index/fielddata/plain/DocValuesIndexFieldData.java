/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
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

package org.elasticsearch.index.fielddata.plain;

import com.google.common.collect.ImmutableSet;
import org.apache.lucene.index.IndexReader;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.fielddata.IndexFieldData;
import org.elasticsearch.index.fielddata.IndexFieldDataCache;
import org.elasticsearch.index.fielddata.IndexNumericFieldData.NumericType;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.FieldMapper.Names;
import org.elasticsearch.index.mapper.internal.IdFieldMapper;
import org.elasticsearch.index.mapper.internal.TimestampFieldMapper;
import org.elasticsearch.index.mapper.internal.UidFieldMapper;
import org.elasticsearch.indices.fielddata.breaker.CircuitBreakerService;

import java.util.Map;
import java.util.Set;

/** {@link IndexFieldData} impl based on Lucene's doc values. Caching is done on the Lucene side. */
public abstract class DocValuesIndexFieldData {

    protected final Index index;
    protected final Names fieldNames;

    public DocValuesIndexFieldData(Index index, Names fieldNames) {
        super();
        this.index = index;
        this.fieldNames = fieldNames;
    }

    public final Names getFieldNames() {
        return fieldNames;
    }

    public final void clear() {
        // can't do
    }

    public final void clear(IndexReader reader) {
        // can't do
    }

    public final Index index() {
        return index;
    }

    public static class Builder implements IndexFieldData.Builder {

        private static final Set<String> BINARY_INDEX_FIELD_NAMES = ImmutableSet.of(UidFieldMapper.NAME, IdFieldMapper.NAME);
        private static final Set<String> NUMERIC_INDEX_FIELD_NAMES = ImmutableSet.of(TimestampFieldMapper.NAME);

        private NumericType numericType;

        public Builder numericType(NumericType type) {
            this.numericType = type;
            return this;
        }

        @Override
        public IndexFieldData<?> build(Index index, Settings indexSettings, FieldMapper<?> mapper, IndexFieldDataCache cache,
                                       CircuitBreakerService breakerService) {
            // Ignore Circuit Breaker
            final FieldMapper.Names fieldNames = mapper.names();
            final Settings fdSettings = mapper.fieldDataType().getSettings();
            final Map<String, Settings> filter = fdSettings.getGroups("filter");
            if (filter != null && !filter.isEmpty()) {
                throw new ElasticsearchIllegalArgumentException("Doc values field data doesn't support filters [" + fieldNames.name() + "]");
            }

            if (BINARY_INDEX_FIELD_NAMES.contains(fieldNames.indexName())) {
                assert numericType == null;
                return new BinaryDVIndexFieldData(index, fieldNames);
            } else if (NUMERIC_INDEX_FIELD_NAMES.contains(fieldNames.indexName())) {
                assert !numericType.isFloatingPoint();
                return new NumericDVIndexFieldData(index, fieldNames);
            } else if (numericType != null) {
                return new BinaryDVNumericIndexFieldData(index, fieldNames, numericType);
            } else {
                return new SortedSetDVBytesIndexFieldData(index, fieldNames);
            }
        }

    }

}
