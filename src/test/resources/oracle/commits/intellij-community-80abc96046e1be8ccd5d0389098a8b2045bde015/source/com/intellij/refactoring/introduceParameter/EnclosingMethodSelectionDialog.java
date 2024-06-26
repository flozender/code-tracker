/*
 * Created by IntelliJ IDEA.
 * User: dsl
 * Date: 06.06.2002
 * Time: 11:30:13
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.refactoring.introduceParameter;

import com.intellij.openapi.help.HelpManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.RefactoringSettings;
import com.intellij.refactoring.turnRefsToSuper.TurnRefsToSuperHandler;
import com.intellij.refactoring.ui.ClassCellRenderer;
import com.intellij.refactoring.ui.MethodCellRenderer;
import com.intellij.refactoring.util.RefactoringHierarchyUtil;
import com.intellij.ui.IdeBorderFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public class EnclosingMethodSelectionDialog extends DialogWrapper {
  private final List<PsiMethod> myEnclosingMethods;

  private JList myEnclosingMethodsList = null;
  private final JCheckBox myCbReplaceInstanceOf = new JCheckBox("Use interface/superclass in instanceof");

  EnclosingMethodSelectionDialog(Project project, List<PsiMethod> enclosingMethods) {
    super(project, true);

    myEnclosingMethods = enclosingMethods;

    setTitle("Introduce Parameter");
    init();
  }

  public PsiMethod getSelectedMethod() {
    if(myEnclosingMethodsList != null) {
      return (PsiMethod) myEnclosingMethodsList.getSelectedValue();
    }
    else {
      return null;
    }
  }

  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction()/*, getHelpAction()*/};
  }

  public JComponent getPreferredFocusedComponent() {
    return myEnclosingMethodsList;
  }

  protected JComponent createNorthPanel() {
    JPanel panel = new JPanel();
    panel.setBorder(IdeBorderFactory.createBorder());

    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();

    gbConstraints.insets = new Insets(4, 8, 4, 8);
    gbConstraints.weighty = 0;
    gbConstraints.weightx = 1;
    gbConstraints.gridy = 0;
    gbConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gbConstraints.gridheight = 1;
    gbConstraints.fill = GridBagConstraints.BOTH;
    gbConstraints.anchor = GridBagConstraints.WEST;
    panel.add(new JLabel("Introduce parameter to method:"), gbConstraints);

    gbConstraints.weighty = 1;
    myEnclosingMethodsList = new JList(myEnclosingMethods.toArray());
    myEnclosingMethodsList.setCellRenderer(new MethodCellRenderer());
    myEnclosingMethodsList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    int indexToSelect = 0;
    myEnclosingMethodsList.setSelectedIndex(indexToSelect);
    gbConstraints.gridy++;
    panel.add(new JScrollPane(myEnclosingMethodsList), gbConstraints);

    return panel;
  }

  protected String getDimensionServiceKey() {
    return "#com.intellij.refactoring.introduceParameter.EnclosingMethodSelectonDialog";
  }

  protected void doOKAction() {
    if (!isOKActionEnabled())
      return;

    super.doOKAction();
  }

  protected JComponent createCenterPanel() {
    return null;
  }

}
