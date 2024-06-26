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
 * <p>Exception thrown when the Serialization process fails.</p>
 *
 * <p>The original error is wrapped within this one.</p>
 *
 * @author Stephen Colebourne
 * @since 1.0
 * @version $Id: SerializationException.java,v 1.7 2004/02/18 22:59:50 ggregory Exp $
 */
public class SerializationException extends NestableRuntimeException {

    /**
     * <p>Constructs a new <code>SerializationException</code> without specified
     * detail message.</p>
     */
    public SerializationException() {
        super();
    }

    /**
     * <p>Constructs a new <code>SerializationException</code> with specified
     * detail message.</p>
     *
     * @param msg  The error message.
     */
    public SerializationException(String msg) {
        super(msg);
    }

    /**
     * <p>Constructs a new <code>SerializationException</code> with specified
     * nested <code>Throwable</code>.</p>
     *
     * @param cause  The <code>Exception</code> or <code>Error</code>
     *  that caused this exception to be thrown.
     */
    public SerializationException(Throwable cause) {
        super(cause);
    }

    /**
     * <p>Constructs a new <code>SerializationException</code> with specified
     * detail message and nested <code>Throwable</code>.</p>
     *
     * @param msg    The error message.
     * @param cause  The <code>Exception</code> or <code>Error</code>
     *  that caused this exception to be thrown.
     */
    public SerializationException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
