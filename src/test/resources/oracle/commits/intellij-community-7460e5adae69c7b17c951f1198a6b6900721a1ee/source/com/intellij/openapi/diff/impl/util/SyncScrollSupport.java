package com.intellij.openapi.diff.impl.util;

import com.intellij.openapi.Disposeable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diff.impl.EditingSides;
import com.intellij.openapi.diff.impl.highlighting.FragmentSide;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.util.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

public class SyncScrollSupport implements Disposeable {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.diff.impl.util.SyncScrollSupport");
  private boolean myDuringVerticalScroll = false;
  private final ArrayList<ScrollListener> myScrollers = new ArrayList<ScrollListener>();

  public void install(EditingSides[] sideContainers) {
    dispose();
    Editor[] editors = new Editor[sideContainers.length + 1];
    editors[0] = sideContainers[0].getEditor(FragmentSide.SIDE1);
    for (int i = 0; i < sideContainers.length; i++) {
      EditingSides sideContainer = sideContainers[i];
      LOG.assertTrue(sideContainer.getEditor(FragmentSide.SIDE1) == editors[i]);
      editors[i + 1] = sideContainer.getEditor(FragmentSide.SIDE2);
    }
    if (editors.length == 3) install3(editors, sideContainers);
    else if (editors.length == 2) install2(editors, sideContainers);
    else LOG.error(String.valueOf(editors.length));
  }

  public void dispose() {
    for (Iterator<ScrollListener> iterator = myScrollers.iterator(); iterator.hasNext();) {
      ScrollListener scrollListener = iterator.next();
      scrollListener.dispose();
    }
    myScrollers.clear();
  }

  private void install2(Editor[] editors, EditingSides[] sideContainers) {
    addSlavesScroller(editors[0], new Pair[]{new Pair(FragmentSide.SIDE1, sideContainers[0])});
    addSlavesScroller(editors[1], new Pair[]{new Pair(FragmentSide.SIDE2, sideContainers[0])});
  }

  private void install3(Editor[] editors, EditingSides[] sideContainers) {
    addSlavesScroller(editors[0], new Pair[]{new Pair(FragmentSide.SIDE1, sideContainers[0]),
                                             new Pair(FragmentSide.SIDE1, sideContainers[1])});
    addSlavesScroller(editors[1], new Pair[]{new Pair(FragmentSide.SIDE2, sideContainers[0]),
                                             new Pair(FragmentSide.SIDE1, sideContainers[1])});
    addSlavesScroller(editors[2], new Pair[]{new Pair(FragmentSide.SIDE2, sideContainers[1]),
                                             new Pair(FragmentSide.SIDE2, sideContainers[0])});
  }

  private void addSlavesScroller(Editor editor, Pair[] contexts) {
    ScrollListener scroller = new ScrollListener(contexts, editor);
    scroller.install();
    myScrollers.add(scroller);
  }

  private class ScrollListener implements VisibleAreaListener, Disposeable {
    private final Pair[] myScrollContexts;
    private final Editor myEditor;

    public ScrollListener(Pair[] scrollContexts, Editor editor) {
      myScrollContexts = scrollContexts;
      myEditor = editor;
      install();
    }

    public void install() {
      myEditor.getScrollingModel().addVisibleAreaListener(this);
    }

    public void dispose() {
      myEditor.getScrollingModel().removeVisibleAreaListener(this);
    }

    public void visibleAreaChanged(VisibleAreaEvent e) {
      if (myDuringVerticalScroll) return;
      Rectangle newRectangle = e.getNewRectangle();
      Rectangle oldRectangle = e.getOldRectangle();
      if (newRectangle == null || oldRectangle == null) return;
      myDuringVerticalScroll = true;
      try {
        for (int i = 0; i < myScrollContexts.length; i++) {
          Pair<FragmentSide, EditingSides> context = (Pair<FragmentSide, EditingSides>)myScrollContexts[i];
          syncVerticalScroll(context, newRectangle, oldRectangle);
          syncHorizontalScroll(context, newRectangle, oldRectangle);
        }
      }
      finally { myDuringVerticalScroll = false; }
    }
  }

  private static void syncHorizontalScroll(Pair<FragmentSide,EditingSides> context, Rectangle newRectangle, Rectangle oldRectangle) {
    int newScrollOffset = newRectangle.x;
    if (newScrollOffset == oldRectangle.x) return;
    EditingSides sidesContainer = context.getSecond();
    FragmentSide masterSide = context.getFirst();
    Editor slaveEditor = sidesContainer.getEditor(masterSide.otherSide());
    if (slaveEditor == null) return;

    ScrollingModel scrollingModel = slaveEditor.getScrollingModel();
    scrollingModel.disableAnimation();
    scrollingModel.scrollHorizontally(newScrollOffset);
    scrollingModel.enableAnimation();
  }

  private static void syncVerticalScroll(Pair<FragmentSide,EditingSides> context, Rectangle newRectangle, Rectangle oldRectangle) {
    if (newRectangle.y == oldRectangle.y && newRectangle.height == oldRectangle.height) return;
    EditingSides sidesContainer = context.getSecond();
    FragmentSide masterSide = context.getFirst();

    Editor master = sidesContainer.getEditor(masterSide);
    Editor slave = sidesContainer.getEditor(masterSide.otherSide());

    if (master == null || slave == null) return;

    Rectangle viewRect = master.getScrollingModel().getVisibleArea();
    int middleY = viewRect.height / 3;

    int materVerticalScrollOffset = master.getScrollingModel().getVerticalScrollOffset();
    int slaveVerticalScrollOffset = slave.getScrollingModel().getVerticalScrollOffset();

    LogicalPosition masterPos = master.xyToLogicalPosition(new Point(viewRect.x, materVerticalScrollOffset + middleY));
    int masterCenterLine = masterPos.line;
    int scrollToLine = sidesContainer.getLineBlocks().transform(masterSide, masterCenterLine) + 1;
    int actualLine = scrollToLine - 1;


    slave.getScrollingModel().disableAnimation();

    if (scrollToLine <= 0) {
      int offset = newRectangle.y - oldRectangle.y;
      slave.getScrollingModel().scrollVertically(slaveVerticalScrollOffset + offset);
    }

    int correction = (materVerticalScrollOffset + middleY) % master.getLineHeight();
    int scrollOffset = actualLine * slave.getLineHeight() - middleY;
    slave.getScrollingModel().scrollVertically(scrollOffset + correction);

    slave.getScrollingModel().enableAnimation();
  }

  public static void scrollEditor(Editor editor, int logicalLine) {
    editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(logicalLine, 0));
    ScrollingModel scrollingModel = editor.getScrollingModel();
    scrollingModel.disableAnimation();
    scrollingModel.scrollToCaret(ScrollType.CENTER);
    scrollingModel.enableAnimation();
  }
}
