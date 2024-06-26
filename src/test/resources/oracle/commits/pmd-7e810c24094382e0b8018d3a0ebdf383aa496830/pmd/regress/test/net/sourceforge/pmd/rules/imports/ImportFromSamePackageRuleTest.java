/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
*/
package test.net.sourceforge.pmd.rules.imports;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleSetNotFoundException;
import test.net.sourceforge.pmd.testframework.SimpleAggregatorTst;
import test.net.sourceforge.pmd.testframework.TestDescriptor;

public class ImportFromSamePackageRuleTest extends SimpleAggregatorTst {

    private Rule rule;

    public void setUp() throws RuleSetNotFoundException {
        rule = findRule("imports", "ImportFromSamePackage");
    }

    public void testAll() {
       runTests(new TestDescriptor[] {
           new TestDescriptor(TEST1, "simple failure", 1, rule),
           new TestDescriptor(TEST2, "class in default package importing from sub package", 0, rule),
           new TestDescriptor(TEST3, "class in default package importing from other package", 0, rule),
           new TestDescriptor(TEST4, "class not in default package importing from default package", 0, rule),
           new TestDescriptor(TEST5, "class in default package importing from default package", 1, rule),
           new TestDescriptor(TEST6, "importing from subpackage", 0, rule),
           new TestDescriptor(TEST7, "importing all from same package", 1, rule),
       });
    }

    private static final String TEST1 =
    "package foo;" + PMD.EOL +
    "import foo.Bar;" + PMD.EOL +
    "public class Baz{}";

    private static final String TEST2 =
    "package foo;" + PMD.EOL +
    "import foo.buz.Bar;" + PMD.EOL +
    "public class Baz{}";

    private static final String TEST3 =
    "import java.util.*;" + PMD.EOL +
    "public class Baz{}";

    private static final String TEST4 =
    "package bar;" + PMD.EOL +
    "import Foo;" + PMD.EOL +
    "public class Baz{}";

    private static final String TEST5 =
    "import Foo;" + PMD.EOL +
    "public class Baz{}";

    private static final String TEST6 =
    "package foo.bar;" + PMD.EOL +
    "import foo.bar.baz.*;" + PMD.EOL +
    "public class Baz{}";

    private static final String TEST7 =
    "package foo.bar;" + PMD.EOL +
    "import foo.bar.*;" + PMD.EOL +
    "public class Baz{}";

}
