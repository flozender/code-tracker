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
package com.intellij.openapi.build;

import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.BuildSystemExecutionEnvironmentProvider;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 * @since 5/11/2016
 */
public class BuildSystemExecutionEnvironmentProviderImpl implements BuildSystemExecutionEnvironmentProvider {

  @Nullable
  @Override
  public ExecutionEnvironment createExecutionEnvironment(@NotNull RunProfile runProfile,
                                                         @NotNull Executor executor,
                                                         @NotNull ExecutionTarget target,
                                                         @NotNull Project project,
                                                         @Nullable RunnerSettings runnerSettings,
                                                         @Nullable ConfigurationPerRunnerSettings configurationSettings,
                                                         @Nullable RunnerAndConfigurationSettings settings) {
    for (BuildSystemDriver buildSystemDriver : BuildSystemDriver.EP_NAME.getExtensions()) {
      if (buildSystemDriver.canRun(executor.getId(), runProfile)) {
        return buildSystemDriver.createExecutionEnvironment(
          runProfile, executor, target, project, runnerSettings, configurationSettings, settings);
      }
    }
    return null;
  }
}
