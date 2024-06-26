package com.intellij.application.options;

import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.HashMap;
import com.intellij.util.ui.AbstractTableCellEditor;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.treetable.ListTreeTableModel;
import com.intellij.util.ui.treetable.TreeTable;
import com.intellij.util.ui.treetable.TreeTableCellRenderer;
import com.intellij.util.ui.treetable.TreeTableModel;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author max
 */
public abstract class OptionTableWithPreviewPanel extends JPanel {
  private static final Logger LOG = Logger.getInstance("#com.intellij.application.options.CodeStyleSpacesPanel");

  public final ColumnInfo TITLE = new ColumnInfo("TITLE") {
    public Object valueOf(Object o) {
      if (o instanceof MyTreeNode) {
        MyTreeNode node = (MyTreeNode)o;
        return node.getText();
      }
      return o.toString();
    }

    public Class getColumnClass() {
      return TreeTableModel.class;
    }
  };

  public final ColumnInfo VALUE = new ColumnInfo("VALUE") {
    private TableCellEditor myEditor = new MyValueEditor();
    private TableCellRenderer myRenderer = new MyValueRenderer();

    public Object valueOf(Object o) {
      if (o instanceof MyTreeNode) {
        MyTreeNode node = (MyTreeNode)o;
        return node.getValue();
      }

      return null;
    }

    public TableCellRenderer getRenderer(Object o) {
      return myRenderer;
    }

    public TableCellEditor getEditor(Object item) {
      return myEditor;
    }

    public boolean isCellEditable(Object o) {
      return o instanceof MyTreeNode;
    }

    public void setValue(Object o, Object o1) {
      MyTreeNode node = (MyTreeNode)o;
      node.setValue(o1);
    }
  };

  public final ColumnInfo[] COLUMNS = new ColumnInfo[]{TITLE, VALUE};

  private TreeCellRenderer myTitleRenderer = new TreeCellRenderer() {
    private JLabel myLabel = new JLabel();

    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean selected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {
      if (value instanceof MyTreeNode) {
        MyTreeNode node = (MyTreeNode)value;
        myLabel.setText(node.getText());
        myLabel.setFont(
          myLabel.getFont().deriveFont(node.getKey() instanceof IntSelectionOptionKey ? Font.BOLD : Font.PLAIN));
      }
      else {
        myLabel.setText(value.toString());
        myLabel.setFont(myLabel.getFont().deriveFont(Font.BOLD));
      }

      Color foreground = selected
                         ? UIManager.getColor("Table.selectionForeground")
                         : UIManager.getColor("Table.textForeground");
      myLabel.setForeground(foreground);

      return myLabel;
    }
  };

  private Editor myEditor;
  private TreeTable myTreeTable;
  private boolean toUpdatePreview = true;
  private HashMap myKeyToFieldMap = new HashMap();
  private ArrayList myKeys = new ArrayList();
  private CodeStyleSettings mySettings;

  public OptionTableWithPreviewPanel(CodeStyleSettings settings) {
    mySettings = settings;
    setLayout(new GridBagLayout());

    initTables();

    myTreeTable = createOptionsTree();
    add(ScrollPaneFactory.createScrollPane(myTreeTable),
        new GridBagConstraints(0, 0, 1, 1, 0, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                               new Insets(7, 7, 3, 4), 0, 0));

    add(createPreviewPanel(),
        new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                               new Insets(0, 0, 0, 4), 0, 0));

    reset();
  }


  public void dispose() {
    EditorFactory.getInstance().releaseEditor(myEditor);
  }

  protected TreeTable createOptionsTree() {
    DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
    String groupName = "";
    DefaultMutableTreeNode groupNode = null;
    for (int i = 0; i < myKeys.size(); i++) {
      if (myKeys.get(i) instanceof BooleanOptionKey) {
        BooleanOptionKey key = (BooleanOptionKey)myKeys.get(i);
        String newGroupName = key.groupName;
        if (!newGroupName.equals(groupName) || groupNode == null) {
          groupName = newGroupName;
          groupNode = new DefaultMutableTreeNode(newGroupName);
          rootNode.add(groupNode);
        }
        groupNode.add(new MyTreeNode(key, key.cbName));
      }
      else if (myKeys.get(i) instanceof IntSelectionOptionKey) {
        IntSelectionOptionKey key = (IntSelectionOptionKey)myKeys.get(i);
        String newGroupName = key.groupName;
        if (!newGroupName.equals(groupName) || groupNode == null) {
          groupName = newGroupName;
          groupNode = new MyTreeNode(key, key.groupName);
          rootNode.add(groupNode);
        }
        else {
          LOG.assertTrue(false);
        }
      }
    }

    ListTreeTableModel model = new ListTreeTableModel(rootNode, COLUMNS);
    TreeTable treeTable = new TreeTable(model) {
      public TreeTableCellRenderer createTableRenderer(TreeTableModel treeTableModel) {
        TreeTableCellRenderer tableRenderer = super.createTableRenderer(treeTableModel);
        tableRenderer.putClientProperty("JTree.lineStyle", "Angled");
        tableRenderer.setRootVisible(false);
        tableRenderer.setShowsRootHandles(true);

        return tableRenderer;
      }

      public TableCellRenderer getCellRenderer(int row, int column) {
        TreePath treePath = getTree().getPathForRow(row);
        if (treePath == null) return super.getCellRenderer(row, column);

        Object node = treePath.getLastPathComponent();

        TableCellRenderer renderer = COLUMNS[column].getRenderer(node);
        return renderer == null ? super.getCellRenderer(row, column) : renderer;
      }

      public TableCellEditor getCellEditor(int row, int column) {
        TreePath treePath = getTree().getPathForRow(row);
        if (treePath == null) return super.getCellEditor(row, column);

        Object node = treePath.getLastPathComponent();
        TableCellEditor editor = COLUMNS[column].getEditor(node);
        return editor == null ? super.getCellEditor(row, column) : editor;
      }
    };

    treeTable.setRootVisible(false);

    final JTree tree = treeTable.getTree();
    tree.setCellRenderer(myTitleRenderer);
    tree.setShowsRootHandles(true);
    //myTreeTable.setRowHeight(new JComboBox(new String[]{"Sample Text"}).getPreferredSize().height);
    treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    treeTable.setTableHeader(null);

    expandTree(tree);

    int maxWidth = tree.getPreferredScrollableViewportSize().width + 10;
    final TableColumn titleColumn = treeTable.getColumnModel().getColumn(0);
    titleColumn.setPreferredWidth(maxWidth);
    titleColumn.setMinWidth(maxWidth);
    titleColumn.setMaxWidth(maxWidth);
    titleColumn.setResizable(false);

    final TableColumn levelColumn = treeTable.getColumnModel().getColumn(1);
    //TODO[max]: better preffered size...
    JLabel value = new JLabel("Chop down if long.");
    final Dimension valueSize = value.getPreferredSize();
    levelColumn.setPreferredWidth(valueSize.width);
    levelColumn.setMaxWidth(valueSize.width);
    levelColumn.setMinWidth(valueSize.width);
    levelColumn.setResizable(false);

    treeTable.setPreferredScrollableViewportSize(new Dimension(maxWidth + valueSize.width + 10, 20));

    return treeTable;
  }

  private void expandTree(final JTree tree) {
    int oldRowCount = 0;
    do {
      int rowCount = tree.getRowCount();
      if (rowCount == oldRowCount) break;
      oldRowCount = rowCount;
      for (int i = 0; i < rowCount; i++) {
        tree.expandRow(i);
      }
    }
    while (true);
  }

  protected JPanel createPreviewPanel() {
    JPanel p = new JPanel(new BorderLayout()) {
      public Dimension getPreferredSize() {
        return new Dimension(200, 0);
      }
    };
    p.setBorder(IdeBorderFactory.createTitledBorder("Preview"));
    myEditor = createEditor();
    p.add(myEditor.getComponent(), BorderLayout.CENTER);
    return p;
  }

  private Editor createEditor() {
    EditorFactory editorFactory = EditorFactory.getInstance();
    Document doc = editorFactory.createDocument("");
    EditorEx editor = (EditorEx)editorFactory.createViewer(doc);

    setupEditorSettings(editor);

    EditorColorsScheme scheme = editor.getColorsScheme();
    scheme.setColor(EditorColors.CARET_ROW_COLOR, null);

    editor.setHighlighter(HighlighterFactory.createJavaHighlighter(scheme, LanguageLevel.HIGHEST));
    return editor;
  }

  protected abstract void initTables();

  protected abstract void setupEditorSettings(Editor editor);

  private void updatePreview() {
    if (!toUpdatePreview) {
      return;
    }

    final Project project = ProjectManagerEx.getInstanceEx().getDefaultProject();
    final PsiManager manager = PsiManager.getInstance(project);
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        PsiElementFactory factory = manager.getElementFactory();
        try {
          PsiFile psiFile = factory.createFileFromText("a.java", getPreviewText());
          CodeStyleSettings saved = mySettings;
          mySettings = (CodeStyleSettings)mySettings.clone();
          apply();
          if (getRightMargin() > 0) {
            mySettings.RIGHT_MARGIN = getRightMargin();
          }

          CodeStyleSettingsManager.getInstance(project).setTemporarySettings(mySettings);
          CodeStyleManager.getInstance(project).reformat(psiFile);
          CodeStyleSettingsManager.getInstance(project).dropTemporarySettings();

          myEditor.getSettings().setTabSize(mySettings.getTabSize(StdFileTypes.JAVA));
          mySettings = saved;

          Document document = myEditor.getDocument();
          document.replaceString(0, document.getTextLength(), psiFile.getText());
        }
        catch (IncorrectOperationException e) {
          LOG.error(e);
        }
      }
    });
  }

  protected int getRightMargin() {
    return -1;
  }

  protected abstract String getPreviewText();

  public void reset() {
    toUpdatePreview = false;
    TreeModel treeModel = myTreeTable.getTree().getModel();
    TreeNode root = (TreeNode)treeModel.getRoot();
    resetNode(root);
    toUpdatePreview = true;
    updatePreview();
  }

  private void resetNode(TreeNode node) {
    if (node instanceof MyTreeNode) {
      ((MyTreeNode)node).reset();
    }
    for (int j = 0; j < node.getChildCount(); j++) {
      TreeNode child = node.getChildAt(j);
      resetNode(child);
    }
  }

  public void apply() {
    TreeModel treeModel = myTreeTable.getTree().getModel();
    TreeNode root = (TreeNode)treeModel.getRoot();
    applyNode(root);
  }

  private void applyNode(TreeNode node) {
    if (node instanceof MyTreeNode) {
      ((MyTreeNode)node).apply();
    }
    for (int j = 0; j < node.getChildCount(); j++) {
      TreeNode child = node.getChildAt(j);
      applyNode(child);
    }
  }

  public boolean isModified() {
    TreeModel treeModel = myTreeTable.getTree().getModel();
    TreeNode root = (TreeNode)treeModel.getRoot();
    if (isModified(root)) {
      return true;
    }
    return false;
  }

  private boolean isModified(TreeNode node) {
    if (node instanceof MyTreeNode) {
      if (((MyTreeNode)node).isModified()) return true;
    }
    for (int j = 0; j < node.getChildCount(); j++) {
      TreeNode child = node.getChildAt(j);
      if (isModified(child)) {
        return true;
      }
    }
    return false;
  }

  protected void initBooleanField(String fieldName, String cbName, String groupName) {
    try {
      Class styleSettingsClass = CodeStyleSettings.class;
      Field field = styleSettingsClass.getField(fieldName);
      BooleanOptionKey key = new BooleanOptionKey(groupName, cbName);
      myKeyToFieldMap.put(key, field);
      myKeys.add(key);
    }
    catch (NoSuchFieldException e) {
    }
    catch (SecurityException e) {
    }
  }

  protected void initRadioGroupField(String fieldName, String groupName, String[] rbNames, int[] values) {
    try {
      Class styleSettingsClass = CodeStyleSettings.class;
      Field field = styleSettingsClass.getField(fieldName);
      IntSelectionOptionKey key = new IntSelectionOptionKey(groupName, rbNames, values);
      myKeyToFieldMap.put(key, field);
      myKeys.add(key);
    }
    catch (NoSuchFieldException e) {
    }
    catch (SecurityException e) {
    }
  }

  private static class BooleanOptionKey {
    final String groupName;
    final String cbName;

    public BooleanOptionKey(String groupName, String cbName) {
      this.groupName = groupName;
      this.cbName = cbName;
    }

    public boolean equals(Object obj) {
      if (!(obj instanceof BooleanOptionKey)) return false;
      BooleanOptionKey key = (BooleanOptionKey)obj;
      return groupName.equals(key.groupName) && cbName.equals(key.cbName);
    }

    public int hashCode() {
      return cbName.hashCode();
    }
  }

  private static class IntSelectionOptionKey {
    final String groupName;
    final String[] rbNames;
    final int[] values;

    public IntSelectionOptionKey(String groupName, String[] rbNames, int[] values) {
      this.groupName = groupName;
      this.rbNames = rbNames;
      this.values = values;
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof IntSelectionOptionKey)) return false;

      final IntSelectionOptionKey intSelectionOptionKey = (IntSelectionOptionKey)o;

      if (!groupName.equals(intSelectionOptionKey.groupName)) return false;
      if (!Arrays.equals(rbNames, intSelectionOptionKey.rbNames)) return false;

      return true;
    }

    public int hashCode() {
      return groupName.hashCode() + rbNames[0].hashCode() * 29;
    }
  }

  private Object getSettingsValue(Object key) {
    try {
      if (key instanceof BooleanOptionKey) {
        Field field = (Field)myKeyToFieldMap.get(key);
        return field.getBoolean(mySettings) ? Boolean.TRUE : Boolean.FALSE;
      }
      else if (key instanceof IntSelectionOptionKey) {
        Field field = (Field)myKeyToFieldMap.get(key);
        IntSelectionOptionKey intKey = (IntSelectionOptionKey)key;
        int[] values = intKey.values;
        int value = field.getInt(mySettings);
        for (int i = 0; i < values.length; i++) {
          if (values[i] == value) return intKey.rbNames[i];
        }
      }
    }
    catch (IllegalAccessException e) {
    }

    return null;
  }

  public void setSettingsValue(Object key, Object value) {
    try {
      if (key instanceof BooleanOptionKey) {
        Field field = (Field)myKeyToFieldMap.get(key);
        field.setBoolean(mySettings, ((Boolean)value).booleanValue());
      }
      else if (key instanceof IntSelectionOptionKey) {
        Field field = (Field)myKeyToFieldMap.get(key);
        IntSelectionOptionKey intKey = (IntSelectionOptionKey)key;
        int[] values = intKey.values;
        for (int i = 0; i < values.length; i++) {
          if (intKey.rbNames[i].equals(value)) {
            field.setInt(mySettings, values[i]);
            return;
          }
        }
      }
    }
    catch (IllegalAccessException e) {
    }
  }

  private class MyTreeNode extends DefaultMutableTreeNode {
    private Object myKey;
    private String myText;
    private Object myValue;

    public MyTreeNode(Object key, String text) {
      myKey = key;
      myText = text;
      myValue = getSettingsValue(key);
    }

    public Object getKey() { return myKey; }

    public String getText() { return myText; }

    public Object getValue() { return myValue; }

    public void setValue(Object value) {
      myValue = value;
      updatePreview();
    }

    public void reset() {
      setValue(getSettingsValue(myKey));
    }

    public boolean isModified() {
      return !myValue.equals(getSettingsValue(myKey));
    }

    public void apply() {
      setSettingsValue(myKey, myValue);
    }
  }

  private class MyValueRenderer implements TableCellRenderer {
    private JLabel myComboBox = new JLabel();
    private JCheckBox myCheckBox = new JCheckBox();
    private JPanel myEmptyLabel = new JPanel();

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
      Color background = table.getBackground();
      if (value instanceof Boolean) {
        myCheckBox.setSelected(((Boolean)value).booleanValue());
        myCheckBox.setBackground(background);
        return myCheckBox;
      }
      else if (value instanceof String) {
        /*
        myComboBox.removeAllItems();
        myComboBox.addItem(value);
        */
        myComboBox.setText((String)value);
        myComboBox.setBackground(background);
        return myComboBox;
      }

      myEmptyLabel.setBackground(background);
      return myEmptyLabel;
    }
  }

  private class MyValueEditor extends AbstractTableCellEditor {
    private JComboBox myComboBox = new JComboBox();
    private JCheckBox myCheckBox = new JCheckBox();
    private Component myCurrentEditor = null;
    private MyTreeNode myCurrentNode = null;

    public MyValueEditor() {
      ActionListener synchronizer = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (myCurrentNode != null) {
            myCurrentNode.setValue(getCellEditorValue());
          }
        }
      };
      myComboBox.addActionListener(synchronizer);
      myCheckBox.addActionListener(synchronizer);
    }

    public Object getCellEditorValue() {
      if (myCurrentEditor == myComboBox) {
        return myComboBox.getSelectedItem();
      }
      else if (myCurrentEditor == myCheckBox) {
        return myCheckBox.isSelected() ? Boolean.TRUE : Boolean.FALSE;
      }

      return null;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      final DefaultMutableTreeNode defaultNode = (DefaultMutableTreeNode)((TreeTable)table).getTree().
        getPathForRow(row).getLastPathComponent();
      myCurrentEditor = null;
      myCurrentNode = null;
      if (defaultNode instanceof MyTreeNode) {
        MyTreeNode node = (MyTreeNode)defaultNode;
        if (node.getKey() instanceof BooleanOptionKey) {
          myCurrentEditor = myCheckBox;
          myCheckBox.setSelected(node.getValue() == Boolean.TRUE);
        }
        else {
          myCurrentEditor = myComboBox;
          myComboBox.removeAllItems();
          IntSelectionOptionKey key = (IntSelectionOptionKey)node.getKey();
          String[] values = key.rbNames;
          for (int i = 0; i < values.length; i++) {
            myComboBox.addItem(values[i]);
          }
          myComboBox.setSelectedItem(node.getValue());
        }
        myCurrentNode = node;
      }

      myCurrentEditor.setBackground(table.getBackground());

      return myCurrentEditor;
    }
  }
}
