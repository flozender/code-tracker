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

import org.elasticsearch.util.xcontent.builder.XContentBuilder;

import java.io.IOException;

/**
 * A filter that restricts search results to values that have a matching prefix in a given
 * field.
 *
 * @author kimchy (shay.banon)
 */
public class PrefixFilterBuilder extends BaseFilterBuilder {

    private final String name;

    private final String prefix;

    /**
     * A filter that restricts search results to values that have a matching prefix in a given
     * field.
     *
     * @param name   The field name
     * @param prefix The prefix
     */
    public PrefixFilterBuilder(String name, String prefix) {
        this.name = name;
        this.prefix = prefix;
    }

    @Override public void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(PrefixFilterParser.NAME);
        builder.field(name, prefix);
        builder.endObject();
    }
}