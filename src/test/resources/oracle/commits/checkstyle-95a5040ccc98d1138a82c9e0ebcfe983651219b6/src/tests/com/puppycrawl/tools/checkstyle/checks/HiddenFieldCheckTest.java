package com.puppycrawl.tools.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.BaseCheckTestCase;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;

public class HiddenFieldCheckTest
    extends BaseCheckTestCase
{
    public void testNoParameters()
        throws Exception
    {
        final DefaultConfiguration checkConfig =
            createCheckConfig(HiddenFieldCheck.class);
        checkConfig.addAttribute("tokens", "VARIABLE_DEF");
        final String[] expected = {
            "18:13: 'hidden' hides a field.",
            "27:13: 'hidden' hides a field.",
            "32:18: 'hidden' hides a field.",
            "46:17: 'innerHidden' hides a field.",
            "55:17: 'innerHidden' hides a field.",
            "56:17: 'hidden' hides a field.",
            "61:22: 'innerHidden' hides a field.",
            "64:22: 'hidden' hides a field.",
            "76:17: 'innerHidden' hides a field.",
            "77:17: 'hidden' hides a field.",
            "82:13: 'hidden' hides a field.",
        };
        verify(checkConfig, getPath("InputHiddenField.java"), expected);
    }

    public void testDefault()
        throws Exception
    {
        final DefaultConfiguration checkConfig =
            createCheckConfig(HiddenFieldCheck.class);
        final String[] expected = {
            "18:13: 'hidden' hides a field.",
            "21:33: 'hidden' hides a field.",
            "27:13: 'hidden' hides a field.",
            "32:18: 'hidden' hides a field.",
            "36:33: 'hidden' hides a field.",
            "46:17: 'innerHidden' hides a field.",
            "49:26: 'innerHidden' hides a field.",
            "55:17: 'innerHidden' hides a field.",
            "56:17: 'hidden' hides a field.",
            "61:22: 'innerHidden' hides a field.",
            "64:22: 'hidden' hides a field.",
            "69:17: 'innerHidden' hides a field.",
            "70:17: 'hidden' hides a field.",
            "76:17: 'innerHidden' hides a field.",
            "77:17: 'hidden' hides a field.",
            "82:13: 'hidden' hides a field.",
        };
        verify(checkConfig, getPath("InputHiddenField.java"), expected);
    }
    
    /** Test against a class with field declarations in different order */
    public void testReordered()
        throws Exception
    {
        final DefaultConfiguration checkConfig =
            createCheckConfig(HiddenFieldCheck.class);
        final String[] expected = {
            "18:13: 'hidden' hides a field.",
            "21:40: 'hidden' hides a field.",
            "27:13: 'hidden' hides a field.",
            "32:18: 'hidden' hides a field.",
            "36:33: 'hidden' hides a field.",
            "46:17: 'innerHidden' hides a field.",
            "49:26: 'innerHidden' hides a field.",
            "55:17: 'innerHidden' hides a field.",
            "56:17: 'hidden' hides a field.",
            "61:22: 'innerHidden' hides a field.",
            "64:22: 'hidden' hides a field.",
            "69:17: 'innerHidden' hides a field.",
            "70:17: 'hidden' hides a field.",
            "76:17: 'innerHidden' hides a field.",
            "77:17: 'hidden' hides a field.",
            "83:13: 'hidden' hides a field.",
        };
        verify(checkConfig, getPath("InputHiddenFieldReorder.java"), expected);
    }
}

