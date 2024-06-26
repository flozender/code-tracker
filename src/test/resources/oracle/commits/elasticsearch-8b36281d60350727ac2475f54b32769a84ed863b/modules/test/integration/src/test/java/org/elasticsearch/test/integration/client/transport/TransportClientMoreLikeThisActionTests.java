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

package org.elasticsearch.test.integration.client.transport;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.server.internal.InternalServer;
import org.elasticsearch.test.integration.document.MoreLikeThisActionTests;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.util.transport.TransportAddress;

import static org.elasticsearch.util.settings.ImmutableSettings.*;

/**
 * @author kimchy (shay.banon)
 */
public class TransportClientMoreLikeThisActionTests extends MoreLikeThisActionTests {

    @Override protected Client getClient1() {
        TransportAddress server1Address = ((InternalServer) server("server1")).injector().getInstance(TransportService.class).boundAddress().publishAddress();
        TransportClient client = new TransportClient(settingsBuilder().putBoolean("discovery.enabled", false).build());
        client.addTransportAddress(server1Address);
        return client;
    }

    @Override protected Client getClient2() {
        TransportAddress server1Address = ((InternalServer) server("server2")).injector().getInstance(TransportService.class).boundAddress().publishAddress();
        TransportClient client = new TransportClient(settingsBuilder().putBoolean("discovery.enabled", false).build());
        client.addTransportAddress(server1Address);
        return client;
    }
}
