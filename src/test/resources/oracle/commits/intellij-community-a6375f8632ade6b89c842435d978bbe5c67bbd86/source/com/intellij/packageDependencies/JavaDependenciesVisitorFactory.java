/*
 * User: anna
 * Date: 21-Jan-2008
 */
package com.intellij.packageDependencies;

import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiUtil;

public class JavaDependenciesVisitorFactory extends DependenciesVisitorFactory {
  @Override
  public PsiElementVisitor createVisitor(final DependenciesBuilder.DependencyProcessor processor) {
    return new JavaRecursiveElementWalkingVisitor() {

      @Override
      public void visitReferenceExpression(PsiReferenceExpression expression) {
        visitReferenceElement(expression);
      }

      @Override
      public void visitElement(PsiElement element) {
        super.visitElement(element);
        PsiReference[] refs = element.getReferences();
        for (PsiReference ref : refs) {
          PsiElement resolved = ref.resolve();
          if (resolved != null) {
            processor.process(ref.getElement(), resolved);
          }
        }
      }

      @Override
      public void visitLiteralExpression(PsiLiteralExpression expression) {
        // empty
        // TODO: thus we'll skip property references and references to file resources. We can't validate them anyway now since
        // TODO: rule syntax does not allow this.
      }

      @Override
      public void visitDocComment(PsiDocComment comment) {
        //empty
      }

      @Override
      public void visitImportStatement(PsiImportStatement statement) {
        if (!DependencyValidationManager.getInstance(statement.getProject()).skipImportStatements()) {
          visitElement(statement);
        }
      }

      @Override
      public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);
        PsiMethod psiMethod = expression.resolveMethod();
        if (psiMethod != null) {
          PsiType returnType = psiMethod.getReturnType();
          if (returnType != null) {
            PsiClass psiClass = PsiUtil.resolveClassInType(returnType);
            if (psiClass != null) {
              processor.process(expression, psiClass);
            }
          }
        }
      }
    };
  }
}