/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.commons.lang.math;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test cases for the {@link FloatRange} class.
 *
 * @author Stephen Colebourne
 * @version $Id: FloatRangeTest.java,v 1.5 2003/08/18 02:22:27 bayard Exp $
 */
public final class FloatRangeTest extends AbstractRangeTest {

    public FloatRangeTest(String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(FloatRangeTest.class);
        suite.setName("FloatRange Tests");
        return suite;
    }
    
    public void setUp() {
        super.setUp();
        tenToTwenty = new FloatRange(float10, float20);
        otherRange = new NumberRange(ten, twenty);
    }

    protected Range createRange(Integer integer1, Integer integer2) {
        return new FloatRange(integer1, integer2);
    }
    protected Range createRange(Integer integer) {
        return new NumberRange(integer);
    }
    
    //--------------------------------------------------------------------------

    public void testConstructor1a() {
        FloatRange nr = new FloatRange(8f);
        assertEquals(float8, nr.getMinimumNumber());
        assertEquals(float8, nr.getMaximumNumber());
        
        try {
            new FloatRange(Float.NaN);
            fail();
        } catch (IllegalArgumentException ex) {}
    }
    
    public void testConstructor1b() {
        FloatRange nr = new FloatRange(float8);
        assertSame(float8, nr.getMinimumNumber());
        assertSame(float8, nr.getMaximumNumber());
        
        Range r = new FloatRange(nonComparable);
        
        try {
            new FloatRange(null);
            fail();
        } catch (IllegalArgumentException ex) {}
        try {
            new FloatRange(new Double(Double.NaN));
            fail();
        } catch (IllegalArgumentException ex) {}
    }
    
    public void testConstructor2a() {
        FloatRange nr = new FloatRange(8f, 10f);
        assertEquals(float8, nr.getMinimumNumber());
        assertEquals(float10, nr.getMaximumNumber());
        
        nr = new FloatRange(10f, 8f);
        assertEquals(float8, nr.getMinimumNumber());
        assertEquals(float10, nr.getMaximumNumber());
        
        try {
            new FloatRange(Float.NaN, 8f);
            fail();
        } catch (IllegalArgumentException ex) {}
    }

    public void testConstructor2b() {
        FloatRange nr = new FloatRange(float8, float10);
        assertSame(float8, nr.getMinimumNumber());
        assertSame(float10, nr.getMaximumNumber());
        
        nr = new FloatRange(float10, float8);
        assertSame(float8, nr.getMinimumNumber());
        assertSame(float10, nr.getMaximumNumber());
        
        nr = new FloatRange(float8, float10);
        assertSame(float8, nr.getMinimumNumber());
        assertEquals(float10, nr.getMaximumNumber());
        
        // not null
        try {
            new FloatRange(float8, null);
            fail();
        } catch (IllegalArgumentException ex) {}
        try {
            new FloatRange(null, float8);
            fail();
        } catch (IllegalArgumentException ex) {}
        try {
            new FloatRange(null, null);
            fail();
        } catch (IllegalArgumentException ex) {}
        
        try {
            new FloatRange(new Double(Double.NaN), float10);
            fail();
        } catch (IllegalArgumentException ex) {}
    }

    //--------------------------------------------------------------------------

    public void testContainsNumber() {
        assertEquals(false, tenToTwenty.containsNumber(null));
        assertEquals(true, tenToTwenty.containsNumber(nonComparable));
        
        assertEquals(false, tenToTwenty.containsNumber(five));
        assertEquals(true, tenToTwenty.containsNumber(ten));
        assertEquals(true, tenToTwenty.containsNumber(fifteen));
        assertEquals(true, tenToTwenty.containsNumber(twenty));
        assertEquals(false, tenToTwenty.containsNumber(twentyFive));
        
        assertEquals(false, tenToTwenty.containsNumber(long8));
        assertEquals(true, tenToTwenty.containsNumber(long10));
        assertEquals(true, tenToTwenty.containsNumber(long12));
        assertEquals(true, tenToTwenty.containsNumber(long20));
        assertEquals(false, tenToTwenty.containsNumber(long21));
        
        assertEquals(false, tenToTwenty.containsNumber(double8));
        assertEquals(true, tenToTwenty.containsNumber(double10));
        assertEquals(true, tenToTwenty.containsNumber(double12));
        assertEquals(true, tenToTwenty.containsNumber(double20));
        assertEquals(false, tenToTwenty.containsNumber(double21));
        
        assertEquals(false, tenToTwenty.containsNumber(float8));
        assertEquals(true, tenToTwenty.containsNumber(float10));
        assertEquals(true, tenToTwenty.containsNumber(float12));
        assertEquals(true, tenToTwenty.containsNumber(float20));
        assertEquals(false, tenToTwenty.containsNumber(float21));
    }

    public void testToString() {
        String str = tenToTwenty.toString();
        assertEquals("Range[10.0,20.0]", str);
        assertSame(str, tenToTwenty.toString());
        assertEquals("Range[-20.0,-10.0]", createRange(new Integer(-20), new Integer(-10)).toString());
    }
    
    //--------------------------------------------------------------------------
    
}
