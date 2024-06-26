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
package com.puppycrawl.tools.checkstyle.checks.design;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * <p>
 * Restricts throws statements to a specified count (default = 1).
 * </p>
 * <p>
 * Rationale:
 * Exceptions form part of a methods interface. Declaring
 * a method to throw too many differently rooted
 * exceptions makes exception handling onerous and leads
 * to poor programming practices such as catch
 * (Exception). This check forces developers to put
 * exceptions into a hierarchy such that in the simplest
 * case, only one type of exception need be checked for by
 * a caller but allows any sub-classes to be caught
 * specifically if necessary.
 * </p>
 * @author <a href="mailto:simon@redhillconsulting.com.au">Simon Harris</a>
 */
public final class ThrowsCountCheck extends Check
{

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_KEY = "throws.count";

    /** default value of max property */
    private static final int DEFAULT_MAX = 1;

    /** maximum allowed throws statements */
    private int max;

    /** Creates new instance of the check. */
    public ThrowsCountCheck()
    {
        setMax(DEFAULT_MAX);
    }

    @Override
    public int[] getDefaultTokens()
    {
        return new int[] {
            TokenTypes.LITERAL_THROWS,
        };
    }

    @Override
    public int[] getRequiredTokens()
    {
        return getDefaultTokens();
    }

    @Override
    public int[] getAcceptableTokens()
    {
        return new int[] {
            TokenTypes.LITERAL_THROWS,
        };
    }

    /**
     * Getter for max property.
     * @return maximum allowed throws statements.
     */
    public int getMax()
    {
        return max;
    }

    /**
     * Setter for max property.
     * @param max maximum allowed throws statements.
     */
    public void setMax(int max)
    {
        this.max = max;
    }

    @Override
    public void visitToken(DetailAST ast)
    {
        if (ast.getType() == TokenTypes.LITERAL_THROWS) {
            visitLiteralThrows(ast);
        }
        else {
            throw new IllegalStateException(ast.toString());
        }
    }

    /**
     * Checks number of throws statements.
     * @param ast throws for check.
     */
    private void visitLiteralThrows(DetailAST ast)
    {
        // Account for all the commas!
        final int count = (ast.getChildCount() + 1) / 2;
        if (count > getMax()) {
            log(ast.getLineNo(),  ast.getColumnNo(), MSG_KEY,
                count, getMax());
        }
    }
}
