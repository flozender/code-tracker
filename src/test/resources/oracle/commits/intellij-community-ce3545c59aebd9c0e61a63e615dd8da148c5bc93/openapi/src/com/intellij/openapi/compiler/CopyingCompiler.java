/*
 * Copyright 2000-2005 JetBrains s.r.o.
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
package com.intellij.openapi.compiler;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.IOUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Compiler which copies the compiled files to a different directory.
 */
public abstract class CopyingCompiler implements PackagingCompiler{
  public abstract VirtualFile[] getFilesToCopy(CompileContext context);
  public abstract String getDestinationPath(VirtualFile sourceFile);

  public final void processOutdatedItem(CompileContext context, String url, ValidityState state) {
    final String destinationPath = ((DestinationFileInfo)state).getDestinationPath();
    new File(destinationPath).delete();
  }

  @NotNull
  public final ProcessingItem[] getProcessingItems(final CompileContext context) {
    return ApplicationManager.getApplication().runReadAction(new Computable<ProcessingItem[]>() {
      public ProcessingItem[] compute() {
        final VirtualFile[] filesToCopy = getFilesToCopy(context);
        final ProcessingItem[] items = new ProcessingItem[filesToCopy.length];
        for (int idx = 0; idx < filesToCopy.length; idx++) {
          final VirtualFile file = filesToCopy[idx];
          items[idx] = new CopyItem(file, getDestinationPath(file));
        }
        return items;
      }
    });
  }

  public ProcessingItem[] process(CompileContext context, ProcessingItem[] items) {
    final List<ProcessingItem> successfullyProcessed = new ArrayList<ProcessingItem>(items.length);
    for (ProcessingItem item : items) {
      final CopyItem copyItem = (CopyItem)item;
      final String fromPath = copyItem.getSourcePath();
      final String toPath = copyItem.getDestinationPath();
      try {
        FileUtil.copy(new File(fromPath), new File(toPath));
        successfullyProcessed.add(copyItem);
      }
      catch (IOException e) {
        context.addMessage(
          CompilerMessageCategory.ERROR,
          CompilerBundle.message("error.copying", fromPath, toPath, e.getMessage()),
          null, -1, -1
        );
      }
    }
    return successfullyProcessed.toArray(new ProcessingItem[successfullyProcessed.size()]);
  }

  @NotNull
  public String getDescription() {
    return CompilerBundle.message("file.copying.compiler.description");
  }

  public boolean validateConfiguration(CompileScope scope) {
    return true;
  }

  public ValidityState createValidityState(DataInputStream is) throws IOException {
    return new DestinationFileInfo(IOUtil.readString(is), true);
  }

  private static class CopyItem implements FileProcessingCompiler.ProcessingItem {
    private final VirtualFile myFile;
    private final DestinationFileInfo myInfo;
    private final String mySourcePath;

    public CopyItem(VirtualFile file, String destinationPath) {
      myFile = file;
      mySourcePath = file.getPath().replace('/', File.separatorChar);
      myInfo = new DestinationFileInfo(destinationPath, new File(destinationPath).exists());
    }

    public VirtualFile getFile() {
      return myFile;
    }

    public ValidityState getValidityState() {
      return myInfo;
    }

    public String getSourcePath() {
      return mySourcePath;
    }

    public String getDestinationPath() {
      return myInfo.getDestinationPath();
    }
  }

  private static class DestinationFileInfo implements ValidityState {
    private final String destinationPath;
    private final boolean myFileExists;

    public DestinationFileInfo(String destinationPath, boolean fileExists) {
      this.destinationPath = destinationPath;
      myFileExists = fileExists;
    }

    public boolean equalsTo(ValidityState otherState) {
      if (!(otherState instanceof DestinationFileInfo)) {
        return false;
      }
      DestinationFileInfo destinationFileInfo = (DestinationFileInfo)otherState;
      return (myFileExists == destinationFileInfo.myFileExists) && (destinationPath.equals(destinationFileInfo.destinationPath));
    }

    public void save(DataOutputStream os) throws IOException {
      IOUtil.writeString(destinationPath, os);
    }

    public String getDestinationPath() {
      return destinationPath;
    }
  }

}
