/*
 * Class ExceptionBreakpointPropertiesPanel
 * @author Jeka
 */
package com.intellij.debugger.ui.breakpoints;

import com.intellij.debugger.DebuggerBundle;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ExceptionBreakpointPropertiesPanel extends BreakpointPropertiesPanel {
  private JCheckBox myNotifyCaughtCheckBox;
  private JCheckBox myNotifyUncaughtCheckBox;
  private ExceptionBreakpoint myExceptionBreakpoint;

  public ExceptionBreakpointPropertiesPanel(Project project) {
    super(project, ExceptionBreakpoint.CATEGORY);
  }

  protected TreeClassChooser.ClassFilter createClassConditionFilter() {
    return null;
  }

  protected JComponent createSpecialBox() {

    myNotifyCaughtCheckBox = new JCheckBox(DebuggerBundle.message("label.exception.breakpoint.properties.panel.caught.exception"));
    myNotifyUncaughtCheckBox = new JCheckBox(DebuggerBundle.message("label.exception.breakpoint.properties.panel.uncaught.exception"));

    Box notificationsBox = Box.createVerticalBox();
    JPanel _panel = new JPanel(new BorderLayout());
    _panel.add(myNotifyCaughtCheckBox, BorderLayout.NORTH);
    notificationsBox.add(_panel);
    _panel = new JPanel(new BorderLayout());
    _panel.add(myNotifyUncaughtCheckBox, BorderLayout.NORTH);
    notificationsBox.add(_panel);

    _panel = new JPanel(new BorderLayout());
    JPanel _panel0 = new JPanel(new BorderLayout());
    _panel0.add(notificationsBox, BorderLayout.CENTER);
    _panel0.add(Box.createHorizontalStrut(3), BorderLayout.WEST);
    _panel0.add(Box.createHorizontalStrut(3), BorderLayout.EAST);
    _panel.add(_panel0, BorderLayout.NORTH);
    _panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), DebuggerBundle.message("label.exception.breakpoint.properties.panel.group.notifications")));

    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (!myNotifyCaughtCheckBox.isSelected() && !myNotifyUncaughtCheckBox.isSelected()) {
          Object source = e.getSource();
          JCheckBox toCheck = null;
          if (myNotifyCaughtCheckBox.equals(source)) {
            toCheck = myNotifyUncaughtCheckBox;
          }
          else if (myNotifyUncaughtCheckBox.equals(source)) {
            toCheck = myNotifyCaughtCheckBox;
          }
          if (toCheck != null) {
            toCheck.setSelected(true);
          }
        }
      }
    };
    myNotifyCaughtCheckBox.addActionListener(listener);
    myNotifyUncaughtCheckBox.addActionListener(listener);
    return _panel;
  }

  protected void updateCheckboxes() {
    super.updateCheckboxes();
    myPassCountCheckbox.setEnabled(!(myExceptionBreakpoint instanceof AnyExceptionBreakpoint));
  }

  public void initFrom(Breakpoint breakpoint) {
    ExceptionBreakpoint exceptionBreakpoint = (ExceptionBreakpoint)breakpoint;
    myExceptionBreakpoint = exceptionBreakpoint;
    super.initFrom(breakpoint);

    myNotifyCaughtCheckBox.setSelected(exceptionBreakpoint.NOTIFY_CAUGHT);
    myNotifyUncaughtCheckBox.setSelected(exceptionBreakpoint.NOTIFY_UNCAUGHT);
  }

  public void saveTo(Breakpoint breakpoint, Runnable afterUpdate) {
    ExceptionBreakpoint exceptionBreakpoint = (ExceptionBreakpoint)breakpoint;
    exceptionBreakpoint.NOTIFY_CAUGHT = myNotifyCaughtCheckBox.isSelected();
    exceptionBreakpoint.NOTIFY_UNCAUGHT = myNotifyUncaughtCheckBox.isSelected();

    super.saveTo(breakpoint, afterUpdate);
  }
}