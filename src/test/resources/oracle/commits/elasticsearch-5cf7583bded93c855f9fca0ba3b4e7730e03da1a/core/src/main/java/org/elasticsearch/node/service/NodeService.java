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

package org.elasticsearch.node.service;

import java.io.Closeable;
import java.io.IOException;

import org.elasticsearch.Build;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.indices.stats.CommonStatsFlags;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.discovery.Discovery;
import org.elasticsearch.http.HttpServer;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.indices.breaker.CircuitBreakerService;
import org.elasticsearch.ingest.IngestService;
import org.elasticsearch.ingest.ProcessorsRegistry;
import org.elasticsearch.monitor.MonitorService;
import org.elasticsearch.plugins.PluginsService;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

/**
 */
public class NodeService extends AbstractComponent implements Closeable {

    private final ThreadPool threadPool;
    private final MonitorService monitorService;
    private final TransportService transportService;
    private final IndicesService indicesService;
    private final PluginsService pluginService;
    private final CircuitBreakerService circuitBreakerService;
    private final IngestService ingestService;
    private final SettingsFilter settingsFilter;
    private ClusterService clusterService;
    private ScriptService scriptService;

    @Nullable
    private final HttpServer httpServer;

    private final Discovery discovery;

    @Inject
    public NodeService(Settings settings, ThreadPool threadPool, MonitorService monitorService, Discovery discovery,
                       TransportService transportService, IndicesService indicesService, PluginsService pluginService,
                       CircuitBreakerService circuitBreakerService, @Nullable HttpServer httpServer,
                       ProcessorsRegistry.Builder processorsRegistryBuilder, ClusterService clusterService,
                       SettingsFilter settingsFilter) {
        super(settings);
        this.threadPool = threadPool;
        this.monitorService = monitorService;
        this.transportService = transportService;
        this.indicesService = indicesService;
        this.discovery = discovery;
        this.pluginService = pluginService;
        this.circuitBreakerService = circuitBreakerService;
        this.httpServer = httpServer;
        this.clusterService = clusterService;
        this.ingestService = new IngestService(settings, threadPool, processorsRegistryBuilder);
        this.settingsFilter = settingsFilter;
        clusterService.add(ingestService.getPipelineStore());
        clusterService.add(ingestService.getPipelineExecutionService());
    }

    // can not use constructor injection or there will be a circular dependency
    @Inject(optional = true)
    public void setScriptService(ScriptService scriptService) {
        this.scriptService = scriptService;
        this.ingestService.buildProcessorsFactoryRegistry(scriptService, clusterService);
    }

    public NodeInfo info(boolean settings, boolean os, boolean process, boolean jvm, boolean threadPool,
                boolean transport, boolean http, boolean plugin, boolean ingest, boolean indices) {
        return new NodeInfo(Version.CURRENT, Build.CURRENT, discovery.localNode(),
                settings ? settingsFilter.filter(this.settings) : null,
                os ? monitorService.osService().info() : null,
                process ? monitorService.processService().info() : null,
                jvm ? monitorService.jvmService().info() : null,
                threadPool ? this.threadPool.info() : null,
                transport ? transportService.info() : null,
                http ? (httpServer == null ? null : httpServer.info()) : null,
                plugin ? (pluginService == null ? null : pluginService.info()) : null,
                ingest ? (ingestService == null ? null : ingestService.info()) : null,
                indices ? indicesService.getTotalIndexingBufferBytes() : null
        );
    }

    public NodeStats stats(CommonStatsFlags indices, boolean os, boolean process, boolean jvm, boolean threadPool,
                           boolean fs, boolean transport, boolean http, boolean circuitBreaker,
                           boolean script, boolean discoveryStats, boolean ingest) {
        // for indices stats we want to include previous allocated shards stats as well (it will
        // only be applied to the sensible ones to use, like refresh/merge/flush/indexing stats)
        return new NodeStats(discovery.localNode(), System.currentTimeMillis(),
                indices.anySet() ? indicesService.stats(true, indices) : null,
                os ? monitorService.osService().stats() : null,
                process ? monitorService.processService().stats() : null,
                jvm ? monitorService.jvmService().stats() : null,
                threadPool ? this.threadPool.stats() : null,
                fs ? monitorService.fsService().stats() : null,
                transport ? transportService.stats() : null,
                http ? httpServer.stats() : null,
                circuitBreaker ? circuitBreakerService.stats() : null,
                script ? scriptService.stats() : null,
                discoveryStats ? discovery.stats() : null,
                ingest ? ingestService.getPipelineExecutionService().stats() : null
        );
    }

    public IngestService getIngestService() {
        return ingestService;
    }

    @Override
    public void close() throws IOException {
        indicesService.close();
    }
}
