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

import org.apache.lucene.util.CollectionUtil;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.RoutingNodes;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.allocation.RoutingAllocation;
import org.elasticsearch.cluster.routing.allocation.decider.Decision;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The primary shard allocator allocates primary shard that were not created as
 * a result of an API to a node that held them last to be recovered.
 */
public abstract class PrimaryShardAllocator extends AbstractComponent {

    private static final Function<String, String> INITIAL_SHARDS_PARSER = (value) -> {
        switch (value) {
            case "quorum":
            case "quorum-1":
            case "half":
            case "one":
            case "full":
            case "full-1":
            case "all-1":
            case "all":
                return value;
            default:
                Integer.parseInt(value); // it can be parsed that's all we care here?
                return value;
        }
    };

    public static final Setting<String> NODE_INITIAL_SHARDS_SETTING = new Setting<>("gateway.initial_shards", (settings) -> settings.get("gateway.local.initial_shards", "quorum"), INITIAL_SHARDS_PARSER, true, Setting.Scope.CLUSTER);
    @Deprecated
    public static final Setting<String> INDEX_RECOVERY_INITIAL_SHARDS_SETTING = new Setting<>("index.recovery.initial_shards", (settings) -> NODE_INITIAL_SHARDS_SETTING.get(settings) , INITIAL_SHARDS_PARSER, true, Setting.Scope.INDEX);

    public PrimaryShardAllocator(Settings settings) {
        super(settings);
        logger.debug("using initial_shards [{}]", NODE_INITIAL_SHARDS_SETTING.get(settings));
    }

    public boolean allocateUnassigned(RoutingAllocation allocation) {
        boolean changed = false;
        final RoutingNodes routingNodes = allocation.routingNodes();
        final MetaData metaData = routingNodes.metaData();

        final RoutingNodes.UnassignedShards.UnassignedIterator unassignedIterator = routingNodes.unassigned().iterator();
        while (unassignedIterator.hasNext()) {
            final ShardRouting shard = unassignedIterator.next();

            if (shard.primary() == false) {
                continue;
            }

            final IndexMetaData indexMetaData = metaData.index(shard.getIndex());
            final IndexSettings indexSettings = new IndexSettings(indexMetaData, settings);

            if (shard.allocatedPostIndexCreate(indexMetaData) == false) {
                // when we create a fresh index
                continue;
            }

            final AsyncShardFetch.FetchResult<TransportNodesListGatewayStartedShards.NodeGatewayStartedShards> shardState = fetchData(shard, allocation);
            if (shardState.hasData() == false) {
                logger.trace("{}: ignoring allocation, still fetching shard started state", shard);
                allocation.setHasPendingAsyncFetch();
                unassignedIterator.removeAndIgnore();
                continue;
            }

            final Set<String> lastActiveAllocationIds = indexMetaData.activeAllocationIds(shard.id());
            final boolean snapshotRestore = shard.restoreSource() != null;
            final boolean recoverOnAnyNode = recoverOnAnyNode(indexSettings);

            final NodesAndVersions nodesAndVersions;
            final boolean enoughAllocationsFound;

            if (lastActiveAllocationIds.isEmpty()) {
                assert indexSettings.getIndexVersionCreated().before(Version.V_3_0_0) : "trying to allocated a primary with an empty allocation id set, but index is new";
                // when we load an old index (after upgrading cluster) or restore a snapshot of an old index
                // fall back to old version-based allocation mode
                // Note that once the shard has been active, lastActiveAllocationIds will be non-empty
                nodesAndVersions = buildNodesAndVersions(shard, snapshotRestore || recoverOnAnyNode, allocation.getIgnoreNodes(shard.shardId()), shardState);
                if (snapshotRestore || recoverOnAnyNode) {
                    enoughAllocationsFound = nodesAndVersions.allocationsFound > 0;
                } else {
                    enoughAllocationsFound = isEnoughVersionBasedAllocationsFound(shard, indexMetaData, nodesAndVersions);
                }
                logger.debug("[{}][{}]: version-based allocation for pre-{} index found {} allocations of {}, highest version: [{}]", shard.index(), shard.id(), Version.V_3_0_0, nodesAndVersions.allocationsFound, shard, nodesAndVersions.highestVersion);
            } else {
                assert lastActiveAllocationIds.isEmpty() == false;
                // use allocation ids to select nodes
                nodesAndVersions = buildAllocationIdBasedNodes(shard, snapshotRestore || recoverOnAnyNode,
                        allocation.getIgnoreNodes(shard.shardId()), lastActiveAllocationIds, shardState);
                enoughAllocationsFound = nodesAndVersions.allocationsFound > 0;
                logger.debug("[{}][{}]: found {} allocations of {} based on allocation ids: [{}]", shard.index(), shard.id(), nodesAndVersions.allocationsFound, shard, lastActiveAllocationIds);
            }

            if (enoughAllocationsFound == false){
                if (snapshotRestore) {
                    // let BalancedShardsAllocator take care of allocating this shard
                    logger.debug("[{}][{}]: missing local data, will restore from [{}]", shard.index(), shard.id(), shard.restoreSource());
                } else if (recoverOnAnyNode) {
                    // let BalancedShardsAllocator take care of allocating this shard
                    logger.debug("[{}][{}]: missing local data, recover from any node", shard.index(), shard.id());
                } else {
                    // we can't really allocate, so ignore it and continue
                    unassignedIterator.removeAndIgnore();
                    logger.debug("[{}][{}]: not allocating, number_of_allocated_shards_found [{}]", shard.index(), shard.id(), nodesAndVersions.allocationsFound);
                }
                continue;
            }

            final NodesToAllocate nodesToAllocate = buildNodesToAllocate(shard, allocation, nodesAndVersions.nodes);
            if (nodesToAllocate.yesNodes.isEmpty() == false) {
                DiscoveryNode node = nodesToAllocate.yesNodes.get(0);
                logger.debug("[{}][{}]: allocating [{}] to [{}] on primary allocation", shard.index(), shard.id(), shard, node);
                changed = true;
                unassignedIterator.initialize(node.id(), nodesAndVersions.highestVersion, ShardRouting.UNAVAILABLE_EXPECTED_SHARD_SIZE);
            } else if (nodesToAllocate.throttleNodes.isEmpty() == true && nodesToAllocate.noNodes.isEmpty() == false) {
                DiscoveryNode node = nodesToAllocate.noNodes.get(0);
                logger.debug("[{}][{}]: forcing allocating [{}] to [{}] on primary allocation", shard.index(), shard.id(), shard, node);
                changed = true;
                unassignedIterator.initialize(node.id(), nodesAndVersions.highestVersion, ShardRouting.UNAVAILABLE_EXPECTED_SHARD_SIZE);
            } else {
                // we are throttling this, but we have enough to allocate to this node, ignore it for now
                logger.debug("[{}][{}]: throttling allocation [{}] to [{}] on primary allocation", shard.index(), shard.id(), shard, nodesToAllocate.throttleNodes);
                unassignedIterator.removeAndIgnore();
            }
        }
        return changed;
    }

    /**
     * Builds a list of nodes. If matchAnyShard is set to false, only nodes that have an allocation id matching
     * lastActiveAllocationIds are added to the list. Otherwise, any node that has a shard is added to the list, but
     * entries with matching allocation id are always at the front of the list.
     */
    protected NodesAndVersions buildAllocationIdBasedNodes(ShardRouting shard, boolean matchAnyShard, Set<String> ignoreNodes,
                                                           Set<String> lastActiveAllocationIds, AsyncShardFetch.FetchResult<TransportNodesListGatewayStartedShards.NodeGatewayStartedShards> shardState) {
        List<DiscoveryNode> matchingNodes = new ArrayList<>();
        List<DiscoveryNode> nonMatchingNodes = new ArrayList<>();
        long highestVersion = -1;
        for (TransportNodesListGatewayStartedShards.NodeGatewayStartedShards nodeShardState : shardState.getData().values()) {
            DiscoveryNode node = nodeShardState.getNode();
            String allocationId = nodeShardState.allocationId();

            if (ignoreNodes.contains(node.id())) {
                continue;
            }

            if (nodeShardState.storeException() == null) {
                if (allocationId == null && nodeShardState.version() != -1) {
                    // old shard with no allocation id, assign dummy value so that it gets added below in case of matchAnyShard
                    allocationId = "_n/a_";
                }

                logger.trace("[{}] on node [{}] has allocation id [{}] of shard", shard, nodeShardState.getNode(), allocationId);
            } else {
                logger.trace("[{}] on node [{}] has allocation id [{}] but the store can not be opened, treating as no allocation id", nodeShardState.storeException(), shard, nodeShardState.getNode(), allocationId);
                allocationId = null;
            }

            if (allocationId != null) {
                if (lastActiveAllocationIds.contains(allocationId)) {
                    matchingNodes.add(node);
                    highestVersion = Math.max(highestVersion, nodeShardState.version());
                } else if (matchAnyShard) {
                    nonMatchingNodes.add(node);
                    highestVersion = Math.max(highestVersion, nodeShardState.version());
                }
            }
        }

        List<DiscoveryNode> nodes = new ArrayList<>();
        nodes.addAll(matchingNodes);
        nodes.addAll(nonMatchingNodes);

        if (logger.isTraceEnabled()) {
            logger.trace("{} candidates for allocation: {}", shard, nodes.stream().map(DiscoveryNode::name).collect(Collectors.joining(", ")));
        }
        return new NodesAndVersions(nodes, nodes.size(), highestVersion);
    }

    /**
     * used by old version-based allocation
     */
    private boolean isEnoughVersionBasedAllocationsFound(ShardRouting shard, IndexMetaData indexMetaData, NodesAndVersions nodesAndVersions) {
        // check if the counts meets the minimum set
        int requiredAllocation = 1;
        // if we restore from a repository one copy is more then enough
        String initialShards = INDEX_RECOVERY_INITIAL_SHARDS_SETTING.get(indexMetaData.getSettings(), settings);
        if ("quorum".equals(initialShards)) {
            if (indexMetaData.getNumberOfReplicas() > 1) {
                requiredAllocation = ((1 + indexMetaData.getNumberOfReplicas()) / 2) + 1;
            }
        } else if ("quorum-1".equals(initialShards) || "half".equals(initialShards)) {
            if (indexMetaData.getNumberOfReplicas() > 2) {
                requiredAllocation = ((1 + indexMetaData.getNumberOfReplicas()) / 2);
            }
        } else if ("one".equals(initialShards)) {
            requiredAllocation = 1;
        } else if ("full".equals(initialShards) || "all".equals(initialShards)) {
            requiredAllocation = indexMetaData.getNumberOfReplicas() + 1;
        } else if ("full-1".equals(initialShards) || "all-1".equals(initialShards)) {
            if (indexMetaData.getNumberOfReplicas() > 1) {
                requiredAllocation = indexMetaData.getNumberOfReplicas();
            }
        } else {
            requiredAllocation = Integer.parseInt(initialShards);
        }

        return nodesAndVersions.allocationsFound >= requiredAllocation;
    }

    /**
     * Split the list of nodes to lists of yes/no/throttle nodes based on allocation deciders
     */
    private NodesToAllocate buildNodesToAllocate(ShardRouting shard, RoutingAllocation allocation, List<DiscoveryNode> nodes) {
        List<DiscoveryNode> yesNodes = new ArrayList<>();
        List<DiscoveryNode> throttledNodes = new ArrayList<>();
        List<DiscoveryNode> noNodes = new ArrayList<>();
        for (DiscoveryNode discoNode : nodes) {
            RoutingNode node = allocation.routingNodes().node(discoNode.id());
            if (node == null) {
                continue;
            }

            Decision decision = allocation.deciders().canAllocate(shard, node, allocation);
            if (decision.type() == Decision.Type.THROTTLE) {
                throttledNodes.add(discoNode);
            } else if (decision.type() == Decision.Type.NO) {
                noNodes.add(discoNode);
            } else {
                yesNodes.add(discoNode);
            }
        }
        return new NodesToAllocate(Collections.unmodifiableList(yesNodes), Collections.unmodifiableList(throttledNodes), Collections.unmodifiableList(noNodes));
    }

    /**
     * Builds a list of nodes. If matchAnyShard is set to false, only nodes that have the highest shard version
     * are added to the list. Otherwise, any node that has a shard is added to the list, but entries with highest
     * version are always at the front of the list.
     */
    NodesAndVersions buildNodesAndVersions(ShardRouting shard, boolean matchAnyShard, Set<String> ignoreNodes,
                                           AsyncShardFetch.FetchResult<TransportNodesListGatewayStartedShards.NodeGatewayStartedShards> shardState) {
        final Map<DiscoveryNode, Long> nodesWithVersion = new HashMap<>();
        int numberOfAllocationsFound = 0;
        long highestVersion = -1;
        for (TransportNodesListGatewayStartedShards.NodeGatewayStartedShards nodeShardState : shardState.getData().values()) {
            long version = nodeShardState.version();
            DiscoveryNode node = nodeShardState.getNode();

            if (ignoreNodes.contains(node.id())) {
                continue;
            }

            // -1 version means it does not exists, which is what the API returns, and what we expect to
            if (nodeShardState.storeException() == null) {
                logger.trace("[{}] on node [{}] has version [{}] of shard", shard, nodeShardState.getNode(), version);
            } else {
                // when there is an store exception, we disregard the reported version and assign it as -1 (same as shard does not exist)
                logger.trace("[{}] on node [{}] has version [{}] but the store can not be opened, treating as version -1", nodeShardState.storeException(), shard, nodeShardState.getNode(), version);
                version = -1;
            }

            if (version != -1) {
                numberOfAllocationsFound++;
                // If we've found a new "best" candidate, clear the
                // current candidates and add it
                if (version > highestVersion) {
                    highestVersion = version;
                    if (matchAnyShard == false) {
                        nodesWithVersion.clear();
                    }
                    nodesWithVersion.put(node, version);
                } else if (version == highestVersion) {
                    // If the candidate is the same, add it to the
                    // list, but keep the current candidate
                    nodesWithVersion.put(node, version);
                }
            }
        }
        // Now that we have a map of nodes to versions along with the
        // number of allocations found (and not ignored), we need to sort
        // it so the node with the highest version is at the beginning
        List<DiscoveryNode> nodesWithHighestVersion = new ArrayList<>();
        nodesWithHighestVersion.addAll(nodesWithVersion.keySet());
        CollectionUtil.timSort(nodesWithHighestVersion, new Comparator<DiscoveryNode>() {
            @Override
            public int compare(DiscoveryNode o1, DiscoveryNode o2) {
                return Long.compare(nodesWithVersion.get(o2), nodesWithVersion.get(o1));
            }
        });

        if (logger.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder("[");
            for (DiscoveryNode n : nodesWithVersion.keySet()) {
                sb.append("[").append(n.getName()).append("]").append(" -> ").append(nodesWithVersion.get(n)).append(", ");
            }
            sb.append("]");
            logger.trace("{} candidates for allocation: {}", shard, sb.toString());
        }

        return new NodesAndVersions(Collections.unmodifiableList(nodesWithHighestVersion), numberOfAllocationsFound, highestVersion);
    }

    /**
     * Return {@code true} if the index is configured to allow shards to be
     * recovered on any node
     */
    private boolean recoverOnAnyNode(IndexSettings indexSettings) {
        return indexSettings.isOnSharedFilesystem()
            && IndexMetaData.INDEX_SHARED_FS_ALLOW_RECOVERY_ON_ANY_NODE_SETTING.get(indexSettings.getSettings());
    }

    protected abstract AsyncShardFetch.FetchResult<TransportNodesListGatewayStartedShards.NodeGatewayStartedShards> fetchData(ShardRouting shard, RoutingAllocation allocation);

    static class NodesAndVersions {
        public final List<DiscoveryNode> nodes;
        public final int allocationsFound;
        public final long highestVersion;

        public NodesAndVersions(List<DiscoveryNode> nodes, int allocationsFound, long highestVersion) {
            this.nodes = nodes;
            this.allocationsFound = allocationsFound;
            this.highestVersion = highestVersion;
        }
    }

    static class NodesToAllocate {
        final List<DiscoveryNode> yesNodes;
        final List<DiscoveryNode> throttleNodes;
        final List<DiscoveryNode> noNodes;

        public NodesToAllocate(List<DiscoveryNode> yesNodes, List<DiscoveryNode> throttleNodes, List<DiscoveryNode> noNodes) {
            this.yesNodes = yesNodes;
            this.throttleNodes = throttleNodes;
            this.noNodes = noNodes;
        }
    }
}
