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
package com.intellij.packaging.impl.elements;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.LibraryImpl;
import com.intellij.openapi.roots.impl.libraries.LibraryTableImplUtil;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.packaging.artifacts.ArtifactPointerManager;
import com.intellij.packaging.elements.*;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.util.ArrayUtil;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class PackagingElementFactoryImpl extends PackagingElementFactory {
  private static final Logger LOG = Logger.getInstance("#com.intellij.packaging.impl.elements.PackagingElementFactoryImpl");
  public static final PackagingElementType<DirectoryPackagingElement> DIRECTORY_ELEMENT_TYPE = new DirectoryElementType();
  public static final PackagingElementType<ArchivePackagingElement> ARCHIVE_ELEMENT_TYPE = new ArchiveElementType();
  public static final PackagingElementType<FileCopyPackagingElement> FILE_COPY_ELEMENT_TYPE = new FileCopyElementType();
  public static final PackagingElementType<DirectoryCopyPackagingElement> DIRECTORY_COPY_ELEMENT_TYPE = new DirectoryCopyElementType();
  public static final PackagingElementType<ExtractedDirectoryPackagingElement> EXTRACTED_DIRECTORY_ELEMENT_TYPE = new ExtractedDirectoryElementType();
  public static final PackagingElementType<ArtifactRootElement<?>> ARTIFACT_ROOT_ELEMENT_TYPE = new ArtifactRootElementType();
  private static final PackagingElementType[] STANDARD_TYPES = {
      DIRECTORY_ELEMENT_TYPE, ARCHIVE_ELEMENT_TYPE,
      LibraryElementType.LIBRARY_ELEMENT_TYPE, ProductionModuleOutputElementType.ELEMENT_TYPE, TestModuleOutputElementType.ELEMENT_TYPE,
      ArtifactElementType.ARTIFACT_ELEMENT_TYPE, FILE_COPY_ELEMENT_TYPE, DIRECTORY_COPY_ELEMENT_TYPE, EXTRACTED_DIRECTORY_ELEMENT_TYPE
  };

  @NotNull
  @Override
  public PackagingElementType<?>[] getNonCompositeElementTypes() {
    final List<PackagingElementType> elementTypes = new ArrayList<PackagingElementType>();
    for (PackagingElementType elementType : getAllElementTypes()) {
      if (!(elementType instanceof CompositePackagingElementType)) {
        elementTypes.add(elementType);
      }
    }
    return elementTypes.toArray(new PackagingElementType[elementTypes.size()]);
  }

  @Override
  @NotNull
  public ComplexPackagingElementType<?>[] getComplexElementTypes() {
    List<ComplexPackagingElementType<?>> types = new ArrayList<ComplexPackagingElementType<?>>();
    for (PackagingElementType type : getAllElementTypes()) {
      if (type instanceof ComplexPackagingElementType) {
        types.add((ComplexPackagingElementType)type);
      }
    }
    return types.toArray(new ComplexPackagingElementType[types.size()]);
  }

  @NotNull
  @Override
  public CompositePackagingElementType<?>[] getCompositeElementTypes() {
    final List<CompositePackagingElementType> elementTypes = new ArrayList<CompositePackagingElementType>();
    for (PackagingElementType elementType : getAllElementTypes()) {
      if (elementType instanceof CompositePackagingElementType) {
        elementTypes.add((CompositePackagingElementType)elementType);
      }
    }
    return elementTypes.toArray(new CompositePackagingElementType[elementTypes.size()]);
  }

  @Override
  public PackagingElementType<?> findElementType(String id) {
    for (PackagingElementType elementType : getAllElementTypes()) {
      if (elementType.getId().equals(id)) {
        return elementType;
      }
    }
    if (id.equals(ARTIFACT_ROOT_ELEMENT_TYPE.getId())) {
      return ARTIFACT_ROOT_ELEMENT_TYPE;
    }
    return null;
  }

  @NotNull
  @Override
  public PackagingElementType[] getAllElementTypes() {
    final PackagingElementType[] types = Extensions.getExtensions(PackagingElementType.EP_NAME);
    return ArrayUtil.mergeArrays(STANDARD_TYPES, types, PackagingElementType.class);
  }

  @NotNull
  @Override
  public PackagingElement<?> createArtifactElement(@NotNull Artifact artifact, @NotNull Project project) {
    return new ArtifactPackagingElement(project, ArtifactPointerManager.getInstance(project).createPointer(artifact));
  }

  @NotNull
  public DirectoryPackagingElement createDirectory(@NotNull @NonNls String directoryName) {
    return new DirectoryPackagingElement(directoryName);
  }

  @NotNull
  @Override
  public ArtifactRootElement<?> createArtifactRootElement() {
    return new ArtifactRootElementImpl();
  }

  @Override
  @NotNull
  public CompositePackagingElement<?> getOrCreateDirectory(@NotNull CompositePackagingElement<?> parent, @NotNull String relativePath) {
    return getOrCreateDirectoryOrArchive(parent, relativePath, true);
  }

  @NotNull
  @Override
  public CompositePackagingElement<?> getOrCreateArchive(@NotNull CompositePackagingElement<?> parent, @NotNull String relativePath) {
    return getOrCreateDirectoryOrArchive(parent, relativePath, false);
  }

  @Override
  public void addFileCopy(@NotNull CompositePackagingElement<?> root, @NotNull String outputDirectoryPath, @NotNull String sourceFilePath) {
    addFileCopy(root, outputDirectoryPath, sourceFilePath, null);
  }

  @Override
  public void addFileCopy(@NotNull CompositePackagingElement<?> root, @NotNull String outputDirectoryPath, @NotNull String sourceFilePath,
                          @Nullable String outputFileName) {
    final String fileName = PathUtil.getFileName(sourceFilePath);
    if (outputFileName != null && outputFileName.equals(fileName)) {
      outputFileName = null;
    }
    getOrCreateDirectory(root, outputDirectoryPath).addOrFindChild(new FileCopyPackagingElement(sourceFilePath, outputFileName));
  }

  @NotNull
  private CompositePackagingElement<?> getOrCreateDirectoryOrArchive(@NotNull CompositePackagingElement<?> root,
                                                                     @NotNull @NonNls String path, final boolean directory) {
    path = StringUtil.trimStart(StringUtil.trimEnd(path, "/"), "/");
    if (path.length() == 0) {
      return root;
    }
    int index = path.lastIndexOf('/');
    String lastName = path.substring(index + 1);
    String parentPath = index != -1 ? path.substring(0, index) : "";

    final CompositePackagingElement<?> parent = getOrCreateDirectoryOrArchive(root, parentPath, true);
    final CompositePackagingElement<?> last = directory ? createDirectory(lastName) : createArchive(lastName);
    return parent.addOrFindChild(last);
  }

  @NotNull
  public PackagingElement<?> createModuleOutput(@NotNull String moduleName, @NotNull Project project) {
    final ModulePointer pointer = ModulePointerManager.getInstance(project).create(moduleName);
    return new ProductionModuleOutputPackagingElement(project, pointer);
  }

  @NotNull
  @Override
  public PackagingElement<?> createModuleOutput(@NotNull Module module) {
    final ModulePointer modulePointer = ModulePointerManager.getInstance(module.getProject()).create(module);
    return new ProductionModuleOutputPackagingElement(module.getProject(), modulePointer);
  }

  @NotNull
  @Override
  public PackagingElement<?> createTestModuleOutput(@NotNull Module module) {
    ModulePointer pointer = ModulePointerManager.getInstance(module.getProject()).create(module);
    return new TestModuleOutputPackagingElement(module.getProject(), pointer);
  }

  @NotNull
  @Override
  public List<? extends PackagingElement<?>> createLibraryElements(@NotNull Library library) {
    final LibraryTable table = library.getTable();
    final String libraryName = library.getName();
    if (table != null) {
      return Collections.singletonList(createLibraryFiles(libraryName, table.getTableLevel(), null));
    }
    if (libraryName != null) {
      final Module module = ((LibraryImpl)library).getModule();
      if (module != null) {
        return Collections.singletonList(createLibraryFiles(libraryName, LibraryTableImplUtil.MODULE_LEVEL, module.getName()));
      }
    }
    final List<PackagingElement<?>> elements = new ArrayList<PackagingElement<?>>();
    for (VirtualFile file : library.getFiles(OrderRootType.CLASSES)) {
      final String path = FileUtil.toSystemIndependentName(PathUtil.getLocalPath(file));
      elements.add(file.isDirectory() && file.isInLocalFileSystem() ? new DirectoryCopyPackagingElement(path) : new FileCopyPackagingElement(path));
    }
    return elements;
  }

  @NotNull
  @Override
  public PackagingElement<?> createArtifactElement(@NotNull ArtifactPointer artifactPointer, @NotNull Project project) {
    return new ArtifactPackagingElement(project, artifactPointer);
  }

  @NotNull
  @Override
  public PackagingElement<?> createLibraryFiles(@NotNull String libraryName, @NotNull String level, String moduleName) {
    return new LibraryPackagingElement(level, libraryName, moduleName);
  }

  @NotNull
  public CompositePackagingElement<?> createArchive(@NotNull @NonNls String archiveFileName) {
    return new ArchivePackagingElement(archiveFileName);
  }

  @Nullable
  private static PackagingElement<?> findArchiveOrDirectoryByName(@NotNull CompositePackagingElement<?> parent, @NotNull String name) {
    for (PackagingElement<?> element : parent.getChildren()) {
      if (element instanceof ArchivePackagingElement && ((ArchivePackagingElement)element).getArchiveFileName().equals(name) ||
          element instanceof DirectoryPackagingElement && ((DirectoryPackagingElement)element).getDirectoryName().equals(name)) {
        return element;
      }
    }
    return null;
  }

  @NotNull
  public static String suggestFileName(@NotNull CompositePackagingElement<?> parent, @NonNls @NotNull String prefix, @NonNls @NotNull String suffix) {
    String name = prefix + suffix;
    int i = 2;
    while (findArchiveOrDirectoryByName(parent, name) != null) {
      name = prefix + i++ + suffix;
    }
    return name;
  }

  @NotNull
  @Override
  public PackagingElement<?> createDirectoryCopyWithParentDirectories(@NotNull String filePath, @NotNull String relativeOutputPath) {
    return createParentDirectories(relativeOutputPath, new DirectoryCopyPackagingElement(filePath));
  }

  @Override
  @NotNull
  public PackagingElement<?> createExtractedDirectoryWithParentDirectories(@NotNull String jarPath, @NotNull String pathInJar,
                                                                           @NotNull String relativeOutputPath) {
    return createParentDirectories(relativeOutputPath, new ExtractedDirectoryPackagingElement(jarPath, pathInJar));
  }

  @NotNull
  @Override
  public PackagingElement<?> createExtractedDirectory(@NotNull VirtualFile jarEntry) {
    LOG.assertTrue(jarEntry.getFileSystem() instanceof JarFileSystem, "Expected file from jar but file from " + jarEntry.getFileSystem() + " found");
    final String fullPath = jarEntry.getPath();
    final int jarEnd = fullPath.indexOf(JarFileSystem.JAR_SEPARATOR);
    return new ExtractedDirectoryPackagingElement(fullPath.substring(0, jarEnd), fullPath.substring(jarEnd + 1));
  }

  @NotNull
  @Override
  public PackagingElement<?> createFileCopyWithParentDirectories(@NotNull String filePath, @NotNull String relativeOutputPath) {
    return createFileCopyWithParentDirectories(filePath, relativeOutputPath, null);
  }

  @NotNull
  @Override
  public PackagingElement<?> createFileCopyWithParentDirectories(@NotNull String filePath,
                                                                 @NotNull String relativeOutputPath,
                                                                 @Nullable String outputFileName) {
    final FileCopyPackagingElement file = new FileCopyPackagingElement(filePath, outputFileName);
    return createParentDirectories(relativeOutputPath, file);
  }

  @NotNull
  @Override
  public PackagingElement<?> createParentDirectories(@NotNull String relativeOutputPath, @NotNull PackagingElement<?> element) {
    return createParentDirectories(relativeOutputPath, Collections.singletonList(element)).get(0);
  }

  @NotNull
  @Override
  public List<? extends PackagingElement<?>> createParentDirectories(@NotNull String relativeOutputPath, @NotNull List<? extends PackagingElement<?>> elements) {
    relativeOutputPath = StringUtil.trimStart(relativeOutputPath, "/");
    if (relativeOutputPath.length() == 0) {
      return elements;
    }
    int slash = relativeOutputPath.indexOf('/');
    if (slash == -1) slash = relativeOutputPath.length();
    String rootName = relativeOutputPath.substring(0, slash);
    String pathTail = relativeOutputPath.substring(slash);
    final DirectoryPackagingElement root = createDirectory(rootName);
    final CompositePackagingElement<?> last = getOrCreateDirectory(root, pathTail);
    last.addOrFindChildren(elements);
    return Collections.singletonList(root);
  }

  public static CompositePackagingElement<?> createDirectoryOrArchiveWithParents(@NotNull String path, final boolean archive) {
    path = FileUtil.toSystemIndependentName(path);
    final String parentPath = PathUtil.getParentPath(path);
    final String fileName = PathUtil.getFileName(path);
    final PackagingElement<?> element = archive ? new ArchivePackagingElement(fileName) : new DirectoryPackagingElement(fileName);
    return (CompositePackagingElement<?>)getInstance().createParentDirectories(parentPath, element);
  }

  private static class ArtifactRootElementType extends PackagingElementType<ArtifactRootElement<?>> {
    protected ArtifactRootElementType() {
      super("root", "");
    }

    @Override
    public boolean canCreate(@NotNull ArtifactEditorContext context, @NotNull Artifact artifact) {
      return false;
    }

    @NotNull
    public List<? extends ArtifactRootElement<?>> chooseAndCreate(@NotNull ArtifactEditorContext context, @NotNull Artifact artifact,
                                                                   @NotNull CompositePackagingElement<?> parent) {
      throw new UnsupportedOperationException("'create' not implemented in " + getClass().getName());
    }

    @NotNull
    public ArtifactRootElement<?> createEmpty(@NotNull Project project) {
      return new ArtifactRootElementImpl();
    }
  }
}
