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
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * @author Julio Vilmar Gesser
 */
public final class ConditionalExpr extends Expression {

    private Expression condition;

    private Expression thenExpr;

    private Expression elseExpr;

    public ConditionalExpr() {
        this(null, new BooleanLiteralExpr(), new StringLiteralExpr(), new StringLiteralExpr());
    }

    public ConditionalExpr(Expression condition, Expression thenExpr, Expression elseExpr) {
        this(null, condition, thenExpr, elseExpr);
    }

    public ConditionalExpr(Range range, Expression condition, Expression thenExpr, Expression elseExpr) {
        super(range);
        setCondition(condition);
        setThenExpr(thenExpr);
        setElseExpr(elseExpr);
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        v.visit(this, arg);
    }

    public Expression getCondition() {
        return condition;
    }

    public Expression getElseExpr() {
        return elseExpr;
    }

    public Expression getThenExpr() {
        return thenExpr;
    }

    public ConditionalExpr setCondition(Expression condition) {
        notifyPropertyChange(ObservableProperty.CONDITION, this.condition, condition);
        this.condition = assertNotNull(condition);
        setAsParentNodeOf(this.condition);
        return this;
    }

    public ConditionalExpr setElseExpr(Expression elseExpr) {
        notifyPropertyChange(ObservableProperty.ELSE_EXPR, this.elseExpr, elseExpr);
        this.elseExpr = assertNotNull(elseExpr);
        setAsParentNodeOf(this.elseExpr);
        return this;
    }

    public ConditionalExpr setThenExpr(Expression thenExpr) {
        notifyPropertyChange(ObservableProperty.THEN_EXPR, this.thenExpr, thenExpr);
        this.thenExpr = assertNotNull(thenExpr);
        setAsParentNodeOf(this.thenExpr);
        return this;
    }
}
