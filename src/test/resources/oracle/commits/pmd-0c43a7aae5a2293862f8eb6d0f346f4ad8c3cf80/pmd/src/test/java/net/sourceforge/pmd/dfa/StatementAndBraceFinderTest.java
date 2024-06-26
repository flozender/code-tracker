/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.dfa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.dfa.DataFlowNode;
import net.sourceforge.pmd.lang.dfa.NodeType;
import net.sourceforge.pmd.lang.java.JavaLanguageModule;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.ast.ASTConstructorDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTExpression;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTStatementExpression;
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclarator;
import net.sourceforge.pmd.lang.java.dfa.StatementAndBraceFinder;
import net.sourceforge.pmd.testframework.ParserTst;

import org.junit.Test;


public class StatementAndBraceFinderTest extends ParserTst {

    @Test
    public void testStatementExpressionParentChildLinks() throws Throwable {
        ASTStatementExpression se = getOrderedNodes(ASTStatementExpression.class, TEST1).get(0);
        ASTMethodDeclaration seParent = (ASTMethodDeclaration) ((DataFlowNode) se.getDataFlowNode().getParents().get(0)).getNode();
        assertEquals(se, ((DataFlowNode) seParent.getDataFlowNode().getChildren().get(0)).getNode());
        assertEquals(seParent, ((DataFlowNode) se.getDataFlowNode().getParents().get(0)).getNode());
    }

    @Test
    public void testVariableDeclaratorParentChildLinks() throws Throwable {
        ASTVariableDeclarator vd = getOrderedNodes(ASTVariableDeclarator.class, TEST2).get(0);
        ASTMethodDeclaration vdParent = (ASTMethodDeclaration) ((DataFlowNode) vd.getDataFlowNode().getParents().get(0)).getNode();
        assertEquals(vd, ((DataFlowNode) vdParent.getDataFlowNode().getChildren().get(0)).getNode());
        assertEquals(vdParent, ((DataFlowNode) vd.getDataFlowNode().getParents().get(0)).getNode());
    }

    @Test
    public void testIfStmtHasCorrectTypes() throws Throwable {
        ASTExpression exp = getOrderedNodes(ASTExpression.class, TEST3).get(0);
        DataFlowNode dfn = exp.getDataFlowNode().getFlow().get(2);
        assertTrue(dfn.isType(NodeType.IF_EXPR));
        assertTrue(dfn.isType(NodeType.IF_LAST_STATEMENT_WITHOUT_ELSE));
    }

    @Test
    public void testWhileStmtHasCorrectTypes() throws Throwable {
        ASTExpression exp = getOrderedNodes(ASTExpression.class, TEST4).get(0);
        DataFlowNode dfn = exp.getDataFlowNode().getFlow().get(2);
        assertTrue(dfn.isType(NodeType.WHILE_EXPR));
        assertTrue(dfn.isType(NodeType.WHILE_LAST_STATEMENT));
    }

    @Test
    public void testForStmtHasCorrectTypes() throws Throwable {
        ASTExpression exp = getOrderedNodes(ASTExpression.class, TEST5).get(0);
        DataFlowNode dfn = exp.getDataFlowNode().getFlow().get(2);
        assertTrue(dfn.isType(NodeType.FOR_INIT));
        dfn = exp.getDataFlowNode().getFlow().get(3);
        assertTrue(dfn.isType(NodeType.FOR_EXPR));
        dfn = exp.getDataFlowNode().getFlow().get(4);
        assertTrue(dfn.isType(NodeType.FOR_UPDATE));
        assertTrue(dfn.isType(NodeType.FOR_BEFORE_FIRST_STATEMENT));
        assertTrue(dfn.isType(NodeType.FOR_END));
    }

    @Test(expected = RuntimeException.class)
    public void testOnlyWorksForMethodsAndConstructors() {
        StatementAndBraceFinder sbf = new StatementAndBraceFinder(LanguageRegistry.getLanguage(JavaLanguageModule.NAME).getDefaultVersion().getLanguageVersionHandler().getDataFlowHandler());
        sbf.buildDataFlowFor(new ASTMethodDeclaration(1));
        sbf.buildDataFlowFor(new ASTConstructorDeclaration(1));
        sbf.buildDataFlowFor(new ASTCompilationUnit(1));
    }

    private static final String TEST1 =
            "class Foo {" + PMD.EOL +
            " void bar() {" + PMD.EOL +
            "  x = 2;" + PMD.EOL +
            " }" + PMD.EOL +
            "}";

    private static final String TEST2 =
            "class Foo {" + PMD.EOL +
            " void bar() {" + PMD.EOL +
            "  int x;" + PMD.EOL +
            " }" + PMD.EOL +
            "}";

    private static final String TEST3 =
            "class Foo {" + PMD.EOL +
            " void bar() {" + PMD.EOL +
            "  if (x) {}" + PMD.EOL +
            " }" + PMD.EOL +
            "}";

    private static final String TEST4 =
            "class Foo {" + PMD.EOL +
            " void bar() {" + PMD.EOL +
            "  while (x) {}" + PMD.EOL +
            " }" + PMD.EOL +
            "}";

    private static final String TEST5 =
            "class Foo {" + PMD.EOL +
            " void bar() {" + PMD.EOL +
            "  for (int i=0; i<10; i++) {}" + PMD.EOL +
            " }" + PMD.EOL +
            "}";

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(StatementAndBraceFinderTest.class);
    }
}
