package com.puppycrawl.tools.checkstyle;

import com.puppycrawl.tools.checkstyle.checks.AvoidStarImport;
import com.puppycrawl.tools.checkstyle.checks.ParameterFormatCheck;
import com.puppycrawl.tools.checkstyle.checks.TypeNameCheck;

public class TypeNameCheckTest
    extends BaseCheckTestCase
{
    public TypeNameCheckTest(String aName)
    {
        super(aName);
    }

    public void testSpecified()
        throws Exception
    {
        final CheckConfiguration checkConfig = new CheckConfiguration();
        checkConfig.setClassname(TypeNameCheck.class.getName());
        checkConfig.addProperty("format", "^inputHe");
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("inputHeader.java");
        final String[] expected = {
        };
        verify(c, fname, expected);
    }

    public void testDefault()
        throws Exception
    {
        final CheckConfiguration checkConfig = new CheckConfiguration();
        checkConfig.setClassname(TypeNameCheck.class.getName());
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("inputHeader.java");
        final String[] expected = {
            "1:48: Name 'inputHeader' must match pattern '^[A-Z][a-zA-Z0-9]*$'."
        };
        verify(c, fname, expected);
    }
}
