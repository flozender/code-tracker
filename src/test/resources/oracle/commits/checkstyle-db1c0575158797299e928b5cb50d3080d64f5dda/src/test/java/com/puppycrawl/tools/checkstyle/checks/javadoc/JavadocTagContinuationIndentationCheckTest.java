////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2015 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.checks.javadoc;

import org.junit.Test;

import com.puppycrawl.tools.checkstyle.BaseCheckTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;

import static com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocTagContinuationIndentationCheck
.MSG_KEY;

public class JavadocTagContinuationIndentationCheckTest
        extends BaseCheckTestSupport {
    @Test
    public void testFP() throws Exception {
        final DefaultConfiguration checkConfig =
                createCheckConfig(JavadocTagContinuationIndentationCheck.class);
        final String[] expected = {
        };
        verify(checkConfig, getPath("javadoc/GuavaFP.java"), expected);
    }

    @Test
    public void testCheck() throws Exception {
        final DefaultConfiguration checkConfig =
                createCheckConfig(JavadocTagContinuationIndentationCheck.class);
        final String[] expected = {
            "47: " + getCheckMessage(MSG_KEY, 4),
            "109: " + getCheckMessage(MSG_KEY, 4),
            "112: " + getCheckMessage(MSG_KEY, 4),
            "203: " + getCheckMessage(MSG_KEY, 4),
            "206: " + getCheckMessage(MSG_KEY, 4),
            "221: " + getCheckMessage(MSG_KEY, 4),
            "223: " + getCheckMessage(MSG_KEY, 4),
            "285: " + getCheckMessage(MSG_KEY, 4),
            "288: " + getCheckMessage(MSG_KEY, 4),
            "290: " + getCheckMessage(MSG_KEY, 4),
            "310: " + getCheckMessage(MSG_KEY, 4),
            "322: " + getCheckMessage(MSG_KEY, 4),
        };
        verify(checkConfig, getPath("javadoc/InputJavaDocTagContinuationIndentation.java"),
                expected);
    }

    @Test
    public void testCheckWithOffset3() throws Exception {
        final DefaultConfiguration checkConfig =
                createCheckConfig(JavadocTagContinuationIndentationCheck.class);
        checkConfig.addAttribute("offset", "3");
        final String[] expected = {
            "7: " + getCheckMessage(MSG_KEY, 3),
            "19: " + getCheckMessage(MSG_KEY, 3),
        };
        verify(checkConfig, getPath("javadoc/InputJavaDocTagContinuationIndentationOffset3.java"),
                expected);
    }
}
