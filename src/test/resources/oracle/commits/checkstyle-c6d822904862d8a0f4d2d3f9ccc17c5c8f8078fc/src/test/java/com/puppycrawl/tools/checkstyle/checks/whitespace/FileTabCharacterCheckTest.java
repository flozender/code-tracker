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

package com.puppycrawl.tools.checkstyle.checks.whitespace;

import static com.puppycrawl.tools.checkstyle.checks.whitespace.FileTabCharacterCheck.MSG_CONTAINS_TAB;
import static com.puppycrawl.tools.checkstyle.checks.whitespace.FileTabCharacterCheck.MSG_FILE_CONTAINS_TAB;

import java.io.File;

import org.junit.Test;

import com.puppycrawl.tools.checkstyle.AbstractModuleTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.Definitions;
import com.puppycrawl.tools.checkstyle.api.LocalizedMessage;

public class FileTabCharacterCheckTest
    extends AbstractModuleTestSupport {

    @Override
    protected String getPackageLocation() {
        return "com/puppycrawl/tools/checkstyle/checks/whitespace/filetabcharacter";
    }

    @Test
    public void testDefault() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(FileTabCharacterCheck.class);
        checkConfig.addAttribute("eachLine", "false");
        final String[] expected = {
            "19:25: " + getCheckMessage(MSG_FILE_CONTAINS_TAB),
        };
        final File[] files = {
            new File(getPath("InputFileTabCharacterSimple.java")),
        };
        verify(createChecker(checkConfig), files, getPath("InputFileTabCharacterSimple.java"),
            expected);
    }

    @Test
    public void testVerbose() throws Exception {
        final DefaultConfiguration checkConfig =
                createModuleConfig(FileTabCharacterCheck.class);
        checkConfig.addAttribute("eachLine", "true");
        final String[] expected = {
            "19:25: " + getCheckMessage(MSG_CONTAINS_TAB),
            "145:35: " + getCheckMessage(MSG_CONTAINS_TAB),
            "146:64: " + getCheckMessage(MSG_CONTAINS_TAB),
            "154:9: " + getCheckMessage(MSG_CONTAINS_TAB),
            "155:10: " + getCheckMessage(MSG_CONTAINS_TAB),
            "156:1: " + getCheckMessage(MSG_CONTAINS_TAB),
            "157:3: " + getCheckMessage(MSG_CONTAINS_TAB),
            "158:3: " + getCheckMessage(MSG_CONTAINS_TAB),
        };
        final File[] files = {
            new File(getPath("InputFileTabCharacterSimple.java")),
        };
        verify(createChecker(checkConfig), files, getPath("InputFileTabCharacterSimple.java"),
            expected);
    }

    @Test
    public void testBadFile() throws Exception {
        final DefaultConfiguration checkConfig =
                createModuleConfig(FileTabCharacterCheck.class);
        checkConfig.addAttribute("eachLine", "false");
        final String path = getPath("Claira");
        final String exceptionMessage = " (No such file or directory)";
        final LocalizedMessage localizedMessage = new LocalizedMessage(0,
                Definitions.CHECKSTYLE_BUNDLE, "general.exception",
                new String[] {path + exceptionMessage}, null, getClass(), null);

        final String[] expected = {
            "0: " + localizedMessage.getMessage(),
        };
        final File[] files = {
            new File(path),
        };
        verify(createChecker(checkConfig), files, path, expected);
    }
}
