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

package org.elasticsearch.index.mapper;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.NotThreadSafe;
import org.elasticsearch.common.util.concurrent.ThreadSafe;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.index.analysis.AnalysisService;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
@ThreadSafe
public interface Mapper extends ToXContent {

    public static final Mapper[] EMPTY_ARRAY = new Mapper[0];

    @NotThreadSafe
    public static class BuilderContext {
        private final Settings indexSettings;
        private final ContentPath contentPath;

        public BuilderContext(@Nullable Settings indexSettings, ContentPath contentPath) {
            this.contentPath = contentPath;
            this.indexSettings = indexSettings;
        }

        public ContentPath path() {
            return this.contentPath;
        }

        @Nullable
        public Settings indexSettings() {
            return this.indexSettings;
        }
    }

    @NotThreadSafe
    public static abstract class Builder<T extends Builder, Y extends Mapper> {

        public String name;

        protected T builder;

        protected Builder(String name) {
            this.name = name;
        }

        public String name() {
            return this.name;
        }

        public abstract Y build(BuilderContext context);
    }

    public interface TypeParser {

        public static class ParserContext {

            private final AnalysisService analysisService;

            private final ImmutableMap<String, TypeParser> typeParsers;

            public ParserContext(AnalysisService analysisService, ImmutableMap<String, TypeParser> typeParsers) {
                this.analysisService = analysisService;
                this.typeParsers = typeParsers;
            }

            public AnalysisService analysisService() {
                return analysisService;
            }

            public TypeParser typeParser(String type) {
                return typeParsers.get(Strings.toUnderscoreCase(type));
            }
        }

        Mapper.Builder parse(String name, Map<String, Object> node, ParserContext parserContext) throws MapperParsingException;
    }

    String name();

    void parse(ParseContext context) throws IOException;

    void merge(Mapper mergeWith, MergeContext mergeContext) throws MergeMappingException;

    void traverse(FieldMapperListener fieldMapperListener);

    void traverse(ObjectMapperListener objectMapperListener);

    void close();
}
