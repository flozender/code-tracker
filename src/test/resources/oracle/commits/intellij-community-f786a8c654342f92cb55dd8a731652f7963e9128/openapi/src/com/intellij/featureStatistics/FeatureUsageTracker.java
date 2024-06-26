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
package com.intellij.featureStatistics;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NonNls;

/**
 * User: anna
 * Date: Jan 28, 2005
 */
public abstract class FeatureUsageTracker {
  public boolean SHOW_IN_COMPILATION_PROGRESS = true;
  public boolean SHOW_IN_OTHER_PROGRESS = true;

  public static FeatureUsageTracker getInstance() {
    return ApplicationManager.getApplication().getComponent(FeatureUsageTracker.class);
  }

  public abstract void triggerFeatureUsed(@NonNls String featureId);

  public abstract void triggerFeatureShown(@NonNls String featureId);
}
