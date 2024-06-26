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

package org.elasticsearch.search;

import org.apache.lucene.search.BooleanQuery;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.geo.ShapesAvailability;
import org.elasticsearch.common.geo.builders.ShapeBuilders;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.Multibinder;
import org.elasticsearch.common.io.stream.NamedWriteable;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.lucene.search.function.ScoreFunction;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.percolator.PercolatorHighlightSubFetchPhase;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.BoostingQueryParser;
import org.elasticsearch.index.query.CommonTermsQueryBuilder;
import org.elasticsearch.index.query.ConstantScoreQueryParser;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.EmptyQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.FieldMaskingSpanQueryParser;
import org.elasticsearch.index.query.FuzzyQueryParser;
import org.elasticsearch.index.query.GeoBoundingBoxQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceRangeQueryBuilder;
import org.elasticsearch.index.query.GeoPolygonQueryBuilder;
import org.elasticsearch.index.query.GeoShapeQueryBuilder;
import org.elasticsearch.index.query.GeohashCellQuery;
import org.elasticsearch.index.query.HasChildQueryBuilder;
import org.elasticsearch.index.query.HasParentQueryBuilder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.IndicesQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryParser;
import org.elasticsearch.index.query.MatchNoneQueryBuilder;
import org.elasticsearch.index.query.MatchPhrasePrefixQueryBuilder;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.MoreLikeThisQueryParser;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.ParentIdQueryBuilder;
import org.elasticsearch.index.query.PercolatorQueryBuilder;
import org.elasticsearch.index.query.PrefixQueryParser;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.index.query.QueryParser;
import org.elasticsearch.index.query.QueryStringQueryParser;
import org.elasticsearch.index.query.RangeQueryParser;
import org.elasticsearch.index.query.RegexpQueryParser;
import org.elasticsearch.index.query.ScriptQueryBuilder;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.index.query.SpanContainingQueryParser;
import org.elasticsearch.index.query.SpanFirstQueryParser;
import org.elasticsearch.index.query.SpanMultiTermQueryBuilder;
import org.elasticsearch.index.query.SpanNearQueryParser;
import org.elasticsearch.index.query.SpanNotQueryParser;
import org.elasticsearch.index.query.SpanOrQueryParser;
import org.elasticsearch.index.query.SpanTermQueryParser;
import org.elasticsearch.index.query.SpanWithinQueryParser;
import org.elasticsearch.index.query.TemplateQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryParser;
import org.elasticsearch.index.query.TypeQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryParser;
import org.elasticsearch.index.query.WrapperQueryBuilder;
import org.elasticsearch.index.query.functionscore.ExponentialDecayFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.GaussDecayFunctionBuilder;
import org.elasticsearch.index.query.functionscore.LinearDecayFunctionBuilder;
import org.elasticsearch.index.query.functionscore.RandomScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionParser;
import org.elasticsearch.index.query.functionscore.ScoreFunctionsRegistry;
import org.elasticsearch.index.query.functionscore.ScriptScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.WeightBuilder;
import org.elasticsearch.indices.query.IndicesQueriesRegistry;
import org.elasticsearch.search.action.SearchTransportService;
import org.elasticsearch.search.aggregations.AggregationBinaryParseElement;
import org.elasticsearch.search.aggregations.AggregationParseElement;
import org.elasticsearch.search.aggregations.AggregationPhase;
import org.elasticsearch.search.aggregations.Aggregator;
import org.elasticsearch.search.aggregations.AggregatorParsers;
import org.elasticsearch.search.aggregations.bucket.children.ChildrenParser;
import org.elasticsearch.search.aggregations.bucket.children.InternalChildren;
import org.elasticsearch.search.aggregations.bucket.filter.FilterParser;
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter;
import org.elasticsearch.search.aggregations.bucket.filters.FiltersParser;
import org.elasticsearch.search.aggregations.bucket.filters.InternalFilters;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoHashGridParser;
import org.elasticsearch.search.aggregations.bucket.geogrid.InternalGeoHashGrid;
import org.elasticsearch.search.aggregations.bucket.global.GlobalParser;
import org.elasticsearch.search.aggregations.bucket.global.InternalGlobal;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramParser;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramParser;
import org.elasticsearch.search.aggregations.bucket.histogram.InternalHistogram;
import org.elasticsearch.search.aggregations.bucket.missing.InternalMissing;
import org.elasticsearch.search.aggregations.bucket.missing.MissingParser;
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested;
import org.elasticsearch.search.aggregations.bucket.nested.InternalReverseNested;
import org.elasticsearch.search.aggregations.bucket.nested.NestedParser;
import org.elasticsearch.search.aggregations.bucket.nested.ReverseNestedParser;
import org.elasticsearch.search.aggregations.bucket.range.InternalRange;
import org.elasticsearch.search.aggregations.bucket.range.RangeParser;
import org.elasticsearch.search.aggregations.bucket.range.date.DateRangeParser;
import org.elasticsearch.search.aggregations.bucket.range.date.InternalDateRange;
import org.elasticsearch.search.aggregations.bucket.range.geodistance.GeoDistanceParser;
import org.elasticsearch.search.aggregations.bucket.range.geodistance.InternalGeoDistance;
import org.elasticsearch.search.aggregations.bucket.range.ipv4.InternalIPv4Range;
import org.elasticsearch.search.aggregations.bucket.range.ipv4.IpRangeParser;
import org.elasticsearch.search.aggregations.bucket.sampler.DiversifiedSamplerParser;
import org.elasticsearch.search.aggregations.bucket.sampler.InternalSampler;
import org.elasticsearch.search.aggregations.bucket.sampler.SamplerParser;
import org.elasticsearch.search.aggregations.bucket.sampler.UnmappedSampler;
import org.elasticsearch.search.aggregations.bucket.significant.SignificantLongTerms;
import org.elasticsearch.search.aggregations.bucket.significant.SignificantStringTerms;
import org.elasticsearch.search.aggregations.bucket.significant.SignificantTermsParser;
import org.elasticsearch.search.aggregations.bucket.significant.UnmappedSignificantTerms;
import org.elasticsearch.search.aggregations.bucket.significant.heuristics.SignificanceHeuristicParser;
import org.elasticsearch.search.aggregations.bucket.significant.heuristics.SignificanceHeuristicParserMapper;
import org.elasticsearch.search.aggregations.bucket.terms.DoubleTerms;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsParser;
import org.elasticsearch.search.aggregations.bucket.terms.UnmappedTerms;
import org.elasticsearch.search.aggregations.metrics.avg.AvgParser;
import org.elasticsearch.search.aggregations.metrics.avg.InternalAvg;
import org.elasticsearch.search.aggregations.metrics.cardinality.CardinalityParser;
import org.elasticsearch.search.aggregations.metrics.cardinality.InternalCardinality;
import org.elasticsearch.search.aggregations.metrics.geobounds.GeoBoundsParser;
import org.elasticsearch.search.aggregations.metrics.geobounds.InternalGeoBounds;
import org.elasticsearch.search.aggregations.metrics.geocentroid.GeoCentroidParser;
import org.elasticsearch.search.aggregations.metrics.geocentroid.InternalGeoCentroid;
import org.elasticsearch.search.aggregations.metrics.max.InternalMax;
import org.elasticsearch.search.aggregations.metrics.max.MaxParser;
import org.elasticsearch.search.aggregations.metrics.min.InternalMin;
import org.elasticsearch.search.aggregations.metrics.min.MinParser;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentileRanksParser;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentilesParser;
import org.elasticsearch.search.aggregations.metrics.percentiles.hdr.InternalHDRPercentileRanks;
import org.elasticsearch.search.aggregations.metrics.percentiles.hdr.InternalHDRPercentiles;
import org.elasticsearch.search.aggregations.metrics.percentiles.tdigest.InternalTDigestPercentileRanks;
import org.elasticsearch.search.aggregations.metrics.percentiles.tdigest.InternalTDigestPercentiles;
import org.elasticsearch.search.aggregations.metrics.scripted.InternalScriptedMetric;
import org.elasticsearch.search.aggregations.metrics.scripted.ScriptedMetricParser;
import org.elasticsearch.search.aggregations.metrics.stats.InternalStats;
import org.elasticsearch.search.aggregations.metrics.stats.StatsParser;
import org.elasticsearch.search.aggregations.metrics.stats.extended.ExtendedStatsParser;
import org.elasticsearch.search.aggregations.metrics.stats.extended.InternalExtendedStats;
import org.elasticsearch.search.aggregations.metrics.sum.InternalSum;
import org.elasticsearch.search.aggregations.metrics.sum.SumParser;
import org.elasticsearch.search.aggregations.metrics.tophits.InternalTopHits;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsParser;
import org.elasticsearch.search.aggregations.metrics.valuecount.InternalValueCount;
import org.elasticsearch.search.aggregations.metrics.valuecount.ValueCountParser;
import org.elasticsearch.search.aggregations.pipeline.InternalSimpleValue;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.InternalBucketMetricValue;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.avg.AvgBucketParser;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.avg.AvgBucketPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.max.MaxBucketParser;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.max.MaxBucketPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.min.MinBucketParser;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.min.MinBucketPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.percentile.PercentilesBucketParser;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.percentile.PercentilesBucketPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.stats.StatsBucketParser;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.stats.StatsBucketPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.stats.extended.ExtendedStatsBucketParser;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.stats.extended.ExtendedStatsBucketPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.sum.SumBucketParser;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.sum.SumBucketPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketscript.BucketScriptParser;
import org.elasticsearch.search.aggregations.pipeline.bucketscript.BucketScriptPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketselector.BucketSelectorParser;
import org.elasticsearch.search.aggregations.pipeline.bucketselector.BucketSelectorPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.cumulativesum.CumulativeSumParser;
import org.elasticsearch.search.aggregations.pipeline.cumulativesum.CumulativeSumPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.derivative.DerivativeParser;
import org.elasticsearch.search.aggregations.pipeline.derivative.DerivativePipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.derivative.InternalDerivative;
import org.elasticsearch.search.aggregations.pipeline.movavg.MovAvgParser;
import org.elasticsearch.search.aggregations.pipeline.movavg.MovAvgPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.movavg.models.MovAvgModel;
import org.elasticsearch.search.aggregations.pipeline.movavg.models.MovAvgModelParserMapper;
import org.elasticsearch.search.aggregations.pipeline.serialdiff.SerialDiffParser;
import org.elasticsearch.search.aggregations.pipeline.serialdiff.SerialDiffPipelineAggregator;
import org.elasticsearch.search.controller.SearchPhaseController;
import org.elasticsearch.search.dfs.DfsPhase;
import org.elasticsearch.search.fetch.FetchPhase;
import org.elasticsearch.search.fetch.FetchSubPhase;
import org.elasticsearch.search.fetch.explain.ExplainFetchSubPhase;
import org.elasticsearch.search.fetch.fielddata.FieldDataFieldsFetchSubPhase;
import org.elasticsearch.search.fetch.innerhits.InnerHitsFetchSubPhase;
import org.elasticsearch.search.fetch.matchedqueries.MatchedQueriesFetchSubPhase;
import org.elasticsearch.search.fetch.parent.ParentFieldSubFetchPhase;
import org.elasticsearch.search.fetch.script.ScriptFieldsFetchSubPhase;
import org.elasticsearch.search.fetch.source.FetchSourceSubPhase;
import org.elasticsearch.search.fetch.version.VersionFetchSubPhase;
import org.elasticsearch.search.highlight.HighlightPhase;
import org.elasticsearch.search.highlight.Highlighter;
import org.elasticsearch.search.highlight.Highlighters;
import org.elasticsearch.search.query.QueryPhase;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.elasticsearch.search.rescore.RescoreBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.suggest.Suggester;
import org.elasticsearch.search.suggest.Suggesters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class SearchModule extends AbstractModule {

    private final Set<Aggregator.Parser> aggParsers = new HashSet<>();
    private final Set<PipelineAggregator.Parser> pipelineAggParsers = new HashSet<>();
    private final Highlighters highlighters = new Highlighters();
    private final Suggesters suggesters;
    /**
     * Map from name to score parser and its ParseField.
     */
    private final Map<String, Tuple<ParseField, ScoreFunctionParser<?>>> scoreFunctionParsers = new HashMap<>();
    private final ScoreFunctionsRegistry scoreFunctionsRegistry = new ScoreFunctionsRegistry(scoreFunctionParsers);
    /**
     * Query parsers constructed at configure time. These have to be constructed
     * at configure time because they depend on things that are registered by
     * plugins (function score parsers).
     */
    private final Map<String, Tuple<ParseField, QueryParser<?>>> queryParsers = new HashMap<>();
    private final Set<Class<? extends FetchSubPhase>> fetchSubPhases = new HashSet<>();
    private final Set<SignificanceHeuristicParser> heuristicParsers = new HashSet<>();
    private final Set<MovAvgModel.AbstractModelParser> modelParsers = new HashSet<>();

    private final Settings settings;
    private final NamedWriteableRegistry namedWriteableRegistry;

    // pkg private so tests can mock
    Class<? extends SearchService> searchServiceImpl = SearchService.class;

    public SearchModule(Settings settings, NamedWriteableRegistry namedWriteableRegistry) {
        this.settings = settings;
        this.namedWriteableRegistry = namedWriteableRegistry;
        suggesters = new Suggesters(namedWriteableRegistry);

        registerBuiltinScoreFunctionParsers();
        registerBuiltinQueryParsers();
        registerBuiltinRescorers();
        registerBuiltinSorts();
        registerBuiltinValueFormats();
    }

    public void registerHighlighter(String key, Class<? extends Highlighter> clazz) {
        highlighters.registerExtension(key, clazz);
    }

    public void registerSuggester(String key, Suggester<?> suggester) {
        suggesters.register(key, suggester);
    }

    /**
     * Register a new ScoreFunctionBuilder. Registration does two things:
     * <ul>
     * <li>Register the {@link ScoreFunctionParser} which parses XContent into a {@link ScoreFunctionBuilder} using its {@link ParseField}
     * </li>
     * <li>Register the {@link Writeable.Reader} which reads a stream representation of the builder under the
     * {@linkplain ParseField#getPreferredName()}.</li>
     * </ul>
     */
    public <T extends ScoreFunctionBuilder<T>> void registerScoreFunction(ScoreFunctionParser<T> parser,
            Writeable.Reader<T> reader, ParseField functionName) {
        for (String name: functionName.getAllNamesIncludedDeprecated()) {
            Tuple<ParseField, ScoreFunctionParser<?>> oldValue = scoreFunctionParsers.putIfAbsent(name, new Tuple<>(functionName, parser));
            if (oldValue != null) {
                throw new IllegalArgumentException(
                        "Function score parser [" + oldValue.v2() + "] already registered for name [" + name + "]");
            }
        }
        namedWriteableRegistry.register(ScoreFunctionBuilder.class, functionName.getPreferredName(), reader);
    }

    /**
     * Fetch the registry of {@linkplain ScoreFunction}s. This is public so extensions can access the score functions.
     */
    public ScoreFunctionsRegistry getScoreFunctionsRegistry() {
        return scoreFunctionsRegistry;
    }

    /**
     * Register a new ValueFormat.
     */
    // private for now, we can consider making it public if there are actual use cases for plugins
    // to register custom value formats
    private void registerValueFormat(String name, Writeable.Reader<? extends DocValueFormat> reader) {
        namedWriteableRegistry.register(DocValueFormat.class, name, reader);
    }

    /**
     * Register a query.
     *
     * @param reader the reader registered for this query's builder. Typically a reference to a constructor that takes a
     *        {@link org.elasticsearch.common.io.stream.StreamInput}
     * @param queryParser the parser the reads the query builder from xcontent
     * @param queryName holds the names by which this query might be parsed. The {@link ParseField#getPreferredName()} is special as it
     *        is the name by under which the reader is registered. So it is the name that the query should use as its
     *        {@link NamedWriteable#getWriteableName()} too.
     */
    public <QB extends QueryBuilder<QB>> void registerQuery(Writeable.Reader<QB> reader, QueryParser<QB> queryParser,
                                                                         ParseField queryName) {
        innerRegisterQueryParser(queryParser, queryName);
        namedWriteableRegistry.register(QueryBuilder.class, queryName.getPreferredName(), reader);
    }

    /**
     * Register a query via its parser's prototype.
     * TODO remove this in favor of registerQuery and merge innerRegisterQueryParser into registerQuery
     */
    public void registerQueryParser(QueryParser<?> queryParser, ParseField queryName) {
        innerRegisterQueryParser(queryParser, queryName);
        namedWriteableRegistry.registerPrototype(QueryBuilder.class, queryParser.getBuilderPrototype());
    }

    private <QB extends QueryBuilder<QB>> void innerRegisterQueryParser(QueryParser<QB> parser, ParseField queryName) {
        Tuple<ParseField, QueryParser<?>> parseFieldQueryParserTuple = new Tuple<>(queryName, parser);
        for (String name: queryName.getAllNamesIncludedDeprecated()) {
            Tuple<ParseField, QueryParser<?>> previousValue = queryParsers.putIfAbsent(name, parseFieldQueryParserTuple);
            if (previousValue != null) {
                throw new IllegalArgumentException("Query parser [" + previousValue.v2() + "] already registered for name [" +
                        name + "] while trying to register [" + parser + "]");
            }
        }
    }

    Set<String> getRegisteredQueries() {
        return queryParsers.keySet();
    }

    public void registerFetchSubPhase(Class<? extends FetchSubPhase> subPhase) {
        fetchSubPhases.add(subPhase);
    }

    public void registerHeuristicParser(SignificanceHeuristicParser parser) {
        heuristicParsers.add(parser);
    }

    public void registerModelParser(MovAvgModel.AbstractModelParser parser) {
        modelParsers.add(parser);
    }

    /**
     * Enabling extending the get module by adding a custom aggregation parser.
     *
     * @param parser The parser for the custom aggregator.
     */
    public void registerAggregatorParser(Aggregator.Parser parser) {
        aggParsers.add(parser);
    }

    public void registerPipelineParser(PipelineAggregator.Parser parser) {
        pipelineAggParsers.add(parser);
    }

    @Override
    protected void configure() {
        IndicesQueriesRegistry indicesQueriesRegistry = buildQueryParserRegistry();
        bind(IndicesQueriesRegistry.class).toInstance(indicesQueriesRegistry);
        bind(Suggesters.class).toInstance(suggesters);
        configureSearch();
        configureAggs(indicesQueriesRegistry);
        configureHighlighters();
        configureFetchSubPhase();
        configureShapes();
    }

    protected void configureFetchSubPhase() {
        Multibinder<FetchSubPhase> fetchSubPhaseMultibinder = Multibinder.newSetBinder(binder(), FetchSubPhase.class);
        fetchSubPhaseMultibinder.addBinding().to(ExplainFetchSubPhase.class);
        fetchSubPhaseMultibinder.addBinding().to(FieldDataFieldsFetchSubPhase.class);
        fetchSubPhaseMultibinder.addBinding().to(ScriptFieldsFetchSubPhase.class);
        fetchSubPhaseMultibinder.addBinding().to(FetchSourceSubPhase.class);
        fetchSubPhaseMultibinder.addBinding().to(VersionFetchSubPhase.class);
        fetchSubPhaseMultibinder.addBinding().to(MatchedQueriesFetchSubPhase.class);
        fetchSubPhaseMultibinder.addBinding().to(HighlightPhase.class);
        fetchSubPhaseMultibinder.addBinding().to(ParentFieldSubFetchPhase.class);
        fetchSubPhaseMultibinder.addBinding().to(PercolatorHighlightSubFetchPhase.class);
        for (Class<? extends FetchSubPhase> clazz : fetchSubPhases) {
            fetchSubPhaseMultibinder.addBinding().to(clazz);
        }
        bind(InnerHitsFetchSubPhase.class).asEagerSingleton();
    }

    public IndicesQueriesRegistry buildQueryParserRegistry() {
        return new IndicesQueriesRegistry(settings, queryParsers);
    }

    protected void configureHighlighters() {
       highlighters.bind(binder());
    }

    protected void configureAggs(IndicesQueriesRegistry indicesQueriesRegistry) {

        MovAvgModelParserMapper movAvgModelParserMapper = new MovAvgModelParserMapper(modelParsers);

        SignificanceHeuristicParserMapper significanceHeuristicParserMapper = new SignificanceHeuristicParserMapper(heuristicParsers);

        registerAggregatorParser(new AvgParser());
        registerAggregatorParser(new SumParser());
        registerAggregatorParser(new MinParser());
        registerAggregatorParser(new MaxParser());
        registerAggregatorParser(new StatsParser());
        registerAggregatorParser(new ExtendedStatsParser());
        registerAggregatorParser(new ValueCountParser());
        registerAggregatorParser(new PercentilesParser());
        registerAggregatorParser(new PercentileRanksParser());
        registerAggregatorParser(new CardinalityParser());
        registerAggregatorParser(new GlobalParser());
        registerAggregatorParser(new MissingParser());
        registerAggregatorParser(new FilterParser());
        registerAggregatorParser(new FiltersParser(indicesQueriesRegistry));
        registerAggregatorParser(new SamplerParser());
        registerAggregatorParser(new DiversifiedSamplerParser());
        registerAggregatorParser(new TermsParser());
        registerAggregatorParser(new SignificantTermsParser(significanceHeuristicParserMapper, indicesQueriesRegistry));
        registerAggregatorParser(new RangeParser());
        registerAggregatorParser(new DateRangeParser());
        registerAggregatorParser(new IpRangeParser());
        registerAggregatorParser(new HistogramParser());
        registerAggregatorParser(new DateHistogramParser());
        registerAggregatorParser(new GeoDistanceParser());
        registerAggregatorParser(new GeoHashGridParser());
        registerAggregatorParser(new NestedParser());
        registerAggregatorParser(new ReverseNestedParser());
        registerAggregatorParser(new TopHitsParser());
        registerAggregatorParser(new GeoBoundsParser());
        registerAggregatorParser(new GeoCentroidParser());
        registerAggregatorParser(new ScriptedMetricParser());
        registerAggregatorParser(new ChildrenParser());

        registerPipelineParser(new DerivativeParser());
        registerPipelineParser(new MaxBucketParser());
        registerPipelineParser(new MinBucketParser());
        registerPipelineParser(new AvgBucketParser());
        registerPipelineParser(new SumBucketParser());
        registerPipelineParser(new StatsBucketParser());
        registerPipelineParser(new ExtendedStatsBucketParser());
        registerPipelineParser(new PercentilesBucketParser());
        registerPipelineParser(new MovAvgParser(movAvgModelParserMapper));
        registerPipelineParser(new CumulativeSumParser());
        registerPipelineParser(new BucketScriptParser());
        registerPipelineParser(new BucketSelectorParser());
        registerPipelineParser(new SerialDiffParser());

        AggregatorParsers aggregatorParsers = new AggregatorParsers(aggParsers, pipelineAggParsers, namedWriteableRegistry);
        AggregationParseElement aggParseElement = new AggregationParseElement(aggregatorParsers, indicesQueriesRegistry);
        AggregationBinaryParseElement aggBinaryParseElement = new AggregationBinaryParseElement(aggregatorParsers, indicesQueriesRegistry);
        AggregationPhase aggPhase = new AggregationPhase(aggParseElement, aggBinaryParseElement);
        bind(AggregatorParsers.class).toInstance(aggregatorParsers);
        bind(AggregationParseElement.class).toInstance(aggParseElement);
        bind(AggregationPhase.class).toInstance(aggPhase);
    }

    protected void configureSearch() {
        // configure search private classes...
        bind(DfsPhase.class).asEagerSingleton();
        bind(QueryPhase.class).asEagerSingleton();
        bind(SearchPhaseController.class).asEagerSingleton();
        bind(FetchPhase.class).asEagerSingleton();
        bind(SearchTransportService.class).asEagerSingleton();
        if (searchServiceImpl == SearchService.class) {
            bind(SearchService.class).asEagerSingleton();
        } else {
            bind(SearchService.class).to(searchServiceImpl).asEagerSingleton();
        }
    }

    private void configureShapes() {
        if (ShapesAvailability.JTS_AVAILABLE && ShapesAvailability.SPATIAL4J_AVAILABLE) {
            ShapeBuilders.register(namedWriteableRegistry);
        }
    }

    private void registerBuiltinRescorers() {
        namedWriteableRegistry.register(RescoreBuilder.class, QueryRescorerBuilder.NAME, QueryRescorerBuilder::new);
    }

    private void registerBuiltinSorts() {
        namedWriteableRegistry.register(SortBuilder.class, GeoDistanceSortBuilder.NAME, GeoDistanceSortBuilder::new);
        namedWriteableRegistry.register(SortBuilder.class, ScoreSortBuilder.NAME, ScoreSortBuilder::new);
        namedWriteableRegistry.register(SortBuilder.class, ScriptSortBuilder.NAME, ScriptSortBuilder::new);
        namedWriteableRegistry.register(SortBuilder.class, FieldSortBuilder.NAME, FieldSortBuilder::new);
    }

    private void registerBuiltinScoreFunctionParsers() {
        registerScoreFunction(ScriptScoreFunctionBuilder::fromXContent, ScriptScoreFunctionBuilder::new,
                ScriptScoreFunctionBuilder.FUNCTION_NAME_FIELD);
        registerScoreFunction(GaussDecayFunctionBuilder.PARSER, GaussDecayFunctionBuilder::new,
                GaussDecayFunctionBuilder.FUNCTION_NAME_FIELD);
        registerScoreFunction(LinearDecayFunctionBuilder.PARSER, LinearDecayFunctionBuilder::new,
                LinearDecayFunctionBuilder.FUNCTION_NAME_FIELD);
        registerScoreFunction(ExponentialDecayFunctionBuilder.PARSER, ExponentialDecayFunctionBuilder::new,
                ExponentialDecayFunctionBuilder.FUNCTION_NAME_FIELD);
        registerScoreFunction(RandomScoreFunctionBuilder::fromXContent, RandomScoreFunctionBuilder::new,
                RandomScoreFunctionBuilder.FUNCTION_NAME_FIELD);
        registerScoreFunction(FieldValueFactorFunctionBuilder::fromXContent, FieldValueFactorFunctionBuilder::new,
                FieldValueFactorFunctionBuilder.FUNCTION_NAME_FIELD);

        //weight doesn't have its own parser, so every function supports it out of the box.
        //Can be a single function too when not associated to any other function, which is why it needs to be registered manually here.
        namedWriteableRegistry.register(ScoreFunctionBuilder.class, WeightBuilder.NAME, WeightBuilder::new);
    }

    private void registerBuiltinValueFormats() {
        registerValueFormat(DocValueFormat.BOOLEAN.getWriteableName(), in -> DocValueFormat.BOOLEAN);
        registerValueFormat(DocValueFormat.DateTime.NAME, DocValueFormat.DateTime::new);
        registerValueFormat(DocValueFormat.Decimal.NAME, DocValueFormat.Decimal::new);
        registerValueFormat(DocValueFormat.GEOHASH.getWriteableName(), in -> DocValueFormat.GEOHASH);
        registerValueFormat(DocValueFormat.IP.getWriteableName(), in -> DocValueFormat.IP);
        registerValueFormat(DocValueFormat.RAW.getWriteableName(), in -> DocValueFormat.RAW);
    }

    private void registerBuiltinQueryParsers() {
        registerQuery(MatchQueryBuilder.PROTOTYPE::readFrom, MatchQueryBuilder::fromXContent, MatchQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(MatchPhraseQueryBuilder.PROTOTYPE::readFrom, MatchPhraseQueryBuilder::fromXContent,
                MatchPhraseQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(MatchPhrasePrefixQueryBuilder.PROTOTYPE::readFrom, MatchPhrasePrefixQueryBuilder::fromXContent,
                MatchPhrasePrefixQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(MultiMatchQueryBuilder.PROTOTYPE::readFrom, MultiMatchQueryBuilder::fromXContent,
                MultiMatchQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(NestedQueryBuilder.PROTOTYPE::readFrom, NestedQueryBuilder::fromXContent, NestedQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(HasChildQueryBuilder.PROTOTYPE::readFrom, HasChildQueryBuilder::fromXContent, HasChildQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(HasParentQueryBuilder.PROTOTYPE::readFrom, HasParentQueryBuilder::fromXContent,
                HasParentQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(DisMaxQueryBuilder.PROTOTYPE::readFrom, DisMaxQueryBuilder::fromXContent, DisMaxQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(IdsQueryBuilder.PROTOTYPE::readFrom, IdsQueryBuilder::fromXContent, IdsQueryBuilder.QUERY_NAME_FIELD);
        registerQueryParser(new MatchAllQueryParser(), MatchAllQueryParser.QUERY_NAME_FIELD);
        registerQueryParser(new QueryStringQueryParser(), QueryStringQueryParser.QUERY_NAME_FIELD);
        registerQueryParser(new BoostingQueryParser(), BoostingQueryParser.QUERY_NAME_FIELD);
        BooleanQuery.setMaxClauseCount(settings.getAsInt("index.query.bool.max_clause_count",
                settings.getAsInt("indices.query.bool.max_clause_count", BooleanQuery.getMaxClauseCount())));
        registerQuery(BoolQueryBuilder.PROTOTYPE::readFrom, BoolQueryBuilder::fromXContent, BoolQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(TermQueryBuilder.PROTOTYPE::readFrom, TermQueryBuilder::fromXContent, TermQueryBuilder.QUERY_NAME_FIELD);
        registerQueryParser(new TermsQueryParser(), TermsQueryParser.QUERY_NAME_FIELD);
        registerQueryParser(new FuzzyQueryParser(), FuzzyQueryParser.QUERY_NAME_FIELD);
        registerQueryParser(new RegexpQueryParser(), RegexpQueryParser.QUERY_NAME_FIELD);
        registerQueryParser(new RangeQueryParser(), RangeQueryParser.QUERY_NAME_FIELD);
        registerQueryParser(new PrefixQueryParser(), PrefixQueryParser.QUERY_NAME_FIELD);
        registerQueryParser(new WildcardQueryParser(), WildcardQueryParser.QUERY_NAME_FIELD);
        registerQueryParser(new ConstantScoreQueryParser(), ConstantScoreQueryParser.QUERY_NAME_FIELD);
        registerQueryParser(new SpanTermQueryParser(), SpanTermQueryParser.QUERY_NAME_FIELD);
        registerQueryParser(new SpanNotQueryParser(), SpanNotQueryParser.QUERY_NAME_FIELD);
        registerQueryParser(new SpanWithinQueryParser(), SpanWithinQueryParser.QUERY_NAME_FIELD);
        registerQueryParser(new SpanContainingQueryParser(), SpanContainingQueryParser.QUERY_NAME_FIELD);
        registerQueryParser(new FieldMaskingSpanQueryParser(), FieldMaskingSpanQueryParser.QUERY_NAME_FIELD);
        registerQueryParser(new SpanFirstQueryParser(), SpanFirstQueryParser.QUERY_NAME_FIELD);
        registerQueryParser(new SpanNearQueryParser(), SpanNearQueryParser.QUERY_NAME_FIELD);
        registerQueryParser(new SpanOrQueryParser(), SpanOrQueryParser.QUERY_NAME_FIELD);
        registerQueryParser(new MoreLikeThisQueryParser(), MoreLikeThisQueryParser.QUERY_NAME_FIELD);
        registerQuery(WrapperQueryBuilder.PROTOTYPE::readFrom, WrapperQueryBuilder::fromXContent, WrapperQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(IndicesQueryBuilder.PROTOTYPE::readFrom, IndicesQueryBuilder::fromXContent, IndicesQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(CommonTermsQueryBuilder.PROTOTYPE::readFrom, CommonTermsQueryBuilder::fromXContent,
                CommonTermsQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(SpanMultiTermQueryBuilder.PROTOTYPE::readFrom, SpanMultiTermQueryBuilder::fromXContent,
                SpanMultiTermQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(FunctionScoreQueryBuilder.PROTOTYPE::readFrom, c -> FunctionScoreQueryBuilder.fromXContent(scoreFunctionsRegistry, c),
                FunctionScoreQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(SimpleQueryStringBuilder.PROTOTYPE::readFrom, SimpleQueryStringBuilder::fromXContent,
                SimpleQueryStringBuilder.QUERY_NAME_FIELD);
        registerQuery(TemplateQueryBuilder.PROTOTYPE::readFrom, TemplateQueryBuilder::fromXContent, TemplateQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(TypeQueryBuilder.PROTOTYPE::readFrom, TypeQueryBuilder::fromXContent, TypeQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(ScriptQueryBuilder.PROTOTYPE::readFrom, ScriptQueryBuilder::fromXContent, ScriptQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(GeoDistanceQueryBuilder.PROTOTYPE::readFrom, GeoDistanceQueryBuilder::fromXContent,
                GeoDistanceQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(GeoDistanceRangeQueryBuilder.PROTOTYPE::readFrom, GeoDistanceRangeQueryBuilder::fromXContent,
                GeoDistanceRangeQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(GeoBoundingBoxQueryBuilder.PROTOTYPE::readFrom, GeoBoundingBoxQueryBuilder::fromXContent,
                GeoBoundingBoxQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(GeohashCellQuery.Builder.PROTOTYPE::readFrom, GeohashCellQuery.Builder::fromXContent,
                GeohashCellQuery.QUERY_NAME_FIELD);
        registerQuery(GeoPolygonQueryBuilder.PROTOTYPE::readFrom, GeoPolygonQueryBuilder::fromXContent,
                GeoPolygonQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(ExistsQueryBuilder.PROTOTYPE::readFrom, ExistsQueryBuilder::fromXContent, ExistsQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(MatchNoneQueryBuilder.PROTOTYPE::readFrom, MatchNoneQueryBuilder::fromXContent,
                MatchNoneQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(ParentIdQueryBuilder.PROTO::readFrom, ParentIdQueryBuilder::fromXContent, ParentIdQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(PercolatorQueryBuilder.PROTO::readFrom, PercolatorQueryBuilder::fromXContent,
                PercolatorQueryBuilder.QUERY_NAME_FIELD);
        if (ShapesAvailability.JTS_AVAILABLE && ShapesAvailability.SPATIAL4J_AVAILABLE) {
            registerQuery(GeoShapeQueryBuilder.PROTOTYPE::readFrom, GeoShapeQueryBuilder::fromXContent,
                    GeoShapeQueryBuilder.QUERY_NAME_FIELD);
        }
        // EmptyQueryBuilder is not registered as query parser but used internally.
        // We need to register it with the NamedWriteableRegistry in order to serialize it
        namedWriteableRegistry.registerPrototype(QueryBuilder.class, EmptyQueryBuilder.PROTOTYPE);
    }

    static {
        // calcs
        InternalAvg.registerStreams();
        InternalSum.registerStreams();
        InternalMin.registerStreams();
        InternalMax.registerStreams();
        InternalStats.registerStreams();
        InternalExtendedStats.registerStreams();
        InternalValueCount.registerStreams();
        InternalTDigestPercentiles.registerStreams();
        InternalTDigestPercentileRanks.registerStreams();
        InternalHDRPercentiles.registerStreams();
        InternalHDRPercentileRanks.registerStreams();
        InternalCardinality.registerStreams();
        InternalScriptedMetric.registerStreams();
        InternalGeoCentroid.registerStreams();

        // buckets
        InternalGlobal.registerStreams();
        InternalFilter.registerStreams();
        InternalFilters.registerStream();
        InternalSampler.registerStreams();
        UnmappedSampler.registerStreams();
        InternalMissing.registerStreams();
        StringTerms.registerStreams();
        LongTerms.registerStreams();
        SignificantStringTerms.registerStreams();
        SignificantLongTerms.registerStreams();
        UnmappedSignificantTerms.registerStreams();
        InternalGeoHashGrid.registerStreams();
        DoubleTerms.registerStreams();
        UnmappedTerms.registerStreams();
        InternalRange.registerStream();
        InternalDateRange.registerStream();
        InternalIPv4Range.registerStream();
        InternalHistogram.registerStream();
        InternalGeoDistance.registerStream();
        InternalNested.registerStream();
        InternalReverseNested.registerStream();
        InternalTopHits.registerStreams();
        InternalGeoBounds.registerStream();
        InternalChildren.registerStream();

        // Pipeline Aggregations
        DerivativePipelineAggregator.registerStreams();
        InternalDerivative.registerStreams();
        InternalSimpleValue.registerStreams();
        InternalBucketMetricValue.registerStreams();
        MaxBucketPipelineAggregator.registerStreams();
        MinBucketPipelineAggregator.registerStreams();
        AvgBucketPipelineAggregator.registerStreams();
        SumBucketPipelineAggregator.registerStreams();
        StatsBucketPipelineAggregator.registerStreams();
        ExtendedStatsBucketPipelineAggregator.registerStreams();
        PercentilesBucketPipelineAggregator.registerStreams();
        MovAvgPipelineAggregator.registerStreams();
        CumulativeSumPipelineAggregator.registerStreams();
        BucketScriptPipelineAggregator.registerStreams();
        BucketSelectorPipelineAggregator.registerStreams();
        SerialDiffPipelineAggregator.registerStreams();
    }

    public Suggesters getSuggesters() {
        return suggesters;
    }
}
