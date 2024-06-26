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
package com.intellij.packaging.impl.artifacts;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.MultiValuesMap;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.elements.ComplexPackagingElementType;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.elements.FileOrDirectoryCopyPackagingElement;
import com.intellij.packaging.impl.elements.ModuleOutputPackagingElement;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class ArtifactBySourceFileFinderImpl extends ArtifactBySourceFileFinder {
  private CachedValue<MultiValuesMap<VirtualFile, Artifact>> myFile2Artifacts;
  private final Project myProject;

  public ArtifactBySourceFileFinderImpl(Project project) {
    myProject = project;
  }

  public CachedValue<MultiValuesMap<VirtualFile, Artifact>> getFileToArtifactsMap() {
    if (myFile2Artifacts == null) {
      myFile2Artifacts =
        CachedValuesManager.getManager(myProject).createCachedValue(new CachedValueProvider<MultiValuesMap<VirtualFile, Artifact>>() {
          public Result<MultiValuesMap<VirtualFile, Artifact>> compute() {
            MultiValuesMap<VirtualFile, Artifact> result = computeFileToArtifactsMap();
            List<ModificationTracker> trackers = new ArrayList<ModificationTracker>();
            trackers.add(ArtifactManager.getInstance(myProject).getModificationTracker());
            for (ComplexPackagingElementType<?> type : PackagingElementFactory.getInstance().getComplexElementTypes()) {
              ContainerUtil.addIfNotNull(type.getAllSubstitutionsModificationTracker(myProject), trackers);
            }
            return Result.create(result, trackers.toArray(new ModificationTracker[trackers.size()]));
          }
        }, false);
    }
    return myFile2Artifacts;
  }

  private MultiValuesMap<VirtualFile, Artifact> computeFileToArtifactsMap() {
    final MultiValuesMap<VirtualFile, Artifact> result = new MultiValuesMap<VirtualFile, Artifact>();
    final ArtifactManager artifactManager = ArtifactManager.getInstance(myProject);
    for (final Artifact artifact : artifactManager.getArtifacts()) {
      final PackagingElementResolvingContext context = artifactManager.getResolvingContext();
      ArtifactUtil.processPackagingElements(artifact, null, new PackagingElementProcessor<PackagingElement<?>>() {
        @Override
        public boolean process(@NotNull PackagingElement<?> element, @NotNull PackagingElementPath path) {
          if (element instanceof FileOrDirectoryCopyPackagingElement<?>) {
            final VirtualFile root = ((FileOrDirectoryCopyPackagingElement)element).findFile();
            if (root != null) {
              result.put(root, artifact);
            }
          }
          else if (element instanceof ModuleOutputPackagingElement) {
            final Module module = ((ModuleOutputPackagingElement)element).findModule(context);
            if (module != null) {
              final ModuleRootModel rootModel = context.getModulesProvider().getRootModel(module);
              for (ContentEntry contentEntry : rootModel.getContentEntries()) {
                for (SourceFolder sourceFolder : contentEntry.getSourceFolders()) {
                  final VirtualFile sourceRoot = sourceFolder.getFile();
                  if (sourceRoot != null && !sourceFolder.isTestSource()) {
                    result.put(sourceRoot, artifact);
                  }
                }
              }
            }
          }
          return true;
        }
      }, context, true);
    }
    return result;
  }

  @Override
  public Collection<? extends Artifact> findArtifacts(@NotNull VirtualFile sourceFile) {
    final MultiValuesMap<VirtualFile, Artifact> map = getFileToArtifactsMap().getValue();
    if (map.isEmpty()) {
      return Collections.emptyList();
    }

    List<Artifact> result = null;
    VirtualFile file = sourceFile;
    while (file != null) {
      final Collection<Artifact> artifacts = map.get(file);
      if (artifacts != null) {
        if (result == null) {
          result = new SmartList<Artifact>();
        }
        result.addAll(artifacts);
      }
      file = file.getParent();
    }
    return result != null ? result : Collections.<Artifact>emptyList();
  }
}
