package com.intellij.debugger.actions;

import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.engine.events.SuspendContextCommandImpl;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeImpl;
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl;
import com.intellij.debugger.ui.tree.ValueDescriptor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;
import com.sun.jdi.*;

import java.util.List;

/*
 * Copyright (c) 2000-2004 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */

public class JumpToObjectAction extends DebuggerAction{
  private static final Logger LOG = Logger.getInstance("#com.intellij.debugger.actions.JumpToObjectAction");
  public void actionPerformed(AnActionEvent e) {
    DebuggerTreeNodeImpl selectedNode = getSelectedNode(e.getDataContext());
    if(selectedNode == null) {
      return;
    }

    final NodeDescriptorImpl descriptor = selectedNode.getDescriptor();
    if(!(descriptor instanceof ValueDescriptor)) {
      return;
    }

    DebuggerContextImpl debuggerContext = getDebuggerContext(e.getDataContext());
    final DebugProcessImpl debugProcess = debuggerContext.getDebugProcess();
    if(debugProcess == null) {
      return;
    }

    debugProcess.getManagerThread().schedule(new NavigateCommand(debuggerContext, (ValueDescriptor)descriptor, debugProcess, e));
  }

  public void update(final AnActionEvent e) {
    if(!isFirstStart(e)) {
      return;
    }

    final DebuggerContextImpl debuggerContext = getDebuggerContext(e.getDataContext());
    final DebugProcessImpl debugProcess = debuggerContext.getDebugProcess();
    if(debugProcess == null) {
      e.getPresentation().setVisible(false);
      return;
    }

    DebuggerTreeNodeImpl selectedNode = getSelectedNode(e.getDataContext());
    if(selectedNode == null) {
      e.getPresentation().setVisible(false);
      return;
    }

    final NodeDescriptorImpl descriptor = selectedNode.getDescriptor();
    if (descriptor instanceof ValueDescriptor) {
      debugProcess.getManagerThread().schedule(new EnableCommand(debuggerContext, (ValueDescriptor)descriptor, debugProcess, e));
    }
    else {
      e.getPresentation().setVisible(false);
    }
  }

  private SourcePosition calcPosition(final ValueDescriptor descriptor, final DebugProcessImpl debugProcess) throws ClassNotLoadedException {
    final Value value = descriptor.getValue();
    if(value == null) {
      return null;
    }

    Type type = value.type();
    if(type == null) {
      return null;
    }

    try {
      if(type instanceof ArrayType) {
        type = ((ArrayType)type).componentType();
      }
      if(type instanceof ClassType) {
        final List<Location> locations = ((ClassType)type).allLineLocations();
        if(locations.size() > 0) {
          final Location location = locations.get(0);
          return ApplicationManager.getApplication().runReadAction(new Computable<SourcePosition>() {
            public SourcePosition compute() {
              return debugProcess.getPositionManager().getSourcePosition(location);
            }
          });
        }
      }
    }
    catch (ClassNotPreparedException e) {
      LOG.debug(e);
    }
    catch (AbsentInformationException e) {
      LOG.debug(e);
    }
    return null;
  }

  private class NavigateCommand extends SourcePositionCommand {
    public NavigateCommand(final DebuggerContextImpl debuggerContext, final ValueDescriptor descriptor, final DebugProcessImpl debugProcess, final AnActionEvent e) {
      super(debuggerContext, descriptor, debugProcess, e);
    }
    protected NavigateCommand createRetryCommand() {
      return new NavigateCommand(myDebuggerContext, myDescriptor, myDebugProcess, myActionEvent);
    }
    protected void doAction(final SourcePosition sourcePosition) {
      if (sourcePosition != null) {
        sourcePosition.navigate(true);
      }
    }
  }

  private class EnableCommand extends SourcePositionCommand {
    public EnableCommand(final DebuggerContextImpl debuggerContext, final ValueDescriptor descriptor, final DebugProcessImpl debugProcess, final AnActionEvent e) {
      super(debuggerContext, descriptor, debugProcess, e);
    }
    protected EnableCommand createRetryCommand() {
      return new EnableCommand(myDebuggerContext, myDescriptor, myDebugProcess, myActionEvent);
    }
    protected void doAction(final SourcePosition sourcePosition) {
      enableAction(myActionEvent, sourcePosition != null);
    }
  }

  public abstract class SourcePositionCommand extends SuspendContextCommandImpl {
    protected final DebuggerContextImpl myDebuggerContext;
    protected final ValueDescriptor myDescriptor;
    protected final DebugProcessImpl myDebugProcess;
    protected final AnActionEvent myActionEvent;

    public SourcePositionCommand(final DebuggerContextImpl debuggerContext,
                                 final ValueDescriptor descriptor,
                                 final DebugProcessImpl debugProcess,
                                 final AnActionEvent actionEvent) {
      super(debuggerContext.getSuspendContext());
      myDebuggerContext = debuggerContext;
      myDescriptor = descriptor;
      myDebugProcess = debugProcess;
      myActionEvent = actionEvent;
    }

    public final void contextAction() throws Exception {
      try {
        doAction(calcPosition(myDescriptor, myDebugProcess));
      }
      catch (ClassNotLoadedException ex) {
        final String className = ex.className();
        if (loadClass(className) != null) {
          myDebugProcess.getManagerThread().schedule(createRetryCommand());
        }
      }
    }

    protected abstract SourcePositionCommand createRetryCommand();

    protected abstract void doAction(SourcePosition sourcePosition);

    private ReferenceType loadClass(final String className) {
      final EvaluationContextImpl eContext = myDebuggerContext.createEvaluationContext();
      try {
        return myDebugProcess.loadClass(eContext, className, eContext.getClassLoader());
      }
      catch(Throwable ignored) {
      }
      return null;
    }
  }

}
