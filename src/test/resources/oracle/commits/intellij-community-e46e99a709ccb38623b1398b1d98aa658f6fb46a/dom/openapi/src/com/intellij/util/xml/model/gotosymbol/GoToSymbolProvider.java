/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.util.xml.model.gotosymbol;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.xml.XmlElement;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.ElementPresentationManager;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class for "Go To Symbol" contributors.
 */
public abstract class GoToSymbolProvider implements ChooseByNameContributor {

  protected abstract void addNames(@NotNull Module module, Set<String> result);

  protected abstract void addItems(@NotNull Module module, String name, List<NavigationItem> result);

  protected abstract boolean acceptModule(final Module module);

  protected static void addNewNames(@NotNull final List<? extends DomElement> elements, final Set<String> existingNames) {
    for (DomElement name : elements) {
      existingNames.add(name.getGenericInfo().getElementName(name));
    }
  }

  public String[] getNames(final Project project, boolean includeNonProjectItems) {
    Set<String> result = new HashSet<String>();
    Module[] modules = ModuleManager.getInstance(project).getModules();
    for (Module module : modules) {
      if (acceptModule(module)) {
        addNames(module, result);
      }
    }

    return result.toArray(new String[result.size()]);
  }

  public NavigationItem[] getItemsByName(final String name, final String pattern, final Project project, boolean includeNonProjectItems) {
    List<NavigationItem> result = new ArrayList<NavigationItem>();
    Module[] modules = ModuleManager.getInstance(project).getModules();
    for (Module module : modules) {
      if (acceptModule(module)) {
        addItems(module, name, result);
      }
    }

    return result.toArray(new NavigationItem[result.size()]);
  }

  @Nullable
  protected static NavigationItem createNavigationItem(final DomElement domElement) {
    final GenericDomValue name = domElement.getGenericInfo().getNameDomElement(domElement);
    assert name != null;
    final XmlElement psiElement = name.getXmlElement();
    final String value = name.getStringValue();
    if (psiElement == null || value == null) {
      return null;
    }
    final Icon icon = ElementPresentationManager.getIcon(domElement);
    return createNavigationItem(psiElement, value, icon);
  }

  @NotNull
  protected static NavigationItem createNavigationItem(@NotNull final PsiElement element,
                                                       @NotNull @NonNls final String text,
                                                       @Nullable final Icon icon) {
    return new BaseNavigationItem(element, text, icon);
  }


  /**
   * Wraps one entry to display in "Go To Symbol" dialog.
   */
  protected static class BaseNavigationItem extends FakePsiElement {

    private final PsiElement myPsiElement;
    private final String myText;
    private final Icon myIcon;

    /**
     * Creates a new display item.
     *
     * @param psiElement The PsiElement to navigate to.
     * @param text       Text to show for this element.
     * @param icon       Icon to show for this element.
     */
    protected BaseNavigationItem(@NotNull PsiElement psiElement, @NotNull @NonNls String text, @Nullable Icon icon) {
      myPsiElement = psiElement;
      myText = text;
      myIcon = icon;
    }

    public PsiElement getNavigationElement() {
      return myPsiElement;
    }

    public Icon getIcon(boolean flags) {
      return myIcon;
    }

    public ItemPresentation getPresentation() {
      return new ItemPresentation() {

        public String getPresentableText() {
          return myText;
        }

        @Nullable
        public String getLocationString() {
          return '(' + myPsiElement.getContainingFile().getName() + ')';
        }

        @Nullable
        public Icon getIcon(boolean open) {
          return myIcon;
        }

        @Nullable
        public TextAttributesKey getTextAttributesKey() {
          return null;
        }
      };
    }

    public PsiElement getParent() {
      return myPsiElement.getParent();
    }

    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final BaseNavigationItem that = (BaseNavigationItem)o;

      if (myPsiElement != null ? !myPsiElement.equals(that.myPsiElement) : that.myPsiElement != null) return false;
      if (myText != null ? !myText.equals(that.myText) : that.myText != null) return false;

      return true;
    }

    public int hashCode() {
      int result;
      result = (myPsiElement != null ? myPsiElement.hashCode() : 0);
      result = 31 * result + (myText != null ? myText.hashCode() : 0);
      return result;
    }
  }

}