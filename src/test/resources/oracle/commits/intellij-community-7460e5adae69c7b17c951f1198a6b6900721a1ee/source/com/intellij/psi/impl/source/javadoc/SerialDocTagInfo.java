package com.intellij.psi.impl.source.javadoc;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiReference;
import com.intellij.psi.javadoc.JavadocTagInfo;
import com.intellij.psi.javadoc.PsiDocTagValue;
import com.intellij.util.ArrayUtil;

public class SerialDocTagInfo implements JavadocTagInfo {
  public String getName() {
    return "serial";
  }

  public boolean isInline() {
    return false;
  }

  public boolean isValidInContext(PsiElement element) {
    return element instanceof PsiClass || element instanceof PsiField;
  }

  public Object[] getPossibleValues(PsiElement context, PsiElement place, String prefix) {
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  public String checkTagValue(PsiDocTagValue value) {
    return null;
  }

  public PsiReference getReference(PsiDocTagValue value) {
    return null;
  }
}
