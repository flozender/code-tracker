/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.elasticsearch.rest.action.admin.indices.alias.put;

import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasAction;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.*;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.rest.RestRequest.Method.PUT;

/**
 */
public class RestIndexPutAliasAction extends BaseRestHandler {

    @Inject
    public RestIndexPutAliasAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(PUT, "/{index}/_alias/{name}", this);
        controller.registerHandler(PUT, "/_alias/{name}", this);
        controller.registerHandler(PUT, "/{index}/_alias", this);
        controller.registerHandler(PUT, "/_alias", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel) {
        String index = request.param("index");
        String alias = request.param("name");
        Map<String, Object> filter = null;
        String routing = null;
        String indexRouting = null;
        String searchRouting = null;

        if (request.hasContent()) {
            XContentParser parser = null;
            try {
                parser = XContentFactory.xContent(request.content()).createParser(request.content());
                XContentParser.Token token = parser.nextToken();
                if (token == null) {
                    throw new ElasticsearchIllegalArgumentException("No index alias is specified");
                }
                String currentFieldName = null;
                while ((token = parser.nextToken()) != null) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        currentFieldName = parser.currentName();
                    } else if (token.isValue()) {
                        if ("index".equals(currentFieldName)) {
                            index = parser.text();
                        } else if ("alias".equals(currentFieldName)) {
                            alias = parser.text();
                        } else if ("routing".equals(currentFieldName)) {
                            routing = parser.textOrNull();
                        } else if ("indexRouting".equals(currentFieldName) || "index-routing".equals(currentFieldName) || "index_routing".equals(currentFieldName)) {
                            indexRouting = parser.textOrNull();
                        } else if ("searchRouting".equals(currentFieldName) || "search-routing".equals(currentFieldName) || "search_routing".equals(currentFieldName)) {
                            searchRouting = parser.textOrNull();
                        }
                    } else if (token == XContentParser.Token.START_OBJECT) {
                        if ("filter".equals(currentFieldName)) {
                            filter = parser.mapOrdered();
                        }
                    }
                }
            } catch (Throwable e) {
                try {
                    channel.sendResponse(new XContentThrowableRestResponse(request, e));
                } catch (IOException e1) {
                    logger.warn("Failed to send response", e1);
                }
                return;
            } finally {
                if (parser != null) {
                    parser.close();
                }
            }
        }

        IndicesAliasesRequest indicesAliasesRequest = new IndicesAliasesRequest();
        indicesAliasesRequest.timeout(request.paramAsTime("timeout", indicesAliasesRequest.timeout()));
        AliasAction aliasAction = new AliasAction(AliasAction.Type.ADD, index, alias);
        indicesAliasesRequest.addAliasAction(aliasAction);
        indicesAliasesRequest.masterNodeTimeout(request.paramAsTime("master_timeout", indicesAliasesRequest.masterNodeTimeout()));

        if (routing != null) {
            aliasAction.routing(routing);
        }
        if (searchRouting != null) {
            aliasAction.searchRouting(searchRouting);
        }
        if (indexRouting != null) {
            aliasAction.indexRouting(indexRouting);
        }
        if (filter != null) {
            aliasAction.filter(filter);
        }
        client.admin().indices().aliases(indicesAliasesRequest, new AcknowledgedRestResponseActionListener<IndicesAliasesResponse>(request, channel, logger));
    }
}
