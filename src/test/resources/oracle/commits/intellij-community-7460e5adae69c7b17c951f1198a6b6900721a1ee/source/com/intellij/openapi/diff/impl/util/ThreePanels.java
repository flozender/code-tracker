package com.intellij.openapi.diff.impl.util;

import javax.swing.*;

public class ThreePanels extends JPanel {
  private final JComponent[] myDividers;
  private final JComponent[] myPanels;

  public ThreePanels(JComponent[] panels, JComponent[] dividers) {
    myDividers = dividers;
    myPanels = panels;
    addAll(dividers);
    addAll(panels);
  }

  private void addAll(JComponent[] components) {
    for (int i = 0; i < components.length; i++) {
      JComponent component = components[i];
      add(component, -1);
    }
  }

  public void doLayout() {
    int width = getWidth();
    int height = getHeight();
    int dividersTotalWidth = 0;
    for (int i = 0; i < myDividers.length; i++) {
      JComponent divider = myDividers[i];
      dividersTotalWidth += divider.getPreferredSize().width;
    }
    int panelWidth = (width - dividersTotalWidth) / 3;
    int x = 0;
    for (int i = 0; i < myPanels.length; i++) {
      JComponent panel = myPanels[i];
      panel.setBounds(x, 0, panelWidth, height);
      panel.validate();
      x += panelWidth;
      if (i < myDividers.length) {
        JComponent divider = myDividers[i];
        int dividerWidth = divider.getPreferredSize().width;
        divider.setBounds(x, 0, dividerWidth, height);
        divider.validate();
        x += dividerWidth;
      }
    }
  }
}
