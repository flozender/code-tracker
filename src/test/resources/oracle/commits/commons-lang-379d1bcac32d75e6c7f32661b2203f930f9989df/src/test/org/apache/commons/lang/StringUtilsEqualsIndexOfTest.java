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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Unit tests {@link org.apache.commons.lang.StringUtils} - Substring methods
 *
 * @author <a href="mailto:scolebourne@joda.org">Stephen Colebourne</a>
 * @author <a href="mailto:ridesmet@users.sourceforge.net">Ringo De Smet</a>
 * @author Phil Steitz
 * @version $Id: StringUtilsEqualsIndexOfTest.java,v 1.8 2003/08/18 02:22:25 bayard Exp $
 */
public class StringUtilsEqualsIndexOfTest extends TestCase {
    private static final String FOO = "foo";
    private static final String BAR = "bar";
    private static final String FOOBAR = "foobar";
    private static final String[] FOOBAR_SUB_ARRAY = new String[] {"ob", "ba"};

    public StringUtilsEqualsIndexOfTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
    	TestSuite suite = new TestSuite(StringUtilsEqualsIndexOfTest.class);
    	suite.setName("StringUtilsEqualsIndexOf Tests");
        return suite;
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    //-----------------------------------------------------------------------

    public void testEquals() {
        assertEquals(true, StringUtils.equals(null, null));
        assertEquals(true, StringUtils.equals(FOO, FOO));
        assertEquals(true, StringUtils.equals(FOO, new String(new char[] { 'f', 'o', 'o' })));
        assertEquals(false, StringUtils.equals(FOO, new String(new char[] { 'f', 'O', 'O' })));
        assertEquals(false, StringUtils.equals(FOO, BAR));
        assertEquals(false, StringUtils.equals(FOO, null));
        assertEquals(false, StringUtils.equals(null, FOO));
    }

    public void testEqualsIgnoreCase() {
        assertEquals(true, StringUtils.equalsIgnoreCase(null, null));
        assertEquals(true, StringUtils.equalsIgnoreCase(FOO, FOO));
        assertEquals(true, StringUtils.equalsIgnoreCase(FOO, new String(new char[] { 'f', 'o', 'o' })));
        assertEquals(true, StringUtils.equalsIgnoreCase(FOO, new String(new char[] { 'f', 'O', 'O' })));
        assertEquals(false, StringUtils.equalsIgnoreCase(FOO, BAR));
        assertEquals(false, StringUtils.equalsIgnoreCase(FOO, null));
        assertEquals(false, StringUtils.equalsIgnoreCase(null, FOO));
    }

    //-----------------------------------------------------------------------
    public void testIndexOf_char() {
        assertEquals(-1, StringUtils.indexOf(null, ' '));
        assertEquals(-1, StringUtils.indexOf("", ' '));
        assertEquals(0, StringUtils.indexOf("aabaabaa", 'a'));
        assertEquals(2, StringUtils.indexOf("aabaabaa", 'b'));
    }

    public void testIndexOf_charInt() {
        assertEquals(-1, StringUtils.indexOf(null, ' ', 0));
        assertEquals(-1, StringUtils.indexOf(null, ' ', -1));
        assertEquals(-1, StringUtils.indexOf("", ' ', 0));
        assertEquals(-1, StringUtils.indexOf("", ' ', -1));
        assertEquals(0, StringUtils.indexOf("aabaabaa", 'a', 0));
        assertEquals(2, StringUtils.indexOf("aabaabaa", 'b', 0));
        assertEquals(5, StringUtils.indexOf("aabaabaa", 'b', 3));
        assertEquals(-1, StringUtils.indexOf("aabaabaa", 'b', 9));
        assertEquals(2, StringUtils.indexOf("aabaabaa", 'b', -1));
    }

    public void testIndexOf_String() {
        assertEquals(-1, StringUtils.indexOf(null, null));
        assertEquals(-1, StringUtils.indexOf("", null));
        assertEquals(0, StringUtils.indexOf("", ""));
        assertEquals(0, StringUtils.indexOf("aabaabaa", "a"));
        assertEquals(2, StringUtils.indexOf("aabaabaa", "b"));
        assertEquals(1, StringUtils.indexOf("aabaabaa", "ab"));
        assertEquals(0, StringUtils.indexOf("aabaabaa", ""));
    }

    public void testIndexOf_StringInt() {
        assertEquals(-1, StringUtils.indexOf(null, null, 0));
        assertEquals(-1, StringUtils.indexOf(null, null, -1));
        assertEquals(-1, StringUtils.indexOf(null, "", 0));
        assertEquals(-1, StringUtils.indexOf(null, "", -1));
        assertEquals(-1, StringUtils.indexOf("", null, 0));
        assertEquals(-1, StringUtils.indexOf("", null, -1));
        assertEquals(0, StringUtils.indexOf("", "", 0));
        assertEquals(0, StringUtils.indexOf("", "", -1));
        assertEquals(0, StringUtils.indexOf("", "", 9));
        assertEquals(0, StringUtils.indexOf("abc", "", 0));
        assertEquals(0, StringUtils.indexOf("abc", "", -1));
        assertEquals(3, StringUtils.indexOf("abc", "", 9));
        assertEquals(3, StringUtils.indexOf("abc", "", 3));
        assertEquals(0, StringUtils.indexOf("aabaabaa", "a", 0));
        assertEquals(2, StringUtils.indexOf("aabaabaa", "b", 0));
        assertEquals(1, StringUtils.indexOf("aabaabaa", "ab", 0));
        assertEquals(5, StringUtils.indexOf("aabaabaa", "b", 3));
        assertEquals(-1, StringUtils.indexOf("aabaabaa", "b", 9));
        assertEquals(2, StringUtils.indexOf("aabaabaa", "b", -1));
        assertEquals(2,StringUtils.indexOf("aabaabaa", "", 2)); 
    }

    //-----------------------------------------------------------------------
    public void testLastIndexOf_char() {
        assertEquals(-1, StringUtils.lastIndexOf(null, ' '));
        assertEquals(-1, StringUtils.lastIndexOf("", ' '));
        assertEquals(7, StringUtils.lastIndexOf("aabaabaa", 'a'));
        assertEquals(5, StringUtils.lastIndexOf("aabaabaa", 'b'));
    }

    public void testLastIndexOf_charInt() {
        assertEquals(-1, StringUtils.lastIndexOf(null, ' ', 0));
        assertEquals(-1, StringUtils.lastIndexOf(null, ' ', -1));
        assertEquals(-1, StringUtils.lastIndexOf("", ' ', 0));
        assertEquals(-1, StringUtils.lastIndexOf("", ' ', -1));
        assertEquals(7, StringUtils.lastIndexOf("aabaabaa", 'a', 8));
        assertEquals(5, StringUtils.lastIndexOf("aabaabaa", 'b', 8));
        assertEquals(2, StringUtils.lastIndexOf("aabaabaa", 'b', 3));
        assertEquals(5, StringUtils.lastIndexOf("aabaabaa", 'b', 9));
        assertEquals(-1, StringUtils.lastIndexOf("aabaabaa", 'b', -1));
        assertEquals(0, StringUtils.lastIndexOf("aabaabaa", 'a', 0));
    }

    public void testLastIndexOf_String() {
        assertEquals(-1, StringUtils.lastIndexOf(null, null));
        assertEquals(-1, StringUtils.lastIndexOf("", null));
        assertEquals(-1, StringUtils.lastIndexOf("", "a"));
        assertEquals(0, StringUtils.lastIndexOf("", ""));
        assertEquals(8, StringUtils.lastIndexOf("aabaabaa", ""));
        assertEquals(7, StringUtils.lastIndexOf("aabaabaa", "a"));
        assertEquals(5, StringUtils.lastIndexOf("aabaabaa", "b"));
        assertEquals(4, StringUtils.lastIndexOf("aabaabaa", "ab"));
    }

    public void testLastIndexOf_StringInt() {
        assertEquals(-1, StringUtils.lastIndexOf(null, null, 0));
        assertEquals(-1, StringUtils.lastIndexOf(null, null, -1));
        assertEquals(-1, StringUtils.lastIndexOf(null, "", 0));
        assertEquals(-1, StringUtils.lastIndexOf(null, "", -1));
        assertEquals(-1, StringUtils.lastIndexOf("", null, 0));
        assertEquals(-1, StringUtils.lastIndexOf("", null, -1));
        assertEquals(0, StringUtils.lastIndexOf("", "", 0));
        assertEquals(-1, StringUtils.lastIndexOf("", "", -1));
        assertEquals(0, StringUtils.lastIndexOf("", "", 9));
        assertEquals(0, StringUtils.lastIndexOf("abc", "", 0));
        assertEquals(-1, StringUtils.lastIndexOf("abc", "", -1));
        assertEquals(3, StringUtils.lastIndexOf("abc", "", 9));
        assertEquals(7, StringUtils.lastIndexOf("aabaabaa", "a", 8));
        assertEquals(5, StringUtils.lastIndexOf("aabaabaa", "b", 8));
        assertEquals(4, StringUtils.lastIndexOf("aabaabaa", "ab", 8));
        assertEquals(2, StringUtils.lastIndexOf("aabaabaa", "b", 3));
        assertEquals(5, StringUtils.lastIndexOf("aabaabaa", "b", 9));
        assertEquals(-1, StringUtils.lastIndexOf("aabaabaa", "b", -1));
        assertEquals(-1, StringUtils.lastIndexOf("aabaabaa", "b", 0));
        assertEquals(0, StringUtils.lastIndexOf("aabaabaa", "a", 0));
    }

    //-----------------------------------------------------------------------
    public void testContainsChar() {
        assertEquals(false, StringUtils.contains(null, ' '));
        assertEquals(false, StringUtils.contains("", ' '));
        assertEquals(false, StringUtils.contains("",null));
        assertEquals(false, StringUtils.contains(null,null));
        assertEquals(true, StringUtils.contains("abc", 'a'));
        assertEquals(true, StringUtils.contains("abc", 'b'));
        assertEquals(true, StringUtils.contains("abc", 'c'));
        assertEquals(false, StringUtils.contains("abc", 'z'));
    }

    public void testContainsString() {
        assertEquals(false, StringUtils.contains(null, null));
        assertEquals(false, StringUtils.contains(null, ""));
        assertEquals(false, StringUtils.contains(null, "a"));
        assertEquals(false, StringUtils.contains("", null));
        assertEquals(true, StringUtils.contains("", ""));
        assertEquals(false, StringUtils.contains("", "a"));
        assertEquals(true, StringUtils.contains("abc", "a"));
        assertEquals(true, StringUtils.contains("abc", "b"));
        assertEquals(true, StringUtils.contains("abc", "c"));
        assertEquals(true, StringUtils.contains("abc", "abc"));
        assertEquals(false, StringUtils.contains("abc", "z"));
    }

    //-----------------------------------------------------------------------
    public void testIndexOfAny_StringStringarray() {
        assertEquals(-1, StringUtils.indexOfAny(null, (String[]) null));
        assertEquals(-1, StringUtils.indexOfAny(null, FOOBAR_SUB_ARRAY));
        assertEquals(-1, StringUtils.indexOfAny(FOOBAR, (String[]) null));
        assertEquals(2, StringUtils.indexOfAny(FOOBAR, FOOBAR_SUB_ARRAY));
        assertEquals(-1, StringUtils.indexOfAny(FOOBAR, new String[0]));
        assertEquals(-1, StringUtils.indexOfAny(null, new String[0]));
        assertEquals(-1, StringUtils.indexOfAny("", new String[0]));
        assertEquals(-1, StringUtils.indexOfAny(FOOBAR, new String[] {"llll"}));
        assertEquals(0, StringUtils.indexOfAny(FOOBAR, new String[] {""}));
        assertEquals(0, StringUtils.indexOfAny("", new String[] {""}));
        assertEquals(-1, StringUtils.indexOfAny("", new String[] {"a"}));
        assertEquals(-1, StringUtils.indexOfAny("", new String[] {null}));
        assertEquals(-1, StringUtils.indexOfAny(FOOBAR, new String[] {null}));
        assertEquals(-1, StringUtils.indexOfAny(null, new String[] {null}));
    }

    public void testLastIndexOfAny_StringStringarray() {
        assertEquals(-1, StringUtils.lastIndexOfAny(null, null));
        assertEquals(-1, StringUtils.lastIndexOfAny(null, FOOBAR_SUB_ARRAY));
        assertEquals(-1, StringUtils.lastIndexOfAny(FOOBAR, null));
        assertEquals(3, StringUtils.lastIndexOfAny(FOOBAR, FOOBAR_SUB_ARRAY));
        assertEquals(-1, StringUtils.lastIndexOfAny(FOOBAR, new String[0]));
        assertEquals(-1, StringUtils.lastIndexOfAny(null, new String[0]));
        assertEquals(-1, StringUtils.lastIndexOfAny("", new String[0]));
        assertEquals(-1, StringUtils.lastIndexOfAny(FOOBAR, new String[] {"llll"}));
        assertEquals(6, StringUtils.lastIndexOfAny(FOOBAR, new String[] {""}));
        assertEquals(0, StringUtils.lastIndexOfAny("", new String[] {""}));
        assertEquals(-1, StringUtils.lastIndexOfAny("", new String[] {"a"}));
        assertEquals(-1, StringUtils.lastIndexOfAny("", new String[] {null}));
        assertEquals(-1, StringUtils.lastIndexOfAny(FOOBAR, new String[] {null}));
        assertEquals(-1, StringUtils.lastIndexOfAny(null, new String[] {null}));
    }

    //-----------------------------------------------------------------------
    public void testIndexOfAny_StringChararray() {
        assertEquals(-1, StringUtils.indexOfAny(null, (char[]) null));
        assertEquals(-1, StringUtils.indexOfAny(null, new char[0]));
        assertEquals(-1, StringUtils.indexOfAny(null, new char[] {'a','b'}));
        
        assertEquals(-1, StringUtils.indexOfAny("", (char[]) null));
        assertEquals(-1, StringUtils.indexOfAny("", new char[0]));
        assertEquals(-1, StringUtils.indexOfAny("", new char[] {'a','b'}));
        
        assertEquals(-1, StringUtils.indexOfAny("zzabyycdxx", (char[]) null)); 
        assertEquals(-1, StringUtils.indexOfAny("zzabyycdxx", new char[0])); 
        assertEquals(0, StringUtils.indexOfAny("zzabyycdxx", new char[] {'z','a'})); 
        assertEquals(3, StringUtils.indexOfAny("zzabyycdxx", new char[] {'b','y'}));
        assertEquals(-1, StringUtils.indexOfAny("ab", new char[] {'z'}));
    }

    public void testIndexOfAny_StringString() {
        assertEquals(-1, StringUtils.indexOfAny(null, (String) null));
        assertEquals(-1, StringUtils.indexOfAny(null, ""));
        assertEquals(-1, StringUtils.indexOfAny(null, "ab"));
        
        assertEquals(-1, StringUtils.indexOfAny("", (String) null));
        assertEquals(-1, StringUtils.indexOfAny("", ""));
        assertEquals(-1, StringUtils.indexOfAny("", "ab"));
        
        assertEquals(-1, StringUtils.indexOfAny("zzabyycdxx", (String) null)); 
        assertEquals(-1, StringUtils.indexOfAny("zzabyycdxx", "")); 
        assertEquals(0, StringUtils.indexOfAny("zzabyycdxx", "za")); 
        assertEquals(3, StringUtils.indexOfAny("zzabyycdxx", "by"));
        assertEquals(-1, StringUtils.indexOfAny("ab", "z"));
    }

    //-----------------------------------------------------------------------
    public void testIndexOfAnyBut_StringChararray() {
        assertEquals(-1, StringUtils.indexOfAnyBut(null, (char[]) null));
        assertEquals(-1, StringUtils.indexOfAnyBut(null, new char[0]));
        assertEquals(-1, StringUtils.indexOfAnyBut(null, new char[] {'a','b'}));
        
        assertEquals(-1, StringUtils.indexOfAnyBut("", (char[]) null));
        assertEquals(-1, StringUtils.indexOfAnyBut("", new char[0]));
        assertEquals(-1, StringUtils.indexOfAnyBut("", new char[] {'a','b'}));
        
        assertEquals(-1, StringUtils.indexOfAnyBut("zzabyycdxx", (char[]) null));
        assertEquals(-1, StringUtils.indexOfAnyBut("zzabyycdxx", new char[0]));
        assertEquals(3, StringUtils.indexOfAnyBut("zzabyycdxx", new char[] {'z','a'})); 
        assertEquals(0, StringUtils.indexOfAnyBut("zzabyycdxx", new char[] {'b','y'})); 
        assertEquals(0, StringUtils.indexOfAnyBut("ab", new char[] {'z'}));
    }

    public void testIndexOfAnyBut_StringString() {
        assertEquals(-1, StringUtils.indexOfAnyBut(null, (String) null));
        assertEquals(-1, StringUtils.indexOfAnyBut(null, ""));
        assertEquals(-1, StringUtils.indexOfAnyBut(null, "ab"));
        
        assertEquals(-1, StringUtils.indexOfAnyBut("", (String) null));
        assertEquals(-1, StringUtils.indexOfAnyBut("", ""));
        assertEquals(-1, StringUtils.indexOfAnyBut("", "ab"));
        
        assertEquals(-1, StringUtils.indexOfAnyBut("zzabyycdxx", (String) null)); 
        assertEquals(-1, StringUtils.indexOfAnyBut("zzabyycdxx", "")); 
        assertEquals(3, StringUtils.indexOfAnyBut("zzabyycdxx", "za")); 
        assertEquals(0, StringUtils.indexOfAnyBut("zzabyycdxx", "by"));
        assertEquals(0, StringUtils.indexOfAnyBut("ab", "z"));
    }
    
    //-----------------------------------------------------------------------
    public void testContainsOnly_String() {
        String str1 = "a";
        String str2 = "b";
        String str3 = "ab";
        String chars1= "b";
        String chars2= "a";
        String chars3= "ab";
        assertEquals(false, StringUtils.containsOnly(null, (String) null));
        assertEquals(false, StringUtils.containsOnly("", (String) null));
        assertEquals(false, StringUtils.containsOnly(null, ""));
        assertEquals(false, StringUtils.containsOnly(str1, ""));
        assertEquals(true, StringUtils.containsOnly("", ""));
        assertEquals(true, StringUtils.containsOnly("", chars1));
        assertEquals(false, StringUtils.containsOnly(str1, chars1));
        assertEquals(true, StringUtils.containsOnly(str1, chars2));
        assertEquals(true, StringUtils.containsOnly(str1, chars3));
        assertEquals(true, StringUtils.containsOnly(str2, chars1));
        assertEquals(false, StringUtils.containsOnly(str2, chars2));
        assertEquals(true, StringUtils.containsOnly(str2, chars3));
        assertEquals(false, StringUtils.containsOnly(str3, chars1));
        assertEquals(false, StringUtils.containsOnly(str3, chars2));
        assertEquals(true, StringUtils.containsOnly(str3, chars3));
    }

    public void testContainsOnly_Chararray() {
        String str1 = "a";
        String str2 = "b";
        String str3 = "ab";
        char[] chars1= {'b'};
        char[] chars2= {'a'};
        char[] chars3= {'a', 'b'};
        char[] emptyChars = new char[0];
        assertEquals(false, StringUtils.containsOnly(null, (char[]) null));
        assertEquals(false, StringUtils.containsOnly("", (char[]) null));
        assertEquals(false, StringUtils.containsOnly(null, emptyChars));
        assertEquals(false, StringUtils.containsOnly(str1, emptyChars));
        assertEquals(true, StringUtils.containsOnly("", emptyChars));
        assertEquals(true, StringUtils.containsOnly("", chars1));
        assertEquals(false, StringUtils.containsOnly(str1, chars1));
        assertEquals(true, StringUtils.containsOnly(str1, chars2));
        assertEquals(true, StringUtils.containsOnly(str1, chars3));
        assertEquals(true, StringUtils.containsOnly(str2, chars1));
        assertEquals(false, StringUtils.containsOnly(str2, chars2));
        assertEquals(true, StringUtils.containsOnly(str2, chars3));
        assertEquals(false, StringUtils.containsOnly(str3, chars1));
        assertEquals(false, StringUtils.containsOnly(str3, chars2));
        assertEquals(true, StringUtils.containsOnly(str3, chars3));
    }

    public void testContainsNone_String() {
        String str1 = "a";
        String str2 = "b";
        String str3 = "ab.";
        String chars1= "b";
        String chars2= ".";
        String chars3= "cd";
        assertEquals(true, StringUtils.containsNone(null, (String) null));
        assertEquals(true, StringUtils.containsNone("", (String) null));
        assertEquals(true, StringUtils.containsNone(null, ""));
        assertEquals(true, StringUtils.containsNone(str1, ""));
        assertEquals(true, StringUtils.containsNone("", ""));
        assertEquals(true, StringUtils.containsNone("", chars1));
        assertEquals(true, StringUtils.containsNone(str1, chars1));
        assertEquals(true, StringUtils.containsNone(str1, chars2));
        assertEquals(true, StringUtils.containsNone(str1, chars3));
        assertEquals(false, StringUtils.containsNone(str2, chars1));
        assertEquals(true, StringUtils.containsNone(str2, chars2));
        assertEquals(true, StringUtils.containsNone(str2, chars3));
        assertEquals(false, StringUtils.containsNone(str3, chars1));
        assertEquals(false, StringUtils.containsNone(str3, chars2));
        assertEquals(true, StringUtils.containsNone(str3, chars3));
    }

    public void testContainsNone_Chararray() {
        String str1 = "a";
        String str2 = "b";
        String str3 = "ab.";
        char[] chars1= {'b'};
        char[] chars2= {'.'};
        char[] chars3= {'c', 'd'};
        char[] emptyChars = new char[0];
        assertEquals(true, StringUtils.containsNone(null, (char[]) null));
        assertEquals(true, StringUtils.containsNone("", (char[]) null));
        assertEquals(true, StringUtils.containsNone(null, emptyChars));
        assertEquals(true, StringUtils.containsNone(str1, emptyChars));
        assertEquals(true, StringUtils.containsNone("", emptyChars));
        assertEquals(true, StringUtils.containsNone("", chars1));
        assertEquals(true, StringUtils.containsNone(str1, chars1));
        assertEquals(true, StringUtils.containsNone(str1, chars2));
        assertEquals(true, StringUtils.containsNone(str1, chars3));
        assertEquals(false, StringUtils.containsNone(str2, chars1));
        assertEquals(true, StringUtils.containsNone(str2, chars2));
        assertEquals(true, StringUtils.containsNone(str2, chars3));
        assertEquals(false, StringUtils.containsNone(str3, chars1));
        assertEquals(false, StringUtils.containsNone(str3, chars2));
        assertEquals(true, StringUtils.containsNone(str3, chars3));
    }

}
