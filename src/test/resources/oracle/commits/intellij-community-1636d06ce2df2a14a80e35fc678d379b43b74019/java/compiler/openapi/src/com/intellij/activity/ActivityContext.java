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
package com.intellij.activity;

import com.intellij.execution.configurations.RunConfiguration;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 * @since 7/15/2016
 */
public class ActivityContext {
  @Nullable
  private Object mySessionId;
  @Nullable
  private RunConfiguration myRunConfiguration;

  public ActivityContext() {
  }

  public ActivityContext(@Nullable Object sessionId) {
    mySessionId = sessionId;
  }

  public ActivityContext(@Nullable Object sessionId, @Nullable RunConfiguration runConfiguration) {
    mySessionId = sessionId;
    myRunConfiguration = runConfiguration;
  }

  @Nullable
  public Object getSessionId() {
    return mySessionId;
  }

  @Nullable
  public RunConfiguration getRunConfiguration() {
    return myRunConfiguration;
  }
}
