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

package org.elasticsearch.cluster.routing.operation;

import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.routing.GroupShardsIterator;
import org.elasticsearch.cluster.routing.ShardsIterator;
import org.elasticsearch.index.IndexShardMissingException;
import org.elasticsearch.indices.IndexMissingException;

import javax.annotation.Nullable;

/**
 * @author kimchy (shay.banon)
 */
public interface OperationRouting {

    ShardsIterator indexShards(ClusterState clusterState, String index, String type, String id, @Nullable String routing) throws IndexMissingException, IndexShardMissingException;

    ShardsIterator deleteShards(ClusterState clusterState, String index, String type, String id, @Nullable String routing) throws IndexMissingException, IndexShardMissingException;

    ShardsIterator getShards(ClusterState clusterState, String index, String type, String id, @Nullable String routing) throws IndexMissingException, IndexShardMissingException;

    GroupShardsIterator deleteByQueryShards(ClusterState clusterState, String index, @Nullable String routing) throws IndexMissingException;

    GroupShardsIterator searchShards(ClusterState clusterState, String[] indices, @Nullable String queryHint, @Nullable String routing) throws IndexMissingException;
}
