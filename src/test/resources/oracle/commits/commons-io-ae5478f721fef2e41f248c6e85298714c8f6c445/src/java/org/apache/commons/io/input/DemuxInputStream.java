/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
package org.apache.commons.io.input;

import java.io.IOException;
import java.io.InputStream;

/**
 * Data written to this stream is forwarded to a stream that has been associated
 * with this thread.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision: 1.5 $ $Date: 2004/02/23 04:38:52 $
 */
public class DemuxInputStream
    extends InputStream
{
    private InheritableThreadLocal m_streams = new InheritableThreadLocal();

    /**
     * Bind the specified stream to the current thread.
     *
     * @param input the stream to bind
     * @return the InputStream that was previously active
     */
    public InputStream bindStream( InputStream input )
    {
        InputStream oldValue = getStream();
        m_streams.set( input );
        return oldValue;
    }

    /**
     * Closes stream associated with current thread.
     *
     * @throws IOException if an error occurs
     */
    public void close()
        throws IOException
    {
        InputStream input = getStream();
        if( null != input )
        {
            input.close();
        }
    }

    /**
     * Read byte from stream associated with current thread.
     *
     * @return the byte read from stream
     * @throws IOException if an error occurs
     */
    public int read()
        throws IOException
    {
        InputStream input = getStream();
        if( null != input )
        {
            return input.read();
        }
        else
        {
            return -1;
        }
    }

    /**
     * Utility method to retrieve stream bound to current thread (if any).
     */
    private InputStream getStream()
    {
        return (InputStream)m_streams.get();
    }
}
