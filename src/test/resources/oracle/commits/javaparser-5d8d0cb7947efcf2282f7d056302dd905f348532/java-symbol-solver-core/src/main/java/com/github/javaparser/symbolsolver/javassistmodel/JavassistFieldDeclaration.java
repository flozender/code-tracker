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

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.symbolsolver.model.declarations.FieldDeclaration;
import com.github.javaparser.symbolsolver.model.declarations.TypeDeclaration;
import com.github.javaparser.symbolsolver.model.declarations.TypeParametrizable;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.model.typesystem.Type;
import javassist.CtField;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.SignatureAttribute;

import java.lang.reflect.Modifier;

/**
 * @author Federico Tomassetti
 */
public class JavassistFieldDeclaration implements FieldDeclaration {
    private CtField ctField;
    private TypeSolver typeSolver;

    public JavassistFieldDeclaration(CtField ctField, TypeSolver typeSolver) {
        this.ctField = ctField;
        this.typeSolver = typeSolver;
    }

    @Override
    public Type getType() {
        try {
            if (ctField.getGenericSignature() != null && declaringType() instanceof TypeParametrizable) {
                javassist.bytecode.SignatureAttribute.Type genericSignatureType = SignatureAttribute.toFieldSignature(ctField.getGenericSignature());
                return JavassistUtils.signatureTypeToType(genericSignatureType, typeSolver, (TypeParametrizable) declaringType());
            } else {
                return JavassistFactory.typeUsageFor(ctField.getType(), typeSolver);
            }
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        } catch (BadBytecode e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(ctField.getModifiers());
    }

    @Override
    public String getName() {
        return ctField.getName();
    }

    @Override
    public boolean isField() {
        return true;
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
    public AccessSpecifier accessLevel() {
        return JavassistFactory.modifiersToAccessLevel(ctField.getModifiers());
    }

    @Override
    public TypeDeclaration declaringType() {
        return JavassistFactory.toTypeDeclaration(ctField.getDeclaringClass(), typeSolver);
    }
}
