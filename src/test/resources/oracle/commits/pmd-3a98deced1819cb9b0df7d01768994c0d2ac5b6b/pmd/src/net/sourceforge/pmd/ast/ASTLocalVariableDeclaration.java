/* Generated By:JJTree: Do not edit this line. ASTLocalVariableDeclaration.java */

package net.sourceforge.pmd.ast;

public class ASTLocalVariableDeclaration extends SimpleNode {
    public ASTLocalVariableDeclaration(int id) {
        super(id);
    }

    public ASTLocalVariableDeclaration(JavaParser p, int id) {
        super(p, id);
    }


    private boolean isFinal;

    public void setIsFinal() {
        this.isFinal = true;
    }

    public boolean isFinal() {
        return this.isFinal;
    }

    /** Accept the visitor. **/
    public Object jjtAccept(JavaParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
