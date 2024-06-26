package org.elasticsearch.action.admin.cluster.stats;
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


import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectIntCursor;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.PluginInfo;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.monitor.fs.FsStats;
import org.elasticsearch.monitor.jvm.JvmInfo;
import org.elasticsearch.monitor.os.OsInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class ClusterStatsNodes implements ToXContent, Streamable {

    private Counts counts;
    private Set<Version> versions;
    private OsStats os;
    private ProcessStats process;
    private JvmStats jvm;
    private FsStats.Info fs;
    private Set<PluginInfo> plugins;

    private ClusterStatsNodes() {
    }

    public ClusterStatsNodes(ClusterStatsNodeResponse[] nodeResponses) {
        this.counts = new Counts();
        this.versions = new HashSet<Version>();
        this.os = new OsStats();
        this.jvm = new JvmStats();
        this.fs = new FsStats.Info();
        this.plugins = new HashSet<PluginInfo>();
        this.process = new ProcessStats();

        Set<InetAddress> seenAddresses = new HashSet<InetAddress>(nodeResponses.length);

        for (ClusterStatsNodeResponse nodeResponse : nodeResponses) {

            counts.addNodeInfo(nodeResponse.nodeInfo());
            versions.add(nodeResponse.nodeInfo().getVersion());
            process.addNodeStats(nodeResponse.nodeStats());
            jvm.addNodeInfoStats(nodeResponse.nodeInfo(), nodeResponse.nodeStats());
            plugins.addAll(nodeResponse.nodeInfo().getPlugins().getInfos());

            // now do the stats that should be deduped by hardware (implemented by ip deduping)
            TransportAddress publishAddress = nodeResponse.nodeInfo().getTransport().address().publishAddress();
            InetAddress inetAddress = null;
            if (publishAddress.uniqueAddressTypeId() == 1) {
                inetAddress = ((InetSocketTransportAddress) publishAddress).address().getAddress();
            }

            if (!seenAddresses.add(inetAddress)) {
                continue;
            }

            os.addNodeInfo(nodeResponse.nodeInfo());
            if (nodeResponse.nodeStats().getFs() != null) {
                fs.add(nodeResponse.nodeStats().getFs().total());
            }
        }
    }


    public Counts getCounts() {
        return this.counts;
    }

    public Set<Version> getVersions() {
        return versions;
    }

    public OsStats getOs() {
        return os;
    }

    public ProcessStats getProcess() {
        return process;
    }

    public JvmStats getJvm() {
        return jvm;
    }

    public FsStats.Info getFs() {
        return fs;
    }

    public Set<PluginInfo> getPlugins() {
        return plugins;
    }


    @Override
    public void readFrom(StreamInput in) throws IOException {
        counts = Counts.readCounts(in);

        int size = in.readVInt();
        versions = new HashSet<Version>(size);
        for (; size > 0; size--) {
            versions.add(Version.readVersion(in));
        }

        os = OsStats.readOsStats(in);
        process = ProcessStats.readStats(in);
        jvm = JvmStats.readJvmStats(in);
        fs = FsStats.Info.readInfoFrom(in);

        size = in.readVInt();
        plugins = new HashSet<PluginInfo>(size);
        for (; size > 0; size--) {
            plugins.add(PluginInfo.readPluginInfo(in));
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        counts.writeTo(out);
        out.writeVInt(versions.size());
        for (Version v : versions) Version.writeVersion(v, out);
        os.writeTo(out);
        process.writeTo(out);
        jvm.writeTo(out);
        fs.writeTo(out);
        out.writeVInt(plugins.size());
        for (PluginInfo p : plugins) {
            p.writeTo(out);
        }
    }

    public static ClusterStatsNodes readNodeStats(StreamInput in) throws IOException {
        ClusterStatsNodes nodeStats = new ClusterStatsNodes();
        nodeStats.readFrom(in);
        return nodeStats;
    }

    static final class Fields {
        static final XContentBuilderString COUNT = new XContentBuilderString("count");
        static final XContentBuilderString VERSIONS = new XContentBuilderString("versions");
        static final XContentBuilderString OS = new XContentBuilderString("os");
        static final XContentBuilderString PROCESS = new XContentBuilderString("process");
        static final XContentBuilderString JVM = new XContentBuilderString("jvm");
        static final XContentBuilderString FS = new XContentBuilderString("fs");
        static final XContentBuilderString PLUGINS = new XContentBuilderString("plugins");
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(Fields.COUNT);
        counts.toXContent(builder, params);
        builder.endObject();

        builder.startArray(Fields.VERSIONS);
        for (Version v : versions) {
            builder.value(v.toString());
        }
        builder.endArray();

        builder.startObject(Fields.OS);
        os.toXContent(builder, params);
        builder.endObject();

        builder.startObject(Fields.PROCESS);
        process.toXContent(builder, params);
        builder.endObject();

        builder.startObject(Fields.JVM);
        jvm.toXContent(builder, params);
        builder.endObject();

        builder.field(Fields.FS);
        fs.toXContent(builder, params);

        builder.startArray(Fields.PLUGINS);
        for (PluginInfo pluginInfo : plugins) {
            pluginInfo.toXContent(builder, params);
        }
        builder.endArray();
        return builder;
    }

    public static class Counts implements Streamable, ToXContent {
        int total;
        int masterOnly;
        int dataOnly;
        int masterData;
        int client;

        public void addNodeInfo(NodeInfo nodeInfo) {
            total++;
            DiscoveryNode node = nodeInfo.getNode();
            if (node.masterNode()) {
                if (node.dataNode()) {
                    masterData++;
                } else {
                    masterOnly++;
                }
            } else if (node.dataNode()) {
                dataOnly++;
            } else if (node.clientNode()) {
                client++;
            }
        }

        public int getTotal() {
            return total;
        }

        public int getMasterOnly() {
            return masterOnly;
        }

        public int getDataOnly() {
            return dataOnly;
        }

        public int getMasterData() {
            return masterData;
        }

        public int getClient() {
            return client;
        }

        public static Counts readCounts(StreamInput in) throws IOException {
            Counts c = new Counts();
            c.readFrom(in);
            return c;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            total = in.readVInt();
            masterOnly = in.readVInt();
            dataOnly = in.readVInt();
            masterData = in.readVInt();
            client = in.readVInt();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeVInt(total);
            out.writeVInt(masterOnly);
            out.writeVInt(dataOnly);
            out.writeVInt(masterData);
            out.writeVInt(client);
        }

        static final class Fields {
            static final XContentBuilderString TOTAL = new XContentBuilderString("total");
            static final XContentBuilderString MASTER_ONLY = new XContentBuilderString("master_only");
            static final XContentBuilderString DATA_ONLY = new XContentBuilderString("data_only");
            static final XContentBuilderString MASTER_DATA = new XContentBuilderString("master_data");
            static final XContentBuilderString CLIENT = new XContentBuilderString("client");
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.field(Fields.TOTAL, total);
            builder.field(Fields.MASTER_ONLY, masterOnly);
            builder.field(Fields.DATA_ONLY, dataOnly);
            builder.field(Fields.MASTER_DATA, masterData);
            builder.field(Fields.CLIENT, client);
            return builder;
        }
    }

    public static class OsStats implements ToXContent, Streamable {

        int availableProcessors;
        long availableMemory;
        ObjectIntOpenHashMap<OsInfo.Cpu> cpus;

        public OsStats() {
            cpus = new ObjectIntOpenHashMap<org.elasticsearch.monitor.os.OsInfo.Cpu>();
        }

        public void addNodeInfo(NodeInfo nodeInfo) {
            availableProcessors += nodeInfo.getOs().availableProcessors();
            if (nodeInfo.getOs() == null) {
                return;
            }
            if (nodeInfo.getOs().cpu() != null) {
                cpus.addTo(nodeInfo.getOs().cpu(), 1);
            }
            if (nodeInfo.getOs().getMem() != null && nodeInfo.getOs().getMem().getTotal().bytes() != -1) {
                availableMemory += nodeInfo.getOs().getMem().getTotal().bytes();
            }
        }

        public int getAvailableProcessors() {
            return availableProcessors;
        }

        public ByteSizeValue getAvailableMemory() {
            return new ByteSizeValue(availableMemory);
        }

        public ObjectIntOpenHashMap<OsInfo.Cpu> getCpus() {
            return cpus;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            availableProcessors = in.readVInt();
            availableMemory = in.readLong();
            int size = in.readVInt();
            cpus = new ObjectIntOpenHashMap<OsInfo.Cpu>(size);
            for (; size > 0; size--) {
                cpus.addTo(OsInfo.Cpu.readCpu(in), in.readVInt());
            }
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeVInt(availableProcessors);
            out.writeLong(availableMemory);
            out.writeVInt(cpus.size());
            for (ObjectIntCursor<OsInfo.Cpu> c : cpus) {
                c.key.writeTo(out);
                out.writeVInt(c.value);
            }

        }

        public static OsStats readOsStats(StreamInput in) throws IOException {
            OsStats os = new OsStats();
            os.readFrom(in);
            return os;
        }

        static final class Fields {
            static final XContentBuilderString AVAILABLE_PROCESSORS = new XContentBuilderString("available_processors");
            static final XContentBuilderString MEM = new XContentBuilderString("mem");
            static final XContentBuilderString TOTAL = new XContentBuilderString("total");
            static final XContentBuilderString TOTAL_IN_BYTES = new XContentBuilderString("total_in_bytes");
            static final XContentBuilderString CPU = new XContentBuilderString("cpu");
            static final XContentBuilderString COUNT = new XContentBuilderString("count");
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.field(Fields.AVAILABLE_PROCESSORS, availableProcessors);
            builder.startObject(Fields.MEM);
            builder.byteSizeField(Fields.TOTAL_IN_BYTES, Fields.TOTAL, availableMemory);
            builder.endObject();

            builder.startArray(Fields.CPU);
            for (ObjectIntCursor<OsInfo.Cpu> cpu : cpus) {
                builder.startObject();
                cpu.key.toXContent(builder, params);
                builder.field(Fields.COUNT, cpu.value);
                builder.endObject();
            }
            builder.endArray();

            return builder;
        }
    }

    public static class ProcessStats implements ToXContent, Streamable {

        int count;
        int cpuPercent;
        long totalOpenFileDescriptors;

        public void addNodeStats(NodeStats nodeStats) {
            if (nodeStats.getProcess() == null) {
                return;
            }
            count++;
            if (nodeStats.getProcess().cpu() != null) {
                // with no sigar, this may not be available
                cpuPercent += nodeStats.getProcess().cpu().getPercent();
            }
            totalOpenFileDescriptors += nodeStats.getProcess().openFileDescriptors();
        }

        /**
         * Cpu usage in percentages - 100 is 1 core.
         */
        public int getCpuPercent() {
            return cpuPercent;
        }

        public long getAvgOpenFileDescriptors() {
            if (count == 0) {
                return -1;
            }
            return totalOpenFileDescriptors / count;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            count = in.readVInt();
            cpuPercent = in.readVInt();
            totalOpenFileDescriptors = in.readVLong();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeVInt(count);
            out.writeVInt(cpuPercent);
            out.writeVLong(totalOpenFileDescriptors);
        }

        public static ProcessStats readStats(StreamInput in) throws IOException {
            ProcessStats cpu = new ProcessStats();
            cpu.readFrom(in);
            return cpu;
        }

        static final class Fields {
            static final XContentBuilderString CPU = new XContentBuilderString("cpu");
            static final XContentBuilderString PERCENT = new XContentBuilderString("percent");
            static final XContentBuilderString AVG_OPEN_FD = new XContentBuilderString("avg_open_file_descriptors");
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject(Fields.CPU).field(Fields.PERCENT, cpuPercent).endObject();
            builder.field(Fields.AVG_OPEN_FD, getAvgOpenFileDescriptors());
            return builder;
        }
    }

    public static class JvmStats implements Streamable, ToXContent {

        ObjectIntOpenHashMap<JvmVersion> versions;
        long threads;
        long maxUptime;
        long heapUsed;
        long heapMax;

        JvmStats() {
            versions = new ObjectIntOpenHashMap<JvmVersion>();
            threads = 0;
            maxUptime = 0;
            heapMax = 0;
            heapUsed = 0;
        }

        public ObjectIntOpenHashMap<JvmVersion> getVersions() {
            return versions;
        }

        /**
         * The total number of threads in the cluster
         */
        public long getThreads() {
            return threads;
        }

        /**
         * The maximum uptime of a node in the cluster
         */
        public TimeValue getMaxUpTime() {
            return new TimeValue(maxUptime);
        }

        /**
         * Total heap used in the cluster
         */
        public ByteSizeValue getHeapUsed() {
            return new ByteSizeValue(heapUsed);
        }

        /**
         * Maximum total heap available to the cluster
         */
        public ByteSizeValue getHeapMax() {
            return new ByteSizeValue(heapMax);
        }

        public void addNodeInfoStats(NodeInfo nodeInfo, NodeStats nodeStats) {
            versions.addTo(new JvmVersion(nodeInfo.getJvm()), 1);
            org.elasticsearch.monitor.jvm.JvmStats js = nodeStats.getJvm();
            if (js == null) {
                return;
            }
            if (js.threads() != null) {
                threads += js.threads().count();
            }
            maxUptime = Math.max(maxUptime, js.uptime().millis());
            if (js.mem() != null) {
                heapUsed += js.mem().getHeapUsed().bytes();
                heapMax += js.mem().getHeapMax().bytes();
            }
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            int size = in.readVInt();
            versions = new ObjectIntOpenHashMap<JvmVersion>(size);
            for (; size > 0; size--) {
                versions.addTo(JvmVersion.readJvmVersion(in), in.readVInt());
            }
            threads = in.readVLong();
            maxUptime = in.readVLong();
            heapUsed = in.readVLong();
            heapMax = in.readVLong();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeVInt(versions.size());
            for (ObjectIntCursor<JvmVersion> v : versions) {
                v.key.writeTo(out);
                out.writeVInt(v.value);
            }

            out.writeVLong(threads);
            out.writeVLong(maxUptime);
            out.writeVLong(heapUsed);
            out.writeVLong(heapMax);
        }

        public static JvmStats readJvmStats(StreamInput in) throws IOException {
            JvmStats jvmStats = new JvmStats();
            jvmStats.readFrom(in);
            return jvmStats;
        }

        static final class Fields {
            static final XContentBuilderString VERSIONS = new XContentBuilderString("versions");
            static final XContentBuilderString VERSION = new XContentBuilderString("version");
            static final XContentBuilderString VM_NAME = new XContentBuilderString("vm_name");
            static final XContentBuilderString VM_VERSION = new XContentBuilderString("vm_version");
            static final XContentBuilderString VM_VENDOR = new XContentBuilderString("vm_vendor");
            static final XContentBuilderString COUNT = new XContentBuilderString("count");
            static final XContentBuilderString THREADS = new XContentBuilderString("threads");
            static final XContentBuilderString MAX_UPTIME = new XContentBuilderString("max_uptime");
            static final XContentBuilderString MAX_UPTIME_IN_MILLIS = new XContentBuilderString("max_uptime_in_millis");
            static final XContentBuilderString MEM = new XContentBuilderString("mem");
            static final XContentBuilderString HEAP_USED = new XContentBuilderString("heap_used");
            static final XContentBuilderString HEAP_USED_IN_BYTES = new XContentBuilderString("heap_used_in_bytes");
            static final XContentBuilderString HEAP_MAX = new XContentBuilderString("heap_max");
            static final XContentBuilderString HEAP_MAX_IN_BYTES = new XContentBuilderString("heap_max_in_bytes");
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.timeValueField(Fields.MAX_UPTIME_IN_MILLIS, Fields.MAX_UPTIME, maxUptime);
            builder.startArray(Fields.VERSIONS);
            for (ObjectIntCursor<JvmVersion> v : versions) {
                builder.startObject();
                builder.field(Fields.VERSION, v.key.version);
                builder.field(Fields.VM_NAME, v.key.vmName);
                builder.field(Fields.VM_VERSION, v.key.vmVersion);
                builder.field(Fields.VM_VENDOR, v.key.vmVendor);
                builder.field(Fields.COUNT, v.value);
                builder.endObject();
            }
            builder.endArray();
            builder.startObject(Fields.MEM);
            builder.byteSizeField(Fields.HEAP_USED_IN_BYTES, Fields.HEAP_USED, heapUsed);
            builder.byteSizeField(Fields.HEAP_MAX_IN_BYTES, Fields.HEAP_MAX, heapMax);
            builder.endObject();

            builder.field(Fields.THREADS, threads);
            return builder;
        }
    }

    public static class JvmVersion implements Streamable {
        String version;
        String vmName;
        String vmVersion;
        String vmVendor;

        JvmVersion(JvmInfo jvmInfo) {
            version = jvmInfo.version();
            vmName = jvmInfo.vmName();
            vmVersion = jvmInfo.vmVersion();
            vmVendor = jvmInfo.vmVendor();
        }

        JvmVersion() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            JvmVersion jvm = (JvmVersion) o;

            return vmVersion.equals(jvm.vmVersion) && vmVendor.equals(jvm.vmVendor);
        }

        @Override
        public int hashCode() {
            return vmVersion.hashCode();
        }

        public static JvmVersion readJvmVersion(StreamInput in) throws IOException {
            JvmVersion jvm = new JvmVersion();
            jvm.readFrom(in);
            return jvm;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            version = in.readString();
            vmName = in.readString();
            vmVersion = in.readString();
            vmVendor = in.readString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeString(version);
            out.writeString(vmName);
            out.writeString(vmVersion);
            out.writeString(vmVendor);
        }
    }


}
