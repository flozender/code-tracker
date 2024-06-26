////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2003  Oliver Burn
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
package com.puppycrawl.tools.checkstyle.filter;

import com.puppycrawl.tools.checkstyle.SuppressionsLoader;
import com.puppycrawl.tools.checkstyle.api.AuditEventFilter;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.FilterChain;

/**
 * <p>
 * This filter suppresses audit events according to file, check, line, and
 * column, as specified in a suppression file.
 * </p>
 * @author Rick Giles
 */
public class SuppressionFilter
    extends AuditEventFilter
{
    /** chain of individual suppresses */
    private FilterChain mFilterChain = new FilterChain();

    /**
     * Loads the suppressions for a file.
     * @param aFileName name of the suppressions file.
     * @throws CheckstyleException if there is an error.
     */
    public void setFile(String aFileName)
        throws CheckstyleException
    {
        mFilterChain = SuppressionsLoader.loadSuppressions(aFileName);
    }

    /** @see com.puppycrawl.tools.checkstyle.api.Filter */
    public int decide(Object aObject)
    {
        return mFilterChain.decide(aObject);
    }

    /** @see java.lang.Object#toString() */
    public String toString()
    {
        return mFilterChain.toString();
    }

    /** @see java.lang.Object#hashCode() */
    public int hashCode()
    {
        return mFilterChain.hashCode();
    }

    /** @see java.lang.Object#equals(java.lang.Object) */
    public boolean equals (Object aObject)
    {
        if (aObject instanceof SuppressionFilter) {
            final SuppressionFilter other = (SuppressionFilter) aObject;
            return this.mFilterChain.equals(other.mFilterChain);
        }
        else {
            return false;
        }
    }
}
