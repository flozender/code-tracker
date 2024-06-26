/*
 * Copyright 2016 Federico Tomassetti
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.javaparser.symbolsolver.javassistmodel;

import javassist.CtClass;
import com.github.javaparser.symbolsolver.model.declarations.ParameterDeclaration;
import com.github.javaparser.symbolsolver.model.usages.typesystem.Type;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;

public class JavassistParameterDeclaration implements ParameterDeclaration {
    private CtClass type;
    private TypeSolver typeSolver;
    private boolean variadic;

    public JavassistParameterDeclaration(CtClass type, TypeSolver typeSolver, boolean variadic) {
        this.type = type;
        this.typeSolver = typeSolver;
        this.variadic = variadic;
    }

    @Override
    public String toString() {
        return "JavassistParameterDeclaration{" +
                "type=" + type +
                ", typeSolver=" + typeSolver +
                ", variadic=" + variadic +
                '}';
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isField() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isParameter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVariadic() {
        return variadic;
    }

    @Override
    public boolean isType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type getType() {
        return JavassistFactory.typeUsageFor(type, typeSolver);
    }
}
