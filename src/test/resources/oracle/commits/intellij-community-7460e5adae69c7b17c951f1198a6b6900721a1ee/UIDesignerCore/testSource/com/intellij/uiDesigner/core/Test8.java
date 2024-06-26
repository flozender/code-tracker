package com.intellij.uiDesigner.core;

import junit.framework.TestCase;

import javax.swing.*;
import java.awt.*;

public final class Test8 extends TestCase{
  /**
   * label 1 (pref=100, min=10, can shrink, can grow) | label 2 (pref=100, min=10, can shrink)
   *                                            (can grow, want grow)
   */
  public void test1() {
    final GridLayoutManager layoutManager = new GridLayoutManager(2,2, new Insets(0,0,0,0), 0, 0);
    final JPanel panel = new JPanel(layoutManager);

    final JLabel label1 = new JLabel();
    label1.setMinimumSize(new Dimension(10,10));
    label1.setPreferredSize(new Dimension(100,10));

    final JLabel label2 = new JLabel();
    label2.setMinimumSize(new Dimension(10,10));
    label2.setPreferredSize(new Dimension(100,10));

    panel.add(label1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_SHRINK + GridConstraints.SIZEPOLICY_CAN_GROW,
      GridConstraints.SIZEPOLICY_FIXED, null, null, null));

    panel.add(label2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_SHRINK,
      GridConstraints.SIZEPOLICY_FIXED, null, null, null));

    panel.add(new JLabel(), new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_SHRINK + GridConstraints.SIZEPOLICY_CAN_GROW + GridConstraints.SIZEPOLICY_WANT_GROW,
      GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));

    assertEquals(20, panel.getMinimumSize().width);
    assertEquals(200, panel.getPreferredSize().width);

    // minimum
    panel.setSize(20, 100);
    panel.doLayout();
    assertEquals(10, label1.getWidth());
    assertEquals(10, label2.getWidth());

    // between min and pref
    panel.setSize(76, 100);
    panel.doLayout();
    assertEquals(38, label1.getWidth());
    assertEquals(38, label2.getWidth());

    // pref-1
    panel.setSize(199, 100);
    panel.doLayout();
    assertEquals(100, label1.getWidth());
    assertEquals(99, label2.getWidth());

    // pref
    panel.setSize(200, 100);
    panel.doLayout();
    assertEquals(100, label1.getWidth());
    assertEquals(100, label2.getWidth());

    // pref+1
    panel.setSize(201, 100);
    panel.doLayout();
    assertEquals(101, label1.getWidth());
    assertEquals(100, label2.getWidth());

    // pref + few
    panel.setSize(205, 100);
    panel.doLayout();
    assertEquals(105, label1.getWidth());
    assertEquals(100, label2.getWidth());
  }

}
