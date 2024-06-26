package com.intellij.refactoring.changeClassSignature;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.help.HelpManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.ui.*;
import com.intellij.refactoring.util.RefactoringMessageUtil;
import com.intellij.ui.*;
import com.intellij.usageView.UsageViewUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.Table;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dsl
 */
public class ChangeClassSignatureDialog extends BaseRefactoringDialog {
  private static final Logger LOG = Logger.getInstance("#com.intellij.refactoring.changeClassSignature.ChangeClassSignatureDialog");
  private static final int NAME_COLUMN = 0;
  private static final int VALUE_COLUMN = 1;

  private final List<TypeParameterInfo> myTypeParameterInfos;
  private final List<PsiTypeCodeFragment> myTypeCodeFragments;
  private final PsiClass myClass;
  private final PsiTypeParameter[] myOriginalParameters;
  private final PsiManager myManager;
  private final MyTableModel myTableModel;
  private Table myTable;

  public ChangeClassSignatureDialog(PsiClass aClass) {
    super(aClass.getProject(), true);
    setTitle("Change Class Signature");
    myClass = aClass;
    myManager = myClass.getManager();
    myTypeParameterInfos = new ArrayList<TypeParameterInfo>();
    myTypeCodeFragments = new ArrayList<PsiTypeCodeFragment>();
    myOriginalParameters = myClass.getTypeParameters();
    for (int i = 0; i < myOriginalParameters.length; i++) {
      myTypeParameterInfos.add(new TypeParameterInfo(i));
      myTypeCodeFragments.add(null);
    }
    myTableModel = new MyTableModel();
    init();
  }

  private PsiTypeCodeFragment createValueCodeFragment() {
    return myManager.getElementFactory().createTypeCodeFragment(
      "",
      myClass.getLBrace(),
      false, true, false
    );
  }

  protected JComponent createNorthPanel() {
    Box box = Box.createHorizontalBox();
    JLabel label = new JLabel("Change signature of " + UsageViewUtil.getDescriptiveName(myClass));
    box.add(label);
    box.add(Box.createHorizontalGlue());
    return box;
  }

  protected void doHelpAction() {
    HelpManager.getInstance().invokeHelp(HelpID.CHANGE_CLASS_SIGNATURE);
  }

  protected JComponent createCenterPanel() {
    myTable = new Table(myTableModel);
    TableColumn nameColumn = myTable.getColumnModel().getColumn(NAME_COLUMN);
    TableColumn valueColumn = myTable.getColumnModel().getColumn(VALUE_COLUMN);
    Project project = myClass.getProject();
    nameColumn.setCellRenderer(new MyCellRenderer());
    nameColumn.setCellEditor(new StringTableCellEditor(project));
    valueColumn.setCellRenderer(new MyCodeFragmentTableCellRenderer());
    valueColumn.setCellEditor(new CodeFragmentTableCellEditor(project));
    EditableRowTableManager manager = new EditableRowTableManager(myTable, myTableModel, true);

    myTable.setPreferredScrollableViewportSize(new Dimension(210, myTable.getRowHeight() * 4));
    myTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myTable.getSelectionModel().setSelectionInterval(0, 0);
    myTable.setSurrendersFocusOnKeystroke(true);
    myTable.setFocusCycleRoot(true);


    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(IdeBorderFactory.createTitledBorder("Parameters"));
    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTable);

    panel.add(scrollPane, BorderLayout.CENTER);
    panel.add(manager.getButtonsPanel(), BorderLayout.EAST);
    return panel;
  }

  protected void doAction() {
    TableUtil.stopEditing(myTable);
    String message = validateAndCommitData();
    if (message != null) {
      RefactoringMessageUtil.showErrorMessage("Incorrect Data", message, HelpID.CHANGE_SIGNATURE, myClass.getProject());
      return;
    }
    ChangeClassSignatureProcessor processor =
      new ChangeClassSignatureProcessor(myClass.getProject(), myClass,
        myTypeParameterInfos.toArray(new TypeParameterInfo[myTypeParameterInfos.size()]));
    invokeRefactoring(processor);
  }

  private String validateAndCommitData() {
    for (int i = 0; i < myTypeParameterInfos.size(); i++) {
      final TypeParameterInfo info = myTypeParameterInfos.get(i);
      if (!info.isForExistingParameter() && !myClass.getManager().getNameHelper().isIdentifier(info.getNewName())) {
        return "Wrong name: " + info.getNewName();
      }
    }
    LOG.assertTrue(myTypeCodeFragments.size() == myTypeParameterInfos.size());
    for (int i = 0; i < myTypeCodeFragments.size(); i++) {
      final PsiTypeCodeFragment codeFragment = myTypeCodeFragments.get(i);
      TypeParameterInfo info = myTypeParameterInfos.get(i);
      if (info.getOldParameterIndex() >= 0) continue;
      PsiType type = null;
      try {
        type = codeFragment.getType();
      }
      catch (PsiTypeCodeFragment.TypeSyntaxException e) {
        return "Wrong default value: '" + codeFragment.getText() + "' for parameter " + info.getNewName();
      }
      catch (PsiTypeCodeFragment.NoTypeException e) {
        return "Specify a type for parameter" + info.getNewName();
      }
      info.setDefaultValue(type);
    }
    return null;
  }

  private class MyTableModel extends AbstractTableModel implements RowEditableTableModel {
    public int getColumnCount() {
      return 2;
    }

    public int getRowCount() {
      return myTypeParameterInfos.size();
    }

    public Class getColumnClass(int columnIndex) {
      switch(columnIndex) {
        case NAME_COLUMN:
          return String.class;

        default:
          return null;
      }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      switch(columnIndex) {
        case NAME_COLUMN:
          TypeParameterInfo info = myTypeParameterInfos.get(rowIndex);
          if (info.isForExistingParameter()) {
            return myOriginalParameters[info.getOldParameterIndex()].getName();
          }
          else {
            return info.getNewName();
          }
        case VALUE_COLUMN:
          return myTypeCodeFragments.get(rowIndex);
      }
      LOG.assertTrue(false);
      return null;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return !myTypeParameterInfos.get(rowIndex).isForExistingParameter();
    }

    public String getColumnName(int column) {
      switch(column) {
        case NAME_COLUMN:
          return "Name";
        case VALUE_COLUMN:
          return "Default Value";
        default:
          LOG.assertTrue(false);
          return null;
      }
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      switch(columnIndex) {
        case NAME_COLUMN:
          myTypeParameterInfos.get(rowIndex).setNewName((String) aValue);
          break;
        case VALUE_COLUMN:
          break;
        default:
          LOG.assertTrue(false);
      }
    }

    public void addRow() {
      TableUtil.stopEditing(myTable);
      myTypeParameterInfos.add(new TypeParameterInfo("", null));
      myTypeCodeFragments.add(createValueCodeFragment());
      int index = myTypeParameterInfos.size() - 1;
      fireTableDataChanged();
      //fireTableRowsInserted(index, index);
    }

    public void removeRow(int index) {
      myTypeParameterInfos.remove(index);
      myTypeCodeFragments.remove(index);
      fireTableDataChanged();
      //fireTableRowsDeleted(index, index);
    }

    public void exchangeRows(int index1, int index2) {
      ContainerUtil.swapElements(myTypeParameterInfos, index1, index2);
      ContainerUtil.swapElements(myTypeCodeFragments, index1, index2);
      fireTableDataChanged();
      //fireTableRowsUpdated(Math.min(index1, index2), Math.max(index1, index2));
    }
  }

  private class MyCellRenderer extends ColoredTableCellRenderer {

    public void customizeCellRenderer(JTable table, Object value,
                                      boolean isSelected, boolean hasFocus, int row, int column) {
      if (!myTableModel.isCellEditable(row, column)) {
        setBackground(getBackground().darker());
      }
      append((String)value, new SimpleTextAttributes(Font.PLAIN, null));
    }
  }

  private class MyCodeFragmentTableCellRenderer extends CodeFragmentTableCellRenderer {

    public MyCodeFragmentTableCellRenderer() {
      super(myProject);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      final Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (!myTableModel.isCellEditable(row, column)) {
        component.setBackground(component.getBackground().darker());
      }

      return component;
    }
  }
}
