package com.intellij.debugger.ui;

import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.engine.evaluation.CodeFragmentFactory;
import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.impl.PositionUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: lex
 * Date: Apr 12, 2004
 * Time: 2:46:02 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class DebuggerEditorImpl extends CompletionEditor{
  private static final Logger LOG = Logger.getInstance("#com.intellij.debugger.ui.DebuggerEditorImpl");

  public static final char SEPARATOR = 13;

  private final Project myProject;
  private PsiElement myContext;

  private final String myRecentsId;

  private final List<DocumentListener> myDocumentListeners = new ArrayList<DocumentListener>();
  private Document myCurrentDocument;

  private final PsiTreeChangeListener myPsiListener = new PsiTreeChangeAdapter() {
    public void childRemoved(PsiTreeChangeEvent event) {
      checkContext();
    }
    public void childReplaced(PsiTreeChangeEvent event) {
      checkContext();
    }
    public void childMoved(PsiTreeChangeEvent event) {
      checkContext();
    }
    private void checkContext() {
      final PsiElement contextElement = getContext();
      if(contextElement == null || !contextElement.isValid()) {
        final DebuggerContextImpl context = DebuggerManagerEx.getInstanceEx(myProject).getContextManager().getContext();
        final PsiElement newContextElement = PositionUtil.getContextElement(context);
        setContext(newContextElement != null && newContextElement.isValid()? newContextElement : null);
      }
    }
  };
  protected CodeFragmentFactory myFactory;

  public DebuggerEditorImpl(Project project, PsiElement context, String recentsId, final CodeFragmentFactory factory) {
    myProject = project;
    myContext = context;
    myRecentsId = recentsId;
    PsiManager.getInstance(project).addPsiTreeChangeListener(myPsiListener);
    myFactory = factory;
  }

  protected TextWithImports createItem(Document document, Project project) {
    if (document != null) {
      PsiDocumentManager.getInstance(project).commitDocument(document);
      PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
      return createText(psiFile.getText(), ((JavaCodeFragment)psiFile).importsToString());
    }

    return createText("");
  }

  protected TextWithImports createText(String text) {
    return createText(text, "");
  }

  protected abstract TextWithImports createText(String text, String importsString);

  public abstract JComponent getPreferredFocusedComponent();

  public void setContext(PsiElement context) {
    TextWithImports text = getText();
    myContext = context;
    setText(text);
  }

  public PsiElement getContext() {
    return myContext;
  }

  protected Project getProject() {
    return myProject;
  }

  public void requestFocus() {
    getPreferredFocusedComponent().requestFocus();
  }

  @Nullable
  protected Document createDocument(TextWithImports item) {
    LOG.assertTrue(myContext == null || myContext.isValid());

    if(item == null) {
      item = createText("");
    }
    JavaCodeFragment codeFragment = myFactory.createPresentationCodeFragment(item, myContext, getProject());
    codeFragment.forceResolveScope(GlobalSearchScope.allScope(myProject));
    if (myContext != null) {
      final PsiClass contextClass = PsiTreeUtil.getNonStrictParentOfType(myContext, PsiClass.class);
      if (contextClass != null) {
        final PsiClassType contextType = JavaPsiFacade.getInstance(codeFragment.getProject()).getElementFactory().createType(contextClass);
        codeFragment.setThisType(contextType);
      }
    }

    if(myCurrentDocument != null) {
      for (DocumentListener documentListener : myDocumentListeners) {
        myCurrentDocument.removeDocumentListener(documentListener);
      }
    }

    myCurrentDocument = PsiDocumentManager.getInstance(getProject()).getDocument(codeFragment);

    if (myCurrentDocument != null) {
      for (DocumentListener documentListener : myDocumentListeners) {
        myCurrentDocument.addDocumentListener(documentListener);
      }
    }

    return myCurrentDocument;
  }

  public String getRecentsId() {
    return myRecentsId;
  }

  public void addRecent(TextWithImports text) {
    if(getRecentsId() != null && text != null && !"".equals(text.getText())){
      DebuggerRecents.getInstance(getProject()).addRecent(getRecentsId(), text);
    }
  }

  public void addDocumentListener(DocumentListener listener) {
    myDocumentListeners.add(listener);
    if(myCurrentDocument != null) {
      myCurrentDocument.addDocumentListener(listener);
    }
  }

  public void removeDocumentListener(DocumentListener listener) {
    myDocumentListeners.remove(listener);
    if(myCurrentDocument != null) {
      myCurrentDocument.removeDocumentListener(listener);
    }
  }

  public void dispose() {
    PsiManager.getInstance(myProject).removePsiTreeChangeListener(myPsiListener);
  }

  public void setFactory(final CodeFragmentFactory factory) {
    myFactory = factory;
  }

  public void revalidate() {
    setText(getText());
  }
}
