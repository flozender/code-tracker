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

package me.tomassetti.symbolsolver.reflectionmodel;

import me.tomassetti.symbolsolver.model.declarations.AccessLevel;
import me.tomassetti.symbolsolver.model.declarations.FieldDeclaration;
import me.tomassetti.symbolsolver.model.declarations.TypeDeclaration;
import me.tomassetti.symbolsolver.model.resolution.TypeSolver;
import me.tomassetti.symbolsolver.model.usages.typesystem.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectionFieldDeclaration implements FieldDeclaration {

    private Field field;
    private TypeSolver typeSolver;
    private Type type;

    public ReflectionFieldDeclaration(Field field, TypeSolver typeSolver) {
        this.field = field;
        this.typeSolver = typeSolver;
        this.type = calcType();
    }

    private ReflectionFieldDeclaration(Field field, TypeSolver typeSolver, Type type) {
        this.field = field;
        this.typeSolver = typeSolver;
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }

    private Type calcType() {
        // TODO consider interfaces, enums, primitive types, arrays
        return ReflectionFactory.typeUsageFor(field.getGenericType(), typeSolver);
    }

    @Override
    public String getName() {
        return field.getName();
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(field.getModifiers());
    }

    @Override
    public boolean isField() {
        return true;
    }

    @Override
    public TypeDeclaration declaringType() {
        throw new UnsupportedOperationException();
    }

    public FieldDeclaration replaceType(Type fieldType) {
        return new ReflectionFieldDeclaration(field, typeSolver, fieldType);
    }

    @Override
    public boolean isParameter() {
        return false;
    }

    @Override
    public boolean isType() {
        return false;
    }

    @Override
    public AccessLevel accessLevel() {
        throw new UnsupportedOperationException();
    }
}
