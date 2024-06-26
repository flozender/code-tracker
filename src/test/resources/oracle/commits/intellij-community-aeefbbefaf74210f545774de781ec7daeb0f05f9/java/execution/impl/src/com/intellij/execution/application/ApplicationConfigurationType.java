package com.intellij.execution.application;

import com.intellij.execution.*;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiMethodUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ApplicationConfigurationType implements LocatableConfigurationType {
  private final ConfigurationFactory myFactory;
  private static final Icon ICON = IconLoader.getIcon("/runConfigurations/application.png");


  /**reflection*/
  public ApplicationConfigurationType() {
    myFactory = new ConfigurationFactory(this) {
      public RunConfiguration createTemplateConfiguration(Project project) {
        return new ApplicationConfiguration("", project, ApplicationConfigurationType.this);
      }

      @Override
      public Icon getIcon(@NotNull final RunConfiguration configuration) {
        return RunConfigurationExtension.getIcon((ApplicationConfiguration)configuration, getIcon());
      }
    };
  }

  public String getDisplayName() {
    return ExecutionBundle.message("application.configuration.name");
  }

  public String getConfigurationTypeDescription() {
    return ExecutionBundle.message("application.configuration.description");
  }

  public Icon getIcon() {
    return ICON;
  }

  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{myFactory};
  }

  public RunnerAndConfigurationSettings createConfigurationByLocation(final Location location) {
    return null;
  }

  public boolean isConfigurationByLocation(final RunConfiguration configuration, final Location location) {
    final PsiClass aClass = getMainClass(location.getPsiElement());
    if (aClass == null) {
      return false;
    }
    return Comparing.equal(JavaExecutionUtil.getRuntimeQualifiedName(aClass), ((ApplicationConfiguration)configuration).MAIN_CLASS_NAME)
           && Comparing.equal(JavaExecutionUtil.findModule(aClass), ((ApplicationConfiguration)configuration).getConfigurationModule().getModule());
  }

  public static PsiClass getMainClass(PsiElement element) {
    while (element != null) {
      if (element instanceof PsiClass) {
        final PsiClass aClass = (PsiClass)element;
        if (PsiMethodUtil.findMainInClass(aClass) != null){
          return aClass;
        }
      } else if (element instanceof PsiJavaFile) {
        final PsiJavaFile javaFile = (PsiJavaFile)element;
        final PsiClass[] classes = javaFile.getClasses();
        for (PsiClass aClass : classes) {
          if (PsiMethodUtil.findMainInClass(aClass) != null) {
            return aClass;
          }
        }
      }
      element = element.getParent();
    }
    return null;
  }


  @NotNull
  @NonNls
  public String getId() {
    return "Application";
  }

  @Nullable
  public static ApplicationConfigurationType getInstance() {
    return ContainerUtil.findInstance(Extensions.getExtensions(CONFIGURATION_TYPE_EP), ApplicationConfigurationType.class);
  }

}
