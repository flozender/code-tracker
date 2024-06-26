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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a reference found in Java code (either an identifier or a sequence of identifiers
 * separated by periods, optionally with generic type arguments).
 */
public interface PsiJavaCodeReferenceElement extends PsiElement, PsiJavaReference {
  /**
   * The empty array of PSI Java code references which can be reused to avoid unnecessary allocations.
   */
  PsiJavaCodeReferenceElement[] EMPTY_ARRAY = new PsiJavaCodeReferenceElement[0];

  /**
   * Returns the element representing the name of the referenced element.
   *
   * @return the element, or null if the reference element is not physical (for example,
   * exists in compiled code).
   */
  @Nullable
  PsiElement getReferenceNameElement();

  /**
   * Returns the list of type arguments specified on the reference.
   *
   * @return the type argument list, or null if the reference does not have any type arguments.
   */
  @Nullable
  PsiReferenceParameterList getParameterList();

  /**
   * Returns the array of types for the type arguments specified on the reference.
   *
   * @return the array of types, or an empty array if the reference does not have any type arguments.
   */
  @NotNull
  PsiType[] getTypeParameters();

  /**
   * Returns the qualifier of the reference (the element representing the content up to the
   * last period).
   *
   * @return the qualifier, or null if the reference is not qualified.
   */
  @Nullable
  PsiElement getQualifier();

  /**
   * Checks if the reference is qualified (consists of elements separated with periods).
   *
   * @return true if the reference is qualified, false otherwise.
   */
  boolean isQualified();

  /**
   * Returns the text of the reference including its qualifier.
   *
   * @return the qualified text of the reference.
   */
  String getQualifiedName();

  /**
   * Returns the text of the reference not including its qualifier.
   *
   * @return the non-qualified text of the reference, or null if the reference
   * element is incomplete.
   */
  @Nullable
  String getReferenceName();
}
