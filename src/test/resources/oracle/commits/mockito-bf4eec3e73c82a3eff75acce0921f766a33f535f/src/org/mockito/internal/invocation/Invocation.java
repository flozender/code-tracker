/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.invocation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.mockito.exceptions.Printable;
import org.mockito.exceptions.base.HasStackTrace;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.internal.creation.MockNamer;
import org.mockito.internal.matchers.ArrayEquals;
import org.mockito.internal.matchers.Equals;

/**
 * Method call on a mock object. 
 * <p>
 * Contains sequence number which should be
 * globally unique and is used for verification in order.
 * <p>
 * Contains stack trace of invocation
 */
@SuppressWarnings("unchecked")
public class Invocation implements Printable {

    private final int sequenceNumber;
    private final Object mock;
    private final Method method;
    private final Object[] arguments;
    private final HasStackTrace stackTrace;

    private boolean verified;
    private boolean verifiedInOrder;

    public Invocation(Object mock, Method method, Object[] args, int sequenceNumber) {
        this.mock = mock;
        this.method = method;
        this.arguments = expandVarArgs(method.isVarArgs(), args);
        this.sequenceNumber = sequenceNumber;
        this.stackTrace = new MockitoException("");
    }

    private static Object[] expandVarArgs(final boolean isVarArgs,
            final Object[] args) {
        if (!isVarArgs || isVarArgs && args[args.length - 1] != null
                && !args[args.length - 1].getClass().isArray()) {
            return args == null ? new Object[0] : args;
        }
        Object[] varArgs = ArrayEquals.createObjectArray(args[args.length - 1]);
        final int nonVarArgsCount = args.length - 1;
        final int varArgsCount = varArgs.length;
        Object[] newArgs = new Object[nonVarArgsCount + varArgsCount];
        System.arraycopy(args, 0, newArgs, 0, nonVarArgsCount);
        System.arraycopy(varArgs, 0, newArgs, nonVarArgsCount, varArgsCount);
        return newArgs;
    }

    public Object getMock() {
        return mock;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public void markVerified() {
        verified = true;
    }

    public boolean isVerified() {
        return verified;
    }
    
    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void markVerifiedInOrder() {
        this.markVerified();
        this.verifiedInOrder = true;
    }

    public boolean isVerifiedInOrder() {
        return verifiedInOrder;
    }
    
    public HasStackTrace getStackTrace() {
        return stackTrace;
    }

    public boolean equals(Object o) {
        if (o == null || !o.getClass().equals(this.getClass())) {
            return false;
        }

        Invocation other = (Invocation) o;

        return this.mock.equals(other.mock) && this.method.equals(other.method)
                && this.equalArguments(other.arguments);
    }

    private boolean equalArguments(Object[] arguments) {
        return Arrays.equals(arguments, this.arguments);
    }

    public int hashCode() {
        throw new RuntimeException("hashCode() is not implemented");
    }
    
    public String toString() {
        return toString(argumentsToMatchers());
    }

    public String toString(List<Matcher> matchers) {
        String mockName = MockNamer.nameForMock(mock);
        String methodName = method.getName();
        String arguments = getArgumentsString(matchers);
        
        return mockName + "." + methodName + arguments;
    }

    public String toStringWithArgumentTypes() {
        StringBuilder result = new StringBuilder();
        result.append((MockNamer.nameForMock(mock) + "." + method.getName()));
        result.append("(");
        for (Class<?> paramType : getMethod().getParameterTypes()) {
            result.append(paramType);
            result.append(", ");
        } 
        return result.toString().replaceFirst(", $", "").concat(")");
    }
    
    private String getArgumentsString(List<Matcher> matchers) {
        Description result = new StringDescription();
        result.appendList("(", ", ", ")", matchers);
        return result.toString();
    }
    
    private List<Matcher> argumentsToMatchers() {
        List<Matcher> matchers = new LinkedList<Matcher>();
        for (Object arg : this.arguments) {
            if (arg != null && arg.getClass().isArray()) {
                matchers.add(new ArrayEquals(arg));
            } else {
                matchers.add(new Equals(arg));
            }
        }
        return matchers;
    }
}