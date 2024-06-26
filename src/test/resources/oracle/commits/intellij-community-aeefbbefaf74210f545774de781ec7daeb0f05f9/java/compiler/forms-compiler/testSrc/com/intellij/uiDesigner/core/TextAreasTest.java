package com.intellij.uiDesigner.core;

import junit.framework.TestCase;

import javax.swing.*;
import java.awt.*;

public final class TextAreasTest extends TestCase{
  /**
   * label   |    label
   * text area (span 2)
   */ 
  
  public void test1() {
    final JPanel panel = new JPanel(new GridLayoutManager(2,2, new Insets(0,0,0,0), 0, 0));

    final JLabel label1 = new JLabel();
    label1.setPreferredSize(new Dimension(15,20));
    final JLabel label2 = new JLabel();
    label2.setPreferredSize(new Dimension(15,20));
    final JTextArea textArea = new JTextArea();
    textArea.setLineWrap(true);

    panel.add(label1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    panel.add(label2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    panel.add(textArea, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0));

    assertEquals(100, textArea.getPreferredSize().width);

    final Dimension initialPreferredSize = panel.getPreferredSize();
    assertEquals(new Dimension(100,20 + textArea.getPreferredSize().height), initialPreferredSize);

    panel.setSize(initialPreferredSize);
    panel.invalidate();
    panel.doLayout();

    assertEquals(initialPreferredSize, panel.getPreferredSize());
  }

  /**
   * textfield1 | textfield2
   *  textfield3 (span 2)
   *
   * important: hspan should be greater than 0
   */
  public void test2() {
    final JPanel panel = new JPanel(new GridLayoutManager(2,2, new Insets(0,0,0,0), 11, 0));

    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(15,20));
    final JTextField field2 = new JTextField();
    field2.setPreferredSize(new Dimension(15,20));
    final JTextField field3 = new JTextField();
    field3.setPreferredSize(new Dimension(100,20));

    panel.add(field1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    panel.add(field2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    panel.add(field3, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    assertEquals(100, panel.getPreferredSize().width);
  }
  
}
