package com.intellij.debugger.ui.tree.render;

import com.intellij.debugger.DebuggerManager;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessAdapter;
import com.intellij.debugger.engine.SuspendContext;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.engine.jdi.ThreadReferenceProxy;
import com.intellij.debugger.engine.managerThread.SuspendContextCommand;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.rt.debugger.BatchEvaluatorServer;
import com.intellij.util.containers.HashMap;
import com.sun.jdi.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * User: lex
 * Date: Jul 7, 2003
 * Time: 11:13:52 PM
 */

public class BatchEvaluator {
  private static final Logger LOG = Logger.getInstance("#com.intellij.debugger.ui.tree.render.BatchEvaluator");

  private final DebugProcess myDebugProcess;
  private boolean myBatchEvaluatorChecked;
  private ObjectReference myBatchEvaluatorObject;
  private Method myBatchEvaluatorMethod;

  private static final Key<BatchEvaluator> BATCH_EVALUATOR_KEY = new Key<BatchEvaluator>("BatchEvaluator");
  public static final Key<Boolean> REMOTE_SESSION_KEY = new Key<Boolean>("is_remote_session_key");

  private final HashMap<SuspendContext, List<ToStringCommand>> myBuffer = new HashMap<SuspendContext, List<ToStringCommand>>();

  private BatchEvaluator(DebugProcess process) {
    myDebugProcess = process;
    myDebugProcess.addDebugProcessListener(new DebugProcessAdapter() {
      public void processDetached(DebugProcess process, boolean closedByUser) {
        myBatchEvaluatorChecked = false;
        myBatchEvaluatorObject= null;
        myBatchEvaluatorMethod = null;
      }
    });
  }

  @SuppressWarnings({"HardCodedStringLiteral"}) public boolean hasBatchEvaluator(EvaluationContext evaluationContext) {
    if (!myBatchEvaluatorChecked) {
      myBatchEvaluatorChecked = true;

      final Boolean isRemote = myDebugProcess.getUserData(REMOTE_SESSION_KEY);
      if (isRemote != null && isRemote.booleanValue()) {
        // optimization: for remote sessions the BatchEvaluator is not there for sure
        return false;
      }

      ThreadReferenceProxy thread = evaluationContext.getSuspendContext().getThread();

      if (thread == null) {
        return false;
      }

      ThreadReference threadReference = thread.getThreadReference();
      if(threadReference == null) {
        return false;
      }

      ClassType batchEvaluatorClass = null;
      try {
        batchEvaluatorClass = (ClassType)myDebugProcess.findClass(evaluationContext, BatchEvaluatorServer.class.getName(),
          evaluationContext.getClassLoader());
      }
      catch (EvaluateException e) {
      }

      if (batchEvaluatorClass != null) {
        Method constructor = batchEvaluatorClass.concreteMethodByName("<init>", "()V");
        if(constructor != null){
          ObjectReference evaluator = null;
          try {
            evaluator = myDebugProcess.newInstance(evaluationContext, batchEvaluatorClass, constructor, new ArrayList());
          }
          catch (Exception e) {
            LOG.debug(e);
          }
          myBatchEvaluatorObject = evaluator;

          if(myBatchEvaluatorObject != null) {
            myBatchEvaluatorMethod = batchEvaluatorClass.concreteMethodByName("evaluate", "([Ljava/lang/Object;)[Ljava/lang/Object;");
          }
        }
      }
    }
    return myBatchEvaluatorMethod != null;
  }

  public void invoke(ToStringCommand command) {
    LOG.assertTrue(DebuggerManager.getInstance(myDebugProcess.getProject()).isDebuggerManagerThread());

    final EvaluationContext evaluationContext = command.getEvaluationContext();
    final SuspendContext suspendContext = evaluationContext.getSuspendContext();

    if(!hasBatchEvaluator(evaluationContext)) {
      myDebugProcess.getManagerThread().invokeCommand(command);
    }
    else {
      List<ToStringCommand> toStringCommands = myBuffer.get(suspendContext);
      if(toStringCommands == null) {
        final List<ToStringCommand> commands = new ArrayList<ToStringCommand>();
        toStringCommands = commands;
        myBuffer.put(suspendContext, commands);

        myDebugProcess.getManagerThread().invokeCommand(new SuspendContextCommand() {
          public SuspendContext getSuspendContext() {
            return suspendContext;
          }

          public void action() {
            myBuffer.remove(suspendContext);

            if(!doEvaluateBatch(commands, evaluationContext)) {
              for (Iterator<ToStringCommand> iterator = commands.iterator(); iterator.hasNext();) {
                ToStringCommand toStringCommand = iterator.next();
                toStringCommand.action();
              }
            }
          }

          public void commandCancelled() {
            myBuffer.remove(suspendContext);
          }
        });
      }

      toStringCommands.add(command);
    }
  }

  public static BatchEvaluator getBatchEvaluator(DebugProcess debugProcess) {
    BatchEvaluator batchEvaluator = debugProcess.getUserData(BATCH_EVALUATOR_KEY);

    if(batchEvaluator == null) {
      batchEvaluator = new BatchEvaluator(debugProcess);
      debugProcess.putUserData(BATCH_EVALUATOR_KEY, batchEvaluator);
    }
    return batchEvaluator;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  private boolean doEvaluateBatch(List<ToStringCommand> requests, EvaluationContext evaluationContext) {
    try {
      DebugProcess debugProcess = evaluationContext.getDebugProcess();
      List<Value> values = new ArrayList<Value>();
      for (Iterator<ToStringCommand> iterator = requests.iterator(); iterator.hasNext();) {
        ToStringCommand toStringCommand = iterator.next();
        final Value value = toStringCommand.getValue();
        values.add(value instanceof ObjectReference? ((ObjectReference)value) : value);
      }

      ArrayType objectArrayClass = (ArrayType)debugProcess.findClass(
        evaluationContext,
        "java.lang.Object[]",
        evaluationContext.getClassLoader());
      if (objectArrayClass == null) {
        return false;
      }

      ArrayReference argArray = debugProcess.newInstance(objectArrayClass, values.size());
      ((SuspendContextImpl)evaluationContext.getSuspendContext()).keep(argArray); // to avoid ObjectCollectedException
      argArray.setValues(values);
      List argList = new ArrayList(1);
      argList.add(argArray);
      Value value = debugProcess.invokeMethod(evaluationContext, myBatchEvaluatorObject,
                                              myBatchEvaluatorMethod, argList);
      if (value instanceof ArrayReference) {
        ((SuspendContextImpl)evaluationContext.getSuspendContext()).keep((ArrayReference)value); // to avoid ObjectCollectedException for both the array and its elements
        final ArrayReference strings = (ArrayReference)value;
        final List<Value> allValuesArray = strings.getValues();
        final Value[] allValues = allValuesArray.toArray(new Value[allValuesArray.size()]);
        int idx = 0;
        for (Iterator<ToStringCommand> iterator = requests.iterator(); iterator.hasNext(); idx++) {
          ToStringCommand request = iterator.next();
          final Value strValue = allValues[idx];
          if(strValue == null || strValue instanceof StringReference){
            try {
              String str = (strValue == null)? null : ((StringReference)strValue).value();
              request.evaluationResult(str);
            }
            catch (ObjectCollectedException e) {
              // ignored
            }
          } 
          else if(strValue instanceof ObjectReference){
            request.evaluationError(EvaluateExceptionUtil.createEvaluateException(new InvocationException((ObjectReference)strValue)).getMessage());
          } 
          else {
            LOG.assertTrue(false);
          }
          request.setEvaluated();
        }
      }
      return true;
    }
    catch (ClassNotLoadedException e) {
    }
    catch (InvalidTypeException e) {
    }
    catch (EvaluateException e) {
    }
    catch (ObjectCollectedException e) {
    }
    return false;
  }


}
