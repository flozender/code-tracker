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

package org.elasticsearch.indices.fielddata.breaker;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.engine.MockRobinEngine;
import org.elasticsearch.test.engine.ThrowingAtomicReaderWrapper;
import org.elasticsearch.test.junit.annotations.TestLogging;
import org.elasticsearch.test.store.MockDirectoryHelper;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for the circuit breaker while random exceptions are happening
 */
public class RandomExceptionCircuitBreakerTests extends ElasticsearchIntegrationTest {

    @Test
    @TestLogging("org.elasticsearch.indices.fielddata.breaker:TRACE,org.elasticsearch.index.fielddata:TRACE,org.elasticsearch.common.breaker:TRACE")
    public void testBreakerWithRandomExceptions() throws IOException, InterruptedException, ExecutionException {
        final int numShards = between(1, 5);
        String mapping = XContentFactory.jsonBuilder()
                .startObject()
                .startObject("type")
                .startObject("properties")
                .startObject("test-str")
                .field("type", "string")
                .field("index", "not_analyzed")
                .startObject("fielddata")
                .field("format", randomBytesFieldDataFormat())
                .endObject() // fielddata
                .endObject() // test-str
                .startObject("test-num")
                // I don't use randomNumericType() here because I don't want "byte", and I want "float" and "double"
                .field("type", randomFrom(Arrays.asList("float", "long", "double", "short", "integer")))
                .startObject("fielddata")
                .field("format", randomNumericFieldDataFormat())
                .endObject() // fielddata
                .endObject() // test-num
                .endObject() // properties
                .endObject() // type
                .endObject() // {}
                .string();
        final double exceptionRate;
        final double exceptionOnOpenRate;
        if (frequently()) {
            if (randomBoolean()) {
                if (randomBoolean()) {
                    exceptionOnOpenRate =  1.0/between(5, 100);
                    exceptionRate = 0.0d;
                } else {
                    exceptionRate =  1.0/between(5, 100);
                    exceptionOnOpenRate = 0.0d;
                }
            } else {
                exceptionOnOpenRate =  1.0/between(5, 100);
                exceptionRate =  1.0/between(5, 100);
            }
        } else {
            // rarely no exception
            exceptionRate = 0d;
            exceptionOnOpenRate = 0d;
        }

        ImmutableSettings.Builder settings = settingsBuilder()
                .put("index.number_of_shards", numShards)
                .put("index.number_of_replicas", randomIntBetween(0, 1))
                .put(MockDirectoryHelper.RANDOM_IO_EXCEPTION_RATE, exceptionRate)
                .put(MockDirectoryHelper.RANDOM_IO_EXCEPTION_RATE_ON_OPEN, exceptionOnOpenRate)
                .put(MockDirectoryHelper.CHECK_INDEX_ON_CLOSE, true);
        logger.info("creating index: [test] using settings: [{}]", settings.build().getAsMap());
        client().admin().indices().prepareCreate("test")
                .setSettings(settings)
                .addMapping("type", mapping).execute().actionGet();
        ClusterHealthResponse clusterHealthResponse = client().admin().cluster()
                .health(Requests.clusterHealthRequest().waitForYellowStatus().timeout(TimeValue.timeValueSeconds(5))).get(); // it's OK to timeout here
        final int numDocs;
        if (clusterHealthResponse.isTimedOut()) {
            /* some seeds just won't let you create the index at all and we enter a ping-pong mode
             * trying one node after another etc. that is ok but we need to make sure we don't wait
             * forever when indexing documents so we set numDocs = 1 and expect all shards to fail
             * when we search below.*/
            logger.info("ClusterHealth timed out - only index one doc and expect searches to fail");
            numDocs = 1;
        } else {
            numDocs = between(10, 100);
        }
        for (int i = 0; i < numDocs ; i++) {
            try {
                client().prepareIndex("test", "type", "" + i)
                        .setTimeout(TimeValue.timeValueSeconds(1)).setSource("test-str", randomUnicodeOfLengthBetween(5, 25), "test-num", i).get();
            } catch (ElasticsearchException ex) {
            }
        }
        logger.info("Start Refresh");
        RefreshResponse refreshResponse = client().admin().indices().prepareRefresh("test").execute().get(); // don't assert on failures here
        final boolean refreshFailed = refreshResponse.getShardFailures().length != 0 || refreshResponse.getFailedShards() != 0;
        logger.info("Refresh failed: [{}] numShardsFailed: [{}], shardFailuresLength: [{}], successfulShards: [{}], totalShards: [{}] ",
                refreshFailed, refreshResponse.getFailedShards(), refreshResponse.getShardFailures().length,
                refreshResponse.getSuccessfulShards(), refreshResponse.getTotalShards());
        final int numSearches = atLeast(10);

        for (int i = 0; i < numSearches; i++) {
            try {
                // Sort by the string and numeric fields, to load them into field data
                client().prepareSearch().setQuery(QueryBuilders.matchAllQuery())
                        .addSort("test-str", SortOrder.ASC)
                        .addSort("test-num", SortOrder.ASC).get();
            } catch (SearchPhaseExecutionException ex) {
                logger.info("expected SearchPhaseException: [{}]", ex.getMessage());
            }

            if (frequently()) {
                // Now, clear the cache and check that the circuit breaker has been
                // successfully set back to zero. If there is a bug in the circuit
                // breaker adjustment code, it should show up here by the breaker
                // estimate being either positive or negative.
                client().admin().indices().prepareClearCache("test").setFieldDataCache(true).execute().actionGet();
                NodesStatsResponse resp = client().admin().cluster().prepareNodesStats().all().execute().actionGet();
                for (NodeStats stats : resp.getNodes()) {
                    assertThat("Breaker reset to 0", stats.getBreaker().getEstimated(), equalTo(0L));
                }
            }
        }
    }

    public static final String EXCEPTION_TOP_LEVEL_RATIO_KEY = "index.engine.exception.ratio.top";
    public static final String EXCEPTION_LOW_LEVEL_RATIO_KEY = "index.engine.exception.ratio.low";

    // TODO: Generalize this class and add it as a utility
    public static class RandomExceptionDirectoryReaderWrapper extends MockRobinEngine.DirectoryReaderWrapper {
        private final Settings settings;
        static class ThrowingSubReaderWrapper extends SubReaderWrapper implements ThrowingAtomicReaderWrapper.Thrower {
            private final Random random;
            private final double topLevelRatio;
            private final double lowLevelRatio;

            ThrowingSubReaderWrapper(Settings settings) {
                final long seed = settings.getAsLong(ElasticsearchIntegrationTest.INDEX_SEED_SETTING, 0l);
                this.topLevelRatio = settings.getAsDouble(EXCEPTION_TOP_LEVEL_RATIO_KEY, 0.1d);
                this.lowLevelRatio = settings.getAsDouble(EXCEPTION_LOW_LEVEL_RATIO_KEY, 0.1d);
                this.random = new Random(seed);
            }

            @Override
            public AtomicReader wrap(AtomicReader reader) {
                return new ThrowingAtomicReaderWrapper(reader, this);
            }

            @Override
            public void maybeThrow(ThrowingAtomicReaderWrapper.Flags flag) throws IOException {
                switch (flag) {
                    case Fields:
                        break;
                    case TermVectors:
                        break;
                    case Terms:
                    case TermsEnum:
                        if (random.nextDouble() < topLevelRatio) {
                            throw new IOException("Forced top level Exception on [" + flag.name() + "]");
                        }
                    case Intersect:
                        break;
                    case Norms:
                        break;
                    case NumericDocValues:
                        break;
                    case BinaryDocValues:
                        break;
                    case SortedDocValues:
                        break;
                    case SortedSetDocValues:
                        break;
                    case DocsEnum:
                    case DocsAndPositionsEnum:
                        if (random.nextDouble() < lowLevelRatio) {
                            throw new IOException("Forced low level Exception on [" + flag.name() + "]");
                        }
                        break;
                }

            }
        }

        public RandomExceptionDirectoryReaderWrapper(DirectoryReader in, Settings settings) {
            super(in, new ThrowingSubReaderWrapper(settings));
            this.settings = settings;
        }

        @Override
        protected DirectoryReader doWrapDirectoryReader(DirectoryReader in) {
            return new RandomExceptionDirectoryReaderWrapper(in, settings);
        }
    }
}
