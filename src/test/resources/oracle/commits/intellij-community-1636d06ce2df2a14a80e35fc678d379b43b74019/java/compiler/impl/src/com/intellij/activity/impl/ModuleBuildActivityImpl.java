/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.activity.impl;

import com.intellij.activity.ModuleBuildActivity;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 * @since 5/11/2016
 */
public class ModuleBuildActivityImpl extends AbstractBuildActivity implements ModuleBuildActivity {
  @NotNull
  private final Module myModule;

  public ModuleBuildActivityImpl(@NotNull Module module, boolean isIncrementalBuild) {
    super(isIncrementalBuild);
    myModule = module;
  }

  @NotNull
  @Override
  public Module getModule() {
    return myModule;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return "Module '" + myModule.getName() + "' build activity";
  }
}
