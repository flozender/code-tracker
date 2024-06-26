package test.net.sourceforge.pmd.ast;

import static org.junit.Assert.assertEquals;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.ast.ASTCompilationUnit;
import net.sourceforge.pmd.ast.ASTLocalVariableDeclaration;

import org.junit.Test;

import test.net.sourceforge.pmd.testframework.ParserTst;

public class ASTLocalVariableDeclarationTest extends ParserTst {

    @Test
    public void testSingleDimArray() {
        ASTCompilationUnit cu = parseJava14(TEST1);
        ASTLocalVariableDeclaration node = cu.findChildrenOfType(ASTLocalVariableDeclaration.class).get(0);
        assertEquals(1, node.getArrayDepth());
    }

    @Test
    public void testMultDimArray() {
        ASTCompilationUnit cu = parseJava14(TEST2);
        ASTLocalVariableDeclaration node = cu.findChildrenOfType(ASTLocalVariableDeclaration.class).get(0);
        assertEquals(2, node.getArrayDepth());
    }

    @Test
    public void testMultDimArraySplitBraces() {
        ASTCompilationUnit cu = parseJava14(TEST3);
        ASTLocalVariableDeclaration node = cu.findChildrenOfType(ASTLocalVariableDeclaration.class).get(0);
        assertEquals(3, node.getArrayDepth());
    }

    private static final String TEST1 =
            "class Foo {" + PMD.EOL +
            " void bar() {int x[] = null;}" + PMD.EOL +
            "}";

    private static final String TEST2 =
            "class Foo {" + PMD.EOL +
            " void bar() {int x[][] = null;}" + PMD.EOL +
            "}";

    private static final String TEST3 =
            "class Foo {" + PMD.EOL +
            " void bar() {int[] x[][] = null;}" + PMD.EOL +
            "}";

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(ASTLocalVariableDeclarationTest.class);
    }
}
