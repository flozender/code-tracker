/*
 * Copyright (c) 2000-2004 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.openapi.ui;

import java.awt.*;
import java.io.Serializable;

public class VerticalFlowLayout extends FlowLayout implements Serializable {
  public static final int BOTTOM = 2;
  public static final int MIDDLE = 1;
  public static final int TOP = 0;
  private boolean myVerticalFill;
  private boolean myHorizontalFill;
  private int vGap;
  private int hGap;

  public VerticalFlowLayout() {
    this(TOP, 5, 5, true, false);
  }

  public VerticalFlowLayout(int alignment) {
    this(alignment, 5, 5, true, false);
  }

  public VerticalFlowLayout(boolean fillHorizontally, boolean fillVertically) {
    this(TOP, 5, 5, fillHorizontally, fillVertically);
  }

  public VerticalFlowLayout(int alignment, boolean fillHorizontally, boolean fillVertically) {
    this(alignment, 5, 5, fillHorizontally, fillVertically);
  }

  public VerticalFlowLayout(int alignment, int hGap, int vGap, boolean fillHorizontally, boolean fillVertically) {
    setAlignment(alignment);
    this.hGap = hGap;
    this.vGap = vGap;
    myHorizontalFill = fillHorizontally;
    myVerticalFill = fillVertically;
  }

  public void layoutContainer(Container container) {
    Insets insets = container.getInsets();
    int i = container.getSize().height - (insets.top + insets.bottom + vGap * 2);
    int j = container.getSize().width - (insets.left + insets.right + hGap * 2);
    int k = container.getComponentCount();
    int l = insets.left + hGap;
    int i1 = 0;
    int j1 = 0;
    int k1 = 0;
    for(int l1 = 0; l1 < k; l1++){
      Component component = container.getComponent(l1);
      if (!component.isVisible()) continue;
      Dimension dimension = component.getPreferredSize();
      if (myVerticalFill && l1 == k - 1){
        dimension.height = Math.max(i - i1, component.getPreferredSize().height);
      }
      if (myHorizontalFill){
        component.setSize(j, dimension.height);
        dimension.width = j;
      }
      else{
        component.setSize(dimension.width, dimension.height);
      }
      if (i1 + dimension.height > i){
        a(container, l, insets.top + vGap, j1, i - i1, k1, l1);
        i1 = dimension.height;
        l += hGap + j1;
        j1 = dimension.width;
        k1 = l1;
        continue;
      }
      if (i1 > 0){
        i1 += vGap;
      }
      i1 += dimension.height;
      j1 = Math.max(j1, dimension.width);
    }

    a(container, l, insets.top + vGap, j1, i - i1, k1, k);
  }

  private void a(Container container, int i, int j, int k, int l, int i1, int j1) {
    int k1 = getAlignment();
    if (k1 == 1){
      j += l / 2;
    }
    if (k1 == 2){
      j += l;
    }
    for(int l1 = i1; l1 < j1; l1++){
      Component component = container.getComponent(l1);
      Dimension dimension = component.getSize();
      if (component.isVisible()){
        int i2 = i + (k - dimension.width) / 2;
        component.setLocation(i2, j);
        j += vGap + dimension.height;
      }
    }
  }

  public boolean getHorizontalFill() {
    return myHorizontalFill;
  }

  public void setHorizontalFill(boolean flag) {
    myHorizontalFill = flag;
  }

  public boolean getVerticalFill() {
    return myVerticalFill;
  }

  public void setVerticalFill(boolean flag) {
    myVerticalFill = flag;
  }

  public Dimension minimumLayoutSize(Container container) {
    Dimension dimension = new Dimension(0, 0);
    for(int i = 0; i < container.getComponentCount(); i++){
      Component component = container.getComponent(i);
      if (!component.isVisible()) continue;
      Dimension dimension1 = component.getMinimumSize();
      dimension.width = Math.max(dimension.width, dimension1.width);
      if (i > 0){
        dimension.height += vGap;
      }
      dimension.height += dimension1.height;
    }
    Insets insets = container.getInsets();
    dimension.width += insets.left + insets.right + hGap * 2;
    dimension.height += insets.top + insets.bottom + vGap * 2;
    return dimension;
  }

  public Dimension preferredLayoutSize(Container container) {
    Dimension dimension = new Dimension(0, 0);
    for(int i = 0; i < container.getComponentCount(); i++){
      Component component = container.getComponent(i);
      if (!component.isVisible()) continue;
      Dimension dimension1 = component.getPreferredSize();
      dimension.width = Math.max(dimension.width, dimension1.width);
      if (i > 0){
        dimension.height += hGap;
      }
      dimension.height += dimension1.height;
    }
    Insets insets = container.getInsets();
    dimension.width += insets.left + insets.right + hGap * 2;
    dimension.height += insets.top + insets.bottom + vGap * 2;
    return dimension;
  }
}
