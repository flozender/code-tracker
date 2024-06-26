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
package com.intellij.usages.impl.rules;

import com.intellij.openapi.vcs.FileStatus;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsageView;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usages.rules.PsiElementUsage;
import com.intellij.usages.rules.UsageGroupingRule;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Dec 23, 2004
 * Time: 2:41:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class NonCodeUsageGroupingRule implements UsageGroupingRule {
  private static class CodeUsageGroup implements UsageGroup {
    public static final UsageGroup INSTANCE = new CodeUsageGroup();
    private CodeUsageGroup() {}

    public String getText(UsageView view) {
      return view == null ? UsageViewBundle.message("node.group.code.usages") : view.getPresentation().getCodeUsagesString();
    }

    public void update() {
    }

    public String toString() {
      //noinspection HardCodedStringLiteral
      return "CodeUsages";
    }

    public Icon getIcon(boolean isOpen) { return null; }
    public FileStatus getFileStatus() { return null; }
    public boolean isValid() { return true; }
    public int compareTo(UsageGroup usageGroup) { return usageGroup == this ? 0 : 1; }
    public void navigate(boolean requestFocus) { }
    public boolean canNavigate() { return false; }

    public boolean canNavigateToSource() {
      return canNavigate();
    }
  }

  private static class NonCodeUsageGroup implements UsageGroup {
    public static final UsageGroup INSTANCE = new NonCodeUsageGroup();
    private NonCodeUsageGroup() {}

    public String getText(UsageView view) {
      return view == null ? UsageViewBundle.message("node.group.code.usages") : view.getPresentation().getNonCodeUsagesString();
    }

    public void update() {
    }

    public String toString() {
      //noinspection HardCodedStringLiteral
      return "NonCodeUsages";
    }
    public Icon getIcon(boolean isOpen) { return null; }
    public FileStatus getFileStatus() { return null; }
    public boolean isValid() { return true; }
    public int compareTo(UsageGroup usageGroup) { return usageGroup == this ? 0 : -1; }
    public void navigate(boolean requestFocus) { }
    public boolean canNavigate() { return false; }

    public boolean canNavigateToSource() {
      return canNavigate();
    }
  }

  public UsageGroup groupUsage(Usage usage) {
    if (usage instanceof PsiElementUsage) {
      return ((PsiElementUsage)usage).isNonCodeUsage() ? NonCodeUsageGroup.INSTANCE : CodeUsageGroup.INSTANCE;
    }
    return null;
  }
}
