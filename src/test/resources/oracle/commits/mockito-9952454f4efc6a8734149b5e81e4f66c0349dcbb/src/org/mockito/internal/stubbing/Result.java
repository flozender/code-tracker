/*
 * Copyright (c) 2007 Mockito contributors 
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.stubbing;

import org.mockito.exceptions.parents.HasStackTrace;
import org.mockito.exceptions.parents.StackTraceFilter;

@SuppressWarnings("unchecked")
public class Result implements IAnswer {

    private IAnswer value;

    private Result(IAnswer value) {
        this.value = value;
    }

    public static Result createThrowResult(final Throwable throwable) {
        return new Result(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                StackTraceFilter filter = new StackTraceFilter();
                final Throwable filtered = throwable.fillInStackTrace();
                
                //TODO unit test?
                filter.filterStackTrace(new HasStackTrace() {
                    public StackTraceElement[] getStackTrace() {
                        return filtered.getStackTrace();
                    }
                    public void setStackTrace(StackTraceElement[] stackTrace) {
                        filtered.setStackTrace(stackTrace);
                    }
                });
                
                throw filtered;
            }
        });
    }
    public static Result createReturnResult(final Object value) {

        return new Result(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                return value;
            }
        });
    }

    public static Result createAnswerResult(IAnswer answer) {
        return new Result(answer);
    }

    public Object answer() throws Throwable {
        return value.answer();
    }
}
