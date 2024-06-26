/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.internal;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;
import com.intellij.util.Processor;

public class StaticIconFieldsAction extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = LangDataKeys.PROJECT.getData(e.getDataContext());

    PsiClass allIcons =
      JavaPsiFacade.getInstance(project).findClass("com.intellij.icons.AllIcons", GlobalSearchScope.allScope(project));

    final UsageViewPresentation presentation = new UsageViewPresentation();
    presentation.setTabName("Statics");
    presentation.setTabText("Statitcs");
    final UsageView view = UsageViewManager.getInstance(project).showUsages(UsageTarget.EMPTY_ARRAY, new Usage[0], presentation);


    ReferencesSearch.search(allIcons).forEach(new Processor<PsiReference>() {
      @Override
      public boolean process(PsiReference reference) {
        PsiElement elt = reference.getElement();

        while (elt instanceof PsiExpression) elt = elt.getParent();

        if (elt instanceof PsiField) {
          UsageInfo info = new UsageInfo(elt, false);
          view.appendUsage(new UsageInfo2UsageAdapter(info));
        }

        return true;
      }
    });
  }
}

