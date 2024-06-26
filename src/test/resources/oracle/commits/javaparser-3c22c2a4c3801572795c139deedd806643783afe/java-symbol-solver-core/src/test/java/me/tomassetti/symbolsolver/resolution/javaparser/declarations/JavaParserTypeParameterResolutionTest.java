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

package me.tomassetti.symbolsolver.resolution.javaparser.declarations;


import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import me.tomassetti.symbolsolver.javaparser.Navigator;
import me.tomassetti.symbolsolver.javaparsermodel.declarations.JavaParserTypeParameter;
import me.tomassetti.symbolsolver.model.declarations.TypeParameterDeclaration;
import me.tomassetti.symbolsolver.model.resolution.TypeSolver;
import me.tomassetti.symbolsolver.resolution.AbstractResolutionTest;
import me.tomassetti.symbolsolver.javaparsermodel.JavaParserFacade;
import me.tomassetti.symbolsolver.resolution.typesolvers.JreTypeSolver;
import org.junit.Test;
import static org.junit.Assert.*;

public class JavaParserTypeParameterResolutionTest extends AbstractResolutionTest {

    @Test
    public void declaredOnMethodPositiveCase() throws ParseException {
        CompilationUnit cu = parseSample("MethodTypeParameter");
        TypeSolver typeSolver = new JreTypeSolver();
        JavaParserFacade javaParserFacade = JavaParserFacade.get(typeSolver);
        ClassOrInterfaceDeclaration classDecl = Navigator.demandClass(cu, "Foo");
        MethodDeclaration methodDecl = Navigator.demandMethod(classDecl, "usage");
        MethodCallExpr callToFoo = (MethodCallExpr) Navigator.findReturnStmt(methodDecl).getExpr();
        me.tomassetti.symbolsolver.model.declarations.MethodDeclaration methodDeclaration = javaParserFacade.solve(callToFoo).getCorrespondingDeclaration();
        for (TypeParameterDeclaration tp : methodDeclaration.getTypeParameters()) {
            assertTrue(tp instanceof JavaParserTypeParameter);
            assertEquals("C", tp.getName());
            assertEquals(true, tp.declaredOnMethod());
            assertEquals(false, tp.declaredOnClass());
        }
    }

    @Test
    public void declaredOnMethodNegativeCase() throws ParseException {
        CompilationUnit cu = parseSample("ClassTypeParameter");
        TypeSolver typeSolver = new JreTypeSolver();
        JavaParserFacade javaParserFacade = JavaParserFacade.get(typeSolver);
        ClassOrInterfaceDeclaration classDecl = Navigator.demandClass(cu, "Foo");
        MethodDeclaration methodDecl = Navigator.demandMethod(classDecl, "usage");
        MethodCallExpr callToFoo = (MethodCallExpr) Navigator.findReturnStmt(methodDecl).getExpr();
        me.tomassetti.symbolsolver.model.declarations.MethodDeclaration methodDeclaration = javaParserFacade.solve(callToFoo).getCorrespondingDeclaration();
        me.tomassetti.symbolsolver.model.declarations.TypeDeclaration typeDeclaration = methodDeclaration.declaringType();
        assertEquals(2, typeDeclaration.getTypeParameters().size());
        assertTrue(typeDeclaration.getTypeParameters().get(0) instanceof JavaParserTypeParameter);
        assertEquals("A", typeDeclaration.getTypeParameters().get(0).getName());
        assertEquals(false, typeDeclaration.getTypeParameters().get(0).declaredOnMethod());
        assertEquals(true, typeDeclaration.getTypeParameters().get(0).declaredOnClass());
        assertTrue(typeDeclaration.getTypeParameters().get(1) instanceof JavaParserTypeParameter);
        assertEquals("B", typeDeclaration.getTypeParameters().get(1).getName());
        assertEquals(false, typeDeclaration.getTypeParameters().get(1).declaredOnMethod());
        assertEquals(true, typeDeclaration.getTypeParameters().get(1).declaredOnClass());

    }

}
