////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2016 the original author or authors.
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

package com.puppycrawl.tools.checkstyle.checks;

import static com.puppycrawl.tools.checkstyle.checks.TranslationCheck.MSG_KEY;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import com.puppycrawl.tools.checkstyle.BaseCheckTestSupport;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.Configuration;

public class TranslationCheckTest extends BaseCheckTestSupport {
    @Override
    protected DefaultConfiguration createCheckerConfig(
        Configuration config) {
        final DefaultConfiguration dc = new DefaultConfiguration("root");
        dc.addChild(config);
        return dc;
    }

    @Override
    protected String getPath(String filename) throws IOException {
        return super.getPath("checks" + File.separator + filename);
    }

    @Override
    protected String getNonCompilablePath(String filename) throws IOException {
        return super.getNonCompilablePath("checks" + File.separator + filename);
    }

    @Test
    public void testTranslation() throws Exception {
        final Configuration checkConfig = createCheckConfig(TranslationCheck.class);
        final String[] expected = {
            "0: " + getCheckMessage(MSG_KEY, "only.english"),
        };
        final File[] propertyFiles = {
            new File(getPath("messages_test_de.properties")),
            new File(getPath("messages_test.properties")),
        };
        verify(
            createChecker(checkConfig),
            propertyFiles,
            getPath("messages_test_de.properties"),
            expected);
    }

    @Test
    public void testOnePropertyFileSet() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(TranslationCheck.class);
        final String[] expected = ArrayUtils.EMPTY_STRING_ARRAY;
        final File[] propertyFiles = {
            new File(getPath("app-dev.properties")),
        };
        verify(
            createChecker(checkConfig),
            propertyFiles,
            getPath("app-dev.properties"),
            expected);
    }

    @Test
    public void testLogIoExceptionFileNotFound() throws Exception {
        //I can't put wrong file here. Checkstyle fails before check started.
        //I saw some usage of file or handling of wrong file in Checker, or somewhere
        //in checks running part. So I had to do it with reflection to improve coverage.
        final TranslationCheck check = new TranslationCheck();
        final DefaultConfiguration checkConfig = createCheckConfig(TranslationCheck.class);
        check.configure(checkConfig);
        final Checker checker = createChecker(checkConfig);
        check.setMessageDispatcher(checker);

        final Method loadKeys =
            check.getClass().getDeclaredMethod("getTranslationKeys", File.class);
        loadKeys.setAccessible(true);
        loadKeys.invoke(check, new File(""));

    }

    @Test
    public void testLogIoException() throws Exception {
        //I can't put wrong file here. Checkstyle fails before check started.
        //I saw some usage of file or handling of wrong file in Checker, or somewhere
        //in checks running part. So I had to do it with reflection to improve coverage.
        final TranslationCheck check = new TranslationCheck();
        final DefaultConfiguration checkConfig = createCheckConfig(TranslationCheck.class);
        check.configure(checkConfig);
        check.setMessageDispatcher(createChecker(checkConfig));

        final Method logIoException = check.getClass().getDeclaredMethod("logIoException",
                IOException.class,
                File.class);
        logIoException.setAccessible(true);
        logIoException.invoke(check, new IOException("test exception"), new File(""));
    }

    @Test
    public void testDefaultTranslationFileIsMissing() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(TranslationCheck.class);
        checkConfig.addAttribute("requiredTranslations", "ja,,, de, ja");

        final File[] propertyFiles = {
            new File(getPath("messages_translation_de.properties")),
            new File(getPath("messages_translation_ja.properties")),
        };

        final String[] expected = {
            "0: Properties file 'messages_translation.properties' is missing.",
        };
        verify(
            createChecker(checkConfig),
            propertyFiles,
            getPath(""),
            expected);
    }

    @Test
    public void testTranslationFilesAreMissing() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(TranslationCheck.class);
        checkConfig.addAttribute("requiredTranslations", "ja, de");

        final File[] propertyFiles = {
            new File(getPath("messages_translation.properties")),
            new File(getPath("messages_translation_ja.properties")),
        };

        final String[] expected = {
            "0: Properties file 'messages_translation_de.properties' is missing.",
        };
        verify(
            createChecker(checkConfig),
            propertyFiles,
            getPath(""),
            expected);
    }

    @Test
    public void testBaseNameWithSeparatorDefaultTranslationIsMissing() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(TranslationCheck.class);
        checkConfig.addAttribute("requiredTranslations", "fr");

        final File[] propertyFiles = {
            new File(getPath("messages-translation_fr.properties")),
        };

        final String[] expected = {
            "0: Properties file 'messages-translation.properties' is missing.",
        };
        verify(
            createChecker(checkConfig),
            propertyFiles,
            getPath(""),
            expected);
    }

    @Test
    public void testBaseNameWithSeparatorTranslationsAreMissing() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(TranslationCheck.class);
        checkConfig.addAttribute("requiredTranslations", "fr, tr");

        final File[] propertyFiles = {
            new File(getPath("messages-translation.properties")),
            new File(getPath("messages-translation_fr.properties")),
        };

        final String[] expected = {
            "0: Properties file 'messages-translation_tr.properties' is missing.",
        };
        verify(
            createChecker(checkConfig),
            propertyFiles,
            getPath(""),
            expected);
    }

    @Test
    public void testIsNotMessagesBundle() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(TranslationCheck.class);
        checkConfig.addAttribute("requiredTranslations", "de");

        final File[] propertyFiles = {
            new File(getPath("app-dev.properties")),
            new File(getPath("app-stage.properties")),
        };

        final String[] expected = ArrayUtils.EMPTY_STRING_ARRAY;
        verify(
            createChecker(checkConfig),
            propertyFiles,
            getPath("app-dev.properties"),
            expected);
    }

    @Test
    public void testTranslationFileWithLanguageCountryVariantIsMissing() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(TranslationCheck.class);
        checkConfig.addAttribute("requiredTranslations", "es, de");

        final File[] propertyFiles = {
            new File(getPath("messages_home.properties")),
            new File(getPath("messages_home_es_US.properties")),
            new File(getPath("messages_home_fr_CA_UNIX.properties")),
            };

        final String[] expected = {
            "0: Properties file 'messages_home_de.properties' is missing.",
        };
        verify(
            createChecker(checkConfig),
            propertyFiles,
            getPath(""),
            expected);
    }

    @Test
    public void testTranslationFileWithLanguageCountryVariantArePresent() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(TranslationCheck.class);
        checkConfig.addAttribute("requiredTranslations", "es, fr");

        final File[] propertyFiles = {
            new File(getPath("messages_home.properties")),
            new File(getPath("messages_home_es_US.properties")),
            new File(getPath("messages_home_fr_CA_UNIX.properties")),
            };

        final String[] expected = ArrayUtils.EMPTY_STRING_ARRAY;
        verify(
            createChecker(checkConfig),
            propertyFiles,
            getPath(""),
            expected);
    }

    @Test
    public void testBaseNameOption() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(TranslationCheck.class);
        checkConfig.addAttribute("requiredTranslations", "de, es, fr, ja");
        checkConfig.addAttribute("baseName", "^.*Labels$");

        final File[] propertyFiles = {
            new File(getPath("ButtonLabels.properties")),
            new File(getPath("ButtonLabels_de.properties")),
            new File(getPath("ButtonLabels_es.properties")),
            new File(getPath("ButtonLabels_fr_CA_UNIX.properties")),
            new File(getPath("messages_home.properties")),
            new File(getPath("messages_home_es_US.properties")),
            new File(getPath("messages_home_fr_CA_UNIX.properties")),
        };

        final String[] expected = {
            "0: Properties file 'ButtonLabels_ja.properties' is missing.",
        };
        verify(
            createChecker(checkConfig),
            propertyFiles,
            getPath(""),
            expected);
    }

    @Test
    public void testFileExtensions() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(TranslationCheck.class);
        checkConfig.addAttribute("requiredTranslations", "de, es, fr, ja");
        checkConfig.addAttribute("fileExtensions", "properties,translation");
        checkConfig.addAttribute("baseName", "^.*(Titles|Labels)$");

        final File[] propertyFiles = {
            new File(getPath("ButtonLabels.properties")),
            new File(getPath("ButtonLabels_de.properties")),
            new File(getPath("ButtonLabels_es.properties")),
            new File(getPath("ButtonLabels_fr_CA_UNIX.properties")),
            new File(getPath("PageTitles.translation")),
            new File(getPath("PageTitles_de.translation")),
            new File(getPath("PageTitles_es.translation")),
            new File(getPath("PageTitles_fr.translation")),
            new File(getPath("PageTitles_ja.translation")),
        };

        final String[] expected = {
            "0: Properties file 'ButtonLabels_ja.properties' is missing.",
        };

        verify(
            createChecker(checkConfig),
            propertyFiles,
            getPath(""),
            expected);
    }

    @Test
    public void testEqualBaseNamesButDifferentExtensions() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(TranslationCheck.class);
        checkConfig.addAttribute("requiredTranslations", "de, es, fr, ja");
        checkConfig.addAttribute("fileExtensions", "properties,translations");
        checkConfig.addAttribute("baseName", "^.*Labels$");

        final File[] propertyFiles = {
            new File(getPath("ButtonLabels.properties")),
            new File(getPath("ButtonLabels_de.properties")),
            new File(getPath("ButtonLabels_es.properties")),
            new File(getPath("ButtonLabels_fr_CA_UNIX.properties")),
            new File(getPath("ButtonLabels.translations")),
            new File(getPath("ButtonLabels_ja.translations")),
            new File(getPath("ButtonLabels_es.translations")),
            new File(getPath("ButtonLabels_fr_CA_UNIX.translations")),
            new File(getPath("ButtonLabels_de.translations")),
        };

        final String[] expected = {
            "0: Properties file 'ButtonLabels_ja.properties' is missing.",
        };

        verify(
            createChecker(checkConfig),
            propertyFiles,
            getPath(""),
            expected);
    }

    @Test
    public void testRegexpToMatchPartOfBaseName() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(TranslationCheck.class);
        checkConfig.addAttribute("requiredTranslations", "de, es, fr, ja");
        checkConfig.addAttribute("fileExtensions", "properties,translations");
        checkConfig.addAttribute("baseName", "^.*Labels.*");

        final File[] propertyFiles = {
            new File(getPath("MyLabelsI18.properties")),
            new File(getPath("MyLabelsI18_de.properties")),
            new File(getPath("MyLabelsI18_es.properties")),
        };

        final String[] expected = {
            "0: Properties file 'MyLabelsI18_fr.properties' is missing.",
            "0: Properties file 'MyLabelsI18_ja.properties' is missing.",
            };

        verify(
            createChecker(checkConfig),
            propertyFiles,
            getPath(""),
            expected);
    }

    @Test
    public void testBundlesWithSameNameButDifferentPaths() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(TranslationCheck.class);
        checkConfig.addAttribute("requiredTranslations", "de");
        checkConfig.addAttribute("fileExtensions", "properties");
        checkConfig.addAttribute("baseName", "^.*Labels.*");

        final File[] propertyFiles = {
            new File(getPath("MyLabelsI18.properties")),
            new File(getPath("MyLabelsI18_de.properties")),
            new File(getNonCompilablePath("MyLabelsI18.properties")),
            new File(getNonCompilablePath("MyLabelsI18_de.properties")),
        };

        final String[] expected = ArrayUtils.EMPTY_STRING_ARRAY;

        verify(
            createChecker(checkConfig),
            propertyFiles,
            getPath(""),
            expected);
    }
}
