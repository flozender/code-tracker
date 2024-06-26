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
package org.apache.commons.lang.builder;

import java.util.ArrayList;
import java.util.HashMap;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.commons.lang.SystemUtils;

/**
 * Unit tests {@link org.apache.commons.lang.builder.MultiLineToStringStyleTest}.
 *
 * @author <a href="mailto:scolebourne@joda.org">Stephen Colebourne</a>
 * @version $Id: MultiLineToStringStyleTest.java,v 1.5 2003/08/16 13:02:14 scolebourne Exp $
 */
public class MultiLineToStringStyleTest extends TestCase {

    private final Integer base = new Integer(5);
    private final String baseStr = base.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(base));
    
    public MultiLineToStringStyleTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(MultiLineToStringStyleTest.class);
        suite.setName("MultiLineToStringStyle Tests");
        return suite;
    }

    protected void setUp() throws Exception {
        super.setUp();
        ToStringBuilder.setDefaultStyle(ToStringStyle.MULTI_LINE_STYLE);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        ToStringBuilder.setDefaultStyle(ToStringStyle.DEFAULT_STYLE);
    }

    //----------------------------------------------------------------
    
    public void testBlank() {
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).toString());
    }

    public void testAppendSuper() {
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).appendSuper("Integer@8888[" + SystemUtils.LINE_SEPARATOR + "]").toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  <null>" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).appendSuper("Integer@8888[" + SystemUtils.LINE_SEPARATOR + "  <null>" + SystemUtils.LINE_SEPARATOR + "]").toString());
        
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  a=hello" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).appendSuper("Integer@8888[" + SystemUtils.LINE_SEPARATOR + "]").append("a", "hello").toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  <null>" + SystemUtils.LINE_SEPARATOR + "  a=hello" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).appendSuper("Integer@8888[" + SystemUtils.LINE_SEPARATOR + "  <null>" + SystemUtils.LINE_SEPARATOR + "]").append("a", "hello").toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  a=hello" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).appendSuper(null).append("a", "hello").toString());
    }
    
    public void testObject() {
        Integer i3 = new Integer(3);
        Integer i4 = new Integer(4);
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  <null>" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append((Object) null).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  3" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append(i3).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  a=<null>" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append("a", (Object) null).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  a=3" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append("a", i3).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  a=3" + SystemUtils.LINE_SEPARATOR + "  b=4" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append("a", i3).append("b", i4).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  a=<Integer>" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append("a", i3, false).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  a=<size=0>" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append("a", new ArrayList(), false).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  a=[]" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append("a", new ArrayList(), true).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  a=<size=0>" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append("a", new HashMap(), false).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  a={}" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append("a", new HashMap(), true).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  a=<size=0>" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append("a", (Object) new String[0], false).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  a={}" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append("a", (Object) new String[0], true).toString());
    }

    public void testLong() {
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  3" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append(3L).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  a=3" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append("a", 3L).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  a=3" + SystemUtils.LINE_SEPARATOR + "  b=4" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append("a", 3L).append("b", 4L).toString());
    }

    public void testObjectArray() {
        Object[] array = new Object[] {null, base, new int[] {3, 6}};
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  {<null>,5,{3,6}}" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append(array).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  {<null>,5,{3,6}}" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append((Object) array).toString());
        array = null;
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  <null>" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append(array).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  <null>" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append((Object) array).toString());
    }

    public void testLongArray() {
        long[] array = new long[] {1, 2, -3, 4};
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  {1,2,-3,4}" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append(array).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  {1,2,-3,4}" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append((Object) array).toString());
        array = null;
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  <null>" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append(array).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  <null>" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append((Object) array).toString());
    }

    public void testLongArrayArray() {
        long[][] array = new long[][] {{1, 2}, null, {5}};
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  {{1,2},<null>,{5}}" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append(array).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  {{1,2},<null>,{5}}" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append((Object) array).toString());
        array = null;
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  <null>" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append(array).toString());
        assertEquals(baseStr + "[" + SystemUtils.LINE_SEPARATOR + "  <null>" + SystemUtils.LINE_SEPARATOR + "]", new ToStringBuilder(base).append((Object) array).toString());
    }

}
