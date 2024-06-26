/*
 * Copyright 2000-2007 JetBrains s.r.o.
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
package com.intellij.debugger.engine.evaluation;

import com.intellij.debugger.DebuggerBundle;
import com.sun.jdi.*;

/**
 * Created by IntelliJ IDEA.
 * User: lex
 * Date: Apr 8, 2004
 * Time: 4:38:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class EvaluateExceptionUtil {
  public static final EvaluateException INCONSISTEND_DEBUG_INFO = createEvaluateException(DebuggerBundle.message("evaluation.error.inconsistent.debug.info"));
  public static final EvaluateException BOOLEAN_EXPECTED = createEvaluateException(DebuggerBundle.message("evaluation.error.boolean.value.expected.in.condition"));
  public static final EvaluateException PROCESS_EXITED = createEvaluateException(DebuggerBundle.message("evaluation.error.process.exited"));
  public static final EvaluateException NULL_STACK_FRAME = createEvaluateException(DebuggerBundle.message("evaluation.error.stack.frame.unavailable"));
  public static final EvaluateException NESTED_EVALUATION_ERROR = createEvaluateException(DebuggerBundle.message("evaluation.error.nested.evaluation"));
  public static final EvaluateException INVALID_DEBUG_INFO = createEvaluateException(DebuggerBundle.message("evaluation.error.sources.out.of.sync"));
  public static final EvaluateException CANNOT_FIND_SOURCE_CLASS = createEvaluateException(DebuggerBundle.message("evaluation.error.cannot.find.stackframe.source"));
  public static final EvaluateException OBJECT_WAS_COLLECTED = createEvaluateException(DebuggerBundle.message("evaluation.error.object.collected"));
  public static final EvaluateException ARRAY_WAS_COLLECTED = createEvaluateException(DebuggerBundle.message("evaluation.error.array.collected"));
  public static final EvaluateException THREAD_WAS_RESUMED = createEvaluateException(DebuggerBundle.message("evaluation.error.thread.resumed"));
  public static final EvaluateException DEBUG_INFO_UNAVAILABLE = createEvaluateException(DebuggerBundle.message("evaluation.error.debug.info.unavailable"));

  public static EvaluateException createEvaluateException(Throwable th) {
    return createEvaluateException(null, th);
  }

  public static EvaluateException createEvaluateException(String msg, Throwable th) {
    final String message = msg != null? msg + ": " + reason(th) : reason(th);
    return new EvaluateException(message, th instanceof EvaluateException ? th.getCause() : th);
  }

  public static EvaluateException createEvaluateException(String reason) {
    return new EvaluateException(reason, null);
  }

  private static String reason(Throwable th) {
    if(th instanceof InvalidTypeException) {
      return DebuggerBundle.message("evaluation.error.type.mismatch");
    }
    else if(th instanceof AbsentInformationException) {
      return DebuggerBundle.message("evaluation.error.debug.info.unavailable");
    }
    else if(th instanceof ClassNotLoadedException) {
      return DebuggerBundle.message("evaluation.error.class.not.loaded", ((ClassNotLoadedException)th).className());
    }
    else if(th instanceof ClassNotPreparedException) {
      return th.getMessage();
    }
    else if(th instanceof IncompatibleThreadStateException) {
      return DebuggerBundle.message("evaluation.error.thread.not.at.breakpoint");
    }
    else if(th instanceof InconsistentDebugInfoException) {
      return DebuggerBundle.message("evaluation.error.inconsistent.debug.info");
    }
    else if(th instanceof ObjectCollectedException) {
      return DebuggerBundle.message("evaluation.error.object.collected");
    }
    else if(th instanceof InvocationException){
      InvocationException invocationException = (InvocationException) th;
      return DebuggerBundle.message("evaluation.error.method.exception", invocationException.exception().referenceType().name());
    }
    else if(th instanceof EvaluateException) {
      return th.getMessage();
    }
    else {
      return th.getClass().getName() + " : " + (th.getMessage() != null ? th.getMessage() : "");
    }
  }
}
