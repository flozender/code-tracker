//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.start;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * Interface for initializing a file resource.
 */
public interface FileInitializer
{
    /**
     * Initialize a file resource
     * 
     * @param uri
     *            the remote URI of the resource acting as its source
     * @param file
     *            the local file resource to initialize
     * @return true if local file is initialized, false if this
     *         {@link FileInitializer} skipped this attempt.
     * @throws IOException
     *             if there was an attempt to initialize, but an error occurred.
     */
    public boolean init(URI uri, Path file) throws IOException;
}
