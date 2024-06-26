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

package org.elasticsearch.action.admin.cluster.repositories.delete;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.TransportMasterNodeOperationAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.repositories.RepositoriesService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

/**
 * Transport action for unregister repository operation
 */
public class TransportDeleteRepositoryAction extends TransportMasterNodeOperationAction<DeleteRepositoryRequest, DeleteRepositoryResponse> {

    private final RepositoriesService repositoriesService;

    @Inject
    public TransportDeleteRepositoryAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                           RepositoriesService repositoriesService, ThreadPool threadPool) {
        super(settings, transportService, clusterService, threadPool);
        this.repositoriesService = repositoriesService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected String transportAction() {
        return DeleteRepositoryAction.NAME;
    }

    @Override
    protected DeleteRepositoryRequest newRequest() {
        return new DeleteRepositoryRequest();
    }

    @Override
    protected DeleteRepositoryResponse newResponse() {
        return new DeleteRepositoryResponse();
    }

    @Override
    protected ClusterBlockException checkBlock(DeleteRepositoryRequest request, ClusterState state) {
        return state.blocks().indexBlockedException(ClusterBlockLevel.METADATA, "");
    }

    @Override
    protected void masterOperation(final DeleteRepositoryRequest request, ClusterState state, final ActionListener<DeleteRepositoryResponse> listener) throws ElasticsearchException {
        repositoriesService.unregisterRepository(
                new RepositoriesService.UnregisterRepositoryRequest("delete_repository [" + request.name() + "]", request.name())
                        .masterNodeTimeout(request.masterNodeTimeout()).ackTimeout(request.timeout()),
                new ActionListener<RepositoriesService.UnregisterRepositoryResponse>() {

                    @Override
                    public void onResponse(RepositoriesService.UnregisterRepositoryResponse unregisterRepositoryResponse) {
                        listener.onResponse(new DeleteRepositoryResponse(unregisterRepositoryResponse.isAcknowledged()));
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        listener.onFailure(e);
                    }
                });
    }
}
