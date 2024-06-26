package com.github.javaparser.ast.type;

import com.github.javaparser.Range;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * Represents a set of types. A given value of this type has to be assignable to at all of the element types.
 * As of Java 8 it is used in casts or while expressing bounds for generic types.
 * <p>
 * For example:
 * <code>public class A&lt;T extends Serializable &amp; Cloneable&gt; { }</code>
 * <p>
 * Or:
 * <code>void foo((Serializable &amp; Cloneable)myObject);</code>
 *
 * @since 3.0.0
 */
public class IntersectionType extends Type implements NodeWithAnnotations<IntersectionType> {

    private NodeList<ReferenceType> elements;

    public IntersectionType(NodeList<ReferenceType> elements) {
        this(null, elements);
    }

    public IntersectionType(Range range, NodeList<ReferenceType> elements) {
        super(range, new NodeList<>());
        setElements(elements);
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        v.visit(this, arg);
    }

    public NodeList<ReferenceType> getElements() {
        return elements;
    }

    public IntersectionType setElements(NodeList<ReferenceType> elements) {
        notifyPropertyChange(ObservableProperty.ELEMENTS, this.elements, elements);
        this.elements = assertNotNull(elements);
        setAsParentNodeOf(this.elements);
        return this;
    }

    @Override
    public IntersectionType setAnnotations(NodeList<AnnotationExpr> annotations) {
        return (IntersectionType) super.setAnnotations(annotations);
    }
}
