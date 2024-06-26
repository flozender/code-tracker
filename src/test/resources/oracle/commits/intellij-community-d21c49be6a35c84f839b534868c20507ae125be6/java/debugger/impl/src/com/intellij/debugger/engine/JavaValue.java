/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.debugger.engine;

import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.actions.JavaValueModifier;
import com.intellij.debugger.actions.JumpToObjectAction;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl;
import com.intellij.debugger.engine.events.DebuggerCommandImpl;
import com.intellij.debugger.engine.events.DebuggerContextCommandImpl;
import com.intellij.debugger.engine.events.SuspendContextCommandImpl;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.impl.watch.*;
import com.intellij.debugger.ui.tree.*;
import com.intellij.debugger.ui.tree.render.ChildrenBuilder;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.debugger.ui.tree.render.NodeRenderer;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.util.PlatformIcons;
import com.intellij.xdebugger.frame.*;
import com.sun.jdi.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
* @author egor
*/
public class JavaValue extends XNamedValue implements NodeDescriptorProvider {
  private final JavaValue myParent;
  private final ValueDescriptorImpl myValueDescriptor;
  private final EvaluationContextImpl myEvaluationContext;
  private final NodeManagerImpl myNodeManager;

  JavaValue(ValueDescriptorImpl valueDescriptor, EvaluationContextImpl evaluationContext, NodeManagerImpl nodeManager) {
    this(null, valueDescriptor, evaluationContext, nodeManager);
  }

  JavaValue(JavaValue parent, ValueDescriptorImpl valueDescriptor, EvaluationContextImpl evaluationContext, NodeManagerImpl nodeManager) {
    super(valueDescriptor.getName());
    myParent = parent;
    myValueDescriptor = valueDescriptor;
    myEvaluationContext = evaluationContext;
    myNodeManager = nodeManager;
  }

  public JavaValue getParent() {
    return myParent;
  }

  @Override
  public ValueDescriptorImpl getDescriptor() {
    return myValueDescriptor;
  }

  public EvaluationContextImpl getEvaluationContext() {
    return myEvaluationContext;
  }

  @Override
  public void computePresentation(@NotNull final XValueNode node, @NotNull XValuePlace place) {
    myEvaluationContext.getDebugProcess().getManagerThread().schedule(new DebuggerContextCommandImpl(getDebuggerContext()) {
      @Override
      public void threadAction() {
        myValueDescriptor.setContext(myEvaluationContext);
        myValueDescriptor.updateRepresentation(myEvaluationContext, new DescriptorLabelListener() {
          @Override
          public void labelChanged() {
            Type type = myValueDescriptor.getType();
            String typeName = type != null ? type.name() : null;

            Icon nodeIcon;
            if (myValueDescriptor instanceof FieldDescriptorImpl && ((FieldDescriptorImpl)myValueDescriptor).isStatic()) {
              nodeIcon = PlatformIcons.FIELD_ICON;
            }
            else if (myValueDescriptor.isArray()) {
              nodeIcon = AllIcons.Debugger.Db_array;
            }
            else if (myValueDescriptor.isPrimitive()) {
              nodeIcon = AllIcons.Debugger.Db_primitive;
            }
            else {
              if (myValueDescriptor instanceof WatchItemDescriptor) {
                nodeIcon = AllIcons.Debugger.Watch;
              }
              else {
                nodeIcon = AllIcons.Debugger.Value;
              }
            }

            node.setPresentation(nodeIcon, typeName, myValueDescriptor.getValueText(), myValueDescriptor.isExpandable());
          }
        });
      }
    });
  }

  @Override
  public void computeChildren(@NotNull final XCompositeNode node) {
    myEvaluationContext.getDebugProcess().getManagerThread().schedule(new SuspendContextCommandImpl(myEvaluationContext.getSuspendContext()) {
      @Override
      public void contextAction() throws Exception {
        final XValueChildrenList children = new XValueChildrenList();
        final NodeRenderer renderer = myValueDescriptor.getRenderer(myEvaluationContext.getDebugProcess());
        renderer.buildChildren(myValueDescriptor.getValue(), new ChildrenBuilder() {
          @Override
          public NodeDescriptorFactory getDescriptorManager() {
            return myNodeManager;
          }

          @Override
          public NodeManager getNodeManager() {
            return myNodeManager;
          }

          @Override
          public ValueDescriptor getParentDescriptor() {
            return myValueDescriptor;
          }

          @Override
          public void setChildren(List<DebuggerTreeNode> nodes) {
            for (DebuggerTreeNode node : nodes) {
              NodeDescriptor descriptor = node.getDescriptor();
              if (descriptor instanceof ValueDescriptorImpl) {
                children.add(new JavaValue(JavaValue.this, (ValueDescriptorImpl)descriptor, myEvaluationContext, myNodeManager));
              }
            }
          }
        }, myEvaluationContext);
        node.addChildren(children, true);
    }});
  }

  @Override
  public void computeSourcePosition(@NotNull XNavigatable navigatable) {
    if (myValueDescriptor instanceof FieldDescriptorImpl) {
      SourcePosition position = ((FieldDescriptorImpl)myValueDescriptor).getSourcePosition(getProject(), getDebuggerContext());
      if (position != null) {
        navigatable.setSourcePosition(DebuggerUtilsEx.toXSourcePosition(position));
      }
    }
    if (myValueDescriptor instanceof LocalVariableDescriptorImpl) {
      SourcePosition position = ((LocalVariableDescriptorImpl)myValueDescriptor).getSourcePosition(getProject(), getDebuggerContext());
      if (position != null) {
        navigatable.setSourcePosition(DebuggerUtilsEx.toXSourcePosition(position));
      }
    }
  }

  private DebuggerContextImpl getDebuggerContext() {
    return DebuggerManagerEx.getInstanceEx(getProject()).getContext();
  }

  public Project getProject() {
    return myEvaluationContext.getProject();
  }

  @Override
  public boolean canNavigateToTypeSource() {
    return true;
  }

  @Override
  public void computeTypeSourcePosition(@NotNull final XNavigatable navigatable) {
    DebugProcessImpl debugProcess = myEvaluationContext.getDebugProcess();
    debugProcess.getManagerThread().schedule(new JumpToObjectAction.NavigateCommand(getDebuggerContext(), myValueDescriptor, debugProcess, null) {
      @Override
      protected void doAction(@Nullable final SourcePosition sourcePosition) {
        if (sourcePosition != null) {
          ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
              navigatable.setSourcePosition(DebuggerUtilsEx.toXSourcePosition(sourcePosition));
            }
          });
        }
      }
    });
  }

  @Nullable
  @Override
  public XValueModifier getModifier() {
    return myValueDescriptor.canSetValue() ? new JavaValueModifier(this) : null;
  }


  private volatile String evaluationExpression = null;
  @Nullable
  @Override
  public String getEvaluationExpression() {
    if (evaluationExpression == null) {
      // TODO: change API to allow to calculate it asynchronously
      DebugProcessImpl debugProcess = myEvaluationContext.getDebugProcess();
      debugProcess.getManagerThread().invokeAndWait(new DebuggerCommandImpl() {
        @Override
        protected void action() throws Exception {
          evaluationExpression = ApplicationManager.getApplication().runReadAction(new Computable<String>() {
            @Override
            public String compute() {
              try {
                return new TextWithImportsImpl(getDescriptor().getTreeEvaluation(JavaValue.this, getDebuggerContext())).getText();
              }
              catch (EvaluateException e) {
                e.printStackTrace();
              }
              return null;
            }
          });
        }
      });
    }
    return evaluationExpression;
  }
}
