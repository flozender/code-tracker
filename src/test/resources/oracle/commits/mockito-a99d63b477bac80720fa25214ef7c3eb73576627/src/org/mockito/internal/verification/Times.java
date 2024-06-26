/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package org.mockito.internal.verification;

import java.util.List;

import org.mockito.exceptions.base.MockitoException;
import org.mockito.internal.invocation.InvocationImpl;
import org.mockito.internal.invocation.InvocationMatcher;
import org.mockito.internal.verification.api.VerificationData;
import org.mockito.internal.verification.api.VerificationDataInOrder;
import org.mockito.internal.verification.api.VerificationInOrderMode;
import org.mockito.internal.verification.checkers.MissingInvocationChecker;
import org.mockito.internal.verification.checkers.MissingInvocationInOrderChecker;
import org.mockito.internal.verification.checkers.NumberOfInvocationsChecker;
import org.mockito.internal.verification.checkers.NumberOfInvocationsInOrderChecker;
import org.mockito.verification.VerificationMode;

public class Times implements VerificationInOrderMode, VerificationMode {
    
    final int wantedCount;
    
    public Times(int wantedNumberOfInvocations) {
        if (wantedNumberOfInvocations < 0) {
            throw new MockitoException("Negative value is not allowed here");
        }
        this.wantedCount = wantedNumberOfInvocations;
    }
    
    public void verify(VerificationData data) {
        if (wantedCount > 0) {
            MissingInvocationChecker missingInvocation = new MissingInvocationChecker();
            missingInvocation.check(data.getAllInvocations(), data.getWanted());
        }
        NumberOfInvocationsChecker numberOfInvocations = new NumberOfInvocationsChecker();
        numberOfInvocations.check(data.getAllInvocations(), data.getWanted(), wantedCount);
    }
    
    public void verifyInOrder(VerificationDataInOrder data) {
        List<InvocationImpl> allInvocations = data.getAllInvocations();
        InvocationMatcher wanted = data.getWanted();
        
        if (wantedCount > 0) {
            MissingInvocationInOrderChecker missingInvocation = new MissingInvocationInOrderChecker();
            missingInvocation.check(allInvocations, wanted, this, data.getOrderingContext());
        }
        NumberOfInvocationsInOrderChecker numberOfCalls = new NumberOfInvocationsInOrderChecker();
        numberOfCalls.check(allInvocations, wanted, wantedCount, data.getOrderingContext());
    }    
    
    @Override
    public String toString() {
        return "Wanted invocations count: " + wantedCount;
    }
}