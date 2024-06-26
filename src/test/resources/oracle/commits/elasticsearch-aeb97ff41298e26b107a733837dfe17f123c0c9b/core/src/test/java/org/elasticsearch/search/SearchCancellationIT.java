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

package org.elasticsearch.search;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.cluster.node.tasks.cancel.CancelTasksResponse;
import org.elasticsearch.action.admin.cluster.node.tasks.list.ListTasksResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollAction;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.PluginsService;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.AbstractSearchScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.tasks.TaskInfo;
import org.elasticsearch.test.ESIntegTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.elasticsearch.index.query.QueryBuilders.scriptQuery;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertNoFailures;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.SUITE)
public class SearchCancellationIT extends ESIntegTestCase {

    private static final ToXContent.Params FORMAT_PARAMS = new ToXContent.MapParams(Collections.singletonMap("pretty", "false"));


    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singleton(ScriptedBlockPlugin.class);
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        boolean lowLevelCancellation = randomBoolean();
        logger.info("Using lowLevelCancellation: {}", lowLevelCancellation);
        return Settings.builder().put(SearchService.LOW_LEVEL_CANCELLATION_SETTING.getKey(), lowLevelCancellation).build();
    }

    private void indexTestData() {
        for (int i = 0; i < 5; i++) {
            // Make sure we have a few segments
            BulkRequestBuilder bulkRequestBuilder = client().prepareBulk().setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            for (int j = 0; j < 20; j++) {
                bulkRequestBuilder.add(client().prepareIndex("test", "type", Integer.toString(i * 5 + j)).setSource("field", "value"));
            }
            assertNoFailures(bulkRequestBuilder.get());
        }
    }

    private List<ScriptedBlockPlugin> initBlockFactory() {
        List<ScriptedBlockPlugin> plugins = new ArrayList<>();
        for (PluginsService pluginsService : internalCluster().getDataNodeInstances(PluginsService.class)) {
            plugins.addAll(pluginsService.filterPlugins(ScriptedBlockPlugin.class));
        }
        for (ScriptedBlockPlugin plugin : plugins) {
            plugin.scriptedBlockFactory.reset();
            plugin.scriptedBlockFactory.enableBlock();
        }
        return plugins;
    }

    private void awaitForBlock(List<ScriptedBlockPlugin> plugins) throws Exception {
        int numberOfShards = getNumShards("test").numPrimaries;
        assertBusy(() -> {
            int numberOfBlockedPlugins = 0;
            for (ScriptedBlockPlugin plugin : plugins) {
                numberOfBlockedPlugins += plugin.scriptedBlockFactory.hits.get();
            }
            logger.info("The plugin blocked on {} out of {} shards", numberOfBlockedPlugins, numberOfShards);
            assertThat(numberOfBlockedPlugins, greaterThan(0));
        });
    }

    private void disableBlocks(List<ScriptedBlockPlugin> plugins) throws Exception {
        for (ScriptedBlockPlugin plugin : plugins) {
            plugin.scriptedBlockFactory.disableBlock();
        }
    }

    private void cancelSearch(String action) {
        ListTasksResponse listTasksResponse = client().admin().cluster().prepareListTasks().setActions(action).get();
        assertThat(listTasksResponse.getTasks(), hasSize(1));
        TaskInfo searchTask = listTasksResponse.getTasks().get(0);

        logger.info("Cancelling search");
        CancelTasksResponse cancelTasksResponse = client().admin().cluster().prepareCancelTasks().setTaskId(searchTask.getTaskId()).get();
        assertThat(cancelTasksResponse.getTasks(), hasSize(1));
        assertThat(cancelTasksResponse.getTasks().get(0).getTaskId(), equalTo(searchTask.getTaskId()));
    }

    private SearchResponse ensureSearchWasCancelled(ListenableActionFuture<SearchResponse> searchResponse) {
        try {
            SearchResponse response = searchResponse.actionGet();
            logger.info("Search response {}", response);
            assertNotEquals("At least one shard should have failed", 0, response.getFailedShards());
            return response;
        } catch (SearchPhaseExecutionException ex) {
            logger.info("All shards failed with", ex);
            return null;
        }
    }

    public void testCancellationDuringQueryPhase() throws Exception {

        List<ScriptedBlockPlugin> plugins = initBlockFactory();
        indexTestData();

        logger.info("Executing search");
        ListenableActionFuture<SearchResponse> searchResponse = client().prepareSearch("test").setQuery(
            scriptQuery(new Script(
                ScriptType.INLINE, "native", NativeTestScriptedBlockFactory.TEST_NATIVE_BLOCK_SCRIPT, Collections.emptyMap())))
            .execute();

        awaitForBlock(plugins);
        cancelSearch(SearchAction.NAME);
        disableBlocks(plugins);
        logger.info("Segments {}", XContentHelper.toString(client().admin().indices().prepareSegments("test").get(), FORMAT_PARAMS));
        ensureSearchWasCancelled(searchResponse);
    }

    public void testCancellationDuringFetchPhase() throws Exception {

        List<ScriptedBlockPlugin> plugins = initBlockFactory();
        indexTestData();

        logger.info("Executing search");
        ListenableActionFuture<SearchResponse> searchResponse = client().prepareSearch("test")
            .addScriptField("test_field",
                new Script(ScriptType.INLINE, "native", NativeTestScriptedBlockFactory.TEST_NATIVE_BLOCK_SCRIPT, Collections.emptyMap())
            ).execute();

        awaitForBlock(plugins);
        cancelSearch(SearchAction.NAME);
        disableBlocks(plugins);
        logger.info("Segments {}", XContentHelper.toString(client().admin().indices().prepareSegments("test").get(), FORMAT_PARAMS));
        ensureSearchWasCancelled(searchResponse);
    }

    @AwaitsFix(bugUrl = "https://github.com/elastic/elasticsearch/issues/21126")
    public void testCancellationOfScrollSearches() throws Exception {

        List<ScriptedBlockPlugin> plugins = initBlockFactory();
        indexTestData();

        logger.info("Executing search");
        ListenableActionFuture<SearchResponse> searchResponse = client().prepareSearch("test")
            .setScroll(TimeValue.timeValueSeconds(10))
            .setSize(5)
            .setQuery(
                scriptQuery(new Script(
                    ScriptType.INLINE, "native", NativeTestScriptedBlockFactory.TEST_NATIVE_BLOCK_SCRIPT, Collections.emptyMap())))
            .execute();

        awaitForBlock(plugins);
        cancelSearch(SearchAction.NAME);
        disableBlocks(plugins);
        ensureSearchWasCancelled(searchResponse);
    }


    @AwaitsFix(bugUrl = "https://github.com/elastic/elasticsearch/issues/21126")
    public void testCancellationOfScrollSearchesOnFollowupRequests() throws Exception {

        List<ScriptedBlockPlugin> plugins = initBlockFactory();
        indexTestData();

        // Disable block so the first request would pass
        disableBlocks(plugins);

        logger.info("Executing search");
        TimeValue keepAlive = TimeValue.timeValueSeconds(5);
        SearchResponse searchResponse = client().prepareSearch("test")
            .setScroll(keepAlive)
            .setSize(2)
            .setQuery(
                scriptQuery(new Script(
                    ScriptType.INLINE, "native", NativeTestScriptedBlockFactory.TEST_NATIVE_BLOCK_SCRIPT, Collections.emptyMap())))
            .get();

        assertNotNull(searchResponse.getScrollId());

        // Enable block so the second request would block
        for (ScriptedBlockPlugin plugin : plugins) {
            plugin.scriptedBlockFactory.reset();
            plugin.scriptedBlockFactory.enableBlock();
        }

        String scrollId = searchResponse.getScrollId();
        logger.info("Executing scroll with id {}", scrollId);
        ListenableActionFuture<SearchResponse> scrollResponse = client().prepareSearchScroll(searchResponse.getScrollId())
            .setScroll(keepAlive).execute();

        awaitForBlock(plugins);
        cancelSearch(SearchScrollAction.NAME);
        disableBlocks(plugins);

        SearchResponse response = ensureSearchWasCancelled(scrollResponse);
        if (response != null) {
            // The response didn't fail completely - update scroll id
            scrollId = response.getScrollId();
        }
        logger.info("Cleaning scroll with id {}", scrollId);
        client().prepareClearScroll().addScrollId(scrollId).get();
    }


    public static class ScriptedBlockPlugin extends Plugin implements ScriptPlugin {
        private NativeTestScriptedBlockFactory scriptedBlockFactory;

        public ScriptedBlockPlugin() {
            scriptedBlockFactory = new NativeTestScriptedBlockFactory();
        }

        @Override
        public List<NativeScriptFactory> getNativeScripts() {
            return Collections.singletonList(scriptedBlockFactory);
        }
    }

    private static class NativeTestScriptedBlockFactory implements NativeScriptFactory {

        public static final String TEST_NATIVE_BLOCK_SCRIPT = "native_test_search_block_script";

        private final AtomicInteger hits = new AtomicInteger();

        private final AtomicBoolean shouldBlock = new AtomicBoolean(true);

        public NativeTestScriptedBlockFactory() {
        }

        public void reset() {
            hits.set(0);
        }

        public void disableBlock() {
            shouldBlock.set(false);
        }

        public void enableBlock() {
            shouldBlock.set(true);
        }

        @Override
        public ExecutableScript newScript(Map<String, Object> params) {
            return new NativeTestScriptedBlock();
        }

        @Override
        public boolean needsScores() {
            return false;
        }

        @Override
        public String getName() {
            return TEST_NATIVE_BLOCK_SCRIPT;
        }

        public class NativeTestScriptedBlock extends AbstractSearchScript {
            @Override
            public Object run() {
                Loggers.getLogger(SearchCancellationIT.class).info("Blocking on the document {}", doc().get("_uid"));
                hits.incrementAndGet();
                try {
                    awaitBusy(() -> shouldBlock.get() == false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
        }
    }

}
