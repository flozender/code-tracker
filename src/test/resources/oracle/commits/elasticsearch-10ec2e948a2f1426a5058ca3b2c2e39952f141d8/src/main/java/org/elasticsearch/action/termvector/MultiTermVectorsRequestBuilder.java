package org.elasticsearch.action.termvector;
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


import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.internal.InternalClient;
import org.elasticsearch.common.Nullable;

public class MultiTermVectorsRequestBuilder extends ActionRequestBuilder<MultiTermVectorsRequest, MultiTermVectorsResponse, MultiTermVectorsRequestBuilder> {
    public MultiTermVectorsRequestBuilder(Client client) {
        super((InternalClient) client, new MultiTermVectorsRequest());
    }

    public MultiTermVectorsRequestBuilder add(String index, @Nullable String type, Iterable<String> ids) {
        for (String id : ids) {
            request.add(index, type, id);
        }
        return this;
    }

    public MultiTermVectorsRequestBuilder add(String index, @Nullable String type, String... ids) {
        for (String id : ids) {
            request.add(index, type, id);
        }
        return this;
    }

    public MultiTermVectorsRequestBuilder add(TermVectorRequest termVectorRequest) {
        request.add(termVectorRequest);
        return this;
    }

    @Override
    protected void doExecute(ActionListener<MultiTermVectorsResponse> listener) {
        ((Client) client).multiTermVectors(request, listener);
    }
}
