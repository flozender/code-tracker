/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
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

package org.elasticsearch.index.query.xcontent;

import org.elasticsearch.util.collect.Maps;
import org.elasticsearch.util.xcontent.builder.XContentBuilder;

import java.io.IOException;
import java.util.Map;

/**
 * A query that uses a script to compute the score.
 *
 * @author kimchy (shay.banon)
 */
public class CustomScoreQueryBuilder extends BaseQueryBuilder {

    private final XContentQueryBuilder queryBuilder;

    private String script;

    private float boost = -1;

    private Map<String, Object> params = null;

    /**
     * A query that simply applies the boost factor to another query (multiply it).
     *
     * @param queryBuilder The query to apply the boost factor to.
     */
    public CustomScoreQueryBuilder(XContentQueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
    }

    /**
     * Sets the boost factor for this query.
     */
    public CustomScoreQueryBuilder script(String script) {
        this.script = script;
        return this;
    }

    /**
     * Additional parameters that can be provided to the script.
     */
    public CustomScoreQueryBuilder params(Map<String, Object> params) {
        if (params == null) {
            this.params = params;
        } else {
            this.params.putAll(params);
        }
        return this;
    }

    /**
     * Additional parameters that can be provided to the script.
     */
    public CustomScoreQueryBuilder param(String key, Object value) {
        if (params == null) {
            params = Maps.newHashMap();
        }
        params.put(key, value);
        return this;
    }

    /**
     * Sets the boost for this query.  Documents matching this query will (in addition to the normal
     * weightings) have their score multiplied by the boost provided.
     */
    public CustomScoreQueryBuilder boost(float boost) {
        this.boost = boost;
        return this;
    }

    @Override protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(CustomScoreQueryParser.NAME);
        builder.field("query");
        queryBuilder.toXContent(builder, params);
        builder.field("script", script);
        if (this.params != null) {
            builder.field("params");
            builder.map(this.params);
        }
        if (boost != -1) {
            builder.field("boost", boost);
        }
        builder.endObject();
    }
}