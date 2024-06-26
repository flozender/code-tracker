package com.intellij.uiDesigner.core;

import junit.framework.TestCase;

import javax.swing.*;
import java.awt.*;

public final class TestSpans extends TestCase{
  
  /**
   * button(can grow) | text field (want grow)
   *   text field (want grow, span 2) 
   */ 
  public void test1() {
    final GridLayoutManager layout = new GridLayoutManager(2,2, new Insets(0,0,0,0), 0, 0);
    final JPanel panel = new JPanel(layout);

    final JButton button = new JButton();
    button.setPreferredSize(new Dimension(50, 10));
    
    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(50, 10));

    final JTextField field2 = new JTextField();

    panel.add(button, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));

    panel.add(field1, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));

    panel.add(field2, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(100, preferredSize.width);
    
    panel.setSize(new Dimension(500, panel.getHeight()));
    panel.doLayout();

    assertEquals(500, field2.getWidth());
    assertEquals(50, button.getWidth());
    assertEquals(450, field1.getWidth());
  }
  
  
  /**
   * button(can grow) | text field (can grow)
   *   text field (want grow, span 2) 
   */ 
  public void test2() {
    final JPanel panel = new JPanel(new GridLayoutManager(2,2, new Insets(0,0,0,0), 0, 0));

    final JButton button = new JButton();
    button.setPreferredSize(new Dimension(50, 10));
    
    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(50, 10));

    final JTextField field2 = new JTextField();

    panel.add(button, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));

    panel.add(field1, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));

    panel.add(field2, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(100, preferredSize.width);
    
    panel.setSize(new Dimension(500, panel.getHeight()));
    panel.doLayout();

    assertEquals(500, field2.getWidth());
    assertEquals(250, button.getWidth());
    assertEquals(250, field1.getWidth());
  }
  
  /**
   * button(can grow) | text field (want grow, span 2)
   */ 
  public void test3() {
    final JPanel panel = new JPanel(new GridLayoutManager(1,3, new Insets(0,0,0,0), 0, 0));

    final JButton button = new JButton();
    button.setPreferredSize(new Dimension(50, 10));
    
    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(110, 10));

    panel.add(button, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));

    panel.add(field1, new GridConstraints(0,1,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(160, preferredSize.width);
    
    panel.setSize(new Dimension(500, panel.getHeight()));
    panel.doLayout();

    assertEquals(50, button.getWidth());
    assertEquals(450, field1.getWidth());
  }
  
  /**
   * button (can grow, span 2 )       | text field 1 (span 1)
   * text field 2 (want grow, span 2) | empty
   */ 
  public void test4() {
    final GridLayoutManager layoutManager = new GridLayoutManager(2,3, new Insets(0,0,0,0), 0, 0);
    final JPanel panel = new JPanel(layoutManager);

    final JButton button = new JButton();
    button.setPreferredSize(new Dimension(50, 10));
    
    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(110, 10));

    final JTextField field2 = new JTextField();
    field2.setPreferredSize(new Dimension(110, 10));

    panel.add(button, new GridConstraints(0,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));

    panel.add(field1, new GridConstraints(0,2,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));

    panel.add(field2, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));

    final Dimension preferredSize = panel.getPreferredSize();

    // field will be not null after getPreferredSize()
    final DimensionInfo horizontalInfo = layoutManager.myHorizontalInfo;
    assertEquals(GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_GROW, horizontalInfo.getCellSizePolicy(0));
    assertEquals(GridConstraints.SIZEPOLICY_CAN_SHRINK, horizontalInfo.getCellSizePolicy(1));
    assertEquals(GridConstraints.SIZEPOLICY_WANT_GROW, horizontalInfo.getCellSizePolicy(2));

    assertEquals(220, preferredSize.width);
    
    panel.setSize(new Dimension(500, panel.getHeight()));
    panel.doLayout();

    assertEquals(250, button.getWidth());
    assertEquals(250, field1.getWidth());
    assertEquals(250, field2.getWidth());
  }
  
}
