/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package org.mockito.internal.verification;

import org.mockito.verification.VerificationMode;

public class VerificationModeFactory {
    
    public static VerificationMode atLeastOnce() {
        return atLeast(1);
    }

    public static VerificationMode atLeast(int minNumberOfInvocations) {
        return new AtLeast(minNumberOfInvocations);
    }

    public static VerificationMode only() {
        return new Only();
    }

    public static Times times(int wantedNumberOfInvocations) {
        return new Times(wantedNumberOfInvocations);
    }

    public static TimesNow timesNow( int wantedNumberOfInvocations ) {
        return new TimesNow( wantedNumberOfInvocations );
    }

    public static NoMoreInteractions noMoreInteractions() {
        return new NoMoreInteractions();
    }

    public static VerificationMode atMost(int maxNumberOfInvocations) {
        return new AtMost(maxNumberOfInvocations);
    }
}