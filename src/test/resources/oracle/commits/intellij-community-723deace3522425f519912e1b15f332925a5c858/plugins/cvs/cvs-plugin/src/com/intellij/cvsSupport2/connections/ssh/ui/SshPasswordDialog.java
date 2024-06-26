/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.cvsSupport2.connections.ssh.ui;

import com.intellij.openapi.ui.DialogWrapper;

import javax.swing.*;

/**
 * User: lesya
 */
public class SshPasswordDialog extends DialogWrapper{
  private JPasswordField myPasswordField;
  private JCheckBox myStoreCheckbox;
  private JPanel myPanel;
  private JLabel myLabel;


  public SshPasswordDialog(String propmtText) {
    super(true);
    myLabel.setText(propmtText);
    setTitle(com.intellij.CvsBundle.message("dialog.title.ssh.password"));
    init();
  }

  protected JComponent createCenterPanel() {
    return myPanel;
  }

  public String getPassword() {
    return new String(myPasswordField.getPassword());
  }

  public boolean saveThisPassword() {
    return myStoreCheckbox.isSelected();
  }

  public JComponent getPreferredFocusedComponent() {
    return myPasswordField;
  }
}
