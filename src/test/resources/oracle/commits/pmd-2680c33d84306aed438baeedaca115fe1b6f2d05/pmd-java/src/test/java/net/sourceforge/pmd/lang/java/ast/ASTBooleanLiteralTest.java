
package net.sourceforge.pmd.lang.java.ast;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.lang.java.ParserTst;

public class ASTBooleanLiteralTest extends ParserTst {

    @Test
    public void testTrue() throws Throwable {
        Set<ASTBooleanLiteral> ops = getNodes(ASTBooleanLiteral.class, TEST1);
        ASTBooleanLiteral b = ops.iterator().next();
        assertTrue(b.isTrue());
    }

    @Test
    public void testFalse() throws Throwable {
        Set<ASTBooleanLiteral> ops = getNodes(ASTBooleanLiteral.class, TEST2);
        ASTBooleanLiteral b = ops.iterator().next();
        assertFalse(b.isTrue());
    }

    private static final String TEST1 = "class Foo { " + PMD.EOL + " boolean bar = true; " + PMD.EOL + "} ";

    private static final String TEST2 = "class Foo { " + PMD.EOL + " boolean bar = false; " + PMD.EOL + "} ";

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(ASTBooleanLiteralTest.class);
    }
}
