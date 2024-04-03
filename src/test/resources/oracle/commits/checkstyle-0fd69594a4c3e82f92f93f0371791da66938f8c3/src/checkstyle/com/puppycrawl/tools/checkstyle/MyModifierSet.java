////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001  Oliver Burn
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
////////////////////////////////////////////////////////////////////////////////
package com.puppycrawl.tools.checkstyle;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a set of modifiers and tracks the first line of the modifiers.
 * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
 **/
class MyModifierSet
{
    /** contains the modifiers **/
    private final Set mModifiers = new HashSet();
    /** the first line of the modifiers **/
    private int mFirstLineNo = Integer.MAX_VALUE;

    /**
     * Adds a modifier to the set.
     * @param aMCA the modifier to add
     **/
    void addModifier(MyCommonAST aMCA)
    {
        if (aMCA.getLineNo() < mFirstLineNo) {
            mFirstLineNo = aMCA.getLineNo();
        }

        mModifiers.add(aMCA.getText());
    }

    /** @return the number of modifiers **/
    int size()
    {
        return mModifiers.size();
    }

    /** @return the line number of the first modifier **/
    int getFirstLineNo()
    {
        return mFirstLineNo;
    }

    /** @return whether the set contains a "static". **/
    boolean containsStatic()
    {
        return mModifiers.contains("static");
    }

    /** @return whether the set contains a "final". **/
    boolean containsFinal()
    {
        return mModifiers.contains("final");
    }

    /** @return whether the set contains a "private". **/
    boolean containsPrivate()
    {
        return mModifiers.contains("private");
    }

    /** @return whether the set contains a "protected". **/
    boolean containsProtected()
    {
        return mModifiers.contains("protected");
    }

    /** @return whether the set contains a "public". **/
    boolean containsPublic()
    {
        return mModifiers.contains("public");
    }
}
