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
package org.elasticsearch.transport.netty;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.component.Lifecycle;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.common.util.concurrent.AbstractRunnable;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.elasticsearch.test.ESIntegTestCase.Scope;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.ActionNotFoundTransportException;
import org.elasticsearch.transport.RequestHandlerRegistry;
import org.elasticsearch.transport.TransportRequest;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;

import static org.elasticsearch.common.settings.Settings.settingsBuilder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 *
 */
@ClusterScope(scope = Scope.TEST, numDataNodes = 1)
public class NettyTransportIT extends ESIntegTestCase {
    // static so we can use it in anonymous classes
    private static String channelProfileName = null;

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return settingsBuilder().put(super.nodeSettings(nodeOrdinal))
                .put(Node.NODE_MODE_SETTING.getKey(), "network")
                .put(NetworkModule.TRANSPORT_TYPE_KEY, "exception-throwing").build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return pluginList(ExceptionThrowingNettyTransport.TestPlugin.class);
    }

    public void testThatConnectionFailsAsIntended() throws Exception {
        Client transportClient = internalCluster().transportClient();
        ClusterHealthResponse clusterIndexHealths = transportClient.admin().cluster().prepareHealth().get();
        assertThat(clusterIndexHealths.getStatus(), is(ClusterHealthStatus.GREEN));
        try {
            transportClient.filterWithHeader(Collections.singletonMap("ERROR", "MY MESSAGE")).admin().cluster().prepareHealth().get();
            fail("Expected exception, but didnt happen");
        } catch (ElasticsearchException e) {
            assertThat(e.getMessage(), containsString("MY MESSAGE"));
            assertThat(channelProfileName, is(NettyTransport.DEFAULT_PROFILE));
        }
    }

    public static final class ExceptionThrowingNettyTransport extends NettyTransport {

        public static class TestPlugin extends Plugin {
            @Override
            public String name() {
                return "exception-throwing-netty-transport";
            }
            @Override
            public String description() {
                return "an exception throwing transport for testing";
            }
            public void onModule(NetworkModule module) {
                module.registerTransport("exception-throwing", ExceptionThrowingNettyTransport.class);
            }
        }

        @Inject
        public ExceptionThrowingNettyTransport(Settings settings, ThreadPool threadPool, NetworkService networkService, BigArrays bigArrays, Version version, NamedWriteableRegistry namedWriteableRegistry) {
            super(settings, threadPool, networkService, bigArrays, version, namedWriteableRegistry);
        }

        @Override
        public ChannelPipelineFactory configureServerChannelPipelineFactory(String name, Settings groupSettings) {
            return new ErrorPipelineFactory(this, name, groupSettings);
        }

        private static class ErrorPipelineFactory extends ServerChannelPipelineFactory {

            private final ESLogger logger;

            public ErrorPipelineFactory(ExceptionThrowingNettyTransport exceptionThrowingNettyTransport, String name, Settings groupSettings) {
                super(exceptionThrowingNettyTransport, name, groupSettings);
                this.logger = exceptionThrowingNettyTransport.logger;
            }

            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = super.getPipeline();
                pipeline.replace("dispatcher", "dispatcher", new MessageChannelHandler(nettyTransport, logger, NettyTransport.DEFAULT_PROFILE) {

                    @Override
                    protected String handleRequest(Channel channel, StreamInput buffer, long requestId, Version version) throws IOException {
                        final String action = buffer.readString();

                        final NettyTransportChannel transportChannel = new NettyTransportChannel(transport, transportServiceAdapter, action, channel, requestId, version, name);
                        try {
                            final RequestHandlerRegistry reg = transportServiceAdapter.getRequestHandler(action);
                            if (reg == null) {
                                throw new ActionNotFoundTransportException(action);
                            }
                            final TransportRequest request = reg.newRequest();
                            request.remoteAddress(new InetSocketTransportAddress((InetSocketAddress) channel.getRemoteAddress()));
                            request.readFrom(buffer);
                            String error = threadPool.getThreadContext().getHeader("ERROR");
                            if (error != null) {
                                throw new ElasticsearchException(error);
                            }
                            if (reg.getExecutor() == ThreadPool.Names.SAME) {
                                //noinspection unchecked
                                reg.processMessageReceived(request, transportChannel);
                            } else {
                                threadPool.executor(reg.getExecutor()).execute(new RequestHandler(reg, request, transportChannel));
                            }
                        } catch (Throwable e) {
                            try {
                                transportChannel.sendResponse(e);
                            } catch (IOException e1) {
                                logger.warn("Failed to send error message back to client for action [" + action + "]", e);
                                logger.warn("Actual Exception", e1);
                            }
                        }
                        channelProfileName = transportChannel.getProfileName();
                        return action;
                    }

                    class RequestHandler extends AbstractRunnable {
                        private final RequestHandlerRegistry reg;
                        private final TransportRequest request;
                        private final NettyTransportChannel transportChannel;

                        public RequestHandler(RequestHandlerRegistry reg, TransportRequest request, NettyTransportChannel transportChannel) {
                            this.reg = reg;
                            this.request = request;
                            this.transportChannel = transportChannel;
                        }

                        @SuppressWarnings({"unchecked"})
                        @Override
                        protected void doRun() throws Exception {
                            reg.processMessageReceived(request, transportChannel);
                        }

                        @Override
                        public boolean isForceExecution() {
                            return reg.isForceExecution();
                        }

                        @Override
                        public void onFailure(Throwable e) {
                            if (transport.lifecycleState() == Lifecycle.State.STARTED) {
                                // we can only send a response transport is started....
                                try {
                                    transportChannel.sendResponse(e);
                                } catch (Throwable e1) {
                                    logger.warn("Failed to send error message back to client for action [" + reg.getAction() + "]", e1);
                                    logger.warn("Actual Exception", e);
                                }
                            }                        }
                    }
                });
                return pipeline;
            }
        }
    }
}
