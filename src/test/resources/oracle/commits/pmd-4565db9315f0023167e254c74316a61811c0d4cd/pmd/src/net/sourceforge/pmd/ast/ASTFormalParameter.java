/* Generated By:JJTree: Do not edit this line. ASTFormalParameter.java */

package net.sourceforge.pmd.ast;

public class ASTFormalParameter extends AccessNode implements Dimensionable {
    public ASTFormalParameter(int id) {
        super(id);
    }

    public ASTFormalParameter(JavaParser p, int id) {
        super(p, id);
    }

    /**
     * Accept the visitor. *
     */
    public Object jjtAccept(JavaParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    public boolean isArray() {
        return checkType() + checkDecl() > 0;
    }

    public int getArrayDepth() {
        if (!isArray()) {
            return 0;
        }
        return checkType() + checkDecl();
    }

    private int checkType() {
        if (jjtGetNumChildren() == 0 || !(jjtGetChild(0) instanceof ASTType)) {
            return 0;
        }
        return ((ASTType) jjtGetChild(0)).getArrayDepth();
    }

    private int checkDecl() {
        if (jjtGetNumChildren() < 2 || !(jjtGetChild(1) instanceof ASTVariableDeclarator)) {
            return 0;
        }
        return ((ASTVariableDeclaratorId) (jjtGetChild(1).jjtGetChild(0))).getArrayDepth();
    }

    public void dump(String prefix) {
        System.out.println(collectDumpedModifiers(prefix));
        dumpChildren(prefix);
    }

}
