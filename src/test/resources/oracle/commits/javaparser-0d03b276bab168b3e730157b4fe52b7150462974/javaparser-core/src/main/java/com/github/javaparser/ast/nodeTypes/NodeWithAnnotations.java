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

package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;

import java.lang.annotation.Annotation;

import static com.github.javaparser.ast.expr.Name.parse;

/**
 * An element which can be the target of annotations.
 *
 * @author Federico Tomassetti
 * @since July 2014
 */
public interface NodeWithAnnotations<N extends Node> {
    NodeList<AnnotationExpr> getAnnotations();

    default AnnotationExpr getAnnotation(int i) {
        return getAnnotations().get(i);
    }

    N setAnnotations(NodeList<AnnotationExpr> annotations);

    /**
     * Annotates this
     *
     * @param name the name of the annotation
     * @return the {@link NormalAnnotationExpr} added
     */
    default NormalAnnotationExpr addAnnotation(String name) {
        NormalAnnotationExpr normalAnnotationExpr = new NormalAnnotationExpr(
                parse(name), new NodeList<>());
        getAnnotations().add(normalAnnotationExpr);
        normalAnnotationExpr.setParentNode((Node) this);
        return normalAnnotationExpr;
    }

    /**
     * Annotates this and automatically add the import
     *
     * @param clazz the class of the annotation
     * @return the {@link NormalAnnotationExpr} added
     */
    default NormalAnnotationExpr addAnnotation(Class<? extends Annotation> clazz) {
        ((Node) this).tryAddImportToParentCompilationUnit(clazz);
        return addAnnotation(clazz.getSimpleName());
    }

    /**
     * Annotates this with a marker annotation
     *
     * @param name the name of the annotation
     * @return this
     */
    @SuppressWarnings("unchecked")
    default N addMarkerAnnotation(String name) {
        MarkerAnnotationExpr markerAnnotationExpr = new MarkerAnnotationExpr(
                Name.parse(name));
        getAnnotations().add(markerAnnotationExpr);
        markerAnnotationExpr.setParentNode((Node) this);
        return (N) this;
    }

    /**
     * Annotates this with a marker annotation and automatically add the import
     *
     * @param clazz the class of the annotation
     * @return this
     */
    default N addMarkerAnnotation(Class<? extends Annotation> clazz) {
        ((Node) this).tryAddImportToParentCompilationUnit(clazz);
        return addMarkerAnnotation(clazz.getSimpleName());
    }

    /**
     * Annotates this with a single member annotation
     *
     * @param name the name of the annotation
     * @param value the value, don't forget to add \"\" for a string value
     * @return this
     */
    @SuppressWarnings("unchecked")
    default N addSingleMemberAnnotation(String name, String value) {
        SingleMemberAnnotationExpr singleMemberAnnotationExpr = new SingleMemberAnnotationExpr(
                Name.parse(name), new NameExpr(value));
        getAnnotations().add(singleMemberAnnotationExpr);
        singleMemberAnnotationExpr.setParentNode((Node) this);
        return (N) this;
    }

    /**
     * Annotates this with a single member annotation and automatically add the import
     *
     * @param clazz the class of the annotation
     * @param value the value, don't forget to add \"\" for a string value
     * @return this
     */
    default N addSingleMemberAnnotation(Class<? extends Annotation> clazz,
                                        String value) {
        ((Node) this).tryAddImportToParentCompilationUnit(clazz);
        return addSingleMemberAnnotation(clazz.getSimpleName(), value);
    }

    /**
     * Check whether an annotation with this name is present on this element
     *
     * @param annotationName the name of the annotation
     * @return true if found, false if not
     */
    default boolean isAnnotationPresent(String annotationName) {
        return getAnnotations().stream().anyMatch(a -> a.getName().getIdentifier().equals(annotationName));
    }

    /**
     * Check whether an annotation with this class is present on this element
     *
     * @param annotationClass the class of the annotation
     * @return true if found, false if not
     */
    default boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return isAnnotationPresent(annotationClass.getSimpleName());
    }

    /**
     * Try to find an annotation by its name
     *
     * @param annotationName the name of the annotation
     * @return null if not found, the annotation otherwise
     */
    default AnnotationExpr getAnnotationByName(String annotationName) {
        return getAnnotations().stream().filter(a -> a.getName().getIdentifier().equals(annotationName)).findFirst()
                .orElse(null);
    }

    /**
     * Try to find an annotation by its class
     *
     * @param annotationClass the class of the annotation
     * @return null if not found, the annotation otherwise
     */
    default AnnotationExpr getAnnotationByClass(Class<? extends Annotation> annotationClass) {
        return getAnnotationByName(annotationClass.getSimpleName());
    }
}
