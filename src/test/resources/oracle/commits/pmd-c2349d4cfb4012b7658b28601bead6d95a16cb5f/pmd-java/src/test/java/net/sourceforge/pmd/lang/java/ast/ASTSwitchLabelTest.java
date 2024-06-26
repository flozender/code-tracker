package net.sourceforge.pmd.lang.java.ast;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.lang.java.ParserTst;

public class ASTSwitchLabelTest extends ParserTst {

    @Test
    public void testDefaultOff() throws Throwable {
        Set<ASTSwitchLabel> ops = getNodes(ASTSwitchLabel.class, TEST1);
        assertFalse(ops.iterator().next().isDefault());
    }

    @Test
    public void testDefaultSet() throws Throwable {
        Set<ASTSwitchLabel> ops = getNodes(ASTSwitchLabel.class, TEST2);
        assertTrue(ops.iterator().next().isDefault());
    }

    private static final String TEST1 = "public class Foo {" + PMD.EOL + " void bar() {" + PMD.EOL + "  switch (x) {"
            + PMD.EOL + "   case 1: y = 2;" + PMD.EOL + "  }" + PMD.EOL + " }" + PMD.EOL + "}";

    private static final String TEST2 = "public class Foo {" + PMD.EOL + " void bar() {" + PMD.EOL + "  switch (x) {"
            + PMD.EOL + "   default: y = 2;" + PMD.EOL + "  }" + PMD.EOL + " }" + PMD.EOL + "}";

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(ASTSwitchLabelTest.class);
    }
}
