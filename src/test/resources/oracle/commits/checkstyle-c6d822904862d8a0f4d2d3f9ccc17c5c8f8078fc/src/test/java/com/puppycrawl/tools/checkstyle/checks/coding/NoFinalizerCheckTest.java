////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2018 the original author or authors.
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

package com.puppycrawl.tools.checkstyle.checks.coding;

import static com.puppycrawl.tools.checkstyle.checks.coding.NoFinalizerCheck.MSG_KEY;

import org.junit.Test;

import com.puppycrawl.tools.checkstyle.AbstractModuleTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.utils.CommonUtils;

/**
 * NoFinalizerCheck test.
 *
 * @author smckay@google.com (Steve McKay)
 */
public class NoFinalizerCheckTest
    extends AbstractModuleTestSupport {

    @Override
    protected String getPackageLocation() {
        return "com/puppycrawl/tools/checkstyle/checks/coding/nofinalizer";
    }

    @Test
    public void testHasFinalizer()
            throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(NoFinalizerCheck.class);
        final String[] expected = {
            "5: " + getCheckMessage(MSG_KEY),
        };
        verify(checkConfig, getPath("InputNoFinalizerHasFinalizer.java"), expected);
    }

    @Test
    public void testHasNoFinalizer()
            throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(NoFinalizerCheck.class);
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getPath("InputNoFinalizerFallThrough.java"), expected);
    }
}
