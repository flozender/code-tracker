// ========================================================================
// Copyright (c) 2008-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses. 
// ========================================================================

package org.eclipse.jetty.security.authentication;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.security.B64Code;
import org.eclipse.jetty.http.security.Constraint;
import org.eclipse.jetty.security.Authentication;
import org.eclipse.jetty.security.DefaultAuthentication;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.server.UserIdentity;

/**
 * @version $Rev: 4793 $ $Date: 2009-03-19 00:00:01 +0100 (Thu, 19 Mar 2009) $
 */
public class ClientCertAuthenticator extends LoginAuthenticator
{
    public ClientCertAuthenticator()
    {
        super();
    }

    public String getAuthMethod()
    {
        return Constraint.__CERT_AUTH;
    }
    
    /**
     * TODO what should happen if an insecure page is accessed without a client
     * cert? Current code requires a client cert always but allows access to
     * insecure pages if it is not recognized.
     * 
     * @return
     * @throws ServerAuthException
     */
    public Authentication validateRequest(ServletRequest req, ServletResponse res, boolean mandatory) throws ServerAuthException
    {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;
        java.security.cert.X509Certificate[] certs = (java.security.cert.X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

        try
        {
            // Need certificates.
            if (certs == null || certs.length == 0 || certs[0] == null)
            {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                                   "A client certificate is required for accessing this web application but the server's listener is not configured for mutual authentication (or the client did not provide a certificate).");
                return DefaultAuthentication.SEND_FAILURE_RESULTS;
            }
            
            Principal principal = certs[0].getSubjectDN();
            if (principal == null) principal = certs[0].getIssuerDN();
            final String username = principal == null ? "clientcert" : principal.getName();
            
            // TODO no idea if this is correct
            final char[] credential = B64Code.encode(certs[0].getSignature());

            UserIdentity user = _loginService.login(username,credential);
            if (user!=null)
                return new DefaultAuthentication(Authentication.Status.SUCCESS,Constraint.__CERT_AUTH2,user);

            if (!mandatory) 
            { 
                return DefaultAuthentication.SUCCESS_UNAUTH_RESULTS; 
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "The provided client certificate does not correspond to a trusted user.");
            return DefaultAuthentication.SEND_FAILURE_RESULTS;
        }
        catch (IOException e)
        {
            throw new ServerAuthException(e.getMessage());
        }
    }

    public Authentication.Status secureResponse(ServletRequest req, ServletResponse res, boolean mandatory, Authentication validatedUser) throws ServerAuthException
    {
        return Authentication.Status.SUCCESS;
    }
}
