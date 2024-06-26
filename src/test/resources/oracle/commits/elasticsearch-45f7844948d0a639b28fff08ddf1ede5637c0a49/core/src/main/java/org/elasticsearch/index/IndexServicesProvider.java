/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

package org.elasticsearch.index;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.index.cache.IndexCache;
import org.elasticsearch.index.engine.EngineFactory;
import org.elasticsearch.index.fielddata.IndexFieldDataService;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.shard.IndexEventListener;
import org.elasticsearch.index.similarity.SimilarityService;
import org.elasticsearch.index.termvectors.TermVectorsService;
import org.elasticsearch.indices.IndicesWarmer;
import org.elasticsearch.indices.cache.query.IndicesQueryCache;
import org.elasticsearch.indices.memory.IndexingMemoryController;
import org.elasticsearch.indices.query.IndicesQueriesRegistry;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ThreadPool;

/**
 * Simple provider class that holds the Index and Node level services used by
 * a shard.
 * This is just a temporary solution until we cleaned up index creation and removed injectors on that level as well.
 */
public final class IndexServicesProvider {

    private final ThreadPool threadPool;
    private final MapperService mapperService;
    private final IndexCache indexCache;
    private final IndicesQueryCache indicesQueryCache;
    private final TermVectorsService termVectorsService;
    private final IndexFieldDataService indexFieldDataService;
    private final IndicesWarmer warmer;
    private final SimilarityService similarityService;
    private final EngineFactory factory;
    private final BigArrays bigArrays;
    private final IndexingMemoryController indexingMemoryController;
    private final IndexEventListener listener;
    private final Client client;
    private final IndicesQueriesRegistry indicesQueriesRegistry;
    private final ScriptService scriptService;

    @Inject
    public IndexServicesProvider(IndexEventListener listener, ThreadPool threadPool, MapperService mapperService, IndexCache indexCache, IndicesQueryCache indicesQueryCache, TermVectorsService termVectorsService, IndexFieldDataService indexFieldDataService, @Nullable IndicesWarmer warmer, SimilarityService similarityService, EngineFactory factory, BigArrays bigArrays, IndexingMemoryController indexingMemoryController, Client client, ScriptService scriptService, IndicesQueriesRegistry indicesQueriesRegistry) {
        this.listener = listener;
        this.threadPool = threadPool;
        this.mapperService = mapperService;
        this.indexCache = indexCache;
        this.indicesQueryCache = indicesQueryCache;
        this.termVectorsService = termVectorsService;
        this.indexFieldDataService = indexFieldDataService;
        this.warmer = warmer;
        this.similarityService = similarityService;
        this.factory = factory;
        this.bigArrays = bigArrays;
        this.indexingMemoryController = indexingMemoryController;
        this.client = client;
        this.indicesQueriesRegistry = indicesQueriesRegistry;
        this.scriptService = scriptService;
    }

    public IndexEventListener getIndexEventListener() {
        return listener;
    }
    public ThreadPool getThreadPool() {
        return threadPool;
    }

    public MapperService getMapperService() {
        return mapperService;
    }

    public IndexCache getIndexCache() {
        return indexCache;
    }

    public IndicesQueryCache getIndicesQueryCache() {
        return indicesQueryCache;
    }

    public TermVectorsService getTermVectorsService() {
        return termVectorsService;
    }

    public IndexFieldDataService getIndexFieldDataService() {
        return indexFieldDataService;
    }

    public IndicesWarmer getWarmer() {
        return warmer;
    }

    public SimilarityService getSimilarityService() {
        return similarityService;
    }

    public EngineFactory getFactory() {
        return factory;
    }

    public BigArrays getBigArrays() { return bigArrays; }

    public Client getClient() {
        return client;
    }

    public IndicesQueriesRegistry getIndicesQueriesRegistry() {
        return indicesQueriesRegistry;
    }

    public ScriptService getScriptService() {
        return scriptService;
    }

    public IndexingMemoryController getIndexingMemoryController() {
        return indexingMemoryController;
    }
}
