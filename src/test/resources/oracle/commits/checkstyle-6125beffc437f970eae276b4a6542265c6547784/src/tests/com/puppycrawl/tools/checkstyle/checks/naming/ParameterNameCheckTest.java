package com.puppycrawl.tools.checkstyle.checks.naming;

import com.puppycrawl.tools.checkstyle.BaseCheckTestCase;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;

public class ParameterNameCheckTest
    extends BaseCheckTestCase
{
    public void testCatch()
        throws Exception
    {
        final DefaultConfiguration checkConfig =
            createCheckConfig(ParameterNameCheck.class);
        checkConfig.addAttribute("format", "^NO_WAY_MATEY$");
        final String[] expected = {
        };
        verify(checkConfig, getPath("InputLeftCurlyOther.java"), expected);
    }

    public void testSpecified()
        throws Exception
    {
        final DefaultConfiguration checkConfig =
            createCheckConfig(ParameterNameCheck.class);
        checkConfig.addAttribute("format", "^a[A-Z][a-zA-Z0-9]*$");
        final String[] expected = {
            "71:19: Name 'badFormat1' must match pattern '^a[A-Z][a-zA-Z0-9]*$'.",
            "71:34: Name 'badFormat2' must match pattern '^a[A-Z][a-zA-Z0-9]*$'.",
            "72:25: Name 'badFormat3' must match pattern '^a[A-Z][a-zA-Z0-9]*$'.",
            "207:21: Name 'O' must match pattern '^a[A-Z][a-zA-Z0-9]*$'.",
        };
        verify(checkConfig, getPath("InputSimple.java"), expected);
    }

    public void testDefault()
        throws Exception
    {
        final DefaultConfiguration checkConfig =
            createCheckConfig(ParameterNameCheck.class);
        final String[] expected = {
            "207:21: Name 'O' must match pattern '^[a-z][a-zA-Z0-9]*$'.",
        };
        verify(checkConfig, getPath("InputSimple.java"), expected);
    }
}
