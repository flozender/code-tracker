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

package org.elasticsearch.routing;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.RoutingMissingException;
import org.elasticsearch.action.explain.ExplainResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.termvector.MultiTermVectorsResponse;
import org.elasticsearch.action.termvector.TermVectorRequest;
import org.elasticsearch.action.termvector.TermVectorResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.Test;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;

/**
 *
 */
public class SimpleRoutingTests extends ElasticsearchIntegrationTest {
   
    
    @Test
    public void testSimpleCrudRouting() throws Exception {
        createIndex("test");
        client().admin().cluster().prepareHealth().setWaitForEvents(Priority.LANGUID).setWaitForGreenStatus().execute().actionGet();

        logger.info("--> indexing with id [1], and routing [0]");
        client().prepareIndex("test", "type1", "1").setRouting("0").setSource("field", "value1").setRefresh(true).execute().actionGet();
        logger.info("--> verifying get with no routing, should not find anything");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareGet("test", "type1", "1").execute().actionGet().isExists(), equalTo(false));
        }
        logger.info("--> verifying get with routing, should find");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareGet("test", "type1", "1").setRouting("0").execute().actionGet().isExists(), equalTo(true));
        }

        logger.info("--> deleting with no routing, should not delete anything");
        client().prepareDelete("test", "type1", "1").setRefresh(true).execute().actionGet();
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareGet("test", "type1", "1").execute().actionGet().isExists(), equalTo(false));
            assertThat(client().prepareGet("test", "type1", "1").setRouting("0").execute().actionGet().isExists(), equalTo(true));
        }

        logger.info("--> deleting with routing, should delete");
        client().prepareDelete("test", "type1", "1").setRouting("0").setRefresh(true).execute().actionGet();
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareGet("test", "type1", "1").execute().actionGet().isExists(), equalTo(false));
            assertThat(client().prepareGet("test", "type1", "1").setRouting("0").execute().actionGet().isExists(), equalTo(false));
        }

        logger.info("--> indexing with id [1], and routing [0]");
        client().prepareIndex("test", "type1", "1").setRouting("0").setSource("field", "value1").setRefresh(true).execute().actionGet();
        logger.info("--> verifying get with no routing, should not find anything");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareGet("test", "type1", "1").execute().actionGet().isExists(), equalTo(false));
        }
        logger.info("--> verifying get with routing, should find");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareGet("test", "type1", "1").setRouting("0").execute().actionGet().isExists(), equalTo(true));
        }

        logger.info("--> deleting_by_query with 1 as routing, should not delete anything");
        client().prepareDeleteByQuery().setQuery(matchAllQuery()).setRouting("1").execute().actionGet();
        client().admin().indices().prepareRefresh().execute().actionGet();
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareGet("test", "type1", "1").execute().actionGet().isExists(), equalTo(false));
            assertThat(client().prepareGet("test", "type1", "1").setRouting("0").execute().actionGet().isExists(), equalTo(true));
        }

        logger.info("--> deleting_by_query with , should delete");
        client().prepareDeleteByQuery().setQuery(matchAllQuery()).setRouting("0").execute().actionGet();
        client().admin().indices().prepareRefresh().execute().actionGet();
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareGet("test", "type1", "1").execute().actionGet().isExists(), equalTo(false));
            assertThat(client().prepareGet("test", "type1", "1").setRouting("0").execute().actionGet().isExists(), equalTo(false));
        }
    }

    @Test
    public void testSimpleSearchRouting() {
        createIndex("test");
        client().admin().cluster().prepareHealth().setWaitForEvents(Priority.LANGUID).setWaitForGreenStatus().execute().actionGet();

        logger.info("--> indexing with id [1], and routing [0]");
        client().prepareIndex("test", "type1", "1").setRouting("0").setSource("field", "value1").setRefresh(true).execute().actionGet();
        logger.info("--> verifying get with no routing, should not find anything");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareGet("test", "type1", "1").execute().actionGet().isExists(), equalTo(false));
        }
        logger.info("--> verifying get with routing, should find");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareGet("test", "type1", "1").setRouting("0").execute().actionGet().isExists(), equalTo(true));
        }

        logger.info("--> search with no routing, should fine one");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareSearch().setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getHits().totalHits(), equalTo(1l));
        }

        logger.info("--> search with wrong routing, should not find");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareSearch().setRouting("1").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getHits().totalHits(), equalTo(0l));
            assertThat(client().prepareCount().setRouting("1").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getCount(), equalTo(0l));
        }

        logger.info("--> search with correct routing, should find");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareSearch().setRouting("0").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getHits().totalHits(), equalTo(1l));
            assertThat(client().prepareCount().setRouting("0").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getCount(), equalTo(1l));
        }

        logger.info("--> indexing with id [2], and routing [1]");
        client().prepareIndex("test", "type1", "2").setRouting("1").setSource("field", "value1").setRefresh(true).execute().actionGet();

        logger.info("--> search with no routing, should fine two");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareSearch().setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getHits().totalHits(), equalTo(2l));
            assertThat(client().prepareCount().setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getCount(), equalTo(2l));
        }

        logger.info("--> search with 0 routing, should find one");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareSearch().setRouting("0").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getHits().totalHits(), equalTo(1l));
            assertThat(client().prepareCount().setRouting("0").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getCount(), equalTo(1l));
        }

        logger.info("--> search with 1 routing, should find one");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareSearch().setRouting("1").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getHits().totalHits(), equalTo(1l));
            assertThat(client().prepareCount().setRouting("1").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getCount(), equalTo(1l));
        }

        logger.info("--> search with 0,1 routings , should find two");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareSearch().setRouting("0", "1").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getHits().totalHits(), equalTo(2l));
            assertThat(client().prepareCount().setRouting("0", "1").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getCount(), equalTo(2l));
        }

        logger.info("--> search with 0,1,0 routings , should find two");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareSearch().setRouting("0", "1", "0").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getHits().totalHits(), equalTo(2l));
            assertThat(client().prepareCount().setRouting("0", "1", "0").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getCount(), equalTo(2l));
        }
    }

    @Test
    public void testRequiredRoutingMapping() throws Exception {
        client().admin().indices().prepareCreate("test")
                .addMapping("type1", XContentFactory.jsonBuilder().startObject().startObject("type1").startObject("_routing").field("required", true).endObject().endObject().endObject())
                .execute().actionGet();
        client().admin().cluster().prepareHealth().setWaitForEvents(Priority.LANGUID).setWaitForGreenStatus().execute().actionGet();

        logger.info("--> indexing with id [1], and routing [0]");
        client().prepareIndex("test", "type1", "1").setRouting("0").setSource("field", "value1").setRefresh(true).execute().actionGet();
        logger.info("--> verifying get with no routing, should fail");

        logger.info("--> indexing with id [1], with no routing, should fail");
        try {
            client().prepareIndex("test", "type1", "1").setSource("field", "value1").setRefresh(true).execute().actionGet();
            assert false;
        } catch (ElasticsearchException e) {
            assertThat(e.unwrapCause(), instanceOf(RoutingMissingException.class));
        }

        logger.info("--> verifying get with routing, should find");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareGet("test", "type1", "1").setRouting("0").execute().actionGet().isExists(), equalTo(true));
        }

        logger.info("--> deleting with no routing, should broadcast the delete since _routing is required");
        client().prepareDelete("test", "type1", "1").setRefresh(true).execute().actionGet();
        for (int i = 0; i < 5; i++) {
            try {
                client().prepareGet("test", "type1", "1").execute().actionGet().isExists();
                assert false;
            } catch (RoutingMissingException e) {
                assertThat(e.status(), equalTo(RestStatus.BAD_REQUEST));
                assertThat(e.getMessage(), equalTo("routing is required for [test]/[type1]/[1]"));
            }
            assertThat(client().prepareGet("test", "type1", "1").setRouting("0").execute().actionGet().isExists(), equalTo(false));
        }

        logger.info("--> indexing with id [1], and routing [0]");
        client().prepareIndex("test", "type1", "1").setRouting("0").setSource("field", "value1").setRefresh(true).execute().actionGet();
        logger.info("--> verifying get with no routing, should not find anything");

        logger.info("--> bulk deleting with no routing, should broadcast the delete since _routing is required");
        client().prepareBulk().add(Requests.deleteRequest("test").type("type1").id("1")).execute().actionGet();
        client().admin().indices().prepareRefresh().execute().actionGet();
        for (int i = 0; i < 5; i++) {
            try {
                client().prepareGet("test", "type1", "1").execute().actionGet().isExists();
                assert false;
            } catch (RoutingMissingException e) {
                assertThat(e.status(), equalTo(RestStatus.BAD_REQUEST));
                assertThat(e.getMessage(), equalTo("routing is required for [test]/[type1]/[1]"));
            }
            assertThat(client().prepareGet("test", "type1", "1").setRouting("0").execute().actionGet().isExists(), equalTo(false));
        }
    }

    @Test
    public void testRequiredRoutingWithPathMapping() throws Exception {
        client().admin().indices().prepareCreate("test")
                .addMapping("type1", XContentFactory.jsonBuilder().startObject().startObject("type1")
                        .startObject("_routing").field("required", true).field("path", "routing_field").endObject()
                        .endObject().endObject())
                .execute().actionGet();
        client().admin().cluster().prepareHealth().setWaitForEvents(Priority.LANGUID).setWaitForGreenStatus().execute().actionGet();

        logger.info("--> indexing with id [1], and routing [0]");
        client().prepareIndex("test", "type1", "1").setSource("field", "value1", "routing_field", "0").setRefresh(true).execute().actionGet();

        logger.info("--> check failure with different routing");
        try {
            client().prepareIndex("test", "type1", "1").setRouting("1").setSource("field", "value1", "routing_field", "0").setRefresh(true).execute().actionGet();
            assert false;
        } catch (ElasticsearchException e) {
            assertThat(e.unwrapCause(), instanceOf(MapperParsingException.class));
        }


        logger.info("--> verifying get with no routing, should fail");
        for (int i = 0; i < 5; i++) {
            try {
                client().prepareGet("test", "type1", "1").execute().actionGet().isExists();
                assert false;
            } catch (RoutingMissingException e) {
                assertThat(e.status(), equalTo(RestStatus.BAD_REQUEST));
                assertThat(e.getMessage(), equalTo("routing is required for [test]/[type1]/[1]"));
            }
        }
        logger.info("--> verifying get with routing, should find");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareGet("test", "type1", "1").setRouting("0").execute().actionGet().isExists(), equalTo(true));
        }
    }

    @Test
    public void testRequiredRoutingWithPathMappingBulk() throws Exception {
        client().admin().indices().prepareCreate("test")
                .addMapping("type1", XContentFactory.jsonBuilder().startObject().startObject("type1")
                        .startObject("_routing").field("required", true).field("path", "routing_field").endObject()
                        .endObject().endObject())
                .execute().actionGet();
        client().admin().cluster().prepareHealth().setWaitForEvents(Priority.LANGUID).setWaitForGreenStatus().execute().actionGet();

        logger.info("--> indexing with id [1], and routing [0]");
        client().prepareBulk().add(
                client().prepareIndex("test", "type1", "1").setSource("field", "value1", "routing_field", "0")).execute().actionGet();
        client().admin().indices().prepareRefresh().execute().actionGet();

        logger.info("--> verifying get with no routing, should fail");
        for (int i = 0; i < 5; i++) {
            try {
                client().prepareGet("test", "type1", "1").execute().actionGet().isExists();
                assert false;
            } catch (RoutingMissingException e) {
                assertThat(e.status(), equalTo(RestStatus.BAD_REQUEST));
                assertThat(e.getMessage(), equalTo("routing is required for [test]/[type1]/[1]"));
            }
        }
        logger.info("--> verifying get with routing, should find");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareGet("test", "type1", "1").setRouting("0").execute().actionGet().isExists(), equalTo(true));
        }
    }

    @Test
    public void testRequiredRoutingWithPathNumericType() throws Exception {

        client().admin().indices().prepareCreate("test")
                .addMapping("type1", XContentFactory.jsonBuilder().startObject().startObject("type1")
                        .startObject("_routing").field("required", true).field("path", "routing_field").endObject()
                        .endObject().endObject())
                .execute().actionGet();
        client().admin().cluster().prepareHealth().setWaitForEvents(Priority.LANGUID).setWaitForGreenStatus().execute().actionGet();

        logger.info("--> indexing with id [1], and routing [0]");
        client().prepareIndex("test", "type1", "1").setSource("field", "value1", "routing_field", 0).execute().actionGet();
        client().admin().indices().prepareRefresh().execute().actionGet();

        logger.info("--> verifying get with no routing, should fail");
        for (int i = 0; i < 5; i++) {
            try {
                client().prepareGet("test", "type1", "1").execute().actionGet().isExists();
                assert false;
            } catch (RoutingMissingException e) {
                assertThat(e.status(), equalTo(RestStatus.BAD_REQUEST));
                assertThat(e.getMessage(), equalTo("routing is required for [test]/[type1]/[1]"));
            }
        }
        logger.info("--> verifying get with routing, should find");
        for (int i = 0; i < 5; i++) {
            assertThat(client().prepareGet("test", "type1", "1").setRouting("0").execute().actionGet().isExists(), equalTo(true));
        }
    }

    @Test
    public void testRequiredRoutingMapping_variousAPIs() throws Exception {
        client().admin().indices().prepareCreate("test")
                .addMapping("type1", XContentFactory.jsonBuilder().startObject().startObject("type1").startObject("_routing").field("required", true).endObject().endObject().endObject())
                .execute().actionGet();
        client().admin().cluster().prepareHealth().setWaitForEvents(Priority.LANGUID).setWaitForGreenStatus().execute().actionGet();

        logger.info("--> indexing with id [1], and routing [0]");
        client().prepareIndex("test", "type1", "1").setRouting("0").setSource("field", "value1").get();
        logger.info("--> indexing with id [2], and routing [0]");
        client().prepareIndex("test", "type1", "2").setRouting("0").setSource("field", "value2").setRefresh(true).get();

        logger.info("--> verifying get with id [1] with routing [0], should succeed");
        assertThat(client().prepareGet("test", "type1", "1").setRouting("0").execute().actionGet().isExists(), equalTo(true));

        logger.info("--> verifying get with id [1], with no routing, should fail");
        try {
            client().prepareGet("test", "type1", "1").get();
            assert false;
        } catch (RoutingMissingException e) {
            assertThat(e.getMessage(), equalTo("routing is required for [test]/[type1]/[1]"));
        }

        logger.info("--> verifying explain with id [2], with routing [0], should succeed");
        ExplainResponse explainResponse = client().prepareExplain("test", "type1", "2")
                .setQuery(QueryBuilders.matchAllQuery())
                .setRouting("0").get();
        assertThat(explainResponse.isExists(), equalTo(true));
        assertThat(explainResponse.isMatch(), equalTo(true));

        logger.info("--> verifying explain with id [2], with no routing, should fail");
        try {
            client().prepareExplain("test", "type1", "2")
                    .setQuery(QueryBuilders.matchAllQuery()).get();
            assert false;
        } catch (RoutingMissingException e) {
            assertThat(e.getMessage(), equalTo("routing is required for [test]/[type1]/[2]"));
        }

        logger.info("--> verifying term vector with id [1], with routing [0], should succeed");
        TermVectorResponse termVectorResponse = client().prepareTermVector("test", "type1", "1").setRouting("0").get();
        assertThat(termVectorResponse.isExists(), equalTo(true));
        assertThat(termVectorResponse.getId(), equalTo("1"));

        try {
            client().prepareTermVector("test", "type1", "1").get();
            assert false;
        } catch (RoutingMissingException e) {
            assertThat(e.getMessage(), equalTo("routing is required for [test]/[type1]/[1]"));
        }

        UpdateResponse updateResponse = client().prepareUpdate("test", "type1", "1").setRouting("0")
                .setDoc("field1", "value1").get();
        assertThat(updateResponse.getId(), equalTo("1"));
        assertThat(updateResponse.getVersion(), equalTo(2l));

        try {
            client().prepareUpdate("test", "type1", "1").setDoc("field1", "value1").get();
            assert false;
        } catch (RoutingMissingException e) {
            assertThat(e.getMessage(), equalTo("routing is required for [test]/[type1]/[1]"));
        }

        logger.info("--> verifying mget with ids [1,2], with routing [0], should succeed");
        MultiGetResponse multiGetResponse = client().prepareMultiGet()
                .add(new MultiGetRequest.Item("test", "type1", "1").routing("0"))
                .add(new MultiGetRequest.Item("test", "type1", "2").routing("0")).get();
        assertThat(multiGetResponse.getResponses().length, equalTo(2));
        assertThat(multiGetResponse.getResponses()[0].isFailed(), equalTo(false));
        assertThat(multiGetResponse.getResponses()[0].getResponse().getId(), equalTo("1"));
        assertThat(multiGetResponse.getResponses()[1].isFailed(), equalTo(false));
        assertThat(multiGetResponse.getResponses()[1].getResponse().getId(), equalTo("2"));

        logger.info("--> verifying mget with ids [1,2], with no routing, should fail");
        multiGetResponse = client().prepareMultiGet()
                .add(new MultiGetRequest.Item("test", "type1", "1"))
                .add(new MultiGetRequest.Item("test", "type1", "2")).get();
        assertThat(multiGetResponse.getResponses().length, equalTo(2));
        assertThat(multiGetResponse.getResponses()[0].isFailed(), equalTo(true));
        assertThat(multiGetResponse.getResponses()[0].getFailure().getId(), equalTo("1"));
        assertThat(multiGetResponse.getResponses()[0].getFailure().getMessage(), equalTo("routing is required, but hasn't been specified"));
        assertThat(multiGetResponse.getResponses()[1].isFailed(), equalTo(true));
        assertThat(multiGetResponse.getResponses()[1].getFailure().getId(), equalTo("2"));
        assertThat(multiGetResponse.getResponses()[1].getFailure().getMessage(), equalTo("routing is required, but hasn't been specified"));

        MultiTermVectorsResponse multiTermVectorsResponse = client().prepareMultiTermVectors()
                .add(new TermVectorRequest("test", "type1", "1").routing("0"))
                .add(new TermVectorRequest("test", "type1", "2").routing("0")).get();
        assertThat(multiTermVectorsResponse.getResponses().length, equalTo(2));
        assertThat(multiTermVectorsResponse.getResponses()[0].getId(), equalTo("1"));
        assertThat(multiTermVectorsResponse.getResponses()[0].isFailed(), equalTo(false));
        assertThat(multiTermVectorsResponse.getResponses()[0].getResponse().getId(), equalTo("1"));
        assertThat(multiTermVectorsResponse.getResponses()[0].getResponse().isExists(), equalTo(true));
        assertThat(multiTermVectorsResponse.getResponses()[1].getId(), equalTo("2"));
        assertThat(multiTermVectorsResponse.getResponses()[1].isFailed(), equalTo(false));
        assertThat(multiTermVectorsResponse.getResponses()[1].getResponse().getId(), equalTo("2"));
        assertThat(multiTermVectorsResponse.getResponses()[1].getResponse().isExists(), equalTo(true));

        multiTermVectorsResponse = client().prepareMultiTermVectors()
                .add(new TermVectorRequest("test", "type1", "1"))
                .add(new TermVectorRequest("test", "type1", "2")).get();
        assertThat(multiTermVectorsResponse.getResponses().length, equalTo(2));
        assertThat(multiTermVectorsResponse.getResponses()[0].getId(), equalTo("1"));
        assertThat(multiTermVectorsResponse.getResponses()[0].isFailed(), equalTo(true));
        assertThat(multiTermVectorsResponse.getResponses()[0].getFailure().getMessage(), equalTo("routing is required, but hasn't been specified"));
        assertThat(multiTermVectorsResponse.getResponses()[0].getResponse(), nullValue());
        assertThat(multiTermVectorsResponse.getResponses()[1].getId(), equalTo("2"));
        assertThat(multiTermVectorsResponse.getResponses()[1].isFailed(), equalTo(true));
        assertThat(multiTermVectorsResponse.getResponses()[1].getResponse(),nullValue());
        assertThat(multiTermVectorsResponse.getResponses()[1].getFailure().getMessage(), equalTo("routing is required, but hasn't been specified"));
    }
}
