package com.intellij.ide.projectView.impl.nodes;

import com.intellij.coverage.CoverageDataManager;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.PsiClassChildrenSource;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.Collection;

public class ClassTreeNode extends BasePsiNode<PsiClass>{
  public ClassTreeNode(Project project, PsiClass value, ViewSettings viewSettings) {
    super(project, value, viewSettings);
  }

  public Collection<AbstractTreeNode> getChildrenImpl() {
    PsiClass parent = getValue();
    final ArrayList<AbstractTreeNode> treeNodes = new ArrayList<AbstractTreeNode>();

    if (getSettings().isShowMembers()) {
      ArrayList<PsiElement> result = new ArrayList<PsiElement>();
      PsiClassChildrenSource.DEFAULT_CHILDREN.addChildren(parent, result);
      for (PsiElement psiElement : result) {
        psiElement.accept(new JavaElementVisitor() {
          @Override public void visitClass(PsiClass aClass) {
            treeNodes.add(new ClassTreeNode(getProject(), aClass, getSettings()));
          }

          @Override public void visitMethod(PsiMethod method) {
            treeNodes.add(new PsiMethodNode(getProject(), method, getSettings()));
          }

          @Override public void visitField(PsiField field) {
            treeNodes.add(new PsiFieldNode(getProject(), field, getSettings()));
          }

          @Override public void visitReferenceExpression(PsiReferenceExpression expression) {
            visitExpression(expression);
          }
        });
      }
    }
    return treeNodes;
  }

  public void updateImpl(PresentationData data) {
    final PsiClass aClass = getValue();
    if (aClass != null) {
      data.setPresentableText(aClass.getName());
      final String qName = aClass.getQualifiedName();
      if (qName != null) {
        final CoverageDataManager coverageManager = CoverageDataManager.getInstance(aClass.getProject());
        final String coverageString = coverageManager.getClassCoverageInformationString(qName);
        data.setLocationString(coverageString);
      }
    }
  }

  public boolean isTopLevel() {
    return getValue() != null && getValue().getParent()instanceof PsiFile;
  }


  public boolean expandOnDoubleClick() {
    return false;
  }

  public PsiClass getPsiClass() {
    return getValue();
  }

  public boolean isAlwaysExpand() {
    return getParentValue() instanceof PsiFile;
  }

  public int getWeight() {
    return 20;
  }
}
