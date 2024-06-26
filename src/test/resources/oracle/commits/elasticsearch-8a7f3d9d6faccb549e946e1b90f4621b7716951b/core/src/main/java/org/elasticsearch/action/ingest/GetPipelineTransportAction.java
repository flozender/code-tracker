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

package org.elasticsearch.action.ingest;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeReadAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.ingest.PipelineStore;
import org.elasticsearch.node.service.NodeService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class GetPipelineTransportAction extends TransportMasterNodeReadAction<GetPipelineRequest, GetPipelineResponse> {

    private final PipelineStore pipelineStore;

    @Inject
    public GetPipelineTransportAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                      TransportService transportService, ActionFilters actionFilters,
                                      IndexNameExpressionResolver indexNameExpressionResolver, NodeService nodeService) {
        super(settings, GetPipelineAction.NAME, transportService, clusterService, threadPool, actionFilters, indexNameExpressionResolver, GetPipelineRequest::new);
        this.pipelineStore = nodeService.getIngestService().getPipelineStore();
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected GetPipelineResponse newResponse() {
        return new GetPipelineResponse();
    }

    @Override
    protected void masterOperation(GetPipelineRequest request, ClusterState state, ActionListener<GetPipelineResponse> listener) throws Exception {
        listener.onResponse(new GetPipelineResponse(pipelineStore.getPipelines(request.getIds())));
    }

    @Override
    protected ClusterBlockException checkBlock(GetPipelineRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }

}
