//
//  ========================================================================
//  Copyright (c) 1995-2012 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.client;


import java.io.IOException;



/**
 * 
 * 
 * 
 */
public interface HttpEventListener
{

    // TODO review the methods here, we can probably trim these down on what to expose
    
    public void onRequestCommitted() throws IOException;


    public void onRequestComplete() throws IOException;


    public void onResponseStatus(ByteBuffer version, int status, ByteBuffer reason) throws IOException;


    public void onResponseHeader(ByteBuffer name, ByteBuffer value) throws IOException;

    
    public void onResponseHeaderComplete() throws IOException;

    
    public void onResponseContent(ByteBuffer content) throws IOException;


    public void onResponseComplete() throws IOException;


    public void onConnectionFailed(Throwable ex);


    public void onException(Throwable ex);


    public void onExpire();
    
    public void onRetry();
    

}
