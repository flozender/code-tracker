/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

package com.intellij.util.xml.tree;

import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.xml.DomFileElement;

public class DomFileElementNode extends BaseDomElementNode {
  private final DomFileElement myFileElement;

  public DomFileElementNode(final DomFileElement fileElement) {
    super(fileElement);

    myFileElement = fileElement;
  }

  public SimpleNode[] getChildren() {
    return doGetChildren(myFileElement.getRootElement());
  }


  public DomFileElement getDomElement() {
    return (DomFileElement)super.getDomElement();
  }


  public boolean isShowContainingFileInfo() {
    return false;
  }
}
