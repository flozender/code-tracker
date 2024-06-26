package org.elasticsearch.action.admin.cluster.stats;
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


import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.monitor.sigar.SigarService;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import org.hamcrest.Matchers;
import org.junit.Test;

@ClusterScope(scope = ElasticsearchIntegrationTest.Scope.TEST, numNodes = 0)
public class ClusterStatsTests extends ElasticsearchIntegrationTest {

    private void assertCounts(ClusterStatsNodes.Counts counts, int total, int masterOnly, int dataOnly, int masterData, int client) {
        assertThat(counts.getTotal(), Matchers.equalTo(total));
        assertThat(counts.getMasterOnly(), Matchers.equalTo(masterOnly));
        assertThat(counts.getDataOnly(), Matchers.equalTo(dataOnly));
        assertThat(counts.getMasterData(), Matchers.equalTo(masterData));
        assertThat(counts.getClient(), Matchers.equalTo(client));
    }

    @Test
    public void testNodeCounts() {
        cluster().startNode();
        ClusterStatsResponse response = client().admin().cluster().prepareClusterStats().get();
        assertCounts(response.getNodesStats().getCounts(), 1, 0, 0, 1, 0);

        cluster().startNode(ImmutableSettings.builder().put("node.data", false));
        response = client().admin().cluster().prepareClusterStats().get();
        assertCounts(response.getNodesStats().getCounts(), 2, 1, 0, 1, 0);

        cluster().startNode(ImmutableSettings.builder().put("node.master", false));
        response = client().admin().cluster().prepareClusterStats().get();
        assertCounts(response.getNodesStats().getCounts(), 3, 1, 1, 1, 0);

        cluster().startNode(ImmutableSettings.builder().put("node.client", true));
        response = client().admin().cluster().prepareClusterStats().get();
        assertCounts(response.getNodesStats().getCounts(), 4, 1, 1, 1, 1);
    }


    private void assertShardStats(ClusterStatsIndices.ShardStats stats, int indices, int total, int primaries, double replicationFactor) {
        assertThat(stats.getIndices(), Matchers.equalTo(indices));
        assertThat(stats.getTotal(), Matchers.equalTo(total));
        assertThat(stats.getPrimaries(), Matchers.equalTo(primaries));
        assertThat(stats.getReplication(), Matchers.equalTo(replicationFactor));
    }

    @Test
    public void testIndicesShardStats() {
        cluster().startNode();

        ClusterStatsResponse response = client().admin().cluster().prepareClusterStats().get();
        assertThat(response.getStatus(), Matchers.equalTo(ClusterHealthStatus.GREEN));


        prepareCreate("test1").setSettings("number_of_shards", 2, "number_of_replicas", 1).get();
        ensureYellow();
        response = client().admin().cluster().prepareClusterStats().get();
        assertThat(response.getStatus(), Matchers.equalTo(ClusterHealthStatus.YELLOW));
        assertThat(response.indicesStats.getDocs().getCount(), Matchers.equalTo(0l));
        assertThat(response.indicesStats.getIndexCount(), Matchers.equalTo(1));
        assertShardStats(response.getIndicesStats().getShards(), 1, 2, 2, 0.0);

        // add another node, replicas should get assigned
        cluster().startNode();
        ensureGreen();
        index("test1", "type", "1", "f", "f");
        refresh(); // make the doc visible
        response = client().admin().cluster().prepareClusterStats().get();
        assertThat(response.getStatus(), Matchers.equalTo(ClusterHealthStatus.GREEN));
        assertThat(response.indicesStats.getDocs().getCount(), Matchers.equalTo(1l));
        assertShardStats(response.getIndicesStats().getShards(), 1, 4, 2, 1.0);

        prepareCreate("test2").setSettings("number_of_shards", 3, "number_of_replicas", 0).get();
        ensureGreen();
        response = client().admin().cluster().prepareClusterStats().get();
        assertThat(response.getStatus(), Matchers.equalTo(ClusterHealthStatus.GREEN));
        assertThat(response.indicesStats.getIndexCount(), Matchers.equalTo(2));
        assertShardStats(response.getIndicesStats().getShards(), 2, 7, 5, 2.0 / 5);

        assertThat(response.getIndicesStats().getShards().getAvgIndexPrimaryShards(), Matchers.equalTo(2.5));
        assertThat(response.getIndicesStats().getShards().getMinIndexPrimaryShards(), Matchers.equalTo(2));
        assertThat(response.getIndicesStats().getShards().getMaxIndexPrimaryShards(), Matchers.equalTo(3));

        assertThat(response.getIndicesStats().getShards().getAvgIndexShards(), Matchers.equalTo(3.5));
        assertThat(response.getIndicesStats().getShards().getMinIndexShards(), Matchers.equalTo(3));
        assertThat(response.getIndicesStats().getShards().getMaxIndexShards(), Matchers.equalTo(4));

        assertThat(response.getIndicesStats().getShards().getAvgIndexReplication(), Matchers.equalTo(0.5));
        assertThat(response.getIndicesStats().getShards().getMinIndexReplication(), Matchers.equalTo(0.0));
        assertThat(response.getIndicesStats().getShards().getMaxIndexReplication(), Matchers.equalTo(1.0));

    }

    @Test
    public void testValuesSmokeScreen() {
        cluster().ensureAtMostNumNodes(5);
        cluster().ensureAtLeastNumNodes(1);
        SigarService sigarService = cluster().getInstance(SigarService.class);
        index("test1", "type", "1", "f", "f");

        ClusterStatsResponse response = client().admin().cluster().prepareClusterStats().get();
        assertThat(response.getTimestamp(), Matchers.greaterThan(946681200000l)); // 1 Jan 2000
        assertThat(response.indicesStats.getStore().getSizeInBytes(), Matchers.greaterThan(0l));

        assertThat(response.nodesStats.getFs().getTotal().bytes(), Matchers.greaterThan(0l));
        assertThat(response.nodesStats.getJvm().getVersions().size(), Matchers.greaterThan(0));
        if (sigarService.sigarAvailable()) {
            // We only get those if we have sigar
            assertThat(response.nodesStats.getOs().getAvailableProcessors(), Matchers.greaterThan(0));
            assertThat(response.nodesStats.getOs().getAvailableMemory().bytes(), Matchers.greaterThan(0l));
            assertThat(response.nodesStats.getOs().getCpus().size(), Matchers.greaterThan(0));
        }
        assertThat(response.nodesStats.getVersions().size(), Matchers.greaterThan(0));
        assertThat(response.nodesStats.getVersions().contains(Version.CURRENT), Matchers.equalTo(true));
        assertThat(response.nodesStats.getPlugins().size(), Matchers.greaterThanOrEqualTo(0));

    }
}
