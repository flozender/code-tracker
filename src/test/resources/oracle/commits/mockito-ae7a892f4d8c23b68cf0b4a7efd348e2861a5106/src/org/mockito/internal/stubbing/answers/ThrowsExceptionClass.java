package org.mockito.internal.stubbing.answers;

import org.mockito.internal.exceptions.base.ConditionalStackTraceFilter;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.objenesis.ObjenesisHelper;

import java.io.Serializable;

public class ThrowsExceptionClass implements Answer<Object>, Serializable {

    private Class<? extends Throwable> throwableClass;
    private final ConditionalStackTraceFilter filter = new ConditionalStackTraceFilter();

    public ThrowsExceptionClass(Class<? extends Throwable> throwableClass) {

        this.throwableClass = throwableClass;
    }

    public Object answer(InvocationOnMock invocation) throws Throwable {

        Throwable throwable = (Throwable) ObjenesisHelper.newInstance(throwableClass);
        throwable.fillInStackTrace();
        filter.filter(throwable);
        throw throwable;
    }

    public Class<? extends Throwable> getThrowableClass() {
        return throwableClass;
    }
}
