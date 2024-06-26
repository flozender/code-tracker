/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.okhttp.internal.net.http;

import com.squareup.okhttp.internal.net.Dns;
import com.squareup.okhttp.internal.util.Libcore;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Selects routes to connect to an origin server. Each connection requires a
 * choice of proxy server, IP address, and TLS mode. Connections may also be
 * recycled.
 */
public final class RouteSelector {
    /**
     * A TLS connection with useful extensions enabled. This mode supports more
     * features, but is less likely to be compatible with older HTTP servers.
     */
    private static final int TLS_MODE_MODERN = 1;

    /**
     * A fallback connection with only basic functionality. Currently this uses
     * SSL 3.0.
     */
    private static final int TLS_MODE_COMPATIBLE = 0;

    /**
     * Unknown TLS mode.
     */
    private static final int TLS_MODE_NULL = -1;

    private final HttpConnection.Address address;
    private final URI uri;
    private final ProxySelector proxySelector;
    private final Dns dns;

    /* The most recently attempted route. */
    private Proxy lastProxy;
    private InetSocketAddress lastInetSocketAddress;

    /* State for negotiating the next proxy to use. */
    private boolean hasNextProxy;
    private Proxy userSpecifiedProxy;
    private Iterator<Proxy> proxySelectorProxies;

    /* State for negotiating the next InetSocketAddress to use. */
    private InetAddress[] socketAddresses;
    private int nextSocketAddressIndex;
    private String socketHost;
    private int socketPort;

    /* State for negotiating the next TLS configuration */
    private int nextTlsMode = TLS_MODE_NULL;

    public RouteSelector(HttpConnection.Address address, URI uri, ProxySelector proxySelector, Dns dns) {
        this.address = address;
        this.uri = uri;
        this.proxySelector = proxySelector;
        this.dns = dns;

        resetNextProxy(uri, address.proxy);
    }

    /**
     * Returns true if there's another route to attempt. Every address has at
     * least one route.
     */
    public boolean hasNext() {
        return hasNextTlsMode() || hasNextInetSocketAddress() || hasNextProxy();
    }

    /**
     * Returns the next route address to attempt.
     *
     * @throws NoSuchElementException if there are no more routes to attempt.
     */
    public HttpConnection next() throws IOException {
        // Always prefer pooled connections over new connections.
        HttpConnection pooled = HttpConnectionPool.INSTANCE.get(address);
        if (pooled != null) {
            return pooled;
        }

        // Compute the next route to attempt.
        if (!hasNextTlsMode()) {
            if (!hasNextInetSocketAddress()) {
                if (!hasNextProxy()) {
                    throw new NoSuchElementException();
                }
                lastProxy = nextProxy();
                resetNextInetSocketAddress(lastProxy);
            }
            lastInetSocketAddress = nextInetSocketAddress();
            resetNextTlsMode();
        }
        boolean modernTls = nextTlsMode() == TLS_MODE_MODERN;

        return new HttpConnection(address, lastProxy, lastInetSocketAddress, modernTls);
    }

    /**
     * Clients should invoke this method when they encounter a connectivity
     * failure on a connection returned by this route selector.
     */
    public void connectFailed(HttpConnection connection, IOException failure) {
        if (connection.getProxy().type() != Proxy.Type.DIRECT && proxySelector != null) {
            // Tell the proxy selector when we fail to connect on a fresh connection.
            proxySelector.connectFailed(uri, connection.getProxy().address(), failure);
        }
    }

    /** Resets {@link #nextProxy} to the first option. */
    private void resetNextProxy(URI uri, Proxy proxy) {
        this.hasNextProxy = true; // This includes NO_PROXY!
        if (proxy != null) {
            this.userSpecifiedProxy = proxy;
        } else {
            List<Proxy> proxyList = proxySelector.select(uri);
            if (proxyList != null) {
                this.proxySelectorProxies = proxyList.iterator();
            }
        }
    }

    /** Returns true if there's another proxy to try. */
    private boolean hasNextProxy() {
        return hasNextProxy;
    }

    /** Returns the next proxy to try. May be PROXY.NO_PROXY but never null. */
    private Proxy nextProxy() {
        // If the user specifies a proxy, try that and only that.
        if (userSpecifiedProxy != null) {
            hasNextProxy = false;
            return userSpecifiedProxy;
        }

        // Try each of the ProxySelector choices until one connection succeeds. If none succeed
        // then we'll try a direct connection below.
        if (proxySelectorProxies != null) {
            while (proxySelectorProxies.hasNext()) {
                Proxy candidate = proxySelectorProxies.next();
                if (candidate.type() != Proxy.Type.DIRECT) {
                    return candidate;
                }
            }
        }

        // Finally try a direct connection.
        hasNextProxy = false;
        return Proxy.NO_PROXY;
    }

    /** Resets {@link #nextInetSocketAddress} to the first option. */
    private void resetNextInetSocketAddress(Proxy proxy) throws UnknownHostException {
        socketAddresses = null; // Clear the addresses. Necessary if getAllByName() below throws!

        if (proxy.type() == Proxy.Type.DIRECT) {
            socketHost = uri.getHost();
            socketPort = Libcore.getEffectivePort(uri);
        } else {
            SocketAddress proxyAddress = proxy.address();
            if (!(proxyAddress instanceof InetSocketAddress)) {
                throw new IllegalArgumentException("Proxy.address() is not an "
                        + "InetSocketAddress: " + proxyAddress.getClass());
            }
            InetSocketAddress proxySocketAddress = (InetSocketAddress) proxyAddress;
            socketHost = proxySocketAddress.getHostName();
            socketPort = proxySocketAddress.getPort();
        }

        // Try each address for best behavior in mixed IPv4/IPv6 environments.
        socketAddresses = dns.getAllByName(socketHost);
        nextSocketAddressIndex = 0;
    }

    /** Returns true if there's another socket address to try. */
    private boolean hasNextInetSocketAddress() {
        return socketAddresses != null;
    }

    /** Returns the next socket address to try. */
    private InetSocketAddress nextInetSocketAddress() throws UnknownHostException {
        InetSocketAddress result = new InetSocketAddress(
                socketAddresses[nextSocketAddressIndex++], socketPort);
        if (nextSocketAddressIndex == socketAddresses.length) {
            socketAddresses = null; // So that hasNextInetSocketAddress() returns false.
            nextSocketAddressIndex = 0;
        }

        return result;
    }

    /** Resets {@link #nextTlsMode} to the first option. */
    private void resetNextTlsMode() {
        nextTlsMode = (address.sslSocketFactory != null)
                ? TLS_MODE_MODERN
                : TLS_MODE_COMPATIBLE;
    }

    /** Returns true if there's another TLS mode to try. */
    private boolean hasNextTlsMode() {
        return nextTlsMode != TLS_MODE_NULL;
    }

    /** Returns the next TLS mode to try. */
    private int nextTlsMode() {
        if (nextTlsMode == TLS_MODE_MODERN) {
            nextTlsMode = TLS_MODE_COMPATIBLE;
            return TLS_MODE_MODERN;
        } else if (nextTlsMode == TLS_MODE_COMPATIBLE) {
            nextTlsMode = TLS_MODE_NULL;  // So that hasNextTlsMode() returns false.
            return TLS_MODE_COMPATIBLE;
        } else {
            throw new AssertionError();
        }
    }
}
