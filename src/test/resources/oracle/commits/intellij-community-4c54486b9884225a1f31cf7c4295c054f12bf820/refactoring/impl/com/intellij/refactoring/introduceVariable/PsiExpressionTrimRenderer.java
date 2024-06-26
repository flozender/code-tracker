/*
 * User: anna
 * Date: 28-Oct-2008
 */
package com.intellij.refactoring.introduceVariable;

import com.intellij.psi.*;

class PsiExpressionTrimRenderer extends JavaRecursiveElementVisitor {
  private final StringBuffer myBuf;

  public PsiExpressionTrimRenderer(final StringBuffer buf) {
    myBuf = buf;
  }

  @Override
  public void visitExpression(final PsiExpression expression) {
    myBuf.append(expression.getText());
  }

  @Override
  public void visitInstanceOfExpression(final PsiInstanceOfExpression expression) {
    expression.getOperand().accept(this);
    myBuf.append(" ").append(PsiKeyword.INSTANCEOF).append(" ");
    final PsiTypeElement checkType = expression.getCheckType();
    if (checkType != null) {
      myBuf.append(checkType.getText());
    }
  }

  @Override
  public void visitParenthesizedExpression(final PsiParenthesizedExpression expression) {
    myBuf.append("(");
    final PsiExpression expr = expression.getExpression();
    if (expr != null) {
      expr.accept(this);
    }
    myBuf.append(")");
  }

  @Override
  public void visitTypeCastExpression(final PsiTypeCastExpression expression) {
    final PsiTypeElement castType = expression.getCastType();
    if (castType != null) {
      myBuf.append("(").append(castType.getText()).append(")");
    }
    final PsiExpression operand = expression.getOperand();
    if (operand != null) {
      operand.accept(this);
    }
  }

  @Override
  public void visitArrayAccessExpression(final PsiArrayAccessExpression expression) {
    expression.getArrayExpression().accept(this);
    myBuf.append("[");
    final PsiExpression indexExpression = expression.getIndexExpression();
    if (indexExpression != null) {
      indexExpression.accept(this);
    }
    myBuf.append("]");
  }

  @Override
  public void visitPrefixExpression(final PsiPrefixExpression expression) {
    myBuf.append(expression.getOperationSign().getText());
    final PsiExpression operand = expression.getOperand();
    if (operand != null) {
      operand.accept(this);
    }
  }

  @Override
  public void visitPostfixExpression(final PsiPostfixExpression expression) {
    expression.getOperand().accept(this);
    myBuf.append(expression.getOperationSign().getText());
  }

  @Override
  public void visitBinaryExpression(final PsiBinaryExpression expression) {
    expression.getLOperand().accept(this);
    myBuf.append(" ").append(expression.getOperationSign().getText()).append(" ");
    final PsiExpression rOperand = expression.getROperand();
    if (rOperand != null) {
      rOperand.accept(this);
    }
  }

  @Override
  public void visitConditionalExpression(final PsiConditionalExpression expression) {
    expression.getCondition().accept(this);

    myBuf.append(" ? ");
    final PsiExpression thenExpression = expression.getThenExpression();
    if (thenExpression != null) {
      thenExpression.accept(this);
    }

    myBuf.append(" : ");
    final PsiExpression elseExpression = expression.getElseExpression();
    if (elseExpression != null) {
      elseExpression.accept(this);
    }
  }

  @Override
  public void visitAssignmentExpression(final PsiAssignmentExpression expression) {
    expression.getLExpression().accept(this);
    myBuf.append(expression.getOperationSign().getText());
    final PsiExpression rExpression = expression.getRExpression();
    if (rExpression != null) {
      rExpression.accept(this);
    }
  }

  @Override
  public void visitReferenceExpression(final PsiReferenceExpression expr) {
    final PsiExpression qualifierExpression = expr.getQualifierExpression();
    if (qualifierExpression != null) {
      qualifierExpression.accept(this);
      myBuf.append(".");
    }
    myBuf.append(expr.getReferenceName());

  }

  @Override
  public void visitMethodCallExpression(final PsiMethodCallExpression expr) {
    expr.getMethodExpression().accept(this);
    expr.getArgumentList().accept(this);
  }


  @Override
  public void visitArrayInitializerExpression(final PsiArrayInitializerExpression expression) {
    myBuf.append("{");
    boolean first = true;
    for (PsiExpression expr : expression.getInitializers()) {
      if (!first) {
        myBuf.append(", ");
      }
      first = false;
      expr.accept(this);
    }
    myBuf.append("}");
  }

  @Override
  public void visitExpressionList(final PsiExpressionList list) {
    final PsiExpression[] args = list.getExpressions();
    if (args.length > 0) {
      myBuf.append("(...)");
    }
    else {
      myBuf.append("()");
    }
  }

  @Override
  public void visitNewExpression(final PsiNewExpression expr) {
    final PsiAnonymousClass anonymousClass = expr.getAnonymousClass();

    final PsiExpressionList argumentList = expr.getArgumentList();

    if (anonymousClass != null) {
      myBuf.append(PsiKeyword.NEW).append(" ").append(anonymousClass.getBaseClassType().getPresentableText());
      if (argumentList != null) argumentList.accept(this);
      myBuf.append(" {...}");
    }
    else {
      final PsiJavaCodeReferenceElement reference = expr.getClassReference();
      if (reference != null) {
        myBuf.append(PsiKeyword.NEW).append(" ").append(reference.getText());

        final PsiExpression[] arrayDimensions = expr.getArrayDimensions();
        if (arrayDimensions.length > 0) myBuf.append("[");
        boolean first = true;
        for (PsiExpression dimension : arrayDimensions) {
          if (!first) myBuf.append(", ");
          first = false;
          dimension.accept(this);
        }
        if (arrayDimensions.length > 0) myBuf.append("]");

        if (argumentList != null) {
          argumentList.accept(this);
        }

        final PsiArrayInitializerExpression arrayInitializer = expr.getArrayInitializer();
        if (arrayInitializer != null) {
          arrayInitializer.accept(this);
        }
      }
      else {
        myBuf.append(expr.getText());
      }
    }
  }
}