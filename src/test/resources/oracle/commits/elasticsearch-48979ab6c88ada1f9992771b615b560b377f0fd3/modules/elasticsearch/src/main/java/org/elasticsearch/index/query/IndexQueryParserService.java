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

package org.elasticsearch.index.query;

import org.elasticsearch.index.AbstractIndexComponent;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.cache.IndexCache;
import org.elasticsearch.index.engine.IndexEngine;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.query.xcontent.XContentIndexQueryParser;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.index.similarity.SimilarityService;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.util.collect.ImmutableMap;
import org.elasticsearch.util.inject.Inject;
import org.elasticsearch.util.settings.Settings;

import javax.annotation.Nullable;
import java.util.Map;

import static org.elasticsearch.util.collect.Maps.*;
import static org.elasticsearch.util.settings.ImmutableSettings.Builder.*;

/**
 * @author kimchy (shay.banon)
 */
public class IndexQueryParserService extends AbstractIndexComponent {

    public static final class Defaults {
        public static final String DEFAULT = "default";
        public static final String PREFIX = "index.queryparser.types";
    }

    private final IndexQueryParser defaultIndexQueryParser;

    private final Map<String, IndexQueryParser> indexQueryParsers;

    public IndexQueryParserService(Index index, MapperService mapperService, IndexCache indexCache, IndexEngine indexEngine, AnalysisService analysisService) {
        this(index, EMPTY_SETTINGS, new ScriptService(EMPTY_SETTINGS), mapperService, indexCache, indexEngine, analysisService, null, null);
    }

    @Inject public IndexQueryParserService(Index index, @IndexSettings Settings indexSettings,
                                           ScriptService scriptService,
                                           MapperService mapperService, IndexCache indexCache,
                                           IndexEngine indexEngine, AnalysisService analysisService,
                                           @Nullable SimilarityService similarityService,
                                           @Nullable Map<String, IndexQueryParserFactory> indexQueryParsersFactories) {
        super(index, indexSettings);
        Map<String, Settings> queryParserGroupSettings;
        if (indexSettings != null) {
            queryParserGroupSettings = indexSettings.getGroups(Defaults.PREFIX);
        } else {
            queryParserGroupSettings = newHashMap();
        }
        Map<String, IndexQueryParser> qparsers = newHashMap();
        if (indexQueryParsersFactories != null) {
            for (Map.Entry<String, IndexQueryParserFactory> entry : indexQueryParsersFactories.entrySet()) {
                String qparserName = entry.getKey();
                Settings qparserSettings = queryParserGroupSettings.get(qparserName);
                if (qparserSettings == null) {
                    qparserSettings = EMPTY_SETTINGS;
                }
                qparsers.put(qparserName, entry.getValue().create(qparserName, qparserSettings));
            }
        }
        if (!qparsers.containsKey(Defaults.DEFAULT)) {
            IndexQueryParser defaultQueryParser = new XContentIndexQueryParser(index, indexSettings, scriptService, mapperService, indexCache, indexEngine, analysisService, similarityService, null, null, Defaults.DEFAULT, null);
            qparsers.put(Defaults.DEFAULT, defaultQueryParser);
        }

        indexQueryParsers = ImmutableMap.copyOf(qparsers);

        defaultIndexQueryParser = indexQueryParser(Defaults.DEFAULT);
    }

    public IndexQueryParser indexQueryParser(String name) {
        return indexQueryParsers.get(name);
    }

    public IndexQueryParser defaultIndexQueryParser() {
        return defaultIndexQueryParser;
    }
}
