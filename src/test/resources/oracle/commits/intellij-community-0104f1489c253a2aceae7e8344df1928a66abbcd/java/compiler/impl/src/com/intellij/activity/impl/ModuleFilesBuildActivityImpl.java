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

import com.intellij.activity.ModuleFilesBuildActivity;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;

import java.util.Collection;

/**
 * @author Vladislav.Soroka
 * @since 5/14/2016
 */
public class ModuleFilesBuildActivityImpl extends ModuleBuildActivityImpl implements ModuleFilesBuildActivity {
  private final VirtualFile[] myFiles;

  public ModuleFilesBuildActivityImpl(Module module, boolean isIncrementalBuild, VirtualFile... files) {
    super(module, isIncrementalBuild);
    myFiles = files;
  }

  public ModuleFilesBuildActivityImpl(Module module, boolean isIncrementalBuild, Collection<VirtualFile> files) {
    this(module, isIncrementalBuild, ArrayUtil.toObjectArray(files, VirtualFile.class));
  }

  @Override
  public VirtualFile[] getFiles() {
    return myFiles;
  }
}
