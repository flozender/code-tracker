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

package org.elasticsearch.transport.local;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.component.Lifecycle;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.ThrowableObjectInputStream;
import org.elasticsearch.common.io.stream.*;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.BoundTransportAddress;
import org.elasticsearch.common.transport.LocalTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.util.concurrent.AbstractRunnable;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.*;
import org.elasticsearch.transport.support.TransportStatus;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.elasticsearch.common.util.concurrent.ConcurrentCollections.newConcurrentMap;

/**
 *
 */
public class LocalTransport extends AbstractLifecycleComponent<Transport> implements Transport {

    private final ThreadPool threadPool;
    private final Version version;
    private volatile TransportServiceAdapter transportServiceAdapter;
    private volatile BoundTransportAddress boundAddress;
    private volatile LocalTransportAddress localAddress;
    private final static ConcurrentMap<TransportAddress, LocalTransport> transports = newConcurrentMap();
    private static final AtomicLong transportAddressIdGenerator = new AtomicLong();
    private final ConcurrentMap<DiscoveryNode, LocalTransport> connectedNodes = newConcurrentMap();

    @Inject
    public LocalTransport(Settings settings, ThreadPool threadPool, Version version) {
        super(settings);
        this.threadPool = threadPool;
        this.version = version;
    }

    @Override
    public TransportAddress[] addressesFromString(String address) {
        return new TransportAddress[]{new LocalTransportAddress(address)};
    }

    @Override
    public boolean addressSupported(Class<? extends TransportAddress> address) {
        return LocalTransportAddress.class.equals(address);
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        localAddress = new LocalTransportAddress(Long.toString(transportAddressIdGenerator.incrementAndGet()));
        transports.put(localAddress, this);
        boundAddress = new BoundTransportAddress(localAddress, localAddress);
    }

    @Override
    protected void doStop() throws ElasticsearchException {
        transports.remove(localAddress);
        // now, go over all the transports connected to me, and raise disconnected event
        for (final LocalTransport targetTransport : transports.values()) {
            for (final Map.Entry<DiscoveryNode, LocalTransport> entry : targetTransport.connectedNodes.entrySet()) {
                if (entry.getValue() == this) {
                    targetTransport.disconnectFromNode(entry.getKey());
                }
            }
        }
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }

    @Override
    public void transportServiceAdapter(TransportServiceAdapter transportServiceAdapter) {
        this.transportServiceAdapter = transportServiceAdapter;
    }

    @Override
    public BoundTransportAddress boundAddress() {
        return boundAddress;
    }

    @Override
    public boolean nodeConnected(DiscoveryNode node) {
        return connectedNodes.containsKey(node);
    }

    @Override
    public void connectToNodeLight(DiscoveryNode node) throws ConnectTransportException {
        connectToNode(node);
    }

    @Override
    public void connectToNode(DiscoveryNode node) throws ConnectTransportException {
        synchronized (this) {
            if (connectedNodes.containsKey(node)) {
                return;
            }
            final LocalTransport targetTransport = transports.get(node.address());
            if (targetTransport == null) {
                throw new ConnectTransportException(node, "Failed to connect");
            }
            connectedNodes.put(node, targetTransport);
            transportServiceAdapter.raiseNodeConnected(node);
        }
    }

    @Override
    public void disconnectFromNode(DiscoveryNode node) {
        synchronized (this) {
            LocalTransport removed = connectedNodes.remove(node);
            if (removed != null) {
                transportServiceAdapter.raiseNodeDisconnected(node);
            }
        }
    }

    @Override
    public long serverOpen() {
        return 0;
    }

    @Override
    public void sendRequest(final DiscoveryNode node, final long requestId, final String action, final TransportRequest request, TransportRequestOptions options) throws IOException, TransportException {
        final Version version = Version.smallest(node.version(), this.version);

        BytesStreamOutput bStream = new BytesStreamOutput();
        StreamOutput stream = new HandlesStreamOutput(bStream);
        stream.setVersion(version);

        stream.writeLong(requestId);
        byte status = 0;
        status = TransportStatus.setRequest(status);
        stream.writeByte(status); // 0 for request, 1 for response.

        stream.writeString(action);
        request.writeTo(stream);

        stream.close();

        final LocalTransport targetTransport = connectedNodes.get(node);
        if (targetTransport == null) {
            throw new NodeNotConnectedException(node, "Node not connected");
        }

        final byte[] data = bStream.bytes().toBytes();

        transportServiceAdapter.sent(data.length);

        threadPool.generic().execute(new Runnable() {
            @Override
            public void run() {
                targetTransport.messageReceived(data, action, LocalTransport.this, version, requestId);
            }
        });
    }

    ThreadPool threadPool() {
        return this.threadPool;
    }

    protected void messageReceived(byte[] data, String action, LocalTransport sourceTransport, Version version, @Nullable final Long sendRequestId) {
        try {
            transportServiceAdapter.received(data.length);
            StreamInput stream = new BytesStreamInput(data, false);
            stream = CachedStreamInput.cachedHandles(stream);
            stream.setVersion(version);

            long requestId = stream.readLong();
            byte status = stream.readByte();
            boolean isRequest = TransportStatus.isRequest(status);

            if (isRequest) {
                handleRequest(stream, requestId, sourceTransport, version);
            } else {
                final TransportResponseHandler handler = transportServiceAdapter.remove(requestId);
                // ignore if its null, the adapter logs it
                if (handler != null) {
                    if (TransportStatus.isError(status)) {
                        handlerResponseError(stream, handler);
                    } else {
                        handleResponse(stream, handler);
                    }
                }
            }
        } catch (Throwable e) {
            if (sendRequestId != null) {
                TransportResponseHandler handler = transportServiceAdapter.remove(sendRequestId);
                if (handler != null) {
                    handleException(handler, new RemoteTransportException(nodeName(), localAddress, action, e));
                }
            } else {
                logger.warn("Failed to receive message for action [" + action + "]", e);
            }
        }
    }

    private void handleRequest(StreamInput stream, long requestId, LocalTransport sourceTransport, Version version) throws Exception {
        final String action = stream.readString();
        final LocalTransportChannel transportChannel = new LocalTransportChannel(this, sourceTransport, action, requestId, version);
        try {
            final TransportRequestHandler handler = transportServiceAdapter.handler(action);
            if (handler == null) {
                throw new ActionNotFoundTransportException("Action [" + action + "] not found");
            }
            final TransportRequest request = handler.newInstance();
            request.readFrom(stream);
            if (handler.executor() == ThreadPool.Names.SAME) {
                //noinspection unchecked
                handler.messageReceived(request, transportChannel);
            } else {
                threadPool.executor(handler.executor()).execute(new AbstractRunnable() {
                    @Override
                    public void run() {
                        try {
                            //noinspection unchecked
                            handler.messageReceived(request, transportChannel);
                        } catch (Throwable e) {
                            if (lifecycleState() == Lifecycle.State.STARTED) {
                                // we can only send a response transport is started....
                                try {
                                    transportChannel.sendResponse(e);
                                } catch (Throwable e1) {
                                    logger.warn("Failed to send error message back to client for action [" + action + "]", e1);
                                    logger.warn("Actual Exception", e);
                                }
                            }
                        }
                    }

                    @Override
                    public boolean isForceExecution() {
                        return handler.isForceExecution();
                    }
                });
            }
        } catch (Throwable e) {
            try {
                transportChannel.sendResponse(e);
            } catch (Throwable e1) {
                logger.warn("Failed to send error message back to client for action [" + action + "]", e);
                logger.warn("Actual Exception", e1);
            }
        }
    }


    protected void handleResponse(StreamInput buffer, final TransportResponseHandler handler) {
        final TransportResponse response = handler.newInstance();
        try {
            response.readFrom(buffer);
        } catch (Throwable e) {
            handleException(handler, new TransportSerializationException("Failed to deserialize response of type [" + response.getClass().getName() + "]", e));
            return;
        }
        handleParsedRespone(response, handler);
    }

    protected void handleParsedRespone(final TransportResponse response, final TransportResponseHandler handler) {
        threadPool.executor(handler.executor()).execute(new Runnable() {
            @SuppressWarnings({"unchecked"})
            @Override
            public void run() {
                try {
                    handler.handleResponse(response);
                } catch (Throwable e) {
                    handleException(handler, new ResponseHandlerFailureTransportException(e));
                }
            }
        });
    }

    private void handlerResponseError(StreamInput buffer, final TransportResponseHandler handler) {
        Throwable error;
        try {
            ThrowableObjectInputStream ois = new ThrowableObjectInputStream(buffer, settings.getClassLoader());
            error = (Throwable) ois.readObject();
        } catch (Throwable e) {
            error = new TransportSerializationException("Failed to deserialize exception response from stream", e);
        }
        handleException(handler, error);
    }

    private void handleException(final TransportResponseHandler handler, Throwable error) {
        if (!(error instanceof RemoteTransportException)) {
            error = new RemoteTransportException("None remote transport exception", error);
        }
        final RemoteTransportException rtx = (RemoteTransportException) error;
        try {
            handler.handleException(rtx);
        } catch (Throwable t) {
            logger.error("failed to handle exception response [{}]", t, handler);
        }
    }
}
