/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.verification;

import static java.util.Arrays.*;
import static org.mockito.internal.progress.VerificationModeImpl.*;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.TestBase;
import org.mockito.exceptions.PrintableInvocation;
import org.mockito.exceptions.Reporter;
import org.mockito.exceptions.base.HasStackTrace;
import org.mockito.internal.invocation.Invocation;
import org.mockito.internal.invocation.InvocationBuilder;
import org.mockito.internal.invocation.InvocationMatcher;
import org.mockito.internal.progress.VerificationModeBuilder;
import org.mockito.internal.progress.VerificationModeImpl;

public class NumberOfInvocationsVerifierTest extends TestBase {

    private NumberOfInvocationsVerifier verifier;
    private ReporterStub reporterStub;
    private InvocationMatcher wanted;
    private LinkedList<Invocation> invocations;
    private InvocationsFinderStub finderStub;
    
    @Before
    public void setup() {
        reporterStub = new ReporterStub();
        finderStub = new InvocationsFinderStub();
        verifier = new NumberOfInvocationsVerifier(reporterStub, finderStub);
        
        wanted = new InvocationBuilder().toInvocationMatcher();
        invocations = new LinkedList<Invocation>(asList(new InvocationBuilder().toInvocation()));
    }

    @Test
    public void shouldNeverVerifyWhenNotModeIsInOrder() throws Exception {
        verifier.verify(null, null, new VerificationModeBuilder().inOrder());
    }
    
    @Test
    public void shouldReportTooLittleActual() throws Exception {
        VerificationModeImpl mode = times(100);
        finderStub.actualToReturn.add(new InvocationBuilder().toInvocation());
        
        verifier.verify(invocations, wanted, mode);
        
        assertEquals(1, reporterStub.actualCount);
        assertEquals(100, reporterStub.wantedCount);
        assertEquals(wanted, reporterStub.wanted);
    }

    @Test
    public void shouldReportWithLastInvocationStackTrace() throws Exception {
        VerificationModeImpl mode = times(100);
        Invocation first = new InvocationBuilder().toInvocation();
        Invocation second = new InvocationBuilder().toInvocation();
        
        finderStub.actualToReturn.addAll(asList(first, second));
        
        verifier.verify(invocations, wanted, mode);
        
        assertSame(second.getStackTrace(), reporterStub.stackTrace);
    }
    
    @Test
    public void shouldNotReportWithLastInvocationStackTraceIfNoInvocationsFound() throws Exception {
        VerificationModeImpl mode = times(100);
        
        assertTrue(finderStub.actualToReturn.isEmpty());
        
        verifier.verify(invocations, wanted, mode);
        
        assertNull(reporterStub.stackTrace);
    }
    
    @Test
    public void shouldReportWithFirstUndesiredInvocationStackTrace() throws Exception {
        VerificationModeImpl mode = times(2);

        Invocation first = new InvocationBuilder().toInvocation();
        Invocation second = new InvocationBuilder().toInvocation();
        Invocation third = new InvocationBuilder().toInvocation();
        
        finderStub.actualToReturn.addAll(asList(first, second, third));
        
        verifier.verify(invocations, wanted, mode);
        
        assertSame(third.getStackTrace(), reporterStub.stackTrace);
    }
    
    @Test
    public void shouldReportTooManyActual() throws Exception {
        VerificationModeImpl mode = times(1);
        finderStub.actualToReturn.add(new InvocationBuilder().toInvocation());
        finderStub.actualToReturn.add(new InvocationBuilder().toInvocation());
        
        verifier.verify(invocations, wanted, mode);
        
        assertEquals(2, reporterStub.actualCount);
        assertEquals(1, reporterStub.wantedCount);
        assertEquals(wanted, reporterStub.wanted);
    }
    
    @Test
    public void shouldReportNeverWantedButInvoked() throws Exception {
        VerificationModeImpl mode = times(0);
        Invocation invocation = new InvocationBuilder().toInvocation();
        finderStub.actualToReturn.add(invocation);
        
        verifier.verify(invocations, wanted, mode);
        
        assertEquals(wanted, reporterStub.wanted);
        assertEquals(invocation.getStackTrace(), reporterStub.stackTrace);
    }
    
    @Test
    public void shouldMarkInvocationsAsVerified() throws Exception {
        Invocation invocation = new InvocationBuilder().toInvocation();
        finderStub.actualToReturn.add(invocation);
        assertFalse(invocation.isVerified());
        
        verifier.verify(invocations, wanted, atLeastOnce());
        
        assertTrue(invocation.isVerified());
    }
    
    class ReporterStub extends Reporter {
        private int wantedCount;
        private int actualCount;
        private PrintableInvocation wanted;
        private HasStackTrace stackTrace;
        @Override public void tooLittleActualInvocations(int wantedCount, int actualCount, PrintableInvocation wanted, HasStackTrace lastActualInvocationStackTrace) {
                    this.wantedCount = wantedCount;
                    this.actualCount = actualCount;
                    this.wanted = wanted;
                    this.stackTrace = lastActualInvocationStackTrace;
        }
        
        @Override public void tooManyActualInvocations(int wantedCount, int actualCount, PrintableInvocation wanted, HasStackTrace firstUndesired) {
                    this.wantedCount = wantedCount;
                    this.actualCount = actualCount;
                    this.wanted = wanted;
                    this.stackTrace = firstUndesired;
        }
        
        @Override
        public void neverWantedButInvoked(PrintableInvocation wanted, HasStackTrace firstUndesired) {
            this.wanted = wanted;
            this.stackTrace = firstUndesired;
        }
    }
}
