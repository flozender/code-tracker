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
package com.intellij.usages;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Set;

/**
 * @author max
 */
public interface UsageView extends Disposable {
  /**
   * Returns {@link com.intellij.usages.UsageTarget} to look usages for
   */
  @NonNls String USAGE_TARGETS = "usageTarget";
  DataKey<UsageTarget[]> USAGE_TARGETS_KEY = DataKey.create(USAGE_TARGETS);

  /**
   * Returns {@link com.intellij.usages.Usage} which are selected in usage view
   */
  @NonNls String USAGES = "usages";
  DataKey<Usage[]> USAGES_KEY = DataKey.create(USAGES);

  @NonNls String USAGE_VIEW = "UsageView.new";
  DataKey<UsageView> USAGE_VIEW_KEY = DataKey.create(USAGE_VIEW);
  DataKey<UsageInfo> USAGE_INFO_KEY = DataKey.create("UsageInfo");
  DataKey<List<UsageInfo>> USAGE_INFO_LIST_KEY = DataKey.create("UsageInfo.List");

  void appendUsage(@NotNull Usage usage);
  void removeUsage(@NotNull Usage usage);
  void includeUsages(@NotNull Usage[] usages);
  void excludeUsages(@NotNull Usage[] usages);
  void selectUsages(@NotNull Usage[] usages);

  void close();
  boolean isSearchInProgress();

  /**
   * @deprecated please specify mnemonic by prefixing the mnenonic character with an ampersand (&& for Mac-specific ampersands)
   */
  void addButtonToLowerPane(@NotNull Runnable runnable, @NotNull String text, char mnemonic);
  void addButtonToLowerPane(@NotNull Runnable runnable, @NotNull String text);

  void addPerformOperationAction(@NotNull Runnable processRunnable, String commandName, String cannotMakeString, @NotNull String shortDescription);

  UsageViewPresentation getPresentation();

  @NotNull
  Set<Usage> getExcludedUsages();

  @Nullable
  Set<Usage> getSelectedUsages();
  @NotNull Set<Usage> getUsages();
  @NotNull List<Usage> getSortedUsages();

  @NotNull JComponent getComponent();

  int getUsagesCount();
}
