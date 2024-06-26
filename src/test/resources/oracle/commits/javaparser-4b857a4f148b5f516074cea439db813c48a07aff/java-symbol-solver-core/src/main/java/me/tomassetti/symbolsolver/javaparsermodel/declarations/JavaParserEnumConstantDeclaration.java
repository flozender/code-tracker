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

package me.tomassetti.symbolsolver.javaparsermodel.declarations;

import com.github.javaparser.ast.body.EnumDeclaration;
import me.tomassetti.symbolsolver.model.declarations.ValueDeclaration;
import me.tomassetti.symbolsolver.model.resolution.TypeSolver;
import me.tomassetti.symbolsolver.model.usages.typesystem.ReferenceTypeImpl;
import me.tomassetti.symbolsolver.model.usages.typesystem.Type;

public class JavaParserEnumConstantDeclaration implements ValueDeclaration {

    private TypeSolver typeSolver;
    private com.github.javaparser.ast.body.EnumConstantDeclaration wrappedNode;

    public JavaParserEnumConstantDeclaration(com.github.javaparser.ast.body.EnumConstantDeclaration wrappedNode, TypeSolver typeSolver) {
        this.wrappedNode = wrappedNode;
        this.typeSolver = typeSolver;
    }

    @Override
    public Type getType() {
        return new ReferenceTypeImpl(new JavaParserEnumDeclaration((EnumDeclaration) wrappedNode.getParentNode(), typeSolver), typeSolver);
    }

    @Override
    public String getName() {
        return wrappedNode.getName();
    }

	/**
	 * Returns the JavaParser node associated with this JavaParserEnumConstantDeclaration.
	 *
	 * @return A visitable JavaParser node wrapped by this object.
	 */
	public com.github.javaparser.ast.body.EnumConstantDeclaration getWrappedNode()
	{
		return wrappedNode;
	}

}
