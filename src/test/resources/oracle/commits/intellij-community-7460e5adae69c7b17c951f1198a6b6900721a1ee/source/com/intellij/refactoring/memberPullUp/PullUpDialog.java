/*
 * Created by IntelliJ IDEA.
 * User: dsl
 * Date: 18.06.2002
 * Time: 13:16:29
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.refactoring.memberPullUp;

import com.intellij.openapi.help.HelpManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.RefactoringDialog;
import com.intellij.refactoring.RefactoringSettings;
import com.intellij.refactoring.ui.ClassCellRenderer;
import com.intellij.refactoring.ui.MemberSelectionPanel;
import com.intellij.refactoring.util.RefactoringHierarchyUtil;
import com.intellij.refactoring.util.classMembers.*;
import com.intellij.ui.IdeBorderFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

public class PullUpDialog extends RefactoringDialog {
  private final Callback myCallback;
  private MemberSelectionPanel myMemberSelectionPanel;
  private MyMemberInfoModel myMemberInfoModel;
  private final PsiClass myClass;
  private final List mySuperClasses;
  private final MemberInfoStorage myMemberInfoStorage;
  private MemberInfo[] myMemberInfos;
  private JavaDocPanel myJavaDocPanel;

  private JComboBox myClassCombo;

  public static interface Callback {
    boolean checkConflicts(PullUpDialog dialog);
  }


  public PullUpDialog(Project project, PsiClass aClass, List superClasses,
                      MemberInfoStorage memberInfoStorage, Callback callback) {
    super(project, true);
    myClass = aClass;
    mySuperClasses = superClasses;
    myMemberInfoStorage = memberInfoStorage;
    myMemberInfos = myMemberInfoStorage.getClassMemberInfos(aClass).toArray(new MemberInfo[0]);
    myCallback = callback;

    setTitle(PullUpHandler.REFACTORING_NAME);

    init();
  }

  public PsiClass getSuperClass() {
    if (myClassCombo != null) {
      return (PsiClass) myClassCombo.getSelectedItem();
    }
    else {
      return null;
    }
  }

  public int getJavaDocPolicy() {
    return myJavaDocPanel.getPolicy();
  }

  public MemberInfo[] getSelectedMemberInfos() {
    ArrayList<MemberInfo> list = new ArrayList<MemberInfo>(myMemberInfos.length);
    for (int idx = 0; idx < myMemberInfos.length; idx++) {
      MemberInfo info = myMemberInfos[idx];
      if (info.isChecked() && myMemberInfoModel.isMemberEnabled(info)) {
        list.add(info);
      }
    }
    return list.toArray(new MemberInfo[list.size()]);
  }

  protected String getDimensionServiceKey() {
    return "#com.intellij.refactoring.memberPullUp.PullUpDialog";
  }

  InterfaceContainmentVerifier getContainmentVerifier() {
    return myInterfaceContainmentVerifier;
  }

  protected JComponent createNorthPanel() {
    JPanel panel = new JPanel();

    panel.setBorder(IdeBorderFactory.createBorder());

    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();

    gbConstraints.insets = new Insets(4, 8, 4, 8);
    gbConstraints.weighty = 1;
    gbConstraints.weightx = 1;
    gbConstraints.gridy = 0;
    gbConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gbConstraints.fill = GridBagConstraints.BOTH;
    gbConstraints.anchor = GridBagConstraints.WEST;
    final JLabel classComboLabel = new JLabel("Pull up members of " + myClass.getQualifiedName()
                             + " to:");
    panel.add(classComboLabel, gbConstraints);

    myClassCombo = new JComboBox(mySuperClasses.toArray());
    myClassCombo.setRenderer(new ClassCellRenderer());
    classComboLabel.setLabelFor(myClassCombo);
    classComboLabel.setDisplayedMnemonic('P');
//    myClassCombo.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    PsiClass nearestBase = RefactoringHierarchyUtil.getNearestBaseClass(myClass, false);
    int indexToSelect = 0;
    if (nearestBase != null) {
      indexToSelect = mySuperClasses.indexOf(nearestBase);
    }
    myClassCombo.setSelectedIndex(indexToSelect);
    myClassCombo.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          updateMemberInfo();
          if (myMemberSelectionPanel != null) {
            myMemberInfoModel.setSuperClass(getSuperClass());
            myMemberSelectionPanel.getTable().setMemberInfos(myMemberInfos);
            myMemberSelectionPanel.getTable().fireExternalDataChange();
          }
        }
      }
    });
    gbConstraints.gridy++;
    panel.add(myClassCombo, gbConstraints);

    return panel;
  }

  protected void doHelpAction() {
    HelpManager.getInstance().invokeHelp(HelpID.MEMBERS_PULL_UP);
  }

  private void updateMemberInfo() {
    final PsiClass targetClass = (PsiClass) myClassCombo.getSelectedItem();
    myMemberInfos = myMemberInfoStorage.getMemberInfosList(targetClass);
    /*Set duplicated = myMemberInfoStorage.getDuplicatedMemberInfos(targetClass);
    for (Iterator iterator = duplicated.iterator(); iterator.hasNext();) {
      ((MemberInfo) iterator.next()).setChecked(false);
    }*/
  }

  /*protected JComponent createSouthPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(super.createSouthPanel(), BorderLayout.CENTER);
    myCbPreviewUsages.setSelected(RefactoringSettings.getInstance().PULL_UP_MEMBERS_PREVIEW_USAGES);
    myCbPreviewUsages.setMnemonic('P');
    panel.add(myCbPreviewUsages, BorderLayout.WEST);
    return panel;
  }*/

  protected void doAction() {
    if (!myCallback.checkConflicts(this)) return;
    RefactoringSettings.getInstance().PULL_UP_MEMBERS_JAVADOC = myJavaDocPanel.getPolicy();
    close(OK_EXIT_CODE);
  }

  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    myMemberSelectionPanel = new MemberSelectionPanel("Members to be pulled up", myMemberInfos, "Make abstract");
    myMemberInfoModel = new MyMemberInfoModel();
    myMemberInfoModel.memberInfoChanged(new MemberInfoChange(myMemberInfos));
    myMemberSelectionPanel.getTable().setMemberInfoModel(myMemberInfoModel);
    myMemberSelectionPanel.getTable().addMemberInfoChangeListener(myMemberInfoModel);
    panel.add(myMemberSelectionPanel, BorderLayout.CENTER);

    myJavaDocPanel = new JavaDocPanel("JavaDoc for abstracts");
    myJavaDocPanel.setPolicy(RefactoringSettings.getInstance().PULL_UP_MEMBERS_JAVADOC);
    panel.add(myJavaDocPanel, BorderLayout.EAST);
    return panel;
  }
  private InterfaceContainmentVerifier myInterfaceContainmentVerifier =
    new InterfaceContainmentVerifier() {
      public boolean checkedInterfacesContain(PsiMethod psiMethod) {
        return PullUpHelper.checkedInterfacesContain(myMemberInfos, psiMethod);
      }
    };

  private class MyMemberInfoModel extends UsesAndInterfacesDependencyMemberInfoModel {
    public MyMemberInfoModel() {
      super(myClass, getSuperClass(), false, myInterfaceContainmentVerifier);
    }


    public boolean isMemberEnabled(MemberInfo member) {
      PsiClass currentSuperClass = getSuperClass();
      if(currentSuperClass == null) return true;
      if (myMemberInfoStorage.getDuplicatedMemberInfos(currentSuperClass).contains(member)) return false;
      if (myMemberInfoStorage.getExtending(currentSuperClass).contains(member.getMember())) return false;
      if (!currentSuperClass.isInterface()) return true;

      PsiElement element = member.getMember();
      if (element instanceof PsiClass && ((PsiClass) element).isInterface()) return true;
      if (element instanceof PsiField) {
        return ((PsiModifierListOwner) element).hasModifierProperty(PsiModifier.STATIC);
      }
      return true;
    }

    public boolean isAbstractEnabled(MemberInfo member) {
      PsiClass currentSuperClass = getSuperClass();
      if (currentSuperClass == null || !currentSuperClass.isInterface()) return true;
      return false;
    }

    public boolean isAbstractWhenDisabled(MemberInfo member) {
      PsiClass currentSuperClass = getSuperClass();
      if(currentSuperClass == null) return false;
      if (currentSuperClass.isInterface()) {
        if (member.getMember() instanceof PsiMethod) {
          return true;
        }
      }
      return false;
    }

    public int checkForProblems(MemberInfo member) {
      if (member.isChecked()) return OK;
      PsiClass currentSuperClass = getSuperClass();

      if (currentSuperClass != null && currentSuperClass.isInterface()) {
        PsiElement element = member.getMember();
        if (element instanceof PsiModifierListOwner) {
          if (((PsiModifierListOwner) element).hasModifierProperty(PsiModifier.STATIC)) {
            return super.checkForProblems(member);
          }
        }
        return OK;
      }
      else {
        return super.checkForProblems(member);
      }
    }

    public Boolean isFixedAbstract(MemberInfo member) {
      return Boolean.TRUE;
    }
  }
}
