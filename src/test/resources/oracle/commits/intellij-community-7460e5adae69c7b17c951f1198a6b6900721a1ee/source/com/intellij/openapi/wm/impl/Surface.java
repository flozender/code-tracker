package com.intellij.openapi.wm.impl;

import com.intellij.openapi.wm.ToolWindowAnchor;

import javax.swing.*;
import java.awt.*;

/**
 * @author Eugene Belyaev
 */
final class Surface extends JComponent {
  private final Image myTopImage;
  private final Image myBottomImage;
  private final double myPixelsPerSec;
  private final int myDirection;
  private final ToolWindowAnchor myAnchor;
  private int myOffset = 0;

  public Surface(final Image topImage, final Image bottomImage, final int direction, final ToolWindowAnchor anchor, final double pixelsPerSec) {
    myTopImage = topImage;
    myBottomImage = bottomImage;
    myAnchor = anchor;
    myDirection = direction;
    myPixelsPerSec = pixelsPerSec;
    setOpaque(true);
  }

  public final void runMovement() {
    if(!isShowing()){
      return;
    }
    final int distance;
    final Rectangle bounds = getBounds();
    if (myAnchor == ToolWindowAnchor.LEFT || myAnchor == ToolWindowAnchor.RIGHT) {
      distance = bounds.width;
    }
    else {
      distance = bounds.height;
    }
    final double desiredTime = distance / myPixelsPerSec * 1000;
    final long startTime = System.currentTimeMillis();
    int count = 0;
    myOffset = 0;


    while(true){
      paintImmediately(0,0,getWidth(),getHeight());
      final long timeSpent = System.currentTimeMillis() - startTime;
      count++;
      if (timeSpent >= desiredTime) break;
      final double onePaintTime = (double)timeSpent / count;
      int iterations = (int)((desiredTime - timeSpent) / onePaintTime);
      iterations = Math.max(1, iterations);
      myOffset += (distance - myOffset) / iterations;
    }
  }

  public final void paint(final Graphics g) {
    final Rectangle bounds = getBounds();
    if (myAnchor == ToolWindowAnchor.LEFT) {
      if (myDirection == 1) {
        g.setClip(null);
        g.clipRect(myOffset, 0, bounds.width - myOffset, bounds.height);
        g.drawImage(myBottomImage, 0, 0, null);
        g.setClip(null);
        g.clipRect(0, 0, myOffset, bounds.height);
        g.drawImage(myTopImage, myOffset - bounds.width, 0, null);
      }
      else {
        g.setClip(null);
        g.clipRect(bounds.width - myOffset, 0, myOffset, bounds.height);
        g.drawImage(myBottomImage, 0, 0, null);
        g.setClip(null);
        g.clipRect(0, 0, bounds.width - myOffset, bounds.height);
        g.drawImage(myTopImage, -myOffset, 0, null);
      }
      myTopImage.flush();
    }
    else if (myAnchor == ToolWindowAnchor.RIGHT) {
      if (myDirection == 1) {
        g.setClip(null);
        g.clipRect(0, 0, bounds.width - myOffset, bounds.height);
        g.drawImage(myBottomImage, 0, 0, null);
        g.setClip(null);
        g.clipRect(bounds.width - myOffset, 0, myOffset, bounds.height);
        g.drawImage(myTopImage, bounds.width - myOffset, 0, null);
      }
      else {
        g.setClip(null);
        g.clipRect(0, 0, myOffset, bounds.height);
        g.drawImage(myBottomImage, 0, 0, null);
        g.setClip(null);
        g.clipRect(myOffset, 0, bounds.width - myOffset, bounds.height);
        g.drawImage(myTopImage, myOffset, 0, null);
      }
    }
    else if (myAnchor == ToolWindowAnchor.TOP) {
      if (myDirection == 1) {
        g.setClip(null);
        g.clipRect(0, myOffset, bounds.width, bounds.height - myOffset);
        g.drawImage(myBottomImage, 0, 0, null);
        g.setClip(null);
        g.clipRect(0, 0, bounds.width, myOffset);
        g.drawImage(myTopImage, 0, -bounds.height + myOffset, null);
      }
      else {
        g.setClip(null);
        g.clipRect(0, bounds.height - myOffset, bounds.width, myOffset);
        g.drawImage(myBottomImage, 0, 0, null);
        g.setClip(null);
        g.clipRect(0, 0, bounds.width, bounds.height - myOffset);
        g.drawImage(myTopImage, 0, -myOffset, null);
      }
    }
    else if (myAnchor == ToolWindowAnchor.BOTTOM) {
      if (myDirection == 1) {
        g.setClip(null);
        g.clipRect(0, 0, bounds.width, bounds.height - myOffset);
        g.drawImage(myBottomImage, 0, 0, null);
        g.setClip(null);
        g.clipRect(0, bounds.height - myOffset, bounds.width, myOffset);
        g.drawImage(myTopImage, 0, bounds.height - myOffset, null);
      }
      else {
        g.setClip(null);
        g.clipRect(0, 0, bounds.width, myOffset);
        g.drawImage(myBottomImage, 0, 0, null);
        g.setClip(null);
        g.clipRect(0, myOffset, bounds.width, bounds.height - myOffset);
        g.drawImage(myTopImage, 0, myOffset, null);
      }
    }
  }
}
