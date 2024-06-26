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
package com.intellij.ide;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;

public class JavaDataAccessors {
  public static final DataAccessor<PsiPackage> FILE_PACKAGE = new DataAccessor<PsiPackage>() {
    public PsiPackage getImpl(DataContext dataContext) throws NoDataException {
      PsiFile psiFile = DataAccessors.PSI_FILE.getNotNull(dataContext);
      PsiDirectory containingDirectory = psiFile.getContainingDirectory();
      if (containingDirectory == null || !containingDirectory.isValid()) return null;
      return JavaDirectoryService.getInstance().getPackage(containingDirectory);
    }
  };

  private JavaDataAccessors() {
  }
}
