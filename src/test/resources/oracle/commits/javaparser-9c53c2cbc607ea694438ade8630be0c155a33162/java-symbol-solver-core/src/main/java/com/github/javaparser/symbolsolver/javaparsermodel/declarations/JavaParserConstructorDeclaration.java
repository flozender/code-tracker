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

package com.github.javaparser.symbolsolver.javaparsermodel.declarations;

import com.github.javaparser.symbolsolver.model.declarations.*;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;

import java.util.List;

/**
 * @author Federico Tomassetti
 */
public class JavaParserConstructorDeclaration implements ConstructorDeclaration {

    private ClassDeclaration classDeclaration;
    private com.github.javaparser.ast.body.ConstructorDeclaration wrapped;
    private TypeSolver typeSolver;

    public JavaParserConstructorDeclaration(ClassDeclaration classDeclaration, com.github.javaparser.ast.body.ConstructorDeclaration wrapped,
                                            TypeSolver typeSolver) {
        this.classDeclaration = classDeclaration;
        this.wrapped = wrapped;
        this.typeSolver = typeSolver;
    }

    @Override
    public ClassDeclaration declaringType() {
        return classDeclaration;
    }

    @Override
    public int getNoParams() {
        return this.wrapped.getParameters().size();
    }

    @Override
    public ParameterDeclaration getParam(int i) {
        if (i < 0 || i >= getNoParams()) {
            throw new IllegalArgumentException(String.format("No param with index %d. Number of params: %d", i, getNoParams()));
        }
        return new JavaParserParameterDeclaration(wrapped.getParameters().get(i), typeSolver);
    }

    @Override
    public String getName() {
        return this.classDeclaration.getName();
    }

    @Override
    public AccessLevel accessLevel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TypeParameterDeclaration> getTypeParameters() {
        throw new UnsupportedOperationException();
    }
}