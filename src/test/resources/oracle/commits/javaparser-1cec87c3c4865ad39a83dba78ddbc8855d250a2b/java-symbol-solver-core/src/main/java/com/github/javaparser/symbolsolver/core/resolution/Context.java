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

package com.github.javaparser.symbolsolver.core.resolution;

import com.github.javaparser.symbolsolver.model.declarations.MethodDeclaration;
import com.github.javaparser.symbolsolver.model.declarations.TypeDeclaration;
import com.github.javaparser.symbolsolver.model.declarations.ValueDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.model.resolution.Value;
import com.github.javaparser.symbolsolver.model.usages.MethodUsage;
import com.github.javaparser.symbolsolver.model.usages.typesystem.Type;

import java.util.List;
import java.util.Optional;

/**
 * Context is very similar to scope.
 * In the context we look for solving symbols.
 *
 * @author Federico Tomassetti
 */
public interface Context {

    Context getParent();

    /* Type resolution */

    default Optional<Type> solveGenericType(String name, TypeSolver typeSolver) {
        return Optional.empty();
    }

    default SymbolReference<TypeDeclaration> solveType(String name, TypeSolver typeSolver) {
        Context parent = getParent();
        if (parent == null) {
            return SymbolReference.unsolved(TypeDeclaration.class);
        } else {
            return getParent().solveType(name, typeSolver);
        }
    }

    /* Symbol resolution */

    SymbolReference<? extends ValueDeclaration> solveSymbol(String name, TypeSolver typeSolver);

    default Optional<Value> solveSymbolAsValue(String name, TypeSolver typeSolver) {
        SymbolReference<? extends ValueDeclaration> ref = solveSymbol(name, typeSolver);
        if (ref.isSolved()) {
            Value value = Value.from(ref.getCorrespondingDeclaration(), typeSolver);
            return Optional.of(value);
        } else {
            return Optional.empty();
        }
    }

    /* Methods resolution */

    /**
     * We find the method declaration which is the best match for the given name and list of typeParametersValues.
     */
    SymbolReference<MethodDeclaration> solveMethod(String name, List<Type> argumentsTypes, TypeSolver typeSolver);

    /**
     * Similar to solveMethod but we return a MethodUsage. A MethodUsage corresponds to a MethodDeclaration plus the
     * resolved type variables.
     */
    default Optional<MethodUsage> solveMethodAsUsage(String name, List<Type> argumentsTypes, TypeSolver typeSolver) {
        SymbolReference<MethodDeclaration> methodSolved = solveMethod(name, argumentsTypes, typeSolver);
        if (methodSolved.isSolved()) {
            MethodDeclaration methodDeclaration = methodSolved.getCorrespondingDeclaration();
            MethodUsage methodUsage = ContextHelper.resolveTypeVariables(this, methodDeclaration, argumentsTypes);//methodDeclaration.resolveTypeVariables(this, argumentsTypes);
            return Optional.of(methodUsage);
        } else {
            return Optional.empty();
        }
    }
}
