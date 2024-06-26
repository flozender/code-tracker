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
package com.intellij.util.ui.treetable;

import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * A TreeCellRenderer that displays a JTree.
 */
public class TreeTableCellRenderer implements TableCellRenderer {
  private TreeTable myTreeTable;
  private final TreeTableTree myTree;
  private TreeCellRenderer myTreeCellRenderer;
  private Border myDefaultBorder = UIUtil.getTableFocusCellHighlightBorder();


  public TreeTableCellRenderer(TreeTable treeTable, TreeTableTree tree) {
    myTreeTable = treeTable;
    myTree = tree;
  }

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    if (myTreeCellRenderer != null)
      myTree.setCellRenderer(myTreeCellRenderer);
    if (isSelected){
      myTree.setBackground(table.getSelectionBackground());
    }
    else{
      myTree.setBackground(table.getBackground());
    }
    TableModel model = myTreeTable.getModel();
    myTree.setTreeTableTreeBorder(hasFocus && model.getColumnClass(column).equals(TreeTableModel.class) ? myDefaultBorder : null);
    myTree.setVisibleRow(row);
    return myTree;
  }

  public void setCellRenderer(TreeCellRenderer treeCellRenderer) {
    myTreeCellRenderer = treeCellRenderer;
  }
  public void setDefaultBorder(Border border) {
    myDefaultBorder = border;
  }
  public void putClientProperty(String s, String s1) {
    myTree.putClientProperty(s, s1);
  }

  public void setRootVisible(boolean b) {
    myTree.setRootVisible(b);
  }

  public void setShowsRootHandles(boolean b) {
    myTree.setShowsRootHandles(b);
  }

}
