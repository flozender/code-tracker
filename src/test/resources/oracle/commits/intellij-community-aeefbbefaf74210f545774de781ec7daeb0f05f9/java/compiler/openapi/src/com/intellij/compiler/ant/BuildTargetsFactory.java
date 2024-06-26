/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

/*
 * User: anna
 * Date: 19-Dec-2006
 */
package com.intellij.compiler.ant;

import com.intellij.compiler.ant.taskdefs.Target;
import com.intellij.openapi.compiler.make.BuildRecipe;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

public abstract class BuildTargetsFactory {
  public static BuildTargetsFactory getInstance() {
    return ServiceManager.getService(BuildTargetsFactory.class);
  }

  public abstract CompositeGenerator createCompositeBuildTarget(ExplodedAndJarTargetParameters parameters, @NonNls String targetName, 
                                                                String description, String depends, @Nullable String jarPath);

  public abstract Target createBuildExplodedTarget(ExplodedAndJarTargetParameters parameters, BuildRecipe buildRecipe, String description);

  public abstract Target createBuildJarTarget(ExplodedAndJarTargetParameters parameters, BuildRecipe buildRecipe, String description);

  public abstract Generator createComment(String comment);

  //for test
  public abstract GenerationOptions getDefaultOptions(Project project);
}