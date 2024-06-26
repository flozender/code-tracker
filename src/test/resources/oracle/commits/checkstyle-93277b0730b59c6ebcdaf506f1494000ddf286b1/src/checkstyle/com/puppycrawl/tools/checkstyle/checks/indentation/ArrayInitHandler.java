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
package com.puppycrawl.tools.checkstyle.checks.indentation;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.IndentationCheck;

/**
 * Handler for array initialization blocks.
 *
 * @author jrichard
 */
public class ArrayInitHandler extends BlockParentHandler 
{
    /**
     * Construct an instance of this handler with the given indentation check,
     * abstract syntax tree, and parent handler.
     * 
     * @param aIndentCheck   the indentation check
     * @param aAst           the abstract syntax tree
     * @param aParent        the parent handler
     */
    public ArrayInitHandler(IndentationCheck aIndentCheck,
        DetailAST aAst, ExpressionHandler aParent) 
    {
        super(aIndentCheck, "array initialization", aAst, aParent);
    }

    /**
     * Compute the indentation amount for this handler.
     * 
     * @return the expected indentation amount
     */
    public int getLevelImpl() 
    {
        DetailAST parentAST = getMainAst().getParent();
        int type = parentAST.getType();
        if (type == TokenTypes.LITERAL_NEW || type == TokenTypes.ASSIGN) {
            // note: assumes new or assignment is line to align with
            return getLineStart(parentAST);
        } 
        else {
            return getParent().getLevel();
        }
    }

    /**
     * There is no top level expression for this handler.
     * 
     * @return null
     */
    protected DetailAST getToplevelAST() 
    {
        return null;
    }

    /**
     * Get the left curly brace portion of the expression we are handling.
     * 
     * @return the left curly brace expression
     */
    protected DetailAST getLCurly()
    {
        return getMainAst();
    }

    /**
     * Get the right curly brace portion of the expression we are handling.
     * 
     * @return the right curly brace expression
     */
    protected DetailAST getRCurly() 
    {
        return getMainAst().findFirstToken(TokenTypes.RCURLY);
    }

    /**
     * Determines if the right curly brace must be at the start of the line.
     * 
     * @return false
     */
    protected boolean rcurlyMustStart() 
    {
        return false;
    }

    /**
     * Determines if child elements within the expression may be nested.
     * 
     * @return true
     */
    protected boolean childrenMayNest() 
    {
        return true;
    }

    /**
     * Get the child element representing the list of statements.
     * 
     * @return the statement list child
     */
    protected DetailAST getListChild() 
    {
        return getMainAst();
    }
}
