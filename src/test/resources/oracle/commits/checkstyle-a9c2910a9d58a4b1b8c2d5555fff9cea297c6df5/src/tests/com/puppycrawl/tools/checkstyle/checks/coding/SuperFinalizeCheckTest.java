////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2011  Oliver Burn
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

import com.puppycrawl.tools.checkstyle.BaseCheckTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import org.junit.Test;

public class SuperFinalizeCheckTest
    extends BaseCheckTestSupport
{
    @Test
    public void testIt() throws Exception
    {
        final DefaultConfiguration checkConfig =
            createCheckConfig(SuperFinalizeCheck.class);
        final String[] expected = {
            "27:17: Method 'finalize' should call 'super.finalize'.",
            "34:17: Method 'finalize' should call 'super.finalize'.",
        };
        verify(checkConfig, getPath("coding/InputFinalize.java"), expected);
    }
}
