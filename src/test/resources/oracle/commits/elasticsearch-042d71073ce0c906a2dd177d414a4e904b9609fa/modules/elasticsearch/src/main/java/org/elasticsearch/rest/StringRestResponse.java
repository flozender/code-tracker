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

package org.elasticsearch.rest;

import org.apache.lucene.util.UnicodeUtil;

/**
 * @author kimchy (Shay Banon)
 */
public class StringRestResponse extends Utf8RestResponse {

    private static ThreadLocal<UnicodeUtil.UTF8Result> cache = new ThreadLocal<UnicodeUtil.UTF8Result>() {
        @Override protected UnicodeUtil.UTF8Result initialValue() {
            return new UnicodeUtil.UTF8Result();
        }
    };

    public StringRestResponse(Status status) {
        super(status);
    }

    public StringRestResponse(Status status, String content) {
        super(status, convert(content));
    }

    private static UnicodeUtil.UTF8Result convert(String content) {
        UnicodeUtil.UTF8Result result = cache.get();
        UnicodeUtil.UTF16toUTF8(content, 0, content.length(), result);
        return result;
    }
}