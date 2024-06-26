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

package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.PrimitiveType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NodeWithVariablesTest {

    @Test
    public void getCommonTypeWorksForNormalVariables() {
        VariableDeclarationExpr declaration = JavaParser.parseVariableDeclarationExpr("int a,b;");
        assertEquals(PrimitiveType.INT_TYPE, declaration.getCommonType());
    }

    @Test
    public void getCommonTypeWorksForArrayTypes() {
        JavaParser.parseVariableDeclarationExpr("int a[],b[];").getCommonType();
    }

    @Test(expected = AssertionError.class)
    public void getCommonTypeFailsOnArrayDifferences() {
        JavaParser.parseVariableDeclarationExpr("int a[],b[][];").getCommonType();
    }

    @Test(expected = AssertionError.class)
    public void getCommonTypeFailsOnDodgySetterUsage() {
        VariableDeclarationExpr declaration = JavaParser.parseVariableDeclarationExpr("int a,b;");
        declaration.getVariable(1).setType(String.class);
        declaration.getCommonType();
    }

    @Test(expected = AssertionError.class)
    public void getCommonTypeFailsOnInvalidEmptyVariableList() {
        VariableDeclarationExpr declaration = JavaParser.parseVariableDeclarationExpr("int a;");
        declaration.getVariables().clear();
        declaration.getCommonType();
    }

    @Test
    public void getElementTypeWorksForNormalVariables() {
        VariableDeclarationExpr declaration = JavaParser.parseVariableDeclarationExpr("int a,b;");
        assertEquals(PrimitiveType.INT_TYPE, declaration.getElementType());
    }

    @Test
    public void getElementTypeWorksForArrayTypes() {
        VariableDeclarationExpr declaration = JavaParser.parseVariableDeclarationExpr("int a[],b[];");
        assertEquals(PrimitiveType.INT_TYPE, declaration.getElementType());
    }

    @Test
    public void getElementTypeIsOkayWithArrayDifferences() {
        JavaParser.parseVariableDeclarationExpr("int a[],b[][];").getElementType();
    }

    @Test(expected = AssertionError.class)
    public void getElementTypeFailsOnDodgySetterUsage() {
        VariableDeclarationExpr declaration = JavaParser.parseVariableDeclarationExpr("int a,b;");
        declaration.getVariable(1).setType(String.class);
        declaration.getElementType();
    }

    @Test(expected = AssertionError.class)
    public void getElementTypeFailsOnInvalidEmptyVariableList() {
        VariableDeclarationExpr declaration = JavaParser.parseVariableDeclarationExpr("int a;");
        declaration.getVariables().clear();
        declaration.getElementType();
    }

}