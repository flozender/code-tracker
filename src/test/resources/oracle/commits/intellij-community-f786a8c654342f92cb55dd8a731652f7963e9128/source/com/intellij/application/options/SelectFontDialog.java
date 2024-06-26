package com.intellij.application.options;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.ui.ListScrollingUtil;
import com.intellij.util.containers.HashMap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class SelectFontDialog extends DialogWrapper {
  private JList myFontList;
  private JCheckBox myShowMonospacedCheckbox;
  private List<String> myFontNames;
  private String myInitialFontName;
  private HashMap myNameToIsMonospaced;

  public SelectFontDialog(Component parent, List<String> fontNames, String initialFontName, HashMap nameToIsMonospaced) {
    super(parent, true);
    myNameToIsMonospaced = nameToIsMonospaced;
    setTitle(ApplicationBundle.message("title.select.font"));
    myFontNames = fontNames;
    myInitialFontName = initialFontName;
    init();
  }

  protected JComponent createCenterPanel() {
    myShowMonospacedCheckbox = new JCheckBox(ApplicationBundle.message("checkbox.show.only.monospaced.fonts"));
    final boolean useOnlyMonospacedFonts = EditorColorsManager.getInstance().isUseOnlyMonospacedFonts();
    myShowMonospacedCheckbox.setSelected(useOnlyMonospacedFonts);
    myFontList = new JList();
    myFontList.setModel(new DefaultListModel());
    fillList(useOnlyMonospacedFonts);

    myFontList.addMouseListener(
      new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          if (e.getClickCount() == 2){
            doOKAction();
          }
        }
      }
    );

    myFontList.setCellRenderer(new MyListCellRenderer());

    myShowMonospacedCheckbox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        boolean onlyMonospaced = myShowMonospacedCheckbox.isSelected();
        EditorColorsManager.getInstance().setUseOnlyMonospacedFonts(onlyMonospaced);
        String selection = (String) myFontList.getSelectedValue();
        fillList(onlyMonospaced);
        if (selection != null) {
          myFontList.setSelectedValue(selection, true);
          myFontList.ensureIndexIsVisible(myFontList.getSelectedIndex());
        }
      }
    });

    JPanel panel = new JPanel(new BorderLayout());

    panel.add(myShowMonospacedCheckbox, BorderLayout.NORTH);
    panel.add(new JScrollPane(myFontList), BorderLayout.CENTER);

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        myShowMonospacedCheckbox.setSelected(useOnlyMonospacedFonts);
      }
    });

    return panel;
  }

  private void fillList(boolean onlyMonospaced) {
    DefaultListModel model = (DefaultListModel) myFontList.getModel();
    model.removeAllElements();
    for (int i = 0; i < myFontNames.size(); i++) {
      String fontName = myFontNames.get(i);
      if (!onlyMonospaced || Boolean.TRUE.equals(myNameToIsMonospaced.get(fontName))) {
        model.addElement(fontName);
      }
    }
  }

  public void show() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        ListScrollingUtil.selectItem(myFontList, myInitialFontName);
      }
    });
    super.show();
  }



  public JComponent getPreferredFocusedComponent() {
    return myFontList;
  }

  public String getFontName() {
    return (String)myFontList.getSelectedValue();
  }

  private static class MyListCellRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(
        JList list,
        Object value,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {
      Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      String fontName = (String) value;
      c.setFont(new Font(fontName, Font.PLAIN, 14));
      return c;
    }
  }
}