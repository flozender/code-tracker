package japa.parser.ast.expr;

import japa.parser.ast.type.Type;
import japa.parser.ast.visitor.GenericVisitor;
import japa.parser.ast.visitor.VoidVisitor;

/**
 * This class is just instantiated as scopes for MethodReferenceExpr nodes to encapsulate Types.
 * @author Raquel Pau
 *
 */
public class TypeExpr extends Expression{

    private Type type;

    public TypeExpr(){}

    public TypeExpr(int beginLine, int beginColumn, int endLine, int endColumn, Type type){
        this.type = type;
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        v.visit(this, arg);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }



}
