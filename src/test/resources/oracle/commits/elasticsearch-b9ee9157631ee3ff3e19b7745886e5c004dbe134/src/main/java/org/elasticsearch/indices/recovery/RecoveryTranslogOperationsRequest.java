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

package org.elasticsearch.indices.recovery;

import com.google.common.collect.Lists;
import org.elasticsearch.Version;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.translog.TranslogStreams;
import org.elasticsearch.index.translog.Translog;
import org.elasticsearch.transport.TransportRequest;

import java.io.IOException;
import java.util.List;

/**
 *
 */
class RecoveryTranslogOperationsRequest extends TransportRequest {

    private long recoveryId;
    private ShardId shardId;
    private List<Translog.Operation> operations;

    RecoveryTranslogOperationsRequest() {
    }

    RecoveryTranslogOperationsRequest(long recoveryId, ShardId shardId, List<Translog.Operation> operations) {
        this.recoveryId = recoveryId;
        this.shardId = shardId;
        this.operations = operations;
    }

    public long recoveryId() {
        return this.recoveryId;
    }

    public ShardId shardId() {
        return shardId;
    }

    public List<Translog.Operation> operations() {
        return operations;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        recoveryId = in.readLong();
        shardId = ShardId.readShardId(in);
        int size = in.readVInt();
        operations = Lists.newArrayListWithExpectedSize(size);
        for (int i = 0; i < size; i++) {
            if (in.getVersion().onOrAfter(Version.V_1_4_0_Beta)) {
                operations.add(TranslogStreams.CHECKSUMMED_TRANSLOG_STREAM.read(in));
            } else {
                operations.add(TranslogStreams.LEGACY_TRANSLOG_STREAM.read(in));
            }

        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeLong(recoveryId);
        shardId.writeTo(out);
        out.writeVInt(operations.size());
        for (Translog.Operation operation : operations) {
            if (out.getVersion().onOrAfter(Version.V_1_4_0_Beta)) {
                TranslogStreams.CHECKSUMMED_TRANSLOG_STREAM.write(out, operation);
            } else {
                TranslogStreams.LEGACY_TRANSLOG_STREAM.write(out, operation);
            }
        }
    }
}
