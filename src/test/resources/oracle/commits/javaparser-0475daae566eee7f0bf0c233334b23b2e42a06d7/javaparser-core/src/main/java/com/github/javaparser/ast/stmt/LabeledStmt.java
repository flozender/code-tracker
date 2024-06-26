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
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * A statement that is labeled, like <code>label123: println("continuing");</code>
 * @author Julio Vilmar Gesser
 */
public final class LabeledStmt extends Statement {

    private SimpleName label;

    private Statement statement;

    public LabeledStmt() {
        this(null, new SimpleName(), new ReturnStmt());
    }

    public LabeledStmt(final String label, final Statement statement) {
        this(null, new SimpleName(label), statement);
    }

    public LabeledStmt(Range range, final SimpleName label, final Statement statement) {
        super(range);
        setLabel(label);
        setStatement(statement);
    }

    @Override
    public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
        return v.visit(this, arg);
    }

    @Override
    public <A> void accept(final VoidVisitor<A> v, final A arg) {
        v.visit(this, arg);
    }

    public Statement getStatement() {
        return statement;
    }

    public LabeledStmt setStatement(final Statement statement) {
        notifyPropertyChange(ObservableProperty.STATEMENT, this.statement, statement);
        this.statement = assertNotNull(statement);
        setAsParentNodeOf(this.statement);
        return this;
    }

    public final SimpleName getLabel() {
        return label;
    }

    public LabeledStmt setLabel(final SimpleName label) {
        notifyPropertyChange(ObservableProperty.LABEL, this.label, label);
        this.label = assertNotNull(label);
        setAsParentNodeOf(label);
        return this;
    }
}
