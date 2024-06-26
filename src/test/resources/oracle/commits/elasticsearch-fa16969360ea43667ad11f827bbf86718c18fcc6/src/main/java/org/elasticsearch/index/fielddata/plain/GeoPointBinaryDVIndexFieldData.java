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

import org.apache.lucene.index.AtomicReaderContext;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.fielddata.*;
import org.elasticsearch.index.fielddata.fieldcomparator.SortMode;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.FieldMapper.Names;
import org.elasticsearch.indices.fielddata.breaker.CircuitBreakerService;

import java.io.IOException;

public class GeoPointBinaryDVIndexFieldData extends DocValuesIndexFieldData implements IndexGeoPointFieldData<AtomicGeoPointFieldData<ScriptDocValues>> {

    public GeoPointBinaryDVIndexFieldData(Index index, Names fieldNames) {
        super(index, fieldNames);
    }

    @Override
    public boolean valuesOrdered() {
        return false;
    }

    @Override
    public final XFieldComparatorSource comparatorSource(@Nullable Object missingValue, SortMode sortMode) {
        throw new ElasticsearchIllegalArgumentException("can't sort on geo_point field without using specific sorting feature, like geo_distance");
    }

    @Override
    public AtomicGeoPointFieldData<ScriptDocValues> load(AtomicReaderContext context) {
        try {
            return new GeoPointBinaryDVAtomicFieldData(context.reader(), context.reader().getBinaryDocValues(fieldNames.indexName()));
        } catch (IOException e) {
            throw new ElasticsearchIllegalStateException("Cannot load doc values", e);
        }
    }

    @Override
    public AtomicGeoPointFieldData<ScriptDocValues> loadDirect(AtomicReaderContext context) throws Exception {
        return load(context);
    }

    public static class Builder implements IndexFieldData.Builder {

        @Override
        public IndexFieldData<?> build(Index index, Settings indexSettings, FieldMapper<?> mapper, IndexFieldDataCache cache,
                                       CircuitBreakerService breakerService) {
            // Ignore breaker
            final FieldMapper.Names fieldNames = mapper.names();
            return new GeoPointBinaryDVIndexFieldData(index, fieldNames);
        }

    }
}
