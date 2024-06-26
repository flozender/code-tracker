/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.execution.scratch;

import com.intellij.debugger.NoDataException;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.PositionManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Zhuravlev
 *         Date: 11-Sep-15
 */
public class JavaScratchPositionManager extends PositionManagerImpl{
  private final VirtualFile myScratchFile;

  public JavaScratchPositionManager(DebugProcessImpl debugProcess, VirtualFile scratchFile) {
    super(debugProcess);
    myScratchFile = scratchFile;
  }

  private PsiFile getScratchPsiFileByLocation(Project project, Location location) {
    if (location == null) {
      return null;
    }
    final ReferenceType refType = location.declaringType();
    if (refType == null) {
      return null;
    }
    final PsiFile psiFile = PsiManager.getInstance(project).findFile(myScratchFile);
    if (!(psiFile instanceof PsiJavaFile)) {
      return null;
    }
    final PsiClass[] classes = ((PsiJavaFile)psiFile).getClasses();
    if (classes.length == 0) {
      return null;
    }

    final String originalQName = refType.name();
    for (PsiClass aClass : classes) {
      if (StringUtil.equals(originalQName, aClass.getQualifiedName())) {
        return psiFile;
      }
    }

    final int dollar = originalQName.indexOf('$');
    final String alternativeQName = dollar > 0? originalQName.substring(0, dollar) : null;
    if (!StringUtil.isEmpty(alternativeQName)) {
      for (PsiClass aClass : classes) {
        if (StringUtil.equals(alternativeQName, aClass.getQualifiedName())) {
          return psiFile;
        }
      }
    }

    return null;
  }

  @Nullable
  @Override
  protected PsiFile getPsiFileByLocation(Project project, Location location) throws NoDataException {
    PsiFile file = getScratchPsiFileByLocation(project, location);
    if (file != null) {
      return file;
    }
    else {
      throw NoDataException.INSTANCE;
    }
  }
}
