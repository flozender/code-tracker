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
package com.puppycrawl.tools.checkstyle.api;

import com.google.common.collect.ImmutableCollection;

/**
 * A context to be used in subcomponents. The general idea of
 * Context/Contextualizable was taken from <a target="_top"
 * href="http://jakarta.apache.org/avalon/">Jakarta's Avalon framework</a>.
 * @author lkuehne
 * @see Contextualizable
 */
public interface Context
{
    /**
     * Searches for the value with the specified attribute key in this context.
     * @param aKey the attribute key.
     * @return the value in this context with the specified attribute key value.
     */
    Object get(String aKey);

    /**
     * Returns the names of all atttributes of this context.
     * @return the names of all atttributes of this context.
     */
    ImmutableCollection<String> getAttributeNames();
}
