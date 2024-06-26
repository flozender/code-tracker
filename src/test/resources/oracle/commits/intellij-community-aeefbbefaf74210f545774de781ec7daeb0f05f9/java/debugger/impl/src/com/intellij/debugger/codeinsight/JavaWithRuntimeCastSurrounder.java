package com.intellij.debugger.codeinsight;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.generation.surroundWith.JavaExpressionSurrounder;
import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.DebuggerInvocationUtil;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.ui.DebuggerExpressionComboBox;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressWindowWithNotification;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nullable;

/**
 * User: lex
 * Date: Jul 17, 2003
 * Time: 7:51:01 PM
 */
public class JavaWithRuntimeCastSurrounder extends JavaExpressionSurrounder {
  private static final Logger LOG = Logger.getInstance("#com.intellij.debugger.codeinsight.SurroundWithRuntimeCastHandler");

  public String getTemplateDescription() {
    return CodeInsightBundle.message("surround.with.runtime.type.template");
  }

  public boolean isApplicable(PsiExpression expr) {
    PsiFile file = expr.getContainingFile();
    if (!(file instanceof PsiCodeFragment)) return false;
    if (file.getUserData(DebuggerExpressionComboBox.KEY) == null) {
      return false;
    }

    return RuntimeTypeEvaluator.isSubtypeable(expr);
  }

  public TextRange surroundExpression(Project project, Editor editor, PsiExpression expr) throws IncorrectOperationException {
    DebuggerContextImpl debuggerContext = (DebuggerManagerEx.getInstanceEx(project)).getContext();
    DebuggerSession debuggerSession = debuggerContext.getDebuggerSession();
    if (debuggerSession != null) {
      final ProgressWindowWithNotification progressWindow = new ProgressWindowWithNotification(true, expr.getProject());
      SurroundWithCastWorker worker = new SurroundWithCastWorker(editor, expr, debuggerContext, progressWindow);
      progressWindow.setTitle(DebuggerBundle.message("title.evaluating"));
      debuggerContext.getDebugProcess().getManagerThread().startProgress(worker, progressWindow);
    }
    return null;
  }

  private class SurroundWithCastWorker extends RuntimeTypeEvaluator {
    private final Editor myEditor;

    public SurroundWithCastWorker(Editor editor, PsiExpression expression, DebuggerContextImpl context, final ProgressIndicator indicator) {
      super(editor, expression, context, indicator);
      myEditor = editor;
    }

    @Override
    protected void typeCalculationFinished(@Nullable final String type) {
      if (type == null) {
        return;
      }

      hold();
      final Project project = myElement.getProject();
      DebuggerInvocationUtil.invokeLater(project, new Runnable() {
        public void run() {
          new WriteCommandAction(project, CodeInsightBundle.message("command.name.surround.with.runtime.cast")) {
            protected void run(Result result) throws Throwable {
              try {
                LOG.assertTrue(type != null);

                PsiElementFactory factory = JavaPsiFacade.getInstance(myElement.getProject()).getElementFactory();
                PsiParenthesizedExpression parenth =
                  (PsiParenthesizedExpression)factory.createExpressionFromText("((" + type + ")expr)", null);
                PsiTypeCastExpression cast = (PsiTypeCastExpression)parenth.getExpression();
                cast.getOperand().replace(myElement);
                parenth = (PsiParenthesizedExpression)JavaCodeStyleManager.getInstance(project).shortenClassReferences(parenth);
                PsiExpression expr = (PsiExpression)myElement.replace(parenth);
                TextRange range = expr.getTextRange();
                myEditor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
                myEditor.getCaretModel().moveToOffset(range.getEndOffset());
                myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
              }
              catch (IncorrectOperationException e) {
                // OK here. Can be caused by invalid type like one for proxy starts with . '.Proxy34'
              }
              finally {
                release();
              }
            }
          }.execute();
        }
      }, myProgressIndicator.getModalityState());
    }

  }
}
