package com.intellij.debugger.ui.impl.watch;

import com.intellij.debugger.ui.tree.NodeDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/*
 * Copyright (c) 2000-2004 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */

public class DescriptorTree {
  private final HashMap<NodeDescriptor, List<NodeDescriptor>> myChildrenMap = new HashMap<NodeDescriptor, List<NodeDescriptor>>();
  private final List<NodeDescriptor> myRootChildren = new ArrayList<NodeDescriptor>();
  private final boolean myInitial;
  private int myFrameCount = -1;
  private int myFrameIndex = -1;

  public DescriptorTree() {
    this(false);
  }

  public DescriptorTree(boolean isInitial) {
    myInitial = isInitial;
  }

  public void clear() {
    myChildrenMap.clear();
    myRootChildren.clear();
  }

  public boolean frameIdEquals(final int frameCount, final int frameIndex) {
    return myFrameCount == frameCount && myFrameIndex == frameIndex;
  }

  public void setFrameId(final int frameCount, final int frameIndex) {
    myFrameIndex = frameIndex;
    myFrameCount = frameCount;
  }

  public void addChild(NodeDescriptor parent, NodeDescriptor child) {
    List<NodeDescriptor> children;

    if(parent == null) {
      children = myRootChildren;
    }
    else {
      children = myChildrenMap.get(parent);
      if(children == null) {
        children = new ArrayList<NodeDescriptor>();
        myChildrenMap.put(parent, children);
      }
    }
    children.add(child);
    if (myInitial && child instanceof LocalVariableDescriptorImpl) {
      ((LocalVariableDescriptorImpl)child).setNewLocal(false);
    }
  }

  public List<NodeDescriptor> getChildren(NodeDescriptor parent) {
    if(parent == null) {
      return myRootChildren;
    }

    List<NodeDescriptor> children = myChildrenMap.get(parent);
    return children != null ? children : Collections.<NodeDescriptor>emptyList();
  }

  public void dfst(DFSTWalker walker) {
    dfstImpl(null, myRootChildren, walker);
  }

  private void dfstImpl(NodeDescriptor descriptor, List<NodeDescriptor> children, DFSTWalker walker) {
    if(children != null) {
      for (NodeDescriptor child : children) {
        walker.visit(descriptor, child);
        dfstImpl(child, myChildrenMap.get(child), walker);
      }
    }
  }

  public interface DFSTWalker {
    void visit(NodeDescriptor parent, NodeDescriptor child);
  }
}
