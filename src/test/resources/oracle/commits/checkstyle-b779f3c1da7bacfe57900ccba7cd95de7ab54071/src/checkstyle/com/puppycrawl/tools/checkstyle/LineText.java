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
package com.puppycrawl.tools.checkstyle;

/**
 * Represents a string with an associated line number.
 * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
 **/
class LineText
    implements Comparable
{
    /** the string **/
    private String mText;
    /** the line number **/
    private final int mLineNo;

    /**
     * Constructs the object.
     * @param aLineNo the line number
     * @param aText the text
     **/
    LineText(int aLineNo, String aText)
    {
        mLineNo = aLineNo;
        mText = aText;
    }

    /**
     * Constructs the object.
     * @param aOther the object to initialise from
     **/
    LineText(LineText aOther)
    {
        this(aOther.getLineNo(), aOther.getText());
    }

    /** @return the text **/
    String getText()
    {
        return mText;
    }

    /** @return the line number **/
    int getLineNo()
    {
        return mLineNo;
    }

    /** Appends to the string.
    * @param aText the text to append
    **/
    void appendText(String aText)
    {
        mText += aText;
    }

    /** @return a string representation of the object **/
    public String toString()
    {
        return "{Text = '" + getText() + "', Line = " + getLineNo() + "}";
    }

    ////////////////////////////////////////////////////////////////////////////
    // Interface Comparable methods
    ////////////////////////////////////////////////////////////////////////////

    /** @see java.lang.Comparable **/
    public int compareTo(Object aOther)
    {
        final LineText lt = (LineText) aOther;
        if (getLineNo() == lt.getLineNo()) {
            return 0;
        }
        else if (getLineNo() < lt.getLineNo()) {
            return -1;
        }
        else {
            return 1;
        }
    }
}


