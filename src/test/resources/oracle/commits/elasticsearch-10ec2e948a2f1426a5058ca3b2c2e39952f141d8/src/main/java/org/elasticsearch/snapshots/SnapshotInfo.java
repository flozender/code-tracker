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
package org.elasticsearch.snapshots;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.joda.FormatDateTimeFormatter;
import org.elasticsearch.common.joda.Joda;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;

/**
 * Information about snapshot
 */
public class SnapshotInfo implements ToXContent, Streamable {

    private static final FormatDateTimeFormatter DATE_TIME_FORMATTER = Joda.forPattern("dateOptionalTime");

    private String name;

    private SnapshotState state;

    private ImmutableList<String> indices;

    private long startTime;

    private long endTime;

    private int totalShards;

    private int successfulShards;

    private ImmutableList<SnapshotShardFailure> shardFailures;

    SnapshotInfo() {

    }

    /**
     * Creates a new snapshot information from a {@link Snapshot}
     *
     * @param snapshot snapshot information returned by repository
     */
    public SnapshotInfo(Snapshot snapshot) {
        name = snapshot.name();
        state = snapshot.state();
        indices = snapshot.indices();
        startTime = snapshot.startTime();
        endTime = snapshot.endTime();
        totalShards = snapshot.totalShard();
        successfulShards = snapshot.successfulShards();
        shardFailures = snapshot.shardFailures();
    }

    /**
     * Returns snapshot name
     *
     * @return snapshot name
     */
    public String name() {
        return name;
    }

    /**
     * Returns snapshot state
     *
     * @return snapshot state
     */
    public SnapshotState state() {
        return state;
    }

    /**
     * Returns indices that were included into this snapshot
     *
     * @return list of indices
     */
    public ImmutableList<String> indices() {
        return indices;
    }

    /**
     * Returns time when snapshot started
     *
     * @return snapshot start time
     */
    public long startTime() {
        return startTime;
    }

    /**
     * Returns time when snapshot ended
     * <p/>
     * Can be 0L if snapshot is still running
     *
     * @return snapshot end time
     */
    public long endTime() {
        return endTime;
    }

    /**
     * Returns total number of shards that were snapshotted
     *
     * @return number of shards
     */
    public int totalShards() {
        return totalShards;
    }

    /**
     * Number of failed shards
     *
     * @return number of failed shards
     */
    public int failedShards() {
        return totalShards -  successfulShards;
    }

    /**
     * Returns total number of shards that were successfully snapshotted
     *
     * @return number of successful shards
     */
    public int successfulShards() {
        return successfulShards;
    }

    /**
     * Returns shard failures
     *
     * @return shard failures
     */
    public ImmutableList<SnapshotShardFailure> shardFailures() {
        return shardFailures;
    }

    /**
     * Returns snapshot REST status
     */
    public RestStatus status() {
        if (shardFailures.size() == 0) {
            return RestStatus.OK;
        }
        RestStatus status = RestStatus.OK;
        if (successfulShards == 0 && totalShards > 0) {
            for (SnapshotShardFailure shardFailure : shardFailures)
                if (shardFailure.status().getStatus() > status().getStatus()) {
                    status = shardFailure.status();
                }
            return status;
        }
        return status;
    }

    static final class Fields {
        static final XContentBuilderString INDICES = new XContentBuilderString("indices");
        static final XContentBuilderString STATE = new XContentBuilderString("state");
        static final XContentBuilderString START_TIME = new XContentBuilderString("start_time");
        static final XContentBuilderString START_TIME_IN_MILLIS = new XContentBuilderString("start_time_in_millis");
        static final XContentBuilderString END_TIME = new XContentBuilderString("end_time");
        static final XContentBuilderString END_TIME_IN_MILLIS = new XContentBuilderString("end_time_in_millis");
        static final XContentBuilderString DURATION = new XContentBuilderString("duration");
        static final XContentBuilderString DURATION_IN_MILLIS = new XContentBuilderString("duration_in_millis");
        static final XContentBuilderString FAILURES = new XContentBuilderString("failures");
        static final XContentBuilderString SHARDS = new XContentBuilderString("shards");
        static final XContentBuilderString TOTAL = new XContentBuilderString("total");
        static final XContentBuilderString FAILED = new XContentBuilderString("failed");
        static final XContentBuilderString SUCCESSFUL = new XContentBuilderString("successful");
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("snapshot", name);
        builder.startArray(Fields.INDICES);
        for (String index : indices) {
            builder.value(index);
        }
        builder.endArray();
        builder.field(Fields.STATE, state);
        if (startTime != 0) {
            builder.field(Fields.START_TIME, DATE_TIME_FORMATTER.printer().print(startTime));
            builder.field(Fields.START_TIME_IN_MILLIS, startTime);
        }
        if (endTime != 0) {
            builder.field(Fields.END_TIME, DATE_TIME_FORMATTER.printer().print(endTime));
            builder.field(Fields.END_TIME_IN_MILLIS, endTime);
            builder.field(Fields.DURATION, endTime - startTime);
            builder.field(Fields.DURATION_IN_MILLIS, TimeValue.timeValueMillis(endTime - startTime));
        }
        builder.startArray(Fields.FAILURES);
        for (SnapshotShardFailure shardFailure : shardFailures) {
            SnapshotShardFailure.toXContent(shardFailure, builder, params);
        }
        builder.endArray();
        builder.startObject(Fields.SHARDS);
        builder.field(Fields.TOTAL, totalShards);
        builder.field(Fields.FAILED, failedShards());
        builder.field(Fields.SUCCESSFUL, successfulShards);
        builder.endObject();
        builder.endObject();
        return builder;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        name = in.readString();
        int size = in.readVInt();
        ImmutableList.Builder<String> indicesListBuilder = ImmutableList.builder();
        for (int i = 0; i < size; i++) {
            indicesListBuilder.add(in.readString());
        }
        indices = indicesListBuilder.build();
        state = SnapshotState.fromValue(in.readByte());
        startTime = in.readVLong();
        endTime = in.readVLong();
        totalShards = in.readVInt();
        successfulShards = in.readVInt();
        size = in.readVInt();
        if (size > 0) {
            ImmutableList.Builder<SnapshotShardFailure> failureBuilder = ImmutableList.builder();
            for (int i = 0; i < size; i++) {
                failureBuilder.add(SnapshotShardFailure.readSnapshotShardFailure(in));
            }
            shardFailures = failureBuilder.build();
        } else {
            shardFailures = ImmutableList.of();
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(name);
        out.writeVInt(indices.size());
        for (String index : indices) {
            out.writeString(index);
        }
        out.writeByte(state.value());
        out.writeVLong(startTime);
        out.writeVLong(endTime);
        out.writeVInt(totalShards);
        out.writeVInt(successfulShards);
        out.writeVInt(shardFailures.size());
        for (SnapshotShardFailure failure : shardFailures) {
            failure.writeTo(out);
        }
    }

    /**
     * Reads snapshot information from stream input
     *
     * @param in stream input
     * @return deserialized snapshot info
     * @throws IOException
     */
    public static SnapshotInfo readSnapshotInfo(StreamInput in) throws IOException {
        SnapshotInfo snapshotInfo = new SnapshotInfo();
        snapshotInfo.readFrom(in);
        return snapshotInfo;
    }

    /**
     * Reads optional snapshot information from stream input
     *
     * @param in stream input
     * @return deserialized snapshot info or null
     * @throws IOException
     */
    public static SnapshotInfo readOptionalSnapshotInfo(StreamInput in) throws IOException {
        return in.readOptionalStreamable(new SnapshotInfo());
    }

}