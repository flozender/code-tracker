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
package com.intellij.openapi.ui;

import javax.swing.*;

/**
 * author: lesya
 */
public class InputException extends RuntimeException{
  private final String myMessage;
  private final JComponent myComponent;

  public InputException(String message, JComponent component) {
    myMessage = message;
    myComponent = component;
  }

  public void show(){
    Messages.showMessageDialog(myMessage, "Input Error", Messages.getErrorIcon());
    myComponent.requestFocus();
  }
}
