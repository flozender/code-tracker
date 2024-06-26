/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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
package org.apache.commons.lang;

import java.math.BigDecimal;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test cases for the {@link NumberRange} class.
 *
 * @author <a href="mailto:chrise@esha.com">Christopher Elkins</a>
 * @author <a href="mailto:ridesmet@users.sourceforge.net">Ringo De Smet</a>
 * @author Tim O'Brien
 * @version $Revision: 1.6 $ $Date: 2003/10/11 20:35:11 $
 */

public final class NumberRangeTest extends TestCase {


    private NumberRange tenToTwenty;
    private NumberRange fifteenToTwentyFive;
    private NumberRange fiveToNine;
    private Number five;
    private Number nine;
    private Number ten;
    private Number fifteen;
    private Number twenty;
    private Number twentyFive;

    public NumberRangeTest(String name) {
        super(name);
    }

    public void setUp() {
        five       = new Integer(5);
        nine      = new Double(9.0);
        ten        = new Integer(10);
        fifteen    = new Integer(15);
        twenty     = new Integer(20);
        twentyFive = new Integer(25);

        tenToTwenty = new NumberRange(ten, twenty);
        fifteenToTwentyFive = new NumberRange( fifteen, twentyFive);
        fiveToNine = new NumberRange( five, nine );
    }


    public static Test suite() {
        TestSuite suite = new TestSuite(NumberRangeTest.class);
        suite.setName("NumberRange Tests");
        return suite;
    }

    public void testMaxMin() {
        boolean expected = true;
        boolean result = tenToTwenty.getMaximum().equals(twenty);
        assertEquals(expected, result);

        expected = true;
        result = tenToTwenty.getMinimum().equals(ten);
        assertEquals(expected, result);
    }

    public void testEquals() {
        boolean expected = false;
        boolean result = tenToTwenty.equals(new NumberRange(five, ten));
        assertEquals(expected, result);

        expected = true;
        result = tenToTwenty.equals(new NumberRange(ten, twenty));
        assertEquals(expected, result);

        expected = false;
        result = tenToTwenty.equals(new NumberRange(ten, fifteen));
        assertEquals(expected, result);

        expected = false;
        result = tenToTwenty.equals(new NumberRange(fifteen, twenty));
        assertEquals(expected, result);
    }
    
    public void testEqualsWithOtherObject() {
        assertEquals( "A NumberRange should not equals a String object", false, fiveToNine.equals("TEST"));
    }

    public void testEqualsWithSameReference() {
        assertEquals( "A NumberRange should equal itself", true, fiveToNine.equals(fiveToNine));
    }

    public void testEqualsNull() {
        assertEquals( "A NumberRange should not equal null", false, fiveToNine.equals(null));
    }

    public void testHashCode() {
        NumberRange nr = new NumberRange( new Integer(5), new Double(9.0));
        assertEquals( "The hashCode of 5-9 should equals the hashcode of another NumberRange of the same min/max",
                                fiveToNine.hashCode(), nr.hashCode());
        assertTrue( "The hashCode of 10-20 should not equal the hashCode of 5-9", 
                            fiveToNine.hashCode() != tenToTwenty.hashCode());                        
    }

    public void testIncludesNumber() {
        boolean expected = false;
        boolean result = tenToTwenty.includesNumber(five);
        assertEquals(expected, result);

        expected = true;
        result = tenToTwenty.includesNumber(ten);
        assertEquals(expected, result);

        expected = true;
        result = tenToTwenty.includesNumber(fifteen);
        assertEquals(expected, result);

        expected = true;
        result = tenToTwenty.includesNumber(twenty);
        assertEquals(expected, result);

        expected = false;
        result = tenToTwenty.includesNumber(twentyFive);
        assertEquals(expected, result);
    }

    public void testIncludesNumberNull() {
        boolean result = tenToTwenty.includesNumber(null);
        assertEquals("Includes number should return false for null values", false, result);
    }

    public void testIncludesRange() {
        boolean expected = false;
        boolean result = tenToTwenty.includesRange(new NumberRange(five, ten));
        assertEquals(expected, result);

        expected = false;
        result = tenToTwenty.includesRange(new NumberRange(five, fifteen));
        assertEquals(expected, result);

        expected = true;
        result = tenToTwenty.includesRange(new NumberRange(ten, fifteen));
        assertEquals(expected, result);

        expected = true;
        result = tenToTwenty.includesRange(new NumberRange(ten, twenty));
        assertEquals(expected, result);

        expected = true;
        result = tenToTwenty.includesRange(new NumberRange(fifteen, twenty));
        assertEquals(expected, result);

        expected = false;
        result = 
            tenToTwenty.includesRange(new NumberRange(fifteen, twentyFive));
        assertEquals(expected, result);

        expected = false;
        result = 
            tenToTwenty.includesRange(new NumberRange(twenty, twentyFive));
        assertEquals(expected, result);
    }

    public void testIncludesRangeNull() {
        boolean result = tenToTwenty.includesRange(null);
        assertEquals("Includes range should return false for null values", false, result);
    }

    public void testConstructor() {
        NumberRange nr = new NumberRange( new Double(2.0));
        assertEquals("Unexpected min on NumberRange", 2.0, nr.getMinimum().doubleValue(), Double.MIN_VALUE);
        assertEquals("Unexpected max on NumberRange", 2.0, nr.getMaximum().doubleValue(), Double.MIN_VALUE);
    }

	public void testConstructorNullParameters() {
		try {
			NumberRange nr = new NumberRange(null);
			fail("NumberRange(null) did not throw an exception.");
		} catch (Exception e) {
			assertTrue(	"NumberRange(null)", e instanceof NullPointerException);
		}

		try {
			NumberRange nr = new NumberRange(five, null);
			fail("NumberRange(five, null) did not throw an exception.");
		} catch (Exception e) {
			assertTrue("NumberRange(five, null)", e instanceof NullPointerException);
		}

		try {
			NumberRange nr = new NumberRange(null, five);
			fail("NumberRange(null, five) did not throw an exception.");
		} catch (Exception e) {
			assertTrue("NumberRange(null, five)", e instanceof NullPointerException);
		}
	}

    public void testConstructorWithMaxLessThanMin() {
        NumberRange nr = new NumberRange( new Double(2.0), new Double(1.0));
        assertEquals("Unexpected min on NumberRange", 2.0, nr.getMinimum().doubleValue(), Double.MIN_VALUE);
        assertEquals("Unexpected max on NumberRange", 2.0, nr.getMaximum().doubleValue(), Double.MIN_VALUE);
    }

    public void testOverlap() {
        assertEquals( "5-9 should not overlap 10-20", false, fiveToNine.overlaps( tenToTwenty ));
        assertEquals( "10-20 should overlap 15-25", true, tenToTwenty.overlaps( fifteenToTwentyFive ));
    }

    public void testOverlapNull() {
        assertEquals( "5-9 should not overlap null", false, fiveToNine.overlaps( null ));
    }

    public void testToString() {
        String expected = "10-20";
        String result = tenToTwenty.toString();
        assertEquals(expected, result);
    }

    public void testToStringWithNegatives() {
        String expected = "(-20)-(-10)";
        NumberRange nr = new NumberRange( new Integer(-20), new Integer(-10));
        String result = nr.toString();
        assertEquals(expected, result);

        expected = "(-20)-10";
        nr = new NumberRange( new Integer(-20), new Integer(10));
        result = nr.toString();
        assertEquals(expected, result);
    }


}

