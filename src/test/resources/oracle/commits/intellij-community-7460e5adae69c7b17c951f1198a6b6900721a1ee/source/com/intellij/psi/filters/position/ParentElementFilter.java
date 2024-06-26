package com.intellij.psi.filters.position;

import com.intellij.psi.PsiElement;
import com.intellij.psi.filters.ElementFilter;

/**
 * Created by IntelliJ IDEA.
 * User: ik
 * Date: 03.02.2003
 * Time: 18:54:57
 * To change this template use Options | File Templates.
 */
public class ParentElementFilter extends PositionElementFilter{
  private PsiElement myParent = null;
  private int myLevel = 1;
  public ParentElementFilter(ElementFilter filter){
    setFilter(filter);
  }

  public ParentElementFilter(ElementFilter filter, int level) {
    setFilter(filter);
    myLevel = level;
  }

  public ParentElementFilter(PsiElement parent){
    myParent = parent;
  }


  public ParentElementFilter(){}

  public boolean isAcceptable(Object element, PsiElement scope){
    if (!(element instanceof PsiElement)) return false;
    PsiElement context = (PsiElement)element;
    for(int i = 0; i < myLevel && context != null; i++){
       context = context.getContext();
    }
    if(context != null){
      if(myParent == null){
        return getFilter().isAcceptable(context, scope);
      }
      return myParent == context;
    }
    return false;
  }


  public String toString(){
    return "parent(" +getFilter()+")";
  }

}
