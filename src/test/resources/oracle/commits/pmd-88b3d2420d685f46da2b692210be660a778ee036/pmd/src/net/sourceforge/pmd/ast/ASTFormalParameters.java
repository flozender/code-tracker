/* Generated By:JJTree: Do not edit this line. ASTFormalParameters.java */

package net.sourceforge.pmd.ast;

public class ASTFormalParameters extends SimpleNode {
  public ASTFormalParameters(int id) {
    super(id);
  }

  public ASTFormalParameters(JavaParser p, int id) {
    super(p, id);
  }

    public int getParameterCount() {
        return jjtGetNumChildren();
    }

  /** Accept the visitor. **/
  public Object jjtAccept(JavaParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
