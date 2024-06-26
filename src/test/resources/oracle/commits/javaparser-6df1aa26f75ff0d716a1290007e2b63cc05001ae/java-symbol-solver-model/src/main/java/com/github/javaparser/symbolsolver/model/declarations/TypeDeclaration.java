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

package com.github.javaparser.symbolsolver.model.declarations;

import java.util.Optional;
import java.util.Set;

/**
 * A declaration of a type. It could be a primitive type, an enum, a class, an interface or a type variable.
 * It cannot be an annotation or an array.
 *
 * @author Federico Tomassetti
 */
public interface TypeDeclaration extends Declaration {

    ///
    /// Containment
    ///

    /**
     * Get the list of types defined inside the current type.
     */
    default Set<ReferenceTypeDeclaration> internalTypes() {
        throw new UnsupportedOperationException("InternalTypes not available for " + this);
    }

    /**
     * Get the TypeDeclaration enclosing this declaration.
     *
     * @return
     */
    default Optional<ReferenceTypeDeclaration> containerType() {
        throw new UnsupportedOperationException("containerType is not supported for " + this.getClass().getCanonicalName());
    }

    ///
    /// Misc
    ///

    /**
     * Is this the declaration of a class?
     * Note that an Enum is not considered a Class in this case.
     */
    default boolean isClass() {
        return false;
    }

    /**
     * Is this the declaration of an interface?
     */
    default boolean isInterface() {
        return false;
    }

    /**
     * Is this the declaration of an enum?
     */
    default boolean isEnum() {
        return false;
    }

    /**
     * Is this the declaration of a type parameter?
     */
    default boolean isTypeParameter() {
        return false;
    }

    @Override
    default boolean isType() {
        return true;
    }

    @Override
    default TypeDeclaration asType() {
        return this;
    }

    /**
     * Return this as a ClassDeclaration or throw UnsupportedOperationException.
     */
    default ClassDeclaration asClass() {
        throw new UnsupportedOperationException(String.format("%s is not a class", this));
    }

    /**
     * Return this as a InterfaceDeclaration or throw UnsupportedOperationException.
     */
    default InterfaceDeclaration asInterface() {
        throw new UnsupportedOperationException(String.format("%s is not an interface", this));
    }

    /**
     * Return this as a EnumDeclaration or throw UnsupportedOperationException.
     */
    default EnumDeclaration asEnum() {
        throw new UnsupportedOperationException(String.format("%s is not an enum", this));
    }

    /**
     * Return this as a TypeParameterDeclaration or throw UnsupportedOperationException.
     */
    default TypeParameterDeclaration asTypeParameter() {
        throw new UnsupportedOperationException(String.format("%s is not a type parameter", this));
    }

    /**
     * The fully qualified name of the type declared.
     */
    String getQualifiedName();

    /**
     * The ID corresponds most of the type to the qualified name. It differs only for local
     * classes which do not have a qualified name but have an ID.
     */
    default String getId() {
        String qname = getQualifiedName();
        if (qname == null) {
            return String.format("<localClass>:%s", getName());
        }
        return qname;
    }


}
