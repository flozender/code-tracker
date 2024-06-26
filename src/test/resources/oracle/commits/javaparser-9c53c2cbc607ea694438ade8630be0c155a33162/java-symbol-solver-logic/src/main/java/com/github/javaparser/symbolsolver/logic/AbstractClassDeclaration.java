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

import com.github.javaparser.symbolsolver.model.declarations.ClassDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.model.usages.typesystem.ReferenceType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractClassDeclaration extends AbstractTypeDeclaration implements ClassDeclaration {

    protected abstract ReferenceType object();

    @Override
    public boolean hasName() {
        return getQualifiedName() != null;
    }

    @Override
    public final List<ReferenceType> getAllSuperClasses() {
        // TODO it could specify type typeParametersValues: they should appear
        List<ReferenceType> superclasses = new ArrayList<>();
        ReferenceType superClass = getSuperClass();
        if (superClass != null) {
            superclasses.add(superClass);
            superclasses.addAll(superClass.getAllAncestors());
            superclasses.add(object());
        }
        boolean foundObject = false;
        for (int i = 0; i < superclasses.size(); i++) {
            if (superclasses.get(i).getQualifiedName().equals(Object.class.getCanonicalName())) {
                if (foundObject) {
                    superclasses.remove(i);
                    i--;
                } else {
                    foundObject = true;
                }
            }
        }
        return superclasses.stream().filter((s) -> s.getTypeDeclaration().isClass()).collect(Collectors.toList());
    }

    @Override
    public final List<ReferenceType> getAllInterfaces() {
        // TODO it could specify type typeParametersValues: they should appear
        List<ReferenceType> interfaces = new ArrayList<>();
        for (ReferenceType interfaceDeclaration : getInterfaces()) {
            interfaces.add(interfaceDeclaration);
            interfaces.addAll(interfaceDeclaration.getAllInterfacesAncestors());
        }
        ReferenceType superClass = this.getSuperClass();
        if (superClass != null) {
            interfaces.addAll(superClass.getAllInterfacesAncestors());
        }
        return interfaces;
    }

    protected abstract TypeSolver typeSolver();

}
