/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2015 The JavaParser Team.
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

import static com.github.javaparser.Position.pos;

import com.github.javaparser.Range;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

/**
 * This class is just instantiated as scopes for MethodReferenceExpr nodes to encapsulate Types.
 * @author Raquel Pau
 *
 */
public class TypeExpr extends Expression implements NodeWithType<TypeExpr> {

    private Type type;

    public TypeExpr(){}

    /**
     * @deprecated prefer using Range objects.
     */
    @Deprecated
    public TypeExpr(int beginLine, int beginColumn, int endLine, int endColumn, Type type) {
        this(new Range(pos(beginLine, beginColumn), pos(endLine, endColumn)), type);
    }
    
    public TypeExpr(Range range, Type type) {
        super(range);
        setType(type);
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        v.visit(this, arg);
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public TypeExpr setType(Type type) {
        this.type = type;
        setAsParentNodeOf(this.type);
        return this;
    }



}
