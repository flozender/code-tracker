/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.unscramble;

import com.intellij.diagnostic.IdeErrorsDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author spleaner
 */
public class AnalyzeStacktraceOnErrorAction extends AnAction {
  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    String message = getMessage(e);
    e.getPresentation().setEnabledAndVisible(project != null && message != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    String message = getMessage(e);
    if (project == null || message == null) return;
    AnalyzeStacktraceUtil.addConsole(project, null, "<Stacktrace>", message);
  }

  private static String getMessage(AnActionEvent e) {
    return IdeErrorsDialog.CURRENT_TRACE_KEY.getData(e.getDataContext());
  }
}