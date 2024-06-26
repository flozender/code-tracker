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
package com.intellij.pom.tree;

import com.intellij.pom.PomModel;
import com.intellij.pom.PomModelAspect;
import com.intellij.pom.event.PomModelEvent;

import java.util.Collections;
import java.util.Set;

public class TreeAspect implements PomModelAspect{
  private final PomModel myModel;

  public TreeAspect(PomModel model) {
    myModel = model;
    myModel.registerAspect(TreeAspect.class, this, (Set<PomModelAspect>)Collections.EMPTY_SET);
  }

  public void projectOpened() {}
  public void projectClosed() {}
  public void initComponent() {}
  public void disposeComponent() {}
  public void update(PomModelEvent event) {}

  public String getComponentName() {
    return "Tree POM aspect";
  }
}
