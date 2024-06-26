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

package com.squareup.okhttp.internal.http;

import com.squareup.okhttp.Address;
import com.squareup.okhttp.Connection;
import com.squareup.okhttp.Handshake;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkResponseCache;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseSource;
import com.squareup.okhttp.TunnelRequest;
import com.squareup.okhttp.internal.Dns;
import com.squareup.okhttp.internal.Platform;
import com.squareup.okhttp.internal.Util;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CacheRequest;
import java.net.CookieHandler;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import static com.squareup.okhttp.internal.Util.EMPTY_INPUT_STREAM;
import static com.squareup.okhttp.internal.Util.getDefaultPort;
import static com.squareup.okhttp.internal.Util.getEffectivePort;

/**
 * Handles a single HTTP request/response pair. Each HTTP engine follows this
 * lifecycle:
 * <ol>
 * <li>It is created.
 * <li>The HTTP request message is sent with sendRequest(). Once the request
 * is sent it is an error to modify the request headers. After
 * sendRequest() has been called the request body can be written to if
 * it exists.
 * <li>The HTTP response message is read with readResponse(). After the
 * response has been read the response headers and body can be read.
 * All responses have a response body input stream, though in some
 * instances this stream is empty.
 * </ol>
 *
 * <p>The request and response may be served by the HTTP response cache, by the
 * network, or by both in the event of a conditional GET.
 *
 * <p>This class may hold a socket connection that needs to be released or
 * recycled. By default, this socket connection is held when the last byte of
 * the response is consumed. To release the connection when it is no longer
 * required, use {@link #automaticallyReleaseConnectionToPool()}.
 *
 * <p>Since we permit redirects across protocols (HTTP to HTTPS or vice versa),
 * the implementation type of the connection doesn't necessarily match the
 * implementation type of its HttpEngine.
 */
public class HttpEngine {
  private static final Response.Body EMPTY_BODY = new Response.Body() {
    @Override public boolean ready() throws IOException {
      return true;
    }
    @Override public MediaType contentType() {
      return null;
    }
    @Override public long contentLength() {
      return 0;
    }
    @Override public InputStream byteStream() {
      return EMPTY_INPUT_STREAM;
    }
  };

  public static final int HTTP_CONTINUE = 100;

  protected final Policy policy;
  protected final OkHttpClient client;

  protected final String method;

  private ResponseSource responseSource;

  protected Connection connection;
  private Handshake handshake;
  protected RouteSelector routeSelector;
  private OutputStream requestBodyOut;

  private Transport transport;

  private InputStream responseTransferIn;
  private InputStream responseBodyIn;

  /** The time when the request headers were written, or -1 if they haven't been written yet. */
  long sentRequestMillis = -1;

  /**
   * True if this client added an "Accept-Encoding: gzip" header field and is
   * therefore responsible for also decompressing the transfer stream.
   */
  private boolean transparentGzip;

  final URI uri;

  RequestHeaders requestHeaders;

  /** Null until a response is received from the network or the cache. */
  ResponseHeaders responseHeaders;

  /**
   * The cache response currently being validated on a conditional get. Null
   * if the cached response doesn't exist or doesn't need validation. If the
   * conditional get succeeds, these will be used for the response. If it fails,
   * it will be set to null.
   */
  private Response validatingResponse;

  /** The cache request currently being populated from a network response. */
  private CacheRequest cacheRequest;

  /**
   * True if the socket connection should be released to the connection pool
   * when the response has been fully read.
   */
  private boolean automaticallyReleaseConnectionToPool;

  /** True if the socket connection is no longer needed by this engine. */
  private boolean connectionReleased;

  /**
   * @param requestHeaders the client's supplied request headers. This class
   *     creates a private copy that it can mutate.
   * @param connection the connection used for an intermediate response
   *     immediately prior to this request/response pair, such as a same-host
   *     redirect. This engine assumes ownership of the connection and must
   *     release it when it is unneeded.
   */
  public HttpEngine(OkHttpClient client, Policy policy, String method, RawHeaders requestHeaders,
      Connection connection, RetryableOutputStream requestBodyOut) throws IOException {
    this.client = client;
    this.policy = policy;
    this.method = method;
    this.connection = connection;
    this.requestBodyOut = requestBodyOut;

    try {
      uri = Platform.get().toUriLenient(policy.getURL());
    } catch (URISyntaxException e) {
      throw new IOException(e.getMessage());
    }

    this.requestHeaders = new RequestHeaders(uri, requestHeaders);

    if (connection != null && connection.getSocket() instanceof SSLSocket) {
      handshake = Handshake.get(((SSLSocket) connection.getSocket()).getSession());
    }
  }

  public URI getUri() {
    return uri;
  }

  /**
   * Figures out what the response source will be, and opens a socket to that
   * source if necessary. Prepares the request headers and gets ready to start
   * writing the request body if it exists.
   */
  public final void sendRequest() throws IOException {
    if (responseSource != null) {
      return;
    }

    prepareRawRequestHeaders();
    initResponseSource();
    OkResponseCache responseCache = client.getOkResponseCache();
    if (responseCache != null) {
      responseCache.trackResponse(responseSource);
    }

    // The raw response source may require the network, but the request
    // headers may forbid network use. In that case, dispose of the network
    // response and use a gateway timeout response instead, as specified
    // by http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.4.
    if (requestHeaders.isOnlyIfCached() && responseSource.requiresConnection()) {
      if (responseSource == ResponseSource.CONDITIONAL_CACHE) {
        Util.closeQuietly(validatingResponse.body());
      }
      this.responseSource = ResponseSource.CACHE;

      RawHeaders gatewayTimeoutHeaders = new RawHeaders.Builder()
          .setStatusLine("HTTP/1.1 504 Gateway Timeout")
          .build();
      this.validatingResponse = new Response.Builder(request(), 504)
          .rawHeaders(gatewayTimeoutHeaders)
          .body(EMPTY_BODY)
          .build();
      promoteValidatingResponse(new ResponseHeaders(uri, gatewayTimeoutHeaders));
    }

    if (responseSource.requiresConnection()) {
      sendSocketRequest();
    } else if (connection != null) {
      client.getConnectionPool().recycle(connection);
      connection = null;
    }
  }

  /**
   * Initialize the source for this response. It may be corrected later if the
   * request headers forbids network use.
   */
  private void initResponseSource() throws IOException {
    responseSource = ResponseSource.NETWORK;
    if (!policy.getUseCaches()) return;

    OkResponseCache responseCache = client.getOkResponseCache();
    if (responseCache == null) return;

    Response candidate = responseCache.get(request());
    if (candidate == null) return;

    if (!acceptCacheResponseType(candidate)) {
      Util.closeQuietly(candidate.body());
      return;
    }

    ResponseHeaders cachedResponseHeaders = new ResponseHeaders(uri, candidate.rawHeaders());
    long now = System.currentTimeMillis();
    ResponseStrategy responseStrategy = ResponseStrategy.get(
        now, cachedResponseHeaders, requestHeaders);
    this.responseSource = responseStrategy.source;
    this.requestHeaders = responseStrategy.request;
    cachedResponseHeaders = responseStrategy.response;

    if (responseSource == ResponseSource.CACHE) {
      this.validatingResponse = candidate;
      promoteValidatingResponse(cachedResponseHeaders);
    } else if (responseSource == ResponseSource.CONDITIONAL_CACHE) {
      this.validatingResponse = candidate;
    } else if (responseSource == ResponseSource.NETWORK) {
      Util.closeQuietly(candidate.body());
    } else {
      throw new AssertionError();
    }
  }

  private Request request() {
    // This doesn't have a body. When we're sending requests to the cache, we don't need it.
    return new Request.Builder(policy.getURL())
        .method(method, null)
        .rawHeaders(requestHeaders.getHeaders())
        .build();
  }

  private Response response() {
    RawHeaders rawHeaders = responseHeaders.getHeaders();

    // Use an unreadable response body when offering the response to the cache. The cache isn't
    // allowed to consume the response body bytes!
    Response.Body body = new UnreadableResponseBody(responseHeaders.getContentType(),
        responseHeaders.getContentLength());

    return new Response.Builder(request(), rawHeaders.getResponseCode())
        .body(body)
        .rawHeaders(rawHeaders)
        .handshake(handshake)
        .build();
  }

  private void sendSocketRequest() throws IOException {
    if (connection == null) {
      connect();
    }

    if (transport != null) {
      throw new IllegalStateException();
    }

    transport = (Transport) connection.newTransport(this);
    requestHeaders = transport.prepareRequestHeaders(requestHeaders);

    if (hasRequestBody() && requestBodyOut == null) {
      // Create a request body if we don't have one already. We'll already
      // have one if we're retrying a failed POST.
      requestBodyOut = transport.createRequestBody();
    }
  }

  /** Connect to the origin server either directly or via a proxy. */
  protected final void connect() throws IOException {
    if (connection != null) {
      return;
    }
    if (routeSelector == null) {
      String uriHost = uri.getHost();
      if (uriHost == null) {
        throw new UnknownHostException(uri.toString());
      }
      SSLSocketFactory sslSocketFactory = null;
      HostnameVerifier hostnameVerifier = null;
      if (uri.getScheme().equalsIgnoreCase("https")) {
        sslSocketFactory = client.getSslSocketFactory();
        hostnameVerifier = client.getHostnameVerifier();
      }
      Address address = new Address(uriHost, getEffectivePort(uri), sslSocketFactory,
          hostnameVerifier, client.getAuthenticator(), client.getProxy(), client.getTransports());
      routeSelector = new RouteSelector(address, uri, client.getProxySelector(),
          client.getConnectionPool(), Dns.DEFAULT, client.getRoutesDatabase());
    }
    connection = routeSelector.next(method);
    if (!connection.isConnected()) {
      connection.connect(client.getConnectTimeout(), client.getReadTimeout(), getTunnelConfig());
      client.getConnectionPool().maybeShare(connection);
      client.getRoutesDatabase().connected(connection.getRoute());
    } else {
      connection.updateReadTimeout(client.getReadTimeout());
    }
    connected(connection);
    if (connection.getRoute().getProxy() != client.getProxy()) {
      // Update the request line if the proxy changed; it may need a host name.
      requestHeaders = requestHeaders.newBuilder().setRequestLine(getRequestLine()).build();
    }
  }

  /**
   * Called after a socket connection has been created or retrieved from the
   * pool. Subclasses use this hook to get a reference to the TLS data.
   */
  private void connected(Connection connection) {
    if (handshake == null && connection.getSocket() instanceof SSLSocket) {
      handshake = Handshake.get(((SSLSocket) connection.getSocket()).getSession());
    }
    policy.setSelectedProxy(connection.getRoute().getProxy());
  }

  /**
   * Called immediately before the transport transmits HTTP request headers.
   * This is used to observe the sent time should the request be cached.
   */
  public void writingRequestHeaders() {
    if (sentRequestMillis != -1) {
      throw new IllegalStateException();
    }
    sentRequestMillis = System.currentTimeMillis();
  }

  private void promoteValidatingResponse(ResponseHeaders responseHeaders) throws IOException {
    if (this.responseBodyIn != null) throw new IllegalStateException();

    this.responseHeaders = responseHeaders;
    this.handshake = validatingResponse.handshake();
    if (validatingResponse.body() != null) {
      initContentStream(validatingResponse.body().byteStream());
    }
  }

  boolean hasRequestBody() {
    return method.equals("POST") || method.equals("PUT") || method.equals("PATCH");
  }

  /** Returns the request body or null if this request doesn't have a body. */
  public final OutputStream getRequestBody() {
    if (responseSource == null) {
      throw new IllegalStateException();
    }
    return requestBodyOut;
  }

  public final boolean hasResponse() {
    return responseHeaders != null;
  }

  public final RequestHeaders getRequestHeaders() {
    return requestHeaders;
  }

  public final ResponseHeaders getResponseHeaders() {
    if (responseHeaders == null) {
      throw new IllegalStateException();
    }
    return responseHeaders;
  }

  public final int getResponseCode() {
    if (responseHeaders == null) {
      throw new IllegalStateException();
    }
    return responseHeaders.getHeaders().getResponseCode();
  }

  public final InputStream getResponseBody() {
    if (responseHeaders == null) {
      throw new IllegalStateException();
    }
    return responseBodyIn;
  }

  public final Connection getConnection() {
    return connection;
  }

  /**
   * Returns true if {@code response} is of the right type. This condition is
   * necessary but not sufficient for the cached response to be used.
   */
  protected boolean acceptCacheResponseType(Response response) {
    return true;
  }

  private void maybeCache() throws IOException {
    // Are we caching at all?
    if (!policy.getUseCaches()) return;
    OkResponseCache responseCache = client.getOkResponseCache();
    if (responseCache == null) return;

    // Should we cache this response for this request?
    if (!ResponseStrategy.isCacheable(responseHeaders, requestHeaders)) {
      responseCache.maybeRemove(request());
      return;
    }

    // Offer this request to the cache.
    cacheRequest = responseCache.put(response());
  }

  /**
   * Cause the socket connection to be released to the connection pool when
   * it is no longer needed. If it is already unneeded, it will be pooled
   * immediately. Otherwise the connection is held so that redirects can be
   * handled by the same connection.
   */
  public final void automaticallyReleaseConnectionToPool() {
    automaticallyReleaseConnectionToPool = true;
    if (connection != null && connectionReleased) {
      client.getConnectionPool().recycle(connection);
      connection = null;
    }
  }

  /**
   * Releases this engine so that its resources may be either reused or
   * closed. Also call {@link #automaticallyReleaseConnectionToPool} unless
   * the connection will be used to follow a redirect.
   */
  public final void release(boolean streamCanceled) {
    // If the response body comes from the cache, close it.
    if (validatingResponse != null
        && validatingResponse.body() != null
        && responseBodyIn == validatingResponse.body().byteStream()) {
      Util.closeQuietly(responseBodyIn);
    }

    if (!connectionReleased && connection != null) {
      connectionReleased = true;

      if (transport == null
          || !transport.makeReusable(streamCanceled, requestBodyOut, responseTransferIn)) {
        Util.closeQuietly(connection);
        connection = null;
      } else if (automaticallyReleaseConnectionToPool) {
        client.getConnectionPool().recycle(connection);
        connection = null;
      }
    }
  }

  private void initContentStream(InputStream transferStream) throws IOException {
    responseTransferIn = transferStream;
    if (transparentGzip && responseHeaders.isContentEncodingGzip()) {
      // If the response was transparently gzipped, remove the gzip header field
      // so clients don't double decompress. http://b/3009828
      //
      // Also remove the Content-Length in this case because it contains the
      // length of the gzipped response. This isn't terribly useful and is
      // dangerous because clients can query the content length, but not the
      // content encoding.
      responseHeaders = responseHeaders.newBuilder()
          .stripContentEncoding()
          .stripContentLength()
          .build();
      responseBodyIn = new GZIPInputStream(transferStream);
    } else {
      responseBodyIn = transferStream;
    }
  }

  /**
   * Returns true if the response must have a (possibly 0-length) body.
   * See RFC 2616 section 4.3.
   */
  public final boolean hasResponseBody() {
    int responseCode = responseHeaders.getHeaders().getResponseCode();

    // HEAD requests never yield a body regardless of the response headers.
    if (method.equals("HEAD")) {
      return false;
    }

    if ((responseCode < HTTP_CONTINUE || responseCode >= 200)
        && responseCode != HttpURLConnectionImpl.HTTP_NO_CONTENT
        && responseCode != HttpURLConnectionImpl.HTTP_NOT_MODIFIED) {
      return true;
    }

    // If the Content-Length or Transfer-Encoding headers disagree with the
    // response code, the response is malformed. For best compatibility, we
    // honor the headers.
    if (responseHeaders.getContentLength() != -1 || responseHeaders.isChunked()) {
      return true;
    }

    return false;
  }

  /**
   * Populates requestHeaders with defaults and cookies.
   *
   * <p>This client doesn't specify a default {@code Accept} header because it
   * doesn't know what content types the application is interested in.
   */
  private void prepareRawRequestHeaders() throws IOException {
    RequestHeaders.Builder result = requestHeaders.newBuilder();

    result.setRequestLine(getRequestLine());

    if (requestHeaders.getUserAgent() == null) {
      result.setUserAgent(getDefaultUserAgent());
    }

    if (requestHeaders.getHost() == null) {
      result.setHost(getOriginAddress(policy.getURL()));
    }

    if ((connection == null || connection.getHttpMinorVersion() != 0)
        && requestHeaders.getConnection() == null) {
      result.setConnection("Keep-Alive");
    }

    if (requestHeaders.getAcceptEncoding() == null) {
      transparentGzip = true;
      result.setAcceptEncoding("gzip");
    }

    if (hasRequestBody() && requestHeaders.getContentType() == null) {
      result.setContentType("application/x-www-form-urlencoded");
    }

    long ifModifiedSince = policy.getIfModifiedSince();
    if (ifModifiedSince != 0) {
      result.setIfModifiedSince(new Date(ifModifiedSince));
    }

    CookieHandler cookieHandler = client.getCookieHandler();
    if (cookieHandler != null) {
      result.addCookies(
          cookieHandler.get(uri, requestHeaders.getHeaders().toMultimap(false)));
    }

    requestHeaders = result.build();
  }

  /**
   * Returns the request status line, like "GET / HTTP/1.1". This is exposed
   * to the application by {@link HttpURLConnectionImpl#getHeaderFields}, so
   * it needs to be set even if the transport is SPDY.
   */
  String getRequestLine() {
    String protocol =
        (connection == null || connection.getHttpMinorVersion() != 0) ? "HTTP/1.1" : "HTTP/1.0";
    return method + " " + requestString() + " " + protocol;
  }

  private String requestString() {
    URL url = policy.getURL();
    if (includeAuthorityInRequestLine()) {
      return url.toString();
    } else {
      return requestPath(url);
    }
  }

  /**
   * Returns the path to request, like the '/' in 'GET / HTTP/1.1'. Never
   * empty, even if the request URL is. Includes the query component if it
   * exists.
   */
  public static String requestPath(URL url) {
    String fileOnly = url.getFile();
    if (fileOnly == null) {
      return "/";
    } else if (!fileOnly.startsWith("/")) {
      return "/" + fileOnly;
    } else {
      return fileOnly;
    }
  }

  /**
   * Returns true if the request line should contain the full URL with host
   * and port (like "GET http://android.com/foo HTTP/1.1") or only the path
   * (like "GET /foo HTTP/1.1").
   *
   * <p>This is non-final because for HTTPS it's never necessary to supply the
   * full URL, even if a proxy is in use.
   */
  protected boolean includeAuthorityInRequestLine() {
    return connection == null
        ? policy.usingProxy() // A proxy was requested.
        : connection.getRoute().getProxy().type() == Proxy.Type.HTTP; // A proxy was selected.
  }

  /**
   * Returns the TLS handshake created when this engine connected, or null if
   * no TLS connection was made.
   */
  public Handshake getHandshake() {
    return handshake;
  }

  public static String getDefaultUserAgent() {
    String agent = System.getProperty("http.agent");
    return agent != null ? agent : ("Java" + System.getProperty("java.version"));
  }

  public static String getOriginAddress(URL url) {
    int port = url.getPort();
    String result = url.getHost();
    if (port > 0 && port != getDefaultPort(url.getProtocol())) {
      result = result + ":" + port;
    }
    return result;
  }

  /**
   * Flushes the remaining request header and body, parses the HTTP response
   * headers and starts reading the HTTP response body if it exists.
   */
  public final void readResponse() throws IOException {
    if (hasResponse()) {
      responseHeaders = responseHeaders.newBuilder().setResponseSource(responseSource).build();
      return;
    }

    if (responseSource == null) {
      throw new IllegalStateException("readResponse() without sendRequest()");
    }

    if (!responseSource.requiresConnection()) {
      return;
    }

    if (sentRequestMillis == -1) {
      if (requestHeaders.getContentLength() == -1
          && requestBodyOut instanceof RetryableOutputStream) {
        // We might not learn the Content-Length until the request body has been buffered.
        int contentLength = ((RetryableOutputStream) requestBodyOut).contentLength();
        requestHeaders = requestHeaders.newBuilder().setContentLength(contentLength).build();
      }
      transport.writeRequestHeaders();
    }

    if (requestBodyOut != null) {
      requestBodyOut.close();
      if (requestBodyOut instanceof RetryableOutputStream) {
        transport.writeRequestBody((RetryableOutputStream) requestBodyOut);
      }
    }

    transport.flushRequest();

    responseHeaders = transport.readResponseHeaders()
        .newBuilder()
        .setLocalTimestamps(sentRequestMillis, System.currentTimeMillis())
        .setResponseSource(responseSource)
        .build();

    if (responseSource == ResponseSource.CONDITIONAL_CACHE) {
      ResponseHeaders validatingResponseHeaders = new ResponseHeaders(
          uri, validatingResponse.rawHeaders());

      if (validatingResponseHeaders.validate(responseHeaders)) {
        release(false);
        responseHeaders = validatingResponseHeaders.combine(responseHeaders);
        handshake = validatingResponse.handshake();

        // Update the cache after combining headers but before stripping the
        // Content-Encoding header (as performed by initContentStream()).
        OkResponseCache responseCache = client.getOkResponseCache();
        responseCache.trackConditionalCacheHit();
        responseCache.update(validatingResponse, response());

        if (validatingResponse.body() != null) {
          initContentStream(validatingResponse.body().byteStream());
        }
        return;
      } else {
        Util.closeQuietly(validatingResponse.body());
      }
    }

    if (hasResponseBody()) {
      maybeCache(); // reentrant. this calls into user code which may call back into this!
    }

    initContentStream(transport.getTransferStream(cacheRequest));
  }

  protected TunnelRequest getTunnelConfig() {
    return null;
  }

  public void receiveHeaders(RawHeaders headers) throws IOException {
    CookieHandler cookieHandler = client.getCookieHandler();
    if (cookieHandler != null) {
      cookieHandler.put(uri, headers.toMultimap(true));
    }
  }

  static class UnreadableResponseBody extends Response.Body {
    private final String contentType;
    private final long contentLength;

    public UnreadableResponseBody(String contentType, long contentLength) {
      this.contentType = contentType;
      this.contentLength = contentLength;
    }

    @Override public boolean ready() throws IOException {
      throw new IllegalStateException("It is an error to read this response body at this time.");
    }

    @Override public MediaType contentType() {
      return contentType != null ? MediaType.parse(contentType) : null;
    }

    @Override public long contentLength() {
      return contentLength;
    }

    @Override public InputStream byteStream() {
      throw new IllegalStateException("It is an error to read this response body at this time.");
    }
  }
}
