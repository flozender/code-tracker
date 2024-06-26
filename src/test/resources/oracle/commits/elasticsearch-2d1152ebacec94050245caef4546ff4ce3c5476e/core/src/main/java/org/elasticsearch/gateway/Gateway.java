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

package org.elasticsearch.gateway;

import com.carrotsearch.hppc.ObjectFloatHashMap;
import com.carrotsearch.hppc.ObjectHashSet;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import org.apache.lucene.util.IOUtils;
import org.elasticsearch.action.FailedNodeException;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.discovery.Discovery;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.index.Index;

import java.nio.file.Path;
import java.util.function.Supplier;

/**
 *
 */
public class Gateway extends AbstractComponent implements ClusterStateListener {

    private final ClusterService clusterService;

    private final NodeEnvironment nodeEnv;

    private final GatewayMetaState metaState;

    private final TransportNodesListGatewayMetaState listGatewayMetaState;

    private final Supplier<Integer> minimumMasterNodesProvider;

    public Gateway(Settings settings, ClusterService clusterService, NodeEnvironment nodeEnv, GatewayMetaState metaState,
                   TransportNodesListGatewayMetaState listGatewayMetaState, Discovery discovery) {
        super(settings);
        this.clusterService = clusterService;
        this.nodeEnv = nodeEnv;
        this.metaState = metaState;
        this.listGatewayMetaState = listGatewayMetaState;
        this.minimumMasterNodesProvider = discovery::getMinimumMasterNodes;
        clusterService.addLast(this);
    }

    public void performStateRecovery(final GatewayStateRecoveredListener listener) throws GatewayException {
        ObjectHashSet<String> nodesIds = new ObjectHashSet<>(clusterService.state().nodes().masterNodes().keys());
        logger.trace("performing state recovery from {}", nodesIds);
        TransportNodesListGatewayMetaState.NodesGatewayMetaState nodesState = listGatewayMetaState.list(nodesIds.toArray(String.class), null).actionGet();


        int requiredAllocation = Math.max(1, minimumMasterNodesProvider.get());


        if (nodesState.failures().length > 0) {
            for (FailedNodeException failedNodeException : nodesState.failures()) {
                logger.warn("failed to fetch state from node", failedNodeException);
            }
        }

        ObjectFloatHashMap<Index> indices = new ObjectFloatHashMap<>();
        MetaData electedGlobalState = null;
        int found = 0;
        for (TransportNodesListGatewayMetaState.NodeGatewayMetaState nodeState : nodesState) {
            if (nodeState.metaData() == null) {
                continue;
            }
            found++;
            if (electedGlobalState == null) {
                electedGlobalState = nodeState.metaData();
            } else if (nodeState.metaData().version() > electedGlobalState.version()) {
                electedGlobalState = nodeState.metaData();
            }
            for (ObjectCursor<IndexMetaData> cursor : nodeState.metaData().indices().values()) {
                indices.addTo(cursor.value.getIndex(), 1);
            }
        }
        if (found < requiredAllocation) {
            listener.onFailure("found [" + found + "] metadata states, required [" + requiredAllocation + "]");
            return;
        }
        // update the global state, and clean the indices, we elect them in the next phase
        MetaData.Builder metaDataBuilder = MetaData.builder(electedGlobalState).removeAllIndices();

        assert !indices.containsKey(null);
        final Object[] keys = indices.keys;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) {
                Index index = (Index) keys[i];
                IndexMetaData electedIndexMetaData = null;
                int indexMetaDataCount = 0;
                for (TransportNodesListGatewayMetaState.NodeGatewayMetaState nodeState : nodesState) {
                    if (nodeState.metaData() == null) {
                        continue;
                    }
                    IndexMetaData indexMetaData = nodeState.metaData().index(index);
                    if (indexMetaData == null) {
                        continue;
                    }
                    if (electedIndexMetaData == null) {
                        electedIndexMetaData = indexMetaData;
                    } else if (indexMetaData.getVersion() > electedIndexMetaData.getVersion()) {
                        electedIndexMetaData = indexMetaData;
                    }
                    indexMetaDataCount++;
                }
                if (electedIndexMetaData != null) {
                    if (indexMetaDataCount < requiredAllocation) {
                        logger.debug("[{}] found [{}], required [{}], not adding", index, indexMetaDataCount, requiredAllocation);
                    }
                    metaDataBuilder.put(electedIndexMetaData, false);
                }
            }
        }
        ClusterState.Builder builder = ClusterState.builder(clusterService.state().getClusterName());
        builder.metaData(metaDataBuilder);
        listener.onSuccess(builder.build());
    }
    public void reset() throws Exception {
        try {
            Path[] dataPaths = nodeEnv.nodeDataPaths();
            logger.trace("removing node data paths: [{}]", (Object)dataPaths);
            IOUtils.rm(dataPaths);
        } catch (Exception ex) {
            logger.debug("failed to delete shard locations", ex);
        }
    }

    @Override
    public void clusterChanged(final ClusterChangedEvent event) {
        // order is important, first metaState, and then shardsState
        // so dangling indices will be recorded
        metaState.clusterChanged(event);
    }

    public interface GatewayStateRecoveredListener {
        void onSuccess(ClusterState build);

        void onFailure(String s);
    }
}
