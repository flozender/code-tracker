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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.elasticsearch.index.AbstractIndexComponent;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.field.function.script.ScriptFieldsFunction;
import org.elasticsearch.index.query.QueryParsingException;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.util.Strings;
import org.elasticsearch.util.ThreadLocals;
import org.elasticsearch.util.inject.Inject;
import org.elasticsearch.util.lucene.search.function.FunctionScoreQuery;
import org.elasticsearch.util.lucene.search.function.ScoreFunction;
import org.elasticsearch.util.settings.Settings;
import org.elasticsearch.util.xcontent.XContentParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author kimchy (shay.banon)
 */
public class CustomScoreQueryParser extends AbstractIndexComponent implements XContentQueryParser {

    public static final String NAME = "custom_score";

    @Inject public CustomScoreQueryParser(Index index, @IndexSettings Settings settings) {
        super(index, settings);
    }

    @Override public String[] names() {
        return new String[]{NAME, Strings.toCamelCase(NAME)};
    }

    @Override public Query parse(QueryParseContext parseContext) throws IOException, QueryParsingException {
        XContentParser parser = parseContext.parser();

        Query query = null;
        float boost = 1.0f;
        String script = null;
        Map<String, Object> vars = null;

        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT) {
                if ("query".equals(currentFieldName)) {
                    query = parseContext.parseInnerQuery();
                } else if ("params".equals(currentFieldName)) {
                    vars = parser.map();
                }
            } else if (token.isValue()) {
                if ("script".equals(currentFieldName)) {
                    script = parser.text();
                } else if ("boost".equals(currentFieldName)) {
                    boost = parser.floatValue();
                }
            }
        }
        if (query == null) {
            throw new QueryParsingException(index, "[custom_score] requires 'query' field");
        }
        if (script == null) {
            throw new QueryParsingException(index, "[custom_score] requires 'script' field");
        }
        FunctionScoreQuery functionScoreQuery = new FunctionScoreQuery(query,
                new ScriptScoreFunction(new ScriptFieldsFunction(script, parseContext.scriptService(), parseContext.mapperService(), parseContext.indexCache().fieldData()), vars));
        functionScoreQuery.setBoost(boost);
        return functionScoreQuery;
    }

    private static ThreadLocal<ThreadLocals.CleanableValue<Map<String, Object>>> cachedVars = new ThreadLocal<ThreadLocals.CleanableValue<Map<String, Object>>>() {
        @Override protected ThreadLocals.CleanableValue<Map<String, Object>> initialValue() {
            return new ThreadLocals.CleanableValue<Map<String, Object>>(new HashMap<String, Object>());
        }
    };

    public static class ScriptScoreFunction implements ScoreFunction {

        private final ScriptFieldsFunction scriptFieldsFunction;

        private Map<String, Object> vars;

        private ScriptScoreFunction(ScriptFieldsFunction scriptFieldsFunction, Map<String, Object> vars) {
            this.scriptFieldsFunction = scriptFieldsFunction;
            this.vars = vars;
        }

        @Override public void setNextReader(IndexReader reader) {
            scriptFieldsFunction.setNextReader(reader);
            if (vars == null) {
                vars = cachedVars.get().get();
                vars.clear();
            }
        }

        @Override public float score(int docId, float subQueryScore) {
            vars.put("score", subQueryScore);
            return ((Number) scriptFieldsFunction.execute(docId, vars)).floatValue();
        }

        @Override public Explanation explain(int docId, Explanation subQueryExpl) {
            float score = score(docId, subQueryExpl.getValue());
            Explanation exp = new Explanation(score, "script score function: product of:");
            exp.addDetail(subQueryExpl);
            return exp;
        }
    }
}