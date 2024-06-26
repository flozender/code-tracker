package com.intellij.ide.structureView.impl.java;

import com.intellij.codeInsight.CodeInsightColors;
import com.intellij.ide.util.treeView.smartTree.Group;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.*;
import com.intellij.psi.util.PropertyUtil;
import com.intellij.psi.util.PsiUtil;

import javax.swing.*;
import java.util.Collection;
import java.util.ArrayList;

public class PropertyGroup implements Group, ItemPresentation, AccessLevelProvider {
  private final String myPropertyName;
  private final PsiType myPropertyType;

  private SmartPsiElementPointer myFieldPointer;
  private SmartPsiElementPointer myGetterPointer;
  private SmartPsiElementPointer mySetterPointer;
  private boolean myIsStatic;
  public static final Icon PROPERTY_READ_ICON = loadIcon("/nodes/propertyRead.png");
  public static final Icon PROPERTY_READ_STATIC_ICON = loadIcon("/nodes/propertyReadStatic.png");
  public static final Icon PROPERTY_WRITE_ICON = loadIcon("/nodes/propertyWrite.png");
  public static final Icon PROPERTY_WRITE_STATIC_ICON = loadIcon("/nodes/propertyWriteStatic.png");
  public static final Icon PROPERTY_READ_WRITE_ICON = loadIcon("/nodes/propertyReadWrite.png");
  public static final Icon PROPERTY_READ_WRITE_STATIC_ICON = loadIcon("/nodes/propertyReadWriteStatic.png");
  private final Project myProject;
  private final Collection<TreeElement> myChildren = new ArrayList<TreeElement>();

  private PropertyGroup(String propertyName, PsiType propertyType, boolean isStatic, Project project) {
    myPropertyName = propertyName;
    myPropertyType = propertyType;
    myIsStatic = isStatic;
    myProject = project;
  }

  public static final PropertyGroup createOn(PsiElement object, final TreeElement treeElement) {
    if (object instanceof PsiField) {
      PsiField field = (PsiField)object;
      PropertyGroup group = new PropertyGroup(PropertyUtil.suggestPropertyName(field.getProject(), field), field.getType(),
                                              field.hasModifierProperty(PsiModifier.STATIC), object.getProject());
      group.setField(field);
      group.myChildren.add(treeElement);
      return group;
    }
    else if (object instanceof PsiMethod) {
      PsiMethod method = (PsiMethod)object;
      if (PropertyUtil.isSimplePropertyGetter(method)) {
        PropertyGroup group = new PropertyGroup(PropertyUtil.getPropertyNameByGetter(method), method.getReturnType(),
                                                method.hasModifierProperty(PsiModifier.STATIC), object.getProject());
        group.setGetter(method);
        group.myChildren.add(treeElement);
        return group;
      }
      else if (PropertyUtil.isSimplePropertySetter(method)) {
        PropertyGroup group =
          new PropertyGroup(PropertyUtil.getPropertyNameBySetter(method), method.getParameterList().getParameters()[0].getType(),
                            method.hasModifierProperty(PsiModifier.STATIC), object.getProject());
        group.setSetter(method);
        group.myChildren.add(treeElement);
        return group;
      }
    }
    return null;
  }

  public Collection<TreeElement> getChildren() {
    return myChildren;
  }

  public ItemPresentation getPresentation() {
    return this;
  }

  public Icon getIcon(boolean open) {
    if (isStatic()) {
      if (getGetter() != null && getSetter() != null) {
        return PROPERTY_READ_WRITE_STATIC_ICON;
      } else if (getGetter() != null) {
        return PROPERTY_READ_STATIC_ICON;
      } else {
        return PROPERTY_WRITE_STATIC_ICON;
      }
    } else {
      if (getGetter() != null && getSetter() != null) {
        return PROPERTY_READ_WRITE_ICON;
      } else if (getGetter() != null) {
        return PROPERTY_READ_ICON;
      } else {
        return PROPERTY_WRITE_ICON;
      }
    }

  }

  private boolean isStatic() {
    return myIsStatic;
  }

  public String getLocationString() {
    return null;
  }

  public String getPresentableText() {
    return myPropertyName + ":" + myPropertyType.getPresentableText();
  }

  public String toString() {
    return myPropertyName;
  }


  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PropertyGroup)) return false;

    final PropertyGroup propertyGroup = (PropertyGroup)o;

    if (myPropertyName != null ? !myPropertyName.equals(propertyGroup.myPropertyName) : propertyGroup.myPropertyName != null) return false;
    if (myPropertyType != null ? !myPropertyType.equals(propertyGroup.myPropertyType) : propertyGroup.myPropertyType != null) return false;
    return true;
  }



  public int hashCode() {
    int result;
    result = myPropertyName != null?myPropertyName.hashCode():0;
    result = 29 * result + (myPropertyType != null ? myPropertyType.hashCode() : 0);
    return result;
  }


  public String getGetterName() {
    return PropertyUtil.suggestGetterName(myPropertyName, myPropertyType);
  }

  public int getAccessLevel() {
    int result = PsiUtil.ACCESS_LEVEL_PRIVATE;
    if (getGetter() != null) {
      result = Math.max(result, PsiUtil.getAccessLevel(getGetter().getModifierList()));
    }
    if (getSetter() != null) {
      result = Math.max(result, PsiUtil.getAccessLevel(getSetter().getModifierList()));
    }
    if (getField() != null) {
      result = Math.max(result, PsiUtil.getAccessLevel(getField().getModifierList()));
    }
    return result;
  }

  public int getSubLevel() {
    return 0;
  }

  public void setField(PsiField field) {
    myFieldPointer = SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(field);
    myIsStatic &= field.hasModifierProperty(PsiModifier.STATIC);
  }

  public void setGetter(PsiMethod getter) {
    myGetterPointer = SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(getter);
    myIsStatic &= getter.hasModifierProperty(PsiModifier.STATIC);
  }

  public void setSetter(PsiMethod setter) {
    mySetterPointer = SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(setter);
    myIsStatic &= setter.hasModifierProperty(PsiModifier.STATIC);
  }

  public PsiField getField() {
    return (PsiField)(myFieldPointer == null ? null : myFieldPointer.getElement());
  }

  public PsiMethod getGetter() {
    return (PsiMethod)(myGetterPointer == null ? null : myGetterPointer.getElement());
  }

  public PsiMethod getSetter() {
    return (PsiMethod)(mySetterPointer == null ? null : mySetterPointer.getElement());
  }

  void copyAccessorsFrom(PropertyGroup group) {
    if (group.getGetter() != null) setGetter(group.getGetter());
    if (group.getSetter() != null) setSetter(group.getSetter());
    if (group.getField() != null) setField(group.getField());
    myChildren.addAll(group.myChildren);
  }

  private static Icon loadIcon(String resourceName) {
    Icon icon = IconLoader.findIcon(resourceName);
    Application application = ApplicationManager.getApplication();
    if (icon == null && application != null && application.isUnitTestMode()) {
      return new ImageIcon();
    }
    return icon;
  }

  public TextAttributesKey getTextAttributesKey() {
    return isDeprecated() ? CodeInsightColors.DEPRECATED_ATTRIBUTES : null;
  }

  private boolean isDeprecated() {
    return isDeprecated(getField()) && isDeprecated(getGetter()) && isDeprecated(getSetter());
  }

  private static boolean isDeprecated(final PsiElement element) {
    if (element == null) return false;
    if (!element.isValid()) return false;
    if (!(element instanceof PsiDocCommentOwner)) return false;
    return ((PsiDocCommentOwner)element).isDeprecated();
  }

  public boolean isComplete() {
    return getGetter() != null || getSetter() != null;
  }

  public Object getValue() {
    return this;
  }
}
