/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.module;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.openapi.project.ProjectBundle;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;

/**
 * @author yole
 */
public abstract class WebModuleTypeBase<T extends ModuleBuilder> extends ModuleType<T> {
  @NonNls public static final String WEB_MODULE = "WEB_MODULE";

  public WebModuleTypeBase() {
    super(WEB_MODULE);
  }

  public String getName() {
    return ProjectBundle.message("module.web.title");
  }

  public String getDescription() {
    return ProjectBundle.message("module.web.description");
  }

  public Icon getBigIcon() {
    return AllIcons.Modules.Types.WebModule;
  }

  public Icon getNodeIcon(final boolean isOpened) {
    return isOpened ? AllIcons.Nodes.ModuleOpen : AllIcons.Nodes.ModuleClosed;
  }
}
