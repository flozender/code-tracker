/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.invocation;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.matchers.IsCollectionContaining;
import org.mockito.TestBase;
import org.mockito.internal.matchers.ArrayEquals;
import org.mockito.internal.matchers.Equals;
import org.mockitousage.IMethods;

@SuppressWarnings("unchecked")
public class InvocationTest extends TestBase {

    private Invocation invocation;

    @Before
    public void setup() throws Exception {
        invocation = new InvocationBuilder().args(" ").mock("mock").toInvocation();
    }

    @Test
    public void shouldKnowIfIsEqualTo() {
        Invocation equal =                  new InvocationBuilder().args(" ").mock("mock").toInvocation();
        Invocation nonEqual =               new InvocationBuilder().args("X").mock("mock").toInvocation();
        Invocation withNewStringInstance =  new InvocationBuilder().args(new String(" ")).mock("mock").toInvocation();

        assertFalse(invocation.equals(null));
        assertFalse(invocation.equals(""));
        assertTrue(invocation.equals(equal));
        assertFalse(invocation.equals(nonEqual));
        assertTrue(invocation.equals(withNewStringInstance));
    }
    
    @Test
    public void shouldEqualToNotConsiderSequenceNumber() {
        Invocation equal = new InvocationBuilder().args(" ").mock("mock").seq(2).toInvocation();
        
        assertTrue(invocation.equals(equal));
        assertTrue(invocation.getSequenceNumber() != equal.getSequenceNumber());
    }
    
    @Test
    public void shouldNotBeACitizenOfHashes() {
        Map map = new HashMap();
        try {
            map.put(invocation, "one");
            fail();
        } catch (RuntimeException e) {
            assertEquals("hashCode() is not implemented", e.getMessage());
        }
    }
    
    @Test
    public void shouldPrintMethodName() {
        invocation = new InvocationBuilder().toInvocation();
        assertEquals("Object.simpleMethod()", invocation.toString());
    }
    
    @Test
    public void shouldPrintMethodArgs() {
        invocation = new InvocationBuilder().args("foo").toInvocation();
        assertEquals("Object.simpleMethod(\"foo\")", invocation.toString());
    }
    
    @Test
    public void shouldPrintMethodIntegerArgAndString() {
        invocation = new InvocationBuilder().args("foo", 1).toInvocation();
        assertEquals("Object.simpleMethod(\"foo\", 1)", invocation.toString());
    }
    
    @Test
    public void shouldPrintNull() {
        invocation = new InvocationBuilder().args((String) null).toInvocation();
        assertEquals("Object.simpleMethod(null)", invocation.toString());
    }
    
    @Test
    public void shouldPrintArray() {
        invocation = new InvocationBuilder().method("oneArray").args(new int[] { 1, 2, 3 }).toInvocation();
        assertEquals("Object.oneArray([1, 2, 3])", invocation.toString());
    }
    
    @Test
    public void shouldPrintNullIfArrayIsNull() throws Exception {
        Method m = IMethods.class.getMethod("oneArray", Object[].class);
        invocation = new InvocationBuilder().method(m).args((Object) null).toInvocation();
        assertEquals("Object.oneArray(null)", invocation.toString());
    }
    
    @Test
    public void shouldMarkVerifiedWhenMarkingVerifiedInOrder() throws Exception {
        assertFalse(invocation.isVerified());
        assertFalse(invocation.isVerifiedInOrder());
        
        invocation.markVerifiedInOrder();
        
        assertTrue(invocation.isVerified());
        assertTrue(invocation.isVerifiedInOrder());
    }
    
    @Test
    public void shouldPrintAllArguments() throws Exception {
        Invocation i = new InvocationBuilder().args(new Object[] {"1", 2, 3, 4, 5}).toInvocation();
        String expected = 
            "    1st: \"1\"\n" +
            "    2nd: 2\n" +
            "    3rd: 3\n" +
            "    4th: 4\n" +
            "    5th: 5";
        
        assertEquals(expected, i.getArgs());
    }
    
    @Test
    public void shouldPrintNoArguments() throws Exception {
        Invocation i = new InvocationBuilder().toInvocation();
        assertEquals("    <NO ARGUMENTS>", i.getArgs());
    }
    
    @Test
    public void shouldTransformArgumentsToMatchers() throws Exception {
        Invocation i = new InvocationBuilder().args("foo", new String[] {"bar"}).toInvocation();
        List matchers = i.argumentsToMatchers();

        //TODO when I use IsCollectionContaining.hasItems ant fails to compile tests. 
        assertEquals(2, matchers.size());
        assertEquals(Equals.class, matchers.get(0).getClass());
        assertEquals(ArrayEquals.class, matchers.get(1).getClass());
    }
}