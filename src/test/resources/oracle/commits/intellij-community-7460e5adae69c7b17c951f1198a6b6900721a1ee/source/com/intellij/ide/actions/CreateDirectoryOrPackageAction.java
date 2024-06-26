package com.intellij.ide.actions;

import com.intellij.ide.IdeView;
import com.intellij.ide.util.PackageUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.DataConstantsEx;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.localVcs.LocalVcs;
import com.intellij.openapi.localVcs.LvcsAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.Icons;

import java.io.File;
import java.util.StringTokenizer;

public class CreateDirectoryOrPackageAction extends AnAction {
  public CreateDirectoryOrPackageAction() {
    super("Create new directory or package", "Create new directory or package", null);
  }

  public void actionPerformed(AnActionEvent e) {
    DataContext dataContext = e.getDataContext();

    IdeView view = (IdeView)dataContext.getData(DataConstantsEx.IDE_VIEW);
    Project project = (Project)dataContext.getData(DataConstants.PROJECT);

    PsiDirectory directory = PackageUtil.getOrChooseDirectory(view);

    if (directory == null) return;
    boolean isDirectory = directory.getPackage() == null;

    MyInputValidator validator = new MyInputValidator(project, directory, isDirectory);
    Messages.showInputDialog(project,
                             "Enter new " + (isDirectory ? "directory" : "package") + " name:",
                             "New " + (isDirectory ? "Directory" : "Package"),
                             Messages.getQuestionIcon(),
                             "",
                             validator);

    if (validator.myCreatedElement == null) return;

    view.selectElement(validator.myCreatedElement);
  }

  public void update(AnActionEvent event){
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();

    Project project = (Project)dataContext.getData(DataConstants.PROJECT);
    if (project == null) {
      presentation.setVisible(false);
      presentation.setEnabled(false);
      return;
    }

    IdeView view = (IdeView)dataContext.getData(DataConstantsEx.IDE_VIEW);
    if (view == null) {
      presentation.setVisible(false);
      presentation.setEnabled(false);
      return;
    }

    final PsiDirectory[] directories = view.getDirectories();
    if (directories.length == 0){
      presentation.setVisible(false);
      presentation.setEnabled(false);
      return;
    }

    presentation.setVisible(true);
    presentation.setEnabled(true);

    boolean isPackage = false;
    for (int i = 0; i < directories.length; i++) {
      if (directories[i].getPackage() != null) {
        isPackage = true;
        break;
      }
    }

    if (isPackage) {
      presentation.setText("Package");
      presentation.setIcon(Icons.PACKAGE_ICON);
    }
    else {
      presentation.setText("Directory");
      presentation.setIcon(Icons.DIRECTORY_OPEN_ICON);
    }
  }

  protected class MyInputValidator implements InputValidator {
    private Project myProject;
    private PsiDirectory myDirectory;
    private boolean myIsDirectory;
    private PsiElement myCreatedElement = null;

    public MyInputValidator(Project project, PsiDirectory directory, boolean isDirectory) {
      myProject = project;
      myDirectory = directory;
      myIsDirectory = isDirectory;
    }

    public boolean checkInput(String inputString){
      return true;
    }

    public boolean canClose(String inputString){
      final String subDirName = inputString;

      if (subDirName.length() == 0) {
        Messages.showMessageDialog(myProject,"A name should be specified", "Error", Messages.getErrorIcon());
        return false;
      }

      //[ven] valentin thinks this is too restrictive
      /*if (!myIsDirectory) {
        PsiNameHelper helper = PsiManager.getInstance(myProject).getNameHelper();
        if (!helper.isQualifiedName(subDirName)) {
          Messages.showMessageDialog(myProject, "A valid package name should be specified", "Error", Messages.getErrorIcon());
          return false;
        }
      }*/

      final boolean multiCreation = !myIsDirectory && subDirName.indexOf('.') != -1;
      if (!multiCreation) {
        try {
          myDirectory.checkCreateSubdirectory(subDirName);
        }
        catch (IncorrectOperationException ex) {
          Messages.showMessageDialog(
            myProject,
            CreateElementActionBase.filterMessage(ex.getMessage()),
            "Error",
            Messages.getErrorIcon());
          return false;
        }
      }

      Runnable command = new Runnable() {
        public void run() {
          final Runnable run = new Runnable() {
            public void run() {
              LvcsAction lvcsAction = LvcsAction.EMPTY;
              try {
                String actionName = myIsDirectory ?
                    "Creating directory " + myDirectory.getVirtualFile().getPresentableUrl() + File.separator + subDirName
                    : "Creating package " + myDirectory.getPackage().getQualifiedName() + "." + subDirName;

                String directoryPath = myDirectory.getVirtualFile().getPath() + "/" + subDirName;

                lvcsAction = LocalVcs.getInstance(myProject).startAction(actionName, directoryPath, false);

                final PsiDirectory createdDir;
                if (myIsDirectory) {
                  createdDir = myDirectory.createSubdirectory(subDirName);
                }
                else {
                  StringTokenizer tokenizer = new StringTokenizer(subDirName, ".");
                  PsiDirectory dir = myDirectory;
                  while(tokenizer.hasMoreTokens()) {
                    String packName = tokenizer.nextToken();
                    if (tokenizer.hasMoreTokens()) {
                      PsiDirectory existingDir = dir.findSubdirectory(packName);
                      if (existingDir != null) {
                        dir = existingDir;
                        continue;
                      }
                    }
                    dir = dir.createSubdirectory(packName);
                  }
                  createdDir = dir;
                }


                myCreatedElement = createdDir;

              } catch (final IncorrectOperationException ex) {
                ApplicationManager.getApplication().invokeLater(new Runnable(){
                              public void run() {
                                Messages.showMessageDialog(
                                  myProject,
                                  CreateElementActionBase.filterMessage(ex.getMessage()),
                                  "Error",
                                  Messages.getErrorIcon()
                                );
                              }
                            });
                return;
              }
              finally {
                lvcsAction.finish();
              }
            }
          };
          ApplicationManager.getApplication().runWriteAction(run);
        }
      };
      CommandProcessor.getInstance().executeCommand(myProject, command, "Create " + (myIsDirectory ? "directory" : "package"), null);

      return myCreatedElement != null;
    }
  }
}
