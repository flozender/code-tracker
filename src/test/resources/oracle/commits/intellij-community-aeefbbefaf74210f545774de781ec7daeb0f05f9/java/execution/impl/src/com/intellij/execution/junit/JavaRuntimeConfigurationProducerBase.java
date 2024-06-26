package com.intellij.execution.junit;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.testframework.TestSearchScope;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.Nullable;

/**
 * @author spleaner
 */
public abstract class JavaRuntimeConfigurationProducerBase extends RuntimeConfigurationProducer {

  protected JavaRuntimeConfigurationProducerBase(final ConfigurationType configurationType) {
    super(configurationType);
  }

  protected static PsiMethod getContainingMethod(PsiElement element) {
    while (element != null)
      if (element instanceof PsiMethod) break;
      else element = element.getParent();
    return (PsiMethod) element;
  }

  @Nullable
  protected static PsiPackage checkPackage(final PsiElement element) {
    if (element == null || !element.isValid()) return null;
    final Project project = element.getProject();
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    if (element instanceof PsiPackage) {
      final PsiPackage aPackage = (PsiPackage)element;
      final PsiDirectory[] directories = aPackage.getDirectories(GlobalSearchScope.projectScope(project));
      for (final PsiDirectory directory : directories) {
        if (isSource(directory, fileIndex)) return aPackage;
      }
      return null;
    }
    else if (element instanceof PsiDirectory) {
      final PsiDirectory directory = (PsiDirectory)element;
      return isSource(directory, fileIndex) ? JavaDirectoryService.getInstance().getPackage(directory) : null;
    }
    else {
      return null;
    }
  }

  private static boolean isSource(final PsiDirectory directory, final ProjectFileIndex fileIndex) {
    final VirtualFile virtualFile = directory.getVirtualFile();
    return fileIndex.getSourceRootForFile(virtualFile) != null;
  }

  protected TestSearchScope setupPackageConfiguration(ConfigurationContext context, Project project, ModuleBasedConfiguration configuration, TestSearchScope scope) {
    copyStepsBeforeRun(project, configuration);
    if (scope != TestSearchScope.WHOLE_PROJECT) {
      if (!setupConfigurationModule(context, configuration)) {
        return TestSearchScope.WHOLE_PROJECT;
      }
    }
    return scope;
  }

  protected boolean setupConfigurationModule(@Nullable ConfigurationContext context, ModuleBasedConfiguration configuration) {
    if (context != null) {
      final RunnerAndConfigurationSettingsImpl template =
        ((RunManagerImpl)context.getRunManager()).getConfigurationTemplate(getConfigurationFactory());
      final Module contextModule = context.getModule();
      final Module predefinedModule = ((ModuleBasedConfiguration)template.getConfiguration()).getConfigurationModule().getModule();
      if (predefinedModule != null) {
        configuration.setModule(predefinedModule);
        return true;
      }
      else if (configuration.getConfigurationModule().getModule() == null && contextModule != null) {
        configuration.setModule(contextModule);
        return true;
      }
    }
    return false;
  }
}
