package com.intellij.uiDesigner.propertyInspector.editors.string;

import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SpeedSearchBase;
import com.intellij.uiDesigner.lw.StringDescriptor;
import com.intellij.uiDesigner.UIDesignerBundle;
import com.intellij.util.ui.Table;
import gnu.trove.TObjectIntHashMap;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NonNls;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class KeyChooserDialog extends DialogWrapper{
  private final String myBundleName;
  /** List of bundle's pairs*/
  private final ArrayList<Pair<String, String>> myPairs;
  private final JComponent myCenterPanel;
  /** Table with key/value pairs */
  private final Table myTable;
  @NonNls private static final String NULL = "null";

  /**
   * @param bundle resource bundle to be shown.
   *@param bundleName name of the resource bundle to be shown. We need this
   * name to create StringDescriptor in {@link #getDescriptor()} method.
   *@param keyToPreselect describes row that should be selected in the
   */
  public KeyChooserDialog(
    final Component parent,
    final PropertiesFile bundle,
    final String bundleName,
    final String keyToPreselect
  ) {
    super(parent, true);

    // Check args
    if(bundle == null){
      throw new IllegalArgumentException();
    }
    if(bundleName == null){
      throw new IllegalArgumentException();
    }

    myBundleName = bundleName;

    setTitle(UIDesignerBundle.message("title.chooser.value"));

    // Read key/value pairs from resource bundle
    myPairs = new ArrayList<Pair<String, String>>();

    final List<Property> properties = bundle.getProperties();
    for (Property property : properties) {
      final String key = property.getKey();
      final String value = property.getValue();
      if (key != null) {
        myPairs.add(new Pair<String, String>(key, value != null? value : NULL));
      }
    }
    Collections.sort(myPairs, new MyPairComparator());

    // Create UI
    final MyTableModel model = new MyTableModel();
    myTable = new Table(model);
    myTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    new MySpeedSearch(myTable);
    myCenterPanel = ScrollPaneFactory.createScrollPane(myTable);

    // Calculate width for "Key" columns
    final FontMetrics metrics = myTable.getFontMetrics(myTable.getFont());
    int width = 0;
    for(int i = myPairs.size() - 1; i >= 0; i--){
      final Pair<String, String> pair = myPairs.get(i);
      width = Math.max(width, metrics.stringWidth(pair.getFirst()));
    }
    width += 30;
    width = Math.max(width, metrics.stringWidth(model.getColumnName(0)));
    final TableColumn keyColumn = myTable.getColumnModel().getColumn(0);
    keyColumn.setMaxWidth(width);
    keyColumn.setMinWidth(width);

    // Preselect proper row
    int indexToPreselect = -1;
    for(int i = myPairs.size() - 1; i >= 0; i--){
      final Pair<String, String> pair = myPairs.get(i);
      if(pair.getFirst().equals(keyToPreselect)){
        indexToPreselect = i;
        break;
      }
    }
    if(indexToPreselect != -1){
      selectElementAt(indexToPreselect);
    }

    init();

    myTable.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (!e.isPopupTrigger() && e.getClickCount() == 2) {
          doOKAction();
        }
      }
    });
  }

  private void selectElementAt(final int index) {
    myTable.getSelectionModel().setSelectionInterval(index, index);
    myTable.scrollRectToVisible(myTable.getCellRect(index, 0, true));
  }

  protected String getDimensionServiceKey() {
    return getClass().getName();
  }

  public JComponent getPreferredFocusedComponent() {
    return myTable;
  }

  /**
   * @return resolved string descriptor. If user chose nothing then the
   * method returns <code>null</code>.
   */
  StringDescriptor getDescriptor(){
    final int selectedRow = myTable.getSelectedRow();
    if(selectedRow < 0 || selectedRow >= myTable.getRowCount()){
      return null;
    }
    else{
      final Pair<String, String> pair = myPairs.get(selectedRow);
      final StringDescriptor descriptor = new StringDescriptor(myBundleName, pair.getFirst());
      descriptor.setResolvedValue(pair.getSecond());
      return descriptor;
    }
  }

  protected JComponent createCenterPanel() {
    return myCenterPanel;
  }

  private static final class MyPairComparator implements Comparator<Pair<String, String>>{
    public int compare(final Pair<String, String> p1, final Pair<String, String> p2) {
      return p1.getFirst().compareToIgnoreCase(p2.getFirst());
    }
  }

  private final class MyTableModel extends AbstractTableModel{
    public int getColumnCount() {
      return 2;
    }

    public String getColumnName(final int column) {
      if(column == 0){
        return UIDesignerBundle.message("column.key");
      }
      else if(column == 1){
        return UIDesignerBundle.message("column.value");
      }
      else{
        //noinspection HardCodedStringLiteral
        throw new IllegalArgumentException("unknown column: " + column);
      }
    }

    public Class getColumnClass(final int column) {
      if(column == 0){
        return String.class;
      }
      else if(column == 1){
        return String.class;
      }
      else{
        //noinspection HardCodedStringLiteral
        throw new IllegalArgumentException("unknown column: " + column);
      }
    }

    public Object getValueAt(final int row, final int column) {
      if(column == 0){
        return myPairs.get(row).getFirst();
      }
      else if(column == 1){
        return myPairs.get(row).getSecond();
      }
      else{
        //noinspection HardCodedStringLiteral
        throw new IllegalArgumentException("unknown column: " + column);
      }
    }

    public int getRowCount() {
      return myPairs.size();
    }
  }

  private class MySpeedSearch extends SpeedSearchBase<Table> {
    private TObjectIntHashMap myElements;
    private Object[] myElementsArray;

    public MySpeedSearch(final Table component) {
      super(component);
    }

    public int getSelectedIndex() {
      return myComponent.getSelectedRow();
    }

    public Object[] getAllElements() {
      if (myElements == null) {
        myElements = new TObjectIntHashMap();
        myElementsArray = myPairs.toArray();
        for (int idx = 0; idx < myElementsArray.length; idx++) {
          Object element = myElementsArray[idx];
          myElements.put(element, idx);
        }
      }
      return myElementsArray;
    }

    public String getElementText(final Object element) {
      return ((Pair<String, String>)element).getFirst();
    }

    public void selectElement(final Object element, final String selectedText) {
      final int index = myElements.get(element);
      selectElementAt(index);
    }
  }
}
