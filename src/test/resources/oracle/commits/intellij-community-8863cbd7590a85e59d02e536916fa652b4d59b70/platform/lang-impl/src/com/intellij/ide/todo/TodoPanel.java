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

package com.intellij.ide.todo;

import com.intellij.icons.AllIcons;
import com.intellij.ide.*;
import com.intellij.ide.actions.ContextHelpAction;
import com.intellij.ide.actions.NextOccurenceToolbarAction;
import com.intellij.ide.actions.PreviousOccurenceToolbarAction;
import com.intellij.ide.todo.nodes.TodoFileNode;
import com.intellij.ide.todo.nodes.TodoItemNode;
import com.intellij.ide.todo.nodes.TodoTreeHelper;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.VisibilityWatcher;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.impl.UsagePreviewPanel;
import com.intellij.util.*;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vladimir Kondratyev
 */
abstract class TodoPanel extends SimpleToolWindowPanel implements OccurenceNavigator, DataProvider, Disposable {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.todo.TodoPanel");
  private static final Icon PREVIEW_ICON = AllIcons.Actions.Preview;

  protected Project myProject;
  private final TodoPanelSettings mySettings;
  private final boolean myCurrentFileMode;
  private final Content myContent;

  private final Tree myTree;
  private final MyTreeExpander myTreeExpander;
  private final MyOccurenceNavigator myOccurenceNavigator;
  protected final TodoTreeBuilder myTodoTreeBuilder;
  private MyVisibilityWatcher myVisibilityWatcher;
  private UsagePreviewPanel myUsagePreviewPanel;

  /**
   * @param currentFileMode if <code>true</code> then view doesn't have "Group By Packages" and "Flatten Packages"
   *                        actions.
   */
  TodoPanel(Project project,
                   TodoPanelSettings settings,
                   boolean currentFileMode,
                   Content content) {
    super(false, true);
    myProject = project;
    mySettings = settings;
    myCurrentFileMode = currentFileMode;
    myContent = content;

    DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode());
    myTree = new Tree(model);
    myTreeExpander = new MyTreeExpander();
    myOccurenceNavigator = new MyOccurenceNavigator();
    initUI();
    myTodoTreeBuilder = createTreeBuilder(myTree, model, myProject);
    Disposer.register(myProject, myTodoTreeBuilder);
    updateTodoFilter();
    myTodoTreeBuilder.setShowPackages(mySettings.arePackagesShown());
    myTodoTreeBuilder.setShowModules(mySettings.areModulesShown());
    myTodoTreeBuilder.setFlattenPackages(mySettings.areFlattenPackages());

    myVisibilityWatcher = new MyVisibilityWatcher();
    myVisibilityWatcher.install(this);
  }

  protected abstract TodoTreeBuilder createTreeBuilder(JTree tree, DefaultTreeModel treeModel, Project project);

  private void initUI() {
    UIUtil.setLineStyleAngled(myTree);
    myTree.setShowsRootHandles(true);
    myTree.setRootVisible(false);
    myTree.setCellRenderer(new TodoCompositeRenderer());
    EditSourceOnDoubleClickHandler.install(myTree);
    new TreeSpeedSearch(myTree);

    DefaultActionGroup group = new DefaultActionGroup();
    group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE));
    group.addSeparator();
    group.add(ActionManager.getInstance().getAction(IdeActions.GROUP_VERSION_CONTROLS));
    PopupHandler.installPopupHandler(myTree, group, ActionPlaces.TODO_VIEW_POPUP, ActionManager.getInstance());

    myTree.addKeyListener(
      new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          if (!e.isConsumed() && KeyEvent.VK_ENTER == e.getKeyCode()) {
            TreePath path = myTree.getSelectionPath();
            if (path == null) {
              return;
            }
            final Object userObject = ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
            NodeDescriptor desciptor = userObject instanceof NodeDescriptor ? (NodeDescriptor)userObject : null;
            if (!(desciptor instanceof TodoItemNode)) {
              return;
            }
            OpenSourceUtil.openSourcesFrom(DataManager.getInstance().getDataContext(TodoPanel.this), false);
          }
        }
      }
    );


    myUsagePreviewPanel = new UsagePreviewPanel(myProject);
    myUsagePreviewPanel.setBorder(IdeBorderFactory.createBorder(SideBorder.LEFT));
    Disposer.register(this, myUsagePreviewPanel);
    myUsagePreviewPanel.setVisible(mySettings.isShowPreview());

    setContent(createCenterComponent());

    myTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(final TreeSelectionEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            if (myUsagePreviewPanel.isVisible()) {
              updatePreviewPanel();
            }
          }
        });
      }
    });


    // Create tool bars and register custom shortcuts

    JPanel toolBarPanel = new JPanel(new GridLayout());

    DefaultActionGroup leftGroup = new DefaultActionGroup();
    leftGroup.add(new PreviousOccurenceToolbarAction(myOccurenceNavigator));
    leftGroup.add(new NextOccurenceToolbarAction(myOccurenceNavigator));
    leftGroup.add(new ContextHelpAction("find.todoList"));
    toolBarPanel.add(
      ActionManager.getInstance().createActionToolbar(ActionPlaces.TODO_VIEW_TOOLBAR, leftGroup, false).getComponent());

    DefaultActionGroup rightGroup = new DefaultActionGroup();
    AnAction expandAllAction = CommonActionsManager.getInstance().createExpandAllAction(myTreeExpander, this);
    rightGroup.add(expandAllAction);

    AnAction collapseAllAction = CommonActionsManager.getInstance().createCollapseAllAction(myTreeExpander, this);
    rightGroup.add(collapseAllAction);

    if (!myCurrentFileMode) {
      MyShowModulesAction showModulesAction = new MyShowModulesAction();
      showModulesAction.registerCustomShortcutSet(
        new CustomShortcutSet(
          KeyStroke.getKeyStroke(KeyEvent.VK_M, SystemInfo.isMac ? InputEvent.META_MASK : InputEvent.CTRL_MASK)),
        myTree);
      rightGroup.add(showModulesAction);
      MyShowPackagesAction showPackagesAction = new MyShowPackagesAction();
      showPackagesAction.registerCustomShortcutSet(
        new CustomShortcutSet(
          KeyStroke.getKeyStroke(KeyEvent.VK_P, SystemInfo.isMac ? InputEvent.META_MASK : InputEvent.CTRL_MASK)),
        myTree);
      rightGroup.add(showPackagesAction);

      MyFlattenPackagesAction flattenPackagesAction = new MyFlattenPackagesAction();
      flattenPackagesAction.registerCustomShortcutSet(
        new CustomShortcutSet(
          KeyStroke.getKeyStroke(KeyEvent.VK_F, SystemInfo.isMac ? InputEvent.META_MASK : InputEvent.CTRL_MASK)),
        myTree);
      rightGroup.add(flattenPackagesAction);
    }

    MyAutoScrollToSourceHandler autoScrollToSourceHandler = new MyAutoScrollToSourceHandler();
    autoScrollToSourceHandler.install(myTree);
    rightGroup.add(autoScrollToSourceHandler.createToggleAction());

    SetTodoFilterAction setTodoFilterAction = new SetTodoFilterAction(myProject, mySettings, new Consumer<TodoFilter>() {
      @Override
      public void consume(TodoFilter todoFilter) {
        setTodoFilter(todoFilter);
      }
    });
    rightGroup.add(setTodoFilterAction);
    rightGroup.add(new MyPreviewAction());
    toolBarPanel.add(
      ActionManager.getInstance().createActionToolbar(ActionPlaces.TODO_VIEW_TOOLBAR, rightGroup, false).getComponent());

    setToolbar(toolBarPanel);
  }

  protected JComponent createCenterComponent() {
    final Splitter splitter = new Splitter(false);
    splitter.setSecondComponent(myUsagePreviewPanel);
    splitter.setFirstComponent(ScrollPaneFactory.createScrollPane(myTree));
    return splitter;
  }

  private void updatePreviewPanel() {
    if (myProject.isDisposed()) return;
    List<UsageInfo> infos = new ArrayList<UsageInfo>();
    final TreePath path = myTree.getSelectionPath();
    if (path != null) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      if (userObject instanceof NodeDescriptor) {
        Object element = ((NodeDescriptor)userObject).getElement();
        TodoItemNode pointer = myTodoTreeBuilder.getFirstPointerForElement(element);
        if (pointer != null) {
          final SmartTodoItemPointer value = pointer.getValue();
          final Document document = value.getDocument();
          final PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
          final RangeMarker rangeMarker = value.getRangeMarker();
          if (psiFile != null) {
            infos.add(new UsageInfo(psiFile, rangeMarker.getStartOffset(), rangeMarker.getEndOffset()));
          }
        }
      }
    }
    myUsagePreviewPanel.updateLayout(infos.isEmpty() ? null : infos);
  }

  public void dispose() {
    if (myVisibilityWatcher != null) {
      myVisibilityWatcher.deinstall(this);
      myVisibilityWatcher = null;
    }
    myProject = null;
  }

  void rebuildCache() {
    myTodoTreeBuilder.rebuildCache();
  }

  /**
   * Immediately updates tree.
   */
  void updateTree() {
    myTodoTreeBuilder.updateTree(false);
  }

  /**
   * Updates current filter. If previously set filter was removed then empty filter is set.
   * 
   * @see TodoTreeBuilder#setTodoFilter
   */
  void updateTodoFilter() {
    TodoFilter filter = TodoConfiguration.getInstance().getTodoFilter(mySettings.getTodoFilterName());
    setTodoFilter(filter);
  }

  /**
   * Sets specified <code>TodoFilter</code>. The method also updates window's title.
   * 
   * @see TodoTreeBuilder#setTodoFilter
   */
  private void setTodoFilter(TodoFilter filter) {
    // Clear name of current filter if it was removed from configuration.
    String filterName = filter != null ? filter.getName() : null;
    mySettings.setTodoFilterName(filterName);
    // Update filter
    myTodoTreeBuilder.setTodoFilter(filter);
    // Update content's title
    myContent.setDescription(filterName);
  }

  /**
   * @return list of all selected virtual files.
   */
  @Nullable
  protected PsiFile getSelectedFile() {
    TreePath path = myTree.getSelectionPath();
    if (path == null) {
      return null;
    }
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
    LOG.assertTrue(node != null);
    if(node.getUserObject() == null){
      return null;
    }
    return myTodoTreeBuilder.getFileForNode(node);
  }

  protected void setDisplayName(String tabName) {
    myContent.setDisplayName(tabName);
  }

  @Nullable
  private PsiElement getSelectedElement() {
    if (myTree == null) return null;
    TreePath path = myTree.getSelectionPath();
    if (path == null) {
      return null;
    }
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
    Object userObject = node.getUserObject();
    final PsiElement selectedElement = TodoTreeHelper.getInstance(myProject).getSelectedElement(userObject);
    if (selectedElement != null) return selectedElement;
    return getSelectedFile();
  }

  public Object getData(String dataId) {
    if (PlatformDataKeys.NAVIGATABLE.is(dataId)) {
      TreePath path = myTree.getSelectionPath();
      if (path == null) {
        return null;
      }
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      if (!(userObject instanceof NodeDescriptor)) {
        return null;
      }
      Object element = ((NodeDescriptor)userObject).getElement();
      if (!(element instanceof TodoFileNode || element instanceof TodoItemNode)) { // allow user to use F4 only on files an TODOs
        return null;
      }
      TodoItemNode pointer = myTodoTreeBuilder.getFirstPointerForElement(element);
      if (pointer != null) {
        return new OpenFileDescriptor(myProject, pointer.getValue().getTodoItem().getFile().getVirtualFile(),
          pointer.getValue().getRangeMarker().getStartOffset()
        );
      }
      else {
        return null;
      }
    }
    else if (PlatformDataKeys.VIRTUAL_FILE.is(dataId)) {
      final PsiFile file = getSelectedFile();
      return file != null ? file.getVirtualFile() : null;
    }
    else if (LangDataKeys.PSI_ELEMENT.is(dataId)) {
      return getSelectedElement();
    }
    else if (PlatformDataKeys.VIRTUAL_FILE_ARRAY.is(dataId)) {
      PsiFile file = getSelectedFile();
      if (file != null) {
        return new VirtualFile[]{file.getVirtualFile()};
      }
      else {
        return VirtualFile.EMPTY_ARRAY;
      }
    }
    else if (PlatformDataKeys.HELP_ID.is(dataId)) {
      //noinspection HardCodedStringLiteral
      return "find.todoList";
    }
    return super.getData(dataId);
  }

  @Nullable
  public OccurenceInfo goPreviousOccurence() {
    return myOccurenceNavigator.goPreviousOccurence();
  }

  public String getNextOccurenceActionName() {
    return myOccurenceNavigator.getNextOccurenceActionName();
  }

  @Nullable
  public OccurenceInfo goNextOccurence() {
    return myOccurenceNavigator.goNextOccurence();
  }

  public boolean hasNextOccurence() {
    return myOccurenceNavigator.hasNextOccurence();
  }

  public String getPreviousOccurenceActionName() {
    return myOccurenceNavigator.getPreviousOccurenceActionName();
  }

  public boolean hasPreviousOccurence() {
    return myOccurenceNavigator.hasPreviousOccurence();
  }

  protected void rebuildWithAlarm(final Alarm alarm) {
    alarm.cancelAllRequests();
    alarm.addRequest(new Runnable() {
      public void run() {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
          public void run() {
            myTodoTreeBuilder.rebuildCache();
          }
        });
        final Runnable runnable = new Runnable() {
          public void run() {
            updateTree();
          }
        };
        ApplicationManager.getApplication().invokeLater(runnable);
      }
    }, 300);
  }

  private final class MyTreeExpander implements TreeExpander {
    public boolean canCollapse() {
      return true;
    }

    public boolean canExpand() {
      return true;
    }

    public void collapseAll() {
      myTodoTreeBuilder.collapseAll();
    }

    public void expandAll() {
      myTodoTreeBuilder.expandAll(null);
    }
  }

  /**
   * Provides support for "auto scroll to source" functionnality.
   */
  private final class MyAutoScrollToSourceHandler extends AutoScrollToSourceHandler {
    MyAutoScrollToSourceHandler() {
    }

    protected boolean isAutoScrollMode() {
      return mySettings.isAutoScrollToSource();
    }

    protected void setAutoScrollMode(boolean state) {
      mySettings.setAutoScrollToSource(state);
    }
  }

  /**
   * Provides support for "Ctrl+Alt+Up/Down" navigation.
   */
  private final class MyOccurenceNavigator implements OccurenceNavigator {
    public boolean hasNextOccurence() {
      TreePath path = myTree.getSelectionPath();
      if (path == null) {
        return false;
      }
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      if (userObject == null) {
        return false;
      }
      if (userObject instanceof NodeDescriptor && ((NodeDescriptor)userObject).getElement() instanceof TodoItemNode) {
        return myTree.getRowCount() != myTree.getRowForPath(path) + 1;
      }
      else {
        return node.getChildCount() > 0;
      }
    }

    public boolean hasPreviousOccurence() {
      TreePath path = myTree.getSelectionPath();
      if (path == null) {
        return false;
      }
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      return userObject instanceof NodeDescriptor && !isFirst(node);
    }

    private boolean isFirst(final TreeNode node) {
      final TreeNode parent = node.getParent();
      return parent == null || parent.getIndex(node) == 0 && isFirst(parent);
    }

    @Nullable
    public OccurenceInfo goNextOccurence() {
      return goToPointer(getNextPointer());
    }

    @Nullable
    public OccurenceInfo goPreviousOccurence() {
      return goToPointer(getPreviousPointer());
    }

    public String getNextOccurenceActionName() {
      return IdeBundle.message("action.next.todo");
    }

    public String getPreviousOccurenceActionName() {
      return IdeBundle.message("action.previous.todo");
    }

    @Nullable
    private OccurenceInfo goToPointer(TodoItemNode pointer) {
      if (pointer == null) return null;
      myTodoTreeBuilder.select(pointer);
      return new OccurenceInfo(
        new OpenFileDescriptor(myProject, pointer.getValue().getTodoItem().getFile().getVirtualFile(),
                               pointer.getValue().getRangeMarker().getStartOffset()),
        -1,
        -1
      );
    }

    @Nullable
    private TodoItemNode getNextPointer() {
      TreePath path = myTree.getSelectionPath();
      if (path == null) {
        return null;
      }
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      if (!(userObject instanceof NodeDescriptor)) {
        return null;
      }
      Object element = ((NodeDescriptor)userObject).getElement();
      TodoItemNode pointer;
      if (element instanceof TodoItemNode) {
        pointer = myTodoTreeBuilder.getNextPointer((TodoItemNode)element);
      }
      else {
        pointer = myTodoTreeBuilder.getFirstPointerForElement(element);
      }
      return pointer;
    }

    @Nullable
    private TodoItemNode getPreviousPointer() {
      TreePath path = myTree.getSelectionPath();
      if (path == null) {
        return null;
      }
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      if (!(userObject instanceof NodeDescriptor)) {
        return null;
      }
      Object element = ((NodeDescriptor)userObject).getElement();
      TodoItemNode pointer;
      if (element instanceof TodoItemNode) {
        pointer = myTodoTreeBuilder.getPreviousPointer((TodoItemNode)element);
      }
      else {
        Object sibling = myTodoTreeBuilder.getPreviousSibling(element);
        if (sibling == null) {
          return null;
        }
        pointer = myTodoTreeBuilder.getLastPointerForElement(sibling);
      }
      return pointer;
    }
  }

  private final class MyShowPackagesAction extends ToggleAction {
    MyShowPackagesAction() {
      super(IdeBundle.message("action.group.by.packages"), null, PlatformIcons.GROUP_BY_PACKAGES);
    }

    public boolean isSelected(AnActionEvent e) {
      return mySettings.arePackagesShown();
    }

    public void setSelected(AnActionEvent e, boolean state) {
      mySettings.setShownPackages(state);
      myTodoTreeBuilder.setShowPackages(state);
    }
  }

  private final class MyShowModulesAction extends ToggleAction {
    MyShowModulesAction() {
      super(IdeBundle.message("action.group.by.modules"), null, AllIcons.ObjectBrowser.ShowModules);
    }

    public boolean isSelected(AnActionEvent e) {
      return mySettings.areModulesShown();
    }

    public void setSelected(AnActionEvent e, boolean state) {
      mySettings.setShownModules(state);
      myTodoTreeBuilder.setShowModules(state);
    }
  }

  private final class MyFlattenPackagesAction extends ToggleAction {
    MyFlattenPackagesAction() {
      super(IdeBundle.message("action.flatten.packages"), null, PlatformIcons.FLATTEN_PACKAGES_ICON);
    }

    public void update(AnActionEvent e) {
      super.update(e);
      e.getPresentation().setEnabled(mySettings.arePackagesShown());
    }

    public boolean isSelected(AnActionEvent e) {
      return mySettings.areFlattenPackages();
    }

    public void setSelected(AnActionEvent e, boolean state) {
      mySettings.setAreFlattenPackages(state);
      myTodoTreeBuilder.setFlattenPackages(state);
    }
  }

  private final class MyVisibilityWatcher extends VisibilityWatcher {
    public void visibilityChanged() {
      PsiDocumentManager.getInstance(myProject).commitAllDocuments();
      myTodoTreeBuilder.setUpdatable(isShowing());
    }
  }

  private final class MyPreviewAction extends ToggleAction {

    MyPreviewAction() {
      super("Preview Usages", null, PREVIEW_ICON);
    }

    public void update(AnActionEvent e) {
      super.update(e);
    }

    public boolean isSelected(AnActionEvent e) {
      return mySettings.isShowPreview();
    }

    public void setSelected(AnActionEvent e, boolean state) {
      mySettings.setShowPreview(state);
      myUsagePreviewPanel.setVisible(state);
      if (state) {
        updatePreviewPanel();
      }
    }
  }

}
