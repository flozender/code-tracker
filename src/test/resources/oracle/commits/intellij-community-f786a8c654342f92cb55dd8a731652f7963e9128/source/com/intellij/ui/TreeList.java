package com.intellij.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

/**
 * @author Eugene Belyaev
 */
public class TreeList extends JList implements TreeModelListener {
  private TreeModel myTreeModel;
  private boolean myRootVisible = false;
  private DefaultListModel myListModel = new DefaultListModel();

  /**
   * Constructs a <code>JList</code> with an empty model.
   */
  public TreeList(TreeModel treeModel) {
    super.setModel(myListModel);
    myTreeModel = treeModel;
    myTreeModel.addTreeModelListener(this);
    Object root = myTreeModel.getRoot();
    if (root instanceof TreeNode) {
      TreeNode node = (TreeNode) root;
      myListModel.addElement(node);
      if (myTreeModel instanceof DefaultTreeModel && ((DefaultTreeModel)myTreeModel).asksAllowsChildren() && node.getAllowsChildren()) {
        Enumeration enumeration = node.children();
        while (enumeration.hasMoreElements()) {
          Object object = enumeration.nextElement();
          myListModel.addElement(object);
        }
      }
    }
  }

  public boolean isRootVisible() {
    return myRootVisible;
  }

  public void setRootVisible(boolean rootVisible) {
    myRootVisible = rootVisible;
  }

  /**
   * Sets the model that represents the contents or "value" of the
   * list and clears the list selection after notifying
   * <code>PropertyChangeListeners</code>.
   * <p>
   * This is a JavaBeans bound property.
   *
   * @param model  the <code>ListModel</code> that provides the
   *						list of items for display
   * @exception IllegalArgumentException  if <code>model</code> is
   *						<code>null</code>
   * @see #getModel
   * @beaninfo
   *       bound: true
   *   attribute: visualUpdate true
   * description: The object that contains the data to be drawn by this JList.
   */
  public void setModel(ListModel model) {
    throw new UnsupportedOperationException("TreeList accepts only TreeModel as a model");
  }

  /**
   * <p>Invoked after a node (or a set of siblings) has changed in some
   * way. The node(s) have not changed locations in the tree or
   * altered their children arrays, but other attributes have
   * changed and may affect presentation. Example: the name of a
   * file has changed, but it is in the same location in the file
   * system.</p>
   * <p>To indicate the root has changed, childIndices and children
   * will be null. </p>
   *
   * <p>Use <code>e.getPath()</code>
   * to get the parent of the changed node(s).
   * <code>e.getChildIndices()</code>
   * returns the index(es) of the changed node(s).</p>
   */
  public void treeNodesChanged(TreeModelEvent e) {
    //To change body of implemented methods use Options | File Templates.
  }

  /**
   * <p>Invoked after nodes have been inserted into the tree.</p>
   *
   * <p>Use <code>e.getPath()</code>
   * to get the parent of the new node(s).
   * <code>e.getChildIndices()</code>
   * returns the index(es) of the new node(s)
   * in ascending order.</p>
   */
  public void treeNodesInserted(TreeModelEvent e) {
    //To change body of implemented methods use Options | File Templates.
  }

  /**
   * <p>Invoked after nodes have been removed from the tree.  Note that
   * if a subtree is removed from the tree, this method may only be
   * invoked once for the root of the removed subtree, not once for
   * each individual set of siblings removed.</p>
   *
   * <p>Use <code>e.getPath()</code>
   * to get the former parent of the deleted node(s).
   * <code>e.getChildIndices()</code>
   * returns, in ascending order, the index(es)
   * the node(s) had before being deleted.</p>
   */
  public void treeNodesRemoved(TreeModelEvent e) {
    //To change body of implemented methods use Options | File Templates.
  }

  /**
   * <p>Invoked after the tree has drastically changed structure from a
   * given node down.  If the path returned by e.getPath() is of length
   * one and the first element does not identify the current root node
   * the first element should become the new root of the tree.<p>
   *
   * <p>Use <code>e.getPath()</code>
   * to get the path to the node.
   * <code>e.getChildIndices()</code>
   * returns null.</p>
   */
  public void treeStructureChanged(TreeModelEvent e) {
    //To change body of implemented methods use Options | File Templates.
  }



  @SuppressWarnings({"HardCodedStringLiteral"})
  public static void main(String[] args) {
    JFrame frame = new JFrame("TreeList Showcase");
    frame.setLocation(0, 0);
    frame.setSize(800, 600);
    frame.addWindowListener(
        new WindowAdapter() {
          /**
           * Invoked when a window is in the process of being closed.
           * The close operation can be overridden at this point.
           */
          public void windowClosing(WindowEvent e) {
            System.exit(0);
          }
        }
    );
    Container contentPane = frame.getContentPane();
    contentPane.setLayout(new BorderLayout());
    DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Sample root node");
    for (int i = 0; i < 5; i++) {
      DefaultMutableTreeNode node = new DefaultMutableTreeNode("Node #" + (i + 1));
      rootNode.add(node);
    }
    DefaultTreeModel treeModel = new DefaultTreeModel(rootNode, true);
    TreeList treeList = new TreeList(treeModel);
    contentPane.add(treeList);
    frame.setVisible(true);
  }
}
