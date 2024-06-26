/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package org.mockito.exceptions;

import org.mockito.internal.debugging.LocationImpl;

public interface PrintableInvocation {
    
    String toString();
    
    LocationImpl getLocation();
    
}