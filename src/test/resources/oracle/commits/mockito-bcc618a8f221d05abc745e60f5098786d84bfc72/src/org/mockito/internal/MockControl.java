/*
 * Copyright (c) 2007 Mockito contributors 
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.mockito.exceptions.Reporter;
import org.mockito.internal.creation.MockAwareInvocationHandler;
import org.mockito.internal.invocation.Invocation;
import org.mockito.internal.invocation.InvocationMatcher;
import org.mockito.internal.invocation.InvocationsChunker;
import org.mockito.internal.invocation.InvocationsMarker;
import org.mockito.internal.invocation.MatchersBinder;
import org.mockito.internal.progress.MockingProgress;
import org.mockito.internal.progress.OngoingStubbing;
import org.mockito.internal.progress.VerificationMode;
import org.mockito.internal.stubbing.EmptyReturnValues;
import org.mockito.internal.stubbing.StubbedMethodSelector;
import org.mockito.internal.stubbing.Stubber;
import org.mockito.internal.stubbing.VoidMethodStubable;
import org.mockito.internal.verification.MissingInvocationVerifier;
import org.mockito.internal.verification.NumberOfInvocationsVerifier;
import org.mockito.internal.verification.Verifier;
import org.mockito.internal.verification.VerifyingRecorder;

public class MockControl<T> implements MockAwareInvocationHandler<T>, OngoingStubbing<T>, VoidMethodStubable<T>, StubbedMethodSelector<T> {

    private final VerifyingRecorder verifyingRecorder;
    private final Stubber stubber;
    private final MatchersBinder matchersBinder;
    private final MockingProgress mockingProgress;
    
    private T mock;
    
    public MockControl(MockingProgress mockingProgress, MatchersBinder matchersBinder) {
        this.mockingProgress = mockingProgress;
        this.matchersBinder = matchersBinder;
        stubber = new Stubber(mockingProgress);
        
        verifyingRecorder = createRecorder(); 
    }

    private VerifyingRecorder createRecorder() {
        InvocationsChunker chunker = new InvocationsChunker(new AllInvocationsFinder());
        InvocationsMarker marker = new InvocationsMarker();
        List<Verifier> verifiers = Arrays.asList(new MissingInvocationVerifier(), new NumberOfInvocationsVerifier(new Reporter()));
        return new VerifyingRecorder(chunker, marker, verifiers);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (stubber.hasThrowableForVoidMethod()) {
            Invocation invocation = new Invocation(proxy, method, args, mockingProgress.nextSequenceNumber());
            InvocationMatcher invocationMatcher = matchersBinder.bindMatchers(invocation);
            stubber.addVoidMethodForThrowable(invocationMatcher);
            return null;
        }
        
        VerificationMode verificationMode = mockingProgress.pullVerificationMode();
        mockingProgress.validateState();
        
        Invocation invocation = new Invocation(proxy, method, args, mockingProgress.nextSequenceNumber());
        InvocationMatcher invocationMatcher = matchersBinder.bindMatchers(invocation);
        
        if (verificationMode != null) {
            verifyingRecorder.verify(invocationMatcher, verificationMode);
            return EmptyReturnValues.emptyValueFor(method.getReturnType());
        } 
        
        stubber.setInvocationForPotentialStubbing(invocationMatcher);
        verifyingRecorder.recordInvocation(invocationMatcher.getInvocation());

        mockingProgress.reportStubable(this);
        
        return stubber.resultFor(invocationMatcher.getInvocation());
    }

    public void verifyNoMoreInteractions() {
        verifyingRecorder.verifyNoMoreInteractions();
    }
    
    public void verifyZeroInteractions() {
        verifyingRecorder.verifyZeroInteractions();
    }

    public void andReturn(T value) {
        verifyingRecorder.eraseLastInvocation();
        stubber.addReturnValue(value);
    }

    public void andThrow(Throwable throwable) {
        verifyingRecorder.eraseLastInvocation();
        stubber.addThrowable(throwable);
    }
    
    public StubbedMethodSelector<T> toThrow(Throwable throwable) {
        stubber.addThrowableForVoidMethod(throwable);
        return this;
    }

    public T on() {
        return mock;
    }

    public void setMock(T mock) {
        this.mock = mock;
    }

    public List<Invocation> getRegisteredInvocations() {
        return verifyingRecorder.getRegisteredInvocations();
    }
}