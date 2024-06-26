package com.intellij.debugger.ui.tree.render;

import com.intellij.debugger.ClassFilter;
import com.intellij.debugger.DebuggerContext;
import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.tree.DebuggerTreeNode;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.debugger.ui.tree.ValueDescriptor;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiExpression;
import com.sun.jdi.*;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import java.util.Iterator;

/*
 * Copyright (c) 2000-2004 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */

public class ToStringRenderer extends NodeRendererImpl {
  public static final @NonNls String UNIQUE_ID = "ToStringRenderer";

  private boolean USE_CLASS_FILTERS = false;
  private ClassFilter[] myClassFilters = ClassFilter.EMPTY_ARRAY;

  public ToStringRenderer() {
    setEnabled(true);
  }

  public String getUniqueId() {
    return UNIQUE_ID;
  }

  public @NonNls String getName() {
    return "toString";
  }

  public void setName(String name) {
    // prohibit change
  }

  public ToStringRenderer clone() {
    final ToStringRenderer cloned = (ToStringRenderer)super.clone();
    final ClassFilter[] classFilters = (myClassFilters.length > 0)? new ClassFilter[myClassFilters.length] : ClassFilter.EMPTY_ARRAY;
    for (int idx = 0; idx < classFilters.length; idx++) {
      classFilters[idx] = myClassFilters[idx].clone();
    }
    cloned.myClassFilters = classFilters;
    return cloned;
  }

  public String calcLabel(final ValueDescriptor valueDescriptor, EvaluationContext evaluationContext, final DescriptorLabelListener labelListener)
    throws EvaluateException {
    final Value value = valueDescriptor.getValue();
    BatchEvaluator.getBatchEvaluator(evaluationContext.getDebugProcess()).invoke(new ToStringCommand(evaluationContext, value) {
      public void evaluationResult(String message) {
        valueDescriptor.setValueLabel(message != null ? "\"" + message + "\"" : "");
        labelListener.labelChanged();
      }

      public void evaluationError(String message) {
        final String msg = value != null? message + " " + DebuggerBundle.message("evaluation.error.cannot.evaluate.tostring", value.type().name()) : message;
        valueDescriptor.setValueLabelFailed(new EvaluateException(msg, null));
        labelListener.labelChanged();
      }
    });
    return NodeDescriptor.EVALUATING_MESSAGE;
  }

  public boolean isUseClassFilters() {
    return USE_CLASS_FILTERS;
  }

  public void setUseClassFilters(boolean value) {
    USE_CLASS_FILTERS = value;
  }

  public boolean isApplicable(Type type) {
    if(!(type instanceof ReferenceType)) {
      return false;
    }

    if(type.name().equals("java.lang.String")) {
      return false; // do not render 'String' objects for performance reasons
    }

    if(!overridesToString(type)) {
      return false;
    }

    if (USE_CLASS_FILTERS) {
      if (!isFiltered(type)) {
        return false;
      }
    }

    return true;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  private static boolean overridesToString(Type type) {
    if(type instanceof ClassType) {
      final ClassType classType = (ClassType)type;
      final java.util.List methods = classType.methodsByName("toString", "()Ljava/lang/String;");
      if (methods.size() > 0) {
        for (Iterator iterator = methods.iterator(); iterator.hasNext();) {
          final Method method = (Method)iterator.next();
          if(!(method.declaringType().name()).equals("java.lang.Object")){
            return true;
          }
        }
      }
    }
    return false;
  }

  public void buildChildren(Value value, ChildrenBuilder builder, EvaluationContext evaluationContext) {
    final DebugProcessImpl debugProcess = (DebugProcessImpl)evaluationContext.getDebugProcess();
    debugProcess.getDefaultRenderer(value).buildChildren(value, builder, evaluationContext);
  }

  public PsiExpression getChildValueExpression(DebuggerTreeNode node, DebuggerContext context) throws EvaluateException {
    final Value parentValue = ((ValueDescriptor)node.getParent().getDescriptor()).getValue();
    final DebugProcessImpl debugProcess = (DebugProcessImpl)context.getDebugProcess();
    return debugProcess.getDefaultRenderer(parentValue).getChildValueExpression(node, context);
  }

  public boolean isExpandable(Value value, EvaluationContext evaluationContext, NodeDescriptor parentDescriptor) {
    final DebugProcessImpl debugProcess = (DebugProcessImpl)evaluationContext.getDebugProcess();
    return debugProcess.getDefaultRenderer(value).isExpandable(value, evaluationContext, parentDescriptor);
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public void readExternal(Element element) throws InvalidDataException {
    super.readExternal(element);
    final String value = JDOMExternalizerUtil.readField(element, "USE_CLASS_FILTERS");
    USE_CLASS_FILTERS = "true".equalsIgnoreCase(value);
    myClassFilters = DebuggerUtilsEx.readFilters(element.getChildren("filter"));
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public void writeExternal(Element element) throws WriteExternalException {
    super.writeExternal(element);
    JDOMExternalizerUtil.writeField(element, "USE_CLASS_FILTERS", USE_CLASS_FILTERS? "true" : "false");
    DebuggerUtilsEx.writeFilters(element, "filter", myClassFilters);
  }

  public ClassFilter[] getClassFilters() {
    return myClassFilters;
  }

  public void setClassFilters(ClassFilter[] classFilters) {
    myClassFilters = classFilters != null? classFilters : ClassFilter.EMPTY_ARRAY;
  }

  private boolean isFiltered(Type t) {
    if (t instanceof ReferenceType) {
      for (ClassFilter classFilter : myClassFilters) {
        if (classFilter.isEnabled() && DebuggerUtilsEx.getSuperType(t, classFilter.getPattern()) != null) {
          return true;
        }
      }
    }
    return DebuggerUtilsEx.isFiltered(t.name(), myClassFilters);
  }
}
