/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package org.mockito.internal.invocation;

import static org.mockitoutil.ExtraMatchers.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.debugging.LocationImpl;
import org.mockito.internal.verification.InOrderContextImpl;
import org.mockito.internal.verification.api.InOrderContext;
import org.mockitousage.IMethods;
import org.mockitoutil.TestBase;


public class InvocationsFinderTest extends TestBase {
    
    private LinkedList<InvocationImpl> invocations = new LinkedList<InvocationImpl>();
    private InvocationImpl simpleMethodInvocation;
    private InvocationImpl simpleMethodInvocationTwo;
    private InvocationImpl differentMethodInvocation;
    private InvocationsFinder finder;
    InOrderContext context = new InOrderContextImpl();
    
    @Mock private IMethods mock;

    @Before
    public void setup() throws Exception {
        simpleMethodInvocation = new InvocationBuilder().mock(mock).simpleMethod().seq(1).toInvocation();
        simpleMethodInvocationTwo = new InvocationBuilder().mock(mock).simpleMethod().seq(2).toInvocation();
        differentMethodInvocation = new InvocationBuilder().mock(mock).differentMethod().seq(3).toInvocation();
        invocations.addAll(Arrays.asList(simpleMethodInvocation, simpleMethodInvocationTwo, differentMethodInvocation));
        finder = new InvocationsFinder();
    }

    @Test
    public void shouldFindActualInvocations() throws Exception {
        List<InvocationImpl> actual = finder.findInvocations(invocations, new InvocationMatcher(simpleMethodInvocation));
        assertThat(actual, hasExactlyInOrder(simpleMethodInvocation, simpleMethodInvocationTwo));
        
        actual = finder.findInvocations(invocations, new InvocationMatcher(differentMethodInvocation));
        assertThat(actual, hasExactlyInOrder(differentMethodInvocation));
    }
    
    @Test
    public void shouldFindFirstUnverifiedInvocation() throws Exception {
        assertSame(simpleMethodInvocation, finder.findFirstUnverified(invocations));
        
        simpleMethodInvocationTwo.markVerified();
        simpleMethodInvocation.markVerified();
        
        assertSame(differentMethodInvocation, finder.findFirstUnverified(invocations));
        
        differentMethodInvocation.markVerified();
        assertNull(finder.findFirstUnverified(invocations));
    }
    
    @Test
    public void shouldFindFirstUnverifiedInOrder() throws Exception {
        //given
        InOrderContextImpl context = new InOrderContextImpl();
        assertSame(simpleMethodInvocation, finder.findFirstUnverifiedInOrder(context, invocations));        
        
        //when
        context.markVerified(simpleMethodInvocationTwo);
        context.markVerified(simpleMethodInvocation);
        
        //then
        assertSame(differentMethodInvocation, finder.findFirstUnverifiedInOrder(context, invocations));
        
        //when
        context.markVerified(differentMethodInvocation);
        
        //then
        assertNull(finder.findFirstUnverifiedInOrder(context, invocations));
    }
    
    @Test
    public void shouldFindFirstUnverifiedInOrderAndRespectSequenceNumber() throws Exception {
        //given
        InOrderContextImpl context = new InOrderContextImpl();
        assertSame(simpleMethodInvocation, finder.findFirstUnverifiedInOrder(context, invocations));        
        
        //when
        //skipping verification of first invocation, then:
        context.markVerified(simpleMethodInvocationTwo);
        context.markVerified(differentMethodInvocation);
        
        //then
        assertSame(null, finder.findFirstUnverifiedInOrder(context, invocations));        
    }
    
    @Test
    public void shouldFindFirstUnverifiedInvocationOnMock() throws Exception {
        assertSame(simpleMethodInvocation, finder.findFirstUnverified(invocations, simpleMethodInvocation.getMock()));
        assertNull(finder.findFirstUnverified(invocations, "different mock"));
    }
    
    @Test
    public void shouldFindFirstSimilarInvocationByName() throws Exception {
        InvocationImpl overloadedSimpleMethod = new InvocationBuilder().mock(mock).simpleMethod().arg("test").toInvocation();
        
        InvocationImpl found = finder.findSimilarInvocation(invocations, new InvocationMatcher(overloadedSimpleMethod));
        assertSame(found, simpleMethodInvocation);
    }
    
    @Test
    public void shouldFindInvocationWithTheSameMethod() throws Exception {
        InvocationImpl overloadedDifferentMethod = new InvocationBuilder().differentMethod().arg("test").toInvocation();
        
        invocations.add(overloadedDifferentMethod);
        
        InvocationImpl found = finder.findSimilarInvocation(invocations, new InvocationMatcher(overloadedDifferentMethod));
        assertSame(found, overloadedDifferentMethod);
    }
    
    @Test
    public void shouldGetLastStackTrace() throws Exception {
        LocationImpl last = finder.getLastLocation(invocations);
        assertSame(differentMethodInvocation.getLocation(), last);
        
        assertNull(finder.getLastLocation(Collections.<InvocationImpl>emptyList()));
    } 
    
    @Test
    public void shouldFindAllMatchingUnverifiedChunks() throws Exception {
        List<InvocationImpl> allMatching = finder.findAllMatchingUnverifiedChunks(invocations, new InvocationMatcher(simpleMethodInvocation), context);
        assertThat(allMatching, hasExactlyInOrder(simpleMethodInvocation, simpleMethodInvocationTwo));
        
        context.markVerified(simpleMethodInvocation);
        allMatching = finder.findAllMatchingUnverifiedChunks(invocations, new InvocationMatcher(simpleMethodInvocation), context);
        assertThat(allMatching, hasExactlyInOrder(simpleMethodInvocationTwo));
        
        context.markVerified(simpleMethodInvocationTwo);
        allMatching = finder.findAllMatchingUnverifiedChunks(invocations, new InvocationMatcher(simpleMethodInvocation), context);
        assertTrue(allMatching.isEmpty());
    }
    
    @Test
    public void shouldFindMatchingChunk() throws Exception {
        List<InvocationImpl> chunk = finder.findMatchingChunk(invocations, new InvocationMatcher(simpleMethodInvocation), 2, context);
        assertThat(chunk, hasExactlyInOrder(simpleMethodInvocation, simpleMethodInvocationTwo));
    }
    
    @Test
    public void shouldReturnAllChunksWhenModeIsAtLeastOnce() throws Exception {
        InvocationImpl simpleMethodInvocationThree = new InvocationBuilder().mock(mock).toInvocation();
        invocations.add(simpleMethodInvocationThree);
        
        List<InvocationImpl> chunk = finder.findMatchingChunk(invocations, new InvocationMatcher(simpleMethodInvocation), 1, context);
        assertThat(chunk, hasExactlyInOrder(simpleMethodInvocation, simpleMethodInvocationTwo, simpleMethodInvocationThree));
    }
    
    @Test
    public void shouldReturnAllChunksWhenWantedCountDoesntMatch() throws Exception {
        InvocationImpl simpleMethodInvocationThree = new InvocationBuilder().mock(mock).toInvocation();
        invocations.add(simpleMethodInvocationThree);
        
        List<InvocationImpl> chunk = finder.findMatchingChunk(invocations, new InvocationMatcher(simpleMethodInvocation), 1, context);
        assertThat(chunk, hasExactlyInOrder(simpleMethodInvocation, simpleMethodInvocationTwo, simpleMethodInvocationThree));
    }
    
    @Test
    public void shouldFindPreviousInOrder() throws Exception {
        InvocationImpl previous = finder.findPreviousVerifiedInOrder(invocations, context);
        assertNull(previous);
        
        context.markVerified(simpleMethodInvocation);
        context.markVerified(simpleMethodInvocationTwo);
        
        previous = finder.findPreviousVerifiedInOrder(invocations, context);
        assertSame(simpleMethodInvocationTwo, previous);
    }
}