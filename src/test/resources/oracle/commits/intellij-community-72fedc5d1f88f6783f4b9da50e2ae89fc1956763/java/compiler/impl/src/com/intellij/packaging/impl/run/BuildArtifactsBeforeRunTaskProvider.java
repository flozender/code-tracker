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
package com.intellij.packaging.impl.run;

import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.compiler.Compiler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.packaging.artifacts.*;
import com.intellij.packaging.impl.compiler.ArtifactAwareCompiler;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.packaging.impl.compiler.IncrementalArtifactsCompiler;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
public class BuildArtifactsBeforeRunTaskProvider extends BeforeRunTaskProvider<BuildArtifactsBeforeRunTask> {
  @NonNls public static final String BUILD_ARTIFACTS_ID = "BuildArtifacts";
  public static final Key<BuildArtifactsBeforeRunTask> ID = Key.create(BUILD_ARTIFACTS_ID);
  private final Project myProject;

  public BuildArtifactsBeforeRunTaskProvider(Project project) {
    myProject = project;
    project.getMessageBus().connect().subscribe(ArtifactManager.TOPIC, new ArtifactAdapter() {
      @Override
      public void artifactRemoved(@NotNull Artifact artifact) {
        final RunManagerEx runManager = RunManagerEx.getInstanceEx(myProject);
        for (RunConfiguration configuration : runManager.getAllConfigurations()) {
          final BuildArtifactsBeforeRunTask task = runManager.getBeforeRunTask(configuration, ID);
          if (task != null) {
            final String artifactName = artifact.getName();
            final List<ArtifactPointer> pointersList = task.getArtifactPointers();
            final ArtifactPointer[] pointers = pointersList.toArray(new ArtifactPointer[pointersList.size()]);
            for (ArtifactPointer pointer : pointers) {
              if (pointer.getArtifactName().equals(artifactName) && ArtifactManager.getInstance(myProject).findArtifact(artifactName) == null) {
                task.removeArtifact(pointer);
              }
            }
          }
        }
      }
    });
  }

  public Key<BuildArtifactsBeforeRunTask> getId() {
    return ID;
  }

  public String getDescription(RunConfiguration runConfiguration, BuildArtifactsBeforeRunTask task) {
    final List<ArtifactPointer> pointers = task.getArtifactPointers();
    if (!task.isEnabled() || pointers.isEmpty()) {
      return "Build Artifacts";
    }
    if (pointers.size() == 1) {
      return "Build '" + pointers.get(0).getArtifactName() + "' artifact";
    }
    return "Build " + pointers.size() + " artifacts";
  }

  public boolean hasConfigurationButton() {
    return true;
  }

  public BuildArtifactsBeforeRunTask createTask(RunConfiguration runConfiguration) {
    return new BuildArtifactsBeforeRunTask(myProject);
  }

  public boolean configureTask(RunConfiguration runConfiguration, BuildArtifactsBeforeRunTask task) {
    final Artifact[] artifacts = ArtifactManager.getInstance(myProject).getArtifacts();
    Set<ArtifactPointer> pointers = new THashSet<ArtifactPointer>();
    for (Artifact artifact : artifacts) {
      pointers.add(ArtifactPointerManager.getInstance(myProject).createPointer(artifact));
    }
    pointers.addAll(task.getArtifactPointers());
    ArtifactChooser chooser = new ArtifactChooser(new ArrayList<ArtifactPointer>(pointers));
    chooser.markElements(task.getArtifactPointers());
    chooser.setPreferredSize(new Dimension(400, 300));

    DialogBuilder builder = new DialogBuilder(myProject);
    builder.setTitle("Select Artifacts");
    builder.setDimensionServiceKey("#BuildArtifactsBeforeRunChooser");
    builder.addOkAction();
    builder.addCancelAction();
    builder.setCenterPanel(chooser);
    builder.setPreferedFocusComponent(chooser);
    if (builder.show() == DialogWrapper.OK_EXIT_CODE) {
      task.setArtifactPointers(chooser.getMarkedElements());
      return true;
    }
    return false;
  }

  public boolean executeTask(DataContext context, RunConfiguration configuration, final BuildArtifactsBeforeRunTask task) {
    final Ref<Boolean> result = Ref.create(false);
    final Semaphore finished = new Semaphore();

    final List<Artifact> artifacts = new ArrayList<Artifact>();
    new ReadAction() {
      protected void run(final Result result) {
        for (ArtifactPointer pointer : task.getArtifactPointers()) {
          ContainerUtil.addIfNotNull(pointer.getArtifact(), artifacts);
        }
      }
    }.execute();
    
    final CompileStatusNotification callback = new CompileStatusNotification() {
      public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
        result.set(!aborted && errors == 0);
        finished.up();
      }
    };
    final CompilerFilter compilerFilter = new CompilerFilter() {
      public boolean acceptCompiler(Compiler compiler) {
        return compiler instanceof IncrementalArtifactsCompiler
               || compiler instanceof ArtifactAwareCompiler && ((ArtifactAwareCompiler)compiler).shouldRun(artifacts);
      }
    };

    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      public void run() {
        final CompilerManager manager = CompilerManager.getInstance(myProject);
        finished.down();
        manager.make(ArtifactCompileScope.createArtifactsScope(myProject, artifacts), compilerFilter, callback);
      }
    }, ModalityState.NON_MODAL);

    finished.waitFor();
    return result.get();
  }

  public static void setBuildArtifactBeforeRunOption(@NotNull JComponent runConfigurationEditorComponent, @NotNull Artifact artifact, final boolean enable) {
    final DataContext dataContext = DataManager.getInstance().getDataContext(runConfigurationEditorComponent);
    final ConfigurationSettingsEditorWrapper editor = ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(dataContext);
    if (editor != null) {
      final BuildArtifactsBeforeRunTask task = (BuildArtifactsBeforeRunTask)editor.getStepsBeforeLaunch().get(ID);
      if (enable) {
        task.addArtifact(artifact);
        task.setEnabled(true);
      }
      else {
        task.removeArtifact(artifact);
        if (task.getArtifactPointers().isEmpty()) {
          task.setEnabled(false);
        }
      }
      editor.updateBeforeRunTaskPanel(ID);
    }
  }

  public static void setBuildArtifactBeforeRun(@NotNull Project project, @NotNull RunConfiguration configuration, @NotNull Artifact artifact) {
    RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
    final BuildArtifactsBeforeRunTask buildArtifactsTask = runManager.getBeforeRunTask(configuration, ID);
    if (buildArtifactsTask != null) {
      buildArtifactsTask.setEnabled(true);
      buildArtifactsTask.addArtifact(artifact);
    }
  }
}
