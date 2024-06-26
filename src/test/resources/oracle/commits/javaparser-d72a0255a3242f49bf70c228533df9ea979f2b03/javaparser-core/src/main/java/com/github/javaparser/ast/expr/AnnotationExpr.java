/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2016 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */
package com.github.javaparser.ast.expr;

import com.github.javaparser.Range;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.observer.ObservableProperty;
import static com.github.javaparser.utils.Utils.assertNotNull;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.metamodel.AnnotationExprMetaModel;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import javax.annotation.Generated;

/**
 * A base class for the different types of annotations.
 *
 * @author Julio Vilmar Gesser
 */
public abstract class AnnotationExpr extends Expression implements NodeWithName<AnnotationExpr> {

    protected Name name;

    public AnnotationExpr() {
        this(null, new Name());
    }

    @AllFieldsConstructor
    public AnnotationExpr(Name name) {
        this(null, name);
    }

    /**This constructor is used by the parser and is considered private.*/
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public AnnotationExpr(Range range, Name name) {
        super(range);
        setName(name);
        customInitialization();
    }

    @Override
    public Name getName() {
        return name;
    }

    @Override
    public AnnotationExpr setName(final Name name) {
        assertNotNull(name);
        if (name == this.name) {
            return (AnnotationExpr) this;
        }
        notifyPropertyChange(ObservableProperty.NAME, this.name, name);
        if (this.name != null)
            this.name.setParentNode(null);
        this.name = name;
        setAsParentNodeOf(name);
        return this;
    }

    @Override
    public boolean remove(Node node) {
        if (node == null)
            return false;
        return super.remove(node);
    }

    @Override
    public AnnotationExpr clone() {
        return (AnnotationExpr) accept(new CloneVisitor(), null);
    }

    @Override
    public AnnotationExprMetaModel getMetaModel() {
        return JavaParserMetaModel.annotationExprMetaModel;
    }
}
