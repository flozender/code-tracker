package com.intellij.debugger.ui;

import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.settings.DebuggerSettings;
import com.intellij.ide.util.ElementsChooser;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.util.ui.OptionsDialog;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * User: lex
 * Date: Oct 6, 2003
 * Time: 5:58:17 PM
 */



public class RunHotswapDialog extends OptionsDialog {
  private JPanel myPanel;
  private ElementsChooser<SessionItem> myElementsChooser;

  public RunHotswapDialog(Project project, java.util.List<DebuggerSession> sessions) {
    super(project);
    myPanel = new JPanel(new BorderLayout());
    final List<SessionItem> items = new ArrayList<SessionItem>(sessions.size());
    for (DebuggerSession session : sessions) {
      items.add(new SessionItem(session));
    }
    Collections.sort(items, new Comparator<SessionItem>() {
      public int compare(SessionItem debuggerSession, SessionItem debuggerSession1) {
        return debuggerSession.getSession().getSessionName().compareTo(debuggerSession1.getSession().getSessionName());
      }
    });
    myElementsChooser = new ElementsChooser<SessionItem>(items, true);
    myPanel.setBorder(IdeBorderFactory.createEmptyBorder(10, 0, 5, 0));
    //myElementsChooser.setBorder(IdeBorderFactory.createEmptyBorder(5, 0, 0, 0));
    if (sessions.size() > 0) {
      myElementsChooser.selectElements(items.subList(0, 1));
    }
    myPanel.add(myElementsChooser, BorderLayout.CENTER);
    //myPanel.add(new JLabel("Choose debug sessions to reload classes:"), BorderLayout.NORTH);
    if(sessions.size() == 1) {
      setTitle("Reload Changed Classes for " + sessions.get(0).getSessionName());
      myPanel.setVisible(false);
    }
    else {
      setTitle("Reload Changed Classes");
    }
    setButtonsAlignment(SwingUtilities.CENTER);
    this.init();
  }

  protected boolean isToBeShown() {
    return DebuggerSettings.RUN_HOTSWAP_ASK.equals(DebuggerSettings.getInstance().RUN_HOTSWAP_AFTER_COMPILE);
  }

  protected void setToBeShown(boolean value, boolean onOk) {
    if (value) {
      DebuggerSettings.getInstance().RUN_HOTSWAP_AFTER_COMPILE = DebuggerSettings.RUN_HOTSWAP_ASK;
    }
    else {
      if (onOk) {
        DebuggerSettings.getInstance().RUN_HOTSWAP_AFTER_COMPILE = DebuggerSettings.RUN_HOTSWAP_ALWAYS;
      }
      else {
        DebuggerSettings.getInstance().RUN_HOTSWAP_AFTER_COMPILE = DebuggerSettings.RUN_HOTSWAP_NEVER;
      }
    }
  }

  protected boolean shouldSaveOptionsOnCancel() {
    return true;
  }

  protected Action[] createActions(){
    setOKButtonText("Yes");
    setCancelButtonText("No");
    return new Action[]{getOKAction(), getCancelAction()};
  }

  protected JComponent createNorthPanel() {
    JLabel label = new JLabel("Some classes have been changed. Reload changed classes now?");
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(label, BorderLayout.CENTER);
    Icon icon = UIManager.getIcon("OptionPane.questionIcon");
    if (icon != null) {
      label.setIcon(icon);
      label.setIconTextGap(7);
    }
    return panel;
  }

  protected JComponent createCenterPanel() {
    return myPanel;
  }

  public Collection<DebuggerSession> getSessionsToReload() {
    final List<SessionItem> markedElements = myElementsChooser.getMarkedElements();
    final List<DebuggerSession>  sessions = new ArrayList<DebuggerSession>(markedElements.size());
    for (SessionItem item : markedElements) {
      sessions.add(item.getSession());
    }
    return sessions;
  }

  private static class SessionItem {
    private final DebuggerSession mySession;

    public SessionItem(DebuggerSession session) {
      mySession = session;
    }

    public DebuggerSession getSession() {
      return mySession;
    }

    public String toString() {
      return mySession.getSessionName();
    }
  }
}
