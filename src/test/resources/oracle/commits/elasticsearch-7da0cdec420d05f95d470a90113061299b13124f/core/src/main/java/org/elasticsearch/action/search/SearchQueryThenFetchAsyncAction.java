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

package org.elasticsearch.action.search;

import com.carrotsearch.hppc.IntArrayList;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.lucene.search.ScoreDoc;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRunnable;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.util.concurrent.AtomicArray;
import org.elasticsearch.search.SearchShardTarget;
import org.elasticsearch.search.action.SearchTransportService;
import org.elasticsearch.search.controller.SearchPhaseController;
import org.elasticsearch.search.fetch.FetchSearchResult;
import org.elasticsearch.search.fetch.ShardFetchSearchRequest;
import org.elasticsearch.search.internal.InternalSearchResponse;
import org.elasticsearch.search.internal.ShardSearchTransportRequest;
import org.elasticsearch.search.query.QuerySearchResultProvider;
import org.elasticsearch.threadpool.ThreadPool;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

class SearchQueryThenFetchAsyncAction extends AbstractSearchAsyncAction<QuerySearchResultProvider> {

    final AtomicArray<FetchSearchResult> fetchResults;
    final AtomicArray<IntArrayList> docIdsToLoad;

    SearchQueryThenFetchAsyncAction(Logger logger, SearchTransportService searchService,
                                            ClusterService clusterService, IndexNameExpressionResolver indexNameExpressionResolver,
                                            SearchPhaseController searchPhaseController, ThreadPool threadPool,
                                            SearchRequest request, ActionListener<SearchResponse> listener) {
        super(logger, searchService, clusterService, indexNameExpressionResolver, searchPhaseController, threadPool, request, listener);
        fetchResults = new AtomicArray<>(firstResults.length());
        docIdsToLoad = new AtomicArray<>(firstResults.length());
    }

    @Override
    protected String firstPhaseName() {
        return "query";
    }

    @Override
    protected void sendExecuteFirstPhase(DiscoveryNode node, ShardSearchTransportRequest request,
                                         ActionListener<QuerySearchResultProvider> listener) {
        searchTransportService.sendExecuteQuery(node, request, listener);
    }

    @Override
    protected void moveToSecondPhase() throws Exception {
        final boolean isScrollRequest = request.scroll() != null;
        sortedShardDocs = searchPhaseController.sortDocs(isScrollRequest, firstResults);
        searchPhaseController.fillDocIdsToLoad(docIdsToLoad, sortedShardDocs);

        if (docIdsToLoad.asList().isEmpty()) {
            finishHim();
            return;
        }

        final ScoreDoc[] lastEmittedDocPerShard = isScrollRequest ?
            searchPhaseController.getLastEmittedDocPerShard(firstResults.asList(), sortedShardDocs, firstResults.length()) : null;
        final AtomicInteger counter = new AtomicInteger(docIdsToLoad.asList().size());
        for (AtomicArray.Entry<IntArrayList> entry : docIdsToLoad.asList()) {
            QuerySearchResultProvider queryResult = firstResults.get(entry.index);
            DiscoveryNode node = nodes.get(queryResult.shardTarget().nodeId());
            ShardFetchSearchRequest fetchSearchRequest = createFetchRequest(queryResult.queryResult(), entry, lastEmittedDocPerShard);
            executeFetch(entry.index, queryResult.shardTarget(), counter, fetchSearchRequest, node);
        }
    }

    void executeFetch(final int shardIndex, final SearchShardTarget shardTarget, final AtomicInteger counter,
                      final ShardFetchSearchRequest fetchSearchRequest, DiscoveryNode node) {
        searchTransportService.sendExecuteFetch(node, fetchSearchRequest, new ActionListener<FetchSearchResult>() {
            @Override
            public void onResponse(FetchSearchResult result) {
                result.shardTarget(shardTarget);
                fetchResults.set(shardIndex, result);
                if (counter.decrementAndGet() == 0) {
                    finishHim();
                }
            }

            @Override
            public void onFailure(Exception t) {
                // the search context might not be cleared on the node where the fetch was executed for example
                // because the action was rejected by the thread pool. in this case we need to send a dedicated
                // request to clear the search context. by setting docIdsToLoad to null, the context will be cleared
                // in TransportSearchTypeAction.releaseIrrelevantSearchContexts() after the search request is done.
                docIdsToLoad.set(shardIndex, null);
                onFetchFailure(t, fetchSearchRequest, shardIndex, shardTarget, counter);
            }
        });
    }

    void onFetchFailure(Exception e, ShardFetchSearchRequest fetchSearchRequest, int shardIndex, SearchShardTarget shardTarget,
                        AtomicInteger counter) {
        if (logger.isDebugEnabled()) {
            logger.debug(new ParameterizedMessage("[{}] Failed to execute fetch phase", fetchSearchRequest.id()), e);
        }
        this.addShardFailure(shardIndex, shardTarget, e);
        successfulOps.decrementAndGet();
        if (counter.decrementAndGet() == 0) {
            finishHim();
        }
    }

    private void finishHim() {
        threadPool.executor(ThreadPool.Names.SEARCH).execute(new ActionRunnable<SearchResponse>(listener) {
            @Override
            public void doRun() throws IOException {
                final boolean isScrollRequest = request.scroll() != null;
                final InternalSearchResponse internalResponse = searchPhaseController.merge(isScrollRequest, sortedShardDocs, firstResults,
                    fetchResults);
                String scrollId = isScrollRequest ? TransportSearchHelper.buildScrollId(request.searchType(), firstResults) : null;
                listener.onResponse(new SearchResponse(internalResponse, scrollId, expectedSuccessfulOps,
                    successfulOps.get(), buildTookInMillis(), buildShardFailures()));
                releaseIrrelevantSearchContexts(firstResults, docIdsToLoad);
            }

            @Override
            public void onFailure(Exception e) {
                try {
                    ReduceSearchPhaseException failure = new ReduceSearchPhaseException("fetch", "", e, buildShardFailures());
                    if (logger.isDebugEnabled()) {
                        logger.debug("failed to reduce search", failure);
                    }
                    super.onFailure(failure);
                } finally {
                    releaseIrrelevantSearchContexts(firstResults, docIdsToLoad);
                }
            }
        });
    }
}
