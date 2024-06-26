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

package org.elasticsearch.memcached.netty;

import org.elasticsearch.memcached.MemcachedRestRequest;
import org.elasticsearch.rest.RestController;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * @author kimchy (shay.banon)
 */
public class MemcachedDispatcher extends SimpleChannelUpstreamHandler {

    private final RestController restController;

    public MemcachedDispatcher(RestController restController) {
        this.restController = restController;
    }

    @Override public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        MemcachedRestRequest request = (MemcachedRestRequest) e.getMessage();
        restController.dispatchRequest(request, new MemcachedRestChannel(ctx.getChannel(), request));
        super.messageReceived(ctx, e);
    }
}
