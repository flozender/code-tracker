package test.net.sourceforge.pmd.ast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.ast.ASTCompilationUnit;
import net.sourceforge.pmd.ast.ASTFieldDeclaration;
import net.sourceforge.pmd.ast.ASTType;
import net.sourceforge.pmd.ast.ASTVariableDeclarator;
import net.sourceforge.pmd.ast.ASTVariableDeclaratorId;
import net.sourceforge.pmd.ast.Dimensionable;

import org.junit.Test;

import test.net.sourceforge.pmd.testframework.ParserTst;

public class ASTFieldDeclarationTest extends ParserTst {

    @Test
    public void testIsArray() {
        ASTCompilationUnit cu = parseJava14(TEST1);
        Dimensionable node = cu.findChildrenOfType(ASTFieldDeclaration.class).get(0);
        assertTrue(node.isArray());
        assertEquals(1, node.getArrayDepth());
    }

    @Test
    public void testMultiDimensionalArray() {
        ASTCompilationUnit cu = parseJava14(TEST2);
        Dimensionable node = cu.findChildrenOfType(ASTFieldDeclaration.class).get(0);
        assertEquals(3, node.getArrayDepth());
    }

    @Test
    public void testIsSyntacticallyPublic() {
        ASTCompilationUnit cu = parseJava14(TEST3);
        ASTFieldDeclaration node = cu.findChildrenOfType(ASTFieldDeclaration.class).get(0);
        assertFalse(node.isSyntacticallyPublic());
        assertFalse(node.isPackagePrivate());
        assertFalse(node.isPrivate());
        assertFalse(node.isProtected());
        assertTrue(node.isFinal());
        assertTrue(node.isStatic());
        assertTrue(node.isPublic());
    }

    @Test
    public void testWithEnum() {
        ASTCompilationUnit cu = parseJava15(TEST4);
        ASTFieldDeclaration node = cu.findChildrenOfType(ASTFieldDeclaration.class).get(0);
        assertFalse(node.isInterfaceMember());
    }

    private static final String TEST1 =
            "class Foo {" + PMD.EOL +
            " String[] foo;" + PMD.EOL +
            "}";

    private static final String TEST2 =
            "class Foo {" + PMD.EOL +
            " String[][][] foo;" + PMD.EOL +
            "}";

    private static final String TEST3 =
            "interface Foo {" + PMD.EOL +
            " int BAR = 6;" + PMD.EOL +
            "}";

    private static final String TEST4 =
            "public enum Foo {" + PMD.EOL +
            " FOO(1);" + PMD.EOL +
            " private int x;" + PMD.EOL +
            "}";

    @Test
    public void testGetVariableName() {
        int id = 0;
        ASTFieldDeclaration n = new ASTFieldDeclaration(id++);
        ASTType t = new ASTType(id++);
        ASTVariableDeclarator decl = new ASTVariableDeclarator(id++);
        ASTVariableDeclaratorId declid = new ASTVariableDeclaratorId(id++);
        n.jjtAddChild(t, 0);
        t.jjtAddChild(decl, 0);
        decl.jjtAddChild(declid, 0);
        declid.setImage("foo");

        assertEquals("foo", n.getVariableName());

    }

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(ASTFieldDeclarationTest.class);
    }
}
