/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package test.net.sourceforge.pmd.rules.imports;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleSetNotFoundException;
import test.net.sourceforge.pmd.testframework.SimpleAggregatorTst;
import test.net.sourceforge.pmd.testframework.TestDescriptor;

public class DontImportJavaLangRuleTest extends SimpleAggregatorTst {

    private Rule rule;

    public void setUp() throws RuleSetNotFoundException {
        rule = findRule("imports", "DontImportJavaLang");
    }

    public void testAll() {
        runTests(new TestDescriptor[]{
            new TestDescriptor(TEST1, "import java.lang.String", 1, rule),
            new TestDescriptor(TEST2, "import java.lang.*", 1, rule),
            new TestDescriptor(TEST3, "import java.lang.ref/reflect/annotation/instrument/management", 0, rule),
        });
    }

    private static final String TEST1 =
            "import java.lang.String;" + PMD.EOL +
            "public class Foo {}";

    private static final String TEST2 =
            "import java.lang.*;" + PMD.EOL +
            "public class Foo {}";

    private static final String TEST3 =
            "import java.lang.ref.*;" + PMD.EOL +
            "import java.lang.reflect.*;" + PMD.EOL +
            "import java.lang.annotation.*;" + PMD.EOL +
            "import java.lang.instrument.*;" + PMD.EOL +
            "import java.lang.management.*;" + PMD.EOL +
            "public class Foo {}";

}
