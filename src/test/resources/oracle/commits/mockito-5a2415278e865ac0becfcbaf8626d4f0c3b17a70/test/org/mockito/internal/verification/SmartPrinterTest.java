/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.verification;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.invocation.InvocationMatcher;
import org.mockito.internal.reporting.SmartPrinter;
import org.mockitousage.IMethods;
import org.mockitoutil.TestBase;

public class SmartPrinterTest extends TestBase {

    private InvocationMatcher multi;
    private InvocationMatcher shortie;
    @Mock private IMethods mock;

    @Before
    public void setup() throws Exception {
        mock.varargs("first very long argument", "second very long argument", "another very long argument");
        multi = new InvocationMatcher(getLastInvocation());

        mock.varargs("short arg");
        shortie = new InvocationMatcher(getLastInvocation());
    }

    @Test
    public void shouldPrintBothInMultilinesWhenFirstIsMulti() {
        //when
        SmartPrinter printer = new SmartPrinter(multi, shortie.getInvocation());
        
        //then
        assertContains("\n", printer.getWanted().toString());
        assertContains("\n", printer.getActual().toString());
    }

    @Test
    public void shouldPrintBothInMultilinesWhenSecondIsMulti() {
        //when
        SmartPrinter printer = new SmartPrinter(shortie, multi.getInvocation());
        
        //then
        assertContains("\n", printer.getWanted().toString());
        assertContains("\n", printer.getActual().toString());
    }

    @Test
    public void shouldPrintBothInMultilinesWhenBothAreMulti() {
        //when
        SmartPrinter printer = new SmartPrinter(multi, multi.getInvocation());
        
        //then
        assertContains("\n", printer.getWanted().toString());
        assertContains("\n", printer.getActual().toString());
    }

    @Test
    public void shouldPrintBothInSingleLineWhenBothAreShort() {
        //when
        SmartPrinter printer = new SmartPrinter(shortie, shortie.getInvocation());
        
        //then
        assertNotContains("\n", printer.getWanted().toString());
        assertNotContains("\n", printer.getActual().toString());
    }
}