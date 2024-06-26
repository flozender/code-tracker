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
package com.puppycrawl.tools.checkstyle.filters;


import com.puppycrawl.tools.checkstyle.api.Filter;

/**
 * <p>
 * This filter drops all objects.
 * </p>
 * <p>You can add this filter to the end of a filter chain to
 * switch from the default "accept all unless instructed otherwise"
 * filtering behaviour to a "deny all unless instructed otherwise"
 * behaviour.
 * </p>
 * <p>
 * Modelled after the log4j DenyAllFilter.
 * </p>
 * @author Rick Giles
 */
public class DenyAllFilter
    implements Filter
{

    /** @see com.puppycrawl.tools.checkstyle.filter.Filter */
    public boolean accept(Object aObject)
    {
        return false;
    }

}
