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

package org.elasticsearch.script.expression;

import org.apache.lucene.expressions.Expression;
import org.apache.lucene.expressions.js.JavascriptCompiler;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptException;
import org.elasticsearch.script.ScriptService.ScriptType;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.histogram.InternalHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.InternalHistogram.Bucket;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.elasticsearch.search.aggregations.pipeline.SimpleValue;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.hamcrest.ElasticsearchAssertions;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.search.aggregations.AggregationBuilders.histogram;
import static org.elasticsearch.search.aggregations.AggregationBuilders.sum;
import static org.elasticsearch.search.aggregations.pipeline.PipelineAggregatorBuilders.bucketScript;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertSearchResponse;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

// TODO: please convert to unit tests!
public class MoreExpressionTests extends ESIntegTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singleton(ExpressionPlugin.class);
    }

    private SearchRequestBuilder buildRequest(String script, Object... params) {
        ensureGreen("test");

        Map<String, Object> paramsMap = new HashMap<>();
        assert (params.length % 2 == 0);
        for (int i = 0; i < params.length; i += 2) {
            paramsMap.put(params[i].toString(), params[i + 1]);
        }

        SearchRequestBuilder req = client().prepareSearch().setIndices("test");
        req.setQuery(QueryBuilders.matchAllQuery())
                .addSort(SortBuilders.fieldSort("_uid")
                        .order(SortOrder.ASC))
                .addScriptField("foo", new Script(script, ScriptType.INLINE, "expression", paramsMap));
        return req;
    }

    public void testBasic() throws Exception {
        createIndex("test");
        ensureGreen("test");
        client().prepareIndex("test", "doc", "1").setSource("foo", 4).setRefresh(true).get();
        SearchResponse rsp = buildRequest("doc['foo'] + 1").get();
        assertEquals(1, rsp.getHits().getTotalHits());
        assertEquals(5.0, rsp.getHits().getAt(0).field("foo").getValue(), 0.0D);
    }

    public void testBasicUsingDotValue() throws Exception {
        createIndex("test");
        ensureGreen("test");
        client().prepareIndex("test", "doc", "1").setSource("foo", 4).setRefresh(true).get();
        SearchResponse rsp = buildRequest("doc['foo'].value + 1").get();
        assertEquals(1, rsp.getHits().getTotalHits());
        assertEquals(5.0, rsp.getHits().getAt(0).field("foo").getValue(), 0.0D);
    }

    public void testScore() throws Exception {
        createIndex("test");
        ensureGreen("test");
        indexRandom(true,
                client().prepareIndex("test", "doc", "1").setSource("text", "hello goodbye"),
                client().prepareIndex("test", "doc", "2").setSource("text", "hello hello hello goodbye"),
                client().prepareIndex("test", "doc", "3").setSource("text", "hello hello goodebye"));
        ScoreFunctionBuilder score = ScoreFunctionBuilders.scriptFunction(new Script("1 / _score", ScriptType.INLINE, "expression", null));
        SearchRequestBuilder req = client().prepareSearch().setIndices("test");
        req.setQuery(QueryBuilders.functionScoreQuery(QueryBuilders.termQuery("text", "hello"), score).boostMode(CombineFunction.REPLACE));
        req.setSearchType(SearchType.DFS_QUERY_THEN_FETCH); // make sure DF is consistent
        SearchResponse rsp = req.get();
        assertSearchResponse(rsp);
        SearchHits hits = rsp.getHits();
        assertEquals(3, hits.getTotalHits());
        assertEquals("1", hits.getAt(0).getId());
        assertEquals("3", hits.getAt(1).getId());
        assertEquals("2", hits.getAt(2).getId());
    }

    public void testDateMethods() throws Exception {
        ElasticsearchAssertions.assertAcked(prepareCreate("test").addMapping("doc", "date0", "type=date", "date1", "type=date"));
        ensureGreen("test");
        indexRandom(true,
                client().prepareIndex("test", "doc", "1").setSource("date0", "2015-04-28T04:02:07Z", "date1", "1985-09-01T23:11:01Z"),
                client().prepareIndex("test", "doc", "2").setSource("date0", "2013-12-25T11:56:45Z", "date1", "1983-10-13T23:15:00Z"));
        SearchResponse rsp = buildRequest("doc['date0'].getSeconds() - doc['date0'].getMinutes()").get();
        assertEquals(2, rsp.getHits().getTotalHits());
        SearchHits hits = rsp.getHits();
        assertEquals(5.0, hits.getAt(0).field("foo").getValue(), 0.0D);
        assertEquals(-11.0, hits.getAt(1).field("foo").getValue(), 0.0D);
        rsp = buildRequest("doc['date0'].getHourOfDay() + doc['date1'].getDayOfMonth()").get();
        assertEquals(2, rsp.getHits().getTotalHits());
        hits = rsp.getHits();
        assertEquals(5.0, hits.getAt(0).field("foo").getValue(), 0.0D);
        assertEquals(24.0, hits.getAt(1).field("foo").getValue(), 0.0D);
        rsp = buildRequest("doc['date1'].getMonth() + 1").get();
        assertEquals(2, rsp.getHits().getTotalHits());
        hits = rsp.getHits();
        assertEquals(9.0, hits.getAt(0).field("foo").getValue(), 0.0D);
        assertEquals(10.0, hits.getAt(1).field("foo").getValue(), 0.0D);
        rsp = buildRequest("doc['date1'].getYear()").get();
        assertEquals(2, rsp.getHits().getTotalHits());
        hits = rsp.getHits();
        assertEquals(1985.0, hits.getAt(0).field("foo").getValue(), 0.0D);
        assertEquals(1983.0, hits.getAt(1).field("foo").getValue(), 0.0D);
    }

    public void testMultiValueMethods() throws Exception {
        ElasticsearchAssertions.assertAcked(prepareCreate("test").addMapping("doc", "double0", "type=double", "double1", "type=double"));
        ensureGreen("test");
        indexRandom(true,
                client().prepareIndex("test", "doc", "1").setSource("double0", "5.0", "double0", "1.0", "double0", "1.5", "double1", "1.2", "double1", "2.4"),
                client().prepareIndex("test", "doc", "2").setSource("double0", "5.0", "double1", "3.0"),
                client().prepareIndex("test", "doc", "3").setSource("double0", "5.0", "double0", "1.0", "double0", "1.5", "double0", "-1.5", "double1", "4.0"));


        SearchResponse rsp = buildRequest("doc['double0'].count() + doc['double1'].count()").get();
        assertSearchResponse(rsp);
        SearchHits hits = rsp.getHits();
        assertEquals(3, hits.getTotalHits());
        assertEquals(5.0, hits.getAt(0).field("foo").getValue(), 0.0D);
        assertEquals(2.0, hits.getAt(1).field("foo").getValue(), 0.0D);
        assertEquals(5.0, hits.getAt(2).field("foo").getValue(), 0.0D);

        rsp = buildRequest("doc['double0'].sum()").get();
        assertSearchResponse(rsp);
        hits = rsp.getHits();
        assertEquals(3, hits.getTotalHits());
        assertEquals(7.5, hits.getAt(0).field("foo").getValue(), 0.0D);
        assertEquals(5.0, hits.getAt(1).field("foo").getValue(), 0.0D);
        assertEquals(6.0, hits.getAt(2).field("foo").getValue(), 0.0D);

        rsp = buildRequest("doc['double0'].avg() + doc['double1'].avg()").get();
        assertSearchResponse(rsp);
        hits = rsp.getHits();
        assertEquals(3, hits.getTotalHits());
        assertEquals(4.3, hits.getAt(0).field("foo").getValue(), 0.0D);
        assertEquals(8.0, hits.getAt(1).field("foo").getValue(), 0.0D);
        assertEquals(5.5, hits.getAt(2).field("foo").getValue(), 0.0D);

        rsp = buildRequest("doc['double0'].median()").get();
        assertSearchResponse(rsp);
        hits = rsp.getHits();
        assertEquals(3, hits.getTotalHits());
        assertEquals(1.5, hits.getAt(0).field("foo").getValue(), 0.0D);
        assertEquals(5.0, hits.getAt(1).field("foo").getValue(), 0.0D);
        assertEquals(1.25, hits.getAt(2).field("foo").getValue(), 0.0D);

        rsp = buildRequest("doc['double0'].min()").get();
        assertSearchResponse(rsp);
        hits = rsp.getHits();
        assertEquals(3, hits.getTotalHits());
        assertEquals(1.0, hits.getAt(0).field("foo").getValue(), 0.0D);
        assertEquals(5.0, hits.getAt(1).field("foo").getValue(), 0.0D);
        assertEquals(-1.5, hits.getAt(2).field("foo").getValue(), 0.0D);

        rsp = buildRequest("doc['double0'].max()").get();
        assertSearchResponse(rsp);
        hits = rsp.getHits();
        assertEquals(3, hits.getTotalHits());
        assertEquals(5.0, hits.getAt(0).field("foo").getValue(), 0.0D);
        assertEquals(5.0, hits.getAt(1).field("foo").getValue(), 0.0D);
        assertEquals(5.0, hits.getAt(2).field("foo").getValue(), 0.0D);

        rsp = buildRequest("doc['double0'].sum()/doc['double0'].count()").get();
        assertSearchResponse(rsp);
        hits = rsp.getHits();
        assertEquals(3, hits.getTotalHits());
        assertEquals(2.5, hits.getAt(0).field("foo").getValue(), 0.0D);
        assertEquals(5.0, hits.getAt(1).field("foo").getValue(), 0.0D);
        assertEquals(1.5, hits.getAt(2).field("foo").getValue(), 0.0D);
    }

    public void testInvalidDateMethodCall() throws Exception {
        ElasticsearchAssertions.assertAcked(prepareCreate("test").addMapping("doc", "double", "type=double"));
        ensureGreen("test");
        indexRandom(true, client().prepareIndex("test", "doc", "1").setSource("double", "178000000.0"));
        try {
            buildRequest("doc['double'].getYear()").get();
            fail();
        } catch (SearchPhaseExecutionException e) {
            assertThat(e.toString() + "should have contained IllegalArgumentException",
                    e.toString().contains("IllegalArgumentException"), equalTo(true));
            assertThat(e.toString() + "should have contained can only be used with a date field type",
                    e.toString().contains("can only be used with a date field type"), equalTo(true));
        }
    }

    public void testSparseField() throws Exception {
        ElasticsearchAssertions.assertAcked(prepareCreate("test").addMapping("doc", "x", "type=long", "y", "type=long"));
        ensureGreen("test");
        indexRandom(true,
                client().prepareIndex("test", "doc", "1").setSource("x", 4),
                client().prepareIndex("test", "doc", "2").setSource("y", 2));
        SearchResponse rsp = buildRequest("doc['x'] + 1").get();
        ElasticsearchAssertions.assertSearchResponse(rsp);
        SearchHits hits = rsp.getHits();
        assertEquals(2, rsp.getHits().getTotalHits());
        assertEquals(5.0, hits.getAt(0).field("foo").getValue(), 0.0D);
        assertEquals(1.0, hits.getAt(1).field("foo").getValue(), 0.0D);
    }

    public void testMissingField() throws Exception {
        createIndex("test");
        ensureGreen("test");
        client().prepareIndex("test", "doc", "1").setSource("x", 4).setRefresh(true).get();
        try {
            buildRequest("doc['bogus']").get();
            fail("Expected missing field to cause failure");
        } catch (SearchPhaseExecutionException e) {
            assertThat(e.toString() + "should have contained ScriptException",
                    e.toString().contains("ScriptException"), equalTo(true));
            assertThat(e.toString() + "should have contained missing field error",
                    e.toString().contains("does not exist in mappings"), equalTo(true));
        }
    }

    public void testParams() throws Exception {
        createIndex("test");
        ensureGreen("test");
        indexRandom(true,
                client().prepareIndex("test", "doc", "1").setSource("x", 10),
                client().prepareIndex("test", "doc", "2").setSource("x", 3),
                client().prepareIndex("test", "doc", "3").setSource("x", 5));
        // a = int, b = double, c = long
        String script = "doc['x'] * a + b + ((c + doc['x']) > 5000000009 ? 1 : 0)";
        SearchResponse rsp = buildRequest(script, "a", 2, "b", 3.5, "c", 5000000000L).get();
        SearchHits hits = rsp.getHits();
        assertEquals(3, hits.getTotalHits());
        assertEquals(24.5, hits.getAt(0).field("foo").getValue(), 0.0D);
        assertEquals(9.5, hits.getAt(1).field("foo").getValue(), 0.0D);
        assertEquals(13.5, hits.getAt(2).field("foo").getValue(), 0.0D);
    }

    public void testCompileFailure() {
        client().prepareIndex("test", "doc", "1").setSource("x", 1).setRefresh(true).get();
        try {
            buildRequest("garbage%@#%@").get();
            fail("Expected expression compilation failure");
        } catch (SearchPhaseExecutionException e) {
            assertThat(e.toString() + "should have contained ScriptException",
                    e.toString().contains("ScriptException"), equalTo(true));
            assertThat(e.toString() + "should have contained compilation failure",
                    e.toString().contains("Failed to parse expression"), equalTo(true));
        }
    }

    public void testNonNumericParam() {
        client().prepareIndex("test", "doc", "1").setSource("x", 1).setRefresh(true).get();
        try {
            buildRequest("a", "a", "astring").get();
            fail("Expected string parameter to cause failure");
        } catch (SearchPhaseExecutionException e) {
            assertThat(e.toString() + "should have contained ScriptException",
                    e.toString().contains("ScriptException"), equalTo(true));
            assertThat(e.toString() + "should have contained non-numeric parameter error",
                    e.toString().contains("must be a numeric type"), equalTo(true));
        }
    }

    public void testNonNumericField() {
        client().prepareIndex("test", "doc", "1").setSource("text", "this is not a number").setRefresh(true).get();
        try {
            buildRequest("doc['text']").get();
            fail("Expected text field to cause execution failure");
        } catch (SearchPhaseExecutionException e) {
            assertThat(e.toString() + "should have contained ScriptException",
                    e.toString().contains("ScriptException"), equalTo(true));
            assertThat(e.toString() + "should have contained non-numeric field error",
                    e.toString().contains("must be numeric"), equalTo(true));
        }
    }

    public void testInvalidGlobalVariable() {
        client().prepareIndex("test", "doc", "1").setSource("foo", 5).setRefresh(true).get();
        try {
            buildRequest("bogus").get();
            fail("Expected bogus variable to cause execution failure");
        } catch (SearchPhaseExecutionException e) {
            assertThat(e.toString() + "should have contained ScriptException",
                    e.toString().contains("ScriptException"), equalTo(true));
            assertThat(e.toString() + "should have contained unknown variable error",
                    e.toString().contains("Unknown variable"), equalTo(true));
        }
    }

    public void testDocWithoutField() {
        client().prepareIndex("test", "doc", "1").setSource("foo", 5).setRefresh(true).get();
        try {
            buildRequest("doc").get();
            fail("Expected doc variable without field to cause execution failure");
        } catch (SearchPhaseExecutionException e) {
            assertThat(e.toString() + "should have contained ScriptException",
                    e.toString().contains("ScriptException"), equalTo(true));
            assertThat(e.toString() + "should have contained a missing specific field error",
                    e.toString().contains("must be used with a specific field"), equalTo(true));
        }
    }

    public void testInvalidFieldMember() {
        client().prepareIndex("test", "doc", "1").setSource("foo", 5).setRefresh(true).get();
        try {
            buildRequest("doc['foo'].bogus").get();
            fail("Expected bogus field member to cause execution failure");
        } catch (SearchPhaseExecutionException e) {
            assertThat(e.toString() + "should have contained ScriptException",
                    e.toString().contains("ScriptException"), equalTo(true));
            assertThat(e.toString() + "should have contained member variable [value] or member methods may be accessed",
                    e.toString().contains("member variable [value] or member methods may be accessed"), equalTo(true));
        }
    }

    public void testSpecialValueVariable() throws Exception {
        // i.e. _value for aggregations
        createIndex("test");
        ensureGreen("test");
        indexRandom(true,
                client().prepareIndex("test", "doc", "1").setSource("x", 5, "y", 1.2),
                client().prepareIndex("test", "doc", "2").setSource("x", 10, "y", 1.4),
                client().prepareIndex("test", "doc", "3").setSource("x", 13, "y", 1.8));

        SearchRequestBuilder req = client().prepareSearch().setIndices("test");
        req.setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(
                        AggregationBuilders.stats("int_agg").field("x")
                                .script(new Script("_value * 3", ScriptType.INLINE, ExpressionScriptEngineService.NAME, null)))
                .addAggregation(
                        AggregationBuilders.stats("double_agg").field("y")
                                .script(new Script("_value - 1.1", ScriptType.INLINE, ExpressionScriptEngineService.NAME, null)));

        SearchResponse rsp = req.get();
        assertEquals(3, rsp.getHits().getTotalHits());

        Stats stats = rsp.getAggregations().get("int_agg");
        assertEquals(39.0, stats.getMax(), 0.0001);
        assertEquals(15.0, stats.getMin(), 0.0001);

        stats = rsp.getAggregations().get("double_agg");
        assertEquals(0.7, stats.getMax(), 0.0001);
        assertEquals(0.1, stats.getMin(), 0.0001);
    }

    public void testStringSpecialValueVariable() throws Exception {
        // i.e. expression script for term aggregations, which is not allowed
        createIndex("test");
        ensureGreen("test");
        indexRandom(true,
                client().prepareIndex("test", "doc", "1").setSource("text", "hello"),
                client().prepareIndex("test", "doc", "2").setSource("text", "goodbye"),
                client().prepareIndex("test", "doc", "3").setSource("text", "hello"));

        SearchRequestBuilder req = client().prepareSearch().setIndices("test");
        req.setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(
                        AggregationBuilders.terms("term_agg").field("text")
                                .script(new Script("_value", ScriptType.INLINE, ExpressionScriptEngineService.NAME, null)));

        String message;
        try {
            // shards that don't have docs with the "text" field will not fail,
            // so we may or may not get a total failure
            SearchResponse rsp = req.get();
            assertThat(rsp.getShardFailures().length, greaterThan(0)); // at least the shards containing the docs should have failed
            message = rsp.getShardFailures()[0].reason();
        } catch (SearchPhaseExecutionException e) {
            message = e.toString();
        }
        assertThat(message + "should have contained ScriptException",
                message.contains("ScriptException"), equalTo(true));
        assertThat(message + "should have contained text variable error",
                message.contains("text variable"), equalTo(true));
    }

    // series of unit test for using expressions as executable scripts
    public void testExecutableScripts() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 2.5);
        vars.put("b", 3);
        vars.put("xyz", -1);

        Expression expr = JavascriptCompiler.compile("a+b+xyz");
        CompiledScript compiledScript = new CompiledScript(ScriptType.INLINE, "", "expression", expr);

        ExpressionExecutableScript ees = new ExpressionExecutableScript(compiledScript, vars);
        assertEquals((Double) ees.run(), 4.5, 0.001);

        ees.setNextVar("b", -2.5);
        assertEquals((Double) ees.run(), -1, 0.001);

        ees.setNextVar("a", -2.5);
        ees.setNextVar("b", -2.5);
        ees.setNextVar("xyz", -2.5);
        assertEquals((Double) ees.run(), -7.5, 0.001);

        String message;

        try {
            vars = new HashMap<>();
            vars.put("a", 1);
            ees = new ExpressionExecutableScript(compiledScript, vars);
            ees.run();
            fail("An incorrect number of variables were allowed to be used in an expression.");
        } catch (ScriptException se) {
            message = se.getMessage();
            assertThat(message + " should have contained number of variables", message.contains("number of variables"), equalTo(true));
        }

        try {
            vars = new HashMap<>();
            vars.put("a", 1);
            vars.put("b", 3);
            vars.put("c", -1);
            ees = new ExpressionExecutableScript(compiledScript, vars);
            ees.run();
            fail("A variable was allowed to be set that does not exist in the expression.");
        } catch (ScriptException se) {
            message = se.getMessage();
            assertThat(message + " should have contained does not exist in", message.contains("does not exist in"), equalTo(true));
        }

        try {
            vars = new HashMap<>();
            vars.put("a", 1);
            vars.put("b", 3);
            vars.put("xyz", "hello");
            ees = new ExpressionExecutableScript(compiledScript, vars);
            ees.run();
            fail("A non-number was allowed to be use in the expression.");
        } catch (ScriptException se) {
            message = se.getMessage();
            assertThat(message + " should have contained process numbers", message.contains("process numbers"), equalTo(true));
        }

    }

    // test to make sure expressions are not allowed to be used as update scripts
    public void testInvalidUpdateScript() throws Exception {
        try {
            createIndex("test_index");
            ensureGreen("test_index");
            indexRandom(true, client().prepareIndex("test_index", "doc", "1").setSource("text_field", "text"));
            UpdateRequestBuilder urb = client().prepareUpdate().setIndex("test_index");
            urb.setType("doc");
            urb.setId("1");
            urb.setScript(new Script("0", ScriptType.INLINE, ExpressionScriptEngineService.NAME, null));
            urb.get();
            fail("Expression scripts should not be allowed to run as update scripts.");
        } catch (Exception e) {
            String message = e.getMessage();
            assertThat(message + " should have contained failed to execute", message.contains("failed to execute"), equalTo(true));
            message = e.getCause().getMessage();
            assertThat(message + " should have contained not supported", message.contains("not supported"), equalTo(true));
        }
    }

    // test to make sure expressions are not allowed to be used as mapping scripts
    public void testInvalidMappingScript() throws Exception{
        try {
            createIndex("test_index");
            ensureGreen("test_index");
            XContentBuilder builder = XContentFactory.jsonBuilder().startObject();
            builder.startObject("transform");
            builder.field("script", "1.0");
            builder.field("lang", ExpressionScriptEngineService.NAME);
            builder.endObject();
            builder.startObject("properties");
            builder.startObject("double_field");
            builder.field("type", "double");
            builder.endObject();
            builder.endObject();
            builder.endObject();
            client().admin().indices().preparePutMapping("test_index").setType("trans_test").setSource(builder).get();
            client().prepareIndex("test_index", "trans_test", "1").setSource("double_field", 0.0).get();
            fail("Expression scripts should not be allowed to run as mapping scripts.");
        } catch (Exception e) {
            String message = ExceptionsHelper.detailedMessage(e);
            assertThat(message + " should have contained failed to parse", message.contains("failed to parse"), equalTo(true));
            assertThat(message + " should have contained not supported", message.contains("not supported"), equalTo(true));
        }
    }

    // test to make sure expressions are allowed to be used for reduce in pipeline aggregations
    public void testPipelineAggregationScript() throws Exception {
        createIndex("agg_index");
        ensureGreen("agg_index");
        indexRandom(true,
                client().prepareIndex("agg_index", "doc", "1").setSource("one", 1.0, "two", 2.0, "three", 3.0, "four", 4.0),
                client().prepareIndex("agg_index", "doc", "2").setSource("one", 2.0, "two", 2.0, "three", 3.0, "four", 4.0),
                client().prepareIndex("agg_index", "doc", "3").setSource("one", 3.0, "two", 2.0, "three", 3.0, "four", 4.0),
                client().prepareIndex("agg_index", "doc", "4").setSource("one", 4.0, "two", 2.0, "three", 3.0, "four", 4.0),
                client().prepareIndex("agg_index", "doc", "5").setSource("one", 5.0, "two", 2.0, "three", 3.0, "four", 4.0));
        SearchResponse response = client()
                .prepareSearch("agg_index")
                .addAggregation(
                        histogram("histogram")
                                .field("one")
                                .interval(2)
                                .subAggregation(sum("twoSum").field("two"))
                                .subAggregation(sum("threeSum").field("three"))
                                .subAggregation(sum("fourSum").field("four"))
                                .subAggregation(
                                        bucketScript("totalSum").setBucketsPaths("twoSum", "threeSum", "fourSum").script(
                                                new Script("_value0 + _value1 + _value2", ScriptType.INLINE, ExpressionScriptEngineService.NAME, null)))).execute().actionGet();

        InternalHistogram<Bucket> histogram = response.getAggregations().get("histogram");
        assertThat(histogram, notNullValue());
        assertThat(histogram.getName(), equalTo("histogram"));
        List<Bucket> buckets = histogram.getBuckets();

        for (int bucketCount = 0; bucketCount < buckets.size(); ++bucketCount) {
            Histogram.Bucket bucket = buckets.get(bucketCount);
            if (bucket.getDocCount() == 1) {
                SimpleValue seriesArithmetic = bucket.getAggregations().get("totalSum");
                assertThat(seriesArithmetic, notNullValue());
                double seriesArithmeticValue = seriesArithmetic.value();
                assertEquals(9.0, seriesArithmeticValue, 0.001);
            } else if (bucket.getDocCount() == 2) {
                SimpleValue seriesArithmetic = bucket.getAggregations().get("totalSum");
                assertThat(seriesArithmetic, notNullValue());
                double seriesArithmeticValue = seriesArithmetic.value();
                assertEquals(18.0, seriesArithmeticValue, 0.001);
            } else {
                fail("Incorrect number of documents in a bucket in the histogram.");
            }
        }
    }
}
