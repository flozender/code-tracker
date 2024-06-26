/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.compiler.impl;

import com.intellij.compiler.server.BuildManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Set;

/**
 * @author Eugene Zhuravlev
 * @since Jun 3, 2008
 *
 * A source file is scheduled for recompilation if
 * 1. its timestamp has changed
 * 2. one of its corresponding output files was deleted
 * 3. output root of containing module has changed
 *
 * An output file is scheduled for deletion if:
 * 1. corresponding source file has been scheduled for recompilation (see above)
 * 2. corresponding source file has been deleted
 */
public class TranslatingCompilerFilesMonitor implements ApplicationComponent {
  private static final Logger LOG = Logger.getInstance("#com.intellij.compiler.impl.TranslatingCompilerFilesMonitor");
  public static boolean ourDebugMode = false;

  public TranslatingCompilerFilesMonitor(VirtualFileManager vfsManager, Application application) {
    vfsManager.addVirtualFileListener(new MyVfsListener(), application);
  }

  public static TranslatingCompilerFilesMonitor getInstance() {
    return ApplicationManager.getApplication().getComponent(TranslatingCompilerFilesMonitor.class);
  }

  @NotNull
  public String getComponentName() {
    return "TranslatingCompilerFilesMonitor";
  }

  public void initComponent() {
  }


  public void disposeComponent() {
  }

  private interface FileProcessor {
    void execute(VirtualFile file);
  }

  private static void processRecursively(final VirtualFile fromFile, final boolean dbOnly, final FileProcessor processor) {
    if (!(fromFile.getFileSystem() instanceof LocalFileSystem)) {
      return;
    }

    final FileTypeManager fileTypeManager = FileTypeManager.getInstance();
    VfsUtilCore.visitChildrenRecursively(fromFile, new VirtualFileVisitor() {
      @NotNull @Override
      public Result visitFileEx(@NotNull VirtualFile file) {
        if (fileTypeManager.isFileIgnored(file)) {
          return SKIP_CHILDREN;
        }

        if (!file.isDirectory()) {
          processor.execute(file);
        }
        return CONTINUE;
      }

      @Nullable
      @Override
      public Iterable<VirtualFile> getChildrenIterable(@NotNull VirtualFile file) {
        if (dbOnly) {
          return file.isDirectory()? ((NewVirtualFile)file).iterInDbChildren() : null;
        }
        return null;
      }
    });
  }

  private static boolean isInContentOfOpenedProject(@NotNull final VirtualFile file) {
    // probably need a read action to ensure that the project was not disposed during the iteration over the project list
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      if (!project.isInitialized() || !BuildManager.getInstance().isProjectWatched(project)) {
        continue;
      }
      if (ProjectRootManager.getInstance(project).getFileIndex().isInContent(file)) {
        return true;
      }
    }
    return false;
  }
  
  private class MyVfsListener extends VirtualFileAdapter {
    public void propertyChanged(@NotNull final VirtualFilePropertyEvent event) {
      if (VirtualFile.PROP_NAME.equals(event.getPropertyName())) {
        final VirtualFile eventFile = event.getFile();
        if (isInContentOfOpenedProject(eventFile)) {
          final VirtualFile parent = event.getParent();
          if (parent != null) {
            final String oldName = (String)event.getOldValue();
            final String root = parent.getPath() + "/" + oldName;
            final Set<File> toMark = new THashSet<File>(FileUtil.FILE_HASHING_STRATEGY);
            if (eventFile.isDirectory()) {
              VfsUtilCore.visitChildrenRecursively(eventFile, new VirtualFileVisitor() {
                private StringBuilder filePath = new StringBuilder(root);
  
                @Override
                public boolean visitFile(@NotNull VirtualFile child) {
                  if (child.isDirectory()) {
                    if (!Comparing.equal(child, eventFile)) {
                      filePath.append("/").append(child.getName());
                    }
                  }
                  else {
                    String childPath = filePath.toString();
                    if (!Comparing.equal(child, eventFile)) {
                      childPath += "/" + child.getName();
                    }
                    toMark.add(new File(childPath));
                  }
                  return true;
                }
  
                @Override
                public void afterChildrenVisited(@NotNull VirtualFile file) {
                  if (file.isDirectory() && !Comparing.equal(file, eventFile)) {
                    filePath.delete(filePath.length() - file.getName().length() - 1, filePath.length());
                  }
                }
              });
            }
            else {
              toMark.add(new File(root));
            }
            notifyFilesDeleted(toMark);
          }
          markDirtyIfSource(eventFile, false);
        }
      }
    }

    public void contentsChanged(@NotNull final VirtualFileEvent event) {
      markDirtyIfSource(event.getFile(), false);
    }

    public void fileCreated(@NotNull final VirtualFileEvent event) {
      processNewFile(event.getFile());
    }

    public void fileCopied(@NotNull final VirtualFileCopyEvent event) {
      processNewFile(event.getFile());
    }

    public void fileMoved(@NotNull VirtualFileMoveEvent event) {
      processNewFile(event.getFile());
    }

    public void beforeFileDeletion(@NotNull final VirtualFileEvent event) {
      final VirtualFile eventFile = event.getFile();
      if (isInContentOfOpenedProject(eventFile)) {
        if ((LOG.isDebugEnabled() && eventFile.isDirectory()) || ourDebugMode) {
          final String message = "Processing file deletion: " + eventFile.getPresentableUrl();
          LOG.debug(message);
          if (ourDebugMode) {
            System.out.println(message);
          }
        }

        final Set<File> pathsToMark = new THashSet<File>(FileUtil.FILE_HASHING_STRATEGY);

        processRecursively(eventFile, true, new FileProcessor() {
          public void execute(final VirtualFile file) {
            pathsToMark.add(new File(file.getPath()));
          }
        });

        notifyFilesDeleted(pathsToMark);
      }
    }

    public void beforeFileMovement(@NotNull final VirtualFileMoveEvent event) {
      markDirtyIfSource(event.getFile(), true);
    }

    private void markDirtyIfSource(final VirtualFile file, final boolean fromMove) {
      if (isInContentOfOpenedProject(file)) {
        final Set<File> pathsToMark = new THashSet<File>(FileUtil.FILE_HASHING_STRATEGY);
        processRecursively(file, false, new FileProcessor() {
          public void execute(final VirtualFile file) {
            pathsToMark.add(new File(file.getPath()));
          }
        });
        if (fromMove) {
          notifyFilesDeleted(pathsToMark);
        }
        else {
          notifyFilesChanged(pathsToMark);
        }
      }
    }
  }

  private static void processNewFile(final VirtualFile file) {
    if (isInContentOfOpenedProject(file)) {
      final Set<File> pathsToMark = new THashSet<File>(FileUtil.FILE_HASHING_STRATEGY);
      processRecursively(file, false, new FileProcessor() {
        @Override
        public void execute(VirtualFile file) {
          pathsToMark.add(new File(file.getPath()));
        }
      });
      notifyFilesChanged(pathsToMark);
    }
  }

  private static void notifyFilesChanged(Collection<File> paths) {
    if (!paths.isEmpty()) {
      BuildManager.getInstance().notifyFilesChanged(paths);
    }
  }

  private static void notifyFilesDeleted(Collection<File> paths) {
    if (!paths.isEmpty()) {
      BuildManager.getInstance().notifyFilesDeleted(paths);
    }
  }

}
