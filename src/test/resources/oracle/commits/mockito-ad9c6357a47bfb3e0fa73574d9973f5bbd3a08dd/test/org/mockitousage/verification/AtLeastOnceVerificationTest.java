/*
 * Copyright (c) 2007 Mockito contributors 
 * This program is made available under the terms of the MIT License.
 */
package org.mockitousage.verification;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.*;
import org.mockito.*;
import org.mockito.exceptions.verification.VerificationError;

@SuppressWarnings("unchecked")
public class AtLeastOnceVerificationTest extends RequiresValidState {

    private List mock;
    private List mockTwo;
    
    @Before public void setup() {
        mock = Mockito.mock(List.class);
        mockTwo = Mockito.mock(List.class);
    }

    @Test
    public void shouldVerifyAtLeastOnce() throws Exception {
        mock.clear();
        mock.clear();
        
        mockTwo.add("add");

        verify(mock, atLeastOnce()).clear();
        verify(mockTwo, atLeastOnce()).add("add");
        try {
            verify(mockTwo, atLeastOnce()).add("foo");
            fail();
        } catch (VerificationError e) {}
    }
    
    @Test(expected=VerificationError.class)
    public void shouldFailIfMethodWasNotCalledAtAll() throws Exception {
        verify(mock, atLeastOnce()).add("foo");
    }
}
