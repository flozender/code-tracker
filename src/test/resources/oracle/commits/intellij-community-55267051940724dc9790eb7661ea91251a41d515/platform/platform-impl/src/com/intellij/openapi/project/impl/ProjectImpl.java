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
package com.intellij.openapi.project.impl;

import com.intellij.diagnostic.PluginException;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.startup.StartupManagerEx;
import com.intellij.notification.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.StorageScheme;
import com.intellij.openapi.components.TrackingPathMacroSubstitutor;
import com.intellij.openapi.components.impl.ComponentManagerImpl;
import com.intellij.openapi.components.impl.ProjectPathMacroManager;
import com.intellij.openapi.components.impl.stores.IComponentStore;
import com.intellij.openapi.components.impl.stores.IProjectStore;
import com.intellij.openapi.components.impl.stores.UnknownMacroNotification;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ex.MessagesEx;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.FrameTitleBuilder;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.*;
import org.picocontainer.defaults.CachingComponentAdapter;
import org.picocontainer.defaults.ConstructorInjectionComponentAdapter;

import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 *
 */
public class ProjectImpl extends ComponentManagerImpl implements ProjectEx {
  private static final Logger LOG = Logger.getInstance("#com.intellij.project.impl.ProjectImpl");
  private static final String PLUGIN_SETTINGS_ERROR = "Plugin Settings Error";

  private ProjectManagerImpl myManager;

  private MyProjectManagerListener myProjectManagerListener;

  private final AtomicBoolean mySavingInProgress = new AtomicBoolean(false);

  public boolean myOptimiseTestLoadSpeed;
  @NonNls public static final String TEMPLATE_PROJECT_NAME = "Default (Template) Project";

  private String myName;
  private String myOldName;

  public static Key<Long> CREATION_TIME = Key.create("ProjectImpl.CREATION_TIME");

  protected ProjectImpl(ProjectManagerImpl manager, String filePath, boolean isOptimiseTestLoadSpeed, String projectName) {
    super(ApplicationManager.getApplication());
    putUserData(CREATION_TIME, System.nanoTime());

    getPicoContainer().registerComponentInstance(Project.class, this);

    getStateStore().setProjectFilePath(filePath);

    myOptimiseTestLoadSpeed = isOptimiseTestLoadSpeed;

    myManager = manager;
    myName = isDefault() ? TEMPLATE_PROJECT_NAME : projectName == null ? getStateStore().getProjectName() : projectName;
    if (!isDefault() && projectName != null && getStateStore().getStorageScheme().equals(StorageScheme.DIRECTORY_BASED)) myOldName = ""; // new project
  }

  public void setProjectName(final String projectName) {
    if (!projectName.equals(myName)) {
      myOldName = myName;
      myName = projectName;
      WindowManager.getInstance().getFrame(this).setTitle(FrameTitleBuilder.getInstance().getProjectTitle(this));
    }
  }

  protected void boostrapPicoContainer() {
    Extensions.instantiateArea(PluginManager.AREA_IDEA_PROJECT, this, null);
    super.boostrapPicoContainer();
    final MutablePicoContainer picoContainer = getPicoContainer();

    final ProjectStoreClassProvider projectStoreClassProvider = (ProjectStoreClassProvider)picoContainer.getComponentInstanceOfType(ProjectStoreClassProvider.class);


    picoContainer.registerComponentImplementation(ProjectPathMacroManager.class);
    picoContainer.registerComponent(new ComponentAdapter() {
      ComponentAdapter myDelegate;


      public ComponentAdapter getDelegate() {
        if (myDelegate == null) {

          final Class storeClass = projectStoreClassProvider.getProjectStoreClass(isDefault());
          myDelegate = new CachingComponentAdapter(
            new ConstructorInjectionComponentAdapter(storeClass, storeClass, null, true));
        }

        return myDelegate;
      }

      public Object getComponentKey() {
        return IComponentStore.class;
      }

      public Class getComponentImplementation() {
        return getDelegate().getComponentImplementation();
      }

      public Object getComponentInstance(final PicoContainer container) throws PicoInitializationException, PicoIntrospectionException {
        return getDelegate().getComponentInstance(container);
      }

      public void verify(final PicoContainer container) throws PicoIntrospectionException {
        getDelegate().verify(container);
      }

      public void accept(final PicoVisitor visitor) {
        visitor.visitComponentAdapter(this);
        getDelegate().accept(visitor);
      }
    });

  }

  @NotNull
  public IProjectStore getStateStore() {
    return (IProjectStore)super.getStateStore();
  }

  public boolean isOpen() {
    return ProjectManagerEx.getInstanceEx().isProjectOpened(this);
  }

  public boolean isInitialized() {
    return isOpen() && !isDisposed() && StartupManagerEx.getInstanceEx(this).startupActivityPassed();
  }

  public void loadProjectComponents() {
    final Application app = ApplicationManager.getApplication();
    final IdeaPluginDescriptor[] plugins = app.getPlugins();
    for (IdeaPluginDescriptor plugin : plugins) {
      if (PluginManager.shouldSkipPlugin(plugin)) continue;
      loadComponentsConfiguration(plugin.getProjectComponents(), plugin, isDefault());
    }
  }

  @NotNull
  public String getProjectFilePath() {
    return getStateStore().getProjectFilePath();
  }


  @Nullable
  public VirtualFile getProjectFile() {
    return getStateStore().getProjectFile();
  }

  @Nullable
  public VirtualFile getBaseDir() {
    return getStateStore().getProjectBaseDir();
  }

  @NotNull
  public String getName() {
    return myName;
  }

  @Nullable
  @NonNls
  public String getPresentableUrl() {
    return getStateStore().getPresentableUrl();
  }

  @NotNull
  @NonNls
  public String getLocationHash() {
    String str = getPresentableUrl();
    if (str == null) str = getName();

    final String prefix = getStateStore().getStorageScheme() == StorageScheme.DIRECTORY_BASED? "" : getName();
    return prefix + Integer.toHexString(str.hashCode());
  }

  @Nullable
  @NonNls
  public String getLocation() {
    if (myName == null) return null; // was called before initialized
    return isDisposed() ? null : getStateStore().getLocation();
  }

  @Nullable
  public VirtualFile getWorkspaceFile() {
    return getStateStore().getWorkspaceFile();
  }

  public boolean isOptimiseTestLoadSpeed() {
    return myOptimiseTestLoadSpeed;
  }

  public void setOptimiseTestLoadSpeed(final boolean optimiseTestLoadSpeed) {
    myOptimiseTestLoadSpeed = optimiseTestLoadSpeed;
  }


  public void init() {
    super.init();
    getMessageBus().syncPublisher(ProjectLifecycleListener.TOPIC).projectComponentsInitialized(this);
    myProjectManagerListener = new MyProjectManagerListener();
    myManager.addProjectManagerListener(this, myProjectManagerListener);
  }

  public boolean isToSaveProjectName() {
    if (!isDefault()) {
      final IProjectStore stateStore = getStateStore();
      if (stateStore.getStorageScheme().equals(StorageScheme.DIRECTORY_BASED)) {
        final VirtualFile baseDir = stateStore.getProjectBaseDir();
        if (baseDir != null && baseDir.isValid()) {
          return myOldName != null && !myOldName.equals(getName());
        }
      }
    }

    return false;
  }

  public void save() {
    if (ApplicationManagerEx.getApplicationEx().isDoNotSave()) return; //no need to save

    if (mySavingInProgress.compareAndSet(false, true)) {
      try {
        if (isToSaveProjectName()) {
          final IProjectStore stateStore = getStateStore();
          final VirtualFile baseDir = stateStore.getProjectBaseDir();
          if (baseDir != null && baseDir.isValid()) {
            final VirtualFile ideaDir = baseDir.findChild(DIRECTORY_STORE_FOLDER);
            if (ideaDir != null && ideaDir.isValid() && ideaDir.isDirectory()) {
              final File nameFile = new File(ideaDir.getPath(), ".name");
              try {
                FileUtil.writeToFile(nameFile, getName().getBytes("UTF-8"), false);
                myOldName = null;
              }
              catch (IOException e) {
                LOG.info("Unable to store project name to: " + nameFile.getPath());
              }
            }
          }
        }

        doSave();
      }
      catch (IComponentStore.SaveCancelledException e) {
        LOG.info(e);
      }
      catch (PluginException e) {
        PluginManager.disablePlugin(e.getPluginId().getIdString());
        Notifications.Bus.notify(new Notification(PLUGIN_SETTINGS_ERROR, "Unable to save plugin settings!",
                                                  "<p>The plugin <i>" + e.getPluginId() + "</i> failed to save settings and has been disabled. Please restart" +
                                                  ApplicationNamesInfo.getInstance().getFullProductName() + "</p>" +
                                (ApplicationManagerEx.getApplicationEx().isInternal() ? "<p>" + StringUtil.getThrowableText(e) + "</p>": ""),
                                                  NotificationType.ERROR), NotificationDisplayType.BALLOON, this);
        LOG.info("Unable to save plugin settings",e);
      }
      catch (IOException e) {
        MessagesEx.error(this, ProjectBundle.message("project.save.error", ApplicationManagerEx.getApplicationEx().isInternal()
                                                                           ? StringUtil.getThrowableText(e)
                                                                           : e.getMessage())).showLater();
        LOG.info("Error saving project", e);
      } finally {
        mySavingInProgress.set(false);
        ApplicationManager.getApplication().getMessageBus().syncPublisher(ProjectSaved.TOPIC).saved(this);
      }
    }
  }

  public synchronized void dispose() {
    ApplicationEx application = ApplicationManagerEx.getApplicationEx();
    assert application.isDispatchThread();

    // can call dispose only via com.intellij.ide.impl.ProjectUtil.closeAndDispose()
    LOG.assertTrue(ApplicationManager.getApplication().isUnitTestMode() || !ProjectManagerEx.getInstanceEx().isProjectOpened(this));

    LOG.assertTrue(!isDisposed());
    if (myProjectManagerListener != null) {
      myManager.removeProjectManagerListener(this, myProjectManagerListener);
    }

    disposeComponents();
    Extensions.disposeArea(this);
    myManager = null;
    myProjectManagerListener = null;

    super.dispose();

    if (!application.isDisposed()) {
      application.getMessageBus().syncPublisher(ProjectLifecycleListener.TOPIC).afterProjectClosed(this);
    }
  }

  private void projectOpened() {
    final ProjectComponent[] components = getComponents(ProjectComponent.class);
    for (ProjectComponent component : components) {
      try {
        component.projectOpened();
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }
  }
  

  private void projectClosed() {
    List<ProjectComponent> components = new ArrayList<ProjectComponent>(Arrays.asList(getComponents(ProjectComponent.class)));
    Collections.reverse(components);
    for (ProjectComponent component : components) {
      try {
        component.projectClosed();
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }
  }

  public <T> T[] getExtensions(final ExtensionPointName<T> extensionPointName) {
    return Extensions.getArea(this).getExtensionPoint(extensionPointName).getExtensions();
  }

  public String getDefaultName() {
    if (isDefault()) return TEMPLATE_PROJECT_NAME;

    return getStateStore().getProjectName();    
  }

  private class MyProjectManagerListener extends ProjectManagerAdapter {
    public void projectOpened(Project project) {
      LOG.assertTrue(project == ProjectImpl.this);
      ProjectImpl.this.projectOpened();
    }

    public void projectClosed(Project project) {
      LOG.assertTrue(project == ProjectImpl.this);
      ProjectImpl.this.projectClosed();
    }
  }

  protected MutablePicoContainer createPicoContainer() {
    return Extensions.getArea(this).getPicoContainer();
  }

  public boolean isDefault() {
    return false;
  }

  public void checkUnknownMacros(final boolean showDialog) {
    final IProjectStore stateStore = getStateStore();

    final TrackingPathMacroSubstitutor[] substitutors = stateStore.getSubstitutors();
    final Set<String> unknownMacros = new HashSet<String>();
    for (final TrackingPathMacroSubstitutor substitutor : substitutors) {
      unknownMacros.addAll(substitutor.getUnknownMacros(null));
    }

    if (!unknownMacros.isEmpty()) {
      if (!showDialog || ProjectMacrosUtil.checkMacros(this, new HashSet<String>(unknownMacros))) {
        final PathMacros pathMacros = PathMacros.getInstance();
        final Set<String> macros2invalidate = new HashSet<String>(unknownMacros);
        for (Iterator it = macros2invalidate.iterator(); it.hasNext();) {
          final String macro = (String)it.next();
          final String value = pathMacros.getValue(macro);
          if ((null == value || value.trim().length() == 0) && !pathMacros.isIgnoredMacroName(macro)) {
            it.remove();
          }
        }

        if (!macros2invalidate.isEmpty()) {
          final Set<String> components = new HashSet<String>();
          for (TrackingPathMacroSubstitutor substitutor : substitutors) {
            components.addAll(substitutor.getComponents(macros2invalidate));
          }

          if (stateStore.isReloadPossible(components)) {
            for (final TrackingPathMacroSubstitutor substitutor : substitutors) {
              substitutor.invalidateUnknownMacros(macros2invalidate);
            }

            final UnknownMacroNotification[] notifications =
              NotificationsManager.getNotificationsManager().getNotificationsOfType(UnknownMacroNotification.class, this);
            for (final UnknownMacroNotification notification : notifications) {
              if (macros2invalidate.containsAll(notification.getMacros())) notification.expire();
            }

            ApplicationManager.getApplication().runWriteAction(new Runnable() {
              public void run() {
                stateStore.reinitComponents(components, true);
              }
            });
          }
          else {
            if (Messages.showYesNoDialog(this, "Component could not be reloaded. Reload project?", "Configuration changed",
                                         Messages.getQuestionIcon()) == 0) {
              ProjectManagerEx.getInstanceEx().reloadProject(this);
            }
          }
        }
      }
    }
  }

  @Override
   public String toString() {
    return "Project "
           + (isDisposed() ? "(Disposed) " : "")
           + (isDefault() ? "(Default) " : "'" + getLocation()+"'")
      ;
  }

  public static void dropUnableToSaveProjectNotification(@NotNull final Project project, final VirtualFile[] readOnlyFiles) {
    final UnableToSaveProjectNotification[] notifications =
      NotificationsManager.getNotificationsManager().getNotificationsOfType(UnableToSaveProjectNotification.class, project);
    if (notifications.length == 0) {
      Notifications.Bus.notify(new UnableToSaveProjectNotification(project, readOnlyFiles), NotificationDisplayType.STICKY_BALLOON, project);
    }
  }

  public static class UnableToSaveProjectNotification extends Notification {
    private Project myProject;
    private final String[] myFileNames;

    private UnableToSaveProjectNotification(@NotNull final Project project, final VirtualFile[] readOnlyFiles) {
      super("Project Settings", "Could not save project!", buildMessage(), NotificationType.ERROR, new NotificationListener() {
        @Override
        public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
          final UnableToSaveProjectNotification unableToSaveProjectNotification = (UnableToSaveProjectNotification)notification;
          final Project _project = unableToSaveProjectNotification.getProject();
          notification.expire();

          if (_project != null && !_project.isDisposed()) {
            _project.save();
          }
        }
      });

      myProject = project;
      myFileNames = ContainerUtil.map(readOnlyFiles, new Function<VirtualFile, String>() {
        @Override
        public String fun(VirtualFile file) {
          return file.getPath();
        }
      }, new String[readOnlyFiles.length]);
    }

    public String[] getFileNames() {
      return myFileNames;
    }

    private static String buildMessage() {
      final StringBuffer sb = new StringBuffer(
        "<p>Unable to save project files. Please ensure project files are writable and you have permissions to modify them.");
      return sb.append(" <a href=\"\">Try to save project again</a>.</p>").toString();
    }

    public Project getProject() {
      return myProject;
    }

    @Override
    public void expire() {
      myProject = null;
      super.expire();
    }
  }
}
