// ========================================================================
// Copyright (c) 2006-2011 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.server;

import java.io.IOException;

import org.eclipse.jetty.http.HttpException;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.AsyncEndPoint;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.nio.AsyncConnection;
import org.eclipse.jetty.io.nio.SelectChannelEndPoint;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;


/* ------------------------------------------------------------ */
/** Asychronous Server HTTP connection
 *
 */
public class AsyncHttpConnection extends AbstractHttpConnection implements AsyncConnection
{
    private final static int NO_PROGRESS_INFO = Integer.getInteger("org.mortbay.jetty.NO_PROGRESS_INFO",100);
    private final static int NO_PROGRESS_CLOSE = Integer.getInteger("org.mortbay.jetty.NO_PROGRESS_CLOSE",200);

    private static final Logger LOG = Log.getLogger(AsyncHttpConnection.class);
    private int _total_no_progress;
    private final AsyncEndPoint _asyncEndp;

    public AsyncHttpConnection(Connector connector, EndPoint endpoint, Server server)
    {
        super(connector,endpoint,server);
        _asyncEndp=(AsyncEndPoint)endpoint;
    }

    public Connection handle() throws IOException
    {
        Connection connection = this;
        boolean some_progress=false;
        boolean progress=true;

        try
        {
            setCurrentConnection(this);

            // While progress and the connection has not changed
            while (progress && connection==this)
            {
                progress=false;
                try
                {
                    // Handle resumed request
                    if (_request._async.isAsync() && !_request._async.isComplete())
                        handleRequest();
                    // else Parse more input
                    else if (!_parser.isComplete() && _parser.parseAvailable())
                        progress=true;

                    // Generate more output
                    if (_generator.isCommitted() && !_generator.isComplete() && !_endp.isOutputShutdown())
                        if (_generator.flushBuffer()>0)
                            progress=true;

                    // Flush output
                    _endp.flush();

                    // Has any IO been done by the endpoint itself since last loop
                    if (_asyncEndp.hasProgressed())
                        progress=true;

                }
                catch (HttpException e)
                {
                    if (LOG.isDebugEnabled())
                    {
                        LOG.debug("uri="+_uri);
                        LOG.debug("fields="+_requestFields);
                        LOG.debug(e);
                    }
                    progress=true;
                    _generator.sendError(e.getStatus(), e.getReason(), null, true);
                }
                finally
                {
                    some_progress|=progress;
                    //  Is this request/response round complete and are fully flushed?
                    if (_parser.isComplete() && _generator.isComplete())
                    {
                        // Reset the parser/generator
                        progress=true;

                        // look for a switched connection instance?
                        if (_response.getStatus()==HttpStatus.SWITCHING_PROTOCOLS_101)
                        {
                            Connection switched=(Connection)_request.getAttribute("org.eclipse.jetty.io.Connection");
                            if (switched!=null)
                                connection=switched;
                        }

                        reset();

                        // TODO Is this required?
                        if (!_generator.isPersistent() && !_endp.isOutputShutdown())
                        {
                            LOG.warn("Safety net oshut!!!");
                            _endp.shutdownOutput();
                        }
                    }
                }
            }
        }
        finally
        {
            setCurrentConnection(null);
            if (!_request.isAsyncStarted())
            { 
                _parser.returnBuffers();
                _generator.returnBuffers();
            }
            
            // Safety net to catch spinning
            if (some_progress)
                _total_no_progress=0;
            else
            {
                _total_no_progress++;
                if (NO_PROGRESS_INFO>0 && _total_no_progress%NO_PROGRESS_INFO==0 && (NO_PROGRESS_CLOSE<=0 || _total_no_progress< NO_PROGRESS_CLOSE))
                    LOG.info("EndPoint making no progress: "+_total_no_progress+" "+_endp+" "+this);
                if (NO_PROGRESS_CLOSE>0 && _total_no_progress==NO_PROGRESS_CLOSE)
                {
                    LOG.warn("Closing EndPoint making no progress: "+_total_no_progress+" "+_endp+" "+this);
                    if (_endp instanceof SelectChannelEndPoint)
                        ((SelectChannelEndPoint)_endp).getChannel().close();
                }
            }
        }
        return connection;
    }

    public void onInputShutdown() throws IOException
    {
        // If we don't have a committed response and we are not suspended
        if (_generator.isIdle() && !_request.getAsyncContinuation().isSuspended())
        {
            // then no more can happen, so close.
            _endp.shutdownOutput();
        }
    }

}
