package com.intellij.find.findUsages;

import com.intellij.psi.*;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.testFramework.PsiTestCase;
import junit.framework.Assert;

/**
 *  @author dsl
 */
public class FindParameterTest extends PsiTestCase {
  public void testMethod() throws Exception {
    String text =
            "void method(final int i) {" +
            "  Runnable runnable = new Runnable() {" +
            "    public void run() {" +
            "      System.out.println(i);" +
            "    }" +
            "  };" +
            "  System.out.println(i);" +
            "}";
    final PsiManager psiManager = PsiManager.getInstance(myProject);
    final PsiElementFactory elementFactory = psiManager.getElementFactory();
    final PsiMethod methodFromText = elementFactory.createMethodFromText(text, null);
    final PsiParameter[] parameters = methodFromText.getParameterList().getParameters();
    final PsiReference[] references = psiManager.getSearchHelper().findReferences(parameters[0], new LocalSearchScope(methodFromText), false);
    Assert.assertEquals(references.length, 2);
  }
}
