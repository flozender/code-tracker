package org.apache.commons.lang;

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

import java.math.BigInteger;
import java.math.BigDecimal;
/**
 * Provides extra functionality for java Number classes.
 *
 * @author <a href="mailto:bayard@generationjava.com">Henri Yandell</a>
 * @author <a href="mailto:rand_mcneely@yahoo.com">Rand McNeely</a>
 * @author <a href="mailto:scolebourne@joda.org">Stephen Colebourne</a>
 * @version $Id: NumberUtils.java,v 1.1 2002/07/19 03:35:54 bayard Exp $
 */
public final class NumberUtils {

    /**
     * Convert a String to an int, returning zero if the conversion fails
     * 
     * @param str  the string to convert
     * @return the int represented by the string, or zero if conversion fails
     */
    public static int stringToInt(String str) {
        return stringToInt(str, 0);
    }

    /**
     * Convert a String to an int, returning a default value if the 
     * conversion fails.
     * 
     * @param str  the string to convert
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
     * <p>
     * Turns a string value into a java.lang.Number.
     * First, the value is examined for a type qualifier on the end 
     * (<code>'f','F','d','D','l','L'</code>).  If it is found, it starts 
     * trying to create succissively larger types from the type specified
     * until one is found that can hold the value.
     * </p>
     * <p>
     * If a type specifier is not found, it will check for a decimal point
     * and then try successively larger types from Integer to BigInteger 
     * and from Float to BigDecimal.
     * </p>
     * <p>
     * If the string starts with "0x" or "-0x", it will be interpreted as a 
     * hexadecimal integer.  Values with leading 0's will not be interpreted 
     * as octal.
     * </p>
     * 
     * @param val String containing a number
     * @return Number created from the string
     * @throws NumberFormatException if the value cannot be converted
     */
    public static Number createNumber(String val) throws NumberFormatException {
        if (val == null) {
            return null;
        }
        if (val.length() == 0) {
            throw new NumberFormatException("\"\" is not a valid number.");
        }
        if (val.startsWith("--")) {
            // this is protection for poorness in java.lang.BigDecimal.
            // it accepts this as a legal value, but it does not appear 
            // to be in specification of class. OS X Java parses it to 
            // a wrong value.
            return null;
        }
        if (val.startsWith("0x") || val.startsWith("-0x")) {
            return createInteger(val);
        }   
        char lastChar = val.charAt(val.length() - 1);
        String mant;
        String dec;
        String exp;
        int decPos = val.indexOf('.');
        int expPos = val.indexOf('e') + val.indexOf('E') + 1;

        if (decPos > -1) {

            if (expPos > -1) {
                if (expPos < decPos) {
                    throw new NumberFormatException(val + " is not a valid number.");
                }
                dec = val.substring(decPos + 1, expPos);
            } else {
                dec = val.substring(decPos + 1);
            }
            mant = val.substring(0, decPos);
        } else {
            if (expPos > -1) {
                mant = val.substring(0, expPos);
            } else {
                mant = val;
            }
            dec = null;
        }
        if (!Character.isDigit(lastChar)) {
            if (expPos > -1 && expPos < val.length() - 1) {
                exp = val.substring(expPos + 1, val.length() - 1);
            } else {
                exp = null;
            }
            //Requesting a specific type..
            String numeric = val.substring(0, val.length() - 1);
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
                    throw new NumberFormatException(val + " is not a valid number.");
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
                    throw new NumberFormatException(val + " is not a valid number.");

            }
        } else {
            //User doesn't have a preference on the return type, so let's start
            //small and go from there...
            if (expPos > -1 && expPos < val.length() - 1) {
                exp = val.substring(expPos + 1, val.length());
            } else {
                exp = null;
            }
            if (dec == null && exp == null) {
                //Must be an int,long,bigint
                try {
                    return createInteger(val);
                } catch (NumberFormatException nfe) {
                }
                try {
                    return createLong(val);
                } catch (NumberFormatException nfe) {
                }
                return createBigInteger(val);

            } else {
                //Must be a float,double,BigDec
                boolean allZeros = isAllZeros(mant) && isAllZeros(exp);
                try {
                    Float f = createFloat(val);
                    if (!(f.isInfinite() || (f.floatValue() == 0.0F && !allZeros))) {
                        return f;
                    }
                } catch (NumberFormatException nfe) {
                }
                try {
                    Double d = createDouble(val);
                    if (!(d.isInfinite() || (d.doubleValue() == 0.0D && !allZeros))) {
                        return d;
                    }
                } catch (NumberFormatException nfe) {
                }

                return createBigDecimal(val);

            }

        }
    }

    /**
     * Utility method for createNumber.  Returns true if s is null
     * 
     * @param s the String to check
     * @return if it is all zeros or null
     */
    private static boolean isAllZeros(String s) {
        if (s == null) {
            return true;
        }
        for (int i = s.length() - 1; i >= 0; i--) {
            if (s.charAt(i) != '0') {
                return false;
            }
        }
        return s.length() > 0;
    }

    /**
     * Convert a String to a Float
     * 
     * @param val  a String to convert
     * @return converted Float
     * @throws NumberFormatException if the value cannot be converted
     */
    public static Float createFloat(String val) {
        return Float.valueOf(val);
    }

    /**
     * Convert a String to a Double
     * 
     * @param val  a String to convert
     * @return converted Double
     * @throws NumberFormatException if the value cannot be converted
     */
    public static Double createDouble(String val) {
        return Double.valueOf(val);
    }

    /**
     * Convert a String to a Integer, handling hex and
     * octal notations.
     * 
     * @param val  a String to convert
     * @return converted Integer
     * @throws NumberFormatException if the value cannot be converted
     */
    public static Integer createInteger(String val) {
        // decode() handles 0xAABD and 0777 (hex and octal) as well.
        return Integer.decode(val);
    }

    /**
     * Convert a String to a Long
     * 
     * @param val  a String to convert
     * @return converted Long
     * @throws NumberFormatException if the value cannot be converted
     */
    public static Long createLong(String val) {
        return Long.valueOf(val);
    }

    /**
     * Convert a String to a BigInteger
     * 
     * @param val  a String to convert
     * @return converted BigInteger
     * @throws NumberFormatException if the value cannot be converted
     */
    public static BigInteger createBigInteger(String val) {
        BigInteger bi = new BigInteger(val);
        return bi;
    }

    /**
     * Convert a String to a BigDecimal
     * 
     * @param val  a String to convert
     * @return converted BigDecimal
     * @throws NumberFormatException if the value cannot be converted
     */
    public static BigDecimal createBigDecimal(String val) {
        BigDecimal bd = new BigDecimal(val);
        return bd;
    }

    /**
     * Get the minimum of three values.
     */
    public static int minimum(int a, int b, int c) {
        if (b < a) {
            a = b;
        }
        if (c < a) {
            a = c;
        }
        return a;
    }

    /**
     * Get the maximum of three values.
     */
    public static int maximum(int a, int b, int c) {
        if (b > a) {
            a = b;
        }
        if (c > a) {
            a = c;
        }
        return a;
    }

    /**
     * Checks whether the String contains only digit characters.
     * Null and blank string will return false.
     *
     * @param str  the string to check
     * @return boolean contains only unicode numeric
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
     * <p>
     * Checks whether the String a valid Java number.
     * Valid numbers include hexadecimal marked with the "0x" qualifier,
     * scientific notation and numbers marked with a type qualifier (e.g. 123L).
     * </p>
     * <p>
     * Null and blank string will return false.
     * </p>
     * 
     * @param str  the string to check
     * @return true if the string is a correctly formatted number
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
        //Deal with any possible sign up front
        int start = (chars[0] == '-') ? 1 : 0;
        if (sz > start + 1) {
            if (chars[start] == '0' && chars[start + 1] == 'x') {
                int i = start + 2;
                if (i == sz) {
                    return false; // str == "0x"
                }
                //Checking hex (it can't be anything else)
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
        sz--; //Don't want to loop to the last char, check it afterwords
              //for type qualifiers
        int i = start;
        //Loop to the next to last char or to the last char if we need another digit to
        //make a valid number (e.g. chars[0..5] = "1234E")
        while (i < sz || (i < sz + 1 && allowSigns && !foundDigit)) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                foundDigit = true;
                allowSigns = false;

            } else if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    //Two decimal points or dec in exponent   
                    return false;
                }
                hasDecPoint = true;
            } else if (chars[i] == 'e' || chars[i] == 'E') {
                //We've already taken care of hex.
                if (hasExp) {
                    //Two E's
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
                foundDigit = false; //We need a digit after the E
            } else {
                return false;
            }
            i++;
        }
        if (i < chars.length) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                //No type qualifier, OK
                return true;
            }
            if (chars[i] == 'e' || chars[i] == 'E') {
                //Can't have an E at the last byte
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
                //Not allowing L with an exponoent
                return foundDigit && !hasExp;
            }
        }
        //allowSigns is true iff the val ends in 'E'
        //Found digit it to make sure weird stuff like '.' and '1E-' doesn't pass
        return !allowSigns && foundDigit;
    }
}
