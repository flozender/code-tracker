/* Generated By:JJTree: Do not edit this line. ASTPostfixExpression.java */

package net.sourceforge.pmd.ast;

public class ASTPostfixExpression extends SimpleNode {

    public ASTPostfixExpression(int id) {
        super(id);
        setDiscardable();
    }

    public ASTPostfixExpression(JavaParser p, int id) {
        super(p, id);
        setDiscardable();
    }


    /**
     * Accept the visitor. *
     */
    public Object jjtAccept(JavaParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
