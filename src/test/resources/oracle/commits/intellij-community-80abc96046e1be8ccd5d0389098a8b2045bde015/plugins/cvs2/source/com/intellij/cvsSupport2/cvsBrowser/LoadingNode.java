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
package com.intellij.cvsSupport2.cvsBrowser;

import com.intellij.openapi.util.IconLoader;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: lesya
 * Date: Jul 22, 2005
 * Time: 6:39:50 PM
 * To change this template use File | Settings | File Templates.
 */
class LoadingNode extends DefaultMutableTreeNode {
  private String myText = "Loading...";
  private int myPeriod = 0;

  private final Icon[] myIcons = new Icon[] {
    IconLoader.getIcon("/_cvs/testInProgress1.png"),
    IconLoader.getIcon("/_cvs/testInProgress2.png"),
    IconLoader.getIcon("/_cvs/testInProgress3.png"),
    IconLoader.getIcon("/_cvs/testInProgress4.png"),
    IconLoader.getIcon("/_cvs/testInProgress5.png"),
    IconLoader.getIcon("/_cvs/testInProgress6.png"),
    IconLoader.getIcon("/_cvs/testInProgress7.png"),
    IconLoader.getIcon("/_cvs/testInProgress8.png")
  };

  public boolean getAllowsChildren() {
    return false;
  }

  public String toString() {
    return myText;
  }

  public void setText(final String s) {
    myText = s;
  }

  public void updatePeriod() {
    myPeriod++;
    myPeriod  = myPeriod - 8 * (myPeriod / 8);
  }

  public Icon getIcon() {
    return myIcons[myPeriod];
  }

}
