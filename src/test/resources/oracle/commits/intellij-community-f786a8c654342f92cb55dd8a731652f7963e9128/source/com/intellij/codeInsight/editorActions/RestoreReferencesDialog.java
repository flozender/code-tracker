package com.intellij.codeInsight.editorActions;

import com.intellij.CommonBundle;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.util.FQNameCellRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.psi.PsiClass;

import javax.swing.*;
import java.awt.*;

class RestoreReferencesDialog extends DialogWrapper {
  private Object[] myNamedElements;
  private JList myList;
  private Object[] mySelectedElements = PsiClass.EMPTY_ARRAY;
  private boolean myContainsClassesOnly = true;

  public RestoreReferencesDialog(final Project project, final Object[] elements) {
    super(project, true);
    myNamedElements = elements;
    for (Object element : elements) {
      if (!(element instanceof PsiClass)) {
        myContainsClassesOnly = false;
        break;
      }
    }
    if (myContainsClassesOnly) {
      setTitle(CodeInsightBundle.message("dialog.import.on.paste.title"));
    }
    else {
      setTitle(CodeInsightBundle.message("dialog.import.on.paste.title2"));
    }
    init();

    myList.setSelectionInterval(0, myNamedElements.length - 1);
  }

  protected void doOKAction() {
    Object[] values = myList.getSelectedValues();
    mySelectedElements = new Object[values.length];
    System.arraycopy(values, 0, mySelectedElements, 0, values.length);
    super.doOKAction();
  }

  protected JComponent createCenterPanel() {
    final JPanel panel = new JPanel(new BorderLayout());
    myList = new JList(myNamedElements);
    myList.setCellRenderer(new FQNameCellRenderer());
    myList.setBorder(BorderFactory.createEtchedBorder());
    panel.add(new JScrollPane(myList), BorderLayout.CENTER);

    JTextArea area = new JTextArea(myContainsClassesOnly ?
                                   CodeInsightBundle.message("dialog.paste.on.import.text") :
                                   CodeInsightBundle.message("dialog.paste.on.import.text2"));
    area.setEditable(false);
    area.setBackground(this.getContentPane().getBackground());
    area.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    panel.add(area, BorderLayout.NORTH);

    final JPanel buttonPanel = new JPanel(new VerticalFlowLayout());
    final JButton okButton = new JButton(CommonBundle.getOkButtonText());
    getRootPane().setDefaultButton(okButton);
    buttonPanel.add(okButton);
    final JButton cancelButton = new JButton(CommonBundle.getCancelButtonText());
    buttonPanel.add(cancelButton);

    panel.setPreferredSize(new Dimension(500, 400));

    return panel;
  }


  protected String getDimensionServiceKey(){
    return "#com.intellij.codeInsight.editorActions.RestoreReferencesDialog";
  }

  public Object[] getSelectedElements(){
    return mySelectedElements;
  }
}