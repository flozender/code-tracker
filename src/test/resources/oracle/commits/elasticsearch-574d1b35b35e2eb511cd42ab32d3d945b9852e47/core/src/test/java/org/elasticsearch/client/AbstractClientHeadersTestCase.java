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

package org.elasticsearch.client;

import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.GenericAction;
import org.elasticsearch.action.admin.cluster.reroute.ClusterRerouteAction;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotAction;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsAction;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheAction;
import org.elasticsearch.action.admin.indices.create.CreateIndexAction;
import org.elasticsearch.action.admin.indices.flush.FlushAction;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsAction;
import org.elasticsearch.action.delete.DeleteAction;
import org.elasticsearch.action.get.GetAction;
import org.elasticsearch.action.index.IndexAction;
import org.elasticsearch.action.indexedscripts.delete.DeleteIndexedScriptAction;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.junit.After;
import org.junit.Before;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 *
 */
public abstract class AbstractClientHeadersTestCase extends ESTestCase {

    protected static final Settings HEADER_SETTINGS = Settings.builder()
            .put(ThreadContext.PREFIX + ".key1", "val1")
            .put(ThreadContext.PREFIX + ".key2", "val 2")
            .build();

    private static final GenericAction[] ACTIONS = new GenericAction[] {
                // client actions
                GetAction.INSTANCE, SearchAction.INSTANCE, DeleteAction.INSTANCE, DeleteIndexedScriptAction.INSTANCE,
                IndexAction.INSTANCE,

                // cluster admin actions
                ClusterStatsAction.INSTANCE, CreateSnapshotAction.INSTANCE, ClusterRerouteAction.INSTANCE,

                // indices admin actions
                CreateIndexAction.INSTANCE, IndicesStatsAction.INSTANCE, ClearIndicesCacheAction.INSTANCE, FlushAction.INSTANCE
    };

    protected ThreadPool threadPool;
    private Client client;

    @Before
    public void initClient() {
        Settings settings = Settings.builder()
                .put(HEADER_SETTINGS)
                .put("path.home", createTempDir().toString())
                .put("name", "test-" + getTestName())
                .build();
        threadPool = new ThreadPool(settings);
        client = buildClient(settings, ACTIONS);
    }

    @After
    public void cleanupClient() throws Exception {
        client.close();
        terminate(threadPool);
    }

    protected abstract Client buildClient(Settings headersSettings, GenericAction[] testedActions);


    public void testActions() {

        // TODO this is a really shitty way to test it, we need to figure out a way to test all the client methods
        //      without specifying each one (reflection doesn't as each action needs its own special settings, without
        //      them, request validation will fail before the test is executed. (one option is to enable disabling the
        //      validation in the settings??? - ugly and conceptually wrong)

        // choosing arbitrary top level actions to test
        client.prepareGet("idx", "type", "id").execute().addListener(new AssertingActionListener<>(GetAction.NAME, client.threadPool()));
        client.prepareSearch().execute().addListener(new AssertingActionListener<>(SearchAction.NAME, client.threadPool()));
        client.prepareDelete("idx", "type", "id").execute().addListener(new AssertingActionListener<>(DeleteAction.NAME, client.threadPool()));
        client.prepareDeleteIndexedScript("lang", "id").execute().addListener(new AssertingActionListener<>(DeleteIndexedScriptAction.NAME, client.threadPool()));
        client.prepareIndex("idx", "type", "id").setSource("source").execute().addListener(new AssertingActionListener<>(IndexAction.NAME, client.threadPool()));

        // choosing arbitrary cluster admin actions to test
        client.admin().cluster().prepareClusterStats().execute().addListener(new AssertingActionListener<>(ClusterStatsAction.NAME, client.threadPool()));
        client.admin().cluster().prepareCreateSnapshot("repo", "bck").execute().addListener(new AssertingActionListener<>(CreateSnapshotAction.NAME, client.threadPool()));
        client.admin().cluster().prepareReroute().execute().addListener(new AssertingActionListener<>(ClusterRerouteAction.NAME, client.threadPool()));

        // choosing arbitrary indices admin actions to test
        client.admin().indices().prepareCreate("idx").execute().addListener(new AssertingActionListener<>(CreateIndexAction.NAME, client.threadPool()));
        client.admin().indices().prepareStats().execute().addListener(new AssertingActionListener<>(IndicesStatsAction.NAME, client.threadPool()));
        client.admin().indices().prepareClearCache("idx1", "idx2").execute().addListener(new AssertingActionListener<>(ClearIndicesCacheAction.NAME, client.threadPool()));
        client.admin().indices().prepareFlush().execute().addListener(new AssertingActionListener<>(FlushAction.NAME, client.threadPool()));
    }

    public void testOverideHeader() throws Exception {
        String key1Val = randomAsciiOfLength(5);
        Map<String, String> expected = new HashMap<>();
        expected.put("key1", key1Val);
        expected.put("key2", "val 2");
        client.threadPool().getThreadContext().putHeader("key1", key1Val);
        client.prepareGet("idx", "type", "id")
                .execute().addListener(new AssertingActionListener<>(GetAction.NAME, expected, client.threadPool()));

        client.admin().cluster().prepareClusterStats()
                .execute().addListener(new AssertingActionListener<>(ClusterStatsAction.NAME, expected, client.threadPool()));

        client.admin().indices().prepareCreate("idx")
                .execute().addListener(new AssertingActionListener<>(CreateIndexAction.NAME, expected, client.threadPool()));
    }

    protected static void assertHeaders(Map<String, String> headers, Map<String, String> expected) {
        assertNotNull(headers);
        assertEquals(expected.size(), headers.size());
        for (Map.Entry<String, String> expectedEntry : expected.entrySet()) {
            assertEquals(headers.get(expectedEntry.getKey()), expectedEntry.getValue());
        }
    }

    protected static void assertHeaders(ThreadPool pool) {
        assertHeaders(pool.getThreadContext().getHeaders(), (Map)HEADER_SETTINGS.getAsSettings(ThreadContext.PREFIX).getAsStructuredMap());
    }

    public static class InternalException extends Exception {

        private final String action;

        public InternalException(String action) {
            this.action = action;
        }
    }

    protected static class AssertingActionListener<T> implements ActionListener<T> {

        private final String action;
        private final Map<String, String> expectedHeaders;
        private final ThreadPool pool;

        public AssertingActionListener(String action, ThreadPool pool) {
            this(action, (Map)HEADER_SETTINGS.getAsSettings(ThreadContext.PREFIX).getAsStructuredMap(), pool);
        }

       public AssertingActionListener(String action, Map<String, String> expectedHeaders, ThreadPool pool) {
            this.action = action;
            this.expectedHeaders = expectedHeaders;
            this.pool = pool;
        }

        @Override
        public void onResponse(T t) {
            fail("an internal exception was expected for action [" + action + "]");
        }

        @Override
        public void onFailure(Throwable t) {
            Throwable e = unwrap(t, InternalException.class);
            assertThat("expected action [" + action + "] to throw an internal exception", e, notNullValue());
            assertThat(action, equalTo(((InternalException) e).action));
            Map<String, String> headers = pool.getThreadContext().getHeaders();
            assertHeaders(headers, expectedHeaders);
        }

        public Throwable unwrap(Throwable t, Class<? extends Throwable> exceptionType) {
            int counter = 0;
            Throwable result = t;
            while (!exceptionType.isInstance(result)) {
                if (result.getCause() == null) {
                    return null;
                }
                if (result.getCause() == result) {
                    return null;
                }
                if (counter++ > 10) {
                    // dear god, if we got more than 10 levels down, WTF? just bail
                    fail("Exception cause unwrapping ran for 10 levels: " + ExceptionsHelper.stackTrace(t));
                    return null;
                }
                result = result.getCause();
            }
            return result;
        }

    }

}
