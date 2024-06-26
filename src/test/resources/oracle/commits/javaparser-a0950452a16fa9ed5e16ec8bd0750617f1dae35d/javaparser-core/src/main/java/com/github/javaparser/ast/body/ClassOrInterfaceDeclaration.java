/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2016 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */
package com.github.javaparser.ast.body;

import com.github.javaparser.Range;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeParameters;
import com.github.javaparser.ast.nodeTypes.modifiers.*;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import java.util.*;
import static com.github.javaparser.utils.Utils.assertNotNull;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.metamodel.ClassOrInterfaceDeclarationMetaModel;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import javax.annotation.Generated;

/**
 * A definition of a class or interface.<br/><code>class X { ... }</code>
 *
 * @author Julio Vilmar Gesser
 */
public final class ClassOrInterfaceDeclaration extends TypeDeclaration<ClassOrInterfaceDeclaration> implements NodeWithImplements<ClassOrInterfaceDeclaration>, NodeWithExtends<ClassOrInterfaceDeclaration>, NodeWithTypeParameters<ClassOrInterfaceDeclaration>, NodeWithAbstractModifier<ClassOrInterfaceDeclaration>, NodeWithFinalModifier<ClassOrInterfaceDeclaration> {

    private boolean isInterface;

    private NodeList<TypeParameter> typeParameters;

    // Can contain more than one item if this is an interface
    private NodeList<ClassOrInterfaceType> extendedTypes;

    private NodeList<ClassOrInterfaceType> implementedTypes;

    public ClassOrInterfaceDeclaration() {
        this(null, EnumSet.noneOf(Modifier.class), new NodeList<>(), false, new SimpleName(), new NodeList<>(), new NodeList<>(), new NodeList<>(), new NodeList<>());
    }

    public ClassOrInterfaceDeclaration(final EnumSet<Modifier> modifiers, final boolean isInterface, final String name) {
        this(null, modifiers, new NodeList<>(), isInterface, new SimpleName(name), new NodeList<>(), new NodeList<>(), new NodeList<>(), new NodeList<>());
    }

    @AllFieldsConstructor
    public ClassOrInterfaceDeclaration(final EnumSet<Modifier> modifiers, final NodeList<AnnotationExpr> annotations, final boolean isInterface, final SimpleName name, final NodeList<TypeParameter> typeParameters, final NodeList<ClassOrInterfaceType> extendedTypes, final NodeList<ClassOrInterfaceType> implementedTypes, final NodeList<BodyDeclaration<?>> members) {
        this(null, modifiers, annotations, isInterface, name, typeParameters, extendedTypes, implementedTypes, members);
    }

    /**This constructor is used by the parser and is considered private.*/
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public ClassOrInterfaceDeclaration(Range range, EnumSet<Modifier> modifiers, NodeList<AnnotationExpr> annotations, boolean isInterface, SimpleName name, NodeList<TypeParameter> typeParameters, NodeList<ClassOrInterfaceType> extendedTypes, NodeList<ClassOrInterfaceType> implementedTypes, NodeList<BodyDeclaration<?>> members) {
        super(range, modifiers, annotations, name, members);
        setInterface(isInterface);
        setTypeParameters(typeParameters);
        setExtendedTypes(extendedTypes);
        setImplementedTypes(implementedTypes);
        customInitialization();
    }

    @Override
    public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
        return v.visit(this, arg);
    }

    @Override
    public <A> void accept(final VoidVisitor<A> v, final A arg) {
        v.visit(this, arg);
    }

    @Override
    public NodeList<ClassOrInterfaceType> getExtendedTypes() {
        return extendedTypes;
    }

    @Override
    public NodeList<ClassOrInterfaceType> getImplementedTypes() {
        return implementedTypes;
    }

    public NodeList<TypeParameter> getTypeParameters() {
        return typeParameters;
    }

    public boolean isInterface() {
        return isInterface;
    }

    @Override
    public ClassOrInterfaceDeclaration setExtendedTypes(final NodeList<ClassOrInterfaceType> extendedTypes) {
        assertNotNull(extendedTypes);
        if (extendedTypes == this.extendedTypes) {
            return (ClassOrInterfaceDeclaration) this;
        }
        notifyPropertyChange(ObservableProperty.EXTENDED_TYPES, this.extendedTypes, extendedTypes);
        if (this.extendedTypes != null)
            this.extendedTypes.setParentNode(null);
        this.extendedTypes = extendedTypes;
        setAsParentNodeOf(extendedTypes);
        return this;
    }

    @Override
    public ClassOrInterfaceDeclaration setImplementedTypes(final NodeList<ClassOrInterfaceType> implementedTypes) {
        assertNotNull(implementedTypes);
        if (implementedTypes == this.implementedTypes) {
            return (ClassOrInterfaceDeclaration) this;
        }
        notifyPropertyChange(ObservableProperty.IMPLEMENTED_TYPES, this.implementedTypes, implementedTypes);
        if (this.implementedTypes != null)
            this.implementedTypes.setParentNode(null);
        this.implementedTypes = implementedTypes;
        setAsParentNodeOf(implementedTypes);
        return this;
    }

    public ClassOrInterfaceDeclaration setInterface(final boolean isInterface) {
        if (isInterface == this.isInterface) {
            return (ClassOrInterfaceDeclaration) this;
        }
        notifyPropertyChange(ObservableProperty.INTERFACE, this.isInterface, isInterface);
        this.isInterface = isInterface;
        return this;
    }

    @Override
    public ClassOrInterfaceDeclaration setTypeParameters(final NodeList<TypeParameter> typeParameters) {
        assertNotNull(typeParameters);
        if (typeParameters == this.typeParameters) {
            return (ClassOrInterfaceDeclaration) this;
        }
        notifyPropertyChange(ObservableProperty.TYPE_PARAMETERS, this.typeParameters, typeParameters);
        if (this.typeParameters != null)
            this.typeParameters.setParentNode(null);
        this.typeParameters = typeParameters;
        setAsParentNodeOf(typeParameters);
        return this;
    }

    /**
     * Try to find a {@link ConstructorDeclaration} with no parameters by its name
     *
     * @return the methods found (multiple in case of polymorphism)
     */
    public Optional<ConstructorDeclaration> getDefaultConstructor() {
        return getMembers().stream().filter(bd -> bd instanceof ConstructorDeclaration).map(bd -> (ConstructorDeclaration) bd).filter(cd -> cd.getParameters().isEmpty()).findFirst();
    }

    /**
     * Adds a constructor to this
     *
     * @param modifiers the modifiers like {@link Modifier#PUBLIC}
     * @return the {@link MethodDeclaration} created
     */
    public ConstructorDeclaration addConstructor(Modifier... modifiers) {
        ConstructorDeclaration constructorDeclaration = new ConstructorDeclaration();
        constructorDeclaration.setModifiers(Arrays.stream(modifiers).collect(toCollection(() -> EnumSet.noneOf(Modifier.class))));
        constructorDeclaration.setName(getName());
        getMembers().add(constructorDeclaration);
        return constructorDeclaration;
    }

    /**
     * Find all constructors for this class.
     *
     * @return the constructors found. This list is immutable.
     */
    public List<ConstructorDeclaration> getConstructors() {
        return unmodifiableList(getMembers().stream().filter(m -> m instanceof ConstructorDeclaration).map(m -> (ConstructorDeclaration) m).collect(toList()));
    }

    /**
     * Try to find a {@link MethodDeclaration} by its parameters types
     *
     * @param paramTypes the types of parameters like "Map&lt;Integer,String&gt;","int" to match<br> void
     * foo(Map&lt;Integer,String&gt; myMap,int number)
     * @return the methods found (multiple in case of overloading)
     */
    public Optional<ConstructorDeclaration> getConstructorByParameterTypes(String... paramTypes) {
        return getConstructors().stream().filter(m -> m.hasParametersOfType(paramTypes)).findFirst();
    }

    /**
     * Try to find a {@link MethodDeclaration} by its parameters types
     *
     * @param paramTypes the types of parameters like "Map&lt;Integer,String&gt;","int" to match<br> void
     * foo(Map&lt;Integer,String&gt; myMap,int number)
     * @return the methods found (multiple in case of overloading)
     */
    public Optional<ConstructorDeclaration> getConstructorByParameterTypes(Class<?>... paramTypes) {
        return getConstructors().stream().filter(m -> m.hasParametersOfType(paramTypes)).findFirst();
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetNodeListsGenerator")
    public List<NodeList<?>> getNodeLists() {
        return Arrays.asList(getExtendedTypes(), getImplementedTypes(), getTypeParameters(), getMembers(), getAnnotations());
    }

    @Override
    public boolean remove(Node node) {
        if (node == null)
            return false;
        for (int i = 0; i < extendedTypes.size(); i++) {
            if (extendedTypes.get(i) == node) {
                extendedTypes.remove(i);
                return true;
            }
        }
        for (int i = 0; i < implementedTypes.size(); i++) {
            if (implementedTypes.get(i) == node) {
                implementedTypes.remove(i);
                return true;
            }
        }
        for (int i = 0; i < typeParameters.size(); i++) {
            if (typeParameters.get(i) == node) {
                typeParameters.remove(i);
                return true;
            }
        }
        return super.remove(node);
    }

    /**
     * @return is this class's parent a LocalClassDeclarationStmt ?
     */
    public boolean isLocalClassDeclaration() {
        return getParentNode().map(p -> p instanceof LocalClassDeclarationStmt).orElse(false);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public ClassOrInterfaceDeclaration clone() {
        return (ClassOrInterfaceDeclaration) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public ClassOrInterfaceDeclarationMetaModel getMetaModel() {
        return JavaParserMetaModel.classOrInterfaceDeclarationMetaModel;
    }
}
