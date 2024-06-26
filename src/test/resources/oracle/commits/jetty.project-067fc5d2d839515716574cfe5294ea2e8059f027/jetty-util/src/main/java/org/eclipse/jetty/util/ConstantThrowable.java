//
//  ========================================================================
//  Copyright (c) 1995-2018 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.util;

/**
 * A {@link Throwable} that may be used in static contexts. It uses Java 7
 * constructor that prevents setting stackTrace inside exception object.
 */
public class ConstantThrowable extends Throwable {

    private String name;

    public ConstantThrowable() {
        this(null);
    }

    public ConstantThrowable(String name) {
        super(null, null, false, false);
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
