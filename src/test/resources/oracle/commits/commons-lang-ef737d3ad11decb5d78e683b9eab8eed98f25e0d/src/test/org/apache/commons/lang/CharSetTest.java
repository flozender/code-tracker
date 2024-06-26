/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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
package org.apache.commons.lang;

import java.lang.reflect.Modifier;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Unit tests {@link org.apache.commons.lang.CharSet}.
 *
 * @author Stephen Colebourne
 * @author Phil Steitz
 * @version $Id: CharSetTest.java,v 1.2 2003/08/04 00:50:14 scolebourne Exp $
 */
public class CharSetTest extends TestCase {
    
    public CharSetTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(CharSetTest.class);
        suite.setName("CharSet Tests");
        return suite;
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    //-----------------------------------------------------------------------
    public void testClass() {
        assertEquals(true, Modifier.isPublic(CharSet.class.getModifiers()));
        assertEquals(false, Modifier.isFinal(CharSet.class.getModifiers()));
    }
    
    //-----------------------------------------------------------------------
    public void testGetInstance() {
        assertSame(CharSet.EMPTY, CharSet.getInstance(null));
        assertSame(CharSet.EMPTY, CharSet.getInstance(""));
        assertSame(CharSet.ASCII_ALPHA, CharSet.getInstance("a-zA-Z"));
        assertSame(CharSet.ASCII_ALPHA, CharSet.getInstance("A-Za-z"));
        assertSame(CharSet.ASCII_ALPHA_LOWER, CharSet.getInstance("a-z"));
        assertSame(CharSet.ASCII_ALPHA_UPPER, CharSet.getInstance("A-Z"));
        assertSame(CharSet.ASCII_NUMERIC, CharSet.getInstance("0-9"));
    }
            
    //-----------------------------------------------------------------------
    public void testConstructor_String_simple() {
        CharSet set;
        CharRange[] array;
        
        set = CharSet.getInstance((String) null);
        array = set.getCharRanges();
        assertEquals("[]", set.toString());
        assertEquals(0, array.length);
        
        set = CharSet.getInstance("");
        array = set.getCharRanges();
        assertEquals("[]", set.toString());
        assertEquals(0, array.length);
        
        set = CharSet.getInstance("a");
        array = set.getCharRanges();
        assertEquals("[a]", set.toString());
        assertEquals(1, array.length);
        assertEquals("a", array[0].toString());
        
        set = CharSet.getInstance("^a");
        array = set.getCharRanges();
        assertEquals("[^a]", set.toString());
        assertEquals(1, array.length);
        assertEquals("^a", array[0].toString());
        
        set = CharSet.getInstance("a-e");
        array = set.getCharRanges();
        assertEquals("[a-e]", set.toString());
        assertEquals(1, array.length);
        assertEquals("a-e", array[0].toString());
        
        set = CharSet.getInstance("^a-e");
        array = set.getCharRanges();
        assertEquals("[^a-e]", set.toString());
        assertEquals(1, array.length);
        assertEquals("^a-e", array[0].toString());
    }
    
    public void testConstructor_String_combo() {
        CharSet set;
        CharRange[] array;
        
        set = CharSet.getInstance("abc");
        array = set.getCharRanges();
        assertEquals(3, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a')));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('b')));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('c')));
        
        set = CharSet.getInstance("a-ce-f");
        array = set.getCharRanges();
        assertEquals(2, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a', 'c')));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('e', 'f')));
        
        set = CharSet.getInstance("ae-f");
        array = set.getCharRanges();
        assertEquals(2, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a')));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('e', 'f')));
        
        set = CharSet.getInstance("e-fa");
        array = set.getCharRanges();
        assertEquals(2, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a')));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('e', 'f')));
        
        set = CharSet.getInstance("ae-fm-pz");
        array = set.getCharRanges();
        assertEquals(4, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a')));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('e', 'f')));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('m', 'p')));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('z')));
    }
    
    public void testConstructor_String_comboNegated() {
        CharSet set;
        CharRange[] array;
        
        set = CharSet.getInstance("^abc");
        array = set.getCharRanges();
        assertEquals(3, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a', 'a', true)));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('b')));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('c')));
        
        set = CharSet.getInstance("b^ac");
        array = set.getCharRanges();
        assertEquals(3, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('b')));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a', 'a', true)));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('c')));
        
        set = CharSet.getInstance("db^ac");
        array = set.getCharRanges();
        assertEquals(4, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('d')));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('b')));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a', 'a', true)));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('c')));
        
        set = CharSet.getInstance("^b^a");
        array = set.getCharRanges();
        assertEquals(2, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('b', 'b', true)));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a', 'a', true)));
        
        set = CharSet.getInstance("b^a-c^z");
        array = set.getCharRanges();
        assertEquals(3, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a', 'c', true)));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('z', 'z', true)));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('b')));
    }
        
    public void testConstructor_String_oddDash() {
        CharSet set;
        CharRange[] array;
        
        set = CharSet.getInstance("-");
        array = set.getCharRanges();
        assertEquals(1, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('-')));
        
        set = CharSet.getInstance("--");
        array = set.getCharRanges();
        assertEquals(1, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('-')));
        
        set = CharSet.getInstance("---");
        array = set.getCharRanges();
        assertEquals(1, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('-')));
        
        set = CharSet.getInstance("----");
        array = set.getCharRanges();
        assertEquals(1, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('-')));
        
        set = CharSet.getInstance("-a");
        array = set.getCharRanges();
        assertEquals(2, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('-')));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a')));
        
        set = CharSet.getInstance("a-");
        array = set.getCharRanges();
        assertEquals(2, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a')));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('-')));
        
        set = CharSet.getInstance("a--");
        array = set.getCharRanges();
        assertEquals(1, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a', '-')));
        
        set = CharSet.getInstance("--a");
        array = set.getCharRanges();
        assertEquals(1, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('-', 'a')));
    }
    
    public void testConstructor_String_oddNegate() {
        CharSet set;
        CharRange[] array;
        set = CharSet.getInstance("^");
        array = set.getCharRanges();
        assertEquals(1, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('^'))); // "^"
        
        set = CharSet.getInstance("^^");
        array = set.getCharRanges();
        assertEquals(1, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('^', '^', true))); // "^^"
        
        set = CharSet.getInstance("^^^");
        array = set.getCharRanges();
        assertEquals(2, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('^', '^', true))); // "^^"
        assertEquals(true, ArrayUtils.contains(array, new CharRange('^', '^'))); // "^"
        
        set = CharSet.getInstance("^^^^");
        array = set.getCharRanges();
        assertEquals(1, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('^', '^', true))); // "^^" x2
        
        set = CharSet.getInstance("a^");
        array = set.getCharRanges();
        assertEquals(2, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a'))); // "a"
        assertEquals(true, ArrayUtils.contains(array, new CharRange('^'))); // "^"
        
        set = CharSet.getInstance("^a-");
        array = set.getCharRanges();
        assertEquals(2, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a', 'a', true))); // "^a"
        assertEquals(true, ArrayUtils.contains(array, new CharRange('-'))); // "-"
        
        set = CharSet.getInstance("^^-c");
        array = set.getCharRanges();
        assertEquals(1, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('^', 'c', true))); // "^^-c"
        
        set = CharSet.getInstance("^c-^");
        array = set.getCharRanges();
        assertEquals(1, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('c', '^', true))); // "^c-^"
        
        set = CharSet.getInstance("^c-^d");
        array = set.getCharRanges();
        assertEquals(2, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('c', '^', true))); // "^c-^"
        assertEquals(true, ArrayUtils.contains(array, new CharRange('d'))); // "d"
        
        set = CharSet.getInstance("^^-");
        array = set.getCharRanges();
        assertEquals(2, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('^', '^', true))); // "^^"
        assertEquals(true, ArrayUtils.contains(array, new CharRange('-'))); // "-"
    }
    
    public void testConstructor_String_oddCombinations() {
        CharSet set;
        CharRange[] array = null;
        
        set = CharSet.getInstance("a-^c");
        array = set.getCharRanges();
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a', '^'))); // "a-^"
        assertEquals(true, ArrayUtils.contains(array, new CharRange('c'))); // "c"
        assertEquals(false, set.contains('b'));
        assertEquals(true, set.contains('^'));  
        assertEquals(true, set.contains('_')); // between ^ and a
        assertEquals(true, set.contains('c'));  
        
        set = CharSet.getInstance("^a-^c");
        array = set.getCharRanges();
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a', '^', true))); // "^a-^"
        assertEquals(true, ArrayUtils.contains(array, new CharRange('c'))); // "c"
        assertEquals(true, set.contains('b'));
        assertEquals(false, set.contains('^'));  
        assertEquals(false, set.contains('_')); // between ^ and a
        
        set = CharSet.getInstance("a- ^-- "); //contains everything
        array = set.getCharRanges();
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a', ' '))); // "a- "
        assertEquals(true, ArrayUtils.contains(array, new CharRange('-', ' ', true))); // "^-- "
        assertEquals(true, set.contains('#'));
        assertEquals(true, set.contains('^'));
        assertEquals(true, set.contains('a'));
        assertEquals(true, set.contains('*'));
        assertEquals(true, set.contains('A'));
        
        set = CharSet.getInstance("^-b");
        array = set.getCharRanges();
        assertEquals(true, ArrayUtils.contains(array, new CharRange('^','b'))); // "^-b"
        assertEquals(true, set.contains('b'));
        assertEquals(true, set.contains('_')); // between ^ and a
        assertEquals(false, set.contains('A'));
        assertEquals(true, set.contains('^')); 
        
        set = CharSet.getInstance("b-^");
        array = set.getCharRanges();
        assertEquals(true, ArrayUtils.contains(array, new CharRange('^','b'))); // "b-^"
        assertEquals(true, set.contains('b'));
        assertEquals(true, set.contains('^'));
        assertEquals(true, set.contains('a')); // between ^ and b
        assertEquals(false, set.contains('c')); 
    }
        
    //-----------------------------------------------------------------------    
    public void testEquals_Object() {
        CharSet abc = CharSet.getInstance("abc");
        CharSet abc2 = CharSet.getInstance("abc");
        CharSet atoc = CharSet.getInstance("a-c");
        CharSet atoc2 = CharSet.getInstance("a-c");
        CharSet notatoc = CharSet.getInstance("^a-c");
        CharSet notatoc2 = CharSet.getInstance("^a-c");
        
        assertEquals(false, abc.equals(null));
        
        assertEquals(true, abc.equals(abc));
        assertEquals(true, abc.equals(abc2));
        assertEquals(false, abc.equals(atoc));
        assertEquals(false, abc.equals(notatoc));
        
        assertEquals(false, atoc.equals(abc));
        assertEquals(true, atoc.equals(atoc));
        assertEquals(true, atoc.equals(atoc2));
        assertEquals(false, atoc.equals(notatoc));
        
        assertEquals(false, notatoc.equals(abc));
        assertEquals(false, notatoc.equals(atoc));
        assertEquals(true, notatoc.equals(notatoc));
        assertEquals(true, notatoc.equals(notatoc2));
    }
            
    public void testHashCode() {
        CharSet abc = CharSet.getInstance("abc");
        CharSet abc2 = CharSet.getInstance("abc");
        CharSet atoc = CharSet.getInstance("a-c");
        CharSet atoc2 = CharSet.getInstance("a-c");
        CharSet notatoc = CharSet.getInstance("^a-c");
        CharSet notatoc2 = CharSet.getInstance("^a-c");
        
        assertEquals(abc.hashCode(), abc.hashCode());
        assertEquals(abc.hashCode(), abc2.hashCode());
        assertEquals(atoc.hashCode(), atoc.hashCode());
        assertEquals(atoc.hashCode(), atoc2.hashCode());
        assertEquals(notatoc.hashCode(), notatoc.hashCode());
        assertEquals(notatoc.hashCode(), notatoc2.hashCode());
    }
    
    //-----------------------------------------------------------------------    
    public void testContains_Char() {
        CharSet btod = CharSet.getInstance("b-d");
        CharSet dtob = CharSet.getInstance("d-b");
        CharSet bcd = CharSet.getInstance("bcd");
        CharSet bd = CharSet.getInstance("bd");
        CharSet notbtod = CharSet.getInstance("^b-d");
        
        assertEquals(false, btod.contains('a'));
        assertEquals(true, btod.contains('b'));
        assertEquals(true, btod.contains('c'));
        assertEquals(true, btod.contains('d'));
        assertEquals(false, btod.contains('e'));
        
        assertEquals(false, bcd.contains('a'));
        assertEquals(true, bcd.contains('b'));
        assertEquals(true, bcd.contains('c'));
        assertEquals(true, bcd.contains('d'));
        assertEquals(false, bcd.contains('e'));
        
        assertEquals(false, bd.contains('a'));
        assertEquals(true, bd.contains('b'));
        assertEquals(false, bd.contains('c'));
        assertEquals(true, bd.contains('d'));
        assertEquals(false, bd.contains('e'));
        
        assertEquals(true, notbtod.contains('a'));
        assertEquals(false, notbtod.contains('b'));
        assertEquals(false, notbtod.contains('c'));
        assertEquals(false, notbtod.contains('d'));
        assertEquals(true, notbtod.contains('e'));
        
        assertEquals(false, dtob.contains('a'));
        assertEquals(true, dtob.contains('b'));
        assertEquals(true, dtob.contains('c'));
        assertEquals(true, dtob.contains('d'));
        assertEquals(false, dtob.contains('e'));
      
        CharRange[] array = dtob.getCharRanges();
        assertEquals("[b-d]", dtob.toString());
        assertEquals(1, array.length);
    }
    
    //-----------------------------------------------------------------------    
    public void testSerialization() {
        CharSet set = CharSet.getInstance("a");
        assertEquals(set, SerializationUtils.clone(set)); 
        set = CharSet.getInstance("a-e");
        assertEquals(set, SerializationUtils.clone(set)); 
        set = CharSet.getInstance("be-f^a-z");
        assertEquals(set, SerializationUtils.clone(set)); 
    }
    
    //-----------------------------------------------------------------------    
    public void testStatics() {
        CharRange[] array;
        
        array = CharSet.EMPTY.getCharRanges();
        assertEquals(0, array.length);
        
        array = CharSet.ASCII_ALPHA.getCharRanges();
        assertEquals(2, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a', 'z')));
        assertEquals(true, ArrayUtils.contains(array, new CharRange('A', 'Z')));
        
        array = CharSet.ASCII_ALPHA_LOWER.getCharRanges();
        assertEquals(1, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('a', 'z')));
        
        array = CharSet.ASCII_ALPHA_UPPER.getCharRanges();
        assertEquals(1, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('A', 'Z')));
        
        array = CharSet.ASCII_NUMERIC.getCharRanges();
        assertEquals(1, array.length);
        assertEquals(true, ArrayUtils.contains(array, new CharRange('0', '9')));
    }
    
}
