/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.squareup.okhttp.internal.io;

import com.squareup.okhttp.Address;
import com.squareup.okhttp.CertificatePinner;
import com.squareup.okhttp.Connection;
import com.squareup.okhttp.ConnectionSpec;
import com.squareup.okhttp.Handshake;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.Route;
import com.squareup.okhttp.internal.ConnectionSpecSelector;
import com.squareup.okhttp.internal.Platform;
import com.squareup.okhttp.internal.Util;
import com.squareup.okhttp.internal.Version;
import com.squareup.okhttp.internal.framed.FramedConnection;
import com.squareup.okhttp.internal.http.Http1xStream;
import com.squareup.okhttp.internal.http.OkHeaders;
import com.squareup.okhttp.internal.http.RouteException;
import com.squareup.okhttp.internal.tls.OkHostnameVerifier;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownServiceException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;

import static com.squareup.okhttp.internal.Util.closeQuietly;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PROXY_AUTH;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public final class RealConnection implements Connection {
  private final Route route;
  private Socket socket;
  private Handshake handshake;
  private Protocol protocol;
  public FramedConnection framedConnection;
  public int streamCount;
  public BufferedSource source;
  public BufferedSink sink;
  public int allocationLimit;
  public int allocationCount;
  public boolean noNewStreams;

  public RealConnection(Route route) {
    this.route = route;
  }

  // TODO(jwilson): make non public.
  public void connect(int connectTimeout, int readTimeout, int writeTimeout,
      List<ConnectionSpec> connectionSpecs, boolean connectionRetryEnabled) throws RouteException {
    if (protocol != null) throw new IllegalStateException("already connected");

    RouteException routeException = null;
    ConnectionSpecSelector connectionSpecSelector = new ConnectionSpecSelector(connectionSpecs);
    Proxy proxy = route.getProxy();
    Address address = route.getAddress();

    if (route.getAddress().getSslSocketFactory() == null
        && !connectionSpecs.contains(ConnectionSpec.CLEARTEXT)) {
      throw new RouteException(new UnknownServiceException(
          "CLEARTEXT communication not supported: " + connectionSpecs));
    }

    while (protocol == null) {
      try {
        socket = proxy.type() == Proxy.Type.DIRECT || proxy.type() == Proxy.Type.HTTP
            ? address.getSocketFactory().createSocket()
            : new Socket(proxy);
        connectSocket(connectTimeout, readTimeout, writeTimeout, connectionSpecSelector);
      } catch (IOException e) {
        Util.closeQuietly(socket);
        socket = null;
        source = null;
        sink = null;
        handshake = null;
        protocol = null;
        framedConnection = null;

        if (routeException == null) {
          routeException = new RouteException(e);
        } else {
          routeException.addConnectException(e);
        }

        if (!connectionRetryEnabled || !connectionSpecSelector.connectionFailed(e)) {
          throw routeException;
        }
      }
    }
  }

  /** Does all the work necessary to build a full HTTP or HTTPS connection on a raw socket. */
  private void connectSocket(int connectTimeout, int readTimeout, int writeTimeout,
      ConnectionSpecSelector connectionSpecSelector) throws IOException {
    socket.setSoTimeout(readTimeout);
    try {
      Platform.get().connectSocket(socket, route.getSocketAddress(), connectTimeout);
    } catch (ConnectException e) {
      throw new ConnectException("Failed to connect to " + route.getSocketAddress());
    }
    source = Okio.buffer(Okio.source(socket));
    sink = Okio.buffer(Okio.sink(socket));

    if (route.getAddress().getSslSocketFactory() != null) {
      connectTls(readTimeout, writeTimeout, connectionSpecSelector);
    } else {
      protocol = Protocol.HTTP_1_1;
    }

    if (protocol == Protocol.SPDY_3 || protocol == Protocol.HTTP_2) {
      socket.setSoTimeout(0); // Framed connection timeouts are set per-stream.
      framedConnection = new FramedConnection.Builder(route.getAddress().url().host(), true, socket)
          .protocol(protocol).build();
      framedConnection.sendConnectionPreface();
    }
  }

  private void connectTls(int readTimeout, int writeTimeout,
      ConnectionSpecSelector connectionSpecSelector) throws IOException {
    if (route.requiresTunnel()) {
      createTunnel(readTimeout, writeTimeout);
    }

    Address address = route.getAddress();
    SSLSocketFactory sslSocketFactory = address.getSslSocketFactory();
    boolean success = false;
    SSLSocket sslSocket = null;
    try {
      // Create the wrapper over the connected socket.
      sslSocket = (SSLSocket) sslSocketFactory.createSocket(
          socket, address.getUriHost(), address.getUriPort(), true /* autoClose */);

      // Configure the socket's ciphers, TLS versions, and extensions.
      ConnectionSpec connectionSpec = connectionSpecSelector.configureSecureSocket(sslSocket);
      if (connectionSpec.supportsTlsExtensions()) {
        Platform.get().configureTlsExtensions(
            sslSocket, address.getUriHost(), address.getProtocols());
      }

      // Force handshake. This can throw!
      sslSocket.startHandshake();
      Handshake unverifiedHandshake = Handshake.get(sslSocket.getSession());

      // Verify that the socket's certificates are acceptable for the target host.
      if (!address.getHostnameVerifier().verify(address.getUriHost(), sslSocket.getSession())) {
        X509Certificate cert = (X509Certificate) unverifiedHandshake.peerCertificates().get(0);
        throw new SSLPeerUnverifiedException("Hostname " + address.getUriHost() + " not verified:"
            + "\n    certificate: " + CertificatePinner.pin(cert)
            + "\n    DN: " + cert.getSubjectDN().getName()
            + "\n    subjectAltNames: " + OkHostnameVerifier.allSubjectAltNames(cert));
      }

      // Check that the certificate pinner is satisfied by the certificates presented.
      address.getCertificatePinner().check(address.getUriHost(),
          unverifiedHandshake.peerCertificates());

      // Success! Save the handshake and the ALPN protocol.
      String maybeProtocol = connectionSpec.supportsTlsExtensions()
          ? Platform.get().getSelectedProtocol(sslSocket)
          : null;
      socket = sslSocket;
      source = Okio.buffer(Okio.source(socket));
      sink = Okio.buffer(Okio.sink(socket));
      handshake = unverifiedHandshake;
      protocol = maybeProtocol != null
          ? Protocol.get(maybeProtocol)
          : Protocol.HTTP_1_1;
      success = true;
    } catch (AssertionError e) {
      if (Util.isAndroidGetsocknameError(e)) throw new IOException(e);
      throw e;
    } finally {
      if (sslSocket != null) {
        Platform.get().afterHandshake(sslSocket);
      }
      if (!success) {
        closeQuietly(sslSocket);
      }
    }
  }

  /**
   * To make an HTTPS connection over an HTTP proxy, send an unencrypted
   * CONNECT request to create the proxy connection. This may need to be
   * retried if the proxy requires authorization.
   */
  private void createTunnel(int readTimeout, int writeTimeout) throws IOException {
    // Make an SSL Tunnel on the first message pair of each SSL + proxy connection.
    Request tunnelRequest = createTunnelRequest();
    HttpUrl url = tunnelRequest.httpUrl();
    String requestLine = "CONNECT " + url.host() + ":" + url.port() + " HTTP/1.1";
    while (true) {
      Http1xStream tunnelConnection = new Http1xStream(null, source, sink);
      source.timeout().timeout(readTimeout, MILLISECONDS);
      sink.timeout().timeout(writeTimeout, MILLISECONDS);
      tunnelConnection.writeRequest(tunnelRequest.headers(), requestLine);
      tunnelConnection.finishRequest();
      Response response = tunnelConnection.readResponse().request(tunnelRequest).build();
      // The response body from a CONNECT should be empty, but if it is not then we should consume
      // it before proceeding.
      long contentLength = OkHeaders.contentLength(response);
      if (contentLength == -1L) {
        contentLength = 0L;
      }
      Source body = tunnelConnection.newFixedLengthSource(contentLength);
      Util.skipAll(body, Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
      body.close();

      switch (response.code()) {
        case HTTP_OK:
          // Assume the server won't send a TLS ServerHello until we send a TLS ClientHello. If
          // that happens, then we will have buffered bytes that are needed by the SSLSocket!
          // This check is imperfect: it doesn't tell us whether a handshake will succeed, just
          // that it will almost certainly fail because the proxy has sent unexpected data.
          if (!source.buffer().exhausted() || !sink.buffer().exhausted()) {
            throw new IOException("TLS tunnel buffered too many bytes!");
          }
          return;

        case HTTP_PROXY_AUTH:
          tunnelRequest = OkHeaders.processAuthHeader(
              route.getAddress().getAuthenticator(), response, route.getProxy());
          if (tunnelRequest != null) continue;
          throw new IOException("Failed to authenticate with proxy");

        default:
          throw new IOException(
              "Unexpected response code for CONNECT: " + response.code());
      }
    }
  }

  /**
   * Returns a request that creates a TLS tunnel via an HTTP proxy, or null if
   * no tunnel is necessary. Everything in the tunnel request is sent
   * unencrypted to the proxy server, so tunnels include only the minimum set of
   * headers. This avoids sending potentially sensitive data like HTTP cookies
   * to the proxy unencrypted.
   */
  private Request createTunnelRequest() throws IOException {
    return new Request.Builder()
        .url(route.getAddress().url())
        .header("Host", Util.hostHeader(route.getAddress().url()))
        .header("Proxy-Connection", "Keep-Alive")
        .header("User-Agent", Version.userAgent()) // For HTTP/1.0 proxies like Squid.
        .build();
  }

  /** Returns true if {@link #connect} has been attempted on this connection. */
  boolean isConnected() {
    return protocol != null;
  }

  @Override public Route getRoute() {
    return route;
  }

  public void cancel() {
    Util.closeQuietly(socket);
  }

  @Override public Socket getSocket() {
    return socket;
  }

  /** Returns true if this connection is ready to host new streams. */
  public boolean isHealthy(boolean doExtensiveChecks) {
    if (socket.isClosed() || socket.isInputShutdown() || socket.isOutputShutdown()) {
      return false;
    }

    if (framedConnection != null) {
      return true; // TODO: check framedConnection.shutdown.
    }

    if (doExtensiveChecks) {
      try {
        int readTimeout = socket.getSoTimeout();
        try {
          socket.setSoTimeout(1);
          if (source.exhausted()) {
            return false; // Stream is exhausted; socket is closed.
          }
          return true;
        } finally {
          socket.setSoTimeout(readTimeout);
        }
      } catch (SocketTimeoutException ignored) {
        // Read timed out; socket is good.
      } catch (IOException e) {
        return false; // Couldn't read; socket is closed.
      }
    }

    return true;
  }

  @Override public Handshake getHandshake() {
    return handshake;
  }

  /**
   * Returns true if this is a SPDY connection. Such connections can be used
   * in multiple HTTP requests simultaneously.
   */
  public boolean isMultiplexed() {
    return framedConnection != null;
  }

  @Override public Protocol getProtocol() {
    return protocol != null ? protocol : Protocol.HTTP_1_1;
  }

  @Override public String toString() {
    return "Connection{"
        + route.getAddress().url().host() + ":" + route.getAddress().url().port()
        + ", proxy="
        + route.getProxy()
        + " hostAddress="
        + route.getSocketAddress()
        + " cipherSuite="
        + (handshake != null ? handshake.cipherSuite() : "none")
        + " protocol="
        + protocol
        + '}';
  }
}
