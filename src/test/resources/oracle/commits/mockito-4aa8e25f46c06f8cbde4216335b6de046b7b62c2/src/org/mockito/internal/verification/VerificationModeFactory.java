/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.verification;

import org.mockito.internal.verification.MockitoVerificationMode.Verification;
import org.mockito.internal.verification.api.VerificationMode;

/**
 * Holds additional information regarding verification.
 * <p> 
 * Implements marking interface which hides details from Mockito users. 
 */
public class VerificationModeFactory {
    
    public static MockitoVerificationMode atLeastOnce() {
        return atLeast(1);
    }

    public static MockitoVerificationMode atLeast(int minNumberOfInvocations) {
        return new MockitoVerificationMode(minNumberOfInvocations, Verification.AT_LEAST);
    }

    public static MockitoVerificationMode times(int wantedNumberOfInvocations) {
        return new MockitoVerificationMode(wantedNumberOfInvocations, Verification.EXPLICIT);
    }

    public static NoMoreInteractionsMode noMoreInteractions() {
        return new NoMoreInteractionsMode();
    }

    public static VerificationMode atMost(int maxNumberOfInvocations) {
        return new AtMostXVerificationMode(maxNumberOfInvocations);
    }
}