package com.intellij.ui;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.util.containers.Convertor;
import com.intellij.util.ui.Tree;
import com.intellij.util.ui.tree.TreeUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class TreeSpeedSearch extends SpeedSearchBase<JTree> {
  private static final Convertor<TreePath, String> TO_STRING = new Convertor<TreePath, String>() {
    public String convert(TreePath object) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)object.getLastPathComponent();
      return node.toString();
    }
  };
  private final Convertor<TreePath, String> myToStringConvertor;
  public static final Convertor<TreePath, String> NODE_DESCRIPTOR_TOSTRING = new Convertor<TreePath, String>() {
        public String convert(TreePath path) {
          final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
          final Object userObject = node.getUserObject();
          if (userObject instanceof NodeDescriptor) {
            NodeDescriptor descr = (NodeDescriptor) userObject;
            return descr.toString();
          }
          return TO_STRING.convert(path);
        }
      };

  /**
   * @deprecated You should use {@link TreeSpeedSearch#TreeSpeedSearch(JTree)}
   * @see com.intellij.util.ui.Tree#getNextMatch(String, int, javax.swing.text.Position.Bias)
   */
  public TreeSpeedSearch(JTree tree, Convertor<TreePath, String> toStringConvertor) {
    super(tree);
    myToStringConvertor = toStringConvertor;
  }

  public TreeSpeedSearch(JTree tree) {
    this(tree, TO_STRING);
  }

  public TreeSpeedSearch(Tree tree, Convertor<TreePath, String> toString) {
    super(tree);
    myToStringConvertor = toString;
  }

  protected void selectElement(Object element, String selectedText) {
    TreeUtil.selectPath(myComponent, (TreePath)element);
  }

  protected int getSelectedIndex() {
    int[] selectionRows = myComponent.getSelectionRows();
    return selectionRows == null || selectionRows.length == 0 ? -1 : selectionRows[0];
  }

  protected Object[] getAllElements() {
    TreePath[] paths = new TreePath[myComponent.getRowCount()];
    for(int i = 0; i < paths.length; i++){
      paths[i] = myComponent.getPathForRow(i);
    }
    return paths;
  }

  protected String getElementText(Object element) {
    TreePath path = (TreePath)element;
    String string = myToStringConvertor.convert(path);
    if (string == null) return TO_STRING.convert(path);
    return string;
  }
}