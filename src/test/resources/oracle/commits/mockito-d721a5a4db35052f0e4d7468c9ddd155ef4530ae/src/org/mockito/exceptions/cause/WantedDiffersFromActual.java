/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.exceptions.cause;

import org.mockito.exceptions.parents.MockitoException;

public class WantedDiffersFromActual extends MockitoException {

    private static final long serialVersionUID = 1L;

    public WantedDiffersFromActual(String message) {
        super(message);
    }
}
