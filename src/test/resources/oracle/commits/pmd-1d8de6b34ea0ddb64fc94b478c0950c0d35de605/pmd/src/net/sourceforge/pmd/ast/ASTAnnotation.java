/* Generated By:JJTree: Do not edit this line. ASTAnnotation.java */

package net.sourceforge.pmd.ast;

public class ASTAnnotation extends SimpleNode {
  public ASTAnnotation(int id) {
    super(id);
  }

  public ASTAnnotation(JavaParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(JavaParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
