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
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import static com.github.javaparser.utils.Utils.assertNotNull;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.metamodel.NameExprMetaModel;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import javax.annotation.Generated;

/**
 * Whenever a SimpleName is used in an expression, it is wrapped in NameExpr.
 * <br/>In <code>int x = a + 3;</code> a is a SimpleName inside a NameExpr.
 *
 * @author Julio Vilmar Gesser
 */
public class NameExpr extends Expression implements NodeWithSimpleName<NameExpr> {

    private SimpleName name;

    public NameExpr() {
        this(null, new SimpleName());
    }

    public NameExpr(final String name) {
        this(null, new SimpleName(name));
    }

    @AllFieldsConstructor
    public NameExpr(final SimpleName name) {
        this(name.getRange().orElse(null), name);
    }

    /**This constructor is used by the parser and is considered private.*/
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public NameExpr(Range range, SimpleName name) {
        super(range);
        setName(name);
        customInitialization();
    }

    @Override
    public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
        return v.visit(this, arg);
    }

    @Override
    public <A> void accept(final VoidVisitor<A> v, final A arg) {
        v.visit(this, arg);
    }

    @Override
    public final SimpleName getName() {
        return name;
    }

    @Override
    public NameExpr setName(final SimpleName name) {
        assertNotNull(name);
        if (name == this.name) {
            return (NameExpr) this;
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
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public NameExpr clone() {
        return (NameExpr) accept(new CloneVisitor(), null);
    }

    @Override
    public NameExprMetaModel getMetaModel() {
        return JavaParserMetaModel.nameExprMetaModel;
    }
}
