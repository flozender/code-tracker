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

package org.elasticsearch.action.bulk;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocumentRequest;
import org.elasticsearch.action.RoutingMissingException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.create.TransportCreateIndexAction;
import org.elasticsearch.action.delete.TransportDeleteAction;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.AutoCreateIndex;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.action.support.replication.TransportWriteAction;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.AtomicArray;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.indices.IndexClosedException;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

/**
 * Groups bulk request items by shard, optionally creating non-existent indices and
 * delegates to {@link TransportShardBulkAction} for shard-level bulk execution
 */
public class TransportBulkAction extends HandledTransportAction<BulkRequest, BulkResponse> {

    private final AutoCreateIndex autoCreateIndex;
    private final boolean allowIdGeneration;
    private final ClusterService clusterService;
    private final TransportShardBulkAction shardBulkAction;
    private final TransportCreateIndexAction createIndexAction;
    private final LongSupplier relativeTimeProvider;

    @Inject
    public TransportBulkAction(Settings settings, ThreadPool threadPool, TransportService transportService, ClusterService clusterService,
                               TransportShardBulkAction shardBulkAction, TransportCreateIndexAction createIndexAction,
                               ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                               AutoCreateIndex autoCreateIndex) {
        this(settings, threadPool, transportService, clusterService,
                shardBulkAction, createIndexAction,
                actionFilters, indexNameExpressionResolver,
                autoCreateIndex,
                System::nanoTime);
    }

    public TransportBulkAction(Settings settings, ThreadPool threadPool, TransportService transportService, ClusterService clusterService,
                               TransportShardBulkAction shardBulkAction, TransportCreateIndexAction createIndexAction,
                               ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                               AutoCreateIndex autoCreateIndex, LongSupplier relativeTimeProvider) {
        super(settings, BulkAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver, BulkRequest::new);
        Objects.requireNonNull(relativeTimeProvider);
        this.clusterService = clusterService;
        this.shardBulkAction = shardBulkAction;
        this.createIndexAction = createIndexAction;

        this.autoCreateIndex = autoCreateIndex;
        this.allowIdGeneration = this.settings.getAsBoolean("action.bulk.action.allow_id_generation", true);
        this.relativeTimeProvider = relativeTimeProvider;
    }

    @Override
    protected final void doExecute(final BulkRequest bulkRequest, final ActionListener<BulkResponse> listener) {
        throw new UnsupportedOperationException("task parameter is required for this operation");
    }

    @Override
    protected void doExecute(Task task, BulkRequest bulkRequest, ActionListener<BulkResponse> listener) {
        final long startTime = relativeTime();
        final AtomicArray<BulkItemResponse> responses = new AtomicArray<>(bulkRequest.requests.size());

        if (needToCheck()) {
            // Keep track of all unique indices and all unique types per index for the create index requests:
            final Set<String> autoCreateIndices = bulkRequest.requests.stream()
                .map(DocumentRequest::index)
                .collect(Collectors.toSet());
            final AtomicInteger counter = new AtomicInteger(autoCreateIndices.size());
            ClusterState state = clusterService.state();
            for (String index : autoCreateIndices) {
                if (shouldAutoCreate(index, state)) {
                    CreateIndexRequest createIndexRequest = new CreateIndexRequest();
                    createIndexRequest.index(index);
                    createIndexRequest.cause("auto(bulk api)");
                    createIndexRequest.masterNodeTimeout(bulkRequest.timeout());
                    createIndexAction.execute(createIndexRequest, new ActionListener<CreateIndexResponse>() {
                        @Override
                        public void onResponse(CreateIndexResponse result) {
                            if (counter.decrementAndGet() == 0) {
                                try {
                                    executeBulk(task, bulkRequest, startTime, listener, responses);
                                } catch (Exception e) {
                                    listener.onFailure(e);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (!(ExceptionsHelper.unwrapCause(e) instanceof IndexAlreadyExistsException)) {
                                // fail all requests involving this index, if create didnt work
                                for (int i = 0; i < bulkRequest.requests.size(); i++) {
                                    DocumentRequest<?> request = bulkRequest.requests.get(i);
                                    if (request != null && setResponseFailureIfIndexMatches(responses, i, request, index, e)) {
                                        bulkRequest.requests.set(i, null);
                                    }
                                }
                            }
                            if (counter.decrementAndGet() == 0) {
                                try {
                                    executeBulk(task, bulkRequest, startTime, listener, responses);
                                } catch (Exception inner) {
                                    inner.addSuppressed(e);
                                    listener.onFailure(inner);
                                }
                            }
                        }
                    });
                } else {
                    if (counter.decrementAndGet() == 0) {
                        executeBulk(task, bulkRequest, startTime, listener, responses);
                    }
                }
            }
        } else {
            executeBulk(task, bulkRequest, startTime, listener, responses);
        }
    }

    boolean needToCheck() {
        return autoCreateIndex.needToCheck();
    }

    boolean shouldAutoCreate(String index, ClusterState state) {
        return autoCreateIndex.shouldAutoCreate(index, state);
    }

    private boolean setResponseFailureIfIndexMatches(AtomicArray<BulkItemResponse> responses, int idx, DocumentRequest<?> request, String index, Exception e) {
        if (index.equals(request.index())) {
            responses.set(idx, new BulkItemResponse(idx, request.opType(), new BulkItemResponse.Failure(request.index(), request.type(), request.id(), e)));
            return true;
        }
        return false;
    }

    /**
     * This method executes the {@link BulkRequest} and calls the given listener once the request returns.
     * This method will not create any indices even if auto-create indices is enabled.
     *
     * @see #doExecute(BulkRequest, org.elasticsearch.action.ActionListener)
     */
    public void executeBulk(final BulkRequest bulkRequest, final ActionListener<BulkResponse> listener) {
        final long startTimeNanos = relativeTime();
        executeBulk(null, bulkRequest, startTimeNanos, listener, new AtomicArray<>(bulkRequest.requests.size()));
    }

    private long buildTookInMillis(long startTimeNanos) {
        return TimeUnit.NANOSECONDS.toMillis(relativeTime() - startTimeNanos);
    }

    void executeBulk(Task task, final BulkRequest bulkRequest, final long startTimeNanos, final ActionListener<BulkResponse> listener, final AtomicArray<BulkItemResponse> responses ) {
        final ClusterState clusterState = clusterService.state();
        // TODO use timeout to wait here if its blocked...
        clusterState.blocks().globalBlockedRaiseException(ClusterBlockLevel.WRITE);

        final ConcreteIndices concreteIndices = new ConcreteIndices(clusterState, indexNameExpressionResolver);
        MetaData metaData = clusterState.metaData();
        for (int i = 0; i < bulkRequest.requests.size(); i++) {
            DocumentRequest documentRequest = bulkRequest.requests.get(i);
            //the request can only be null because we set it to null in the previous step, so it gets ignored
            if (documentRequest == null) {
                continue;
            }
            if (addFailureIfIndexIsUnavailable(documentRequest, bulkRequest, responses, i, concreteIndices, metaData)) {
                continue;
            }
            Index concreteIndex = concreteIndices.resolveIfAbsent(documentRequest);
            try {
                switch (documentRequest.opType()) {
                    case CREATE:
                    case INDEX:
                        IndexRequest indexRequest = (IndexRequest) documentRequest;
                        MappingMetaData mappingMd = null;
                        final IndexMetaData indexMetaData = metaData.index(concreteIndex);
                        if (indexMetaData != null) {
                            mappingMd = indexMetaData.mappingOrDefault(indexRequest.type());
                        }
                        indexRequest.resolveRouting(metaData);
                        indexRequest.process(mappingMd, allowIdGeneration, concreteIndex.getName());
                        break;
                    case UPDATE:
                    case DELETE:
                        TransportWriteAction.resolveAndValidateRouting(metaData, concreteIndex.getName(), documentRequest);
                        break;
                    default: throw new AssertionError("request type not supported: [" + documentRequest.opType() + "]");
                }
            } catch (ElasticsearchParseException | RoutingMissingException e) {
                BulkItemResponse.Failure failure = new BulkItemResponse.Failure(concreteIndex.getName(), documentRequest.type(), documentRequest.id(), e);
                BulkItemResponse bulkItemResponse = new BulkItemResponse(i, documentRequest.opType(), failure);
                responses.set(i, bulkItemResponse);
                // make sure the request gets never processed again
                bulkRequest.requests.set(i, null);
            }
        }

        // first, go over all the requests and create a ShardId -> Operations mapping
        Map<ShardId, List<BulkItemRequest>> requestsByShard = new HashMap<>();
        for (int i = 0; i < bulkRequest.requests.size(); i++) {
            DocumentRequest<?> request = bulkRequest.requests.get(i);
            if (request == null) {
                continue;
            }
            String concreteIndex = concreteIndices.getConcreteIndex(request.index()).getName();
            ShardId shardId = clusterService.operationRouting().indexShards(clusterState, concreteIndex, request.id(), request.routing()).shardId();
            List<BulkItemRequest> shardRequests = requestsByShard.computeIfAbsent(shardId, shard -> new ArrayList<>());
            shardRequests.add(new BulkItemRequest(i, request));
        }

        if (requestsByShard.isEmpty()) {
            listener.onResponse(new BulkResponse(responses.toArray(new BulkItemResponse[responses.length()]), buildTookInMillis(startTimeNanos)));
            return;
        }

        final AtomicInteger counter = new AtomicInteger(requestsByShard.size());
        String nodeId = clusterService.localNode().getId();
        for (Map.Entry<ShardId, List<BulkItemRequest>> entry : requestsByShard.entrySet()) {
            final ShardId shardId = entry.getKey();
            final List<BulkItemRequest> requests = entry.getValue();
            BulkShardRequest bulkShardRequest = new BulkShardRequest(shardId, bulkRequest.getRefreshPolicy(),
                    requests.toArray(new BulkItemRequest[requests.size()]));
            bulkShardRequest.waitForActiveShards(bulkRequest.waitForActiveShards());
            bulkShardRequest.timeout(bulkRequest.timeout());
            if (task != null) {
                bulkShardRequest.setParentTask(nodeId, task.getId());
            }
            shardBulkAction.execute(bulkShardRequest, new ActionListener<BulkShardResponse>() {
                @Override
                public void onResponse(BulkShardResponse bulkShardResponse) {
                    for (BulkItemResponse bulkItemResponse : bulkShardResponse.getResponses()) {
                        // we may have no response if item failed
                        if (bulkItemResponse.getResponse() != null) {
                            bulkItemResponse.getResponse().setShardInfo(bulkShardResponse.getShardInfo());
                        }
                        responses.set(bulkItemResponse.getItemId(), bulkItemResponse);
                    }
                    if (counter.decrementAndGet() == 0) {
                        finishHim();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    // create failures for all relevant requests
                    for (BulkItemRequest request : requests) {
                        final String indexName = concreteIndices.getConcreteIndex(request.index()).getName();
                        DocumentRequest<?> documentRequest = request.request();
                        responses.set(request.id(), new BulkItemResponse(request.id(), documentRequest.opType(),
                            new BulkItemResponse.Failure(indexName, documentRequest.type(), documentRequest.id(), e)));
                    }
                    if (counter.decrementAndGet() == 0) {
                        finishHim();
                    }
                }

                private void finishHim() {
                    listener.onResponse(new BulkResponse(responses.toArray(new BulkItemResponse[responses.length()]), buildTookInMillis(startTimeNanos)));
                }
            });
        }
    }

    private boolean addFailureIfIndexIsUnavailable(DocumentRequest request, BulkRequest bulkRequest, AtomicArray<BulkItemResponse> responses, int idx,
                                              final ConcreteIndices concreteIndices,
                                              final MetaData metaData) {
        Index concreteIndex = concreteIndices.getConcreteIndex(request.index());
        Exception unavailableException = null;
        if (concreteIndex == null) {
            try {
                concreteIndex = concreteIndices.resolveIfAbsent(request);
            } catch (IndexClosedException | IndexNotFoundException ex) {
                // Fix for issue where bulk request references an index that
                // cannot be auto-created see issue #8125
                unavailableException = ex;
            }
        }
        if (unavailableException == null) {
            IndexMetaData indexMetaData = metaData.getIndexSafe(concreteIndex);
            if (indexMetaData.getState() == IndexMetaData.State.CLOSE) {
                unavailableException = new IndexClosedException(concreteIndex);
            }
        }
        if (unavailableException != null) {
            BulkItemResponse.Failure failure = new BulkItemResponse.Failure(request.index(), request.type(), request.id(),
                    unavailableException);
            BulkItemResponse bulkItemResponse = new BulkItemResponse(idx, request.opType(), failure);
            responses.set(idx, bulkItemResponse);
            // make sure the request gets never processed again
            bulkRequest.requests.set(idx, null);
            return true;
        }
        return false;
    }

    private static class ConcreteIndices  {
        private final ClusterState state;
        private final IndexNameExpressionResolver indexNameExpressionResolver;
        private final Map<String, Index> indices = new HashMap<>();

        ConcreteIndices(ClusterState state, IndexNameExpressionResolver indexNameExpressionResolver) {
            this.state = state;
            this.indexNameExpressionResolver = indexNameExpressionResolver;
        }

        Index getConcreteIndex(String indexOrAlias) {
            return indices.get(indexOrAlias);
        }

        Index resolveIfAbsent(DocumentRequest request) {
            Index concreteIndex = indices.get(request.index());
            if (concreteIndex == null) {
                concreteIndex = indexNameExpressionResolver.concreteSingleIndex(state, request);
                indices.put(request.index(), concreteIndex);
            }
            return concreteIndex;
        }
    }

    private long relativeTime() {
        return relativeTimeProvider.getAsLong();
    }

}
