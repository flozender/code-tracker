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
package com.intellij.debugger.ui.breakpoints;

import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.HelpID;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.StringBuilderSpinAllocator;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaMethodBreakpointProperties;

import javax.swing.*;

/**
 * @author Eugene Zhuravlev
 *         Date: Apr 26, 2005
 */
public class JavaMethodBreakpointType extends JavaLineBreakpointTypeBase<JavaMethodBreakpointProperties> implements JavaBreakpointType {
  public JavaMethodBreakpointType() {
    super("javaMethod", DebuggerBundle.message("method.breakpoints.tab.title"));
  }

  @NotNull
  @Override
  public Icon getEnabledIcon() {
    return AllIcons.Debugger.Db_method_breakpoint;
  }

  @NotNull
  @Override
  public Icon getDisabledIcon() {
    return AllIcons.Debugger.Db_disabled_method_breakpoint;
  }

  //@Override
  protected String getHelpID() {
    return HelpID.METHOD_BREAKPOINTS;
  }

  @Override
  public boolean isAddBreakpointButtonVisible() {
    return false;
  }

  //@Override
  public String getDisplayName() {
    return DebuggerBundle.message("method.breakpoints.tab.title");
  }

  @Override
  public String getShortText(XLineBreakpoint<JavaMethodBreakpointProperties> breakpoint) {
    return getDisplayText(breakpoint);
  }

  @Override
  public String getDisplayText(XLineBreakpoint<JavaMethodBreakpointProperties> breakpoint) {
    return getText(breakpoint);
  }

  static String getText(XBreakpoint<JavaMethodBreakpointProperties> breakpoint) {
    final StringBuilder buffer = StringBuilderSpinAllocator.alloc();
    try {
      //if(isValid()) {
      final String className = breakpoint.getProperties().myClassPattern;
      final boolean classNameExists = className != null && className.length() > 0;
      if (classNameExists) {
        buffer.append(className);
      }
      if(breakpoint.getProperties().myMethodName != null) {
        if (classNameExists) {
          buffer.append(".");
        }
        buffer.append(breakpoint.getProperties().myMethodName);
      }
      //}
      //else {
      //  buffer.append(DebuggerBundle.message("status.breakpoint.invalid"));
      //}
      return buffer.toString();
    }
    finally {
      StringBuilderSpinAllocator.dispose(buffer);
    }
  }

  @Nullable
  @Override
  public XBreakpointCustomPropertiesPanel createCustomPropertiesPanel() {
    return new MethodBreakpointPropertiesPanel();
  }

  @Nullable
  @Override
  public JavaMethodBreakpointProperties createProperties() {
    return new JavaMethodBreakpointProperties();
  }

  @Nullable
  @Override
  public JavaMethodBreakpointProperties createBreakpointProperties(@NotNull VirtualFile file, int line) {
    return new JavaMethodBreakpointProperties();
  }

  @Override
  public Breakpoint createJavaBreakpoint(Project project, XBreakpoint breakpoint) {
    return new MethodBreakpoint(project, breakpoint);
  }
}
