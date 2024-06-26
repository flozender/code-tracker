package com.intellij.codeInsight.hint;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.LightweightHint;

import java.awt.*;
import java.lang.ref.WeakReference;

public class ShowContainerInfoHandler implements CodeInsightActionHandler {
  private static final Key MY_LAST_HINT_KEY = Key.create("MY_LAST_HINT_KEY");
  private static final Key CONTAINER_KEY = Key.create("CONTAINER_KEY");

  public void invoke(final Project project, final Editor editor, PsiFile file) {
    PsiDocumentManager.getInstance(project).commitAllDocuments();

    PsiElement container = null;
    WeakReference ref = (WeakReference)editor.getUserData(MY_LAST_HINT_KEY);
    if (ref != null){
      LightweightHint hint = (LightweightHint)ref.get();
      if (hint != null && hint.isVisible()){
        hint.hide();
        container = (PsiElement)hint.getUserData(CONTAINER_KEY);
        if (!container.isValid()){
          container = null;
        }
      }
    }

    if (container == null){
      int offset = editor.getCaretModel().getOffset();
      container = file.findElementAt(offset);
      if (container == null) return;
    }

    while(true){
      container = findContainer(container);
      if (container == null) return;
      if (!isDeclarationVisible(container, editor)) break;
    }

    final TextRange range = EditorFragmentComponent.getDeclarationRange(container);
    final PsiElement _container = container;
    ApplicationManager.getApplication().invokeLater(new Runnable() {
        public void run() {
          LightweightHint hint = EditorFragmentComponent.showEditorFragmentHint(editor, range, true);
          hint.putUserData(CONTAINER_KEY, _container);
          editor.putUserData(MY_LAST_HINT_KEY, new WeakReference(hint));
        }
      });
  }

  public boolean startInWriteAction() {
    return false;
  }

  private PsiElement findContainer(PsiElement element) {
    PsiElement container = element.getParent();
    while(true){
      if (container instanceof PsiFile) return null;
      if (container instanceof PsiMethod || container instanceof PsiClass || container instanceof PsiClassInitializer) break;
      if (container instanceof XmlTag) break;
      container = container.getParent();
    }
    return container;
  }

  private boolean isDeclarationVisible(PsiElement container, Editor editor) {
    Rectangle viewRect = editor.getScrollingModel().getVisibleArea();
    TextRange range = EditorFragmentComponent.getDeclarationRange(container);
    LogicalPosition pos = editor.offsetToLogicalPosition(range.getStartOffset());
    Point loc = editor.logicalPositionToXY(pos);
    return loc.y >= viewRect.y;
  }
}