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
package com.intellij.ide.highlighter;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.fileTypes.InternalFileType;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class WorkspaceFileType implements InternalFileType {
  private static final Icon ICON = AllIcons.Nodes.IdeaWorkspace;
  @NonNls public static final String DEFAULT_EXTENSION = "iws";
  @NonNls public static final String DOT_DEFAULT_EXTENSION = "." + DEFAULT_EXTENSION;

  @NotNull
  public String getName() {
    return "IDEA_WORKSPACE";
  }

  @NotNull
  public String getDescription() {
    return IdeBundle.message("filetype.description.idea.workspace");
  }

  @NotNull
  public String getDefaultExtension() {
    return DEFAULT_EXTENSION;
  }

  public Icon getIcon() {
    return ICON;
  }

  public boolean isBinary() {
    return false;
  }

  public boolean isReadOnly() {
    return true;
  }

  public String getCharset(@NotNull VirtualFile file, final byte[] content) {
    return CharsetToolkit.UTF8;
  }
}
