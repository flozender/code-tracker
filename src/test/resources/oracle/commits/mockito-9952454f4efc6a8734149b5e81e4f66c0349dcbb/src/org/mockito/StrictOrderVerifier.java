/*
 * Copyright (c) 2007 Mockito contributors 
 * This program is made available under the terms of the MIT License.
 */
package org.mockito;

import java.util.*;

import org.mockito.exceptions.Exceptions;
import org.mockito.internal.state.OngoingVerifyingMode;

class StrictOrderVerifier implements Strictly {
    
    List<Object> mocksToBeVerifiedInOrder = new LinkedList<Object>();
    
    public <T> T verify(T mock) {
        return this.verify(mock, 1);
    }
    //TODO get rid of interface with int
    public <T> T verify(T mock, int wantedNumberOfInvocations) {
        return this.verify(mock, OngoingVerifyingMode.inOrder(wantedNumberOfInvocations, mocksToBeVerifiedInOrder));
    }
    
    public <T> T verify(T mock, OngoingVerifyingMode ongoingVerifyingMode) {
        if (!mocksToBeVerifiedInOrder.contains(mock)) {
            Exceptions.strictlyRequiresFamiliarMock();
        }
        return Mockito.verify(mock, OngoingVerifyingMode.inOrder(ongoingVerifyingMode.wantedCount(), mocksToBeVerifiedInOrder));
    }

    public void addMockToBeVerifiedInOrder(Object mock) {
        mocksToBeVerifiedInOrder.add(mock);
    }
}
