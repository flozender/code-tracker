/* Generated By:JJTree: Do not edit this line. ASTMethodDeclaration.java */

package net.sourceforge.pmd.ast;


public class ASTMethodDeclaration extends SimpleNode {
  public ASTMethodDeclaration(int id) {
    super(id);
  }

  public ASTMethodDeclaration(JavaParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(JavaParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
