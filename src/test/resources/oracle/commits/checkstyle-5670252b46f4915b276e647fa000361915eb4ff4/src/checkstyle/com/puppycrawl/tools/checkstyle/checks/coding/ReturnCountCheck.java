////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2004  Oliver Burn
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

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import com.puppycrawl.tools.checkstyle.checks.CheckUtils;

import java.util.Stack;

/**
 * <p>
 * Restricts return statements to a specified count (default = 2).
 * Ignores equals methods.
 * </p>
 * <p>
 * Rationale: Too many return points can be indication that code is
 * attempting to do too much or may be difficult to understand.
 * </p>
 *
 * @author <a href="mailto:simon@redhillconsulting.com.au">Simon Harris</a>
 * TODO: Test for inside a static block
 */
public final class ReturnCountCheck extends Check
{
    /** Default allowed number of return statements. */
    private static final int DEFAULT_MAX = 2;

    /** Stack of method contexts. */
    private final Stack mContextStack = new Stack();
    /** Maximum allowed number of return stmts. */
    private int mMax;
    /** Current method context. */
    private Context mContext;

    /** Creates new instance of the checks. */
    public ReturnCountCheck()
    {
        setMax(DEFAULT_MAX);
    }

    /** @see Check */
    public int[] getDefaultTokens()
    {
        return new int[] {
            TokenTypes.CTOR_DEF,
            TokenTypes.METHOD_DEF,
            TokenTypes.LITERAL_RETURN,
        };
    }

    /** @see Check */
    public int[] getRequiredTokens()
    {
        return new int[]{
            TokenTypes.CTOR_DEF,
            TokenTypes.METHOD_DEF,
        };
    }

    /**
     * Getter for max property.
     * @return maximum allowed number of return statements.
     */
    public int getMax()
    {
        return mMax;
    }

    /**
     * Setter for max property.
     * @param aMax maximum allowed number of return statements.
     */
    public void setMax(int aMax)
    {
        mMax = aMax;
    }

    /** @see Check */
    public void beginTree(DetailAST aRootAST)
    {
        mContext = null;
        mContextStack.clear();
    }

    /** @see Check */
    public void visitToken(DetailAST aAST)
    {
        switch (aAST.getType()) {
        case TokenTypes.CTOR_DEF:
        case TokenTypes.METHOD_DEF:
            visitMethodDef(aAST);
            break;
        case TokenTypes.LITERAL_RETURN:
            mContext.visitLiteralReturn();
            break;
        default:
            throw new IllegalStateException(aAST.toString());
        }
    }

    /** @see Check */
    public void leaveToken(DetailAST aAST)
    {
        switch (aAST.getType()) {
        case TokenTypes.CTOR_DEF:
        case TokenTypes.METHOD_DEF:
            leaveMethodDef(aAST);
            break;
        case TokenTypes.LITERAL_RETURN:
            // Do nothing
            break;
        default:
            throw new IllegalStateException(aAST.toString());
        }
    }

    /**
     * Creates new method context and places old one on the stack.
     * @param aAST method definition for check.
     */
    private void visitMethodDef(DetailAST aAST)
    {
        mContextStack.push(mContext);
        mContext = new Context(!CheckUtils.isEqualsMethod(aAST));
    }

    /**
     * Checks number of return statments and restore
     * previous method context.
     * @param aAST method def node.
     */
    private void leaveMethodDef(DetailAST aAST)
    {
        mContext.checkCount(aAST);
        mContext = (Context) mContextStack.pop();
    }

    /**
     * Class to encapsulate information about one method.
     * @author <a href="mailto:simon@redhillconsulting.com.au">Simon Harris</a>
     */
    private class Context
    {
        /** Whether we should check this method or not. */
        private final boolean mChecking;
        /** Counter for return statements. */
        private int mCount;

        /**
         * Creates new method context.
         * @param aChecking should we check this method or not.
         */
        public Context(boolean aChecking)
        {
            mChecking = aChecking;
            mCount = 0;
        }

        /** Increase number of return statements. */
        public void visitLiteralReturn()
        {
            ++mCount;
        }

        /**
         * Checks if number of return statements in method more
         * than allowed.
         * @param aAST method def associated with this context.
         */
        public void checkCount(DetailAST aAST)
        {
            if (mChecking && mCount > getMax()) {
                log(aAST.getLineNo(), aAST.getColumnNo(), "return.count",
                    new Integer(mCount), new Integer(getMax()));
            }
        }
    }
}
