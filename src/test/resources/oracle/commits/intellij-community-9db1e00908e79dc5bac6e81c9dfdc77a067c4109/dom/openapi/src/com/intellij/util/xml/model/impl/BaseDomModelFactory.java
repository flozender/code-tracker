package com.intellij.util.xml.model.impl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.ModelMerger;
import com.intellij.util.xml.DomService;
import com.intellij.util.xml.model.DomModel;
import com.intellij.util.xml.model.MultipleDomModelFactory;
import com.intellij.util.xml.model.SimpleModelFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: Sergey.Vasiliev
 */
public abstract class BaseDomModelFactory<S extends UserDataHolder, T extends DomElement, M extends DomModel<T>, C extends PsiElement>
    extends DomModelFactoryHelper<T> implements SimpleModelFactory<T, M>, MultipleDomModelFactory<S, T, M> {

  private final Project myProject;
  private final SimpleModelFactory<T,M> mySimpleDomModelFactory;
  private final MultipleDomModelFactory<S, T, M> myMultipleDomModelFactory;

  protected BaseDomModelFactory(@NotNull Class<T> aClass, final Project project, @NonNls String name) {
    super(aClass, DomService.getInstance().createModelMerger());

    myProject = project;

    mySimpleDomModelFactory = createSimpleModelFactory(aClass, getModelMerger(), project, name);

    myMultipleDomModelFactory = createMultipleDomModelFactory(aClass, getModelMerger(), project, name);
  }

  protected abstract S getModelScope(final XmlFile file);

  @Nullable
  protected abstract List<M> computeAllModels(@NotNull S scope);

  protected abstract M createCombinedModel(@NotNull Set<XmlFile> configFiles, @NotNull DomFileElement<T> mergedModel, M firstModel, final S scope);

  @Nullable
  public M getModel(@NotNull C context){
    final PsiFile psiFile = context.getContainingFile();
    if (psiFile instanceof XmlFile) {
      return getModelByConfigFile((XmlFile)psiFile);
    }
    return null;
  }

  @NotNull
  public List<M> getAllModels(@NotNull S scope) {
    return myMultipleDomModelFactory.getAllModels(scope);
  }

  @Nullable
  public M getModelByConfigFile(@Nullable XmlFile psiFile) {
    return mySimpleDomModelFactory.getModelByConfigFile(psiFile);
  }

  @NotNull
  public Object[] computeDependencies(@Nullable M model, @Nullable S scope) {

    final ArrayList<Object> dependencies = new ArrayList<Object>();
    dependencies.add(PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
    if (scope != null) {
      dependencies.add(ProjectRootManager.getInstance(getProject()));
    }
    return dependencies.toArray(new Object[dependencies.size()]);
  }

  @Nullable
  protected M computeModel(@NotNull XmlFile psiFile, @Nullable S scope) {
    if (scope == null) {
      return null;
    }
    final List<M> models = getAllModels(scope);
    for (M model : models) {
      final Set<XmlFile> configFiles = model.getConfigFiles();
      if (configFiles.contains(psiFile)) {
        return model;
      }
    }
    return null;
  }

  @Nullable
  public M getCombinedModel(@Nullable S scope) {
    return myMultipleDomModelFactory.getCombinedModel(scope);
  }

  @NotNull
  public Set<XmlFile> getAllConfigFiles(@NotNull S scope) {
    return myMultipleDomModelFactory.getAllConfigFiles(scope);
  }

  @Nullable
  public DomFileElement<T> createMergedModelRoot(final Set<XmlFile> configFiles) {
    return mySimpleDomModelFactory.createMergedModelRoot(configFiles);
  }

  private CachedMultipleDomModelFactory<S, T, M, C> createMultipleDomModelFactory(final Class<T> aClass,
                                                                                  final ModelMerger modelMerger,
                                                                                  final Project project,
                                                                                  final String name) {
    return new CachedMultipleDomModelFactory<S, T, M, C>(aClass, modelMerger, project, name) {
      public M getModel(@NotNull final C context) {
        return BaseDomModelFactory.this.getModel(context);
      }

      protected List<M> computeAllModels(@NotNull final S scope) {
        return BaseDomModelFactory.this.computeAllModels(scope);
      }

      protected M createCombinedModel(final Set<XmlFile> configFiles,
                                      final DomFileElement<T> mergedModel,
                                      final M firstModel,
                                      final S scope) {
        return BaseDomModelFactory.this.createCombinedModel(configFiles, mergedModel, firstModel, scope);
      }

      @NotNull
      public Object[] computeDependencies(@Nullable final M model, @Nullable final S scope) {
        return BaseDomModelFactory.this.computeDependencies(model, scope);
      }

      public S getModelScope(@NotNull final XmlFile xmlFile) {
        return BaseDomModelFactory.this.getModelScope(xmlFile);
      }
    };
  }

  private CachedSimpleDomModelFactory<T, M, S> createSimpleModelFactory(final Class<T> aClass,
                                                                        final ModelMerger modelMerger,
                                                                        final Project project,
                                                                        final String name) {
    return new CachedSimpleDomModelFactory<T, M, S>(aClass, modelMerger, project, name) {

      protected M computeModel(@NotNull final XmlFile psiFile, @Nullable final S scope) {
        return BaseDomModelFactory.this.computeModel(psiFile, scope);
      }

      @NotNull
      public Object[] computeDependencies(@Nullable final M model, @Nullable final S scope) {
        return BaseDomModelFactory.this.computeDependencies(model, scope);
      }

      public S getModelScope(@NotNull XmlFile file) {
        return BaseDomModelFactory.this.getModelScope(file);
      }
    };
  }

  public Project getProject() {
    return myProject;
  }
}
