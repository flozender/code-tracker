package com.intellij.compiler;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.Nullable;

public final class CompilerMessageImpl implements CompilerMessage {

  private final Project myProject;
  private final CompilerMessageCategory myCategory;
  @Nullable private Navigatable myNavigatable;
  private final String myMessage;
  private VirtualFile myFile;
  private final int myRow;
  private final int myColumn;

  public CompilerMessageImpl(Project project,
                             CompilerMessageCategory category,
                             String message,
                             final String url,
                             int row,
                             int column,
                             @Nullable final Navigatable navigatable) {
    myProject = project;
    myCategory = category;
    myNavigatable = navigatable;
    myMessage = message == null ? "" : message;
    myRow = row;
    myColumn = column;
    ApplicationManager.getApplication().runReadAction(new Runnable() {
      public void run() {
        myFile = url == null ? null : VirtualFileManager.getInstance().findFileByUrl(url);
      }
    });
  }

  public CompilerMessageCategory getCategory() {
    return myCategory;
  }

  public String getMessage() {
    return myMessage;
  }

  public Navigatable getNavigatable() {
    if (myNavigatable != null) {
      return myNavigatable;
    }
    final VirtualFile virtualFile = getVirtualFile();
    if (virtualFile != null && virtualFile.isValid()) {
      final int line = getLine() - 1; // editor lines are zero-based
      if (line >= 0) {
        return myNavigatable = new OpenFileDescriptor(myProject, virtualFile, line, Math.max(0, getColumn()-1));
      }
    }
    return null;
  }

  public VirtualFile getVirtualFile() {
    return myFile;
  }

  public String getExportTextPrefix() {
    if (getLine() >= 0) {
      return CompilerBundle.message("compiler.results.export.text.prefix", getLine());
    }
    return "";
  }

  public String getRenderTextPrefix() {
    if (getLine() >= 0) {
      return "(" + getLine() + ", " + getColumn() + ")";
    }
    return "";
  }

  public int getLine() {
    return myRow;
  }

  public int getColumn() {
    return myColumn;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CompilerMessage)) return false;

    final CompilerMessageImpl compilerMessage = (CompilerMessageImpl)o;

    if (myColumn != compilerMessage.myColumn) return false;
    if (myRow != compilerMessage.myRow) return false;
    if (!myCategory.equals(compilerMessage.myCategory)) return false;
    if (myFile != null ? !myFile.equals(compilerMessage.myFile) : compilerMessage.myFile != null) return false;
    if (!myMessage.equals(compilerMessage.myMessage)) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = myCategory.hashCode();
    result = 29 * result + myMessage.hashCode();
    result = 29 * result + (myFile != null ? myFile.hashCode() : 0);
    result = 29 * result + myRow;
    result = 29 * result + myColumn;
    return result;
  }

  public String toString() {
    return myMessage;
  }
}
