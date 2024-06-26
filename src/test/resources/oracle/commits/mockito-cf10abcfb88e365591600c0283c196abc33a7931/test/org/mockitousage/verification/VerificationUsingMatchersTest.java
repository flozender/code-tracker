/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockitousage.verification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;
import static org.mockito.CrazyMatchers.and;
import static org.mockito.CrazyMatchers.contains;
import static org.mockito.CrazyMatchers.geq;
import static org.mockito.CrazyMatchers.leq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.CrazyMatchers;
import org.mockito.Mockito;
import org.mockito.RequiresValidState;
import org.mockito.exceptions.verification.VerificationError;
import org.mockitousage.IMethods;

@SuppressWarnings("unchecked")  
public class VerificationUsingMatchersTest extends RequiresValidState {
    private IMethods mock;

    @Before
    public void setUp() {
        mock = Mockito.mock(IMethods.class);
    }

    @Test
    public void shouldVerifyUsingSameMatcher() {
        Object one = new String("1243");
        Object two = new String("1243");
        Object three = new String("1243");

        assertNotSame(one, two);
        assertEquals(one, two);
        assertEquals(two, three);

        mock.oneArg(one);
        mock.oneArg(two);
        
        verify(mock).oneArg(CrazyMatchers.same(one));
        verify(mock, times(2)).oneArg(two);
        
        try {
            verify(mock).oneArg(CrazyMatchers.same(three));
            fail();
        } catch (VerificationError e) {}
    }  
    
    @Test
    public void shouldVerifyUsingMixedMatchers() {
        mock.threeArgumentMethod(11, "", "01234");

        try {
            verify(mock).threeArgumentMethod(and(geq(7), leq(10)), isA(String.class), contains("123"));
            fail();
        } catch (VerificationError e) {}

        mock.threeArgumentMethod(8, new Object(), "01234");
        
        try {
            verify(mock).threeArgumentMethod(and(geq(7), leq(10)), isA(String.class), contains("123"));
            fail();
        } catch (VerificationError e) {}
        
        mock.threeArgumentMethod(8, "", "no match");

        try {
            verify(mock).threeArgumentMethod(and(geq(7), leq(10)), isA(String.class), contains("123"));
            fail();
        } catch (VerificationError e) {}
        
        mock.threeArgumentMethod(8, "", "123");
        
        verify(mock).threeArgumentMethod(and(geq(7), leq(10)), isA(String.class), contains("123"));
    }
}