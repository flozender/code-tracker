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

package org.elasticsearch.cluster.routing;

import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.index.Index;
import org.elasticsearch.test.ESTestCase;

/**
 * A helper that allows to create shard routing instances within tests, while not requiring to expose
 * different simplified constructors on the ShardRouting itself.
 */
public class TestShardRouting {

    public static ShardRouting newShardRouting(String index, int shardId, String currentNodeId, long primaryTerm, boolean primary, ShardRoutingState state) {
        return newShardRouting(new Index(index, IndexMetaData.INDEX_UUID_NA_VALUE), shardId, currentNodeId, primaryTerm,primary, state);
    }

    public static ShardRouting newShardRouting(Index index, int shardId, String currentNodeId, long primaryTerm, boolean primary, ShardRoutingState state) {
        return new ShardRouting(index, shardId, currentNodeId, null, null, primaryTerm, primary, state, buildUnassignedInfo(state), buildAllocationId(state), true, -1);
    }

    public static ShardRouting newShardRouting(String index, int shardId, String currentNodeId, String relocatingNodeId, long primaryTerm, boolean primary, ShardRoutingState state) {
        return newShardRouting(new Index(index, IndexMetaData.INDEX_UUID_NA_VALUE), shardId, currentNodeId, relocatingNodeId, primaryTerm, primary, state);
    }

    public static ShardRouting newShardRouting(Index index, int shardId, String currentNodeId, String relocatingNodeId, long primaryTerm, boolean primary, ShardRoutingState state) {
        return new ShardRouting(index, shardId, currentNodeId, relocatingNodeId, null, primaryTerm, primary, state, buildUnassignedInfo(state), buildAllocationId(state), true, -1);
    }

    public static ShardRouting newShardRouting(String index, int shardId, String currentNodeId, String relocatingNodeId, long primaryTerm, boolean primary, ShardRoutingState state, AllocationId allocationId) {
        return newShardRouting(new Index(index, IndexMetaData.INDEX_UUID_NA_VALUE), shardId, currentNodeId, relocatingNodeId, primaryTerm, primary, state, allocationId);
    }

    public static ShardRouting newShardRouting(Index index, int shardId, String currentNodeId, String relocatingNodeId, long primaryTerm, boolean primary, ShardRoutingState state, AllocationId allocationId) {
        return new ShardRouting(index, shardId, currentNodeId, relocatingNodeId, null, primaryTerm, primary, state, buildUnassignedInfo(state), allocationId, true, -1);
    }

    public static ShardRouting newShardRouting(String index, int shardId, String currentNodeId, String relocatingNodeId, RestoreSource restoreSource, long primaryTerm, boolean primary, ShardRoutingState state) {
        return newShardRouting(new Index(index, IndexMetaData.INDEX_UUID_NA_VALUE), shardId, currentNodeId, relocatingNodeId, restoreSource, primaryTerm, primary, state);
    }

    public static ShardRouting newShardRouting(Index index, int shardId, String currentNodeId, String relocatingNodeId, RestoreSource restoreSource, long primaryTerm, boolean primary, ShardRoutingState state) {
        return new ShardRouting(index, shardId, currentNodeId, relocatingNodeId, restoreSource, primaryTerm, primary, state, buildUnassignedInfo(state), buildAllocationId(state), true, -1);
    }

    public static ShardRouting newShardRouting(String index, int shardId, String currentNodeId,
                                               String relocatingNodeId, RestoreSource restoreSource, long primaryTerm, boolean primary,
                                               ShardRoutingState state, UnassignedInfo unassignedInfo) {
        return newShardRouting(new Index(index, IndexMetaData.INDEX_UUID_NA_VALUE), shardId, currentNodeId, relocatingNodeId, restoreSource, primaryTerm, primary, state, unassignedInfo);
    }

    public static ShardRouting newShardRouting(Index index, int shardId, String currentNodeId,
                                               String relocatingNodeId, RestoreSource restoreSource, long primaryTerm, boolean primary,
                                               ShardRoutingState state, UnassignedInfo unassignedInfo) {
        return new ShardRouting(index, shardId, currentNodeId, relocatingNodeId, restoreSource, primaryTerm, primary, state, unassignedInfo, buildAllocationId(state), true, -1);
    }

    public static void relocate(ShardRouting shardRouting, String relocatingNodeId, long expectedShardSize) {
        shardRouting.relocate(relocatingNodeId, expectedShardSize);
    }

    private static AllocationId buildAllocationId(ShardRoutingState state) {
        switch (state) {
            case UNASSIGNED:
                return null;
            case INITIALIZING:
            case STARTED:
                return AllocationId.newInitializing();
            case RELOCATING:
                AllocationId allocationId = AllocationId.newInitializing();
                return AllocationId.newRelocation(allocationId);
            default:
                throw new IllegalStateException("illegal state");
        }
    }

    private static UnassignedInfo buildUnassignedInfo(ShardRoutingState state) {
        switch (state) {
            case UNASSIGNED:
            case INITIALIZING:
                return new UnassignedInfo(ESTestCase.randomFrom(UnassignedInfo.Reason.values()), "auto generated for test");
            case STARTED:
            case RELOCATING:
                return null;
            default:
                throw new IllegalStateException("illegal state");
        }
    }
}
