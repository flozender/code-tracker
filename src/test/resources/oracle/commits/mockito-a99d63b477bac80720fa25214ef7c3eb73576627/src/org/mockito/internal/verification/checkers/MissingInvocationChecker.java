/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package org.mockito.internal.verification.checkers;

import java.util.List;

import org.mockito.exceptions.Reporter;
import org.mockito.internal.invocation.InvocationImpl;
import org.mockito.internal.invocation.InvocationMatcher;
import org.mockito.internal.invocation.InvocationsFinder;
import org.mockito.internal.reporting.SmartPrinter;
import org.mockito.internal.verification.argumentmatching.ArgumentMatchingTool;

public class MissingInvocationChecker {
    
    private final Reporter reporter;
    private final InvocationsFinder finder;
    
    public MissingInvocationChecker() {
        this(new InvocationsFinder(), new Reporter());
    }
    
    MissingInvocationChecker(InvocationsFinder finder, Reporter reporter) {
        this.finder = finder;
        this.reporter = reporter;
    }
    
    public void check(List<InvocationImpl> invocations, InvocationMatcher wanted) {
        List<InvocationImpl> actualInvocations = finder.findInvocations(invocations, wanted);
        
        if (actualInvocations.isEmpty()) {
            InvocationImpl similar = finder.findSimilarInvocation(invocations, wanted);
            if (similar != null) {
                ArgumentMatchingTool argumentMatchingTool = new ArgumentMatchingTool();
                Integer[] indexesOfSuspiciousArgs = argumentMatchingTool.getSuspiciouslyNotMatchingArgsIndexes(wanted.getMatchers(), similar.getArguments());
                SmartPrinter smartPrinter = new SmartPrinter(wanted, similar, indexesOfSuspiciousArgs);
                reporter.argumentsAreDifferent(smartPrinter.getWanted(), smartPrinter.getActual(), similar.getLocation());
            } else {
                reporter.wantedButNotInvoked(wanted, invocations);
            }
        }
    }
}