/*
 * Created by IntelliJ IDEA.
 * User: dsl
 * Date: 17.04.2002
 * Time: 14:39:57
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.refactoring.makeStatic;

import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.refactoring.util.ParameterTablePanel;
import com.intellij.refactoring.util.RefactoringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

public class MakeStaticUtil {
  public static InternalUsageInfo[] findClassRefsInMember(PsiTypeParameterListOwner member, boolean includeSelf) {
    PsiClass containingClass = member.getContainingClass();
    ArrayList<InternalUsageInfo> classRefs = new ArrayList<InternalUsageInfo>();
    addClassRefs(member, classRefs, containingClass, member, includeSelf);
    return classRefs.toArray(new InternalUsageInfo[classRefs.size()]);
  }

  private static void addClassRefs(PsiTypeParameterListOwner originalMember, ArrayList<InternalUsageInfo> classRefs, PsiClass containingClass, PsiElement element, boolean includeSelf) {
    if (element instanceof PsiReferenceExpression) {
      PsiReferenceExpression ref = (PsiReferenceExpression)element;

      if (!ref.isQualified()) { // resolving only "naked" fields and methods
        PsiElement resolved = ref.resolve();

        if (resolved instanceof PsiMember && !((PsiMember)resolved).hasModifierProperty(PsiModifier.STATIC)) {
          PsiMember member = (PsiMember)resolved;
          if (originalMember.getManager().areElementsEquivalent(member, originalMember)) {
            if (includeSelf) {
              classRefs.add(new SelfUsageInfo(element, originalMember));
            }
          }
          else {
            if (isPartOf(member.getContainingClass(), containingClass)) {
              classRefs.add(new InternalUsageInfo(element, member));
            }
          }
        }
      }
    }
    else if (element instanceof PsiThisExpression || element instanceof PsiSuperExpression || element instanceof PsiNewExpression) {
      PsiJavaCodeReferenceElement classRef;
      if (element instanceof PsiThisExpression) {
        classRef = ((PsiThisExpression)element).getQualifier();

      }
      else if (element instanceof PsiSuperExpression) {
        classRef = ((PsiSuperExpression)element).getQualifier();
      }
      else {
        classRef = ((PsiNewExpression)element).getClassReference();
      }
      if (classRef != null) {
        PsiElement resolved = classRef.resolve();
        if (resolved instanceof PsiClass && isPartOf((PsiClass)resolved, containingClass)) {
          classRefs.add(new InternalUsageInfo(element, containingClass));
        }

      }
      else if (!(element instanceof PsiNewExpression) && !RefactoringUtil.isInsideAnonymous(element, containingClass)) {
        classRefs.add(new InternalUsageInfo(element, containingClass));
      }
    }

    PsiElement[] children = element.getChildren();
    for (PsiElement child : children) {
      addClassRefs(originalMember, classRefs, containingClass, child, includeSelf);
    }
  }


  private static boolean isPartOf(PsiClass elementClass, PsiClass containingClass) {
    while(elementClass != null) {
      if (InheritanceUtil.isInheritorOrSelf(containingClass, elementClass, true)) return true;
      if (elementClass.hasModifierProperty(PsiModifier.STATIC)) return false;
      elementClass = elementClass.getContainingClass();
    }

    return false;
  }

  public static boolean buildVariableData(PsiTypeParameterListOwner member, ArrayList<ParameterTablePanel.VariableData> result) {
    final InternalUsageInfo[] classRefsInMethod = findClassRefsInMember(member, false);
    return collectVariableData(member, classRefsInMethod, result);
  }

  public static boolean collectVariableData(PsiMember member, InternalUsageInfo[] internalUsages,
                                             ArrayList<ParameterTablePanel.VariableData> variableDatum) {
    HashSet<PsiField> reported = new HashSet<PsiField>();
    HashSet<PsiField> accessedForWriting = new HashSet<PsiField>();
    boolean needClassParameter = false;
    for (InternalUsageInfo usage : internalUsages) {
      final PsiElement referencedElement = usage.getReferencedElement();
      if (usage.isWriting()) {
        accessedForWriting.add((PsiField)referencedElement);
        needClassParameter = true;
      }
      else if (referencedElement instanceof PsiField) {
        PsiField field = (PsiField)referencedElement;
        reported.add(field);
      }
      else {
        needClassParameter = true;
      }
    }

    final ArrayList<PsiField> psiFields = new ArrayList<PsiField>(reported);
    Collections.sort(psiFields, new Comparator<PsiField>() {
      public int compare(PsiField psiField, PsiField psiField1) {
        return psiField.getName().compareTo(psiField1.getName());
      }
    });
    for (final PsiField field : psiFields) {
      if (accessedForWriting.contains(field)) continue;
      ParameterTablePanel.VariableData data = new ParameterTablePanel.VariableData(field);
      CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(member.getProject());
      String name = field.getName();
      name = codeStyleManager.variableNameToPropertyName(name, VariableKind.FIELD);
      name = codeStyleManager.propertyNameToVariableName(name, VariableKind.PARAMETER);
      name = RefactoringUtil.suggestUniqueVariableName(name, member, field);
      data.name = name;
      data.passAsParameter = true;
      variableDatum.add(data);
    }
    return needClassParameter;
  }
}
