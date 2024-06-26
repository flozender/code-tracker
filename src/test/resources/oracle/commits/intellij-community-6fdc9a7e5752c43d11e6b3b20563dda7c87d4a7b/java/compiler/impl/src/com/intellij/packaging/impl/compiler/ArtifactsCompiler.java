/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.packaging.impl.compiler;

import com.intellij.compiler.impl.newApi.CompileItem;
import com.intellij.compiler.impl.newApi.CompilerInstance;
import com.intellij.compiler.impl.newApi.NewCompiler;
import com.intellij.compiler.impl.newApi.VirtualFileCompileItem;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * @author nik
 */
public class ArtifactsCompiler extends NewCompiler<String, ArtifactPackagingItemOutputState> {
  static final Key<Set<String>> WRITTEN_PATHS_KEY = Key.create("artifacts_written_paths");
  static final Key<Set<Artifact>> AFFECTED_ARTIFACTS = Key.create("affected_artifacts");

  public ArtifactsCompiler() {
    super("artifacts_compiler", 0, NewCompiler.CompileOrderPlace.PACKAGING);
  }

  @Nullable
  public static ArtifactsCompiler getInstance(@NotNull Project project) {
    final ArtifactsCompiler[] compilers = CompilerManager.getInstance(project).getCompilers(ArtifactsCompiler.class);
    return compilers.length == 1 ? compilers[0] : null;
  }

  @NotNull
  @Override
  public KeyDescriptor<String> getItemKeyDescriptor() {
    return VirtualFileCompileItem.KEY_DESCRIPTOR;
  }

  @NotNull
  @Override
  public DataExternalizer<ArtifactPackagingItemOutputState> getItemStateExternalizer() {
    return ArtifactCompilerCompileItem.OUTPUT_EXTERNALIZER;
  }

  @NotNull
  @Override
  public CompilerInstance<ArtifactBuildTarget, ? extends CompileItem<String, ArtifactPackagingItemOutputState>, String, ArtifactPackagingItemOutputState> createInstance(
    @NotNull CompileContext context) {
    return new ArtifactsCompilerInstance(context);
  }

  public boolean validateConfiguration(final CompileScope scope) {
    return true;
  }

  @NotNull
  public String getDescription() {
    return "Artifacts Packaging Compiler";
  }

  public static Set<Artifact> getAffectedArtifacts(final CompileContext compileContext) {
    return compileContext.getUserData(AFFECTED_ARTIFACTS);
  }

  @Nullable
  public static Set<String> getWrittenPaths(@NotNull CompileContext context) {
    return context.getUserData(WRITTEN_PATHS_KEY);
  }
}
