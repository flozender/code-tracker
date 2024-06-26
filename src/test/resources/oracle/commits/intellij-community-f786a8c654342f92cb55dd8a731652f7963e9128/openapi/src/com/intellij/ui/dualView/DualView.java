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
package com.intellij.ui.dualView;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.fileView.DualViewColumnInfo;
import com.intellij.peer.PeerFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.UIHelper;
import com.intellij.ui.UIBundle;
import com.intellij.ui.table.BaseTableView;
import com.intellij.ui.table.SelectionProvider;
import com.intellij.ui.table.TableView;
import com.intellij.util.config.Storage;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.Table;
import com.intellij.util.ui.Tree;
import com.intellij.util.ui.treetable.ListTreeTableModelOnColumns;
import com.intellij.util.ui.treetable.TreeTableModel;
import com.intellij.execution.ExecutionBundle;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;

import org.jetbrains.annotations.NonNls;

public class DualView extends JPanel {
  private final CardLayout myCardLayout;
  private TreeTableView myTreeView;

  private JTable myCurrentView = null;

  private TableView myFlatView;
  @NonNls private static final String TREE = "TREE";
  @NonNls private static final String FLAT = "FLAT";
  private TreeCellRenderer myTreeCellRenderer;
  private boolean myRootVisible;
  private boolean myTableRefreshingIsLocked = false;
  private CellWrapper myCellWrapper;

  private final Storage.PropertiesComponentStorage myFlatStorage;
  private final Storage.PropertiesComponentStorage myTreeStorage;
  private final PropertyChangeListener myPropertyChangeListener;

  public DualView(Object root, DualViewColumnInfo[] columns, @NonNls String columnServiceKey, Project project) {
    super(new CardLayout());

    myTreeStorage = new Storage.PropertiesComponentStorage(columnServiceKey + "_tree",
                                                           PropertiesComponent.getInstance(project));
    myFlatStorage = new Storage.PropertiesComponentStorage(columnServiceKey + "_flat",
                                                           PropertiesComponent.getInstance(project));

    myCardLayout = (CardLayout)getLayout();

    add(createTreeComponent(columns, (TreeNode)root), TREE);

    add(createFlatComponent(columns), FLAT);

    (myTreeView.getTreeViewModel()).addTreeModelListener(new TreeModelListener() {
      public void treeNodesInserted(TreeModelEvent e) {
        refreshFlatModel();
      }

      public void treeNodesRemoved(TreeModelEvent e) {
        refreshFlatModel();
      }

      public void treeStructureChanged(TreeModelEvent e) {
        refreshFlatModel();
      }

      public void treeNodesChanged(TreeModelEvent e) {
        refreshFlatModel();
      }
    });

    setRootVisible(true);

    switchToTheFlatMode();

    restoreState();

    myPropertyChangeListener = new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        saveState();
      }
    };

    addWidthListenersTo(myTreeView);
    addWidthListenersTo(myFlatView);
  }

  private void addWidthListenersTo(Table treeView) {
    TableColumnModel columnModel = treeView.getColumnModel();
    int columnCount = columnModel.getColumnCount();
    for (int i = 0; i < columnCount; i++) {
      columnModel.getColumn(i).addPropertyChangeListener(myPropertyChangeListener);
    }
  }

  public void restoreState() {
    BaseTableView.restore(myFlatStorage, myFlatView);
    BaseTableView.restore(myTreeStorage, myTreeView);
  }

  public void lockTableRefreshing() {
    myTableRefreshingIsLocked = true;
  }

  public void unlockTableRefreshingAndRefresh() {
    myTableRefreshingIsLocked = false;
    refreshFlatModel();
  }

  private void refreshFlatModel() {
    if (myTableRefreshingIsLocked) return;
    ((ListTableModel)myFlatView.getModel()).setItems(myTreeView.getFlattenItems());
  }

  private ColumnInfo[] createTreeColumns(DualViewColumnInfo[] columns) {
    Collection<ColumnInfo> result = new ArrayList<ColumnInfo>();

    final ColumnInfo firstColumn = columns[0];
    ColumnInfo firstTreeColumn = new ColumnInfo(firstColumn.getName()) {
      public Object valueOf(Object object) {
        return firstColumn.valueOf(object);
      }

      public Class getColumnClass() {
        return TreeTableModel.class;
      }

      public boolean isCellEditable(Object o) {
        return true;
      }
    };
    result.add(firstTreeColumn);
    for (int i = 1; i < columns.length; i++) {
      DualViewColumnInfo column = columns[i];
      if (column.shouldBeShownIsTheTree()) result.add(column);
    }

    return result.toArray(new ColumnInfo[result.size()]);
  }

  public void switchToTheFlatMode() {
    if (myFlatView == myCurrentView) return;
    changeVeiwTo(myFlatView);
    copySelection(myTreeView, myFlatView);
    myCardLayout.show(this, FLAT);
  }

  private void changeVeiwTo(JTable view) {
    myCurrentView = view;
  }

  private void copySelection(SelectionProvider from, SelectionProvider to) {
    to.clearSelection();

    Collection selection = from.getSelection();

    for (Iterator each = selection.iterator(); each.hasNext();) {
      to.addSelection(each.next());
    }
  }

  public void switchToTheTreeMode() {
    if (myTreeView == myCurrentView) return;
    changeVeiwTo(myTreeView);
    copySelection(myFlatView, myTreeView);
    myCardLayout.show(this, TREE);
  }

  private Component createTreeComponent(DualViewColumnInfo[] columns, TreeNode root) {
    myTreeView = new TreeTableView(new ListTreeTableModelOnColumns(root, createTreeColumns(columns))) {
      public TableCellRenderer getCellRenderer(int row, int column) {
        return createWrappedRenderer(super.getCellRenderer(row, column));
      }
    };
    myTreeView.getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    JPanel result = new JPanel(new BorderLayout());
    result.add(ScrollPaneFactory.createScrollPane(myTreeView), BorderLayout.CENTER);
    return result;
  }

  private Component createFlatComponent(DualViewColumnInfo[] columns) {

    ArrayList<ColumnInfo> shownColumns = new ArrayList<ColumnInfo>();

    for (int i = 0; i < columns.length; i++) {
      DualViewColumnInfo column = columns[i];
      if (column.shouldBeShownIsTheTable()) shownColumns.add(column);
    }

    ListTableModel flatModel = new ListTableModel(shownColumns.toArray(new ColumnInfo[shownColumns.size()]));
    myFlatView = new TableView(flatModel) {
      public TableCellRenderer getCellRenderer(int row, int column) {
        return createWrappedRenderer(super.getCellRenderer(row, column));
      }
    };

    refreshFlatModel();

    JPanel result = new JPanel(new BorderLayout());
    result.add(ScrollPaneFactory.createScrollPane(myFlatView), BorderLayout.CENTER);
    return result;
  }

  private TableCellRenderer createWrappedRenderer(final TableCellRenderer renderer) {
    if (myCellWrapper == null) {
      return renderer;
    }
    else {
      return new TableCellRenderer() {
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
          Component result = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
          Object treeNode = null;
          if (myCurrentView == myTreeView) {
            TreePath path = myTreeView.getTree().getPathForRow(row);
            if (path != null) {
              treeNode = path.getLastPathComponent();
            }
          }
          else if (myCurrentView == myFlatView) {
            treeNode = myFlatView.getItems().get(row);
          }

          myCellWrapper.wrap(result, table, value, isSelected, hasFocus, row, column, treeNode);
          return result;

        }
      };
    }
  }

  public void expandAll() {
    expandPath(myTreeView.getTree(), new TreePath(myTreeView.getTree().getModel().getRoot()));
  }

  public void collapseAll() {
    collapsePath(myTreeView.getTree(), new TreePath(myTreeView.getTree().getModel().getRoot()));
  }

  private void expandPath(JTree tree, TreePath path) {
    tree.expandPath(path);

    final TreeNode node = ((TreeNode)path.getLastPathComponent());
    final Enumeration children = node.children();
    while (children.hasMoreElements()) {
      TreeNode child = (TreeNode)children.nextElement();
      expandPath(tree, path.pathByAddingChild(child));
    }
  }

  private void collapsePath(JTree tree, TreePath path) {

    final TreeNode node = ((TreeNode)path.getLastPathComponent());
    final Enumeration children = node.children();
    while (children.hasMoreElements()) {
      TreeNode child = (TreeNode)children.nextElement();
      collapsePath(tree, path.pathByAddingChild(child));
    }

    if (!((path.getLastPathComponent() == tree.getModel().getRoot()) && !myRootVisible)) {
      tree.collapsePath(path);
    }
  }

  public List getSelection() {
    ArrayList result = new ArrayList();
    SelectionProvider visibleTable = (SelectionProvider)getVisibleTable();
    Collection selection = visibleTable.getSelection();
    for (Iterator each = selection.iterator(); each.hasNext();) {
      result.add((Object)each.next());
    }
    return result;
  }

  private JTable getVisibleTable() {
    return myCurrentView;
  }

  public void setShowGrid(boolean aBoolean) {
    myTreeView.setShowGrid(aBoolean);
  }

  public void setSelectionInterval(int first, int last) {
    myTreeView.getSelectionModel().addSelectionInterval(first, last);
    myFlatView.getSelectionModel().addSelectionInterval(first, last);
  }

  public void addListSelectionListener(ListSelectionListener listSelectionListener) {
    myTreeView.getSelectionModel().addListSelectionListener(listSelectionListener);
    myFlatView.getSelectionModel().addListSelectionListener(listSelectionListener);
  }

  public void changeColumnSet(DualViewColumnInfo[] columns) {
    myTreeView.setTableModel(new ListTreeTableModelOnColumns((TreeNode)myTreeView.getTreeViewModel().getRoot(),
                                                             createTreeColumns(columns)));
    myFlatView.setModel(new ListTableModel(columns));
    if (myTreeCellRenderer != null) myTreeView.setTreeCellRenderer(myTreeCellRenderer);
    setRootVisible(myRootVisible);

    refreshFlatModel();

    addWidthListenersTo(myTreeView);
    addWidthListenersTo(myFlatView);
  }

  public Tree getTree() {
    return myTreeView.getTree();
  }

  public TreeTableView getTreeView() {
    return myTreeView;
  }

  public TableView getFlatView() {
    return myFlatView;
  }

  public void setRootVisible(boolean aBoolean) {
    myRootVisible = aBoolean;
    myTreeView.setRootVisible(myRootVisible);
  }

  public void setTreeCellRenderer(TreeCellRenderer cellRenderer) {
    myTreeCellRenderer = cellRenderer;
    myTreeView.setTreeCellRenderer(cellRenderer);
  }

  public AnAction getExpandAllAction() {
    return new AnAction(UIBundle.message("tree.view.expand.all.action.name"), null, IconLoader.getIcon("/actions/expandall.png")) {
      public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setVisible(true);
        presentation.setEnabled(myCurrentView == myTreeView);
      }

      public void actionPerformed(AnActionEvent e) {
        expandAll();
      }
    };
  }

  public AnAction getCollapseAllAction() {
    return new AnAction(UIBundle.message("tree.view.collapse.all.action.name"), null, IconLoader.getIcon("/actions/collapseall.png")) {
      public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setVisible(true);
        presentation.setEnabled(myCurrentView == myTreeView);
      }

      public void actionPerformed(AnActionEvent e) {
        collapseAll();
      }
    };
  }

  public void setCellWrapper(CellWrapper wrapper) {
    myCellWrapper = wrapper;
  }

  public void installEditSourceOnDoubleClickHandler() {
    UIHelper uiHelper = PeerFactory.getInstance().getUIHelper();
    uiHelper.installEditSourceOnDoubleClick(myTreeView);
    uiHelper.installEditSourceOnDoubleClick(myFlatView);
  }

  public void installDoubleClickHandler(AnAction action) {
    action.registerCustomShortcutSet(CommonShortcuts.DOUBLE_CLICK_1, myFlatView);
    action.registerCustomShortcutSet(CommonShortcuts.DOUBLE_CLICK_1, myTreeView);
  }

  public void dispose() {
    saveState();
  }

  public void saveState() {
    BaseTableView.store(myFlatStorage, myFlatView);
    BaseTableView.store(myTreeStorage, myTreeView);
  }

  public void setRoot(TreeNode node) {
    myTreeView.getTreeViewModel().setRoot(node);
  }

  public void rebuild() {
    ((AbstractTableModel)myFlatView.getModel()).fireTableDataChanged();
    ((AbstractTableModel)myTreeView.getModel()).fireTableDataChanged();
  }
}
