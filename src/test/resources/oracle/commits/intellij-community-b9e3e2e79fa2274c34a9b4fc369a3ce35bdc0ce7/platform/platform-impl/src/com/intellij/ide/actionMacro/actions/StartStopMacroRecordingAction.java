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
package com.intellij.ide.actionMacro.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actionMacro.ActionMacroManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAware;

/**
 * @author max
 */
public class StartStopMacroRecordingAction extends AnAction implements DumbAware {
  public void update(AnActionEvent e) {
    boolean editorAvailable = e.getDataContext().getData(DataConstants.EDITOR) != null;
    boolean isRecording = ActionMacroManager.getInstance().isRecording();

    e.getPresentation().setEnabled(editorAvailable || isRecording);
    e.getPresentation().setText(isRecording
                                ? IdeBundle.message("action.stop.macro.recording")
                                : IdeBundle.message("action.start.macro.recording"));
  }

  public void actionPerformed(AnActionEvent e) {
    if (!ActionMacroManager.getInstance().isRecording() ) {
      final ActionMacroManager manager = ActionMacroManager.getInstance();
      manager.startRecording(IdeBundle.message("macro.noname"));
    }
    else {
      ActionMacroManager.getInstance().stopRecording(PlatformDataKeys.PROJECT.getData(e.getDataContext()));
    }
  }
}
