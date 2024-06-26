/*
 * Copyright 2002-2005 The Apache Software Foundation.
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
package org.apache.commons.io.output;

import java.io.IOException;
import java.io.FilterOutputStream;
import java.io.OutputStream;

/**
 * A Proxy stream which acts as expected, that is it passes the method 
 * calls on to the proxied stream and doesn't change which methods are 
 * being called. It is an alternative base class to FilterOutputStream
 * to increase reusability.
 * 
 * @author Henri Yandell
 * @author Stephen Colebourne
 * @version $Id$
 */
public class ProxyOutputStream extends FilterOutputStream {

    /**
     * Constructs a new ProxyOutputStream.
     * 
     * @param proxy  the OutputStream to delegate to
     */
    public ProxyOutputStream(OutputStream proxy) {
        super(proxy);
        // the proxy is stored in a protected superclass variable named 'out'
    }

    /** @see java.io.OutputStream#write(int) */
    public void write(int idx) throws IOException {
        out.write(idx);
    }

    /** @see java.io.OutputStream#write(byte[]) */
    public void write(byte[] bts) throws IOException {
        out.write(bts);
    }

    /** @see java.io.OutputStream#write(byte[], int, int) */
    public void write(byte[] bts, int st, int end) throws IOException {
        out.write(bts, st, end);
    }

    /** @see java.io.OutputStream#flush() */
    public void flush() throws IOException {
        out.flush();
    }

    /** @see java.io.OutputStream#close() */
    public void close() throws IOException {
        out.close();
    }

}
