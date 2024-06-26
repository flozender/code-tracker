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

package org.elasticsearch.indices.recovery;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.index.shard.ShardId;

/**
 *
 */
public class RecoveryFailedException extends ElasticsearchException {

    public RecoveryFailedException(StartRecoveryRequest request, Throwable cause) {
        this(request.shardId(), request.sourceNode(), request.targetNode(), cause);
    }

    public RecoveryFailedException(ShardId shardId, DiscoveryNode sourceNode, DiscoveryNode targetNode, Throwable cause) {
        super(shardId + ": Recovery failed from " + sourceNode + " into " + targetNode, cause);
    }
}
