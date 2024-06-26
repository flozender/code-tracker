/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

/* Generated By:JJTree: Do not edit this line. ASTTypeMethod.java Version 4.1 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY= */
package net.sourceforge.pmd.lang.plsql.ast;

public class ASTTypeMethod extends net.sourceforge.pmd.lang.plsql.ast.AbstractPLSQLNode
implements ExecutableCode {
  public ASTTypeMethod(int id) {
    super(id);
  }

  public ASTTypeMethod(PLSQLParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(PLSQLParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  /**
   * Gets the name of the method.
   *
   * @return a String representing the name of the method
   */
  public String getMethodName() {
    ASTMethodDeclarator md = getFirstChildOfType(ASTMethodDeclarator.class);
    if (md != null) {
      return md.getImage();
    }
    return null;
  }
}
/* JavaCC - OriginalChecksum=657963d26263c637fa18391bce82f1c1 (do not edit this line) */
