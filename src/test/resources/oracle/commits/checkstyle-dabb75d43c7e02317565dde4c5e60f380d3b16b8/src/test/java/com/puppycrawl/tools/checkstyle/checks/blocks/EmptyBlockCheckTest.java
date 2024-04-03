////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2018 the original author or authors.
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

package com.puppycrawl.tools.checkstyle.checks.blocks;

import static com.puppycrawl.tools.checkstyle.checks.blocks.EmptyBlockCheck.MSG_KEY_BLOCK_EMPTY;
import static com.puppycrawl.tools.checkstyle.checks.blocks.EmptyBlockCheck.MSG_KEY_BLOCK_NO_STATEMENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.puppycrawl.tools.checkstyle.AbstractModuleTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.utils.CommonUtils;

public class EmptyBlockCheckTest
    extends AbstractModuleTestSupport {
    @Override
    protected String getPackageLocation() {
        return "com/puppycrawl/tools/checkstyle/checks/blocks/emptyblock";
    }

    /* Additional test for jacoco, since valueOf()
     * is generated by javac and jacoco reports that
     * valueOf() is uncovered.
     */
    @Test
    public void testBlockOptionValueOf() {
        final BlockOption option = BlockOption.valueOf("TEXT");
        assertEquals("Invalid valueOf result", BlockOption.TEXT, option);
    }

    @Test
    public void testDefault()
            throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(EmptyBlockCheck.class);
        final String[] expected = {
            "33:13: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
            "35:17: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
            "37:13: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
            "40:17: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
            "63:5: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
            "71:29: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
            "73:41: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
            "84:12: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
        };
        verify(checkConfig, getPath("InputEmptyBlockSemantic.java"), expected);
    }

    @Test
    public void testText()
            throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(EmptyBlockCheck.class);
        checkConfig.addAttribute("option", BlockOption.TEXT.toString());
        final String[] expected = {
            "33:13: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "try"),
            "35:17: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "finally"),
            "63:5: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "INSTANCE_INIT"),
            "71:29: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "synchronized"),
            "84:12: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "STATIC_INIT"),
        };
        verify(checkConfig, getPath("InputEmptyBlockSemantic.java"), expected);
    }

    @Test
    public void testStatement()
            throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(EmptyBlockCheck.class);
        checkConfig.addAttribute("option", BlockOption.STATEMENT.toString());
        final String[] expected = {
            "33:13: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
            "35:17: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
            "37:13: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
            "40:17: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
            "63:5: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
            "71:29: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
            "73:41: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
            "84:12: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
        };
        verify(checkConfig, getPath("InputEmptyBlockSemantic.java"), expected);
    }

    @Test
    public void allowEmptyLoops() throws Exception {
        final DefaultConfiguration checkConfig =
                createModuleConfig(EmptyBlockCheck.class);
        checkConfig.addAttribute("option", BlockOption.STATEMENT.toString());
        checkConfig.addAttribute("tokens", "LITERAL_TRY, LITERAL_CATCH,"
                + "LITERAL_FINALLY, LITERAL_DO, LITERAL_IF,"
                + "LITERAL_ELSE, INSTANCE_INIT, STATIC_INIT, LITERAL_SWITCH");
        final String[] expected = {
            "16:29: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
            "19:42: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
            "22:29: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
            "23:28: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT),
        };
        verify(checkConfig, getPath("InputEmptyBlockSemantic2.java"), expected);
    }

    @Test
    public void allowEmptyLoopsText() throws Exception {
        final DefaultConfiguration checkConfig =
                createModuleConfig(EmptyBlockCheck.class);
        checkConfig.addAttribute("option", BlockOption.TEXT.toString());
        checkConfig.addAttribute("tokens", "LITERAL_TRY, LITERAL_CATCH,"
                + "LITERAL_FINALLY, LITERAL_DO, LITERAL_IF,"
                + "LITERAL_ELSE, INSTANCE_INIT, STATIC_INIT, LITERAL_SWITCH");
        final String[] expected = {
            "16:29: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "if"),
            "19:42: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "if"),
            "22:29: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "if"),
            "23:28: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "switch"),
        };
        verify(checkConfig, getPath("InputEmptyBlockSemantic2.java"), expected);
    }

    @Test
    public void testInvalidOption() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(EmptyBlockCheck.class);
        checkConfig.addAttribute("option", "invalid_option");

        try {
            final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;

            verify(checkConfig, getPath("InputEmptyBlockSemantic.java"), expected);
            fail("exception expected");
        }
        catch (CheckstyleException ex) {
            final String messageStart =
                "cannot initialize module com.puppycrawl.tools.checkstyle.TreeWalker - "
                    + "Cannot set property 'option' to 'invalid_option' in module";

            assertTrue("Invalid exception message, should start with: " + messageStart,
                ex.getMessage().startsWith(messageStart));
        }
    }

    @Test
    public void testAllowEmptyCaseWithText() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(EmptyBlockCheck.class);
        checkConfig.addAttribute("option", BlockOption.TEXT.toString());
        checkConfig.addAttribute("tokens", "LITERAL_CASE");
        final String[] expected = {
            "12:28: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "case"),
            "18:13: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "case"),
            "29:29: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "case"),
            "31:37: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "case"),
            "32:29: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "case"),
        };
        verify(checkConfig, getPath("InputEmptyBlockCase.java"), expected);
    }

    @Test
    public void testForbidCaseWithoutStmt() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(EmptyBlockCheck.class);
        checkConfig.addAttribute("option", BlockOption.STATEMENT.toString());
        checkConfig.addAttribute("tokens", "LITERAL_CASE");
        final String[] expected = {
            "12:28: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT, "case"),
            "18:13: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT, "case"),
            "22:13: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT, "case"),
            "29:29: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT, "case"),
            "31:37: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT, "case"),
            "32:29: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT, "case"),
            "32:40: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT, "case"),
        };
        verify(checkConfig, getPath("InputEmptyBlockCase.java"), expected);
    }

    @Test
    public void testAllowEmptyDefaultWithText() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(EmptyBlockCheck.class);
        checkConfig.addAttribute("option", BlockOption.TEXT.toString());
        checkConfig.addAttribute("tokens", "LITERAL_DEFAULT");
        final String[] expected = {
            "5:30: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "default"),
            "11:13: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "default"),
            "36:22: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "default"),
            "44:47: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "default"),
            "50:22: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "default"),
            "78:13: " + getCheckMessage(MSG_KEY_BLOCK_EMPTY, "default"),
        };
        verify(checkConfig, getPath("InputEmptyBlockDefault.java"), expected);
    }

    @Test
    public void testForbidDefaultWithoutStatement() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(EmptyBlockCheck.class);
        checkConfig.addAttribute("option", BlockOption.STATEMENT.toString());
        checkConfig.addAttribute("tokens", "LITERAL_DEFAULT");
        final String[] expected = {
            "5:30: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT, "default"),
            "11:13: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT, "default"),
            "15:13: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT, "default"),
            "26:30: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT, "default"),
            "36:22: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT, "default"),
            "44:47: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT, "default"),
            "50:22: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT, "default"),
            "65:22: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT, "default"),
            "78:13: " + getCheckMessage(MSG_KEY_BLOCK_NO_STATEMENT, "default"),
        };
        verify(checkConfig, getPath("InputEmptyBlockDefault.java"), expected);
    }

    @Test
    public void testAnnotationDefaultKeyword() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(EmptyBlockCheck.class);
        checkConfig.addAttribute("option", BlockOption.STATEMENT.toString());
        checkConfig.addAttribute("tokens", "LITERAL_DEFAULT");
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;
        final String path = getPath("InputEmptyBlockAnnotationDefaultKeyword.java");
        verify(checkConfig, path, expected);
    }
}
