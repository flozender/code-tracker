package org.jetbrains.plugins.gradle.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Denis Zhdanov
 * @since 1/18/12 1:16 PM
 */
public class GradleIcons {

  public static final Icon GRADLE_ICON       = IconLoader.getIcon("/icons/gradle.png");
  public static final Icon LIB_ICON          = AllIcons.Nodes.PpLib;
  public static final Icon MODULE_ICON       = AllIcons.Nodes.ModuleOpen;
  public static final Icon CONTENT_ROOT_ICON = AllIcons.Modules.AddContentEntry;
  public static final Icon JAR_ICON          = AllIcons.Nodes.PpJar;
  public static final Icon PROJECT_ICON      = getProjectIcon();

  private GradleIcons() {
  }

  @NotNull
  private static Icon getProjectIcon() {
    try {
      return IconLoader.getIcon(ApplicationInfoEx.getInstanceEx().getSmallIconUrl());
    }
    catch (Exception e) {
      // Control flow may reach this place if we run tests and platform IoC has not been initialised.
      return IconLoader.getIcon("/nodes/ideProject.png");
    }
  }
}
