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

package me.tomassetti.symbolsolver.resolution.typesolvers;

import me.tomassetti.symbolsolver.model.declarations.TypeDeclaration;
import me.tomassetti.symbolsolver.model.resolution.SymbolReference;
import me.tomassetti.symbolsolver.model.resolution.TypeSolver;
import me.tomassetti.symbolsolver.javaparsermodel.UnsolvedSymbolException;

import java.util.ArrayList;
import java.util.List;

public class CombinedTypeSolver implements TypeSolver {

    private TypeSolver parent;
    private List<TypeSolver> elements = new ArrayList<>();

    public CombinedTypeSolver(TypeSolver... elements) {
        for (TypeSolver el : elements) {
            add(el);
        }
    }

    @Override
    public TypeSolver getParent() {
        return parent;
    }

    @Override
    public void setParent(TypeSolver parent) {
        this.parent = parent;
    }

    public void add(TypeSolver typeSolver) {
        this.elements.add(typeSolver);
        typeSolver.setParent(this);
    }

    @Override
    public SymbolReference<TypeDeclaration> tryToSolveType(String name) {
        for (TypeSolver ts : elements) {
            SymbolReference<TypeDeclaration> res = ts.tryToSolveType(name);
            if (res.isSolved()) {
                return res;
            }
        }
        return SymbolReference.unsolved(TypeDeclaration.class);
    }

    @Override
    public TypeDeclaration solveType(String name) throws UnsolvedSymbolException {
        SymbolReference<TypeDeclaration> res = tryToSolveType(name);
        if (res.isSolved()) {
            return res.getCorrespondingDeclaration();
        } else {
            throw new UnsolvedSymbolException(name);
        }
    }
}
