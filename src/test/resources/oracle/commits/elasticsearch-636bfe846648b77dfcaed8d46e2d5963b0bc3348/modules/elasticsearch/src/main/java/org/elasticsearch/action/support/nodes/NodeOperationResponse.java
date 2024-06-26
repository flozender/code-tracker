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

package org.elasticsearch.action.support.nodes;

import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.util.io.stream.StreamInput;
import org.elasticsearch.util.io.stream.StreamOutput;
import org.elasticsearch.util.io.stream.Streamable;

import java.io.IOException;

/**
 * A base class for node level operations.
 *
 * @author kimchy (shay.banon)
 */
public abstract class NodeOperationResponse implements Streamable {

    private DiscoveryNode node;

    protected NodeOperationResponse() {
    }

    protected NodeOperationResponse(DiscoveryNode node) {
        this.node = node;
    }

    /**
     * The node this information relates to.
     */
    public DiscoveryNode node() {
        return node;
    }

    /**
     * The node this information relates to.
     */
    public DiscoveryNode getNode() {
        return node();
    }

    @Override public void readFrom(StreamInput in) throws IOException {
        node = DiscoveryNode.readNode(in);
    }

    @Override public void writeTo(StreamOutput out) throws IOException {
        node.writeTo(out);
    }
}
