/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.roots;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface providing root information model for a given module.
 *
 * @author dsl
 */
public interface ModuleRootModel {
  /**
   * Returns the module to which the model belongs.
   *
   * @return the module instance.
   */
  @NotNull
  Module getModule();

  /**
   * Use this method to obtain all content entries of a module. Entries are given in
   * lexicographical order of their paths.
   *
   * @return list of content entries for this module
   * @see ContentEntry
   */
  ContentEntry[] getContentEntries();

  /**
   * Use this method to obtain order of roots of a module. Order of entries is important.
   *
   * @return list of order entries for this module
   */
  @NotNull
  OrderEntry[] getOrderEntries();

 /*
  * Returns the JDK used by the module.
  *
  * @return either module-specific or inherited JDK
  * @see #isSdkInherited()
  */
 @Nullable
 Sdk getSdk();

 /**
  * Returns <code>true</code> if JDK for this module is inherited from a project.
  *
  * @return true if the JDK is inherited, false otherwise
  * @see ProjectRootManager#getProjectJdk()
  * @see ProjectRootManager#setProjectJdk(com.intellij.openapi.projectRoots.Sdk)
  */
 boolean isSdkInherited();


 /**

  /**
   * Returns an array of content roots from all content entries. A helper method.
   *
   * @return the array of content roots.
   * @see #getContentEntries()
   */
  @NotNull VirtualFile[] getContentRoots();

  /**
   * Returns an array of content root urls from all content entries. A helper method.
   *
   * @return the array of content root URLs.
   * @see #getContentEntries()
   */
  @NotNull String[] getContentRootUrls();

  /**
   * Returns an array of exclude roots from all content entries. A helper method.
   *
   * @return the array of excluded roots.
   * @see #getContentEntries()
   */
  @NotNull VirtualFile[] getExcludeRoots();

  /**
   * Returns an array of exclude root urls from all content entries. A helper method.
   *
   * @return the array of excluded root URLs.
   * @see #getContentEntries()
   */
  @NotNull String[] getExcludeRootUrls();

  /**
   * Returns an array of source roots from all content entries. A helper method.
   *
   * @return the array of source roots.
   * @see #getContentEntries()
   * @see #getSourceRoots(boolean)
   */
  @NotNull
  VirtualFile[] getSourceRoots();

  /**
   * Returns an array of source roots from all content entries. A helper method.
   *
   * @param includingTests determines whether test source roots should be included in the result
   * @return the array of source roots.
   * @see #getContentEntries()
   * @since 10.0
   */
  @NotNull
  VirtualFile[] getSourceRoots(boolean includingTests);

  /**
   * Returns an array of source root urls from all content entries. A helper method.
   *
   * @return the array of source root URLs.
   * @see #getContentEntries()
   * @see #getSourceRootUrls(boolean)
   */
  @NotNull String[] getSourceRootUrls();

  /**
   * Returns an array of source root urls from all content entries. A helper method.
   *
   * @param includingTests determines whether test source root urls should be included in the result
   * @return the array of source root URLs.
   * @see #getContentEntries()
   * @since 10.0
   */
  @NotNull String[] getSourceRootUrls(boolean includingTests);

  /**
   * Returns an exploded directory path of the module, if it is valid.
   *
   * @return exploded directory path of the module, or null if not applicable or not set.
   */
  @Nullable
  VirtualFile getExplodedDirectory();

  /**
   * Returns an exploded directory path url.
   *
   * @return exploded directory path url, or null if it has never been set
   *         or if not applicable for this module.
   */
  @Nullable
  String getExplodedDirectoryUrl();

  /**
   * Passes all order entries in the module to the specified visitor.
   *
   * @param policy       the visitor to accept.
   * @param initialValue the default value to be returned by the visit process.
   * @return the value returned by the visitor.
   * @see OrderEntry#accept(RootPolicy, Object)
   */
  <R> R processOrder(RootPolicy<R> policy, R initialValue);

  /**
   * Returns {@link OrderEnumerator} instance which can be used to process order entries of the module (with or without dependencies) and
   * collect classes or source roots
   *
   * @return {@link OrderEnumerator} instance
   * @since 10.0
   */
  @NotNull
  OrderEnumerator orderEntries();

  /**
   * Returns list of module names <i>this module</i> depends on.
   *
   * @return the list of module names this module depends on.
   */
  @NotNull String[] getDependencyModuleNames();

  @NotNull VirtualFile[] getRootPaths(OrderRootType rootType);
  @NotNull String[] getRootUrls(OrderRootType rootType);

  <T> T getModuleExtension(Class<T> klass);
}
