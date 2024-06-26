/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.roots.ui.configuration.projectRoot;

import com.intellij.facet.*;
import com.intellij.facet.impl.invalid.InvalidFacetManager;
import com.intellij.facet.impl.invalid.InvalidFacetType;
import com.intellij.facet.impl.ui.facetType.FacetTypeEditor;
import com.intellij.facet.ui.FacetEditor;
import com.intellij.facet.ui.MultipleFacetSettingsEditor;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.FacetProjectStructureElement;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import com.intellij.openapi.ui.DetailsComponent;
import com.intellij.openapi.ui.NamedConfigurable;
import com.intellij.ui.treeStructure.filtered.FilteringTreeBuilder;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.*;
import java.util.List;

/**
 * @author nik
 */
public class FacetStructureConfigurable extends BaseStructureConfigurable {
  private static final Icon ICON = AllIcons.Modules.Modules;//todo[nik] use facets icon
  private final ModuleManager myModuleManager;
  private final Map<FacetType<?, ?>, FacetTypeEditor> myFacetTypeEditors = new HashMap<FacetType<?,?>, FacetTypeEditor>();
  private MultipleFacetSettingsEditor myCurrentMultipleSettingsEditor;
  @NonNls private static final String NO_FRAMEWORKS_NODE = "No facets are configured";
  private boolean myTreeWasInitialized;

  public FacetStructureConfigurable(final Project project, ModuleManager moduleManager) {
    super(project);
    myModuleManager = moduleManager;
  }

  @Override
  protected String getComponentStateKey() {
    return "FacetStructureConfigurable.UI";
  }

  public static FacetStructureConfigurable getInstance(final @NotNull Project project) {
    return ServiceManager.getService(project, FacetStructureConfigurable.class);
  }

  public boolean isVisible() {
    return FacetTypeRegistry.getInstance().getFacetTypes().length > 0 || !InvalidFacetManager.getInstance(myProject).getInvalidFacets().isEmpty();
  }

  @Override
  protected void initTree() {
    super.initTree();
    if (!myTreeWasInitialized) {
      myTreeWasInitialized = true;
      final FacetsTreeCellRenderer separatorRenderer = new FacetsTreeCellRenderer();
      final TreeCellRenderer oldRenderer = myTree.getCellRenderer();
      myTree.setCellRenderer(new TreeCellRenderer() {
        @Override
        public Component getTreeCellRendererComponent(JTree tree,
                                                      Object value,
                                                      boolean selected,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus) {
          if (value instanceof MyNode && ((MyNode)value).getConfigurable() instanceof FrameworkDetectionConfigurable) {
            return separatorRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
          }
          return oldRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
      });
      myTree.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
          revalidateTree();
        }

        @Override
        public void componentMoved(ComponentEvent e) {
          revalidateTree();
        }

        @Override
        public void componentShown(ComponentEvent e) {
          revalidateTree();
        }
      });
    }
  }

  private void revalidateTree() {
    FilteringTreeBuilder.revalidateTree(myTree);
  }

  protected void loadTree() {
    myTree.setRootVisible(false);
    myTree.setShowsRootHandles(false);
    boolean hasFacetTypeNodes = false;
    for (FacetType<?,?> facetType : FacetTypeRegistry.getInstance().getFacetTypes()) {
      if (ProjectFacetManager.getInstance(myProject).hasFacets(facetType.getId())) {
        hasFacetTypeNodes = true;
        addFacetTypeNode(facetType);
      }
    }
    if (!InvalidFacetManager.getInstance(myProject).getInvalidFacets().isEmpty()) {
      hasFacetTypeNodes = true;
      addFacetTypeNode(InvalidFacetType.getInstance());
    }
    if (!hasFacetTypeNodes) {
      addNode(new MyNode(new TextConfigurable<String>(NO_FRAMEWORKS_NODE, NO_FRAMEWORKS_NODE, "Facets", "Press '+' button to add a new facet", null, null)), myRoot);
    }
    addNode(new MyNode(new FrameworkDetectionConfigurable(myProject)), myRoot);
  }

  @Override
  protected Comparator<MyNode> getNodeComparator() {
    return new Comparator<MyNode>() {
      @Override
      public int compare(MyNode node1, MyNode node2) {
        final NamedConfigurable c1 = node1.getConfigurable();
        final NamedConfigurable c2 = node2.getConfigurable();
        if (c1 instanceof FrameworkDetectionConfigurable && !(c2 instanceof FrameworkDetectionConfigurable)) return 1;
        if (!(c1 instanceof FrameworkDetectionConfigurable) && c2 instanceof FrameworkDetectionConfigurable) return -1;

        return node1.getDisplayName().compareToIgnoreCase(node2.getDisplayName());
      }
    };
  }

  private MyNode addFacetTypeNode(FacetType<?, ?> facetType) {
    final MyNode noFrameworksNode = findNodeByObject(myRoot, NO_FRAMEWORKS_NODE);
    if (noFrameworksNode != null) {
      removePaths(TreeUtil.getPathFromRoot(noFrameworksNode));
    }

    FacetTypeConfigurable facetTypeConfigurable = new FacetTypeConfigurable(this, facetType);
    MyNode facetTypeNode = new MyNode(facetTypeConfigurable);
    addNode(facetTypeNode, myRoot);

    for (Module module : myModuleManager.getModules()) {
      Collection<? extends Facet> facets = FacetManager.getInstance(module).getFacetsByType(facetType.getId());
      FacetEditorFacadeImpl editorFacade = ModuleStructureConfigurable.getInstance(myProject).getFacetEditorFacade();
      for (Facet facet : facets) {
        addFacetNode(facetTypeNode, facet, editorFacade);
      }
    }
    return facetTypeNode;
  }

  @NotNull
  @Override
  protected Collection<? extends ProjectStructureElement> getProjectStructureElements() {
    List<ProjectStructureElement> elements = new ArrayList<ProjectStructureElement>();
    for (Module module : myModuleManager.getModules()) {
      Facet[] facets = FacetManager.getInstance(module).getAllFacets();
      for (Facet facet : facets) {
        elements.add(new FacetProjectStructureElement(myContext, facet));
      }
    }
    return elements;
  }

  public MyNode getOrCreateFacetTypeNode(FacetType facetType) {
    final MyNode node = findNodeByObject(myRoot, facetType);
    if (node != null) {
      return node;
    }
    return addFacetTypeNode(facetType);
  }

  public void addFacetNode(@NotNull MyNode facetTypeNode, @NotNull Facet facet, @NotNull FacetEditorFacadeImpl editorFacade) {
    FacetConfigurable facetConfigurable = editorFacade.getOrCreateConfigurable(facet);
    addNode(new FacetConfigurableNode(facetConfigurable), facetTypeNode);
    myContext.getDaemonAnalyzer().queueUpdate(new FacetProjectStructureElement(myContext, facet));
  }

  @Nullable
  public FacetTypeEditor getFacetTypeEditor(@NotNull FacetType<?, ?> facetType) {
    return myFacetTypeEditors.get(facetType);
  }

  public FacetTypeEditor getOrCreateFacetTypeEditor(@NotNull FacetType<?, ?> facetType) {
    FacetTypeEditor editor = myFacetTypeEditors.get(facetType);
    if (editor == null) {
      editor = new FacetTypeEditor(myProject, myContext, facetType);
      editor.reset();
      myFacetTypeEditors.put(facetType, editor);
    }
    return editor;
  }

  public void reset() {
    myFacetTypeEditors.clear();
    super.reset();
    TreeUtil.expandAll(myTree);
  }


  public void apply() throws ConfigurationException {
    super.apply();
    for (FacetTypeEditor editor : myFacetTypeEditors.values()) {
      editor.apply();
    }
  }

  public boolean isModified() {
    return super.isModified() || isEditorsModified();
  }

  private boolean isEditorsModified() {
    for (FacetTypeEditor editor : myFacetTypeEditors.values()) {
      if (editor.isModified()) {
        return true;
      }
    }
    return false;
  }

  public void disposeUIResources() {
    super.disposeUIResources();

    for (FacetTypeEditor editor : myFacetTypeEditors.values()) {
      editor.disposeUIResources();
    }
    myFacetTypeEditors.clear();
  }

  @NotNull
  protected ArrayList<AnAction> createActions(final boolean fromPopup) {
    ArrayList<AnAction> actions = new ArrayList<AnAction>();
    actions.add(new AbstractAddGroup("Add") {
      @NotNull
      @Override
      public AnAction[] getChildren(@Nullable AnActionEvent e) {
        return AddFacetOfTypeAction.createAddFacetActions(FacetStructureConfigurable.this);
      }
    });
    if (fromPopup) {
      actions.add(new MyNavigateAction());
    }
    actions.add(new MyRemoveAction());
    actions.add(Separator.getInstance());
    addCollapseExpandActions(actions);
    return actions;
  }

  protected List<Facet> removeFacet(final Facet facet) {
    List<Facet> removed = super.removeFacet(facet);
    ModuleStructureConfigurable.getInstance(myProject).removeFacetNodes(removed);
    for (Facet removedFacet : removed) {
      myContext.getDaemonAnalyzer().removeElement(new FacetProjectStructureElement(myContext, removedFacet));
    }
    return removed;
  }

  protected boolean updateMultiSelection(final List<NamedConfigurable> selectedConfigurables) {
    return updateMultiSelection(selectedConfigurables, getDetailsComponent());
  }

  public boolean updateMultiSelection(final List<NamedConfigurable> selectedConfigurables, final DetailsComponent detailsComponent) {
    FacetType selectedFacetType = null;
    List<FacetEditor> facetEditors = new ArrayList<FacetEditor>();
    for (NamedConfigurable selectedConfigurable : selectedConfigurables) {
      if (selectedConfigurable instanceof FacetConfigurable) {
        FacetConfigurable facetConfigurable = (FacetConfigurable)selectedConfigurable;
        FacetType facetType = facetConfigurable.getEditableObject().getType();
        if (selectedFacetType != null && selectedFacetType != facetType) {
          return false;
        }
        selectedFacetType = facetType;
        facetEditors.add(facetConfigurable.getEditor());
      }
    }
    if (facetEditors.size() <= 1 || selectedFacetType == null) {
      return false;
    }

    FacetEditor[] selectedEditors = facetEditors.toArray(new FacetEditor[facetEditors.size()]);
    MultipleFacetSettingsEditor editor = selectedFacetType.createMultipleConfigurationsEditor(myProject, selectedEditors);
    if (editor == null) {
      return false;
    }

    setSelectedNode(null);
    myCurrentMultipleSettingsEditor = editor;
    detailsComponent.setText(ProjectBundle.message("multiple.facets.banner.0.1.facets", selectedEditors.length,
                                                        selectedFacetType.getPresentableName()));
    detailsComponent.setContent(editor.createComponent());
    return true;
  }

  protected void updateSelection(@Nullable final NamedConfigurable configurable) {
    disposeMultipleSettingsEditor();
    if (configurable instanceof FacetTypeConfigurable) {
      ((FacetTypeConfigurable)configurable).updateComponent();
    }
    super.updateSelection(configurable);
  }

  public void disposeMultipleSettingsEditor() {
    if (myCurrentMultipleSettingsEditor != null) {
      myCurrentMultipleSettingsEditor.disposeUIResources();
      myCurrentMultipleSettingsEditor = null;
    }
  }

  @Nullable
  protected AbstractAddGroup createAddAction() {
    return null;
  }

  protected void processRemovedItems() {
  }

  protected boolean wasObjectStored(final Object editableObject) {
    return false;
  }

  public String getDisplayName() {
    return ProjectBundle.message("project.facets.display.name");
  }

  public Icon getIcon() {
    return ICON;
  }

  public String getHelpTopic() {
    final Component component = PlatformDataKeys.CONTEXT_COMPONENT.getData(DataManager.getInstance().getDataContext());
    if (myTree.equals(component)) {
      final NamedConfigurable selectedConfigurable = getSelectedConfigurable();
      if (selectedConfigurable instanceof FacetTypeConfigurable) {
        final FacetType facetType = ((FacetTypeConfigurable)selectedConfigurable).getEditableObject();
        final String topic = facetType.getHelpTopic();
        if (topic != null) {
          return topic;
        }
      }
    }
    if (myCurrentMultipleSettingsEditor != null) {
      final String topic = myCurrentMultipleSettingsEditor.getHelpTopic();
      if (topic != null) {
        return topic;
      }
    }
    String topic = super.getHelpTopic();
    if (topic != null) {
      return topic;
    }
    return "reference.settingsdialog.project.structure.facet";
  }

  @NotNull
  public String getId() {
    return "project.facets";
  }

  public Runnable enableSearch(final String option) {
    return null;
  }

  public void dispose() {
  }

  private class FacetConfigurableNode extends MyNode {
    public FacetConfigurableNode(final FacetConfigurable facetConfigurable) {
      super(facetConfigurable);
    }

    @NotNull
    public String getDisplayName() {
      FacetConfigurable facetConfigurable = (FacetConfigurable)getConfigurable();
      String moduleName = myContext.getRealName(facetConfigurable.getEditableObject().getModule());
      return facetConfigurable.getDisplayName() + " (" + moduleName + ")";
    }
  }

  private class MyNavigateAction extends AnAction implements DumbAware {
    private MyNavigateAction() {
      super(ProjectBundle.message("action.name.facet.navigate"));
      registerCustomShortcutSet(CommonShortcuts.getEditSource(), myTree);
    }

    public void update(final AnActionEvent e) {
      NamedConfigurable selected = getSelectedConfigurable();
      e.getPresentation().setEnabled(selected instanceof FacetConfigurable);
    }

    public void actionPerformed(final AnActionEvent e) {
      NamedConfigurable selected = getSelectedConfigurable();
      if (selected instanceof FacetConfigurable) {
        ProjectStructureConfigurable.getInstance(myProject).select(((FacetConfigurable)selected).getEditableObject(), true);
      }
    }
  }
}
