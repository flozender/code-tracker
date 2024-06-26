package com.intellij.refactoring;

import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.localVcs.LvcsAction;
import com.intellij.openapi.localVcs.impl.LvcsIntegration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.refactoring.listeners.RefactoringListenerManager;
import com.intellij.refactoring.listeners.impl.RefactoringListenerManagerImpl;
import com.intellij.refactoring.listeners.impl.RefactoringTransaction;
import com.intellij.refactoring.ui.ConflictsDialog;
import com.intellij.usageView.FindUsagesCommand;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.usageView.UsageViewUtil;
import com.intellij.usages.*;
import com.intellij.usages.rules.PsiElementUsage;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.HashSet;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

public abstract class BaseRefactoringProcessor {
  private static final Logger LOG = Logger.getInstance("#com.intellij.refactoring.BaseRefactoringProcessor");
  public static final Runnable EMPTY_CALLBACK = EmptyRunnable.getInstance();
  protected final Project myProject;

  private RefactoringTransaction myTransaction;
  private boolean myIsPreviewUsages;
  protected Runnable myPrepareSuccessfulSwingThreadCallback = EMPTY_CALLBACK;


  protected BaseRefactoringProcessor(Project project) {
    this(project, null);
  }

  protected BaseRefactoringProcessor(Project project, Runnable prepareSuccessfulCallback) {
    myProject = project;
    myPrepareSuccessfulSwingThreadCallback = prepareSuccessfulCallback;
  }

  protected abstract UsageViewDescriptor createUsageViewDescriptor(UsageInfo[] usages, FindUsagesCommand refreshCommand);

  /**
   * Is called inside atomic action.
   */
  @NotNull protected abstract UsageInfo[] findUsages();

  /**
   * is called when usage search is re-run.
   * @param elements - refreshed elements that are returned by UsageViewDescriptor.getElements()
   */
  protected abstract void refreshElements(PsiElement[] elements);

  /**
   * Is called inside atomic action.
   * @param refUsages
   */
  protected boolean preprocessUsages(Ref<UsageInfo[]> refUsages) {
    prepareSuccessful();
    return true;
  }

  /**
   * Is called inside atomic action.
   */
  protected boolean isPreviewUsages(UsageInfo[] usages) {
    if (UsageViewUtil.hasReadOnlyUsages(usages)) {
      WindowManager.getInstance().getStatusBar(myProject).setInfo(RefactoringBundle.message("readonly.occurences.found"));
      return true;
    }
    return myIsPreviewUsages;
  }

  boolean isPreviewUsages() {
    return myIsPreviewUsages;
  }


  public void setPreviewUsages(boolean isPreviewUsages) {
    myIsPreviewUsages = isPreviewUsages;
  }

  public void setPrepareSuccessfulSwingThreadCallback(Runnable prepareSuccessfulSwingThreadCallback) {
    myPrepareSuccessfulSwingThreadCallback = prepareSuccessfulSwingThreadCallback;
  }

  protected RefactoringTransaction getTransaction() {
    return myTransaction;
  }

  /**
   * Is called in a command and inside atomic action.
   */
  protected abstract void performRefactoring(UsageInfo[] usages);

  /**
   * If this method returns <code>true</code>, it means that element to rename is variable, and in the findUsages tree it
   * will be shown with read-access or write-access icons.
   * @return true if element to rename is variable(local, field, or parameter).
   */
  protected boolean isVariable() {
    return false;
  }

  protected abstract String getCommandName();

  protected void doRun() {

    final Ref<UsageInfo[]> refUsages = new Ref<UsageInfo[]>();

    final Runnable findUsagesRunnable = new Runnable() {
      public void run() {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
          public void run() {
            refUsages.set(findUsages());
          }
        });
      }
    };

    if (!ApplicationManager.getApplication().runProcessWithProgressSynchronously(findUsagesRunnable, RefactoringBundle.message("progress.text"), true, myProject)) return;

    LOG.assertTrue(!refUsages.isNull());
    if (!preprocessUsages(refUsages)) return;
    final UsageInfo[] usages = refUsages.get();
    if (!myIsPreviewUsages) ensureFilesWritable(usages);
    boolean toPreview = isPreviewUsages(usages);
    if (toPreview) {
      FindUsagesCommand findUsagesCommand = new FindUsagesCommand() {
        public UsageInfo[] execute(PsiElement[] elementsToSearch) {
          refreshElements(elementsToSearch);
          findUsagesRunnable.run();
          return refUsages.get();
        }
      };
      UsageViewDescriptor descriptor = createUsageViewDescriptor(usages, findUsagesCommand);
      showUsageView(descriptor, isVariable(), isVariable());
    } else {
      execute(usages);
    }
  }

  private void ensureFilesWritable(UsageInfo[] usages) {
    Set<VirtualFile> files = new HashSet<VirtualFile>();
    for (UsageInfo usage : usages) {
      final PsiFile file = usage.getElement().getContainingFile();
      final VirtualFile virtualFile = file.getVirtualFile();
      if (virtualFile != null) {
        files.add(virtualFile);
      }
    }
    ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(files.toArray(new VirtualFile[files.size()]));
  }

  void execute(final UsageInfo[] usages) {
    CommandProcessor.getInstance().executeCommand(
        myProject, new Runnable() {
          public void run() {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
              public void run() {
                doRefactoring(usages, new HashSet<UsageInfo>());
              }
            });
          }
        },
        getCommandName(),
        null
    );
  }

  private static UsageViewPresentation createPresentation(UsageViewDescriptor descriptor, final Usage[] usages) {
    UsageViewPresentation presentation = new UsageViewPresentation();
    presentation.setTabText(RefactoringBundle.message("usageView.tabText"));
    presentation.setTargetsNodeText(descriptor.getProcessedElementsHeader());
    presentation.setShowReadOnlyStatusAsRed(true);
    presentation.setShowCancelButton(true);
    presentation.setUsagesString(RefactoringBundle.message("usageView.usagesText"));
    int codeUsageCount = 0;
    int nonCodeUsageCount = 0;
    Set<PsiFile> codeFiles = new HashSet<PsiFile>();
    Set<PsiFile> nonCodeFiles = new HashSet<PsiFile>();

    for (Usage usage : usages) {
      if (usage instanceof PsiElementUsage) {
        final PsiElementUsage elementUsage = ((PsiElementUsage)usage);
        if (elementUsage.isNonCodeUsage()) {
          nonCodeUsageCount++;
          nonCodeFiles.add(elementUsage.getElement().getContainingFile());
        }
        else {
          codeUsageCount++;
          codeFiles.add(elementUsage.getElement().getContainingFile());
        }
      }
    }
    codeFiles.remove(null);
    nonCodeFiles.remove(null);

    presentation.setCodeUsagesString(descriptor.getCodeReferencesText(codeUsageCount, codeFiles.size()));
    presentation.setNonCodeUsagesString(descriptor.getCommentReferencesText(nonCodeUsageCount, nonCodeFiles.size()));
    return presentation;
  }

  private void showUsageView(final UsageViewDescriptor viewDescriptor, boolean showReadAccessIcon, boolean showWriteAccessIcon) {
    UsageViewManager viewManager = myProject.getComponent(UsageViewManager.class);

    final PsiElement[] initialElements = viewDescriptor.getElements();
    final UsageTarget[] targets = PsiElement2UsageTargetAdapter.convert(initialElements);
    final Usage[] usages = UsageInfo2UsageAdapter.convert(viewDescriptor.getUsages());

    final UsageViewPresentation presentation = createPresentation(viewDescriptor, usages);

    final UsageView usageView = viewManager.showUsages(targets, usages, presentation);

    final Runnable refactoringRunnable = new Runnable() {
      public void run() {
        final Set<UsageInfo> excludedUsageInfos = getExcludedUsages(usageView);
        doRefactoring(viewDescriptor.getUsages(), excludedUsageInfos);
      }
    };

    String canNotMakeString = RefactoringBundle.message("usageView.need.reRun");

    usageView.addPerformOperationAction(refactoringRunnable, getCommandName(), canNotMakeString, RefactoringBundle.message("usageView.doAction"));
  }

  private static Set<UsageInfo> getExcludedUsages(final UsageView usageView) {
    Set<Usage> usages = usageView.getExcludedUsages();

    Set<UsageInfo> excludedUsageInfos = new HashSet<UsageInfo>();
    for (Usage usage : usages) {
      if (usage instanceof UsageInfo2UsageAdapter) {
        excludedUsageInfos.add(((UsageInfo2UsageAdapter)usage).getUsageInfo());
      }
    }
    return excludedUsageInfos;
  }

  private void doRefactoring(UsageInfo[] usages, Set<UsageInfo> excludedUsages) {
    if (usages != null) {
      ArrayList<UsageInfo> array = new ArrayList<UsageInfo>(Arrays.asList(usages));
      array.removeAll(excludedUsages);

      for (Iterator<UsageInfo> iterator = array.iterator(); iterator.hasNext();) {
        final PsiElement element = iterator.next().getElement();
        if (element == null || !element.isWritable()) iterator.remove();
      }
      usages = array.toArray(new UsageInfo[array.size()]);
    }

    LvcsAction action = LvcsIntegration.checkinFilesBeforeRefactoring(myProject, getCommandName());

    try {
      final UsageInfo[] _usages = usages;
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        public void run() {
          PsiDocumentManager.getInstance(myProject).commitAllDocuments();
          RefactoringListenerManagerImpl listenerManager =
              (RefactoringListenerManagerImpl) RefactoringListenerManager.getInstance(myProject);
          myTransaction = listenerManager.startTransaction();
          Set<PsiJavaFile> touchedJavaFiles = getTouchedJavaFiles(_usages);
          performRefactoring(_usages);
          removeRedundantImports(touchedJavaFiles);
          myTransaction.commit();
          performPsiSpoilingRefactoring();
        }
      });
    } finally {
        LvcsIntegration.checkinFilesAfterRefactoring(myProject, action);
    }

    if (usages != null) {
      int count = usages.length;
      if (count > 0) {
        WindowManager.getInstance().getStatusBar(myProject).setInfo(RefactoringBundle.message("statusBar.refactoring.result", count));
      } else {
        if (!isPreviewUsages(usages)) {
          WindowManager.getInstance().getStatusBar(myProject).setInfo(RefactoringBundle.message("statusBar.noUsages"));
        }
      }
    }
  }

  private void removeRedundantImports(final Set<PsiJavaFile> javaFiles) {
    final CodeStyleManager styleManager = PsiManager.getInstance(myProject).getCodeStyleManager();
    for (PsiJavaFile file : javaFiles) {
      try {
        if (file.getVirtualFile() != null) {
          styleManager.removeRedundantImports(file);
        }
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }
  }

  private Set<PsiJavaFile> getTouchedJavaFiles(final UsageInfo[] usages) {
    Set<PsiJavaFile> javaFiles = new HashSet<PsiJavaFile>();
    for (UsageInfo usage : usages) {
      final PsiElement element = usage.getElement();
      if (element != null) {
        final PsiFile file = element.getContainingFile();
        if (file instanceof PsiJavaFile) {
          javaFiles.add((PsiJavaFile)file);
        }
      }
    }
    return javaFiles;
  }

  /**
   * Refactorings that spoil PSI (write something directly to documents etc.) should
   * do that in this method.<br>
   * This method is called immediately after
   * <code>{@link #performRefactoring(com.intellij.usageView.UsageInfo[])}</code>.
   */
  protected void performPsiSpoilingRefactoring() {

  }

  protected void prepareSuccessful() {
    if (myPrepareSuccessfulSwingThreadCallback != null) {
      // make sure that dialog is closed in swing thread
      if (!ApplicationManager.getApplication().isDispatchThread()) {
        try {
          SwingUtilities.invokeAndWait(myPrepareSuccessfulSwingThreadCallback);
        } catch (InterruptedException e) {
          LOG.error(e);
        } catch (InvocationTargetException e) {
          LOG.error(e);
        }
      } else {
        myPrepareSuccessfulSwingThreadCallback.run();
      }
//      ToolWindowManager.getInstance(myProject).invokeLater(myPrepareSuccessfulSwingThreadCallback);
    }
  }

  /**
   * Override in subclasses
   */
  protected void prepareTestRun() {

  }

  public final void  run() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      testRun();
    }
    else {
      doRun();
    }
  }

  private final void  testRun() {
    PsiDocumentManager.getInstance(myProject).commitAllDocuments();
    prepareTestRun();
    Ref<UsageInfo[]> refUsages = new Ref<UsageInfo[]>(findUsages());
    preprocessUsages(refUsages);
    RefactoringListenerManagerImpl listenerManager =
        (RefactoringListenerManagerImpl) RefactoringListenerManager.getInstance(myProject);
    myTransaction = listenerManager.startTransaction();
    final UsageInfo[] usages = refUsages.get();
    Set<PsiJavaFile> touchedJavaFiles = getTouchedJavaFiles(usages);
    performRefactoring(usages);
    removeRedundantImports(touchedJavaFiles);
    myTransaction.commit();
    performPsiSpoilingRefactoring();
  }

  protected boolean showConflicts(final ArrayList<String> conflicts) {
    if (conflicts.size() > 0 && myPrepareSuccessfulSwingThreadCallback != null) {
      final ConflictsDialog conflictsDialog = new ConflictsDialog(myProject, conflicts);
      conflictsDialog.show();
      if (!conflictsDialog.isOK()) return false;
    }

    prepareSuccessful();
    return true;
  }
}