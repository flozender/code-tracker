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

package me.tomassetti.symbolsolver.javaparsermodel.contexts;

import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import me.tomassetti.symbolsolver.javaparsermodel.JavaParserFacade;
import me.tomassetti.symbolsolver.model.declarations.MethodDeclaration;
import me.tomassetti.symbolsolver.model.declarations.ValueDeclaration;
import me.tomassetti.symbolsolver.model.resolution.SymbolReference;
import me.tomassetti.symbolsolver.model.resolution.TypeSolver;
import me.tomassetti.symbolsolver.model.usages.typesystem.ReferenceTypeImpl;
import me.tomassetti.symbolsolver.model.usages.typesystem.Type;

import java.util.List;

import static me.tomassetti.symbolsolver.javaparser.Navigator.getParentNode;

public class SwitchEntryContext extends AbstractJavaParserContext<SwitchEntryStmt> {

    public SwitchEntryContext(SwitchEntryStmt wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    @Override
    public SymbolReference<? extends ValueDeclaration> solveSymbol(String name, TypeSolver typeSolver) {
        SwitchStmt switchStmt = (SwitchStmt) getParentNode(wrappedNode);
        Type type = JavaParserFacade.get(typeSolver).getType(switchStmt.getSelector());
        if (type.isReferenceType() && type.asReferenceType().getTypeDeclaration().isEnum()) {
            if (type instanceof ReferenceTypeImpl) {
                ReferenceTypeImpl typeUsageOfTypeDeclaration = (ReferenceTypeImpl) type;
                if (typeUsageOfTypeDeclaration.getTypeDeclaration().hasField(name)) {
                    return SymbolReference.solved(typeUsageOfTypeDeclaration.getTypeDeclaration().getField(name));
                }
            } else {
                throw new UnsupportedOperationException();
            }
        }
        return getParent().solveSymbol(name, typeSolver);
    }

    @Override
    public SymbolReference<MethodDeclaration> solveMethod(String name, List<Type> argumentsTypes, TypeSolver typeSolver) {
        return getParent().solveMethod(name, argumentsTypes, typeSolver);
    }
}
