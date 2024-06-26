package japa.bdd.visitors;

import japa.parser.ast.*;
import japa.parser.ast.body.*;
import japa.parser.ast.comments.BlockComment;
import japa.parser.ast.comments.JavadocComment;
import japa.parser.ast.comments.LineComment;
import japa.parser.ast.expr.*;
import japa.parser.ast.stmt.*;
import japa.parser.ast.type.*;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.Is.is;


public class PositionTestVisitor extends VoidVisitorAdapter<Object> {

    private int numberOfNodesVisited;

    @Override public void visit(final AnnotationDeclaration n, final Object arg) {
        doTest(n);
        doTest(n.getNameExpr());
        super.visit(n, arg);
    }

    @Override public void visit(final AnnotationMemberDeclaration n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final ArrayAccessExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final ArrayCreationExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final ArrayInitializerExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final AssertStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final AssignExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final BinaryExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final BlockComment n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final BlockStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final BooleanLiteralExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final BreakStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final CastExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final CatchClause n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final CharLiteralExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final ClassExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final ClassOrInterfaceDeclaration n, final Object arg) {
        doTest(n);
        doTest(n.getNameExpr());
        super.visit(n, arg);
    }

    @Override public void visit(final ClassOrInterfaceType n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final CompilationUnit n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final ConditionalExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final ConstructorDeclaration n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final ContinueStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final DoStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final DoubleLiteralExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final EmptyMemberDeclaration n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final EmptyStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final EmptyTypeDeclaration n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final EnclosedExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final EnumConstantDeclaration n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final EnumDeclaration n, final Object arg) {
        doTest(n);
        doTest(n.getNameExpr());
        super.visit(n, arg);
    }

    @Override public void visit(final ExplicitConstructorInvocationStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final ExpressionStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final FieldAccessExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final FieldDeclaration n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final ForeachStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final ForStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final IfStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final ImportDeclaration n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final InitializerDeclaration n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final InstanceOfExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final IntegerLiteralExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final IntegerLiteralMinValueExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final JavadocComment n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final LabeledStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final LineComment n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final LongLiteralExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final LongLiteralMinValueExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final MarkerAnnotationExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final MemberValuePair n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final MethodCallExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final MethodDeclaration n, final Object arg) {
        doTest(n);
        doTest(n.getNameExpr());
        super.visit(n, arg);
    }

    @Override public void visit(final NameExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final NormalAnnotationExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final NullLiteralExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final ObjectCreationExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final PackageDeclaration n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final Parameter n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final PrimitiveType n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final QualifiedNameExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final ReferenceType n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final ReturnStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final SingleMemberAnnotationExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final StringLiteralExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final SuperExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final SwitchEntryStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final SwitchStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final SynchronizedStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final ThisExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final ThrowStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final TryStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final TypeDeclarationStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final TypeParameter n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final UnaryExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final VariableDeclarationExpr n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final VariableDeclarator n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final VariableDeclaratorId n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final VoidType n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final WhileStmt n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    @Override public void visit(final WildcardType n, final Object arg) {
        doTest(n);
        super.visit(n, arg);
    }

    void doTest(final Node node) {
        final String parsed = node.toString();

        assertThat(node.getBeginLine(), is(greaterThanOrEqualTo(0)));
        assertThat(node.getBeginColumn(), is(greaterThanOrEqualTo(0)));
        assertThat(node.getEndLine(), is(greaterThanOrEqualTo(0)));
        assertThat(node.getEndColumn(), is(greaterThanOrEqualTo(0)));

        if (node.getBeginLine() == node.getEndLine()) {
            assertThat(node.getBeginColumn(), is(lessThanOrEqualTo(node.getEndColumn())));
        } else {
            assertThat(node.getBeginLine(), is(lessThanOrEqualTo(node.getEndLine())));
        }
        numberOfNodesVisited++;
    }

    public int getNumberOfNodesVisited() {
        return numberOfNodesVisited;
    }
}
