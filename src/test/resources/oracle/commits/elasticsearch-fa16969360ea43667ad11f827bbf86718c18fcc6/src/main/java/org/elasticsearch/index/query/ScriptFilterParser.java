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

package org.elasticsearch.index.query;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.BitsFilteredDocIdSet;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.lucene.docset.MatchDocIdSet;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.cache.filter.support.CacheKeyFilter;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.SearchScript;
import org.elasticsearch.search.lookup.SearchLookup;

import java.io.IOException;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 *
 */
public class ScriptFilterParser implements FilterParser {

    public static final String NAME = "script";

    @Inject
    public ScriptFilterParser() {
    }

    @Override
    public String[] names() {
        return new String[]{NAME};
    }

    @Override
    public Filter parse(QueryParseContext parseContext) throws IOException, QueryParsingException {
        XContentParser parser = parseContext.parser();

        XContentParser.Token token;

        boolean cache = false; // no need to cache it by default, changes a lot?
        CacheKeyFilter.Key cacheKey = null;
        // also, when caching, since its isCacheable is false, will result in loading all bit set...
        String script = null;
        String scriptLang = null;
        Map<String, Object> params = null;

        String filterName = null;
        String currentFieldName = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT) {
                if ("params".equals(currentFieldName)) {
                    params = parser.map();
                } else {
                    throw new QueryParsingException(parseContext.index(), "[script] filter does not support [" + currentFieldName + "]");
                }
            } else if (token.isValue()) {
                if ("script".equals(currentFieldName)) {
                    script = parser.text();
                } else if ("lang".equals(currentFieldName)) {
                    scriptLang = parser.text();
                } else if ("_name".equals(currentFieldName)) {
                    filterName = parser.text();
                } else if ("_cache".equals(currentFieldName)) {
                    cache = parser.booleanValue();
                } else if ("_cache_key".equals(currentFieldName) || "_cacheKey".equals(currentFieldName)) {
                    cacheKey = new CacheKeyFilter.Key(parser.text());
                } else {
                    throw new QueryParsingException(parseContext.index(), "[script] filter does not support [" + currentFieldName + "]");
                }
            }
        }

        if (script == null) {
            throw new QueryParsingException(parseContext.index(), "script must be provided with a [script] filter");
        }
        if (params == null) {
            params = newHashMap();
        }

        Filter filter = new ScriptFilter(scriptLang, script, params, parseContext.scriptService(), parseContext.lookup());
        if (cache) {
            filter = parseContext.cacheFilter(filter, cacheKey);
        }
        if (filterName != null) {
            parseContext.addNamedFilter(filterName, filter);
        }
        return filter;
    }

    public static class ScriptFilter extends Filter {

        private final String script;

        private final Map<String, Object> params;

        private final SearchScript searchScript;

        public ScriptFilter(String scriptLang, String script, Map<String, Object> params, ScriptService scriptService, SearchLookup searchLookup) {
            this.script = script;
            this.params = params;

            this.searchScript = scriptService.search(searchLookup, scriptLang, script, newHashMap(params));
        }

        @Override
        public String toString() {
            StringBuilder buffer = new StringBuilder();
            buffer.append("ScriptFilter(");
            buffer.append(script);
            buffer.append(")");
            return buffer.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ScriptFilter that = (ScriptFilter) o;

            if (params != null ? !params.equals(that.params) : that.params != null) return false;
            if (script != null ? !script.equals(that.script) : that.script != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = script != null ? script.hashCode() : 0;
            result = 31 * result + (params != null ? params.hashCode() : 0);
            return result;
        }

        @Override
        public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
            searchScript.setNextReader(context);
            // LUCENE 4 UPGRADE: we can simply wrap this here since it is not cacheable and if we are not top level we will get a null passed anyway 
            return BitsFilteredDocIdSet.wrap(new ScriptDocSet(context.reader().maxDoc(), acceptDocs, searchScript), acceptDocs);
        }

        static class ScriptDocSet extends MatchDocIdSet {

            private final SearchScript searchScript;

            public ScriptDocSet(int maxDoc, @Nullable Bits acceptDocs, SearchScript searchScript) {
                super(maxDoc, acceptDocs);
                this.searchScript = searchScript;
            }

            @Override
            public boolean isCacheable() {
                return true;
            }

            @Override
            protected boolean matchDoc(int doc) {
                searchScript.setNextDocId(doc);
                Object val = searchScript.run();
                if (val == null) {
                    return false;
                }
                if (val instanceof Boolean) {
                    return (Boolean) val;
                }
                if (val instanceof Number) {
                    return ((Number) val).longValue() != 0;
                }
                throw new ElasticsearchIllegalArgumentException("Can't handle type [" + val + "] in script filter");
            }
        }
    }
}