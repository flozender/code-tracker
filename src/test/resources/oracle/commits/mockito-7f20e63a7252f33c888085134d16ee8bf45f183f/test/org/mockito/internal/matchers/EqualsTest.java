/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package org.mockito.internal.matchers;

import org.junit.Test;
import org.mockitoutil.TestBase;


public class EqualsTest extends TestBase {

    @Test
    public void shouldBeEqual() {
        assertEquals(new Equals(null), new Equals(null));
        assertEquals(new Equals(new Integer(2)), new Equals(new Integer(2)));
        assertFalse(new Equals(null).equals(null));
        assertFalse(new Equals(null).equals("Test"));
        assertEquals(1, new Equals(null).hashCode());
    }

    @Test
    public void shouldArraysBeEqual() {
        assertTrue(new Equals(new int[] {1, 2}).matches(new int[] {1, 2}));
        assertFalse(new Equals(new Object[] {"1"}).matches(new Object[] {"1.0"}));
    }
    
    @Test
    public void shouldDescribeWithExtraTypeInfo() throws Exception {
        String descStr = new Equals(100).getTypedDescription();
        
        assertEquals("(Integer) 100", descStr);
    }

    @Test
    public void shouldDescribeWithExtraTypeInfoOfLong() throws Exception {
        String descStr = new Equals(100L).getTypedDescription();
        
        assertEquals("(Long) 100", descStr);
    }

    @Test
    public void shouldDescribeWithTypeOfString() throws Exception {
        String descStr = new Equals("x").getTypedDescription();

        assertEquals("(String) \"x\"", descStr);
    }
    
    @Test
    public void shouldAppendQuotingForString() {
        String descStr = new Equals("str").describe();
        
        assertEquals("\"str\"", descStr);
    }

    @Test
    public void shouldAppendQuotingForChar() {
        String descStr = new Equals('s').describe();
        
        assertEquals("'s'", descStr);
    }
    
    @Test
    public void shouldDescribeUsingToString() {
        String descStr = new Equals(100).describe();
        
        assertEquals("100", descStr);
    }

    @Test
    public void shouldDescribeNull() {
        String descStr = new Equals(null).describe();
        
        assertEquals("null", descStr);
    }
    
    @Test
    public void shouldMatchTypes() throws Exception {
        //when
        ContainsTypedDescription equals = new Equals(10);
        
        //then
        assertTrue(equals.typeMatches(10));
        assertFalse(equals.typeMatches(10L));
    }
    
    @Test
    public void shouldMatchTypesSafelyWhenActualIsNull() throws Exception {
        //when
        ContainsTypedDescription equals = new Equals(null);
        
        //then
        assertFalse(equals.typeMatches(10));
    }

    @Test
    public void shouldMatchTypesSafelyWhenGivenIsNull() throws Exception {
        //when
        ContainsTypedDescription equals = new Equals(10);
        
        //then
        assertFalse(equals.typeMatches(null));
    }
}