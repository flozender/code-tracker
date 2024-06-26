//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.spdy.server.proxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpGenerator;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpParser;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.spdy.ISession;
import org.eclipse.jetty.spdy.IStream;
import org.eclipse.jetty.spdy.StandardSession;
import org.eclipse.jetty.spdy.StandardStream;
import org.eclipse.jetty.spdy.api.ByteBufferDataInfo;
import org.eclipse.jetty.spdy.api.DataInfo;
import org.eclipse.jetty.spdy.api.GoAwayInfo;
import org.eclipse.jetty.spdy.api.GoAwayReceivedInfo;
import org.eclipse.jetty.spdy.api.HeadersInfo;
import org.eclipse.jetty.spdy.api.PushInfo;
import org.eclipse.jetty.spdy.api.ReplyInfo;
import org.eclipse.jetty.spdy.api.RstInfo;
import org.eclipse.jetty.spdy.api.SessionStatus;
import org.eclipse.jetty.spdy.api.Stream;
import org.eclipse.jetty.spdy.api.StreamFrameListener;
import org.eclipse.jetty.spdy.api.SynInfo;
import org.eclipse.jetty.spdy.server.http.HTTPSPDYHeader;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.Promise;

public class ProxyHTTPSPDYConnection extends HttpConnection implements HttpParser.RequestHandler<ByteBuffer>
{
    private final short version;
    private final Fields headers = new Fields();
    private final ProxyEngineSelector proxyEngineSelector;
    private final ISession session;
    private HTTPStream stream;
    private ByteBuffer content;

    public ProxyHTTPSPDYConnection(Connector connector, HttpConfiguration config, EndPoint endPoint, short version, ProxyEngineSelector proxyEngineSelector)
    {
        super(config,connector,endPoint);
        this.version = version;
        this.proxyEngineSelector = proxyEngineSelector;
        this.session = new HTTPSession(version, connector);
    }

    @Override
    protected HttpParser.RequestHandler<ByteBuffer> newRequestHandler()
    {
        return this;
    }

    @Override
    public boolean startRequest(HttpMethod method, String methodString, ByteBuffer uri, HttpVersion httpVersion)
    {
        Connector connector = getConnector();
        String scheme = connector.getConnectionFactory(SslConnectionFactory.class) != null ? "https" : "http";
        headers.put(HTTPSPDYHeader.SCHEME.name(version), scheme);
        headers.put(HTTPSPDYHeader.METHOD.name(version), methodString);
        headers.put(HTTPSPDYHeader.URI.name(version), BufferUtil.toUTF8String(uri)); // TODO handle bad encodings
        headers.put(HTTPSPDYHeader.VERSION.name(version), httpVersion.asString());
        return false;
    }

    @Override
    public boolean parsedHeader(HttpField field)
    {
        if (field.getHeader()==HttpHeader.HOST)
            headers.put(HTTPSPDYHeader.HOST.name(version), field.getValue());
        else
            headers.put(field.getName(), field.getValue());
        return false;
    }

    @Override
    public boolean parsedHostHeader(String host, int port)
    {
        return false;
    }

    @Override
    public boolean headerComplete()
    {
        return false;
    }

    @Override
    public boolean content(ByteBuffer item)
    {
        if (content == null)
        {
            stream = syn(false);
            content = item;
        }
        else
        {
            stream.getStreamFrameListener().onData(stream, toDataInfo(item, false));
        }
        return false;
    }

    @Override
    public boolean messageComplete()
    {
        if (stream == null)
        {
            assert content == null;
            if (headers.isEmpty())
                proxyEngineSelector.onGoAway(session, new GoAwayReceivedInfo(0, SessionStatus.OK));
            else
                syn(true);
        }
        else
        {
            stream.getStreamFrameListener().onData(stream, toDataInfo(content, true));
        }
        headers.clear();
        stream = null;
        content = null;
        return false;
    }

    @Override
    public boolean earlyEOF()
    {
        // TODO
        return false;
    }

    @Override
    public void badMessage(int status, String reason)
    {
        // TODO
    }

    private HTTPStream syn(boolean close)
    {
        HTTPStream stream = new HTTPStream(1, (byte)0, session, null);
        StreamFrameListener streamFrameListener = proxyEngineSelector.onSyn(stream, new SynInfo(headers, close));
        stream.setStreamFrameListener(streamFrameListener);
        return stream;
    }

    private DataInfo toDataInfo(ByteBuffer buffer, boolean close)
    {
        return new ByteBufferDataInfo(buffer, close);
    }

    private class HTTPSession extends StandardSession
    {
        private HTTPSession(short version, Connector connector)
        {
            super(version, connector.getByteBufferPool(), connector.getExecutor(), connector.getScheduler(), null,
                    getEndPoint(), null, 1, proxyEngineSelector, null, null);
        }

        @Override
        public void rst(RstInfo rstInfo, Callback handler)
        {
            // Not much we can do in HTTP land: just close the connection
            goAway(new GoAwayInfo(rstInfo.getTimeout(), rstInfo.getUnit()), handler);
        }

        @Override
        public void goAway(GoAwayInfo goAwayInfo, Callback handler)
        {
            getEndPoint().close();
            handler.succeeded();
        }
    }

    /**
     * <p>This stream will convert the SPDY invocations performed by the proxy into HTTP to be sent to the client.</p>
     */
    private class HTTPStream extends StandardStream
    {
        private final Pattern statusRegexp = Pattern.compile("(\\d{3})\\s+(.*)");

        private HTTPStream(int id, byte priority, ISession session, IStream associatedStream)
        {
            super(id, priority, session, associatedStream, null);
        }

        @Override
        public void push(PushInfo pushInfo, Promise<Stream> handler)
        {
            // HTTP does not support pushed streams
            handler.succeeded(new HTTPPushStream(2, getPriority(), getSession(), this));
        }

        @Override
        public void headers(HeadersInfo headersInfo, Callback handler)
        {
            // TODO
            throw new UnsupportedOperationException("Not Yet Implemented");
        }

        @Override
        public void reply(ReplyInfo replyInfo, Callback handler)
        {
            try
            {
                Fields headers = new Fields(replyInfo.getHeaders(), false);

                headers.remove(HTTPSPDYHeader.SCHEME.name(version));

                String status = headers.remove(HTTPSPDYHeader.STATUS.name(version)).value();
                Matcher matcher = statusRegexp.matcher(status);
                matcher.matches();
                int code = Integer.parseInt(matcher.group(1));
                String reason = matcher.group(2).trim();

                HttpVersion httpVersion = HttpVersion.fromString(headers.remove(HTTPSPDYHeader.VERSION.name(version)).value());

                // Convert the Host header from a SPDY special header to a normal header
                Fields.Field host = headers.remove(HTTPSPDYHeader.HOST.name(version));
                if (host != null)
                    headers.put("host", host.value());

                HttpFields fields = new HttpFields();
                for (Fields.Field header : headers)
                {
                    String name = camelize(header.name());
                    fields.put(name, header.value());
                }

                // TODO: handle better the HEAD last parameter
                HttpGenerator.ResponseInfo info = new HttpGenerator.ResponseInfo(httpVersion, fields, -1, code, reason, false);
                send(info, null, replyInfo.isClose());

                if (replyInfo.isClose())
                    completed();

                handler.succeeded();
            }
            catch (IOException x)
            {
                handler.failed(x);
            }
        }

        private String camelize(String name)
        {
            char[] chars = name.toCharArray();
            chars[0] = Character.toUpperCase(chars[0]);

            for (int i = 0; i < chars.length; ++i)
            {
                char c = chars[i];
                int j = i + 1;
                if (c == '-' && j < chars.length)
                    chars[j] = Character.toUpperCase(chars[j]);
            }
            return new String(chars);
        }

        @Override
        public void data(DataInfo dataInfo, Callback handler)
        {
            try
            {
                // Data buffer must be copied, as the ByteBuffer is pooled
                ByteBuffer byteBuffer = dataInfo.asByteBuffer(false);

                send(null, byteBuffer, dataInfo.isClose());

                if (dataInfo.isClose())
                    completed();

                handler.succeeded();
            }
            catch (IOException x)
            {
                handler.failed(x);
            }
        }
    }

    private class HTTPPushStream extends StandardStream
    {
        private HTTPPushStream(int id, byte priority, ISession session, IStream associatedStream)
        {
            super(id, priority, session, associatedStream, null);
        }

        @Override
        public void headers(HeadersInfo headersInfo, Callback handler)
        {
            // Ignore pushed headers
            handler.succeeded();
        }

        @Override
        public void data(DataInfo dataInfo, Callback handler)
        {
            // Ignore pushed data
            handler.succeeded();
        }
    }
}
