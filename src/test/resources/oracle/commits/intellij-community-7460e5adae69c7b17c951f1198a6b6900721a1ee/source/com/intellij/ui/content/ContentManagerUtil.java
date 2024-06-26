package com.intellij.ui.content;

import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.DataConstantsEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.ide.DataManager;

import javax.swing.*;

public class ContentManagerUtil {
  /**
   * This is utility method. It returns <code>ContentManager</code> from the current context.
   */
  public static ContentManager getContentManagerFromContext(DataContext dataContext, boolean requiresVisibleToolWindow){
    Project project = (Project)dataContext.getData(DataConstants.PROJECT);
    if (project == null) {
      return null;
    }

    ToolWindowManagerEx mgr=ToolWindowManagerEx.getInstanceEx(project);

    String id = mgr.getActiveToolWindowId();
    if (id == null) {
      if(mgr.isEditorComponentActive()){
        id = mgr.getLastActiveToolWindowId();
      }
    }
    if(id == null){
      return null;
    }

    ToolWindowEx toolWindow = (ToolWindowEx)mgr.getToolWindow(id);
    if (requiresVisibleToolWindow && !toolWindow.isVisible()) {
      return null;
    }

    JComponent component = toolWindow.getComponent();
    ContentManager contentManager = (ContentManager)DataManager.getInstance().getDataContext(component).getData(DataConstantsEx.CONTENT_MANAGER);
    return contentManager;
  }
}
