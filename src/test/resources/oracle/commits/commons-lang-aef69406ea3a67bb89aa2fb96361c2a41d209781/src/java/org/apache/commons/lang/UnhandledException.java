/*
 * Copyright 2002-2004 The Apache Software Foundation.
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
package org.apache.commons.lang;

import org.apache.commons.lang.exception.NestableRuntimeException;

/**
 * <p>Thrown when it is impossible or undesirable to consume or throw a checked exception.</p>
 * This exception supplements the standard exception classes by providing a more
 * semantically rich description of the problem.</p>
 * 
 * <p><code>UnhandledException</code> represents the case where a method has to deal
 * with a checked exception but does not wish to.
 * Instead, the checked exception is rethrown in this unchecked wrapper.</p>
 * 
 * <pre>
 * public void foo() {
 *   try {
 *     // do something that throws IOException
 *   } catch (IOException ex) {
 *     // don't want to or can't throw IOException from foo()
 *     throw new UnhandledException(ex);
 *   }
 * }
 * </pre>
 *
 * @author Matthew Hawthorne
 * @since 2.0
 * @version $Id: UnhandledException.java,v 1.6 2004/10/15 20:55:01 scolebourne Exp $
 */
public class UnhandledException extends NestableRuntimeException {

    /**
     * Constructs the exception using a cause.
     *
     * @param cause  the underlying cause
     */
    public UnhandledException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs the exception using a message and cause.
     *
     * @param message  the message to use
     * @param cause  the underlying cause
     */
    public UnhandledException(String message, Throwable cause) {
        super(message, cause);
    }

}
