/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package org.mockito.internal.progress;

import org.mockito.MockSettings;
import org.mockito.internal.invocation.InvocationImpl;
import org.mockito.internal.listeners.MockingProgressListener;
import org.mockito.verification.VerificationMode;

@SuppressWarnings("unchecked")
public interface MockingProgress {
    
    void reportOngoingStubbing(IOngoingStubbing iOngoingStubbing);

    IOngoingStubbing pullOngoingStubbing();

    void verificationStarted(VerificationMode verificationMode);

    VerificationMode pullVerificationMode();

    void stubbingStarted();

    void stubbingCompleted(InvocationImpl invocation);
    
    void validateState();

    void reset();

    /**
     * Removes ongoing stubbing so that in case the framework is misused
     * state validation errors are more accurate
     */
    void resetOngoingStubbing();

    ArgumentMatcherStorage getArgumentMatcherStorage();
    
    void mockingStarted(Object mock, Class classToMock, MockSettings mockSettings);

    void setListener(MockingProgressListener listener);
}