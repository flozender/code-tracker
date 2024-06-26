/*
 * Copyright 2000-2005 JetBrains s.r.o.
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
package com.intellij.psi;

import com.intellij.openapi.util.Pair;
import com.intellij.pom.java.PomMemberOwner;
import com.intellij.psi.meta.PsiMetaOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a Java class or interface.
 *
 * @see PsiJavaFile#getClasses() 
 */
public interface PsiClass
  extends PsiElement, PsiNamedElement, PsiModifierListOwner, PsiDocCommentOwner, PsiMetaOwner, PsiTypeParameterListOwner, PsiMember {
  /**
   * The empty array of PSI classes which can be reused to avoid unnecessary allocations.
   */
  PsiClass[] EMPTY_ARRAY = new PsiClass[0];

  /**
   * Returns the fully qualified name of the class.
   *
   * @return the qualified name of the class, or null for anonymous and local classes, and for type parameters
   */
  @Nullable
  String getQualifiedName();

  /**
   * Checks if the class is an interface.
   *
   * @return true if the class is an interface, false otherwise.
   */
  boolean isInterface();

  /**
   * Checks if the class is an annotation type.
   *
   * @return true if the class is an annotation type, false otherwise
   */
  boolean isAnnotationType();

  /**
   * Checks if the class is an enumeration.
   *
   * @return true if the class is an enumeration, false otherwise.
   */
  boolean isEnum();

  /**
   * Returns the list of classes that this class or interface extends.
   *
   * @return the extends list, or null for anonymous classes, enums and annotation types
   */
  @Nullable
  PsiReferenceList getExtendsList();

  /**
   * Returns the list of interfaces that this class implements.
   *
   * @return the implements list, or null for anonymous classes
   */
  @Nullable
  PsiReferenceList getImplementsList();

  /**
   * Returns the list of class types for the classes that this class or interface extends.
   *
   * @return the list of extended class types, or an empty list for anonymous classes,
   *         enums and annotation types
   */
  @NotNull
  PsiClassType[] getExtendsListTypes();

  /**
   * Returns the list of class types for the interfaces that this class implements.
   *
   * @return the list of extended class types, or an empty list for anonymous classes,
   *         enums and annotation types
   */
  @NotNull
  PsiClassType[] getImplementsListTypes();

  /**
   * Returns the base class of this class.
   *
   * @return the base class. May return null when jdk is not configured, so no java.lang.Object is found,
   *         or for java.lang.Object itself
   */
  @Nullable
  PsiClass getSuperClass();

  /**
   * Returns the list of interfaces implemented by the class, or extended by the interface.
   *
   * @return the list of interfaces.
   */
  PsiClass[] getInterfaces();

  /**
   * Returns the list of classes and interfaces extended or implemented by the class.
   *
   * @return the list of classes or interfaces. May return zero elements when jdk is
   *         not configured, so no java.lang.Object is found
   */
  PsiClass[] getSupers();

  /**
   * Returns the list of class types for the classes and interfaces extended or
   * implemented by the class.
   *
   * @return the list of class types for the classes or interfaces. The returned
   *         list always contains at least one element, for the type of the java.lang.Object class.
   */
  PsiClassType[] getSuperTypes();

  /**
   * Returns the list of fields in the class.
   *
   * @return the list of fields.
   */
  @NotNull
  PsiField[] getFields();

  /**
   * Returns the list of methods in the class.
   *
   * @return the list of methods.
   */
  @NotNull
  PsiMethod[] getMethods();

  /**
   * Returns the list of constructors for the class.
   *
   * @return the list of constructors,
   */
  @NotNull
  PsiMethod[] getConstructors();

  /**
   * Returns the list of inner classes for the class.
   *
   * @return the list of inner classes.
   */
  PsiClass[] getInnerClasses();

  /**
   * Returns the list of class initializers for the class.
   *
   * @return the list of class initializers.
   */
  PsiClassInitializer[] getInitializers();

  /**
   * Returns the list of fields in the class and all its superclasses.
   *
   * @return the list of fields.
   */
  PsiField[] getAllFields();

  /**
   * Returns the list of methods in the class and all its superclasses.
   *
   * @return the list of methods.
   */
  PsiMethod[] getAllMethods();

  /**
   * Returns the list of inner classes for the class and all its superclasses..
   *
   * @return the list of inner classes.
   */
  PsiClass[] getAllInnerClasses();

  /**
   * Searches the class (and optionally its superclasses) for the field with the specified name.
   *
   * @param name       the name of the field to find.
   * @param checkBases if true, the field is also searched in the base classes of the class.
   * @return the field instance, or null if the field cannot be found.
   */
  @Nullable
  PsiField findFieldByName(String name, boolean checkBases);

  /**
   * Searches the class (and optionally its superclasses) for the method with
   * the signature matching the signature of the specified method.
   *
   * @param patternMethod the method used as a pattern for the search.
   * @param checkBases    if true, the method is also searched in the base classes of the class.
   * @return the method instance, or null if the method cannot be found.
   */
  @Nullable
  PsiMethod findMethodBySignature(PsiMethod patternMethod, boolean checkBases);

  /**
   * Searches the class (and optionally its superclasses) for the methods with the signature
   * matching the signature of the specified method. If the superclasses are not searched,
   * the method returns multiple results only in case of a syntax error (duplicate method).
   *
   * @param patternMethod the method used as a pattern for the search.
   * @param checkBases    if true, the method is also searched in the base classes of the class.
   * @return the found methods, or an empty array if no methods are found.
   */
  @NotNull
  PsiMethod[] findMethodsBySignature(PsiMethod patternMethod, boolean checkBases);

  /**
   * Searches the class (and optionally its superclasses) for the methods with the specified name.
   *
   * @param name       the name of the methods to find.
   * @param checkBases if true, the methods are also searched in the base classes of the class.
   * @return the found methods, or an empty array if no methods are found.
   */
  @NotNull
  PsiMethod[] findMethodsByName(String name, boolean checkBases);

  /**
   * Searches the class (and optionally its superclasses) for the methods with the specified name
   * and returns the methods along with their substitutors.
   *
   * @param name       the name of the methods to find.
   * @param checkBases if true, the methods are also searched in the base classes of the class.
   * @return the found methods and their substitutors, or an empty list if no methods are found.
   */
  @NotNull
  List<Pair<PsiMethod, PsiSubstitutor>> findMethodsAndTheirSubstitutorsByName(String name, boolean checkBases);

  /**
   * Returns the list of methods in the class and all its superclasses, along with their
   * substitutors.
   *
   * @return the list of methods and their substitutors
   */
  @NotNull
  List<Pair<PsiMethod, PsiSubstitutor>> getAllMethodsAndTheirSubstitutors();

  /**
   * Searches the class (and optionally its superclasses) for the inner class with the specified name.
   *
   * @param name       the name of the inner class to find.
   * @param checkBases if true, the inner class is also searched in the base classes of the class.
   * @return the inner class instance, or null if the inner class cannot be found.
   */
  @Nullable
  PsiClass findInnerClassByName(String name, boolean checkBases);

  /**
   * Returns the token representing the opening curly brace of the class.
   *
   * @return the token instance, or null if the token is missing in the source code file.
   */
  @Nullable
  PsiJavaToken getLBrace();

  /**
   * Returns the token representing the closing curly brace of the class.
   *
   * @return the token instance, or null if the token is missing in the source code file.
   */
  @Nullable
  PsiJavaToken getRBrace();

  /**
   * Returns the name identifier of the class.
   *
   * @return the name identifier, or null if the class is incomplete and the name identifier is missing.
   */
  @Nullable
  PsiIdentifier getNameIdentifier();

  /**
   * Returns the PSI member in which the class has been declared (for example,
   * the method containing the anonymous inner class, or the file containing a regular
   * class, or the class owning a type parameter).
   *
   * @return the member in which the class has been declared.
   */
  PsiElement getScope();

  /**
   * Checks if this class is an inheritor of the specified base class.
   *
   * @param baseClass the base class to check the inheritance.
   * @param checkDeep if false, only direct inheritance is checked; if true, the base class is
   *                  searched in the entire inheritance chain
   * @return true if the class is an inheritor, false otherwise
   */
  boolean isInheritor(PsiClass baseClass, boolean checkDeep);

  /**
   * Returns the {@link com.intellij.pom POM} representation of the class.
   *
   * @return the POM representation.
   */
  @Nullable
  PomMemberOwner getPom();

  /**
   * For an inner class, returns its containing class.
   *
   * @return the containing class, or null if the class is not an inner class.
   */
  @Nullable
  PsiClass getContainingClass();
}
