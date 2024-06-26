////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2002  Oliver Burn
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
package com.puppycrawl.tools.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.Utils;

/**
 * Checks the placement of left curly braces on types.
 *
 * @author <a href="mailto:checkstyle@puppycrawl.com">Oliver Burn</a>
 * @version 1.0
 */
public class TypeLeftCurlyCheck
    extends LeftCurlyCheck
{
    /** @see Check */
    public int[] getDefaultTokens()
    {
        return new int[] {TokenTypes.INTERFACE_DEF,
                          TokenTypes.CLASS_DEF};
    }

    /** @see Check */
    public void visitToken(DetailAST aAST)
    {
        final DetailAST brace = (DetailAST)
            Utils.getLastSibling(aAST.getFirstChild())
            .getFirstChild();
        // TODO: should check for modifiers
        final DetailAST startToken =
            (DetailAST) aAST.getFirstChild().getNextSibling();

        verifyBrace(brace, startToken);
    }
}
