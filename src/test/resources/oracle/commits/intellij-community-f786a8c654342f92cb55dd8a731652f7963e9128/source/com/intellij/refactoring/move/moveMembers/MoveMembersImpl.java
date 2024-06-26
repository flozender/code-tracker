/**
 * created at Nov 21, 2001
 * @author Jeka
 */
package com.intellij.refactoring.move.moveMembers;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.util.RefactoringMessageUtil;

import java.util.HashSet;
import java.util.Set;

public class MoveMembersImpl {
  public static final String REFACTORING_NAME = RefactoringBundle.message("move.members.title");

  /**
   * element should be either not anonymous PsiClass whose members should be moved
   * or PsiMethod of a non-anonymous PsiClass
   * or PsiField of a non-anonymous PsiClass
   * or Inner PsiClass
   */
  public static void doMove(final Project project, PsiElement[] elements, PsiElement targetContainer, MoveCallback moveCallback) {
    if (elements.length == 0) {
      return;
    }
    final PsiClass sourceClass;
    if (elements[0].getParent() instanceof PsiClass) {
      sourceClass = (PsiClass) elements[0].getParent();
    } else {
      return;
    }
    final Set<PsiMember> preselectMembers = new HashSet<PsiMember>();
    for (PsiElement element : elements) {
      if (!sourceClass.equals(element.getParent())) {
        String message = RefactoringBundle.getCannotRefactorMessage(
          RefactoringBundle.message("members.to.be.moved.should.belong.to.the.same.class"));
        RefactoringMessageUtil.showErrorMessage(REFACTORING_NAME, message, HelpID.MOVE_MEMBERS, project);
        return;
      }
      if (element instanceof PsiField) {
        PsiField field = (PsiField)element;
        if (!field.hasModifierProperty(PsiModifier.STATIC)) {
          String fieldName = PsiFormatUtil.formatVariable(
            field,
            PsiFormatUtil.SHOW_NAME | PsiFormatUtil.SHOW_TYPE | PsiFormatUtil.TYPE_AFTER,
            PsiSubstitutor.EMPTY);
          String message = RefactoringBundle.message("field.0.is.not.static", fieldName,
                                                          REFACTORING_NAME);
          RefactoringMessageUtil.showErrorMessage(REFACTORING_NAME, message, HelpID.MOVE_MEMBERS, project);
          return;
        }
        preselectMembers.add(field);
      }
      else if (element instanceof PsiMethod) {
        PsiMethod method = (PsiMethod)element;
        String methodName = PsiFormatUtil.formatMethod(
          method,
          PsiSubstitutor.EMPTY, PsiFormatUtil.SHOW_NAME | PsiFormatUtil.SHOW_PARAMETERS,
          PsiFormatUtil.SHOW_TYPE
        );
        if (method.isConstructor()) {
          String message = RefactoringBundle.message("0.refactoring.cannot.be.applied.to.constructors", REFACTORING_NAME);
          RefactoringMessageUtil.showErrorMessage(REFACTORING_NAME, message, HelpID.MOVE_MEMBERS, project);
          return;
        }
        if (!method.hasModifierProperty(PsiModifier.STATIC)) {
          String message = RefactoringBundle.message("method.0.is.not.static", methodName,
                                                REFACTORING_NAME);
          RefactoringMessageUtil.showErrorMessage(REFACTORING_NAME, message, HelpID.MOVE_MEMBERS, project);
          return;
        }
        preselectMembers.add(method);
      }
      else if (element instanceof PsiClass) {
        PsiClass aClass = (PsiClass)element;
        if (!aClass.hasModifierProperty(PsiModifier.STATIC)) {
          String message = RefactoringBundle.message("inner.class.0.is.not.static", aClass.getQualifiedName(),
                                                REFACTORING_NAME);
          RefactoringMessageUtil.showErrorMessage(REFACTORING_NAME, message, HelpID.MOVE_MEMBERS, project);
          return;
        }
        preselectMembers.add(aClass);
      }
    }

    if (!sourceClass.isWritable()) {
      if (!RefactoringMessageUtil.checkReadOnlyStatus(project, sourceClass)) return;
    }

    final PsiClass initialTargerClass = targetContainer instanceof PsiClass? (PsiClass) targetContainer : (PsiClass) null;

    MoveMembersDialog dialog = new MoveMembersDialog(
            project,
            sourceClass,
            initialTargerClass,
            preselectMembers,
            moveCallback);
    dialog.show();
  }
}
