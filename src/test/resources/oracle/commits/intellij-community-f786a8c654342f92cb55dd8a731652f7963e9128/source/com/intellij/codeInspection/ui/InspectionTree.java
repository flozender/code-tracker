/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Nov 4, 2001
 * Time: 5:19:35 PM
 */
package com.intellij.codeInspection.ui;

import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ex.DescriptorProviderInspection;
import com.intellij.codeInspection.ex.InspectionTool;
import com.intellij.codeInspection.ex.InspectionToolsPanel;
import com.intellij.codeInspection.reference.RefClass;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.TreeToolTipHandler;
import com.intellij.util.containers.Convertor;
import com.intellij.util.ui.Tree;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class InspectionTree extends Tree {
  private final HashSet<Object> myExpandedUserObjects;
  private SelectionPath mySelectionPath;
  private static final RefElement[] EMPTY_ELEMENTS_ARRAY = new RefElement[0];
  private static final ProblemDescriptor[] EMPTY_DESCRIPTORS = new ProblemDescriptor[0];

  public InspectionTree(final Project project) {
    super(new InspectionRootNode(project));

    setCellRenderer(new CellRenderer());//project));
    setShowsRootHandles(true);
    UIUtil.setLineStyleAngled(this);
    addTreeWillExpandListener(new ExpandListener());

    myExpandedUserObjects = new HashSet<Object>();
    myExpandedUserObjects.add(project);

    TreeToolTipHandler.install(this);
    TreeUtil.installActions(this);
    new TreeSpeedSearch(this, new Convertor<TreePath, String>() {
      public String convert(TreePath o) {
        return InspectionToolsPanel.getDisplayTextToSort(((DefaultMutableTreeNode)o.getLastPathComponent()).toString());
      }
    });

    addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        TreePath newSelection = e.getNewLeadSelectionPath();
        if (newSelection != null) {
          mySelectionPath = new SelectionPath(newSelection);
        }
      }
    });
  }

  public void removeAllNodes() {
    getRoot().removeAllChildren();
    nodeStructureChanged(getRoot());
  }

  public InspectionTreeNode getRoot() {
    return (InspectionTreeNode)getModel().getRoot();
  }

  @Nullable
  public InspectionTool getSelectedTool() {
    final TreePath[] paths = getSelectionPaths();
    if (paths == null) return null;
    InspectionTool tool = null;
    for (TreePath path : paths) {
      Object[] nodes = path.getPath();
      for (int j = nodes.length - 1; j >= 0; j--) {
        Object node = nodes[j];
        if (node instanceof InspectionNode) {
          if (tool == null) {
            tool = ((InspectionNode)node).getTool();
          }
          else if (tool != ((InspectionNode)node).getTool()) {
            return null;
          }
          break;
        }
      }
    }

    return tool;
  }

  public RefElement[] getSelectedElements() {
    TreePath[] selectionPaths = getSelectionPaths();
    if (selectionPaths != null) {
      final InspectionTool selectedTool = getSelectedTool();
      if (selectedTool == null) return EMPTY_ELEMENTS_ARRAY;

      Set<RefElement> result = new HashSet<RefElement>();
      for (TreePath selectionPath : selectionPaths) {
        final InspectionTreeNode node = (InspectionTreeNode)selectionPath.getLastPathComponent();
        addElementsInNode(node, result);
      }
      return result.toArray(new RefElement[result.size()]);
    }
    return EMPTY_ELEMENTS_ARRAY;
  }

  private void addElementsInNode(InspectionTreeNode node, Collection<RefElement> out) {
    if (!node.isValid()) return;
    if (node instanceof RefElementNode) {
      out.add(((RefElementNode)node).getElement());
    }
    if (node instanceof ProblemDescriptionNode) {
      out.add(((ProblemDescriptionNode)node).getElement());
    }
    else if (node instanceof InspectionTreeNode) {
      final Enumeration children = node.children();
      while (children.hasMoreElements()) {
        InspectionTreeNode child = (InspectionTreeNode)children.nextElement();
        addElementsInNode(child, out);
      }
    }
  }

  public ProblemDescriptor[] getSelectedDescriptors() {
    final InspectionTool tool = getSelectedTool();
    if (getSelectionCount() == 0 || !(tool instanceof DescriptorProviderInspection)) return EMPTY_DESCRIPTORS;
    final TreePath[] paths = getSelectionPaths();
    Collection<RefElement> out = new ArrayList<RefElement>();
    Set<ProblemDescriptor> descriptors = new com.intellij.util.containers.HashSet<ProblemDescriptor>();
    for (TreePath path : paths) {
      Object node = path.getLastPathComponent();
      if (node instanceof ProblemDescriptionNode) {
        final ProblemDescriptionNode problemNode = (ProblemDescriptionNode)node;
        descriptors.add(problemNode.getDescriptor());
      } else if (node instanceof InspectionTreeNode){
        addElementsInNode((InspectionTreeNode)node, out);
      }
    }
    for (RefElement refElement : out) {
      final ProblemDescriptor[] descriptions = ((DescriptorProviderInspection)tool).getDescriptions(refElement);
      if (descriptions != null) {
        descriptors.addAll(Arrays.asList(descriptions));
      }
    }
    return descriptors.toArray(new ProblemDescriptor[descriptors.size()]);
  }

  public List<RefElement> getElementsToSuppressInSubTree(InspectionTreeNode node){
    List<RefElement> result = new ArrayList<RefElement>();
    if (node instanceof RefElementNode){
      result.add(((RefElementNode)node).getElement());
    } else if (node instanceof ProblemDescriptionNode){
      result.add(((ProblemDescriptionNode)node).getElement());
    } else {
      for(int i = 0; i < node.getChildCount(); i++){
        result.addAll(getElementsToSuppressInSubTree((InspectionTreeNode)node.getChildAt(i)));
      }
    }
    return result;
  }

  public void nodeStructureChanged(InspectionTreeNode node) {
    ((DefaultTreeModel)getModel()).nodeStructureChanged(node);
  }

  private class ExpandListener implements TreeWillExpandListener {
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
      final InspectionTreeNode node = (InspectionTreeNode)event.getPath().getLastPathComponent();
      myExpandedUserObjects.add(node.getUserObject());
      if (node instanceof RefElementNode && !node.children().hasMoreElements()) {       
        sortChildren(node);
      }

      // Smart expand
      if (node.getChildCount() == 1) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          public void run() {
            expandPath(new TreePath(node.getPath()));
          }
        });
      }
    }

    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
      InspectionTreeNode node = (InspectionTreeNode)event.getPath().getLastPathComponent();
      myExpandedUserObjects.remove(node.getUserObject());
    }
  }

  public void restoreExpantionAndSelection() {
    restoreExpantion();
    if (mySelectionPath != null) {
      mySelectionPath.restore();
    }
  }

  private void restoreExpantion() {
    restoreExpantionStatus((InspectionTreeNode)getModel().getRoot());
  }


  private void restoreExpantionStatus(InspectionTreeNode node) {
    if (myExpandedUserObjects.contains(node.getUserObject())) {
      TreeNode[] pathToNode = node.getPath();
      expandPath(new TreePath(pathToNode));
      Enumeration children = node.children();
      while (children.hasMoreElements()) {
        InspectionTreeNode childNode = (InspectionTreeNode)children.nextElement();
        restoreExpantionStatus(childNode);
      }
    }
  }

  private class CellRenderer extends ColoredTreeCellRenderer {
    /*  private Project myProject;
      InspectionManagerEx myManager;
      public CellRenderer(Project project) {
        myProject = project;
        myManager = (InspectionManagerEx)InspectionManager.getInstance(myProject);
      }*/

    public void customizeCellRenderer(JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
      InspectionTreeNode node = (InspectionTreeNode)value;

      if (!node.isWritable()) {
        append(InspectionsBundle.message("inspection.read.only.node.prefix"), SimpleTextAttributes.ERROR_ATTRIBUTES);
      }

      append(node.toString(), appearsBold(node)
                              ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
                              : getMainForegroundAttributes(node));

      if (!node.isValid()) {
        append(" " + InspectionsBundle.message("inspection.invalid.node.text"), SimpleTextAttributes.ERROR_ATTRIBUTES);
      }

      int problemCount = node.getProblemCount();
      if (problemCount > 0) {
        append(" " + InspectionsBundle.message("inspection.problem.descriptor.count", problemCount), SimpleTextAttributes.GRAYED_ATTRIBUTES);
      }

      setIcon(node.getIcon(expanded));
      /* if (node instanceof InspectionNode){
         final HighlightDisplayLevel level = myManager.getCurrentProfile().getErrorLevel(HighlightDisplayKey.find(((InspectionNode)node).getTool().getDisplayName()));
         LayeredIcon icon = new LayeredIcon(2);
         icon.setIcon(node.getIcon(expanded), 1);
         icon.setIcon(level.getIcon(),
                      0);
         setIcon(icon);
       }*/
    }

    private SimpleTextAttributes getMainForegroundAttributes(InspectionTreeNode node) {
      SimpleTextAttributes foreground = SimpleTextAttributes.REGULAR_ATTRIBUTES;
      if (node instanceof RefElementNode) {
        RefElement refElement = ((RefElementNode)node).getElement();

        if (refElement instanceof RefClass) {
          RefElement defaultConstructor = ((RefClass)refElement).getDefaultConstructor();
          if (defaultConstructor != null) refElement = defaultConstructor;
        }

        if (refElement.isEntry() && refElement.isPermanentEntry()) {
          foreground = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, Color.blue);
        }
      }
      return foreground;
    }

    private boolean appearsBold(Object node) {
      return ((InspectionTreeNode)node).appearsBold();
    }
  }

  public void sort() {
    sortChildren(getRoot());
  }

  protected void sortChildren(InspectionTreeNode node) {
    TreeUtil.sort(node, InspectionResultsViewComparator.getInstance());
  }

  private class SelectionPath {
    private Object[] myPath;
    private int[] myIndicies;

    public SelectionPath(TreePath path) {
      myPath = path.getPath();
      myIndicies = new int[myPath.length];
      for (int i = 0; i < myPath.length - 1; i++) {
        InspectionTreeNode node = (InspectionTreeNode)myPath[i];
        myIndicies[i + 1] = getChildIndex(node, (InspectionTreeNode)myPath[i + 1]);
      }
    }

    private int getChildIndex(InspectionTreeNode node, InspectionTreeNode child) {
      int idx = 0;
      Enumeration children = node.children();
      while (children.hasMoreElements()) {
        InspectionTreeNode ch = (InspectionTreeNode)children.nextElement();
        if (ch == child) break;
        idx++;
      }
      return idx;
    }

    public void restore() {
      getSelectionModel().removeSelectionPaths(getSelectionModel().getSelectionPaths());
      TreeUtil.selectPath(InspectionTree.this, restorePath());
    }

    private TreePath restorePath() {
      ArrayList<Object> newPath = new ArrayList<Object>();

      newPath.add(getModel().getRoot());
      restorePath(newPath, 1);

      return new TreePath(newPath.toArray(new InspectionTreeNode[newPath.size()]));
    }

    private void restorePath(ArrayList<Object> newPath, int idx) {
      if (idx >= myPath.length) return;
      InspectionTreeNode oldNode = (InspectionTreeNode)myPath[idx];

      InspectionTreeNode newRoot = (InspectionTreeNode)newPath.get(idx - 1);


      InspectionResultsViewComparator comparator = InspectionResultsViewComparator.getInstance();
      Enumeration children = newRoot.children();
      while (children.hasMoreElements()) {
        InspectionTreeNode child = (InspectionTreeNode)children.nextElement();
        if (comparator.compare(child, oldNode) == 0) {
          newPath.add(child);
          restorePath(newPath, idx + 1);
          return;
        }
      }

      // Exactly same element not found. Trying to select somewhat near.
      int count = newRoot.getChildCount();
      if (count > 0) {
        if (myIndicies[idx] < count) {
          newPath.add(newRoot.getChildAt(myIndicies[idx]));
        }
        else {
          newPath.add(newRoot.getChildAt(count - 1));
        }
      }
    }
  }
}
