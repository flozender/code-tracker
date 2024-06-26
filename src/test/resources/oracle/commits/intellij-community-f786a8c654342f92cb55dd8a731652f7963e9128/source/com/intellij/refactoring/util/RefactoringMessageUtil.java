
package com.intellij.refactoring.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.usageView.UsageViewUtil;
import org.jetbrains.annotations.NonNls;

import java.util.*;

public class RefactoringMessageUtil {
  public static void showErrorMessage(String title, String message, String helpId, Project project) {
    RefactoringMessageDialog dialog=new RefactoringMessageDialog(title,message,helpId,"OptionPane.errorIcon",false, project);
    dialog.show();
  }

  public static boolean checkReadOnlyStatus(Project project, PsiElement element) {
    return checkReadOnlyStatus(element, project, RefactoringBundle.message("refactoring.cannot.be.performed"));
  }

  public static boolean checkReadOnlyStatus(PsiElement element, Project project, String messagePrefix) {
    return checkReadOnlyStatus(Collections.singleton(element), project, messagePrefix, false);
  }

  public static boolean checkReadOnlyStatusRecursively (Project project, Collection<PsiElement> element) {
    return checkReadOnlyStatus(element, project, RefactoringBundle.message("refactoring.cannot.be.performed"), true);
  }

  private static boolean checkReadOnlyStatus(Collection<PsiElement> elements,
                                             Project project,
                                             final String messagePrefix,
                                             boolean recursively
    ) {
    //Not writable, but could be checked out
    final List<VirtualFile> readonly = new ArrayList<VirtualFile>();
    //Those located in jars
    final List<VirtualFile> failed = new ArrayList<VirtualFile>();

    for (PsiElement element : elements) {
      if (element.isWritable()) continue;

      if (element instanceof PsiDirectory) {
        PsiDirectory dir = (PsiDirectory)element;
        final VirtualFile vFile = dir.getVirtualFile();
        if (vFile.getFileSystem() instanceof JarFileSystem) {
          /*String message1 = messagePrefix + ".\n Directory " + vFile.getPresentableUrl() + " is located in a jar file.";
         showErrorMessage("Cannot Modify Jar", message1, null, project);
         return false;*/
          failed.add(vFile);
        }
        else {
          if (recursively) {
            addVirtualFiles(vFile, readonly);

          }
          else {
            readonly.add(vFile);
          }
        }
      }
      else if (element instanceof PsiPackage) {
        final PsiDirectory[] directories = ((PsiPackage)element).getDirectories();
        for (PsiDirectory directory : directories) {
          VirtualFile virtualFile = directory.getVirtualFile();
          if (recursively) {
            if (virtualFile.getFileSystem() instanceof JarFileSystem) {
              failed.add(virtualFile);
            }
            else {
              addVirtualFiles(virtualFile, readonly);
            }
          }
          else {
            if (!directory.isWritable()) {
              if (virtualFile.getFileSystem() instanceof JarFileSystem) {
                failed.add(virtualFile);
              }
              else {
                readonly.add(virtualFile);
              }
            }
          }
        }
      }
      else if (element instanceof PsiCompiledElement) {
        final PsiFile file = element.getContainingFile();
        if (file != null) {
          failed.add(file.getVirtualFile());
        }
      }
      else {
        PsiFile file = element.getContainingFile();
        if (!file.isWritable()) {
          final VirtualFile vFile = file.getVirtualFile();
          if (vFile != null) {
            readonly.add(vFile);
          }
        }
      }
    }

    final ReadonlyStatusHandler.OperationStatus status = ReadonlyStatusHandler.getInstance(project)
      .ensureFilesWritable(readonly.toArray(new VirtualFile[readonly.size()]));
    failed.addAll(Arrays.asList(status.getReadonlyFiles()));
    if (failed.size() > 0) {
      StringBuffer message = new StringBuffer(messagePrefix);
      message.append('\n');
      for (VirtualFile virtualFile : failed) {
        final String presentableUrl = virtualFile.getPresentableUrl();
        final String subj = virtualFile.isDirectory() ?
                            RefactoringBundle.message("directory.description", presentableUrl) :
                            RefactoringBundle.message("file.description", presentableUrl);
        if (virtualFile.getFileSystem() instanceof JarFileSystem) {
          message.append(RefactoringBundle.message("0.is.located.in.a.jar.file", subj));
        }
        else {
          message.append(RefactoringBundle.message("0.is.read.only", subj + presentableUrl));
        }
      }
      return false;
    }

    return true;
  }

  private static void addVirtualFiles(final VirtualFile vFile, final List<VirtualFile> list) {
    if (!vFile.isWritable()) {
      list.add(vFile);
    }
    final VirtualFile[] children = vFile.getChildren();
    if (children != null) {
      for (VirtualFile virtualFile : children) {
        addVirtualFiles(virtualFile, list);
      }
    }
  }

  public static String getIncorrectIdentifierMessage(String identifierName) {
    return RefactoringBundle.message("0.is.not.a.legal.java.identifier", identifierName);
  }

  /**
   * @return null, if can create a class
   *         an error message, if cannot create a class
   *
   */
  public static String checkCanCreateClass(PsiDirectory destinationDirectory, String className) {
    PsiClass[] classes = destinationDirectory.getClasses();
    VirtualFile file = destinationDirectory.getVirtualFile();
    for (PsiClass aClass : classes) {
      if (className.equals(aClass.getName())) {
        return RefactoringBundle.message("directory.0.already.contains.1.named.2",
                                         file.getPresentableUrl(), UsageViewUtil.getType(aClass), className);
      }
    }
    @NonNls String fileName = className+".java";
    return checkCanCreateFile(destinationDirectory, fileName);
  }
  public static String checkCanCreateFile(PsiDirectory destinationDirectory, String fileName) {
    VirtualFile file = destinationDirectory.getVirtualFile();
    VirtualFile child = file.findChild(fileName);
    if (child != null) {
      return RefactoringBundle.message("directory.0.already.contains.a.file.named.1",
                                       file.getPresentableUrl(), fileName);
    }
    return null;
  }

  public static String getGetterSetterMessage(String newName, String action, PsiMethod getter, PsiMethod setter) {
    String text;
    if (getter != null && setter != null) {
      text = RefactoringBundle.message("getter.and.setter.methods.found.for.the.field.0", newName, action);
    } else if (getter != null) {
      text = RefactoringBundle.message("getter.method.found.for.the.field.0", newName, action);
    } else {
      text = RefactoringBundle.message("setter.method.found.for.the.field.0", newName, action);
    }
    return text;
  }

}