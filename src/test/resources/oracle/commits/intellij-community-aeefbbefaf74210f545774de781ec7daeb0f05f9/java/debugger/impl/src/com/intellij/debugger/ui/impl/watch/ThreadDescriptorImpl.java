package com.intellij.debugger.ui.impl.watch;

import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.engine.DebuggerManagerThreadImpl;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.SuspendManager;
import com.intellij.debugger.engine.SuspendManagerUtil;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.jdi.ThreadGroupReferenceProxyImpl;
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl;
import com.intellij.debugger.ui.tree.ThreadDescriptor;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.openapi.util.IconLoader;
import com.intellij.xdebugger.ui.DebuggerIcons;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ThreadReference;

import javax.swing.*;

public class ThreadDescriptorImpl extends NodeDescriptorImpl implements ThreadDescriptor{
  private final ThreadReferenceProxyImpl myThread;
  private String myName = null;
  private boolean myIsExpandable   = true;
  private boolean myIsSuspended    = false;
  private boolean myIsCurrent;
  private boolean myIsFrozen;

  private boolean            myIsAtBreakpoint;
  private SuspendContextImpl mySuspendContext;

  private static final Icon myRunningThreadIcon = IconLoader.getIcon("/debugger/threadRunning.png");
  private static final Icon myCurrentThreadIcon = IconLoader.getIcon("/debugger/threadCurrent.png");
  private static final Icon myThreadAtBreakpointIcon = IconLoader.getIcon("/debugger/threadAtBreakpoint.png");
  private static final Icon myFrozenThreadIcon = IconLoader.getIcon("/debugger/threadFrozen.png");

  public ThreadDescriptorImpl(ThreadReferenceProxyImpl thread) {
    myThread = thread;
  }

  public String getName() {
    return myName;
  }

  protected String calcRepresentation(EvaluationContextImpl context, DescriptorLabelListener labelListener) throws EvaluateException {
    DebuggerManagerThreadImpl.assertIsManagerThread();
    ThreadReferenceProxyImpl thread = getThreadReference();
    try {
      myName = thread.name();
      ThreadGroupReferenceProxyImpl gr = getThreadReference().threadGroupProxy();
      final String grname = (gr != null)? gr.name() : null;
      final String threadStatusText = DebuggerUtilsEx.getThreadStatusText(getThreadReference().status());
      //noinspection HardCodedStringLiteral
      if (grname != null && !"SYSTEM".equalsIgnoreCase(grname)) {
        return DebuggerBundle.message("label.thread.node.in.group", myName, thread.uniqueID(), threadStatusText, grname);
      }

      return DebuggerBundle.message("label.thread.node", myName, thread.uniqueID(), threadStatusText);
    }
    catch (ObjectCollectedException e) {
      return myName != null ? DebuggerBundle.message("label.thread.node.thread.collected", myName) : "";
    }
  }

  public ThreadReferenceProxyImpl getThreadReference() {
    return myThread;
  }

  public boolean isCurrent() {
    return myIsCurrent;
  }

  public boolean isFrozen() {
    return myIsFrozen;
  }

  public boolean isExpandable() {
    return myIsExpandable;
  }

  public void setContext(EvaluationContextImpl context) {
    final ThreadReferenceProxyImpl thread = getThreadReference();
    final SuspendManager suspendManager = context.getDebugProcess().getSuspendManager();
    final SuspendContextImpl suspendContext = context.getSuspendContext();

    try {
      myIsSuspended = suspendManager.isSuspended(thread);
    }
    catch (ObjectCollectedException e) {
      myIsSuspended = false;
    }
    myIsExpandable   = calcExpandable(myIsSuspended);
    mySuspendContext = SuspendManagerUtil.getSuspendContextForThread(suspendContext, thread);
    myIsAtBreakpoint = SuspendManagerUtil.findContextByThread(suspendManager, thread) != null;
    myIsCurrent      = suspendContext.getThread() == thread;
    myIsFrozen       = suspendManager.isFrozen(thread);
  }

  private boolean calcExpandable(final boolean isSuspended) {
    if (!isSuspended) {
      return false;
    }
    final int status = getThreadReference().status();
    if (status == ThreadReference.THREAD_STATUS_UNKNOWN ||
        status == ThreadReference.THREAD_STATUS_NOT_STARTED ||
        status == ThreadReference.THREAD_STATUS_ZOMBIE) {
      return false;
    }
    return true;
    /*
    // [jeka] with lots of threads calling threadProxy.frameCount() in advance while setting context can be costly....
    // see IDEADEV-2020
    try {
      return threadProxy.frameCount() > 0;
    }
    catch (EvaluateException e) {
      //LOG.assertTrue(false);
      // if we pause during evaluation of this method the exception is thrown
      //  private static void longMethod(){
      //    try {
      //      Thread.sleep(100000);
      //    } catch (InterruptedException e) {
      //      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      //    }
      //  }
      return false;
    }
    */
  }

  public SuspendContextImpl getSuspendContext() {
    return mySuspendContext;
  }

  public boolean isAtBreakpoint() {
    return myIsAtBreakpoint;
  }

  public boolean isSuspended() {
    return myIsSuspended;
  }

  public Icon getIcon() {
    if(isCurrent()) {
      return myCurrentThreadIcon;
    }
    if(isFrozen()) {
      return myFrozenThreadIcon;
    }
    if(isAtBreakpoint()) {
      return myThreadAtBreakpointIcon;
    }
    if(isSuspended()) {
      return DebuggerIcons.SUSPENDED_THREAD_ICON;
    }
    return myRunningThreadIcon;
  }
}