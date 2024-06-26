package com.intellij.ui.plaf.beg;

import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalScrollBarUI;
import javax.swing.plaf.metal.MetalScrollPaneUI;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Eugene Belyaev
 */
public class BegScrollPaneUI extends MetalScrollPaneUI {
  @NonNls public static final String VERTICAL_SCROLL_BAR_PROPERTY = "verticalScrollBar";
  @NonNls public static final String HORIZONTAL_SCROLL_BAR_PROPERTY = "horizontalScrollBar";
  @NonNls public static final String BORDER_PROPERTY = "border";

  public static ComponentUI createUI(JComponent x) {
    return new BegScrollPaneUI();
  }

  /**
   * If the border of the scrollpane is an instance of
   * <code>MetalBorders.ScrollPaneBorder</code>, the client property
   * <code>FREE_STANDING_PROP</code> of the scrollbars
   * is set to false, otherwise it is set to true.
   */
  private void updateScrollbarsFreeStanding() {
    if (scrollpane == null) {
      return;
    }
    Object value = Boolean.FALSE;
    scrollpane.getHorizontalScrollBar().putClientProperty
        (MetalScrollBarUI.FREE_STANDING_PROP, value);
    scrollpane.getVerticalScrollBar().putClientProperty
        (MetalScrollBarUI.FREE_STANDING_PROP, value);
  }

  protected PropertyChangeListener createScrollBarSwapListener() {
    return new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        String propertyName = e.getPropertyName();
        if (propertyName.equals(VERTICAL_SCROLL_BAR_PROPERTY) ||
            propertyName.equals(HORIZONTAL_SCROLL_BAR_PROPERTY)) {
          ((JScrollBar) e.getOldValue()).putClientProperty(MetalScrollBarUI.FREE_STANDING_PROP,
              null);
          ((JScrollBar) e.getNewValue()).putClientProperty(MetalScrollBarUI.FREE_STANDING_PROP,
              Boolean.FALSE);
        }
        else if (BORDER_PROPERTY.equals(propertyName)) {
          updateScrollbarsFreeStanding();
        }
      }
    };
  }
}
