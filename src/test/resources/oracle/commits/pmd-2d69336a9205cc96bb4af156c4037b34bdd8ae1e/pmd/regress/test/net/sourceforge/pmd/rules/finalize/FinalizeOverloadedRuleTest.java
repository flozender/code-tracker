package test.net.sourceforge.pmd.rules.finalize;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleSetNotFoundException;
import test.net.sourceforge.pmd.testframework.SimpleAggregatorTst;
import test.net.sourceforge.pmd.testframework.TestDescriptor;

public class FinalizeOverloadedRuleTest extends SimpleAggregatorTst {

    private Rule rule;

    public void setUp() throws RuleSetNotFoundException {
        rule = findRule("finalizers", "FinalizeOverloaded");
    }

    public void testAll() {
        runTests(new TestDescriptor[]{
            new TestDescriptor(TEST1, "bad", 1, rule),
            new TestDescriptor(TEST2, "ok", 0, rule),
        });
    }

    private static final String TEST1 =
            "public class Foo {" + PMD.EOL +
            " public void finalize(int foo) {}" + PMD.EOL +
            "}";

    private static final String TEST2 =
            "public class Foo {" + PMD.EOL +
            " public void finalize() {" + PMD.EOL +
            "  doSomething();" + PMD.EOL +
            " }" + PMD.EOL +
            "}";
}
