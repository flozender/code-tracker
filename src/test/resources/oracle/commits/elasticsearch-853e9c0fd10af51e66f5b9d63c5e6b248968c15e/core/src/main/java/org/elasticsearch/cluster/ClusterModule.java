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

package org.elasticsearch.cluster;

import org.elasticsearch.cluster.action.index.MappingUpdatedAction;
import org.elasticsearch.cluster.action.index.NodeIndexDeletedAction;
import org.elasticsearch.cluster.action.index.NodeMappingRefreshAction;
import org.elasticsearch.cluster.action.shard.ShardStateAction;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.metadata.IndexTemplateFilter;
import org.elasticsearch.cluster.metadata.MetaDataCreateIndexService;
import org.elasticsearch.cluster.metadata.MetaDataDeleteIndexService;
import org.elasticsearch.cluster.metadata.MetaDataIndexAliasesService;
import org.elasticsearch.cluster.metadata.MetaDataIndexStateService;
import org.elasticsearch.cluster.metadata.MetaDataIndexTemplateService;
import org.elasticsearch.cluster.metadata.MetaDataMappingService;
import org.elasticsearch.cluster.metadata.MetaDataUpdateSettingsService;
import org.elasticsearch.cluster.node.DiscoveryNodeService;
import org.elasticsearch.cluster.routing.OperationRouting;
import org.elasticsearch.cluster.routing.RoutingService;
import org.elasticsearch.cluster.routing.UnassignedInfo;
import org.elasticsearch.cluster.routing.allocation.AllocationService;
import org.elasticsearch.cluster.routing.allocation.allocator.BalancedShardsAllocator;
import org.elasticsearch.cluster.routing.allocation.allocator.ShardsAllocator;
import org.elasticsearch.cluster.routing.allocation.decider.AllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.AllocationDeciders;
import org.elasticsearch.cluster.routing.allocation.decider.AwarenessAllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.ClusterRebalanceAllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.ConcurrentRebalanceAllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.DiskThresholdDecider;
import org.elasticsearch.cluster.routing.allocation.decider.EnableAllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.FilterAllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.NodeVersionAllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.RebalanceOnlyWhenActiveAllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.ReplicaAfterPrimaryActiveAllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.SameShardAllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.ShardsLimitAllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.SnapshotInProgressAllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.ThrottlingAllocationDecider;
import org.elasticsearch.cluster.service.InternalClusterService;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.cluster.settings.DynamicSettings;
import org.elasticsearch.cluster.settings.Validator;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.ExtensionPoint;
import org.elasticsearch.gateway.GatewayAllocator;
import org.elasticsearch.gateway.PrimaryShardAllocator;
import org.elasticsearch.index.engine.EngineConfig;
import org.elasticsearch.index.indexing.IndexingSlowLog;
import org.elasticsearch.index.search.stats.SearchSlowLog;
import org.elasticsearch.index.settings.IndexDynamicSettings;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.index.shard.MergePolicyConfig;
import org.elasticsearch.index.shard.MergeSchedulerConfig;
import org.elasticsearch.index.store.IndexStore;
import org.elasticsearch.index.translog.TranslogConfig;
import org.elasticsearch.indices.IndicesWarmer;
import org.elasticsearch.indices.cache.request.IndicesRequestCache;
import org.elasticsearch.indices.ttl.IndicesTTLService;
import org.elasticsearch.search.internal.DefaultSearchContext;

import java.util.*;

/**
 * Configures classes and services that affect the entire cluster.
 */
public class ClusterModule extends AbstractModule {

    public static final String EVEN_SHARD_COUNT_ALLOCATOR = "even_shard";
    public static final String BALANCED_ALLOCATOR = "balanced"; // default
    public static final String SHARDS_ALLOCATOR_TYPE_KEY = "cluster.routing.allocation.type";
    public static final List<Class<? extends AllocationDecider>> DEFAULT_ALLOCATION_DECIDERS =
        Collections.unmodifiableList(Arrays.asList(
            SameShardAllocationDecider.class,
            FilterAllocationDecider.class,
            ReplicaAfterPrimaryActiveAllocationDecider.class,
            ThrottlingAllocationDecider.class,
            RebalanceOnlyWhenActiveAllocationDecider.class,
            ClusterRebalanceAllocationDecider.class,
            ConcurrentRebalanceAllocationDecider.class,
            EnableAllocationDecider.class,
            AwarenessAllocationDecider.class,
            ShardsLimitAllocationDecider.class,
            NodeVersionAllocationDecider.class,
            DiskThresholdDecider.class,
            SnapshotInProgressAllocationDecider.class));

    private final Settings settings;
    private final DynamicSettings.Builder indexDynamicSettings = new DynamicSettings.Builder();
    private final ExtensionPoint.SelectedType<ShardsAllocator> shardsAllocators = new ExtensionPoint.SelectedType<>("shards_allocator", ShardsAllocator.class);
    private final ExtensionPoint.ClassSet<AllocationDecider> allocationDeciders = new ExtensionPoint.ClassSet<>("allocation_decider", AllocationDecider.class, AllocationDeciders.class);
    private final ExtensionPoint.ClassSet<IndexTemplateFilter> indexTemplateFilters = new ExtensionPoint.ClassSet<>("index_template_filter", IndexTemplateFilter.class);

    // pkg private so tests can mock
    Class<? extends ClusterInfoService> clusterInfoServiceImpl = InternalClusterInfoService.class;

    public ClusterModule(Settings settings) {
        this.settings = settings;

        registerBuiltinIndexSettings();

        for (Class<? extends AllocationDecider> decider : ClusterModule.DEFAULT_ALLOCATION_DECIDERS) {
            registerAllocationDecider(decider);
        }
        registerShardsAllocator(ClusterModule.BALANCED_ALLOCATOR, BalancedShardsAllocator.class);
        registerShardsAllocator(ClusterModule.EVEN_SHARD_COUNT_ALLOCATOR, BalancedShardsAllocator.class);
    }


    private void registerBuiltinIndexSettings() {
        registerIndexDynamicSetting(IndexStore.INDEX_STORE_THROTTLE_MAX_BYTES_PER_SEC, Validator.BYTES_SIZE);
        registerIndexDynamicSetting(IndexStore.INDEX_STORE_THROTTLE_TYPE, Validator.EMPTY);
        registerIndexDynamicSetting(MergeSchedulerConfig.MAX_THREAD_COUNT, Validator.NON_NEGATIVE_INTEGER);
        registerIndexDynamicSetting(MergeSchedulerConfig.MAX_MERGE_COUNT, Validator.EMPTY);
        registerIndexDynamicSetting(MergeSchedulerConfig.AUTO_THROTTLE, Validator.EMPTY);
        registerIndexDynamicSetting(FilterAllocationDecider.INDEX_ROUTING_REQUIRE_GROUP + "*", Validator.EMPTY);
        registerIndexDynamicSetting(FilterAllocationDecider.INDEX_ROUTING_INCLUDE_GROUP + "*", Validator.EMPTY);
        registerIndexDynamicSetting(FilterAllocationDecider.INDEX_ROUTING_EXCLUDE_GROUP + "*", Validator.EMPTY);
        registerIndexDynamicSetting(EnableAllocationDecider.INDEX_ROUTING_ALLOCATION_ENABLE, Validator.EMPTY);
        registerIndexDynamicSetting(EnableAllocationDecider.INDEX_ROUTING_REBALANCE_ENABLE, Validator.EMPTY);
        registerIndexDynamicSetting(TranslogConfig.INDEX_TRANSLOG_FS_TYPE, Validator.EMPTY);
        registerIndexDynamicSetting(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, Validator.NON_NEGATIVE_INTEGER);
        registerIndexDynamicSetting(IndexMetaData.SETTING_AUTO_EXPAND_REPLICAS, Validator.EMPTY);
        registerIndexDynamicSetting(IndexMetaData.SETTING_READ_ONLY, Validator.EMPTY);
        registerIndexDynamicSetting(IndexMetaData.SETTING_BLOCKS_READ, Validator.EMPTY);
        registerIndexDynamicSetting(IndexMetaData.SETTING_BLOCKS_WRITE, Validator.EMPTY);
        registerIndexDynamicSetting(IndexMetaData.SETTING_BLOCKS_METADATA, Validator.EMPTY);
        registerIndexDynamicSetting(IndexMetaData.SETTING_SHARED_FS_ALLOW_RECOVERY_ON_ANY_NODE, Validator.EMPTY);
        registerIndexDynamicSetting(IndexMetaData.SETTING_PRIORITY, Validator.NON_NEGATIVE_INTEGER);
        registerIndexDynamicSetting(IndicesTTLService.INDEX_TTL_DISABLE_PURGE, Validator.EMPTY);
        registerIndexDynamicSetting(IndexShard.INDEX_REFRESH_INTERVAL, Validator.TIME);
        registerIndexDynamicSetting(PrimaryShardAllocator.INDEX_RECOVERY_INITIAL_SHARDS, Validator.EMPTY);
        registerIndexDynamicSetting(EngineConfig.INDEX_COMPOUND_ON_FLUSH, Validator.BOOLEAN);
        registerIndexDynamicSetting(EngineConfig.INDEX_GC_DELETES_SETTING, Validator.TIME);
        registerIndexDynamicSetting(IndexShard.INDEX_FLUSH_ON_CLOSE, Validator.BOOLEAN);
        registerIndexDynamicSetting(EngineConfig.INDEX_VERSION_MAP_SIZE, Validator.BYTES_SIZE_OR_PERCENTAGE);
        registerIndexDynamicSetting(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_WARN, Validator.TIME);
        registerIndexDynamicSetting(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_INFO, Validator.TIME);
        registerIndexDynamicSetting(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_DEBUG, Validator.TIME);
        registerIndexDynamicSetting(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_TRACE, Validator.TIME);
        registerIndexDynamicSetting(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_REFORMAT, Validator.EMPTY);
        registerIndexDynamicSetting(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_LEVEL, Validator.EMPTY);
        registerIndexDynamicSetting(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_MAX_SOURCE_CHARS_TO_LOG, Validator.EMPTY);
        registerIndexDynamicSetting(SearchSlowLog.INDEX_SEARCH_SLOWLOG_THRESHOLD_QUERY_WARN, Validator.TIME);
        registerIndexDynamicSetting(SearchSlowLog.INDEX_SEARCH_SLOWLOG_THRESHOLD_QUERY_INFO, Validator.TIME);
        registerIndexDynamicSetting(SearchSlowLog.INDEX_SEARCH_SLOWLOG_THRESHOLD_QUERY_DEBUG, Validator.TIME);
        registerIndexDynamicSetting(SearchSlowLog.INDEX_SEARCH_SLOWLOG_THRESHOLD_QUERY_TRACE, Validator.TIME);
        registerIndexDynamicSetting(SearchSlowLog.INDEX_SEARCH_SLOWLOG_THRESHOLD_FETCH_WARN, Validator.TIME);
        registerIndexDynamicSetting(SearchSlowLog.INDEX_SEARCH_SLOWLOG_THRESHOLD_FETCH_INFO, Validator.TIME);
        registerIndexDynamicSetting(SearchSlowLog.INDEX_SEARCH_SLOWLOG_THRESHOLD_FETCH_DEBUG, Validator.TIME);
        registerIndexDynamicSetting(SearchSlowLog.INDEX_SEARCH_SLOWLOG_THRESHOLD_FETCH_TRACE, Validator.TIME);
        registerIndexDynamicSetting(SearchSlowLog.INDEX_SEARCH_SLOWLOG_REFORMAT, Validator.EMPTY);
        registerIndexDynamicSetting(SearchSlowLog.INDEX_SEARCH_SLOWLOG_LEVEL, Validator.EMPTY);
        registerIndexDynamicSetting(ShardsLimitAllocationDecider.INDEX_TOTAL_SHARDS_PER_NODE, Validator.INTEGER);
        registerIndexDynamicSetting(MergePolicyConfig.INDEX_MERGE_POLICY_EXPUNGE_DELETES_ALLOWED, Validator.DOUBLE);
        registerIndexDynamicSetting(MergePolicyConfig.INDEX_MERGE_POLICY_FLOOR_SEGMENT, Validator.BYTES_SIZE);
        registerIndexDynamicSetting(MergePolicyConfig.INDEX_MERGE_POLICY_MAX_MERGE_AT_ONCE, Validator.INTEGER_GTE_2);
        registerIndexDynamicSetting(MergePolicyConfig.INDEX_MERGE_POLICY_MAX_MERGE_AT_ONCE_EXPLICIT, Validator.INTEGER_GTE_2);
        registerIndexDynamicSetting(MergePolicyConfig.INDEX_MERGE_POLICY_MAX_MERGED_SEGMENT, Validator.BYTES_SIZE);
        registerIndexDynamicSetting(MergePolicyConfig.INDEX_MERGE_POLICY_SEGMENTS_PER_TIER, Validator.DOUBLE_GTE_2);
        registerIndexDynamicSetting(MergePolicyConfig.INDEX_MERGE_POLICY_RECLAIM_DELETES_WEIGHT, Validator.NON_NEGATIVE_DOUBLE);
        registerIndexDynamicSetting(MergePolicyConfig.INDEX_COMPOUND_FORMAT, Validator.EMPTY);
        registerIndexDynamicSetting(IndexShard.INDEX_TRANSLOG_FLUSH_THRESHOLD_OPS, Validator.INTEGER);
        registerIndexDynamicSetting(IndexShard.INDEX_TRANSLOG_FLUSH_THRESHOLD_SIZE, Validator.BYTES_SIZE);
        registerIndexDynamicSetting(IndexShard.INDEX_TRANSLOG_DISABLE_FLUSH, Validator.EMPTY);
        registerIndexDynamicSetting(TranslogConfig.INDEX_TRANSLOG_DURABILITY, Validator.EMPTY);
        registerIndexDynamicSetting(IndicesWarmer.INDEX_WARMER_ENABLED, Validator.EMPTY);
        registerIndexDynamicSetting(IndicesRequestCache.INDEX_CACHE_REQUEST_ENABLED, Validator.BOOLEAN);
        registerIndexDynamicSetting(IndicesRequestCache.DEPRECATED_INDEX_CACHE_REQUEST_ENABLED, Validator.BOOLEAN);
        registerIndexDynamicSetting(UnassignedInfo.INDEX_DELAYED_NODE_LEFT_TIMEOUT_SETTING, Validator.TIME);
        registerIndexDynamicSetting(DefaultSearchContext.MAX_RESULT_WINDOW, Validator.POSITIVE_INTEGER);
    }

    public void registerIndexDynamicSetting(String setting, Validator validator) {
        indexDynamicSettings.addSetting(setting, validator);
    }


    public void registerAllocationDecider(Class<? extends AllocationDecider> allocationDecider) {
        allocationDeciders.registerExtension(allocationDecider);
    }

    public void registerShardsAllocator(String name, Class<? extends ShardsAllocator> clazz) {
        shardsAllocators.registerExtension(name, clazz);
    }

    public void registerIndexTemplateFilter(Class<? extends IndexTemplateFilter> indexTemplateFilter) {
        indexTemplateFilters.registerExtension(indexTemplateFilter);
    }

    @Override
    protected void configure() {
        bind(DynamicSettings.class).annotatedWith(IndexDynamicSettings.class).toInstance(indexDynamicSettings.build());

        // bind ShardsAllocator
        String shardsAllocatorType = shardsAllocators.bindType(binder(), settings, ClusterModule.SHARDS_ALLOCATOR_TYPE_KEY, ClusterModule.BALANCED_ALLOCATOR);
        if (shardsAllocatorType.equals(ClusterModule.EVEN_SHARD_COUNT_ALLOCATOR)) {
            final ESLogger logger = Loggers.getLogger(getClass(), settings);
            logger.warn("{} allocator has been removed in 2.0 using {} instead", ClusterModule.EVEN_SHARD_COUNT_ALLOCATOR, ClusterModule.BALANCED_ALLOCATOR);
        }
        allocationDeciders.bind(binder());
        indexTemplateFilters.bind(binder());

        bind(ClusterInfoService.class).to(clusterInfoServiceImpl).asEagerSingleton();
        bind(GatewayAllocator.class).asEagerSingleton();
        bind(AllocationService.class).asEagerSingleton();
        bind(DiscoveryNodeService.class).asEagerSingleton();
        bind(ClusterService.class).to(InternalClusterService.class).asEagerSingleton();
        bind(OperationRouting.class).asEagerSingleton();
        bind(MetaDataCreateIndexService.class).asEagerSingleton();
        bind(MetaDataDeleteIndexService.class).asEagerSingleton();
        bind(MetaDataIndexStateService.class).asEagerSingleton();
        bind(MetaDataMappingService.class).asEagerSingleton();
        bind(MetaDataIndexAliasesService.class).asEagerSingleton();
        bind(MetaDataUpdateSettingsService.class).asEagerSingleton();
        bind(MetaDataIndexTemplateService.class).asEagerSingleton();
        bind(IndexNameExpressionResolver.class).asEagerSingleton();
        bind(RoutingService.class).asEagerSingleton();
        bind(ShardStateAction.class).asEagerSingleton();
        bind(NodeIndexDeletedAction.class).asEagerSingleton();
        bind(NodeMappingRefreshAction.class).asEagerSingleton();
        bind(MappingUpdatedAction.class).asEagerSingleton();
    }
}
