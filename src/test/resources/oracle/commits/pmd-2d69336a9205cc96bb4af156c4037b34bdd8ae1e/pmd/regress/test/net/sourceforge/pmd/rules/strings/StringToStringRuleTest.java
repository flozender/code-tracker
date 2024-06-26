/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package test.net.sourceforge.pmd.rules.strings;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.Rule;
import test.net.sourceforge.pmd.testframework.SimpleAggregatorTst;
import test.net.sourceforge.pmd.testframework.TestDescriptor;

public class StringToStringRuleTest extends SimpleAggregatorTst {

    private Rule rule;

    public void setUp() throws Exception {
        rule = findRule("strings", "StringToString");
    }

    public void testAll() {
        runTests(new TestDescriptor[]{
            new TestDescriptor(TEST1, "local var", 1, rule),
            new TestDescriptor(TEST2, "parameter", 1, rule),
            new TestDescriptor(TEST3, "field", 1, rule),
            new TestDescriptor(TEST4, "primitive", 0, rule),
            new TestDescriptor(TEST5, "multiple similar params", 0, rule),
            new TestDescriptor(TEST6, "string array", 1, rule)
        });
    }

    private static final String TEST1 =
            "public class Foo {" + PMD.EOL +
            " private String baz() {" + PMD.EOL +
            "  String bar = \"howdy\";" + PMD.EOL +
            "  return bar.toString();" + PMD.EOL +
            " }" + PMD.EOL +
            "}";

    private static final String TEST2 =
            "public class Foo {" + PMD.EOL +
            " private String baz(String bar) {" + PMD.EOL +
            "  return bar.toString();" + PMD.EOL +
            " }" + PMD.EOL +
            "}";

    private static final String TEST3 =
            "public class Foo {" + PMD.EOL +
            " private String baz;" + PMD.EOL +
            " private int getBaz() {" + PMD.EOL +
            "  return baz.toString();" + PMD.EOL +
            " }" + PMD.EOL +
            "}";

    private static final String TEST4 =
            "public class Foo {" + PMD.EOL +
            " private int baz;" + PMD.EOL +
            " private int getBaz() {" + PMD.EOL +
            "  return baz;" + PMD.EOL +
            " }" + PMD.EOL +
            "}";

    private static final String TEST5 =
            "public class Foo {" + PMD.EOL +
            " private String getBaz(String foo, StringBuffer buffer) {" + PMD.EOL +
            "  return buffer.toString();" + PMD.EOL +
            " }" + PMD.EOL +
            "}";

    private static final String TEST6 =
            "public class Foo {" + PMD.EOL +
            " private String getBaz() {" + PMD.EOL +
            "  String[] foo = {\"hi\"};" + PMD.EOL +
            "  return foo[0].toString();" + PMD.EOL +
            " }" + PMD.EOL +
            "}";

}
