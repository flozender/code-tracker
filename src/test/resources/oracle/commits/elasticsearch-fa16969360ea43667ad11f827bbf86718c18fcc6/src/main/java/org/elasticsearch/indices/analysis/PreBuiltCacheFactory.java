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
package org.elasticsearch.indices.analysis;

import com.google.common.collect.Maps;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.Version;

import java.util.Map;

/**
 *
 */
public class PreBuiltCacheFactory {

    /**
     * The strategy of caching the analyzer
     *
     * ONE               Exactly one version is stored. Useful for analyzers which do not store version information
     * LUCENE            Exactly one version for each lucene version is stored. Useful to prevent different analyzers with the same version
     * ELASTICSEARCH     Exactly one version per elasticsearch version is stored. Useful if you change an analyzer between elasticsearch releases, when the lucene version does not change
     */
    static enum CachingStrategy { ONE, LUCENE, ELASTICSEARCH };

    public interface PreBuiltCache<T> {
        T get(Version version);
        void put(Version version, T t);
    }

    private PreBuiltCacheFactory() {}

    static <T> PreBuiltCache<T> getCache(CachingStrategy cachingStrategy) {
        switch (cachingStrategy) {
            case ONE:
                return new PreBuiltCacheStrategyOne<T>();
            case LUCENE:
                return new PreBuiltCacheStrategyLucene<T>();
            case ELASTICSEARCH:
                return new PreBuiltCacheStrategyElasticsearch<T>();
            default:
                throw new ElasticsearchException("No action configured for caching strategy[" + cachingStrategy + "]");
        }
    }

    /**
     * This is a pretty simple cache, it only contains one version
     */
    private static class PreBuiltCacheStrategyOne<T> implements PreBuiltCache<T> {

        private T model = null;

        @Override
        public T get(Version version) {
            return model;
        }

        @Override
        public void put(Version version, T model) {
            this.model = model;
        }
    }

    /**
     * This cache contains one version for each elasticsearch version object
     */
    private static class PreBuiltCacheStrategyElasticsearch<T> implements PreBuiltCache<T> {

        Map<Version, T> mapModel = Maps.newHashMapWithExpectedSize(2);

        @Override
        public T get(Version version) {
            return mapModel.get(version);
        }

        @Override
        public void put(Version version, T model) {
            mapModel.put(version, model);
        }
    }

    /**
     * This cache uses the lucene version for caching
     */
    private static class PreBuiltCacheStrategyLucene<T> implements PreBuiltCache<T> {

        private Map<org.apache.lucene.util.Version, T> mapModel = Maps.newHashMapWithExpectedSize(2);

        @Override
        public T get(Version version) {
            return mapModel.get(version.luceneVersion);
        }

        @Override
        public void put(org.elasticsearch.Version version, T model) {
            mapModel.put(version.luceneVersion, model);
        }
    }
}
