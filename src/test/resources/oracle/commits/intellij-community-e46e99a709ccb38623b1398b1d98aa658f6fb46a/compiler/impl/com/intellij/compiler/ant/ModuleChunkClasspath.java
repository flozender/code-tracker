/*
 * Copyright (c) 2000-2004 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.compiler.ant;

import com.intellij.compiler.ant.taskdefs.Path;
import com.intellij.compiler.ant.taskdefs.PathElement;
import com.intellij.compiler.ant.taskdefs.PathRef;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.containers.OrderedSet;
import gnu.trove.TObjectHashingStrategy;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Generator of module chunk classspath. The class used to generate both runtime and compile time classpaths.
 *
 * @author Eugene Zhuravlev
 */
public class ModuleChunkClasspath extends Path {

  /**
   * A constructor
   *
   * @param chunk                    a chunk to process
   * @param genOptions               a generation options
   * @param generateRuntimeClasspath if true, runtime classpath is being generated. Otherwise a compile time classpath is constructed
   */
  @SuppressWarnings({"unchecked"})
  public ModuleChunkClasspath(final ModuleChunk chunk, final GenerationOptions genOptions, final boolean generateRuntimeClasspath) {
    super(generateRuntimeClasspath
          ? BuildProperties.getRuntimeClasspathProperty(chunk.getName())
          : BuildProperties.getClasspathProperty(chunk.getName()));

    final OrderedSet<ClasspathItem> pathItems =
      new OrderedSet<ClasspathItem>((TObjectHashingStrategy<ClasspathItem>)TObjectHashingStrategy.CANONICAL);
    final String moduleChunkBasedirProperty = BuildProperties.getModuleChunkBasedirProperty(chunk);
    final Module[] modules = chunk.getModules();
    // processed chunks (used only in runtime classpath), every chunk is referenced exactly once
    final Set<ModuleChunk> processedChunks = new HashSet<ModuleChunk>();
    // pocessed modules
    final Set<Module> processedModules = new HashSet<Module>();
    for (final Module module : modules) {
      new Object() {
        /**
         * Process the module. The logic is different for compile-time case and runtime case.
         * In the case of runtime, only directly referenced objects are included in classpath.
         * Indirectly referenced are
         *
         * @param module a module to process.
         * @param dependencyLevel is increased with every of recursion.
         * @param isModuleExported if true the module is exported from the previous level
         */
        public void processModule(final Module module, final int dependencyLevel, final boolean isModuleExported) {
          if (processedModules.contains(module)) {
            // the module is already processed, nothing should be done
            return;
          }
          if (dependencyLevel > 1 && !isModuleExported && !(genOptions.inlineRuntimeClasspath && generateRuntimeClasspath)) {
            // the module is not in exports and it is not directly included skip it in the case of library pathgeneration
            return;
          }
          processedModules.add(module);
          final ProjectEx project = (ProjectEx)chunk.getProject();
          final File baseDir = BuildProperties.getProjectBaseDir(project);
          for (final OrderEntry orderEntry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (!orderEntry.isValid()) {
              continue;
            }
            if (!generateRuntimeClasspath) {
              // needed for compilation classpath only
              if ((orderEntry instanceof ModuleSourceOrderEntry)) {
                // this is the entry for outpath of the currently processed module
                if (dependencyLevel == 0 || chunk.contains(module)) {
                  // the root module is never included
                  continue;
                }
              }
              else {
                final boolean isExported = (orderEntry instanceof ExportableOrderEntry) && ((ExportableOrderEntry)orderEntry).isExported();
                if (dependencyLevel > 0 && !isExported) {
                  if (!(orderEntry instanceof ModuleOrderEntry)) {
                    // non-exported dependencies are excluded and not processed
                    continue;
                  }
                }
              }
            }
            if (orderEntry instanceof JdkOrderEntry) {
              if (genOptions.forceTargetJdk && !generateRuntimeClasspath) {
                pathItems
                  .add(new PathRefItem(BuildProperties.propertyRef(BuildProperties.getModuleChunkJdkClasspathProperty(chunk.getName()))));
              }
            }
            else if (orderEntry instanceof ModuleOrderEntry) {
              final ModuleOrderEntry moduleOrderEntry = (ModuleOrderEntry)orderEntry;
              final Module dependentModule = moduleOrderEntry.getModule();
              if (!chunk.contains(dependentModule)) {
                if (generateRuntimeClasspath && !genOptions.inlineRuntimeClasspath) {
                  // in case of runtime classpath, just an referenced to corresponding classpath is created
                  final ModuleChunk depChunk = genOptions.getChunkByModule(dependentModule);
                  if (!processedChunks.contains(depChunk)) {
                    // chunk references are included in the runtime classpath only once
                    processedChunks.add(depChunk);
                    pathItems.add(new PathRefItem(BuildProperties.getRuntimeClasspathProperty(depChunk.getName())));
                  }
                }
                else {
                  // in case of compile classpath or inlined runtime classpath,
                  // the referenced module is processed recursively
                  processModule(dependentModule, dependencyLevel + 1, moduleOrderEntry.isExported());
                }
              }
            }
            else if (orderEntry instanceof LibraryOrderEntry) {
              final LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)orderEntry;
              final String libraryName = libraryOrderEntry.getLibraryName();
              if (((LibraryOrderEntry)orderEntry).isModuleLevel()) {
                CompositeGenerator gen = new CompositeGenerator();
                gen.setHasLeadingNewline(false);
                LibraryDefinitionsGeneratorFactory.genLibraryContent(project, genOptions, libraryOrderEntry.getLibrary(), baseDir, gen);
                pathItems.add(new GeneratorItem(libraryName, gen));
              }
              else {
                pathItems.add(new PathRefItem(BuildProperties.getLibraryPathId(libraryName)));
              }
            }
            else {
              // Module source entry?
              for (String url : getCompilationClasses(orderEntry, ((GenerationOptionsImpl)genOptions), generateRuntimeClasspath)) {
                if (url.endsWith(JarFileSystem.JAR_SEPARATOR)) {
                  url = url.substring(0, url.length() - JarFileSystem.JAR_SEPARATOR.length());
                }
                final String propertyRef = genOptions.getPropertyRefForUrl(url);
                if (propertyRef != null) {
                  pathItems.add(new PathElementItem(propertyRef));
                }
                else {
                  final String path = VirtualFileManager.extractPath(url);
                  pathItems.add(new PathElementItem(
                    GenerationUtils.toRelativePath(path, chunk.getBaseDir(), moduleChunkBasedirProperty, genOptions,
                                                   !chunk.isSavePathsRelative())));
                }
              }
            }
          }
        }
      }.processModule(module, 0, false);
    }
    // convert path items to generators
    for (final ClasspathItem pathItem : pathItems) {
      add(pathItem.toGenerator());
    }
  }

  private static String[] getCompilationClasses(final OrderEntry orderEntry,
                                                final GenerationOptionsImpl options,
                                                final boolean forRuntime) {
    if (!forRuntime) {
      return orderEntry.getUrls(OrderRootType.COMPILATION_CLASSES);
    }
    final Set<String> jdkUrls = options.getAllJdkUrls();

    final OrderedSet<String> urls = new OrderedSet<String>();
    urls.addAll(Arrays.asList(orderEntry.getUrls(OrderRootType.CLASSES_AND_OUTPUT)));
    urls.removeAll(jdkUrls);
    return urls.toArray(new String[urls.size()]);
  }

  /**
   * The base class for an item in the class path. Instances of the subclasses are used instead
   * of generators when building the class path content. The subclasses implement {@link Object#equals(Object)}
   * and {@link Object#hashCode()} methods in order to eliminate duplicates when building classpath.
   */
  private abstract static class ClasspathItem {
    /**
     * primary reference or path of the element
     */
    protected final String myValue;

    /**
     * A constructor
     *
     * @param value primary value of the element
     */
    public ClasspathItem(String value) {
      myValue = value;
    }

    /**
     * @return a generator for path elements.
     */
    public abstract Generator toGenerator();
  }

  /**
   * Class path item that directly embeds generator
   */
  private static class GeneratorItem extends ClasspathItem {
    /**
     * An embedded generator
     */
    final Generator myGenerator;

    /**
     * A constructor
     *
     * @param value     primary value of the element
     * @param generator a generator to use
     */
    public GeneratorItem(String value, final Generator generator) {
      super(value);
      myGenerator = generator;
    }

    public Generator toGenerator() {
      return myGenerator;
    }
  }

  /**
   * This path element directly references some location.
   */
  private static class PathElementItem extends ClasspathItem {
    /**
     * A constructor
     *
     * @param value a referenced location
     */
    public PathElementItem(String value) {
      super(value);
    }

    @Override
    public Generator toGenerator() {
      return new PathElement(myValue);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PathElementItem)) return false;

      final PathElementItem pathElementItem = (PathElementItem)o;

      if (!myValue.equals(pathElementItem.myValue)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return myValue.hashCode();
    }
  }

  /**
   * This path element references a path
   */
  private static class PathRefItem extends ClasspathItem {
    /**
     * A constructor
     *
     * @param value an indentifier of referenced classpath
     */
    public PathRefItem(String value) {
      super(value);
    }

    @Override
    public Generator toGenerator() {
      return new PathRef(myValue);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PathRefItem)) return false;

      final PathRefItem pathRefItem = (PathRefItem)o;

      if (!myValue.equals(pathRefItem.myValue)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return myValue.hashCode();
    }
  }
}
