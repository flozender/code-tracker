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

import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Java package.
 */
public interface PsiPackage extends PsiElement, PsiNamedElement, NavigationItem {
  /**
   * Returns the full-qualified name of the package.
   *
   * @return the full-qualified name, or an empty string for the default package.
   */
  @NotNull
  String getQualifiedName();

  /**
   * Returns the array of all directories (under all source roots in the project)
   * corresponding to the package.
   *
   * @return the array of directories.
   */
  @NotNull
  PsiDirectory[] getDirectories();

  /**
   * Returns the array of directories corresponding to the package in the specified search scope.
   *
   * @param scope the scope in which directories are searched.
   * @return the array of directories.
   */
  @NotNull
  PsiDirectory[] getDirectories(GlobalSearchScope scope);

  /**
   * Returns the parent of the package.
   *
   * @return the parent package, or null for the default package.
   */
  @Nullable
  PsiPackage getParentPackage();

  /**
   * Returns the list of subpackages of this package under all source roots of the project.
   *
   * @return the array of subpackages.
   */
  @NotNull
  PsiPackage[] getSubPackages();

  /**
   * Returns the list of subpackages of this package in the specified search scope.
   *
   * @param scope the scope in which packages are searched.
   * @return the array of subpackages.
   */
  @NotNull
  PsiPackage[] getSubPackages(GlobalSearchScope scope);

  /**
   * Returns the list of classes in all directories corresponding to the package.
   *
   * @return the array of classes.
   */
  @NotNull
  PsiClass[] getClasses();

  /**
   * Returns the list of classes in directories corresponding to the package in the specified
   * search scope.
   *
   * @param scope the scope in which directories are searched.
   * @return the array of classes.
   */
  @NotNull
  PsiClass[] getClasses(GlobalSearchScope scope);

  /**
   * Returns the list of package-level annotations for the package.
   *
   * @return the list of annotations, or null if the package does not have any package-level annotations.
   */
  @Nullable PsiModifierList getAnnotationList();

  /**
   * Checks if it is possible to rename the package to the specified name,
   * and throws an exception if the rename is not possible. Does not actually modify anything.
   *
   * @param name the new name to check the renaming possibility for. 
   * @throws IncorrectOperationException if the rename is not supported or not possible for some reason.
   */
  void checkSetName(String name) throws IncorrectOperationException;

  /**
   * This method must be invoked on the package after all directoris corresponding
   * to it have been renamed/moved accordingly to qualified name change.
   *
   * @param newQualifiedName the new qualified name of the package.
   */
  void handleQualifiedNameChange(String newQualifiedName);

  /**
   * Returns source roots that this package occurs in package prefixes of.
   *
   * @return the array of virtual files for the source roots.
   */
  VirtualFile[] occursInPackagePrefixes();
}
