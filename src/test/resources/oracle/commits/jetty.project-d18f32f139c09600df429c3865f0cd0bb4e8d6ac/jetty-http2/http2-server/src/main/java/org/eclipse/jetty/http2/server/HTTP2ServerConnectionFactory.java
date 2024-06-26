//
//  ========================================================================
//  Copyright (c) 1995-2016 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.http2.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.http2.ErrorCode;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.IStream;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.api.server.ServerSessionListener;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.GoAwayFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.http2.frames.PushPromiseFrame;
import org.eclipse.jetty.http2.frames.ResetFrame;
import org.eclipse.jetty.http2.frames.SettingsFrame;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.NegotiatingServerConnection.CipherDiscriminator;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class HTTP2ServerConnectionFactory extends AbstractHTTP2ServerConnectionFactory implements CipherDiscriminator
{
    private static final Logger LOG = Log.getLogger(HTTP2ServerConnectionFactory.class);

    public HTTP2ServerConnectionFactory(@Name("config") HttpConfiguration httpConfiguration)
    {
        super(httpConfiguration);
    }

    public HTTP2ServerConnectionFactory(@Name("config") HttpConfiguration httpConfiguration,String... protocols)
    {
        super(httpConfiguration,protocols);
    }

    @Override
    protected ServerSessionListener newSessionListener(Connector connector, EndPoint endPoint)
    {
        return new HTTPServerSessionListener(connector, endPoint);
    }

    @Override
    public boolean isAcceptable(String protocol, String tlsProtocol, String tlsCipher)
    {
        // Implement 9.2.2 for draft 14
        boolean acceptable = "h2-14".equals(protocol) || !(HTTP2Cipher.isBlackListProtocol(tlsProtocol) && HTTP2Cipher.isBlackListCipher(tlsCipher));
        if (LOG.isDebugEnabled())
            LOG.debug("proto={} tls={} cipher={} 9.2.2-acceptable={}",protocol,tlsProtocol,tlsCipher,acceptable);
        return acceptable;
    }

    protected class HTTPServerSessionListener extends ServerSessionListener.Adapter implements Stream.Listener
    {
        private final Connector connector;
        private final EndPoint endPoint;

        public HTTPServerSessionListener(Connector connector, EndPoint endPoint)
        {
            this.connector = connector;
            this.endPoint = endPoint;
        }

        protected HTTP2ServerConnection getConnection()
        {
            return (HTTP2ServerConnection)endPoint.getConnection();
        }

        @Override
        public Map<Integer, Integer> onPreface(Session session)
        {
            Map<Integer, Integer> settings = new HashMap<>();
            settings.put(SettingsFrame.HEADER_TABLE_SIZE, getMaxDynamicTableSize());
            settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, getInitialStreamRecvWindow());
            int maxConcurrentStreams = getMaxConcurrentStreams();
            if (maxConcurrentStreams >= 0)
                settings.put(SettingsFrame.MAX_CONCURRENT_STREAMS, maxConcurrentStreams);
            settings.put(SettingsFrame.MAX_HEADER_LIST_SIZE, getHttpConfiguration().getRequestHeaderSize());
            return settings;
        }

        @Override
        public Stream.Listener onNewStream(Stream stream, HeadersFrame frame)
        {
            getConnection().onNewStream(connector, (IStream)stream, frame);
            return this;
        }

        @Override
        public boolean onIdleTimeout(Session session)
        {
            boolean close = super.onIdleTimeout(session);
            if (!close)
                return false;

            long idleTimeout = getConnection().getEndPoint().getIdleTimeout();
            return getConnection().onSessionTimeout(new TimeoutException("Session idle timeout " + idleTimeout + " ms"));
        }

        @Override
        public void onClose(Session session, GoAwayFrame frame)
        {
            ErrorCode error = ErrorCode.from(frame.getError());
            if (error == null)
                error = ErrorCode.STREAM_CLOSED_ERROR;
            String reason = frame.tryConvertPayload();
            if (reason != null && !reason.isEmpty())
                reason = " (" + reason + ")";
            getConnection().onSessionFailure(new EofException("HTTP/2 " + error + reason));
        }

        @Override
        public void onFailure(Session session, Throwable failure)
        {
            getConnection().onSessionFailure(failure);
        }

        @Override
        public void onHeaders(Stream stream, HeadersFrame frame)
        {
            // Servers do not receive responses.
            close(stream, "response_headers");
        }

        @Override
        public Stream.Listener onPush(Stream stream, PushPromiseFrame frame)
        {
            // Servers do not receive pushes.
            close(stream, "push_promise");
            return null;
        }

        @Override
        public void onData(Stream stream, DataFrame frame, Callback callback)
        {
            getConnection().onData((IStream)stream, frame, callback);
        }

        @Override
        public void onReset(Stream stream, ResetFrame frame)
        {
            ErrorCode error = ErrorCode.from(frame.getError());
            if (error == null)
                error = ErrorCode.CANCEL_STREAM_ERROR;
            getConnection().onStreamFailure((IStream)stream, new EofException("HTTP/2 " + error));
        }

        @Override
        public boolean onIdleTimeout(Stream stream, Throwable x)
        {
            return getConnection().onStreamTimeout((IStream)stream, x);
        }

        private void close(Stream stream, String reason)
        {
            stream.getSession().close(ErrorCode.PROTOCOL_ERROR.code, reason, Callback.NOOP);
        }
    }
}
