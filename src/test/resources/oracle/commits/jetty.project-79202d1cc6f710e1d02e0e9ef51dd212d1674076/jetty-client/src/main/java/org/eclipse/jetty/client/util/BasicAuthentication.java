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

package org.eclipse.jetty.client.util;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.Attributes;
import org.eclipse.jetty.util.B64Code;

/**
 * Implementation of the HTTP "Basic" authentication defined in RFC 2617.
 * <p />
 * Applications should create objects of this class and add them to the
 * {@link AuthenticationStore} retrieved from the {@link HttpClient}
 * via {@link HttpClient#getAuthenticationStore()}.
 */
public class BasicAuthentication extends AbstractAuthentication
{
    private final String user;
    private final String password;

    /**
     * @param uri the URI to match for the authentication
     * @param realm the realm to match for the authentication
     * @param user the user that wants to authenticate
     * @param password the password of the user
     */
    public BasicAuthentication(URI uri, String realm, String user, String password)
    {
        super(uri, realm);
        this.user = user;
        this.password = password;
    }

    @Override
    public String getType()
    {
        return "Basic";
    }

    @Override
    public Result authenticate(Request request, ContentResponse response, HeaderInfo headerInfo, Attributes context)
    {
        String value = "Basic " + B64Code.encode(user + ":" + password, StandardCharsets.ISO_8859_1);
        return new BasicResult(headerInfo.getHeader(), value);
    }

    private class BasicResult implements Result
    {
        private final HttpHeader header;
        private final String value;

        public BasicResult(HttpHeader header, String value)
        {
            this.header = header;
            this.value = value;
        }

        @Override
        public URI getURI()
        {
            return BasicAuthentication.this.getURI();
        }

        @Override
        public void apply(Request request)
        {
            request.header(header, value);
        }

        @Override
        public String toString()
        {
            return String.format("Basic authentication result for %s", getURI());
        }
    }
}
