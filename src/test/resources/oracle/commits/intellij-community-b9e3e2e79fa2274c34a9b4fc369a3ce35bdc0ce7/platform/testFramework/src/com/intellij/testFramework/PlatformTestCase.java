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
package com.intellij.testFramework;

import com.intellij.codeInsight.completion.CompletionProgressIndicator;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.ide.highlighter.ProjectFileType;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.idea.IdeaLogger;
import com.intellij.idea.IdeaTestApplication;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.impl.UndoManagerImpl;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.impl.EditorFactoryImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.EmptyModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.impl.ModuleManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.project.impl.TooManyProjectLeakedException;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.impl.VirtualFilePointerManagerImpl;
import com.intellij.openapi.vfs.impl.local.LocalFileSystemImpl;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.impl.PsiDocumentManagerImpl;
import com.intellij.util.PatchedWeakReference;
import junit.framework.TestCase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author yole
 */
public abstract class PlatformTestCase extends UsefulTestCase implements DataProvider {
  protected static final String PROFILE = "Configurable";
  protected static IdeaTestApplication ourApplication;
  protected boolean myRunCommandForTest = false;
  protected ProjectManagerEx myProjectManager;
  protected Project myProject;
  protected Module myModule;
  protected static final Collection<File> myFilesToDelete = new HashSet<File>();
  protected boolean myAssertionsInTestDetected;
  protected static final Logger LOG = Logger.getInstance("#com.intellij.testFramework.PlatformTestCase");
  public static Thread ourTestThread;
  private static TestCase ourTestCase = null;
  public static final long DEFAULT_TEST_TIME = 300L;
  public static long ourTestTime = DEFAULT_TEST_TIME;
  private static final MyThreadGroup MY_THREAD_GROUP = new MyThreadGroup();
  private static final String ourOriginalTempDir = FileUtil.getTempDirectory();
  private EditorListenerTracker myEditorListenerTracker;
  private String myTempDirPath;
  private ThreadTracker myThreadTracker;

  static {
    Logger.setFactory(TestLoggerFactory.getInstance());
  }

  protected static long getTimeRequired() {
    return DEFAULT_TEST_TIME;
  }

  @Nullable
  protected String getApplicationConfigDirPath() throws Exception {
    return null;
  }

  protected void initApplication() throws Exception {
    boolean firstTime = ourApplication == null;
    ourApplication = IdeaTestApplication.getInstance(getApplicationConfigDirPath());
    ourApplication.setDataProvider(this);

    if (firstTime) {
      cleanPersistedVFSContent();
    }
  }

  private static void cleanPersistedVFSContent() {
    ((PersistentFS)ManagingFS.getInstance()).cleanPersistedContents();
  }

  @Override
  protected CodeStyleSettings getCurrentCodeStyleSettings() {
    return CodeStyleSettingsManager.getSettings(getProject());
  }

  protected void setUp() throws Exception {
    super.setUp();
    if (ourTestCase != null) {
      String message = "Previous test " + ourTestCase +
                       " hasn't called tearDown(). Probably overriden without super call.";
      ourTestCase = null;
      fail(message);
    }
    IdeaLogger.ourErrorsOccurred = null;

    LOG.info(getClass().getName() + ".setUp()");

    myTempDirPath = ourOriginalTempDir + "/"+getTestName(true) + "/";
    setTmpDir(myTempDirPath);
    new File(myTempDirPath).mkdir();

    initApplication();

    myEditorListenerTracker = new EditorListenerTracker();
    myThreadTracker = new ThreadTracker();

    setUpProject();
    storeSettings();
    ourTestCase = this;
  }

  public Project getProject() {
    return myProject;
  }

  public final PsiManager getPsiManager() {
    return PsiManager.getInstance(myProject);
  }

  public Module getModule() {
    return myModule;
  }

  protected void setUpProject() throws Exception {
    myProjectManager = ProjectManagerEx.getInstanceEx();
    assertNotNull("Cannot instantiate ProjectManager component", myProjectManager);

    File projectFile = getIprFile();
    LocalFileSystem.getInstance().refreshIoFiles(myFilesToDelete);

    myProject = createProject(projectFile, getClass().getName() + "." + getName());

    setUpModule();

    setUpJdk();

    ProjectManagerEx.getInstanceEx().setCurrentTestProject(myProject);

    runStartupActivities();
  }

  @Nullable
  public static Project createProject(File projectFile, String creationPlace) {
    try {
      Project project =
        ProjectManagerEx.getInstanceEx().newProject(FileUtil.getNameWithoutExtension(projectFile), projectFile.getPath(), false, false);
      assert project != null;

      project.putUserData(CREATION_PLACE, creationPlace);
      return project;
    }
    catch (TooManyProjectLeakedException e) {
      StringBuilder leakers = new StringBuilder();
      leakers.append("Too many projects leaked: \n");
      for (Project project : e.getLeakedProjects()) {
        String place = project.getUserData(CREATION_PLACE);
        leakers.append(place != null ? place : project.getBaseDir());
        leakers.append("\n");
      }

      fail(leakers.toString());
      return null;
    }
  }

  protected void runStartupActivities() {
    ((StartupManagerImpl)StartupManager.getInstance(myProject)).runStartupActivities();
    ((StartupManagerImpl)StartupManager.getInstance(myProject)).runPostStartupActivities();
  }

  protected File getIprFile() throws IOException {
    File tempFile = FileUtil.createTempFile("temp_" + getName(), ProjectFileType.DOT_DEFAULT_EXTENSION);
    myFilesToDelete.add(tempFile);
    return tempFile;
  }

  protected void setUpModule() {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        try {
          myModule = createMainModule();
        }
        catch (IOException e) {
          LOG.error(e);
        }
      }
    });
  }

  protected Module createMainModule() throws IOException {
    return createModule(myProject.getName());
  }

  protected Module createModule(@NonNls final String moduleName) {
    return doCreateRealModule(moduleName);
  }

  protected Module doCreateRealModule(final String moduleName) {
    final VirtualFile baseDir = myProject.getBaseDir();
    assertNotNull(baseDir);
    final File moduleFile = new File(baseDir.getPath().replace('/', File.separatorChar),
                                     moduleName + ModuleFileType.DOT_DEFAULT_EXTENSION);
    FileUtil.createIfDoesntExist(moduleFile);
    myFilesToDelete.add(moduleFile);
    final VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(moduleFile);
    Module module = ModuleManager.getInstance(myProject).newModule(virtualFile.getPath(), getModuleType());
    module.getModuleFile();
    return module;
  }

  protected ModuleType getModuleType() {
    return EmptyModuleType.getInstance();
  }

  private void cleanupApplicationCaches() {
    try {
      LocalFileSystemImpl localFileSystem = (LocalFileSystemImpl)LocalFileSystem.getInstance();
      if (localFileSystem != null) {
        localFileSystem.cleanupForNextTest();
      }
    }
    catch (IOException e) {
      // ignore
    }
    VirtualFilePointerManagerImpl virtualFilePointerManager = (VirtualFilePointerManagerImpl)VirtualFilePointerManager.getInstance();
    if (virtualFilePointerManager != null) {
      virtualFilePointerManager.cleanupForNextTest();
    }
    PatchedWeakReference.clearAll();
    resetAllFields();
  }

  protected void tearDown() throws Exception {
    checkAllTimersAreDisposed();
    if (myProject != null) {
      ((StartupManagerImpl)StartupManager.getInstance(myProject)).prepareForNextTest();
      final LookupManager lookupManager = LookupManager.getInstance(myProject);
      if (lookupManager != null) {
        lookupManager.hideActiveLookup();
      }

      ((PsiDocumentManagerImpl)PsiDocumentManager.getInstance(getProject())).clearUncommitedDocuments();
    }

    InspectionProfileManager.getInstance().deleteProfile(PROFILE);
    try {
      checkForSettingsDamage();

      assertNotNull("Application components damaged", ProjectManager.getInstance());

      ApplicationManager.getApplication().runWriteAction(EmptyRunnable.getInstance()); // Flash posponed formatting if any.
      FileDocumentManager.getInstance().saveAllDocuments();

      doPostponedFormatting(myProject);

      try {
        disposeProject();

        ((UndoManagerImpl)UndoManager.getGlobalInstance()).dropHistoryInTests();

        for (final File fileToDelete : myFilesToDelete) {
          delete(fileToDelete);
        }
        LocalFileSystem.getInstance().refreshIoFiles(myFilesToDelete);

        FileUtil.asyncDelete(new File(myTempDirPath));

        setTmpDir(ourOriginalTempDir);

        Throwable fromThreadGroup = MY_THREAD_GROUP.popThrowable();
        if (fromThreadGroup != null) {
          throw new RuntimeException(fromThreadGroup);
        }

        if (!myAssertionsInTestDetected) {
          if (IdeaLogger.ourErrorsOccurred != null) {
            throw IdeaLogger.ourErrorsOccurred;
          }
          assertTrue("Logger errors occurred in " + getFullName(), IdeaLogger.ourErrorsOccurred == null);
        }

        ourApplication.setDataProvider(null);

      }
      finally {
        ourTestCase = null;
      }
      CompletionProgressIndicator.cleanupForNextTest();

      super.tearDown();

      EditorFactory editorFactory = EditorFactory.getInstance();
      final Editor[] allEditors = editorFactory.getAllEditors();
      ((EditorFactoryImpl)editorFactory).validateEditorsAreReleased(getProject());
      for (Editor editor : allEditors) {
        editorFactory.releaseEditor(editor);
      }
      assertEquals(0, allEditors.length);

      //cleanTheWorld();
      myEditorListenerTracker.checkListenersLeak();
      myThreadTracker.checkLeak();
    }
    finally {
      myProjectManager = null;
      myProject = null;
      myModule = null;
      myFilesToDelete.clear();
    }
  }

  private void disposeProject() {
    if (myProject != null) {
      Disposer.dispose(myProject);
      ProjectManagerEx projectManager = ProjectManagerEx.getInstanceEx();
      if (projectManager != null) {
        projectManager.setCurrentTestProject(null);
      }
    }
  }

  protected void resetAllFields() {
    resetClassFields(getClass());
  }

  protected final <T extends Disposable> T disposeOnTearDown(T disposable) {
    Disposer.register(myProject, disposable);
    return disposable;
  }

  private void resetClassFields(final Class<?> aClass) {
    try {
      clearDeclaredFields(this, aClass);
    }
    catch (IllegalAccessException e) {
      LOG.error(e);
    }

    if (aClass == PlatformTestCase.class) return;
    resetClassFields(aClass.getSuperclass());
  }

  private String getFullName() {
    return getClass().getName() + "." + getName();
  }

  private void delete(File file) {
    boolean b = FileUtil.delete(file);
    if (!b && file.exists() && !myAssertionsInTestDetected) {
      fail("Can't delete " + file.getAbsolutePath() + " in " + getFullName());
    }
  }

  protected void simulateProjectOpen() {
    ModuleManagerImpl mm = (ModuleManagerImpl)ModuleManager.getInstance(myProject);
    StartupManagerImpl sm = (StartupManagerImpl)StartupManager.getInstance(myProject);

    mm.projectOpened();
    setUpJdk();
    sm.runStartupActivities();
    // extra init for libraries
    sm.runPostStartupActivities();
  }

  protected void setUpJdk() {
    //final ProjectJdkEx jdk = ProjectJdkUtil.getDefaultJdk("java 1.4");
    final Sdk jdk = getTestProjectJdk();
//    ProjectJdkImpl jdk = ProjectJdkTable.getInstance().addJdk(defaultJdk);
    Module[] modules = ModuleManager.getInstance(myProject).getModules();
    for (Module module : modules) {
      final ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        public void run() {
          final ModifiableRootModel rootModel = rootManager.getModifiableModel();
          rootModel.setSdk(jdk);
          rootModel.commit();
        }
      });
    }
  }

  protected Sdk getTestProjectJdk() {
    return null;
  }

  public void runBare() throws Throwable {
    final Throwable[] throwable = new Throwable[1];

    Thread thread = new Thread(MY_THREAD_GROUP, new Runnable() {
      public void run() {
        try {
          runBareImpl();
        }
        catch (Throwable th) {
          throwable[0] = th;
        } finally {
          try {
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
              public void run() {
                cleanupApplicationCaches();
              }
            }, ModalityState.NON_MODAL);
          }
          catch (Throwable e) {
            // Ignore
          }
        }
      }
    }, "IDEA Test Case Thread");
    thread.start();
    thread.join();

    if (throwable[0] != null) {
      throw throwable[0];
    }
  }

  private void runBareImpl() throws Throwable {
    final Throwable[] throwables = new Throwable[1];
    Runnable runnable = new Runnable() {
      public void run() {
        ourTestThread = Thread.currentThread();
        ourTestTime = getTimeRequired();
        try {
          try {
            setUp();
          }
          catch (Throwable e) {
            disposeProject();
            throw e;
          }
          try {
            myAssertionsInTestDetected = true;
            runTest();
            myAssertionsInTestDetected = false;
          }
          finally {
            try {
              tearDown();
            }
            catch (Throwable th) {
              th.printStackTrace();
            }
          }
        }
        catch (Throwable throwable) {
          throwables[0] = throwable;
        }
        finally {
          ourTestThread = null;
        }
      }
    };

    runBareRunnable(runnable);

    if (IdeaLogger.ourErrorsOccurred != null) {
      throw IdeaLogger.ourErrorsOccurred;
    }

    if (throwables[0] != null) {
      throw throwables[0];
    }

    // just to make sure all deffered Runnable's to finish
    waitForAllLaters();
    if (IdeaLogger.ourErrorsOccurred != null) {
      throw IdeaLogger.ourErrorsOccurred;
    }
  }

  private static void waitForAllLaters() throws InterruptedException, InvocationTargetException {
    for (int i = 0; i < 3; i++) {
      SwingUtilities.invokeAndWait(EmptyRunnable.getInstance());
    }
  }

  protected void runBareRunnable(Runnable runnable) throws Throwable {
    SwingUtilities.invokeAndWait(runnable);
  }

  protected void runTest() throws Throwable {
    /*
    Method runMethod = null;
    try {
      runMethod = getClass().getMethod(getName(), new Class[0]);
    }
    catch (NoSuchMethodException e) {
      fail("Method \"" + getName() + "\" not found");
    }
    if (runMethod != null && !Modifier.isPublic(runMethod.getModifiers())) {
      fail("Method \"" + getName() + "\" should be public");
    }

    final Method method = runMethod;
    */

    final Throwable[] throwables = new Throwable[1];

    Runnable runnable = new Runnable() {
      public void run() {
        try {
          PlatformTestCase.super.runTest();
          /*
          method.invoke(IdeaTestCase.this, new Class[0]);
          */
        }
        catch (InvocationTargetException e) {
          e.fillInStackTrace();
          throwables[0] = e.getTargetException();
        }
        catch (IllegalAccessException e) {
          e.fillInStackTrace();
          throwables[0] = e;
        }
        catch (Throwable e) {
          throwables[0] = e;
        }
      }
    };

    invokeTestRunnable(runnable);

    if (throwables[0] != null) {
      throw throwables[0];
    }
  }

  protected boolean isRunInWriteAction() {
    return true;
  }

  protected void invokeTestRunnable(final Runnable runnable) throws Exception {
    final Exception[] e = new Exception[1];
    Runnable runnable1 = new Runnable() {
      public void run() {
        try {
          if (ApplicationManager.getApplication().isDispatchThread() && isRunInWriteAction()) {
            ApplicationManager.getApplication().runWriteAction(runnable);
          }
          else {
            runnable.run();
          }
        }
        catch (Exception e1) {
          e[0] = e1;
        }
      }
    };

    if (myRunCommandForTest) {
      CommandProcessor.getInstance().executeCommand(myProject, runnable1, "", null);
    }
    else {
      runnable1.run();
    }

    if (e[0] != null) {
      throw e[0];
    }
  }

  public Object getData(String dataId) {
    if (dataId.equals(DataConstants.PROJECT)) {
      return myProject;
    }
    else if (dataId.equals(DataConstants.EDITOR)) {
      return FileEditorManager.getInstance(myProject).getSelectedTextEditor();
    }
    else {
      return null;
    }
  }

  public static File createTempDir(@NonNls final String prefix) throws IOException {
    final File tempDirectory = FileUtil.createTempDirectory(prefix, null);
    myFilesToDelete.add(tempDirectory);
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        VirtualFileManager.getInstance().refresh(false);
      }
    });

    return tempDirectory;
  }

  protected static VirtualFile getVirtualFile(final File file) {
    return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
  }

  protected File createTempDirectory() throws IOException {
    File dir = FileUtil.createTempDirectory(getTestName(true), null);
    myFilesToDelete.add(dir);
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        VirtualFileManager.getInstance().refresh(false);
      }
    });
    return dir;
  }

  protected PsiFile getPsiFile(final Document document) {
    return PsiDocumentManager.getInstance(getProject()).getPsiFile(document);
  }

  private static void setTmpDir(String path) {
    System.setProperty("java.io.tmpdir", path);
    Class<File> ioFile = File.class;
    try {
      Field field = ioFile.getDeclaredField("tmpdir");

      field.setAccessible(true);
      field.set(ioFile, null);
    }
    catch (NoSuchFieldException ignore) {
      // field was removed in JDK 1.6.0_12
    }
    catch (IllegalAccessException e) {
      LOG.error(e);
    }
  }

  private static class MyThreadGroup extends ThreadGroup {
    private Throwable myThrowable;
    @NonNls private static final String IDEATEST_THREAD_GROUP = "IDEATest";

    private MyThreadGroup() {
      super(IDEATEST_THREAD_GROUP);
    }

    public void uncaughtException(Thread t, Throwable e) {
      myThrowable = e;
      super.uncaughtException(t, e);
    }

    public Throwable popThrowable() {
      try {
        return myThrowable;
      }
      finally {
        myThrowable = null;
      }
    }
  }
}
