/**
 * $Id$
 * 
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 *
 * Created on Dec 13, 2002
 */
package test.net.sourceforge.pmd.rules.design;

import net.sourceforge.pmd.rules.design.NullAssignmentRule;
import test.net.sourceforge.pmd.rules.RuleTst;

/**
 * @author dpeugh
 *
 * Tests the NullAssignmentRule
 * 
 */
public class NullAssignmentRuleTest extends RuleTst {

    public void testInitAssignment() throws Throwable {
        runTest("NullAssignment1.java", 0, new NullAssignmentRule());
    }

    public void testBadAssignment() throws Throwable {
        runTest("NullAssignment2.java", 1, new NullAssignmentRule());
    }

    public void testCheckTest() throws Throwable {
        runTest("NullAssignment3.java", 0, new NullAssignmentRule());
    }

    public void testNullParamOnRHS() throws Throwable {
        runTest("NullAssignment4.java", 0, new NullAssignmentRule());
    }
}
