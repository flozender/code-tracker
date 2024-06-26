package com.puppycrawl.tools.checkstyle;

import junit.framework.TestCase;
import org.apache.regexp.RE;
import com.puppycrawl.tools.checkstyle.api.Utils;

public class UtilsTest
    extends TestCase
{
    public UtilsTest(String name)
    {
        super(name);
    }

    /**
     * Test Utils.countCharInString.
     */
    public void testLengthExpandedTabs()
        throws Exception
    {
        String s1 = "\t";
        assertEquals(8, Utils.lengthExpandedTabs(s1, s1.length(), 8));

        String s2 = "  \t";
        assertEquals(8, Utils.lengthExpandedTabs(s2, s2.length(), 8));

        String s3 = "\t\t";
        assertEquals(16, Utils.lengthExpandedTabs(s3, s3.length(), 8));

        String s4 = " \t ";
        assertEquals(9, Utils.lengthExpandedTabs(s4, s4.length(), 8));

        assertEquals(0, Utils.lengthMinusTrailingWhitespace(""));
        assertEquals(0, Utils.lengthMinusTrailingWhitespace(" \t "));
        assertEquals(3, Utils.lengthMinusTrailingWhitespace(" 23"));
        assertEquals(3, Utils.lengthMinusTrailingWhitespace(" 23 \t "));

        final RE r1 = Utils.getRE("a");
        final RE r2 = Utils.getRE("a");
        assertEquals(r1, r2);
    }

}
