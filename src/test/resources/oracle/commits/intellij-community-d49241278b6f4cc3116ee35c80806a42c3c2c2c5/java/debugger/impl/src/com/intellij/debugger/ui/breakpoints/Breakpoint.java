/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * Class Breakpoint
 * @author Jeka
 */
package com.intellij.debugger.ui.breakpoints;

import com.intellij.debugger.*;
import com.intellij.debugger.engine.*;
import com.intellij.debugger.engine.evaluation.*;
import com.intellij.debugger.engine.evaluation.expression.EvaluatorBuilderImpl;
import com.intellij.debugger.engine.evaluation.expression.ExpressionEvaluator;
import com.intellij.debugger.engine.events.SuspendContextCommandImpl;
import com.intellij.debugger.engine.requests.RequestManagerImpl;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.debugger.settings.DebuggerSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.classFilter.ClassFilter;
import com.intellij.util.StringBuilderSpinAllocator;
import com.intellij.xdebugger.breakpoints.SuspendPolicy;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaBreakpointProperties;

import javax.swing.*;
import java.util.List;

public abstract class Breakpoint<P extends JavaBreakpointProperties> implements FilteredRequestor, ClassPrepareRequestor {
  final XBreakpoint<P> myXBreakpoint;
  protected final Project myProject;

  //private boolean ENABLED = true;
  //private boolean LOG_ENABLED = false;
  //private boolean LOG_EXPRESSION_ENABLED = false;
  //private boolean REMOVE_AFTER_HIT = false;
  //private TextWithImports  myLogMessage; // an expression to be evaluated and printed
  //@NonNls private static final String LOG_MESSAGE_OPTION_NAME = "LOG_MESSAGE";
  public static final Breakpoint[] EMPTY_ARRAY = new Breakpoint[0];
  protected boolean myCachedVerifiedState = false;
  //private TextWithImportsImpl myLogMessage;

  protected Breakpoint(@NotNull Project project, XBreakpoint<P> xBreakpoint) {
    //super(project);
    myProject = project;
    myXBreakpoint = xBreakpoint;
    //myLogMessage = new TextWithImportsImpl(CodeFragmentKind.EXPRESSION, "");
    //noinspection AbstractMethodCallInConstructor
    //final BreakpointDefaults defaults = DebuggerManagerEx.getInstanceEx(project).getBreakpointManager().getBreakpointDefaults(getCategory());
    //SUSPEND_POLICY = defaults.getSuspendPolicy();
    //CONDITION_ENABLED = defaults.isConditionEnabled();
  }

  public Project getProject() {
    return myProject;
  }

  protected P getProperties() {
    return myXBreakpoint.getProperties();
  }

  public abstract PsiClass getPsiClass();
  /**
   * Request for creating all needed JPDA requests in the specified VM
   * @param debuggerProcess the requesting process
   */
  public abstract void createRequest(DebugProcessImpl debuggerProcess);

  /**
   * Request for creating all needed JPDA requests in the specified VM
   * @param debuggerProcess the requesting process
   */
  @Override
  public abstract void processClassPrepare(DebugProcess debuggerProcess, final ReferenceType referenceType);

  public abstract String getDisplayName ();
  
  public String getShortName() {
    return getDisplayName();
  }

  @Nullable
  public String getClassName() {
    return null;
  }

  public void markVerified(boolean isVerified) {
    myCachedVerifiedState = isVerified;
  }

  public boolean isRemoveAfterHit() {
    return myXBreakpoint instanceof XLineBreakpoint && ((XLineBreakpoint)myXBreakpoint).isTemporary();
  }

  public void setRemoveAfterHit(boolean value) {
    if (myXBreakpoint instanceof XLineBreakpoint) {
      ((XLineBreakpoint)myXBreakpoint).setTemporary(value);
    }
  }

  @Nullable
  public String getShortClassName() {
    final String className = getClassName();
    if (className != null) {
      final int dotIndex = className.lastIndexOf('.');
      return dotIndex >= 0 && dotIndex + 1 < className.length()? className.substring(dotIndex + 1) : className;
    }
    return className;
  }

  @Nullable
  public String getPackageName() {
    return null;
  }

  public abstract Icon getIcon();

  public abstract void reload();

  /**
   * returns UI representation
   */
  public abstract String getEventMessage(LocatableEvent event);

  public abstract boolean isValid();

  public abstract Key<? extends Breakpoint> getCategory();

  /**
   * Associates breakpoint with class.
   *    Create requests for loaded class and registers callback for loading classes
   * @param debugProcess the requesting process
   */
  protected void createOrWaitPrepare(DebugProcessImpl debugProcess, String classToBeLoaded) {
    debugProcess.getRequestsManager().callbackOnPrepareClasses(this, classToBeLoaded);

    List list = debugProcess.getVirtualMachineProxy().classesByName(classToBeLoaded);
    for (final Object aList : list) {
      ReferenceType refType = (ReferenceType)aList;
      if (refType.isPrepared()) {
        processClassPrepare(debugProcess, refType);
      }
    }
  }

  protected void createOrWaitPrepare(final DebugProcessImpl debugProcess, final SourcePosition classPosition) {
    debugProcess.getRequestsManager().callbackOnPrepareClasses(this, classPosition);

    for (ReferenceType refType : debugProcess.getPositionManager().getAllClasses(classPosition)) {
      if (refType.isPrepared()) {
        processClassPrepare(debugProcess, refType);
      }
    }
  }

  protected ObjectReference getThisObject(SuspendContextImpl context, LocatableEvent event) throws EvaluateException {
    ThreadReferenceProxyImpl thread = context.getThread();
    if(thread != null) {
      StackFrameProxyImpl stackFrameProxy = thread.frame(0);
      if(stackFrameProxy != null) {
        return stackFrameProxy.thisObject();
      }
    }
    return null;
  }

  @Override
  public boolean processLocatableEvent(final SuspendContextCommandImpl action, final LocatableEvent event) throws EventProcessingException {
    final SuspendContextImpl context = action.getSuspendContext();
    if(!isValid()) {
      context.getDebugProcess().getRequestsManager().deleteRequest(this);
      return false;
    }

    final String[] title = {DebuggerBundle.message("title.error.evaluating.breakpoint.condition") };

    try {
      final StackFrameProxyImpl frameProxy = context.getThread().frame(0);
      if (frameProxy == null) {
        // might be if the thread has been collected
        return false;
      }

      final EvaluationContextImpl evaluationContext = new EvaluationContextImpl(
        action.getSuspendContext(),
        frameProxy,
        getThisObject(context, event)
      );

      if(!evaluateCondition(evaluationContext, event)) {
        return false;
      }

      title[0] = DebuggerBundle.message("title.error.evaluating.breakpoint.action");
      runAction(evaluationContext, event);
    }
    catch (final EvaluateException ex) {
      if(ApplicationManager.getApplication().isUnitTestMode()) {
        System.out.println(ex.getMessage());
        return false;
      }

      throw new EventProcessingException(title[0], ex.getMessage(), ex);
    } 

    return true;
  }

  private void runAction(final EvaluationContextImpl context, LocatableEvent event) {
    final DebugProcessImpl debugProcess = context.getDebugProcess();
    if (isLogEnabled() || isLogExpressionEnabled()) {
      final StringBuilder buf = StringBuilderSpinAllocator.alloc();
      try {
        if (myXBreakpoint.isLogMessage()) {
          buf.append(getEventMessage(event));
          buf.append("\n");
        }
        final TextWithImports expressionToEvaluate = getLogMessage();
        if (myXBreakpoint.getLogExpression() != null && !expressionToEvaluate.getText().isEmpty()) {
          if(!debugProcess.isAttached()) {
            return;
          }
  
          try {
            ExpressionEvaluator evaluator = DebuggerInvocationUtil.commitAndRunReadAction(getProject(), new EvaluatingComputable<ExpressionEvaluator>() {
              @Override
              public ExpressionEvaluator compute() throws EvaluateException {
                return EvaluatorBuilderImpl.build(expressionToEvaluate, ContextUtil.getContextElement(context), ContextUtil.getSourcePosition(context));
              }
            });
            final Value eval = evaluator.evaluate(context);
            final String result = eval instanceof VoidValue ? "void" : DebuggerUtils.getValueAsString(context, eval);
            buf.append(result);
          }
          catch (EvaluateException e) {
            buf.append(DebuggerBundle.message("error.unable.to.evaluate.expression"));
            buf.append(" \"");
            buf.append(expressionToEvaluate);
            buf.append("\"");
            buf.append(" : ");
            buf.append(e.getMessage());
          }
          buf.append("\n");
        }
        if (buf.length() > 0) {
          debugProcess.printToConsole(buf.toString());
        }
      }
      finally {
        StringBuilderSpinAllocator.dispose(buf);
      }
    }
    if (isRemoveAfterHit()) {
      handleTemporaryBreakpointHit(debugProcess);
    }
  }

  /**
   * @return true if the ID was added or false otherwise
   */
  private boolean hasObjectID(long id) {
    for (InstanceFilter instanceFilter : getInstanceFilters()) {
      if (instanceFilter.getId() == id) {
        return true;
      }
    }
    return false;
  }

  public boolean evaluateCondition(final EvaluationContextImpl context, LocatableEvent event) throws EvaluateException {
    if(isCountFilterEnabled()) {
      final DebugProcessImpl debugProcess = context.getDebugProcess();
      debugProcess.getVirtualMachineProxy().suspend();
      debugProcess.getRequestsManager().deleteRequest(this);
      ((Breakpoint)this).createRequest(debugProcess);
      debugProcess.getVirtualMachineProxy().resume();
    }
    if (isInstanceFiltersEnabled()) {
      Value value = context.getThisObject();
      if (value != null) {  // non-static
        ObjectReference reference = (ObjectReference)value;
        if(!hasObjectID(reference.uniqueID())) {
          return false;
        }
      }
    }

    if (isClassFiltersEnabled()) {
      String typeName = calculateEventClass(context, event);
      if (!typeMatchesClassFilters(typeName)) return false;
    }

    if (isConditionEnabled() && !getCondition().getText().isEmpty()) {
      try {
        ExpressionEvaluator evaluator = DebuggerInvocationUtil.commitAndRunReadAction(context.getProject(), new EvaluatingComputable<ExpressionEvaluator>() {
          public ExpressionEvaluator compute() throws EvaluateException {
            final SourcePosition contextSourcePosition = ContextUtil.getSourcePosition(context);
            // IMPORTANT: calculate context psi element basing on the location where the exception
            // has been hit, not on the location where it was set. (For line breakpoints these locations are the same, however,
            // for method, exception and field breakpoints these locations differ)
            PsiElement contextPsiElement = ContextUtil.getContextElement(contextSourcePosition);
            if (contextPsiElement == null) {
              contextPsiElement = getEvaluationElement(); // as a last resort
            }
            return EvaluatorBuilderImpl.build(getCondition(), contextPsiElement, contextSourcePosition);
          }
        });
        final Value value = evaluator.evaluate(context);
        if (!(value instanceof BooleanValue)) {
          throw EvaluateExceptionUtil.createEvaluateException(DebuggerBundle.message("evaluation.error.boolean.expected"));
        }
        if(!((BooleanValue)value).booleanValue()) {
          return false;
        }
      }
      catch (EvaluateException ex) {
        if(ex.getCause() instanceof VMDisconnectedException) {
          return false;
        }
        throw EvaluateExceptionUtil.createEvaluateException(
          DebuggerBundle.message("error.failed.evaluating.breakpoint.condition", getCondition(), ex.getMessage())
        );
      }
      return true;
    }

    return true;
  }

  protected String calculateEventClass(EvaluationContextImpl context, LocatableEvent event) throws EvaluateException {
    return event.location().declaringType().name();
  }

  private boolean typeMatchesClassFilters(@Nullable String typeName) {
    if (typeName == null) {
      return true;
    }
    boolean matches = false, hasEnabled = false;
    for (ClassFilter classFilter : getClassFilters()) {
      if (classFilter.isEnabled()) {
        hasEnabled = true;
        if (classFilter.matches(typeName)) {
          matches = true;
          break;
        }
      }
    }
    if(hasEnabled && !matches) {
      return false;
    }
    for (ClassFilter classFilter : getClassExclusionFilters()) {
      if (classFilter.isEnabled() && classFilter.matches(typeName)) {
        return false;
      }
    }
    return true;
  }

  private void handleTemporaryBreakpointHit(final DebugProcessImpl debugProcess) {
    debugProcess.addDebugProcessListener(new DebugProcessAdapter() {
      @Override
      public void resumed(SuspendContext suspendContext) {
        removeBreakpoint();
      }

      @Override
      public void processDetached(DebugProcess process, boolean closedByUser) {
        removeBreakpoint();
      }

      private void removeBreakpoint() {
        AppUIUtil.invokeOnEdt(new Runnable() {
          @Override
          public void run() {
            DebuggerManagerEx.getInstanceEx(myProject).getBreakpointManager().removeBreakpoint(Breakpoint.this);
          }
        });
        debugProcess.removeDebugProcessListener(this);
      }
    });
  }

  public void updateUI() {
  }

  public void delete() {
    RequestManagerImpl.deleteRequests(this);
  }

  //@Override
  //public void readExternal(Element parentNode) throws InvalidDataException {
    //super.readExternal(parentNode);
    //String logMessage = JDOMExternalizerUtil.readField(parentNode, LOG_MESSAGE_OPTION_NAME);
    //if (logMessage != null) {
    //  setLogMessage(new TextWithImportsImpl(CodeFragmentKind.EXPRESSION, logMessage));
    //}
  //}

  //@Override
  //public void writeExternal(Element parentNode) throws WriteExternalException {
    //super.writeExternal(parentNode);
    //JDOMExternalizerUtil.writeField(parentNode, LOG_MESSAGE_OPTION_NAME, getLogMessage().toExternalForm());
  //}

  //public void setLogMessage(TextWithImports logMessage) {
  //  myLogMessage = logMessage;
  //}

  public abstract PsiElement getEvaluationElement();

  protected TextWithImports getLogMessage() {
    return new TextWithImportsImpl(CodeFragmentKind.EXPRESSION, myXBreakpoint.getLogExpression());
  }

  protected TextWithImports getCondition() {
    return new TextWithImportsImpl(CodeFragmentKind.EXPRESSION, myXBreakpoint.getCondition());
  }

  public boolean isEnabled() {
    return myXBreakpoint.isEnabled();
  }

  public void setEnabled(boolean enabled) {
    myXBreakpoint.setEnabled(enabled);
  }

  protected boolean isLogEnabled() {
    return myXBreakpoint.isLogMessage();
  }

  public void setLogEnabled(boolean logEnabled) {
    myXBreakpoint.setLogMessage(logEnabled);
  }

  protected boolean isLogExpressionEnabled() {
    return myXBreakpoint.getLogExpression() != null;
  }

  protected void setLogExpressionEnabled(boolean LOG_EXPRESSION_ENABLED) {
  }

  @Override
  public boolean isCountFilterEnabled() {
    return myXBreakpoint.getProperties().COUNT_FILTER_ENABLED;
  }
  public void setCountFilterEnabled(boolean enabled) {
    myXBreakpoint.getProperties().COUNT_FILTER_ENABLED = enabled;
  }

  @Override
  public int getCountFilter() {
    return myXBreakpoint.getProperties().COUNT_FILTER;
  }

  public void setCountFilter(int filter) {
    myXBreakpoint.getProperties().COUNT_FILTER = filter;
  }

  @Override
  public boolean isClassFiltersEnabled() {
    return myXBreakpoint.getProperties().CLASS_FILTERS_ENABLED;
  }

  public void setClassFiltersEnabled(boolean enabled) {
    myXBreakpoint.getProperties().CLASS_FILTERS_ENABLED = enabled;
  }

  @Override
  public ClassFilter[] getClassFilters() {
    return myXBreakpoint.getProperties().getClassFilters();
  }

  public void setClassFilters(ClassFilter[] filters) {
    myXBreakpoint.getProperties().setClassFilters(filters);
  }

  @Override
  public ClassFilter[] getClassExclusionFilters() {
    return myXBreakpoint.getProperties().getClassExclusionFilters();
  }

  protected void setClassExclusionFilters(ClassFilter[] filters) {
    myXBreakpoint.getProperties().setClassExclusionFilters(filters);
  }

  @Override
  public boolean isInstanceFiltersEnabled() {
    return myXBreakpoint.getProperties().INSTANCE_FILTERS_ENABLED;
  }

  public void setInstanceFiltersEnabled(boolean enabled) {
    myXBreakpoint.getProperties().INSTANCE_FILTERS_ENABLED = enabled;
  }

  @Override
  public InstanceFilter[] getInstanceFilters() {
    return myXBreakpoint.getProperties().getInstanceFilters();
  }

  public void setInstanceFilters(InstanceFilter[] filters) {
    myXBreakpoint.getProperties().setInstanceFilters(filters);
  }

  private static String getSuspendPolicy(XBreakpoint breakpoint) {
    switch (breakpoint.getSuspendPolicy()) {
      case ALL:
        return DebuggerSettings.SUSPEND_ALL;
      case THREAD:
        return DebuggerSettings.SUSPEND_THREAD;
      case NONE:
        return DebuggerSettings.SUSPEND_NONE;

      default:
        throw new IllegalArgumentException("unknown suspend policy");
    }
  }

  private static SuspendPolicy transformSuspendPolicy(String policy) {
    if (DebuggerSettings.SUSPEND_ALL.equals(policy)) {
      return SuspendPolicy.ALL;
    } else if (DebuggerSettings.SUSPEND_THREAD.equals(policy)) {
      return SuspendPolicy.THREAD;
    } else if (DebuggerSettings.SUSPEND_NONE.equals(policy)) {
      return SuspendPolicy.NONE;
    } else {
      throw new IllegalArgumentException("unknown suspend policy");
    }
  }

  protected boolean isSuspend() {
    return myXBreakpoint.getSuspendPolicy() != SuspendPolicy.NONE;
  }

  @Override
  public String getSuspendPolicy() {
    return getSuspendPolicy(myXBreakpoint);
  }

  public void setSuspendPolicy(String policy) {
    myXBreakpoint.setSuspendPolicy(transformSuspendPolicy(policy));
  }

  protected void setLogMessage(TextWithImports logMessage) {
    myXBreakpoint.setLogExpression(logMessage.getText());
  }

  protected boolean isConditionEnabled() {
    return myXBreakpoint.getCondition() != null && !myXBreakpoint.getCondition().isEmpty();
  }

  public void setCondition(String condition) {
    myXBreakpoint.setCondition(condition);
  }

  protected void addInstanceFilter(long l) {
    myXBreakpoint.getProperties().addInstanceFilter(l);
  }
}
