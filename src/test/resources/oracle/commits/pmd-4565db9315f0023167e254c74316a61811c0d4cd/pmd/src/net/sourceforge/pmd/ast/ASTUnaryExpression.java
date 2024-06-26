/* Generated By:JJTree: Do not edit this line. ASTUnaryExpression.java */

package net.sourceforge.pmd.ast;

public class ASTUnaryExpression extends SimpleNode {
    public ASTUnaryExpression(int id) {
        super(id);
        setDiscardable();
    }

    public ASTUnaryExpression(JavaParser p, int id) {
        super(p, id);
        setDiscardable();
    }

    /**
     * Accept the visitor. *
     */
    public Object jjtAccept(JavaParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    public void dump(String prefix) {
        System.out.println(toString(prefix) + ":" + (getImage()));
        dumpChildren(prefix);
    }
}
