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

package org.elasticsearch.search.aggregations.metrics.tophits;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.SearchScript;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationInitializationException;
import org.elasticsearch.search.aggregations.AggregatorFactories.Builder;
import org.elasticsearch.search.aggregations.AggregatorFactory;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.InternalAggregation.Type;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder.ScriptField;
import org.elasticsearch.search.fetch.StoredFieldsContext;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.fetch.subphase.ScriptFieldsContext;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortAndFormats;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class TopHitsAggregationBuilder extends AbstractAggregationBuilder<TopHitsAggregationBuilder> {
    public static final String NAME = "top_hits";
    private static final InternalAggregation.Type TYPE = new Type(NAME);

    private int from = 0;
    private int size = 3;
    private boolean explain = false;
    private boolean version = false;
    private boolean trackScores = false;
    private List<SortBuilder<?>> sorts = null;
    private HighlightBuilder highlightBuilder;
    private StoredFieldsContext storedFieldsContext;
    private List<String> fieldDataFields;
    private Set<ScriptField> scriptFields;
    private FetchSourceContext fetchSourceContext;

    public TopHitsAggregationBuilder(String name) {
        super(name, TYPE);
    }

    /**
     * Read from a stream.
     */
    public TopHitsAggregationBuilder(StreamInput in) throws IOException {
        super(in, TYPE);
        explain = in.readBoolean();
        fetchSourceContext = in.readOptionalWriteable(FetchSourceContext::new);
        if (in.readBoolean()) {
            int size = in.readVInt();
            fieldDataFields = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                fieldDataFields.add(in.readString());
            }
        }
        storedFieldsContext = in.readOptionalWriteable(StoredFieldsContext::new);
        from = in.readVInt();
        highlightBuilder = in.readOptionalWriteable(HighlightBuilder::new);
        if (in.readBoolean()) {
            int size = in.readVInt();
            scriptFields = new HashSet<>(size);
            for (int i = 0; i < size; i++) {
                scriptFields.add(new ScriptField(in));
            }
        }
        size = in.readVInt();
        if (in.readBoolean()) {
            int size = in.readVInt();
            sorts = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                sorts.add(in.readNamedWriteable(SortBuilder.class));
            }
        }
        trackScores = in.readBoolean();
        version = in.readBoolean();
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        out.writeBoolean(explain);
        out.writeOptionalWriteable(fetchSourceContext);
        boolean hasFieldDataFields = fieldDataFields != null;
        out.writeBoolean(hasFieldDataFields);
        if (hasFieldDataFields) {
            out.writeVInt(fieldDataFields.size());
            for (String fieldName : fieldDataFields) {
                out.writeString(fieldName);
            }
        }
        out.writeOptionalWriteable(storedFieldsContext);
        out.writeVInt(from);
        out.writeOptionalWriteable(highlightBuilder);
        boolean hasScriptFields = scriptFields != null;
        out.writeBoolean(hasScriptFields);
        if (hasScriptFields) {
            out.writeVInt(scriptFields.size());
            for (ScriptField scriptField : scriptFields) {
                scriptField.writeTo(out);
            }
        }
        out.writeVInt(size);
        boolean hasSorts = sorts != null;
        out.writeBoolean(hasSorts);
        if (hasSorts) {
            out.writeVInt(sorts.size());
            for (SortBuilder<?> sort : sorts) {
                out.writeNamedWriteable(sort);
            }
        }
        out.writeBoolean(trackScores);
        out.writeBoolean(version);
    }

    /**
     * From index to start the search from. Defaults to <tt>0</tt>.
     */
    public TopHitsAggregationBuilder from(int from) {
        if (from < 0) {
            throw new IllegalArgumentException("[from] must be greater than or equal to 0. Found [" + from + "] in [" + name + "]");
        }
        this.from = from;
        return this;
    }

    /**
     * Gets the from index to start the search from.
     **/
    public int from() {
        return from;
    }

    /**
     * The number of search hits to return. Defaults to <tt>10</tt>.
     */
    public TopHitsAggregationBuilder size(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("[size] must be greater than or equal to 0. Found [" + size + "] in [" + name + "]");
        }
        this.size = size;
        return this;
    }

    /**
     * Gets the number of search hits to return.
     */
    public int size() {
        return size;
    }

    /**
     * Adds a sort against the given field name and the sort ordering.
     *
     * @param name
     *            The name of the field
     * @param order
     *            The sort ordering
     */
    public TopHitsAggregationBuilder sort(String name, SortOrder order) {
        if (name == null) {
            throw new IllegalArgumentException("sort [name] must not be null: [" + name + "]");
        }
        if (order == null) {
            throw new IllegalArgumentException("sort [order] must not be null: [" + name + "]");
        }
        if (name.equals(ScoreSortBuilder.NAME)) {
            sort(SortBuilders.scoreSort().order(order));
        }
        sort(SortBuilders.fieldSort(name).order(order));
        return this;
    }

    /**
     * Add a sort against the given field name.
     *
     * @param name
     *            The name of the field to sort by
     */
    public TopHitsAggregationBuilder sort(String name) {
        if (name == null) {
            throw new IllegalArgumentException("sort [name] must not be null: [" + name + "]");
        }
        if (name.equals(ScoreSortBuilder.NAME)) {
            sort(SortBuilders.scoreSort());
        }
        sort(SortBuilders.fieldSort(name));
        return this;
    }

    /**
     * Adds a sort builder.
     */
    public TopHitsAggregationBuilder sort(SortBuilder<?> sort) {
        if (sort == null) {
            throw new IllegalArgumentException("[sort] must not be null: [" + name + "]");
        }
        if (sorts == null) {
                sorts = new ArrayList<>();
        }
        sorts.add(sort);
        return this;
    }

    /**
     * Adds a sort builder.
     */
    public TopHitsAggregationBuilder sorts(List<SortBuilder<?>> sorts) {
        if (sorts == null) {
            throw new IllegalArgumentException("[sorts] must not be null: [" + name + "]");
        }
        if (this.sorts == null) {
            this.sorts = new ArrayList<>();
        }
        for (SortBuilder<?> sort : sorts) {
            this.sorts.add(sort);
        }
        return this;
    }

    /**
     * Gets the bytes representing the sort builders for this request.
     */
    public List<SortBuilder<?>> sorts() {
        return sorts;
    }

    /**
     * Adds highlight to perform as part of the search.
     */
    public TopHitsAggregationBuilder highlighter(HighlightBuilder highlightBuilder) {
        if (highlightBuilder == null) {
            throw new IllegalArgumentException("[highlightBuilder] must not be null: [" + name + "]");
        }
        this.highlightBuilder = highlightBuilder;
        return this;
    }

    /**
     * Gets the hightlighter builder for this request.
     */
    public HighlightBuilder highlighter() {
        return highlightBuilder;
    }

    /**
     * Indicates whether the response should contain the stored _source for
     * every hit
     */
    public TopHitsAggregationBuilder fetchSource(boolean fetch) {
        FetchSourceContext fetchSourceContext = this.fetchSourceContext != null ? this.fetchSourceContext
            : FetchSourceContext.FETCH_SOURCE;
        this.fetchSourceContext = new FetchSourceContext(fetch, fetchSourceContext.includes(), fetchSourceContext.excludes());
        return this;
    }

    /**
     * Indicate that _source should be returned with every hit, with an
     * "include" and/or "exclude" set which can include simple wildcard
     * elements.
     *
     * @param include
     *            An optional include (optionally wildcarded) pattern to
     *            filter the returned _source
     * @param exclude
     *            An optional exclude (optionally wildcarded) pattern to
     *            filter the returned _source
     */
    public TopHitsAggregationBuilder fetchSource(@Nullable String include, @Nullable String exclude) {
        fetchSource(include == null ? Strings.EMPTY_ARRAY : new String[] { include },
                exclude == null ? Strings.EMPTY_ARRAY : new String[] { exclude });
        return this;
    }

    /**
     * Indicate that _source should be returned with every hit, with an
     * "include" and/or "exclude" set which can include simple wildcard
     * elements.
     *
     * @param includes
     *            An optional list of include (optionally wildcarded)
     *            pattern to filter the returned _source
     * @param excludes
     *            An optional list of exclude (optionally wildcarded)
     *            pattern to filter the returned _source
     */
    public TopHitsAggregationBuilder fetchSource(@Nullable String[] includes, @Nullable String[] excludes) {
        FetchSourceContext fetchSourceContext = this.fetchSourceContext != null ? this.fetchSourceContext
            : FetchSourceContext.FETCH_SOURCE;
        this.fetchSourceContext = new FetchSourceContext(fetchSourceContext.fetchSource(), includes, excludes);
        return this;
    }

    /**
     * Indicate how the _source should be fetched.
     */
    public TopHitsAggregationBuilder fetchSource(@Nullable FetchSourceContext fetchSourceContext) {
        if (fetchSourceContext == null) {
            throw new IllegalArgumentException("[fetchSourceContext] must not be null: [" + name + "]");
        }
        this.fetchSourceContext = fetchSourceContext;
        return this;
    }

    /**
     * Gets the {@link FetchSourceContext} which defines how the _source
     * should be fetched.
     */
    public FetchSourceContext fetchSource() {
        return fetchSourceContext;
    }

    /**
     * Adds a stored field to load and return (note, it must be stored) as part of the search request.
     * To disable the stored fields entirely (source and metadata fields) use {@code storedField("_none_")}.
     */
    public TopHitsAggregationBuilder storedField(String field) {
        return storedFields(Collections.singletonList(field));
    }

    /**
     * Sets the stored fields to load and return as part of the search request.
     * To disable the stored fields entirely (source and metadata fields) use {@code storedField("_none_")}.
     */
    public TopHitsAggregationBuilder storedFields(List<String> fields) {
        if (fields == null) {
            throw new IllegalArgumentException("[fields] must not be null: [" + name + "]");
        }
        if (storedFieldsContext == null) {
            storedFieldsContext = StoredFieldsContext.fromList(fields);
        } else {
            storedFieldsContext.addFieldNames(fields);
        }
        return this;
    }

    /**
     * Gets the stored fields context
     */
    public StoredFieldsContext storedFields() {
        return storedFieldsContext;
    }

    /**
     * Adds a field to load from the field data cache and return as part of
     * the search request.
     */
    public TopHitsAggregationBuilder fieldDataField(String fieldDataField) {
        if (fieldDataField == null) {
            throw new IllegalArgumentException("[fieldDataField] must not be null: [" + name + "]");
        }
        if (fieldDataFields == null) {
            fieldDataFields = new ArrayList<>();
        }
        fieldDataFields.add(fieldDataField);
        return this;
    }

    /**
     * Adds fields to load from the field data cache and return as part of
     * the search request.
     */
    public TopHitsAggregationBuilder fieldDataFields(List<String> fieldDataFields) {
        if (fieldDataFields == null) {
            throw new IllegalArgumentException("[fieldDataFields] must not be null: [" + name + "]");
        }
        if (this.fieldDataFields == null) {
            this.fieldDataFields = new ArrayList<>();
        }
        this.fieldDataFields.addAll(fieldDataFields);
        return this;
    }

    /**
     * Gets the field-data fields.
     */
    public List<String> fieldDataFields() {
        return fieldDataFields;
    }

    /**
     * Adds a script field under the given name with the provided script.
     *
     * @param name
     *            The name of the field
     * @param script
     *            The script
     */
    public TopHitsAggregationBuilder scriptField(String name, Script script) {
        if (name == null) {
            throw new IllegalArgumentException("scriptField [name] must not be null: [" + name + "]");
        }
        if (script == null) {
            throw new IllegalArgumentException("scriptField [script] must not be null: [" + name + "]");
        }
        scriptField(name, script, false);
        return this;
    }

    /**
     * Adds a script field under the given name with the provided script.
     *
     * @param name
     *            The name of the field
     * @param script
     *            The script
     */
    public TopHitsAggregationBuilder scriptField(String name, Script script, boolean ignoreFailure) {
        if (name == null) {
            throw new IllegalArgumentException("scriptField [name] must not be null: [" + name + "]");
        }
        if (script == null) {
            throw new IllegalArgumentException("scriptField [script] must not be null: [" + name + "]");
        }
        if (scriptFields == null) {
            scriptFields = new HashSet<>();
        }
        scriptFields.add(new ScriptField(name, script, ignoreFailure));
        return this;
    }

    public TopHitsAggregationBuilder scriptFields(List<ScriptField> scriptFields) {
        if (scriptFields == null) {
            throw new IllegalArgumentException("[scriptFields] must not be null: [" + name + "]");
        }
        if (this.scriptFields == null) {
            this.scriptFields = new HashSet<>();
        }
        this.scriptFields.addAll(scriptFields);
        return this;
    }

    /**
     * Gets the script fields.
     */
    public Set<ScriptField> scriptFields() {
        return scriptFields;
    }

    /**
     * Should each {@link org.elasticsearch.search.SearchHit} be returned
     * with an explanation of the hit (ranking).
     */
    public TopHitsAggregationBuilder explain(boolean explain) {
        this.explain = explain;
        return this;
    }

    /**
     * Indicates whether each search hit will be returned with an
     * explanation of the hit (ranking)
     */
    public boolean explain() {
        return explain;
    }

    /**
     * Should each {@link org.elasticsearch.search.SearchHit} be returned
     * with a version associated with it.
     */
    public TopHitsAggregationBuilder version(boolean version) {
        this.version = version;
        return this;
    }

    /**
     * Indicates whether the document's version will be included in the
     * search hits.
     */
    public boolean version() {
        return version;
    }

    /**
     * Applies when sorting, and controls if scores will be tracked as well.
     * Defaults to <tt>false</tt>.
     */
    public TopHitsAggregationBuilder trackScores(boolean trackScores) {
        this.trackScores = trackScores;
        return this;
    }

    /**
     * Indicates whether scores will be tracked for this request.
     */
    public boolean trackScores() {
        return trackScores;
    }

    @Override
    public TopHitsAggregationBuilder subAggregations(Builder subFactories) {
        throw new AggregationInitializationException("Aggregator [" + name + "] of type [" + type + "] cannot accept sub-aggregations");
    }

    @Override
    protected TopHitsAggregatorFactory doBuild(SearchContext context, AggregatorFactory<?> parent, Builder subfactoriesBuilder)
            throws IOException {
        List<ScriptFieldsContext.ScriptField> fields = new ArrayList<>();
        if (scriptFields != null) {
            for (ScriptField field : scriptFields) {
                SearchScript searchScript = context.getQueryShardContext().getSearchScript(field.script(),
                    ScriptContext.Standard.SEARCH);
                fields.add(new org.elasticsearch.search.fetch.subphase.ScriptFieldsContext.ScriptField(
                    field.fieldName(), searchScript, field.ignoreFailure()));
            }
        }

        final Optional<SortAndFormats> optionalSort;
        if (sorts == null) {
            optionalSort = Optional.empty();
        } else {
            optionalSort = SortBuilder.buildSort(sorts, context.getQueryShardContext());
        }
        return new TopHitsAggregatorFactory(name, type, from, size, explain, version, trackScores, optionalSort, highlightBuilder,
                storedFieldsContext, fieldDataFields, fields, fetchSourceContext, context, parent, subfactoriesBuilder, metaData);
    }

    @Override
    protected XContentBuilder internalXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(SearchSourceBuilder.FROM_FIELD.getPreferredName(), from);
        builder.field(SearchSourceBuilder.SIZE_FIELD.getPreferredName(), size);
        builder.field(SearchSourceBuilder.VERSION_FIELD.getPreferredName(), version);
        builder.field(SearchSourceBuilder.EXPLAIN_FIELD.getPreferredName(), explain);
        if (fetchSourceContext != null) {
            builder.field(SearchSourceBuilder._SOURCE_FIELD.getPreferredName(), fetchSourceContext);
        }
        if (storedFieldsContext != null) {
            storedFieldsContext.toXContent(SearchSourceBuilder.STORED_FIELDS_FIELD.getPreferredName(), builder);
        }
        if (fieldDataFields != null) {
            builder.startArray(SearchSourceBuilder.DOCVALUE_FIELDS_FIELD.getPreferredName());
            for (String fieldDataField : fieldDataFields) {
                builder.value(fieldDataField);
            }
            builder.endArray();
        }
        if (scriptFields != null) {
            builder.startObject(SearchSourceBuilder.SCRIPT_FIELDS_FIELD.getPreferredName());
            for (ScriptField scriptField : scriptFields) {
                scriptField.toXContent(builder, params);
            }
            builder.endObject();
        }
        if (sorts != null) {
            builder.startArray(SearchSourceBuilder.SORT_FIELD.getPreferredName());
            for (SortBuilder<?> sort : sorts) {
                    sort.toXContent(builder, params);
            }
            builder.endArray();
        }
        if (trackScores) {
            builder.field(SearchSourceBuilder.TRACK_SCORES_FIELD.getPreferredName(), true);
        }
        if (highlightBuilder != null) {
            builder.field(SearchSourceBuilder.HIGHLIGHT_FIELD.getPreferredName(), highlightBuilder);
        }
        builder.endObject();
        return builder;
    }

    public static TopHitsAggregationBuilder parse(String aggregationName, QueryParseContext context) throws IOException {
        TopHitsAggregationBuilder factory = new TopHitsAggregationBuilder(aggregationName);
        XContentParser.Token token;
        String currentFieldName = null;
        XContentParser parser = context.parser();
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token.isValue()) {
                if (SearchSourceBuilder.FROM_FIELD.match(currentFieldName)) {
                    factory.from(parser.intValue());
                } else if (SearchSourceBuilder.SIZE_FIELD.match(currentFieldName)) {
                    factory.size(parser.intValue());
                } else if (SearchSourceBuilder.VERSION_FIELD.match(currentFieldName)) {
                    factory.version(parser.booleanValue());
                } else if (SearchSourceBuilder.EXPLAIN_FIELD.match(currentFieldName)) {
                    factory.explain(parser.booleanValue());
                } else if (SearchSourceBuilder.TRACK_SCORES_FIELD.match(currentFieldName)) {
                    factory.trackScores(parser.booleanValue());
                } else if (SearchSourceBuilder._SOURCE_FIELD.match(currentFieldName)) {
                    factory.fetchSource(FetchSourceContext.parse(context.parser()));
                } else if (SearchSourceBuilder.STORED_FIELDS_FIELD.match(currentFieldName)) {
                    factory.storedFieldsContext =
                        StoredFieldsContext.fromXContent(SearchSourceBuilder.STORED_FIELDS_FIELD.getPreferredName(), context);
                } else if (SearchSourceBuilder.SORT_FIELD.match(currentFieldName)) {
                    factory.sort(parser.text());
                } else {
                    throw new ParsingException(parser.getTokenLocation(), "Unknown key for a " + token + " in [" + currentFieldName + "].",
                            parser.getTokenLocation());
                }
            } else if (token == XContentParser.Token.START_OBJECT) {
                if (SearchSourceBuilder._SOURCE_FIELD.match(currentFieldName)) {
                    factory.fetchSource(FetchSourceContext.parse(context.parser()));
                } else if (SearchSourceBuilder.SCRIPT_FIELDS_FIELD.match(currentFieldName)) {
                    List<ScriptField> scriptFields = new ArrayList<>();
                    while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                        String scriptFieldName = parser.currentName();
                        token = parser.nextToken();
                        if (token == XContentParser.Token.START_OBJECT) {
                            Script script = null;
                            boolean ignoreFailure = false;
                            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                                if (token == XContentParser.Token.FIELD_NAME) {
                                    currentFieldName = parser.currentName();
                                } else if (token.isValue()) {
                                    if (SearchSourceBuilder.SCRIPT_FIELD.match(currentFieldName)) {
                                        script = Script.parse(parser, context.getDefaultScriptLanguage());
                                    } else if (SearchSourceBuilder.IGNORE_FAILURE_FIELD.match(currentFieldName)) {
                                        ignoreFailure = parser.booleanValue();
                                    } else {
                                        throw new ParsingException(parser.getTokenLocation(),
                                                "Unknown key for a " + token + " in [" + currentFieldName + "].",
                                                parser.getTokenLocation());
                                    }
                                } else if (token == XContentParser.Token.START_OBJECT) {
                                    if (SearchSourceBuilder.SCRIPT_FIELD.match(currentFieldName)) {
                                        script = Script.parse(parser, context.getDefaultScriptLanguage());
                                    } else {
                                        throw new ParsingException(parser.getTokenLocation(),
                                                "Unknown key for a " + token + " in [" + currentFieldName + "].",
                                                parser.getTokenLocation());
                                    }
                                } else {
                                    throw new ParsingException(parser.getTokenLocation(),
                                            "Unknown key for a " + token + " in [" + currentFieldName + "].", parser.getTokenLocation());
                                }
                            }
                            scriptFields.add(new ScriptField(scriptFieldName, script, ignoreFailure));
                        } else {
                            throw new ParsingException(parser.getTokenLocation(), "Expected [" + XContentParser.Token.START_OBJECT
                                    + "] in [" + currentFieldName + "] but found [" + token + "]", parser.getTokenLocation());
                        }
                    }
                    factory.scriptFields(scriptFields);
                } else if (SearchSourceBuilder.HIGHLIGHT_FIELD.match(currentFieldName)) {
                    factory.highlighter(HighlightBuilder.fromXContent(context));
                } else if (SearchSourceBuilder.SORT_FIELD.match(currentFieldName)) {
                    List<SortBuilder<?>> sorts = SortBuilder.fromXContent(context);
                    factory.sorts(sorts);
                } else {
                    throw new ParsingException(parser.getTokenLocation(), "Unknown key for a " + token + " in [" + currentFieldName + "].",
                            parser.getTokenLocation());
                }
            } else if (token == XContentParser.Token.START_ARRAY) {

                if (SearchSourceBuilder.STORED_FIELDS_FIELD.match(currentFieldName)) {
                    factory.storedFieldsContext =
                        StoredFieldsContext.fromXContent(SearchSourceBuilder.STORED_FIELDS_FIELD.getPreferredName(), context);
                } else if (SearchSourceBuilder.DOCVALUE_FIELDS_FIELD.match(currentFieldName)) {
                    List<String> fieldDataFields = new ArrayList<>();
                    while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                        if (token == XContentParser.Token.VALUE_STRING) {
                            fieldDataFields.add(parser.text());
                        } else {
                            throw new ParsingException(parser.getTokenLocation(), "Expected [" + XContentParser.Token.VALUE_STRING
                                    + "] in [" + currentFieldName + "] but found [" + token + "]", parser.getTokenLocation());
                        }
                    }
                    factory.fieldDataFields(fieldDataFields);
                } else if (SearchSourceBuilder.SORT_FIELD.match(currentFieldName)) {
                    List<SortBuilder<?>> sorts = SortBuilder.fromXContent(context);
                    factory.sorts(sorts);
                } else if (SearchSourceBuilder._SOURCE_FIELD.match(currentFieldName)) {
                    factory.fetchSource(FetchSourceContext.parse(context.parser()));
                } else {
                    throw new ParsingException(parser.getTokenLocation(), "Unknown key for a " + token + " in [" + currentFieldName + "].",
                            parser.getTokenLocation());
                }
            } else {
                throw new ParsingException(parser.getTokenLocation(), "Unknown key for a " + token + " in [" + currentFieldName + "].",
                        parser.getTokenLocation());
            }
        }
        return factory;
    }

    @Override
    protected int doHashCode() {
        return Objects.hash(explain, fetchSourceContext, fieldDataFields, storedFieldsContext, from, highlightBuilder,
            scriptFields, size, sorts, trackScores, version);
    }

    @Override
    protected boolean doEquals(Object obj) {
        TopHitsAggregationBuilder other = (TopHitsAggregationBuilder) obj;
        return Objects.equals(explain, other.explain)
                && Objects.equals(fetchSourceContext, other.fetchSourceContext)
                && Objects.equals(fieldDataFields, other.fieldDataFields)
                && Objects.equals(storedFieldsContext, other.storedFieldsContext)
                && Objects.equals(from, other.from)
                && Objects.equals(highlightBuilder, other.highlightBuilder)
                && Objects.equals(scriptFields, other.scriptFields)
                && Objects.equals(size, other.size)
                && Objects.equals(sorts, other.sorts)
                && Objects.equals(trackScores, other.trackScores)
                && Objects.equals(version, other.version);
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }
}
