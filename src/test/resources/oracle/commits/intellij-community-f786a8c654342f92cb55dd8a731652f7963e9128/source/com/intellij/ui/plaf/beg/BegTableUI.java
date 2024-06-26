package com.intellij.ui.plaf.beg;

import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTableUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * @author mike
 */
public class BegTableUI extends BasicTableUI {
  private KeyAdapter myAdapter= new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          if (table.isEditing()) {
            e.consume();
            table.removeEditor();

            if (e.getSource() != table) {
              ((JComponent)e.getSource()).removeKeyListener(this);
            }
          }
        }
      }
    };
  @NonNls public static final String START_EDITING_ACTION_KEY = "startEditing";

  public static ComponentUI createUI(JComponent c) {
    return new BegTableUI();
  }

  public void installUI(JComponent c) {
    super.installUI(c);
    c.getActionMap().put(START_EDITING_ACTION_KEY, new StartEditingAction());
  }

  protected KeyListener createKeyListener() {
    return myAdapter;
  }

  private class StartEditingAction extends AbstractAction {
    public void actionPerformed(ActionEvent e) {
      JTable table = (JTable)e.getSource();
      if (!table.hasFocus()) {
        CellEditor cellEditor = table.getCellEditor();
        if (cellEditor != null && !cellEditor.stopCellEditing()) {
          return;
        }
        table.requestFocus();
        return;
      }
      ListSelectionModel rsm = table.getSelectionModel();
      int anchorRow = rsm.getAnchorSelectionIndex();
      ListSelectionModel csm = table.getColumnModel().getSelectionModel();
      int anchorColumn = csm.getAnchorSelectionIndex();
      table.editCellAt(anchorRow, anchorColumn);
      Component editorComp = table.getEditorComponent();
      if (editorComp != null) {
        editorComp.addKeyListener(myAdapter);
        editorComp.requestFocus();
      }
    }
  }

}
