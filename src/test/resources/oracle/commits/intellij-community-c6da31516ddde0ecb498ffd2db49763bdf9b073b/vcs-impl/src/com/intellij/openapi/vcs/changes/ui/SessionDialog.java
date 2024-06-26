/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.openapi.vcs.changes.ui;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.CommitSession;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class SessionDialog extends DialogWrapper {
  private final CommitSession mySession;
  private final List<Change> myChanges;

  private final Alarm myOKButtonUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
  private final String myCommitMessage;

  private final JPanel myCenterPanel = new JPanel(new BorderLayout());
  private JComponent myConfigurationComponent;

  public SessionDialog(String title, Project project,
                       CommitSession session, List<Change> changes,
                       String commitMessage) {
    super(project, true);
    mySession = session;
    myChanges = changes;
    myCommitMessage = commitMessage;
    myConfigurationComponent = createConfigurationUI(mySession, myChanges, myCommitMessage);
    setTitle(CommitChangeListDialog.trimEllipsis(title));
    init();
    updateButtons();
  }

  public static JComponent createConfigurationUI(final CommitSession session, final List<Change> changes, final String commitMessage) {
    try {
      return session.getAdditionalConfigurationUI(changes, commitMessage);
    }
    catch(AbstractMethodError e) {
      return session.getAdditionalConfigurationUI();
    }
  }

  @Nullable
  protected JComponent createCenterPanel() {
    myCenterPanel.add(myConfigurationComponent, BorderLayout.CENTER);
    return myCenterPanel;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myConfigurationComponent;
  }

  private void updateButtons() {
    setOKActionEnabled(mySession.canExecute(myChanges, myCommitMessage));
    myOKButtonUpdateAlarm.cancelAllRequests();
    myOKButtonUpdateAlarm.addRequest(new Runnable() {
      public void run() {
        updateButtons();
      }
    }, 300, ModalityState.stateForComponent(myCenterPanel));
  }

  protected void dispose() {
    super.dispose();
    myOKButtonUpdateAlarm.cancelAllRequests();
  }
}
