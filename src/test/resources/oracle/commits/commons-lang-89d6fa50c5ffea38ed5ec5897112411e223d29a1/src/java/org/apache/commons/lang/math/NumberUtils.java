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
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.commons.lang.NullArgumentException;

/**
 * <p>Provides extra functionality for Java Number classes.</p>
 *
 * @author <a href="mailto:bayard@generationjava.com">Henri Yandell</a>
 * @author <a href="mailto:rand_mcneely@yahoo.com">Rand McNeely</a>
 * @author Stephen Colebourne
 * @author <a href="mailto:steve.downey@netfolio.com">Steve Downey</a>
 * @author Eric Pugh
 * @author Phil Steitz
 * @author Matthew Hawthorne
 * @since 2.0
 * @version $Id: NumberUtils.java,v 1.5 2003/07/26 02:57:37 ggregory Exp $
 */
public final class NumberUtils {
    
    /** Reusable Long constant for zero. */
    public static final Long LONG_ZERO = new Long(0L);
    /** Reusable Long constant for one. */
    public static final Long LONG_ONE = new Long(1L);
    /** Reusable Long constant for minus one. */
    public static final Long LONG_MINUS_ONE = new Long(-1L);
    /** Reusable Integer constant for zero. */
    public static final Integer INTEGER_ZERO = new Integer(0);
    /** Reusable Integer constant for one. */
    public static final Integer INTEGER_ONE = new Integer(1);
    /** Reusable Integer constant for minus one. */
    public static final Integer INTEGER_MINUS_ONE = new Integer(-1);
    /** Reusable Short constant for zero. */
    public static final Short SHORT_ZERO = new Short((short) 0);
    /** Reusable Short constant for one. */
    public static final Short SHORT_ONE = new Short((short) 1);
    /** Reusable Short constant for minus one. */
    public static final Short SHORT_MINUS_ONE = new Short((short) -1);
    /** Reusable Byte constant for zero. */
    public static final Byte BYTE_ZERO = new Byte((byte) 0);
    /** Reusable Byte constant for one. */
    public static final Byte BYTE_ONE = new Byte((byte) 1);
    /** Reusable Byte constant for minus one. */
    public static final Byte BYTE_MINUS_ONE = new Byte((byte) -1);
    /** Reusable Double constant for zero. */
    public static final Double DOUBLE_ZERO = new Double(0.0d);
    /** Reusable Double constant for one. */
    public static final Double DOUBLE_ONE = new Double(1.0d);
    /** Reusable Double constant for minus one. */
    public static final Double DOUBLE_MINUS_ONE = new Double(-1.0d);
    /** Reusable Float constant for zero. */
    public static final Float FLOAT_ZERO = new Float(0.0f);
    /** Reusable Float constant for one. */
    public static final Float FLOAT_ONE = new Float(1.0f);
    /** Reusable Float constant for minus one. */
    public static final Float FLOAT_MINUS_ONE = new Float(-1.0f);

    /**
     * <p><code>NumberUtils</code> instances should NOT be constructed in standard programming.
     * Instead, the class should be used as <code>NumberUtils.stringToInt("6");</code>.</p>
     *
     * <p>This constructor is public to permit tools that require a JavaBean instance
     * to operate.</p>
     */
    public NumberUtils() {
    }

    //-----------------------------------------------------------------------
    /**
     * <p>Convert a <code>String</code> to an <code>int</code>, returning
     * <code>zero</code> if the conversion fails.</p>
     *
     * <p>If the string is <code>null</code>, <code>zero</code> is returned.</p>
     * 
     * @param str  the string to convert, may be null
     * @return the int represented by the string, or <code>zero</code> if
     *  conversion fails
     */
    public static int stringToInt(String str) {
        return stringToInt(str, 0);
    }

    /**
     * <p>Convert a <code>String</code> to an <code>int</code>, returning a
     * default value if the conversion fails.</p>
     *
     * <p>If the string is <code>null</code>, the default value is returned.</p>
     * 
     * @param str  the string to convert, may be null
     * @param defaultValue  the default value
     * @return the int represented by the string, or the default if conversion fails
     */
    public static int stringToInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    //-----------------------------------------------------------------------
    // must handle Long, Float, Integer, Float, Short,
    //                  BigDecimal, BigInteger and Byte
    // useful methods:
    // Byte.decode(String)
    // Byte.valueOf(String,int radix)
    // Byte.valueOf(String)
    // Double.valueOf(String)
    // Float.valueOf(String)
    // new Float(String)
    // Integer.valueOf(String,int radix)
    // Integer.valueOf(String)
    // Integer.decode(String)
    // Integer.getInteger(String)
    // Integer.getInteger(String,int val)
    // Integer.getInteger(String,Integer val)
    // new Integer(String)
    // new Double(String)
    // new Byte(String)
    // new Long(String)
    // Long.getLong(String)
    // Long.getLong(String,int)
    // Long.getLong(String,Integer)
    // Long.valueOf(String,int)
    // Long.valueOf(String)
    // new Short(String)
    // Short.decode(String)
    // Short.valueOf(String,int)
    // Short.valueOf(String)
    // new BigDecimal(String)
    // new BigInteger(String)
    // new BigInteger(String,int radix)
    // Possible inputs:
    // 45 45.5 45E7 4.5E7 Hex Oct Binary xxxF xxxD xxxf xxxd
    // plus minus everything. Prolly more. A lot are not separable.

    /**
     * <p>Turns a string value into a java.lang.Number.</p>
     *
     * <p>First, the value is examined for a type qualifier on the end
     * (<code>'f','F','d','D','l','L'</code>).  If it is found, it starts 
     * trying to create successively larger types from the type specified
     * until one is found that can represent the value.</p>
     *
     * <p>If a type specifier is not found, it will check for a decimal point
     * and then try successively larger types from <code>Integer</code> to
     * <code>BigInteger</code> and from <code>Float</code> to
     * <code>BigDecimal</code>.</p>
     *
     * <p>If the string starts with <code>0x</code> or <code>-0x</code>, it
     * will be interpreted as a hexadecimal integer.  Values with leading
     * <code>0</code>'s will not be interpreted as octal.</p>
     *
     * <p>Returns <code>null</code> if the string is <code>null</code>.</p>
     *
     * <p>This method does not trim the input string, i.e., strings with leading
     * or trailing spaces will generate NumberFormatExceptions.</p>
     *
     * @param str  String containing a number, may be null
     * @return Number created from the string
     * @throws NumberFormatException if the value cannot be converted
     */
    public static Number createNumber(String str) throws NumberFormatException {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            throw new NumberFormatException("\"\" is not a valid number.");
        }
        if (str.startsWith("--")) {
            // this is protection for poorness in java.lang.BigDecimal.
            // it accepts this as a legal value, but it does not appear 
            // to be in specification of class. OS X Java parses it to 
            // a wrong value.
            return null;
        }
        if (str.startsWith("0x") || str.startsWith("-0x")) {
            return createInteger(str);
        }   
        char lastChar = str.charAt(str.length() - 1);
        String mant;
        String dec;
        String exp;
        int decPos = str.indexOf('.');
        int expPos = str.indexOf('e') + str.indexOf('E') + 1;

        if (decPos > -1) {

            if (expPos > -1) {
                if (expPos < decPos) {
                    throw new NumberFormatException(str + " is not a valid number.");
                }
                dec = str.substring(decPos + 1, expPos);
            } else {
                dec = str.substring(decPos + 1);
            }
            mant = str.substring(0, decPos);
        } else {
            if (expPos > -1) {
                mant = str.substring(0, expPos);
            } else {
                mant = str;
            }
            dec = null;
        }
        if (!Character.isDigit(lastChar)) {
            if (expPos > -1 && expPos < str.length() - 1) {
                exp = str.substring(expPos + 1, str.length() - 1);
            } else {
                exp = null;
            }
            //Requesting a specific type..
            String numeric = str.substring(0, str.length() - 1);
            boolean allZeros = isAllZeros(mant) && isAllZeros(exp);
            switch (lastChar) {
                case 'l' :
                case 'L' :
                    if (dec == null
                        && exp == null
                        && isDigits(numeric.substring(1))
                        && (numeric.charAt(0) == '-' || Character.isDigit(numeric.charAt(0)))) {
                        try {
                            return createLong(numeric);
                        } catch (NumberFormatException nfe) {
                            //Too big for a long
                        }
                        return createBigInteger(numeric);

                    }
                    throw new NumberFormatException(str + " is not a valid number.");
                case 'f' :
                case 'F' :
                    try {
                        Float f = NumberUtils.createFloat(numeric);
                        if (!(f.isInfinite() || (f.floatValue() == 0.0F && !allZeros))) {
                            //If it's too big for a float or the float value = 0 and the string
                            //has non-zeros in it, then float doens't have the presision we want
                            return f;
                        }

                    } catch (NumberFormatException nfe) {
                    }
                    //Fall through
                case 'd' :
                case 'D' :
                    try {
                        Double d = NumberUtils.createDouble(numeric);
                        if (!(d.isInfinite() || (d.floatValue() == 0.0D && !allZeros))) {
                            return d;
                        }
                    } catch (NumberFormatException nfe) {
                    }
                    try {
                        return createBigDecimal(numeric);
                    } catch (NumberFormatException e) {
                    }
                    //Fall through
                default :
                    throw new NumberFormatException(str + " is not a valid number.");

            }
        } else {
            //User doesn't have a preference on the return type, so let's start
            //small and go from there...
            if (expPos > -1 && expPos < str.length() - 1) {
                exp = str.substring(expPos + 1, str.length());
            } else {
                exp = null;
            }
            if (dec == null && exp == null) {
                //Must be an int,long,bigint
                try {
                    return createInteger(str);
                } catch (NumberFormatException nfe) {
                }
                try {
                    return createLong(str);
                } catch (NumberFormatException nfe) {
                }
                return createBigInteger(str);

            } else {
                //Must be a float,double,BigDec
                boolean allZeros = isAllZeros(mant) && isAllZeros(exp);
                try {
                    Float f = createFloat(str);
                    if (!(f.isInfinite() || (f.floatValue() == 0.0F && !allZeros))) {
                        return f;
                    }
                } catch (NumberFormatException nfe) {
                }
                try {
                    Double d = createDouble(str);
                    if (!(d.isInfinite() || (d.doubleValue() == 0.0D && !allZeros))) {
                        return d;
                    }
                } catch (NumberFormatException nfe) {
                }

                return createBigDecimal(str);

            }
        }
    }

    /**
     * <p>Utility method for {@link #createNumber(java.lang.String)}.</p>
     *
     * <p>Returns <code>true</code> if s is <code>null</code>.</p>
     * 
     * @param str  the String to check
     * @return if it is all zeros or <code>null</code>
     */
    private static boolean isAllZeros(String str) {
        if (str == null) {
            return true;
        }
        for (int i = str.length() - 1; i >= 0; i--) {
            if (str.charAt(i) != '0') {
                return false;
            }
        }
        return str.length() > 0;
    }

    //-----------------------------------------------------------------------
    /**
     * <p>Convert a <code>String</code> to a <code>Float</code>.</p>
     *
     * <p>Returns <code>null</code> if the string is <code>null</code>.</p>
     * 
     * @param str  a <code>String</code> to convert, may be null
     * @return converted <code>Float</code>
     * @throws NumberFormatException if the value cannot be converted
     */
    public static Float createFloat(String str) {
        if (str == null) {
            return null;
        }
        return Float.valueOf(str);
    }

    /**
     * <p>Convert a <code>String</code> to a <code>Double</code>.</p>
     * 
     * <p>Returns <code>null</code> if the string is <code>null</code>.</p>
     *
     * @param str  a <code>String</code> to convert, may be null
     * @return converted <code>Double</code>
     * @throws NumberFormatException if the value cannot be converted
     */
    public static Double createDouble(String str) {
        if (str == null) {
            return null;
        }
        return Double.valueOf(str);
    }

    /**
     * <p>Convert a <code>String</code> to a <code>Integer</code>, handling
     * hex and octal notations.</p>
     *
     * <p>Returns <code>null</code> if the string is <code>null</code>.</p>
     * 
     * @param str  a <code>String</code> to convert, may be null
     * @return converted <code>Integer</code>
     * @throws NumberFormatException if the value cannot be converted
     */
    public static Integer createInteger(String str) {
        // decode() handles 0xAABD and 0777 (hex and octal) as well.
        if (str == null) {
            return null;
        }
        return Integer.decode(str);
    }

    /**
     * <p>Convert a <code>String</code> to a <code>Long</code>.</p>
     * 
     * <p>Returns <code>null</code> if the string is <code>null</code>.</p>
     *
     * @param str  a <code>String</code> to convert, may be null
     * @return converted <code>Long</code>
     * @throws NumberFormatException if the value cannot be converted
     */
    public static Long createLong(String str) {
        if (str == null) {
            return null;
        }
        return Long.valueOf(str);
    }

    /**
     * <p>Convert a <code>String</code> to a <code>BigInteger</code>.</p>
     *
     * <p>Returns <code>null</code> if the string is <code>null</code>.</p>
     * 
     * @param str  a <code>String</code> to convert, may be null
     * @return converted <code>BigInteger</code>
     * @throws NumberFormatException if the value cannot be converted
     */
    public static BigInteger createBigInteger(String str) {
        if (str == null) {
            return null;
        }
        return new BigInteger(str);
    }

    /**
     * <p>Convert a <code>String</code> to a <code>BigDecimal</code>.</p>
     * 
     * <p>Returns <code>null</code> if the string is <code>null</code>.</p>
     *
     * @param str  a <code>String</code> to convert, may be null
     * @return converted <code>BigDecimal</code>
     * @throws NumberFormatException if the value cannot be converted
     */
    public static BigDecimal createBigDecimal(String str) {
        if (str == null) {
            return null;
        }
        return new BigDecimal(str);
    }

    // Min in array
    //--------------------------------------------------------------------
    /**
     * <p>Returns the minimum value in an array.</p>
     * 
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws NullArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
     */
    public static long min(long[] array) {
        // Validates input
        if (array == null) {
            throw new NullArgumentException("Array");
        } else if (array.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }
    
        // Finds and returns min
        long min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }
    
        return min;
    }

    /**
     * <p>Returns the minimum value in an array.</p>
     * 
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws NullArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
     */
    public static int min(int[] array) {
        // Validates input
        if (array == null) {
            throw new NullArgumentException("Array");
        } else if (array.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }
    
        // Finds and returns min
        int min = array[0];
        for (int j = 1; j < array.length; j++) {
            if (array[j] < min) {
                min = array[j];
            }
        }
    
        return min;
    }

    /**
     * <p>Returns the minimum value in an array.</p>
     * 
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws NullArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
     */
    public static short min(short[] array) {
        // Validates input
        if (array == null) {
            throw new NullArgumentException("Array");
        } else if (array.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }
    
        // Finds and returns min
        short min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }
    
        return min;
    }

     /**
     * <p>Returns the minimum value in an array.</p>
     * 
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws NullArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
     */
    public static double min(double[] array) {
        // Validates input
        if (array == null) {
            throw new NullArgumentException("Array");
        } else if (array.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }
    
        // Finds and returns min
        double min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }
    
        return min;
    }

    /**
     * <p>Returns the minimum value in an array.</p>
     * 
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws NullArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
     */
    public static float min(float[] array) {
        // Validates input
        if (array == null) {
            throw new NullArgumentException("Array");
        } else if (array.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }
    
        // Finds and returns min
        float min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }
    
        return min;
    }

    // Max in array
    //--------------------------------------------------------------------
    /**
     * <p>Returns the maximum value in an array.</p>
     * 
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws NullArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
     */
    public static long max(long[] array) {
        // Validates input
        if (array == null) {
            throw new NullArgumentException("Array");
        } else if (array.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }

        // Finds and returns max
        long max = array[0];
        for (int j = 1; j < array.length; j++) {
            if (array[j] > max) {
                max = array[j];
            }
        }

        return max;
    }

    /**
     * <p>Returns the maximum value in an array.</p>
     * 
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws NullArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
     */
    public static int max(int[] array) {
        // Validates input
        if (array == null) {
            throw new NullArgumentException("Array");
        } else if (array.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }
    
        // Finds and returns max
        int max = array[0];
        for (int j = 1; j < array.length; j++) {
            if (array[j] > max) {
                max = array[j];
            }
        }
    
        return max;
    }

    /**
     * <p>Returns the maximum value in an array.</p>
     * 
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws NullArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
     */
    public static short max(short[] array) {
        // Validates input
        if (array == null) {
            throw new NullArgumentException("Array");
        } else if (array.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }
    
        // Finds and returns max
        short max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
    
        return max;
    }

    /**
     * <p>Returns the maximum value in an array.</p>
     * 
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws NullArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
     */
    public static double max(double[] array) {
        // Validates input
        if (array== null) {
            throw new NullArgumentException("Array");
        } else if (array.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }
    
        // Finds and returns max
        double max = array[0];
        for (int j = 1; j < array.length; j++) {
            if (array[j] > max) {
                max = array[j];
            }
        }
    
        return max;
    }

    /**
     * <p>Returns the maximum value in an array.</p>
     * 
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws NullArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
     */
    public static float max(float[] array) {
        // Validates input
        if (array == null) {
            throw new NullArgumentException("Array");
        } else if (array.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }

        // Finds and returns max
        float max = array[0];
        for (int j = 1; j < array.length; j++) {
            if (array[j] > max) {
                max = array[j];
            }
        }

        return max;
    }
     
    // 3 param min
    //-----------------------------------------------------------------------
    /**
     * <p>Gets the minimum of three <code>long</code> values.</p>
     * 
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the smallest of the values
     */
    public static long min(long a, long b, long c) {
        if (b < a) {
            a = b;
        }
        if (c < a) {
            a = c;
        }
        return a;
    }

    /**
     * <p>Gets the minimum of three <code>int</code> values.</p>
     * 
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the smallest of the values
     */
    public static int min(int a, int b, int c) {
        if (b < a) {
            a = b;
        }
        if (c < a) {
            a = c;
        }
        return a;
    }

    /**
     * <p>Gets the minimum of three <code>short</code> values.</p>
     * 
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the smallest of the values
     */
    public static short min(short a, short b, short c) {
        if (b < a) {
            a = b;
        }
        if (c < a) {
            a = c;
        }
        return a;
    }

    /**
     * <p>Gets the minimum of three <code>byte</code> values.</p>
     * 
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the smallest of the values
     */
    public static byte min(byte a, byte b, byte c) {
        if (b < a) {
            a = b;
        }
        if (c < a) {
            a = c;
        }
        return a;
    }

    /**
     * <p>Gets the minimum of three <code>double</code> values.</p>
     * 
     * <p>If any value is <code>NaN</code>, <code>NaN</code> is
     * returned. Infinity is handled.</p>
     * 
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the smallest of the values
     */
    public static double min(double a, double b, double c) {
        return Math.min(Math.min(a, b), c);
    }

    /**
     * <p>Gets the minimum of three <code>float</code> values.</p>
     * 
     * <p>If any value is <code>NaN</code>, <code>NaN</code> is
     * returned. Infinity is handled.</p>
     *
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the smallest of the values
     */
    public static float min(float a, float b, float c) {
        return Math.min(Math.min(a, b), c);
    }

    // 3 param max
    //-----------------------------------------------------------------------
    /**
     * <p>Gets the maximum of three <code>long</code> values.</p>
     * 
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the largest of the values
     */
    public static long max(long a, long b, long c) {
        if (b > a) {
            a = b;
        }
        if (c > a) {
            a = c;
        }
        return a;
    }

    /**
     * <p>Gets the maximum of three <code>int</code> values.</p>
     * 
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the largest of the values
     */
    public static int max(int a, int b, int c) {
        if (b > a) {
            a = b;
        }
        if (c > a) {
            a = c;
        }
        return a;
    }

    /**
     * <p>Gets the maximum of three <code>short</code> values.</p>
     * 
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the largest of the values
     */
    public static short max(short a, short b, short c) {
        if (b > a) {
            a = b;
        }
        if (c > a) {
            a = c;
        }
        return a;
    }

    /**
     * <p>Gets the maximum of three <code>byte</code> values.</p>
     * 
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the largest of the values
     */
    public static byte max(byte a, byte b, byte c) {
        if (b > a) {
            a = b;
        }
        if (c > a) {
            a = c;
        }
        return a;
    }

    /**
     * <p>Gets the maximum of three <code>double</code> values.</p>
     * 
     * <p>If any value is <code>NaN</code>, <code>NaN</code> is
     * returned. Infinity is handled.</p>
     *
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the largest of the values
     */
    public static double max(double a, double b, double c) {
        return Math.max(Math.max(a, b), c);
    }

    /**
     * <p>Gets the maximum of three <code>float</code> values.</p>
     * 
     * <p>If any value is <code>NaN</code>, <code>NaN</code> is
     * returned. Infinity is handled.</p>
     *
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the largest of the values
     */
    public static float max(float a, float b, float c) {
        return Math.max(Math.max(a, b), c);
    }

    //-----------------------------------------------------------------------
    /**
     * <p>Compares two <code>doubles</code> for order.</p>
     *
     * <p>This method is more comprehensive than the standard Java greater
     * than, less than and equals operators.</p>
     * <ul>
     *  <li>It returns <code>-1</code> if the first value is less than the second.</li>
     *  <li>It returns <code>+1</code> if the first value is greater than the second.</li>
     *  <li>It returns <code>0</code> if the values are equal.</li>
     * </ul>
     *
     * <p>
     * The ordering is as follows, largest to smallest:
     * <ul>
     *  <li>NaN
     *  <li>Positive infinity
     *  <li>Maximum double
     *  <li>Normal positve numbers
     *  <li>+0.0
     *  <li>-0.0
     *  <li>Normal negative numbers
     *  <li>Minimum double (<code>-Double.MAX_VALUE</code>)
     *  <li>Negative infinity
     * </ul>
     * </p>
     *
     * <p>Comparing <code>NaN</code> with <code>NaN</code> will
     * return <code>0</code>.</p>
     * 
     * @param lhs  the first <code>double</code>
     * @param rhs  the second <code>double</code>
     * @return <code>-1</code> if lhs is less, <code>+1</code> if greater,
     *  <code>0</code> if equal to rhs
     */
    public static int compare(double lhs, double rhs) {
        if (lhs < rhs) {
            return -1;
        }
        if (lhs > rhs) {
            return +1;
        }
        // Need to compare bits to handle 0.0 == -0.0 being true
        // compare should put -0.0 < +0.0
        // Two NaNs are also == for compare purposes
        // where NaN == NaN is false
        long lhsBits = Double.doubleToLongBits(lhs);
        long rhsBits = Double.doubleToLongBits(rhs);
        if (lhsBits == rhsBits) {
            return 0;
        }
        // Something exotic! A comparison to NaN or 0.0 vs -0.0
        // Fortunately NaN's long is > than everything else
        // Also negzeros bits < poszero
        // NAN: 9221120237041090560
        // MAX: 9218868437227405311
        // NEGZERO: -9223372036854775808
        if (lhsBits < rhsBits) {
            return -1;
        } else {
            return +1;
        }
    }
    
    /**
     * <p>Compares two floats for order.</p>
     *
     * <p>This method is more comprhensive than the standard Java greater than,
     * less than and equals operators.</p>
     * <ul>
     *  <li>It returns <code>-1</code> if the first value is less than the second.
     *  <li>It returns <code>+1</code> if the first value is greater than the second.
     *  <li>It returns <code>0</code> if the values are equal.
     * </ul>
     *
     * <p> The ordering is as follows, largest to smallest:
     * <ul>
     * <li>NaN
     * <li>Positive infinity
     * <li>Maximum float
     * <li>Normal positve numbers
     * <li>+0.0
     * <li>-0.0
     * <li>Normal negative numbers
     * <li>Minimum float (<code>-Float.MAX_VALUE</code>)
     * <li>Negative infinity
     * </ul>
     *
     * <p>Comparing <code>NaN</code> with <code>NaN</code> will return
     * <code>0</code>.</p>
     * 
     * @param lhs  the first <code>float</code>
     * @param rhs  the second <code>float</code>
     * @return <code>-1</code> if lhs is less, <code>+1</code> if greater,
     *  <code>0</code> if equal to rhs
     */
    public static int compare(float lhs, float rhs) {
        if (lhs < rhs) {
            return -1;
        }
        if (lhs > rhs) {
            return +1;
        }
        //Need to compare bits to handle 0.0 == -0.0 being true
        // compare should put -0.0 < +0.0
        // Two NaNs are also == for compare purposes
        // where NaN == NaN is false
        int lhsBits = Float.floatToIntBits(lhs);
        int rhsBits = Float.floatToIntBits(rhs);
        if (lhsBits == rhsBits) {
            return 0;
        }
        //Something exotic! A comparison to NaN or 0.0 vs -0.0
        //Fortunately NaN's int is > than everything else
        //Also negzeros bits < poszero
        //NAN: 2143289344
        //MAX: 2139095039
        //NEGZERO: -2147483648
        if (lhsBits < rhsBits) {
            return -1;
        } else {
            return +1;
        }
    }
    
    //-----------------------------------------------------------------------
    /**
     * <p>Checks whether the <code>String</code> contains only
     * digit characters.</p>
     *
     * <p><code>Null</code> and empty String will return
     * <code>false</code>.</p>
     *
     * @param str  the <code>String</code> to check
     * @return <code>true</code> if str contains only unicode numeric
     */
    public static boolean isDigits(String str) {
        if ((str == null) || (str.length() == 0)) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Checks whether the String a valid Java number.</p>
     *
     * <p>Valid numbers include hexadecimal marked with the <code>0x</code>
     * qualifier, scientific notation and numbers marked with a type
     * qualifier (e.g. 123L).</p>
     *
     * <p><code>Null</code> and empty String will return
     * <code>false</code>.</p>
     *
     * @param str  the <code>String</code> to check
     * @return <code>true</code> if the string is a correctly formatted number
     */
    public static boolean isNumber(String str) {
        if ((str == null) || (str.length() == 0)) {
            return false;
        }
        char[] chars = str.toCharArray();
        int sz = chars.length;
        boolean hasExp = false;
        boolean hasDecPoint = false;
        boolean allowSigns = false;
        boolean foundDigit = false;
        // deal with any possible sign up front
        int start = (chars[0] == '-') ? 1 : 0;
        if (sz > start + 1) {
            if (chars[start] == '0' && chars[start + 1] == 'x') {
                int i = start + 2;
                if (i == sz) {
                    return false; // str == "0x"
                }
                // checking hex (it can't be anything else)
                for (; i < chars.length; i++) {
                    if ((chars[i] < '0' || chars[i] > '9')
                        && (chars[i] < 'a' || chars[i] > 'f')
                        && (chars[i] < 'A' || chars[i] > 'F')) {
                        return false;
                    }
                }
                return true;
            }
        }
        sz--; // don't want to loop to the last char, check it afterwords
              // for type qualifiers
        int i = start;
        // loop to the next to last char or to the last char if we need another digit to
        // make a valid number (e.g. chars[0..5] = "1234E")
        while (i < sz || (i < sz + 1 && allowSigns && !foundDigit)) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                foundDigit = true;
                allowSigns = false;

            } else if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // two decimal points or dec in exponent   
                    return false;
                }
                hasDecPoint = true;
            } else if (chars[i] == 'e' || chars[i] == 'E') {
                // we've already taken care of hex.
                if (hasExp) {
                    // two E's
                    return false;
                }
                if (!foundDigit) {
                    return false;
                }
                hasExp = true;
                allowSigns = true;
            } else if (chars[i] == '+' || chars[i] == '-') {
                if (!allowSigns) {
                    return false;
                }
                allowSigns = false;
                foundDigit = false; // we need a digit after the E
            } else {
                return false;
            }
            i++;
        }
        if (i < chars.length) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                // no type qualifier, OK
                return true;
            }
            if (chars[i] == 'e' || chars[i] == 'E') {
                // can't have an E at the last byte
                return false;
            }
            if (!allowSigns
                && (chars[i] == 'd'
                    || chars[i] == 'D'
                    || chars[i] == 'f'
                    || chars[i] == 'F')) {
                return foundDigit;
            }
            if (chars[i] == 'l'
                || chars[i] == 'L') {
                // not allowing L with an exponoent
                return foundDigit && !hasExp;
            }
            // last character is illegal
            return false;
        }
        // allowSigns is true iff the val ends in 'E'
        // found digit it to make sure weird stuff like '.' and '1E-' doesn't pass
        return !allowSigns && foundDigit;
    }
    
}
