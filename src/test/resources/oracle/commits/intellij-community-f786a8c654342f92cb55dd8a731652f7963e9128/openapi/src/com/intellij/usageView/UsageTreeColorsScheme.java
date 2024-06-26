/*
 * Copyright 2000-2005 JetBrains s.r.o.
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
package com.intellij.usageView;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

public class UsageTreeColorsScheme implements ApplicationComponent, JDOMExternalizable{
  private EditorColorsScheme myColorsScheme;
  private EditorColorsManager myEditorColorsManager;
  @NonNls public static final String DEFAULT_SCHEME_NAME = "Default";

  public UsageTreeColorsScheme(EditorColorsManager editorColorsManager) {
    myEditorColorsManager = editorColorsManager;
  }

  public static UsageTreeColorsScheme getInstance() {
    return ApplicationManager.getApplication().getComponent(UsageTreeColorsScheme.class);
  }

  public String getComponentName() {
    return "FindViewColorsScheme";
  }

  public EditorColorsScheme getScheme() {
    return myColorsScheme;
  }

  public void initComponent() {
  }

  public void disposeComponent() {
  }

  public void readExternal(Element element) throws InvalidDataException {
    if (myColorsScheme == null){
      myColorsScheme = (EditorColorsScheme) myEditorColorsManager.getScheme(DEFAULT_SCHEME_NAME).clone();
    }
    myColorsScheme.readExternal(element);
  }

  public void writeExternal(Element element) throws WriteExternalException {
    throw new WriteExternalException();
  }
}
