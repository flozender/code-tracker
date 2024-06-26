/*
 * Copyright 2002-2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.lang.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests the org.apache.commons.lang.util.IdentifierUtils class.
 *
 * @deprecated WILL BE REMOVED SOON
 * @author Stephen Colebourne
 * @version $Id: IdentifierUtilsTest.java,v 1.6 2004/02/18 23:03:51 ggregory Exp $
 */
public class IdentifierUtilsTest extends junit.framework.TestCase {

    /**
     * Construct
     */
    public IdentifierUtilsTest(String name) {
        super(name);
    }

    /**
     * Return class aa a test suite.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(IdentifierUtilsTest.class);
        suite.setName("IdentifierUtils Tests");
        return suite;
    }

    //-----------------------------------------------------------------------
    public void testConstructor() {
        assertNotNull(new IdentifierUtils());
        Constructor[] cons = IdentifierUtils.class.getDeclaredConstructors();
        assertEquals(1, cons.length);
        assertEquals(true, Modifier.isPublic(cons[0].getModifiers()));
        assertEquals(true, Modifier.isPublic(IdentifierUtils.class.getModifiers()));
        assertEquals(false, Modifier.isFinal(IdentifierUtils.class.getModifiers()));
    }
    
    //--------------------------------------------------------------------------

    public void testLongIncrementing() {
        LongIdentifierFactory f = IdentifierUtils.LONG_IDENTIFIER_FACTORY;
        assertEquals(new Long(0), f.nextLongIdentifier());
        assertEquals(new Long(1), f.nextLongIdentifier());
        assertEquals(new Long(2), f.nextIdentifier());
        assertEquals(new Long(3), f.nextLongIdentifier());
        assertEquals(new Long(4), IdentifierUtils.nextLongIdentifier());
        assertEquals(new Long(5), f.nextLongIdentifier());
        assertEquals(new Long(6), IdentifierUtils.nextLongIdentifier());
        assertEquals(new Long(7), IdentifierUtils.nextLongIdentifier());
    }

    public void testLongIncrementingNoArgs() {
        LongIdentifierFactory f = IdentifierUtils.longIdentifierFactory();
        assertEquals(new Long(0), f.nextLongIdentifier());
        assertEquals(new Long(1), f.nextLongIdentifier());
        assertTrue(f != IdentifierUtils.LONG_IDENTIFIER_FACTORY);
    }

    public void testLongIncrementingInit() {
        LongIdentifierFactory f = IdentifierUtils.longIdentifierFactory(true, 100);
        assertEquals(new Long(100), f.nextLongIdentifier());
        assertEquals(new Long(101), f.nextLongIdentifier());
    }

    public void testLongIncrementingWrap() {
        LongIdentifierFactory f = IdentifierUtils.longIdentifierFactory(true, Long.MAX_VALUE - 1);
        assertEquals(new Long(Long.MAX_VALUE - 1), f.nextLongIdentifier());
        assertEquals(new Long(Long.MAX_VALUE), f.nextLongIdentifier());
        assertEquals(new Long(Long.MIN_VALUE), f.nextLongIdentifier());
    }

    public void testLongIncrementingNoWrap() {
        LongIdentifierFactory f = IdentifierUtils.longIdentifierFactory(false, Long.MAX_VALUE);
        try {
            f.nextLongIdentifier();
            fail();
        } catch (IllegalStateException ex) {}
    }

    //--------------------------------------------------------------------------

    public void testStringNumericLong() {
        StringIdentifierFactory f = IdentifierUtils.STRING_NUMERIC_IDENTIFIER_FACTORY;
        assertEquals("0", f.nextStringIdentifier());
        assertEquals("1", f.nextStringIdentifier());
        assertEquals("2", f.nextIdentifier());
        assertEquals("3", f.nextStringIdentifier());
        assertEquals("4", IdentifierUtils.nextStringNumericIdentifier());
        assertEquals("5", f.nextStringIdentifier());
        assertEquals("6", IdentifierUtils.nextStringNumericIdentifier());
        assertEquals("7", IdentifierUtils.nextStringNumericIdentifier());
    }

    public void testStringNumericNoArgs() {
        StringIdentifierFactory f = IdentifierUtils.stringNumericIdentifierFactory();
        assertEquals("0", f.nextStringIdentifier());
        assertEquals("1", f.nextStringIdentifier());
        assertTrue(f != IdentifierUtils.STRING_NUMERIC_IDENTIFIER_FACTORY);
    }

    public void testStringNumericInit() {
        StringIdentifierFactory f = IdentifierUtils.stringNumericIdentifierFactory(true, 100);
        assertEquals("100", f.nextStringIdentifier());
        assertEquals("101", f.nextStringIdentifier());
    }

    public void testStringNumericWrap() {
        StringIdentifierFactory f = IdentifierUtils.stringNumericIdentifierFactory(true, Long.MAX_VALUE - 1);
        assertEquals(Long.toString(Long.MAX_VALUE - 1), f.nextStringIdentifier());
        assertEquals(Long.toString(Long.MAX_VALUE), f.nextStringIdentifier());
        assertEquals(Long.toString(Long.MIN_VALUE), f.nextStringIdentifier());
    }

    public void testStringNumericNoWrap() {
        StringIdentifierFactory f = IdentifierUtils.stringNumericIdentifierFactory(false, Long.MAX_VALUE);
        try {
            f.nextStringIdentifier();
            fail();
        } catch (IllegalStateException ex) { }
    }

    //--------------------------------------------------------------------------

    public void testStringAlphanumeric() {
        StringIdentifierFactory f = IdentifierUtils.STRING_ALPHANUMERIC_IDENTIFIER_FACTORY;
        assertEquals("000000000000001", f.nextStringIdentifier());
        assertEquals("000000000000002", f.nextIdentifier());
        assertEquals("000000000000003", f.nextStringIdentifier());
        assertEquals("000000000000004", f.nextStringIdentifier());
        assertEquals("000000000000005", f.nextStringIdentifier());
        assertEquals("000000000000006", f.nextStringIdentifier());
        assertEquals("000000000000007", f.nextStringIdentifier());
        assertEquals("000000000000008", f.nextStringIdentifier());
        assertEquals("000000000000009", f.nextStringIdentifier());
        assertEquals("00000000000000a", f.nextStringIdentifier());
        assertEquals("00000000000000b", f.nextStringIdentifier());
        assertEquals("00000000000000c", f.nextStringIdentifier());
        assertEquals("00000000000000d", IdentifierUtils.nextStringAlphanumericIdentifier());
        assertEquals("00000000000000e", f.nextStringIdentifier());
        assertEquals("00000000000000f", f.nextStringIdentifier());
        assertEquals("00000000000000g", f.nextStringIdentifier());
        assertEquals("00000000000000h", f.nextStringIdentifier());
        assertEquals("00000000000000i", f.nextStringIdentifier());
        assertEquals("00000000000000j", f.nextStringIdentifier());
        assertEquals("00000000000000k", f.nextStringIdentifier());
        assertEquals("00000000000000l", f.nextStringIdentifier());
        assertEquals("00000000000000m", f.nextStringIdentifier());
        assertEquals("00000000000000n", f.nextStringIdentifier());
        assertEquals("00000000000000o", f.nextStringIdentifier());
        assertEquals("00000000000000p", f.nextStringIdentifier());
        assertEquals("00000000000000q", f.nextStringIdentifier());
        assertEquals("00000000000000r", f.nextStringIdentifier());
        assertEquals("00000000000000s", f.nextStringIdentifier());
        assertEquals("00000000000000t", f.nextStringIdentifier());
        assertEquals("00000000000000u", f.nextStringIdentifier());
        assertEquals("00000000000000v", f.nextStringIdentifier());
        assertEquals("00000000000000w", f.nextStringIdentifier());
        assertEquals("00000000000000x", f.nextStringIdentifier());
        assertEquals("00000000000000y", f.nextStringIdentifier());
        assertEquals("00000000000000z", f.nextStringIdentifier());
        assertEquals("000000000000010", f.nextStringIdentifier());
        assertEquals("000000000000011", f.nextStringIdentifier());
        assertEquals("000000000000012", f.nextStringIdentifier());
        assertEquals("000000000000013", f.nextStringIdentifier());
    }

    public void testLongAlphanumericNoArgs() {
        StringIdentifierFactory f = IdentifierUtils.stringAlphanumericIdentifierFactory();
        assertEquals("000000000000001", f.nextStringIdentifier());
        assertEquals("000000000000002", f.nextStringIdentifier());
        assertTrue(f != IdentifierUtils.STRING_ALPHANUMERIC_IDENTIFIER_FACTORY);
    }

    public void testStringAlphanumericWrap() {
        try {
            IdentifierUtils.stringAlphanumericIdentifierFactory(true, -1);
            fail();
        } catch (IllegalArgumentException ex) {}
        
        StringIdentifierFactory f = IdentifierUtils.stringAlphanumericIdentifierFactory(true, 1);
        assertEquals("1", f.nextStringIdentifier());
        assertEquals("2", f.nextStringIdentifier());
        assertEquals("3", f.nextStringIdentifier());
        assertEquals("4", f.nextStringIdentifier());
        assertEquals("5", f.nextStringIdentifier());
        assertEquals("6", f.nextStringIdentifier());
        assertEquals("7", f.nextStringIdentifier());
        assertEquals("8", f.nextStringIdentifier());
        assertEquals("9", f.nextStringIdentifier());
        assertEquals("a", f.nextStringIdentifier());
        assertEquals("b", f.nextStringIdentifier());
        assertEquals("c", f.nextStringIdentifier());
        assertEquals("d", f.nextStringIdentifier());
        assertEquals("e", f.nextStringIdentifier());
        assertEquals("f", f.nextStringIdentifier());
        assertEquals("g", f.nextStringIdentifier());
        assertEquals("h", f.nextStringIdentifier());
        assertEquals("i", f.nextStringIdentifier());
        assertEquals("j", f.nextStringIdentifier());
        assertEquals("k", f.nextStringIdentifier());
        assertEquals("l", f.nextStringIdentifier());
        assertEquals("m", f.nextStringIdentifier());
        assertEquals("n", f.nextStringIdentifier());
        assertEquals("o", f.nextStringIdentifier());
        assertEquals("p", f.nextStringIdentifier());
        assertEquals("q", f.nextStringIdentifier());
        assertEquals("r", f.nextStringIdentifier());
        assertEquals("s", f.nextStringIdentifier());
        assertEquals("t", f.nextStringIdentifier());
        assertEquals("u", f.nextStringIdentifier());
        assertEquals("v", f.nextStringIdentifier());
        assertEquals("w", f.nextStringIdentifier());
        assertEquals("x", f.nextStringIdentifier());
        assertEquals("y", f.nextStringIdentifier());
        assertEquals("z", f.nextStringIdentifier());
        assertEquals("0", f.nextStringIdentifier());
    }

    public void testStringAlphanumericNoWrap() {
        try {
            IdentifierUtils.stringAlphanumericIdentifierFactory(false, -1);
            fail();
        } catch (IllegalArgumentException ex) {}
        
        StringIdentifierFactory f = IdentifierUtils.stringAlphanumericIdentifierFactory(false, 1);
        assertEquals("1", f.nextStringIdentifier());
        assertEquals("2", f.nextStringIdentifier());
        assertEquals("3", f.nextStringIdentifier());
        assertEquals("4", f.nextStringIdentifier());
        assertEquals("5", f.nextStringIdentifier());
        assertEquals("6", f.nextStringIdentifier());
        assertEquals("7", f.nextStringIdentifier());
        assertEquals("8", f.nextStringIdentifier());
        assertEquals("9", f.nextStringIdentifier());
        assertEquals("a", f.nextStringIdentifier());
        assertEquals("b", f.nextStringIdentifier());
        assertEquals("c", f.nextStringIdentifier());
        assertEquals("d", f.nextStringIdentifier());
        assertEquals("e", f.nextStringIdentifier());
        assertEquals("f", f.nextStringIdentifier());
        assertEquals("g", f.nextStringIdentifier());
        assertEquals("h", f.nextStringIdentifier());
        assertEquals("i", f.nextStringIdentifier());
        assertEquals("j", f.nextStringIdentifier());
        assertEquals("k", f.nextStringIdentifier());
        assertEquals("l", f.nextStringIdentifier());
        assertEquals("m", f.nextStringIdentifier());
        assertEquals("n", f.nextStringIdentifier());
        assertEquals("o", f.nextStringIdentifier());
        assertEquals("p", f.nextStringIdentifier());
        assertEquals("q", f.nextStringIdentifier());
        assertEquals("r", f.nextStringIdentifier());
        assertEquals("s", f.nextStringIdentifier());
        assertEquals("t", f.nextStringIdentifier());
        assertEquals("u", f.nextStringIdentifier());
        assertEquals("v", f.nextStringIdentifier());
        assertEquals("w", f.nextStringIdentifier());
        assertEquals("x", f.nextStringIdentifier());
        assertEquals("y", f.nextStringIdentifier());
        assertEquals("z", f.nextStringIdentifier());
        try {
            f.nextStringIdentifier();
            fail();
        } catch (IllegalStateException ex) {}
    }

    //--------------------------------------------------------------------------

    public void testStringSession() {
        StringIdentifierFactory f = IdentifierUtils.STRING_SESSION_IDENTIFIER_FACTORY;
        assertTrue(f != IdentifierUtils.stringSessionIdentifierFactory());
        
        String a = (String) f.nextStringIdentifier();
        String b = (String) IdentifierUtils.nextStringSessionIdentifier();
        String c = (String) f.nextIdentifier();
        assertTrue(a.length() >= 10);
        assertTrue(b.length() >= 10);
        assertTrue(c.length() >= 10);
        try {
            // could fail, but unlikely
            assertTrue(a.substring(6, 9).equals(b.substring(6, 9)));
            assertTrue(a.substring(6, 9).equals(c.substring(6, 9)));
        } catch (AssertionFailedError ex) {
            // try again to make test more robust
            a = (String) f.nextStringIdentifier();
            b = (String) IdentifierUtils.nextStringSessionIdentifier();
            c = (String) f.nextIdentifier();
            assertTrue(a.substring(6, 9).equals(b.substring(6, 9)));
            assertTrue(a.substring(6, 9).equals(c.substring(6, 9)));
        }
        assertEquals("0", a.substring(9));
        assertEquals("1", b.substring(9));
        assertEquals("2", c.substring(9));
    }

    //--------------------------------------------------------------------------

}
