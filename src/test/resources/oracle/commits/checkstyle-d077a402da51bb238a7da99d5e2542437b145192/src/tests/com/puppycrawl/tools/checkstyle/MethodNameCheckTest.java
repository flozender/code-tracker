package com.puppycrawl.tools.checkstyle;

import com.puppycrawl.tools.checkstyle.checks.AvoidStarImport;
import com.puppycrawl.tools.checkstyle.checks.ParameterFormatCheck;
import com.puppycrawl.tools.checkstyle.checks.TypeNameCheck;
import com.puppycrawl.tools.checkstyle.checks.MethodNameCheck;

public class MethodNameCheckTest
    extends BaseCheckTestCase
{
    public MethodNameCheckTest(String aName)
    {
        super(aName);
    }

    public void testDefault()
        throws Exception
    {
        final CheckConfiguration checkConfig = new CheckConfiguration();
        checkConfig.setClassname(MethodNameCheck.class.getName());
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("InputSimple.java");
        final String[] expected = {
            "137:10: Name 'ALL_UPPERCASE_METHOD' must match pattern '^[a-z][a-zA-Z0-9]*$'.",
        };
        verify(c, fname, expected);
    }
}
