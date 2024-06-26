package com.intellij.debugger.ui.tree.render;

import com.intellij.debugger.DebuggerContext;
import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.engine.DebuggerManagerThreadImpl;
import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.tree.DebuggerTreeNode;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.debugger.ui.tree.ValueDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiExpression;
import com.sun.jdi.*;
import org.jetbrains.annotations.NonNls;

/**
 * User: lex
 * Date: Sep 18, 2003
 * Time: 3:07:27 PM
 */
public class PrimitiveRenderer extends NodeRendererImpl {
  public static final @NonNls String UNIQUE_ID = "PrimitiveRenderer";
  private static final Logger LOG = Logger.getInstance("#com.intellij.debugger.ui.tree.render.PrimitiveRenderer");

  public PrimitiveRenderer() {
    //noinspection HardCodedStringLiteral
    myProperties.setName("Primitive");
  }

  public String getUniqueId() {
    return UNIQUE_ID;
  }

  public @NonNls String getName() {
    return "Primitive";
  }

  public void setName(String text) {
    // prohibit name change
  }

  public final boolean isEnabled() {
    return true;
  }

  public void setEnabled(boolean enabled) {
    // prohibit change
  }

  public boolean isApplicable(Type type) {
    return (type == null) || (type instanceof PrimitiveType) || (type instanceof VoidType);
  }

  public String calcLabel(ValueDescriptor valueDescriptor, EvaluationContext evaluationContext, DescriptorLabelListener labelListener) {
    Value value = valueDescriptor.getValue();
    if(value == null) {
      //noinspection HardCodedStringLiteral
      return "null";
    }
    else if (value instanceof PrimitiveValue) {
      StringBuffer buf = new StringBuffer(16);
      if (value instanceof CharValue) {
        buf.append("'");
        buf.append(value.toString());
        buf.append("' ");
        long longValue = ((PrimitiveValue)value).longValue();
        buf.append(Long.toString(longValue));
      }
      else if (value instanceof ByteValue) {
        buf.append(value.toString());
      }
      else if (value instanceof ShortValue) {
        buf.append(value.toString());
      }
      else if (value instanceof IntegerValue) {
        buf.append(value.toString());
      }
      else if (value instanceof LongValue) {
        buf.append(value.toString());
      }
      else {
        buf.append(value.toString());
      }
      return buf.toString();
    }
    else {
      return DebuggerBundle.message("label.undefined");
    }
  }

  public void buildChildren(Value value, ChildrenBuilder builder, EvaluationContext evaluationContext) {
    DebuggerManagerThreadImpl.assertIsManagerThread();
  }

  public ChildrenRenderer getChildrenRenderer() {
    return this;
  }

  public ValueLabelRenderer getLabelRenderer() {
    return this;
  }

  public PsiExpression getChildValueExpression(DebuggerTreeNode node, DebuggerContext context) {
    LOG.assertTrue(false);
    return null;
  }

  public boolean isExpandable(Value value, EvaluationContext evaluationContext, NodeDescriptor parentDescriptor) {
    return false;
  }
}
