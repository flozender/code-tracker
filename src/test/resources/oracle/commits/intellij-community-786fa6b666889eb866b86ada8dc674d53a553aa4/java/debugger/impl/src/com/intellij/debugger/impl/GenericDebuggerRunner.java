package com.intellij.debugger.impl;

import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.settings.DebuggerSettings;
import com.intellij.debugger.ui.DebuggerPanelsManager;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.JavaPatchableProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.history.LocalHistory;
import com.intellij.history.LocalHistoryConfiguration;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GenericDebuggerRunner extends JavaPatchableProgramRunner<GenericDebuggerRunnerSettings> {
  public boolean canRun(@NotNull final String executorId, @NotNull final RunProfile profile) {
    return executorId.equals(DefaultDebugExecutor.EXECUTOR_ID) && profile instanceof ModuleRunProfile;
  }

  @NotNull
  public String getRunnerId() {
    return DebuggingRunnerData.DEBUGGER_RUNNER_ID;
  }

  protected RunContentDescriptor doExecute(final Project project, final Executor executor, final RunProfileState state, final RunContentDescriptor contentToReuse,
                                           final ExecutionEnvironment env) throws ExecutionException {
    FileDocumentManager.getInstance().saveAllDocuments();

    if (LocalHistoryConfiguration.getInstance().ADD_LABEL_ON_RUNNING) {
      final String name = env.getRunProfile().getName();
      final String label = state instanceof RemoteState
                           ? DebuggerBundle.message("debugger.runner.vcs.label.remote.debug", name)
                           : DebuggerBundle.message("debugger.runner.vcs.label.debugging", name);
      LocalHistory.putSystemLabel(project, label);
    }

    return createContentDescriptor(project, executor, state, contentToReuse, env);
  }

  @Nullable
  protected RunContentDescriptor createContentDescriptor(Project project, Executor executor, RunProfileState state,
                                                         RunContentDescriptor contentToReuse,
                                                         ExecutionEnvironment env) throws ExecutionException {
    if (state instanceof JavaCommandLine) {
      final JavaParameters parameters = ((JavaCommandLine)state).getJavaParameters();
      RemoteConnection connection = DebuggerManagerImpl.createDebugParameters(parameters, true, DebuggerSettings.getInstance().DEBUGGER_TRANSPORT, "", false);
      return attachVirtualMachine(project, executor, state, contentToReuse, env, connection, true);
    }
    if (state instanceof PatchedRunnableState) {
      final RemoteConnection connection = doPatch(new JavaParameters(), state.getRunnerSettings());
      return attachVirtualMachine(project, executor, state, contentToReuse, env, connection, true);
    }
    if (state instanceof RemoteState) {
      final RemoteConnection connection = createRemoteDebugConnection((RemoteState)state, state.getRunnerSettings());
      return attachVirtualMachine(project, executor, state, contentToReuse, env, connection, false);
    }

    return null;
  }

  @Nullable
  protected RunContentDescriptor attachVirtualMachine(Project project, Executor executor, RunProfileState state,
                                                      RunContentDescriptor contentToReuse,
                                                      ExecutionEnvironment env, RemoteConnection connection, boolean pollConnection)
    throws ExecutionException {
    final DebuggerPanelsManager manager = DebuggerPanelsManager.getInstance(project);
    return manager.attachVirtualMachine(executor, this, env, state, contentToReuse, connection, pollConnection);
  }

  private static RemoteConnection createRemoteDebugConnection(RemoteState connection, final RunnerSettings settings) {
    final RemoteConnection remoteConnection = connection.getRemoteConnection();

    GenericDebuggerRunnerSettings debuggerRunnerSettings = ((GenericDebuggerRunnerSettings)settings.getData());

    if (debuggerRunnerSettings != null) {
      remoteConnection.setUseSockets(debuggerRunnerSettings.getTransport() == DebuggerSettings.SOCKET_TRANSPORT);
      remoteConnection.setAddress(debuggerRunnerSettings.DEBUG_PORT);
    }

    return remoteConnection;
  }

  public GenericDebuggerRunnerSettings createConfigurationData(ConfigurationInfoProvider settingsProvider) {
    return new GenericDebuggerRunnerSettings();
  }

  public void patch(JavaParameters javaParameters, RunnerSettings settings, final boolean beforeExecution) throws ExecutionException {
    doPatch(javaParameters, settings);
  }

  private static RemoteConnection doPatch(final JavaParameters javaParameters, final RunnerSettings settings) throws ExecutionException {
    final GenericDebuggerRunnerSettings debuggerSettings = ((GenericDebuggerRunnerSettings)settings.getData());
    return DebuggerManagerImpl.createDebugParameters(javaParameters, debuggerSettings, false);
  }

  public SettingsEditor<GenericDebuggerRunnerSettings> getSettingsEditor(final Executor executor, RunConfiguration configuration) {
    if (configuration instanceof RunConfigurationWithRunnerSettings) {
      if (((RunConfigurationWithRunnerSettings)configuration).isSettingsNeeded()) {
        return new GenericDebuggerParametersRunnerConfigurable(configuration.getProject());
      }
    }
    return null;
  }
}
