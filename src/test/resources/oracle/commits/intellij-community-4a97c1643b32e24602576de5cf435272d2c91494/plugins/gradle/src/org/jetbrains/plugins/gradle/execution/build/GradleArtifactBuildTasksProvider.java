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
package org.jetbrains.plugins.gradle.execution.build;

import com.intellij.openapi.build.BuildContext;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskPojo;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 * @since 7/11/2016
 */
public interface GradleArtifactBuildTasksProvider {
  ExtensionPointName<GradleArtifactBuildTasksProvider> EP_NAME =
    ExtensionPointName.create("org.jetbrains.plugins.gradle.artifactBuildTasksProvider");

  boolean isApplicable(@NotNull Artifact artifact);

  void addArtifactsTargetsBuildTasks(@NotNull BuildContext buildContext,
                                     @NotNull Consumer<ExternalTaskPojo> cleanTasksConsumer,
                                     @NotNull Consumer<ExternalTaskPojo> buildTasksConsumer,
                                     @NotNull Artifact artifact);
}
