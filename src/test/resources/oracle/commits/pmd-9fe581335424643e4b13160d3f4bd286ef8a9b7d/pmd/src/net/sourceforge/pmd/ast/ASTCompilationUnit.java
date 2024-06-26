/* Generated By:JJTree: Do not edit this line. ASTCompilationUnit.java */

package net.sourceforge.pmd.ast;

public class ASTCompilationUnit extends SimpleJavaNode implements CompilationUnit, TypeNode {
    public ASTCompilationUnit(int id) {
        super(id);
    }

    public ASTCompilationUnit(JavaParser p, int id) {
        super(p, id);
    }


    /**
     * Accept the visitor. *
     */
    public Object jjtAccept(JavaParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
    private Class type;
    public void setType(Class type){
        this.type = type;
    }
    
    public Class getType(){
        return type;
    }

    public boolean declarationsAreInDefaultPackage() {
        return getPackageDeclaration() == null;
    }

    public ASTPackageDeclaration getPackageDeclaration() {
        return getFirstChildOfType(ASTPackageDeclaration.class);
    }
}
