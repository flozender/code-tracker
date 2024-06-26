/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.invocation;

import java.util.List;

import org.hamcrest.Matcher;
import org.mockito.exceptions.Reporter;
import org.mockito.internal.progress.ArgumentMatcherStorage;

@SuppressWarnings("unchecked")
public class MatchersBinder {

    public InvocationMatcher bindMatchers(ArgumentMatcherStorage argumentMatcherStorage, Invocation invocation) {
        List<Matcher> lastMatchers = argumentMatcherStorage.pullMatchers();
        validateMatchers(invocation, lastMatchers);

        InvocationMatcher invocationWithMatchers = new InvocationMatcher(invocation, lastMatchers);
        return invocationWithMatchers;
    }

    private void validateMatchers(Invocation invocation, List<Matcher> matchers) {
        if (matchers != null) {
            int recordedMatchersSize = matchers.size();
            int expectedMatchersSize = invocation.getArguments().length;
            if (expectedMatchersSize != recordedMatchersSize) {
                new Reporter().invalidUseOfMatchers(expectedMatchersSize, recordedMatchersSize);
            }
        }
    }
}