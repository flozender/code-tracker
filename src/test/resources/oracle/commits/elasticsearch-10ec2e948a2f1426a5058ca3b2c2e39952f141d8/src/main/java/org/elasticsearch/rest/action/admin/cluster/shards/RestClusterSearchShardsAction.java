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

package org.elasticsearch.rest.action.admin.cluster.shards;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.shards.ClusterSearchShardsRequest;
import org.elasticsearch.action.admin.cluster.shards.ClusterSearchShardsResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestXContentBuilder;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;

/**
 */
public class RestClusterSearchShardsAction extends BaseRestHandler {

    @Inject
    public RestClusterSearchShardsAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(GET, "/_search_shards", this);
        controller.registerHandler(POST, "/_search_shards", this);
        controller.registerHandler(GET, "/{index}/_search_shards", this);
        controller.registerHandler(POST, "/{index}/_search_shards", this);
        controller.registerHandler(GET, "/{index}/{type}/_search_shards", this);
        controller.registerHandler(POST, "/{index}/{type}/_search_shards", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel) {
        String[] indices = Strings.splitStringByCommaToArray(request.param("index"));
        final ClusterSearchShardsRequest clusterSearchShardsRequest = Requests.clusterSearchShardsRequest(indices);
        clusterSearchShardsRequest.local(request.paramAsBoolean("local", clusterSearchShardsRequest.local()));
        clusterSearchShardsRequest.listenerThreaded(false);

        clusterSearchShardsRequest.types(Strings.splitStringByCommaToArray(request.param("type")));
        clusterSearchShardsRequest.routing(request.param("routing"));
        clusterSearchShardsRequest.preference(request.param("preference"));
        clusterSearchShardsRequest.indicesOptions(IndicesOptions.fromRequest(request, clusterSearchShardsRequest.indicesOptions()));

        client.admin().cluster().searchShards(clusterSearchShardsRequest, new ActionListener<ClusterSearchShardsResponse>() {
            @Override
            public void onResponse(ClusterSearchShardsResponse response) {
                try {
                    XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);
                    builder.startObject();
                    builder.field("ok", true);
                    response.toXContent(builder, request);
                    builder.endObject();
                    channel.sendResponse(new XContentRestResponse(request, RestStatus.OK, builder));
                } catch (Throwable e) {
                    onFailure(e);
                }
            }

            @Override
            public void onFailure(Throwable e) {
                try {
                    channel.sendResponse(new XContentThrowableRestResponse(request, e));
                } catch (IOException e1) {
                    logger.error("Failed to send failure response", e1);
                }
            }
        });
    }
}
