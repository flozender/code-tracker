package com.intellij.psi.filters.position;

import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.filters.ElementFilter;

/**
 * Created by IntelliJ IDEA.
 * User: ik
 * Date: 03.02.2003
 * Time: 18:29:13
 * To change this template use Options | File Templates.
 */
public class RootTagFilter extends PositionElementFilter{
  public RootTagFilter(ElementFilter filter){
    setFilter(filter);
  }

  public RootTagFilter(){}
  public boolean isAcceptable(Object element, PsiElement scope){
    if (!(element instanceof XmlDocument)) return false;
    final XmlTag rootTag = ((XmlDocument)element).getRootTag();
    if(rootTag == null) return false;

    return getFilter().isAcceptable(rootTag, (PsiElement)element);
  }

  public String toString(){
    return "roottag(" + getFilter().toString() + ")";
  }
}
