/*
 * Copyright (c) 2000-2004 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.compiler.ant;

import com.intellij.compiler.ant.taskdefs.FileSet;
import com.intellij.compiler.ant.taskdefs.Path;
import com.intellij.compiler.ant.taskdefs.PathElement;
import com.intellij.compiler.ant.taskdefs.PatternSetRef;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * @author Eugene Zhuravlev
 *         Date: Nov 25, 2004
 */
public class LibraryDefinitionsGeneratorFactory {
  private final ProjectEx myProject;
  private final GenerationOptions myGenOptions;
  private final Set<String> myUsedLibraries = new HashSet<String>();

  public LibraryDefinitionsGeneratorFactory(ProjectEx project, GenerationOptions genOptions) {
    myProject = project;
    myGenOptions = genOptions;
    final ModuleManager moduleManager = ModuleManager.getInstance(project);
    final Module[] modules = moduleManager.getModules();
    for (Module module : modules) {
      final OrderEntry[] orderEntries = ModuleRootManager.getInstance(module).getOrderEntries();
      for (OrderEntry orderEntry : orderEntries) {
        if (orderEntry instanceof LibraryOrderEntry && orderEntry.isValid()) {
          Library library = ((LibraryOrderEntry)orderEntry).getLibrary();
          if (library != null) {
            final String name = library.getName();
            if (name != null) {
              myUsedLibraries.add(name);
            }
          }
        }
      }
    }
  }

  /**
   * Create a generator for the specified libary type. It generates a list of library definitions.
   *
   * @param libraryTable a library table to examine
   * @param baseDir      base directory for ant build script
   * @param comment      a comment to use for the library
   * @return the created generator or null if there is a nothing to generate
   */
  @Nullable
  public Generator create(LibraryTable libraryTable, File baseDir, final String comment) {
    final Library[] libraries = libraryTable.getLibraries();
    if (libraries.length == 0) {
      return null;
    }

    final CompositeGenerator gen = new CompositeGenerator();

    gen.add(new Comment(comment), 1);
    // sort libraries to ensure stable order of them.
    TreeMap<String, Library> sortedLibs = new TreeMap<String, Library>();
    for (final Library library : libraries) {
      final String libraryName = library.getName();
      if (!myUsedLibraries.contains(libraryName)) {
        continue;
      }
      sortedLibs.put(BuildProperties.getLibraryPathId(libraryName), library);
    }
    for (final Library library : sortedLibs.values()) {
      final String libraryName = library.getName();
      final Path libraryPath = new Path(BuildProperties.getLibraryPathId(libraryName));
      genLibraryContent(myProject, myGenOptions, library, baseDir, libraryPath);
      gen.add(libraryPath, 1);
    }
    return gen.getGeneratorCount() > 0 ? gen : null;
  }

  /**
   * Generate library content
   *
   * @param project     the context project
   * @param genOptions  the generation options
   * @param library     the library which content is generated
   * @param baseDir     the base directory
   * @param libraryPath the composite generator to update
   */
  public static void genLibraryContent(final ProjectEx project,
                                       final GenerationOptions genOptions,
                                       final Library library,
                                       final File baseDir,
                                       final CompositeGenerator libraryPath) {
    if (genOptions.expandJarDirectories) {
      final VirtualFile[] files = library.getFiles(OrderRootType.COMPILATION_CLASSES);
      // note that it is assumed that directory entries inside library path are unordered
      TreeSet<String> visitedPaths = new TreeSet<String>();
      for (final VirtualFile file : files) {
        final String path = GenerationUtils
          .toRelativePath(file, baseDir, BuildProperties.getProjectBaseDirProperty(), genOptions, !project.isSavePathsRelative());
        visitedPaths.add(path);
      }
      for (final String path : visitedPaths) {
        libraryPath.add(new PathElement(path));
      }
    }
    else {
      TreeSet<String> urls = new TreeSet<String>(Arrays.asList(library.getUrls(OrderRootType.COMPILATION_CLASSES)));
      for (String url : urls) {
        File file = fileFromUrl(url);
        final String path = GenerationUtils
          .toRelativePath(file.getPath(), baseDir, BuildProperties.getProjectBaseDirProperty(), genOptions, !project.isSavePathsRelative());
        if (url.startsWith(JarFileSystem.PROTOCOL_PREFIX)) {
          libraryPath.add(new PathElement(path));
        }
        else if (url.startsWith(LocalFileSystem.PROTOCOL_PREFIX)) {
          if (library.isJarDirectory(url)) {
            final FileSet fileSet = new FileSet(path);
            fileSet.add(new PatternSetRef(BuildProperties.PROPERTY_LIBRARIES_PATTERNS));
            libraryPath.add(fileSet);
          }
          else {
            libraryPath.add(new PathElement(path));
          }
        }
        else {
          throw new IllegalStateException("Unknown url type: " + url);
        }
      }
    }
  }

  /**
   * Gets file from jar of file URL
   *
   * @param url an url to parse
   * @return
   */
  private static File fileFromUrl(String url) {
    final String filePart;
    if (url.startsWith(JarFileSystem.PROTOCOL_PREFIX) && url.endsWith(JarFileSystem.JAR_SEPARATOR)) {
      filePart = url.substring(JarFileSystem.PROTOCOL_PREFIX.length(), url.length() - JarFileSystem.JAR_SEPARATOR.length());
    }
    else if (url.startsWith(LocalFileSystem.PROTOCOL_PREFIX)) {
      filePart = url.substring(JarFileSystem.PROTOCOL_PREFIX.length());
    }
    else {
      throw new IllegalArgumentException("Unknown url type: " + url);
    }
    return new File(filePart.replace('/', File.separatorChar));
  }
}
