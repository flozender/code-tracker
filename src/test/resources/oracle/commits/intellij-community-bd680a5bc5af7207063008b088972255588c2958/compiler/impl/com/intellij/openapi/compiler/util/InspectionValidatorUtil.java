/*
 * Copyright 2000-2007 JetBrains s.r.o.
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
package com.intellij.openapi.compiler.util;

import com.intellij.compiler.impl.FileSetCompileScope;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.descriptors.ConfigFile;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author peter
 */
public class InspectionValidatorUtil {
  private InspectionValidatorUtil() {
  }

  public static void addDescriptor(@NotNull final Collection<VirtualFile> result, @Nullable final ConfigFile configFile) {
    if (configFile != null) {
      ContainerUtil.addIfNotNull(configFile.getVirtualFile(), result);
    }
  }

  public static void addFile(@NotNull final Collection<VirtualFile> result, @Nullable final PsiFile psiFile) {
    if (psiFile != null) {
      ContainerUtil.addIfNotNull(psiFile.getVirtualFile(), result);
    }
  }


  public static Collection<VirtualFile> expandCompileScopeIfNeeded(final Collection<VirtualFile> result, final CompileContext context) {
    final ProjectFileIndex index = ProjectRootManager.getInstance(context.getProject()).getFileIndex();
    final THashSet<VirtualFile> set = new THashSet<VirtualFile>();
    final THashSet<Module> modules = new THashSet<Module>();
    for (VirtualFile file : result) {
      if (index.getSourceRootForFile(file) == null) {
        set.add(file);
        ContainerUtil.addIfNotNull(index.getModuleForFile(file), modules);
      }
    }
    if (!set.isEmpty()) {
      ((CompileContextEx)context).addScope(new FileSetCompileScope(set.toArray(new VirtualFile[set.size()]), modules.toArray(new Module[modules.size()])));
    }
    return result;
  }
}
