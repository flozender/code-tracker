/*
 * Copyright (c) 2007 Mockito contributors 
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal;

public class SequenceNumberProvider {

    private int sequence = 1;
    
    public Integer sequenceNumber() {
        return sequence++;
    }
}
