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
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import static com.github.javaparser.utils.Utils.assertNotNull;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.metamodel.MemberValuePairMetaModel;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import javax.annotation.Generated;

/**
 * A value for a member of an annotation.
 * In <code>@Counters(a=15)</code> a=15 is a MemberValuePair. Its name is a, and its value is 15.
 *
 * @author Julio Vilmar Gesser
 */
public final class MemberValuePair extends Node implements NodeWithSimpleName<MemberValuePair> {

    private SimpleName name;

    private Expression value;

    public MemberValuePair() {
        this(null, new SimpleName(), new StringLiteralExpr());
    }

    public MemberValuePair(final String name, final Expression value) {
        this(null, new SimpleName(name), value);
    }

    @AllFieldsConstructor
    public MemberValuePair(final SimpleName name, final Expression value) {
        this(null, name, value);
    }

    /**This constructor is used by the parser and is considered private.*/
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public MemberValuePair(Range range, SimpleName name, Expression value) {
        super(range);
        setName(name);
        setValue(value);
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
    public SimpleName getName() {
        return name;
    }

    public Expression getValue() {
        return value;
    }

    @Override
    public MemberValuePair setName(final SimpleName name) {
        assertNotNull(name);
        if (name == this.name) {
            return (MemberValuePair) this;
        }
        notifyPropertyChange(ObservableProperty.NAME, this.name, name);
        if (this.name != null)
            this.name.setParentNode(null);
        this.name = name;
        setAsParentNodeOf(name);
        return this;
    }

    public MemberValuePair setValue(final Expression value) {
        assertNotNull(value);
        if (value == this.value) {
            return (MemberValuePair) this;
        }
        notifyPropertyChange(ObservableProperty.VALUE, this.value, value);
        if (this.value != null)
            this.value.setParentNode(null);
        this.value = value;
        setAsParentNodeOf(value);
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
    public MemberValuePair clone() {
        return (MemberValuePair) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public MemberValuePairMetaModel getMetaModel() {
        return JavaParserMetaModel.memberValuePairMetaModel;
    }
}
