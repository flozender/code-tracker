/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.verification.checkers;

import java.util.List;

import org.mockito.exceptions.Discrepancy;
import org.mockito.exceptions.Reporter;
import org.mockito.internal.debugging.Location;
import org.mockito.internal.invocation.Invocation;
import org.mockito.internal.invocation.InvocationMatcher;
import org.mockito.internal.invocation.InvocationsFinder;

public class NumberOfInvocationsInOrderChecker {
    
    private final Reporter reporter;
    private final InvocationsFinder finder;
    
    public NumberOfInvocationsInOrderChecker() {
        this(new InvocationsFinder(), new Reporter());
    }
    
    NumberOfInvocationsInOrderChecker(InvocationsFinder finder, Reporter reporter) {
        this.finder = finder;
        this.reporter = reporter;
    }
    
    public void check(List<Invocation> invocations, InvocationMatcher wanted, int wantedCount) {
        List<Invocation> chunk = finder.findMatchingChunk(invocations, wanted, wantedCount);
        
        int actualCount = chunk.size();
        
        if (wantedCount > actualCount) {
            Location lastInvocation = finder.getLastStackTrace(chunk);
            reporter.tooLittleActualInvocationsInOrder(new Discrepancy(wantedCount, actualCount), wanted, lastInvocation);
        } else if (wantedCount < actualCount) {
            Location firstUndesired = chunk.get(wantedCount).getLocation();
            reporter.tooManyActualInvocationsInOrder(wantedCount, actualCount, wanted, firstUndesired);
        }
        
        for (Invocation i : chunk) {
            i.markVerifiedInOrder();
        }
    }
}