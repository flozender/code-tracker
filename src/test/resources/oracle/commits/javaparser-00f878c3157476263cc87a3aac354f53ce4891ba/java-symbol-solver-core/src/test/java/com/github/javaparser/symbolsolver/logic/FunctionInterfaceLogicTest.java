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

package com.github.javaparser.symbolsolver.logic;

import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.model.usages.typesystem.ReferenceTypeImpl;
import com.github.javaparser.symbolsolver.model.usages.typesystem.Type;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionInterfaceDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JreTypeSolver;
import org.junit.Test;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.*;

public class FunctionInterfaceLogicTest {

    @Test
    public void testGetFunctionalMethodNegativeCaseOnClass() {
        TypeSolver typeSolver = new JreTypeSolver();
        Type string = new ReferenceTypeImpl(new ReflectionClassDeclaration(String.class, typeSolver), typeSolver);
        assertEquals(false, FunctionalInterfaceLogic.getFunctionalMethod(string).isPresent());
    }

    @Test
    public void testGetFunctionalMethodPositiveCasesOnInterfaces() {
        TypeSolver typeSolver = new JreTypeSolver();
        Type function = new ReferenceTypeImpl(new ReflectionInterfaceDeclaration(Function.class, typeSolver), typeSolver);
        assertEquals(true, FunctionalInterfaceLogic.getFunctionalMethod(function).isPresent());
        assertEquals("apply", FunctionalInterfaceLogic.getFunctionalMethod(function).get().getName());
        Type consumer = new ReferenceTypeImpl(new ReflectionInterfaceDeclaration(Consumer.class, typeSolver), typeSolver);
        assertEquals(true, FunctionalInterfaceLogic.getFunctionalMethod(consumer).isPresent());
        assertEquals("accept", FunctionalInterfaceLogic.getFunctionalMethod(consumer).get().getName());
    }
}
