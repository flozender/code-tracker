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

package com.github.javaparser.ast.stmt;

import com.github.javaparser.Range;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

import java.util.Optional;

import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * @author Julio Vilmar Gesser
 */
public final class AssertStmt extends Statement {

    private Expression check;

    private Expression message;

    public AssertStmt() {
        this(null, new BooleanLiteralExpr(), null);
    }

    public AssertStmt(final Expression check) {
        this(null, check, null);
    }

    public AssertStmt(final Expression check, final Expression message) {
        this(null, check, message);
    }

    public AssertStmt(final Range range, final Expression check, final Expression message) {
        super(range);
        setCheck(check);
        setMessage(message);
    }

    @Override
    public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
        return v.visit(this, arg);
    }

    @Override
    public <A> void accept(final VoidVisitor<A> v, final A arg) {
        v.visit(this, arg);
    }

    public Expression getCheck() {
        return check;
    }

    public Optional<Expression> getMessage() {
        return Optional.ofNullable(message);
    }

    public AssertStmt setCheck(final Expression check) {
        notifyPropertyChange(ObservableProperty.CHECK, this.check, check);
        this.check = assertNotNull(check);
        setAsParentNodeOf(this.check);
        return this;
    }

    /**
     * Sets the message
     *
     * @param msg the message, can be null
     * @return this, the AssertStmt
     */
    public AssertStmt setMessage(final Expression msg) {
        notifyPropertyChange(ObservableProperty.MESSAGE, this.message, msg);
        this.message = msg;
        setAsParentNodeOf(this.message);
        return this;
    }
}
