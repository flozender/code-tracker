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

package com.intellij.openapi.wm.impl.status;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.HectorComponent;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightLevelUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.PowerSaveMode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.UIBundle;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class TogglePopupHintsPanel extends EditorBasedWidget implements StatusBarWidget.Multiframe, StatusBarWidget.IconPresentation {
  private static final Icon INSPECTIONS_ICON = AllIcons.Ide.HectorOn;
  private static final Icon INSPECTIONS_OFF_ICON = AllIcons.Ide.HectorOff;
  private static final Icon SYNTAX_ONLY_ICON = AllIcons.Ide.HectorSyntax;
  private static final Icon EMPTY_ICON = AllIcons.Ide.HectorNo;

  private Icon myCurrentIcon;
  private String myToolTipText;

  public TogglePopupHintsPanel(@NotNull final Project project) {
    super(project);
    myCurrentIcon = EMPTY_ICON;
    myConnection.subscribe(PowerSaveMode.TOPIC, new PowerSaveMode.Listener() {
      @Override
      public void powerSaveStateChanged() {
        updateStatus();
      }
    });
  }

  @Override
  public void selectionChanged(FileEditorManagerEvent event) {
    updateStatus();
  }


  @Override
  public void fileOpened(FileEditorManager source, VirtualFile file) {
    updateStatus();
  }

  @Override
  public StatusBarWidget copy() {
    return new TogglePopupHintsPanel(getProject());
  }

  @NotNull
  public Icon getIcon() {
    return myCurrentIcon;
  }

  public String getTooltipText() {
    return myToolTipText;
  }

  public Consumer<MouseEvent> getClickConsumer() {
    return new Consumer<MouseEvent>() {
      public void consume(final MouseEvent e) {
        Point point = new Point(0, 0);
        final PsiFile file = getCurrentFile();
        if (file != null) {
          if (!DaemonCodeAnalyzer.getInstance(file.getProject()).isHighlightingAvailable(file)) return;
          final HectorComponent component = new HectorComponent(file);
          final Dimension dimension = component.getPreferredSize();
          point = new Point(point.x - dimension.width, point.y - dimension.height);
          component.showComponent(new RelativePoint(e.getComponent(), point));
        }
      }
    };
  }

  @NotNull
  public String ID() {
    return "InspectionProfile";
  }

  public WidgetPresentation getPresentation(@NotNull PlatformType type) {
    return this;
  }

  public void clear() {
    myCurrentIcon = EMPTY_ICON;
    myToolTipText = null;
    myStatusBar.updateWidget(ID());
  }

  public void updateStatus() {
    updateStatus(getCurrentFile());
  }

  private void updateStatus(PsiFile file) {
    if (isStateChangeable(file)) {
      if (PowerSaveMode.isEnabled()) {
        myCurrentIcon = EMPTY_ICON;
        myToolTipText = "Code analysis is disabled in power save mode. ";
      }
      else if (HighlightLevelUtil.shouldInspect(file)) {
        myCurrentIcon = INSPECTIONS_ICON;
        myToolTipText = "Current inspection profile: " +
                        InspectionProjectProfileManager.getInstance(file.getProject()).getInspectionProfile().getName() +
                        ". ";
      }
      else if (HighlightLevelUtil.shouldHighlight(file)) {
        myCurrentIcon = SYNTAX_ONLY_ICON;
        myToolTipText = "Highlighting level is: Syntax. ";
      }
      else {
        myCurrentIcon = INSPECTIONS_OFF_ICON;
        myToolTipText = "Inspections are off. ";
      }
      myToolTipText += UIBundle.message("popup.hints.panel.click.to.configure.highlighting.tooltip.text");
    }
    else {
      myCurrentIcon = EMPTY_ICON;
      myToolTipText = null;
    }

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      myStatusBar.updateWidget(ID());
    }
  }

  private static boolean isStateChangeable(PsiFile file) {
    return file != null && DaemonCodeAnalyzer.getInstance(file.getProject()).isHighlightingAvailable(file);
  }

  @Nullable
  private PsiFile getCurrentFile() {
    VirtualFile virtualFile = getSelectedFile();
    if (virtualFile != null && virtualFile.isValid()){
      return PsiManager.getInstance(getProject()).findFile(virtualFile);
    }
    return null;
  }

}
