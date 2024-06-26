/**
 * class NewArrayInstanceEvaluator
 * created Jun 27, 2001
 * @author Jeka
 */
package com.intellij.debugger.engine.evaluation.expression;

import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.DebuggerBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.sun.jdi.*;

import java.util.ArrayList;
import java.util.List;

class NewArrayInstanceEvaluator implements Evaluator {
  private static final Logger LOG = Logger.getInstance("#com.intellij.debugger.engine.evaluation.expression.NewArrayInstanceEvaluator");
  private Evaluator myArrayTypeEvaluator;
  private Evaluator myDimensionEvaluator = null;
  private Evaluator myInitializerEvaluator = null;

  /**
   * either dimensionEvaluator or initializerEvaluators must be null!
   */
  public NewArrayInstanceEvaluator(Evaluator arrayTypeEvaluator, Evaluator dimensionEvaluator, Evaluator initializerEvaluator) {
    myArrayTypeEvaluator = arrayTypeEvaluator;
    myDimensionEvaluator = dimensionEvaluator;
    myInitializerEvaluator = initializerEvaluator;
  }

  public Object evaluate(EvaluationContextImpl context) throws EvaluateException {
//    throw new EvaluateException("Creating new array instances is not supported yet", true);
    DebugProcessImpl debugProcess = context.getDebugProcess();
    Object obj = myArrayTypeEvaluator.evaluate(context);
    if (!(obj instanceof ArrayType)) {
      throw EvaluateExceptionUtil.createEvaluateException(DebuggerBundle.message("evaluation.error.array.type.expected"));
    }
    ArrayType arrayType = (ArrayType)obj;
    int dimension;
    Object[] initialValues = null;
    if (myDimensionEvaluator != null) {
      Object o = myDimensionEvaluator.evaluate(context);
      if (!(o instanceof Value && DebuggerUtilsEx.isNumeric((Value)o))) {
        throw EvaluateExceptionUtil.createEvaluateException(
          DebuggerBundle.message("evaluation.error.array.dimention.numeric.value.expected")
        );
      }
      PrimitiveValue value = (PrimitiveValue)o;
      dimension = value.intValue();
    }
    else { // myInitializerEvaluator must not be null
      Object o = myInitializerEvaluator.evaluate(context);
      if (!(o instanceof Object[])) {
        throw EvaluateExceptionUtil.createEvaluateException(DebuggerBundle.message("evaluation.error.cannot.evaluate.array.initializer"));
      }
      initialValues = (Object[])o;
      dimension = initialValues.length;
    }
    ArrayReference arrayReference = debugProcess.newInstance(arrayType, dimension);
    if (initialValues != null && initialValues.length > 0) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Setting initial values: dimension = "+dimension + "; array size is "+initialValues.length);
      }
      setInitialValues(arrayReference, initialValues, context);
    }
    return arrayReference;
  }

  private void setInitialValues(ArrayReference arrayReference, Object[] values, EvaluationContextImpl context) throws EvaluateException {
    ArrayType type = (ArrayType)arrayReference.referenceType();
    DebugProcessImpl debugProcess = context.getDebugProcess();
    try {
      if (type.componentType() instanceof ArrayType) {
        ArrayType componentType = (ArrayType)type.componentType();
        int length = arrayReference.length();
        for (int idx = 0; idx < length; idx++) {
          ArrayReference componentArray = (ArrayReference)arrayReference.getValue(idx);
          Object[] componentArrayValues = (Object[])values[idx];
          if (componentArray == null) {
            componentArray = debugProcess.newInstance(componentType, componentArrayValues.length);
            arrayReference.setValue(idx, componentArray);
          }
          setInitialValues(componentArray, componentArrayValues, context);
        }
      }
      else {
        if (values.length > 0) {
          List list = new ArrayList(values.length);
          for (int idx = 0; idx < values.length; idx++) {
            list.add(values[idx]);
          }
          arrayReference.setValues(list);
        }
      }
    }
    catch (ClassNotLoadedException ex) {
      final ReferenceType referenceType;
      try {
        referenceType = debugProcess.loadClass(context, ex.className(), type.classLoader());
      }
      catch (InvocationException e) {
        throw EvaluateExceptionUtil.createEvaluateException(e);
      }
      catch (ClassNotLoadedException e) {
        throw EvaluateExceptionUtil.createEvaluateException(e);
      }
      catch (IncompatibleThreadStateException e) {
        throw EvaluateExceptionUtil.createEvaluateException(e);
      }
      catch (InvalidTypeException e) {
        throw EvaluateExceptionUtil.createEvaluateException(e);
      }
      if (referenceType != null) {
        setInitialValues(arrayReference, values, context);
      }
      else {
        throw EvaluateExceptionUtil.createEvaluateException(DebuggerBundle.message("error.class.not.loaded", ex.className()));
      }
    }
    catch (InvalidTypeException ex) {
      throw EvaluateExceptionUtil.createEvaluateException(DebuggerBundle.message("evaluation.error.incompatible.array.initializer.type"));
    }
    catch (IndexOutOfBoundsException ex) {
      throw EvaluateExceptionUtil.createEvaluateException(DebuggerBundle.message("evaluation.error.invalid.array.size"));
    }
    catch (ClassCastException ex) {
      throw EvaluateExceptionUtil.createEvaluateException(DebuggerBundle.message("evaluation.error.cannot.initialize.array"));
    }
  }

  public Modifier getModifier() {
    return null;
  }
}
