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

package org.elasticsearch.memcached;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.util.Classes;

import static org.elasticsearch.common.inject.ModulesFactory.*;

/**
 * @author kimchy (shay.banon)
 */
public class MemcachedServerModule extends AbstractModule {

    private final Settings settings;

    public MemcachedServerModule(Settings settings) {
        this.settings = settings;
    }

    @SuppressWarnings({"unchecked"}) @Override protected void configure() {
        bind(MemcachedServer.class).asEagerSingleton();

        Class<? extends Module> defaultMemcachedServerTransportModule = null;
        try {
            Classes.getDefaultClassLoader().loadClass("org.elasticsearch.memcached.netty.NettyMemcachedServerTransport");
            defaultMemcachedServerTransportModule = (Class<? extends Module>) Classes.getDefaultClassLoader().loadClass("org.elasticsearch.memcached.netty.NettyMemcachedServerTransportModule");
        } catch (ClassNotFoundException e) {
            // no netty one, ok...
            if (settings.get("memcached.type") == null) {
                // no explicit one is configured, bail
                return;
            }
        }

        Class<? extends Module> moduleClass = settings.getAsClass("memcached.type", defaultMemcachedServerTransportModule, "org.elasticsearch.memcached.", "MemcachedServerTransportModule");
        createModule(moduleClass, settings).configure(binder());
    }
}
