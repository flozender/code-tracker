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
package com.intellij.debugger.engine.evaluation;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiCodeFragment;
import com.intellij.psi.PsiElement;

public interface CodeFragmentFactory extends ApplicationComponent {

  PsiCodeFragment createCodeFragment(TextWithImports item, PsiElement context, Project project);

  boolean isContextAccepted(PsiElement contextElement);

  /**
   * @return the name dsiplayed in UI (normally the language name this factory deals with)
   */
  String getDisplayName();
}
