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

package org.elasticsearch.indices.store;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.IndexShardRoutingTable;
import org.elasticsearch.cluster.routing.ShardRoutingState;
import org.elasticsearch.cluster.routing.TestShardRouting;
import org.elasticsearch.cluster.routing.UnassignedInfo;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.LocalTransportAddress;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.Version.CURRENT;
import static org.elasticsearch.cluster.service.ClusterServiceUtils.createClusterService;
import static org.elasticsearch.test.VersionUtils.randomVersion;

/**
 */
public class IndicesStoreTests extends ESTestCase {
    private final static ShardRoutingState[] NOT_STARTED_STATES;

    static {
        Set<ShardRoutingState> set = new HashSet<>();
        set.addAll(Arrays.asList(ShardRoutingState.values()));
        set.remove(ShardRoutingState.STARTED);
        NOT_STARTED_STATES = set.toArray(new ShardRoutingState[set.size()]);
    }

    private static ThreadPool threadPool;

    private IndicesStore indicesStore;
    private DiscoveryNode localNode;

    private ClusterService clusterService;

    @BeforeClass
    public static void beforeClass() {
        threadPool = new ThreadPool("ShardReplicationTests");
    }

    @AfterClass
    public static void afterClass() {
        ThreadPool.terminate(threadPool, 30, TimeUnit.SECONDS);
        threadPool = null;
    }

    @Before
    public void before() {
        localNode = new DiscoveryNode("abc", new LocalTransportAddress("abc"), Version.CURRENT);
        clusterService = createClusterService(threadPool);
        indicesStore = new IndicesStore(Settings.EMPTY, null, clusterService, new TransportService(null, null), null);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        clusterService.close();
    }

    public void testShardCanBeDeletedNoShardRouting() throws Exception {
        int numShards = randomIntBetween(1, 7);
        int numReplicas = randomInt(2);

        ClusterState.Builder clusterState = ClusterState.builder(new ClusterName("test"));
        clusterState.metaData(MetaData.builder().put(IndexMetaData.builder("test").settings(settings(Version.CURRENT)).numberOfShards(numShards).numberOfReplicas(numReplicas)));
        IndexShardRoutingTable.Builder routingTable = new IndexShardRoutingTable.Builder(new ShardId("test", "_na_", 1));

        assertFalse(indicesStore.shardCanBeDeleted(clusterState.build(), routingTable.build()));
    }

    public void testShardCanBeDeletedNoShardStarted() throws Exception {
        int numShards = randomIntBetween(1, 7);
        int numReplicas = randomInt(2);

        ClusterState.Builder clusterState = ClusterState.builder(new ClusterName("test"));
        clusterState.metaData(MetaData.builder().put(IndexMetaData.builder("test").settings(settings(Version.CURRENT)).numberOfShards(numShards).numberOfReplicas(numReplicas)));
        final int primaryTerm = randomInt(200);
        IndexShardRoutingTable.Builder routingTable = new IndexShardRoutingTable.Builder(new ShardId("test", "_na_", 1));
        for (int i = 0; i < numShards; i++) {
            int unStartedShard = randomInt(numReplicas);
            for (int j = 0; j <= numReplicas; j++) {
                ShardRoutingState state;
                if (j == unStartedShard) {
                    state = randomFrom(NOT_STARTED_STATES);
                } else {
                    state = randomFrom(ShardRoutingState.values());
                }
                UnassignedInfo unassignedInfo = null;
                if (state == ShardRoutingState.UNASSIGNED) {
                    unassignedInfo = new UnassignedInfo(UnassignedInfo.Reason.INDEX_CREATED, null);
                }
                routingTable.addShard(TestShardRouting.newShardRouting("test", i, "xyz", null, null, primaryTerm, j == 0, state, unassignedInfo));
            }
        }
        assertFalse(indicesStore.shardCanBeDeleted(clusterState.build(), routingTable.build()));
    }

    public void testShardCanBeDeletedShardExistsLocally() throws Exception {
        int numShards = randomIntBetween(1, 7);
        int numReplicas = randomInt(2);

        ClusterState.Builder clusterState = ClusterState.builder(new ClusterName("test"));
        clusterState.metaData(MetaData.builder().put(IndexMetaData.builder("test").settings(settings(Version.CURRENT)).numberOfShards(numShards).numberOfReplicas(numReplicas)));
        clusterState.nodes(DiscoveryNodes.builder().localNodeId(localNode.id()).put(localNode).put(new DiscoveryNode("xyz", new LocalTransportAddress("xyz"), Version.CURRENT)));
        IndexShardRoutingTable.Builder routingTable = new IndexShardRoutingTable.Builder(new ShardId("test", "_na_", 1));
        int localShardId = randomInt(numShards - 1);
        final int primaryTerm = randomInt(200);
        for (int i = 0; i < numShards; i++) {
            String nodeId = i == localShardId ? localNode.getId() : randomBoolean() ? "abc" : "xyz";
            String relocationNodeId = randomBoolean() ? null : randomBoolean() ? localNode.getId() : "xyz";
            routingTable.addShard(TestShardRouting.newShardRouting("test", i, nodeId, relocationNodeId, primaryTerm, true, ShardRoutingState.STARTED));
            for (int j = 0; j < numReplicas; j++) {
                routingTable.addShard(TestShardRouting.newShardRouting("test", i, nodeId, relocationNodeId, primaryTerm, false, ShardRoutingState.STARTED));
            }
        }

        // Shard exists locally, can't delete shard
        assertFalse(indicesStore.shardCanBeDeleted(clusterState.build(), routingTable.build()));
    }

    public void testShardCanBeDeletedNodeNotInList() throws Exception {
        int numShards = randomIntBetween(1, 7);
        int numReplicas = randomInt(2);

        ClusterState.Builder clusterState = ClusterState.builder(new ClusterName("test"));
        clusterState.metaData(MetaData.builder().put(IndexMetaData.builder("test").settings(settings(Version.CURRENT)).numberOfShards(numShards).numberOfReplicas(numReplicas)));
        clusterState.nodes(DiscoveryNodes.builder().localNodeId(localNode.id()).put(localNode));
        final int primaryTerm = randomInt(200);
        IndexShardRoutingTable.Builder routingTable = new IndexShardRoutingTable.Builder(new ShardId("test", "_na_", 1));
        for (int i = 0; i < numShards; i++) {
            String relocatingNodeId = randomBoolean() ? null : "def";
            routingTable.addShard(TestShardRouting.newShardRouting("test", i, "xyz", relocatingNodeId, primaryTerm, true, ShardRoutingState.STARTED));
            for (int j = 0; j < numReplicas; j++) {
                routingTable.addShard(TestShardRouting.newShardRouting("test", i, "xyz", relocatingNodeId, primaryTerm, false, ShardRoutingState.STARTED));
            }
        }

        // null node -> false
        assertFalse(indicesStore.shardCanBeDeleted(clusterState.build(), routingTable.build()));
    }

    public void testShardCanBeDeletedNodeVersion() throws Exception {
        int numShards = randomIntBetween(1, 7);
        int numReplicas = randomInt(2);

        // Most of the times don't test bwc and use current version
        final Version nodeVersion = randomBoolean() ? CURRENT : randomVersion(random());
        ClusterState.Builder clusterState = ClusterState.builder(new ClusterName("test"));
        clusterState.metaData(MetaData.builder().put(IndexMetaData.builder("test").settings(settings(Version.CURRENT)).numberOfShards(numShards).numberOfReplicas(numReplicas)));
        clusterState.nodes(DiscoveryNodes.builder().localNodeId(localNode.id()).put(localNode).put(new DiscoveryNode("xyz", new LocalTransportAddress("xyz"), nodeVersion)));
        final int primaryTerm = randomInt(200);
        IndexShardRoutingTable.Builder routingTable = new IndexShardRoutingTable.Builder(new ShardId("test", "_na_", 1));
        for (int i = 0; i < numShards; i++) {
            routingTable.addShard(TestShardRouting.newShardRouting("test", i, "xyz", null, primaryTerm, true, ShardRoutingState.STARTED));
            for (int j = 0; j < numReplicas; j++) {
                routingTable.addShard(TestShardRouting.newShardRouting("test", i, "xyz", null, primaryTerm, false, ShardRoutingState.STARTED));
            }
        }

        // shard exist on other node (abc)
        assertTrue(indicesStore.shardCanBeDeleted(clusterState.build(), routingTable.build()));
    }

    public void testShardCanBeDeletedRelocatingNode() throws Exception {
        int numShards = randomIntBetween(1, 7);
        int numReplicas = randomInt(2);

        ClusterState.Builder clusterState = ClusterState.builder(new ClusterName("test"));
        clusterState.metaData(MetaData.builder().put(IndexMetaData.builder("test").settings(settings(Version.CURRENT)).numberOfShards(numShards).numberOfReplicas(numReplicas)));
        final Version nodeVersion = randomBoolean() ? CURRENT : randomVersion(random());
        final int primaryTerm = randomInt(200);
        clusterState.nodes(DiscoveryNodes.builder().localNodeId(localNode.id())
                .put(localNode)
                .put(new DiscoveryNode("xyz", new LocalTransportAddress("xyz"), Version.CURRENT))
                .put(new DiscoveryNode("def", new LocalTransportAddress("def"), nodeVersion) // <-- only set relocating, since we're testing that in this test
                ));
        IndexShardRoutingTable.Builder routingTable = new IndexShardRoutingTable.Builder(new ShardId("test", "_na_", 1));
        for (int i = 0; i < numShards; i++) {
            routingTable.addShard(TestShardRouting.newShardRouting("test", i, "xyz", "def", primaryTerm, true, ShardRoutingState.STARTED));
            for (int j = 0; j < numReplicas; j++) {
                routingTable.addShard(TestShardRouting.newShardRouting("test", i, "xyz", "def", primaryTerm, false, ShardRoutingState.STARTED));
            }
        }

        // shard exist on other node (abc and def)
        assertTrue(indicesStore.shardCanBeDeleted(clusterState.build(), routingTable.build()));
    }
}
