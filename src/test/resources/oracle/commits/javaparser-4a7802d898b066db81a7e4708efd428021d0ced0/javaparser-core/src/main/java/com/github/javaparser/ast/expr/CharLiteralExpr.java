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

import com.github.javaparser.Range;
import com.github.javaparser.ast.internal.Utils;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

import static com.github.javaparser.Position.pos;
import static com.github.javaparser.Range.*;

/**
 * @author Julio Vilmar Gesser
 */
public final class CharLiteralExpr extends StringLiteralExpr {

    public CharLiteralExpr() {
    }

    public CharLiteralExpr(String value) {
        super(value);
    }

    /**
     * @deprecated prefer using Range objects.
     */
    @Deprecated
    public CharLiteralExpr(int beginLine, int beginColumn, int endLine, int endColumn, String value) {
        this(range(pos(beginLine, beginColumn), pos(endLine, endColumn)), value);
    }
    
    public CharLiteralExpr(Range range, String value) {
        super(range, value);
    }

    /**
     * Utility method that creates a new StringLiteralExpr. Escapes EOL characters.
     */
    public static CharLiteralExpr escape(String string) {
        return new CharLiteralExpr(Utils.escapeEndOfLines(string));
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        v.visit(this, arg);
    }
}
