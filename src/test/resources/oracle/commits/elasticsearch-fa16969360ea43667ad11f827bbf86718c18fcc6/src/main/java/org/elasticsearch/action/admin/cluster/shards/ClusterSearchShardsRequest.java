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

package org.elasticsearch.action.admin.cluster.shards;

import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.MasterNodeOperationRequest;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 */
public class ClusterSearchShardsRequest extends MasterNodeOperationRequest<ClusterSearchShardsRequest> {
    private String[] indices;
    @Nullable
    private String routing;
    @Nullable
    private String preference;
    private boolean local = false;
    private String[] types = Strings.EMPTY_ARRAY;
    private IndicesOptions indicesOptions = IndicesOptions.lenient();


    public ClusterSearchShardsRequest() {
    }

    public ClusterSearchShardsRequest(String... indices) {
        indices(indices);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    /**
     * Sets the indices the search will be executed on.
     */
    public ClusterSearchShardsRequest indices(String... indices) {
        if (indices == null) {
            throw new ElasticsearchIllegalArgumentException("indices must not be null");
        } else {
            for (int i = 0; i < indices.length; i++) {
                if (indices[i] == null) {
                    throw new ElasticsearchIllegalArgumentException("indices[" + i + "] must not be null");
                }
            }
        }
        this.indices = indices;
        return this;
    }

    /**
     * The indices
     */
    public String[] indices() {
        return indices;
    }

    public IndicesOptions indicesOptions() {
        return indicesOptions;
    }

    public ClusterSearchShardsRequest indicesOptions(IndicesOptions indicesOptions) {
        this.indicesOptions = indicesOptions;
        return this;
    }

    /**
     * The document types to execute the search against. Defaults to be executed against
     * all types.
     */
    public String[] types() {
        return types;
    }

    /**
     * The document types to execute the search against. Defaults to be executed against
     * all types.
     */
    public ClusterSearchShardsRequest types(String... types) {
        this.types = types;
        return this;
    }

    /**
     * A comma separated list of routing values to control the shards the search will be executed on.
     */
    public String routing() {
        return this.routing;
    }

    /**
     * A comma separated list of routing values to control the shards the search will be executed on.
     */
    public ClusterSearchShardsRequest routing(String routing) {
        this.routing = routing;
        return this;
    }

    /**
     * The routing values to control the shards that the search will be executed on.
     */
    public ClusterSearchShardsRequest routing(String... routings) {
        this.routing = Strings.arrayToCommaDelimitedString(routings);
        return this;
    }

    /**
     * Sets the preference to execute the search. Defaults to randomize across shards. Can be set to
     * <tt>_local</tt> to prefer local shards, <tt>_primary</tt> to execute only on primary shards, or
     * a custom value, which guarantees that the same order will be used across different requests.
     */
    public ClusterSearchShardsRequest preference(String preference) {
        this.preference = preference;
        return this;
    }

    public String preference() {
        return this.preference;
    }

    public ClusterSearchShardsRequest local(boolean local) {
        this.local = local;
        return this;
    }

    public boolean local() {
        return this.local;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);

        indices = new String[in.readVInt()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = in.readString();
        }

        routing = in.readOptionalString();
        preference = in.readOptionalString();

        types = in.readStringArray();
        indicesOptions = IndicesOptions.readIndicesOptions(in);
        local = in.readBoolean();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);

        out.writeVInt(indices.length);
        for (String index : indices) {
            out.writeString(index);
        }

        out.writeOptionalString(routing);
        out.writeOptionalString(preference);

        out.writeStringArray(types);
        indicesOptions.writeIndicesOptions(out);
        out.writeBoolean(local);
    }

}
