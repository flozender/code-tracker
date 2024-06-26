package com.intellij.psi.impl.compiled;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.impl.ElementPresentationUtil;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.cache.RecordUtil;
import com.intellij.psi.impl.java.stubs.JavaStubElementTypes;
import com.intellij.psi.impl.java.stubs.PsiParameterStub;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.stubs.StubElement;
import com.intellij.ui.RowIcon;
import com.intellij.util.Icons;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;

public class ClsParameterImpl extends ClsRepositoryPsiElement<PsiParameterStub> implements PsiParameter {
  private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.compiled.ClsParameterImpl");

  private PsiTypeElement myType;        //guarded by PsiLock
  private String myMirrorName = null;   //guarded by PsiLock
  private String myName = null;         //no point guarding
  public static final ClsParameterImpl[] EMPTY_ARRAY = new ClsParameterImpl[0];

  public ClsParameterImpl(final PsiParameterStub stub) {
    super(stub);
  }

  public PsiIdentifier getNameIdentifier() {
    return null;
  }

  public String getName() {
    if (myName == null) {
      if (DumbService.getInstance().isDumb()) {
        return null;
      }
      
      ClsMethodImpl method = (ClsMethodImpl)getDeclarationScope();
      PsiMethod sourceMethod = (PsiMethod)method.getNavigationElement();
      if (sourceMethod == method) return null;
      myName = sourceMethod.getParameterList().getParameters()[getIndex()].getName();
    }
    return myName;
  }

  public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
    PsiImplUtil.setName(getNameIdentifier(), name);
    return this;
  }

  @NotNull
  public PsiTypeElement getTypeElement() {
    synchronized (LAZY_BUILT_LOCK) {
      if (myType == null) {
        myType = new ClsTypeElementImpl(this, RecordUtil.createTypeText(getStub().getParameterType()), ClsTypeElementImpl.VARIANCE_NONE);
      }
      return myType;
    }
  }

  @NotNull
  public PsiType getType() {
    return getTypeElement().getType();
  }

  @NotNull
  public PsiModifierList getModifierList() {
    final StubElement<PsiModifierList> child = getStub().findChildStubByType(JavaStubElementTypes.MODIFIER_LIST);
    assert child != null;
    return child.getPsi();
  }

  public boolean hasModifierProperty(@NotNull String name) {
    return getModifierList().hasModifierProperty(name);
  }

  public PsiExpression getInitializer() {
    return null;
  }

  public boolean hasInitializer() {
    return false;
  }

  public Object computeConstantValue() {
    return null;
  }

  public void normalizeDeclaration() throws IncorrectOperationException {
  }

  public void appendMirrorText(final int indentLevel, final StringBuffer buffer) {
    PsiAnnotation[] annotations = getAnnotations();
    for (PsiAnnotation annotation : annotations) {
      ((ClsAnnotationImpl)annotation).appendMirrorText(indentLevel, buffer);
      buffer.append(" ");
    }
    ((ClsElementImpl)getTypeElement()).appendMirrorText(indentLevel, buffer);
    buffer.append(" ");
    buffer.append(getMirrorName());
  }

  private String getMirrorName() {
    synchronized (LAZY_BUILT_LOCK) {
      if (myMirrorName == null) {
        PsiParameter[] parms = ((PsiParameterList) getParent()).getParameters();
        if (DumbService.getInstance().isDumb()) {
          return "p" + Arrays.asList(parms).indexOf(this);
        }

        @NonNls String name = getName();
        if (name != null) return name;

        String[] nameSuggestions = JavaCodeStyleManager.getInstance(getProject()).suggestVariableName(VariableKind.PARAMETER, null,
            null, getType())
            .names;
        name = "p";
        if (nameSuggestions.length > 0) {
          name = nameSuggestions[0];
        }

        AttemptsLoop:
        while (true) {
          for (PsiParameter parm : parms) {
            if (parm == this) break AttemptsLoop;
            String name1 = ((ClsParameterImpl) parm).getMirrorName();
            if (name.equals(name1)) {
              name = nextName(name);
              continue AttemptsLoop;
            }
          }
        }
        myMirrorName = name;
      }
      return myMirrorName;
    }
  }

  private static String nextName(String name) {
    int count = 0;
    while (true) {
      if (count == name.length()) break;
      char c = name.charAt(name.length() - count - 1);
      if ('0' <= c && c <= '9') {
        count++;
      }
      else {
        break;
      }
    }

    try {
      int n = count > 0 ? Integer.parseInt(name.substring(name.length() - count)) : 0;
      n++;
      return name.substring(0, name.length() - count) + n;
    }
    catch (NumberFormatException e) {
      LOG.assertTrue(false);
      return null;
    }
  }

  public void setMirror(@NotNull TreeElement element) {
    setMirrorCheckingType(element, null);

    PsiParameter mirror = (PsiParameter)SourceTreeToPsiMap.treeElementToPsi(element);
      ((ClsElementImpl)getModifierList()).setMirror((TreeElement)SourceTreeToPsiMap.psiElementToTree(mirror.getModifierList()));
      ((ClsElementImpl)getTypeElement()).setMirror((TreeElement)SourceTreeToPsiMap.psiElementToTree(mirror.getTypeElement()));
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JavaElementVisitor) {
      ((JavaElementVisitor)visitor).visitParameter(this);
    }
    else {
      visitor.visitElement(this);
    }
  }

  public String toString() {
    return "PsiParameter";
  }

  @NotNull
  public PsiElement getDeclarationScope() {
    // only method parameters exist in compiled code
    return getParent().getParent();
  }

  private int getIndex() {
    final PsiParameterStub stub = getStub();
    return stub.getParentStub().getChildrenStubs().indexOf(stub);
  }

  public boolean isVarArgs() {
    final PsiParameterList paramList = (PsiParameterList)getParent();
    final PsiMethod method = (PsiMethod)paramList.getParent();
    return method.isVarArgs() && getIndex() == paramList.getParametersCount() - 1;
  }

  @NotNull
  public PsiAnnotation[] getAnnotations() {
    return getModifierList().getAnnotations();
  }

  public Icon getElementIcon(final int flags) {
    final RowIcon baseIcon = createLayeredIcon(Icons.PARAMETER_ICON, 0);
    return ElementPresentationUtil.addVisibilityIcon(this, flags, baseIcon);
  }

  @NotNull
  public SearchScope getUseScope() {
    return new LocalSearchScope(getDeclarationScope());
  }
}
