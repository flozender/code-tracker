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

/*
 * Class BreakpointManager
 * @author Jeka
 */
package com.intellij.debugger.ui.breakpoints;

import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.DebuggerInvocationUtil;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.engine.BreakpointStepMethodFilter;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.requests.RequestManagerImpl;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.impl.DebuggerContextListener;
import com.intellij.debugger.impl.DebuggerManagerImpl;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.ui.JavaDebuggerSupport;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiField;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.*;
import com.intellij.xdebugger.impl.DebuggerSupport;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointManagerImpl;
import com.intellij.xdebugger.impl.breakpoints.XDependentBreakpointManager;
import com.intellij.xdebugger.impl.breakpoints.XLineBreakpointImpl;
import com.sun.jdi.InternalException;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.*;
import gnu.trove.THashMap;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaExceptionBreakpointProperties;

import javax.swing.*;
import java.util.*;

public class BreakpointManager {
  private static final Logger LOG = Logger.getInstance("#com.intellij.debugger.ui.breakpoints.BreakpointManager");

  @NonNls private static final String MASTER_BREAKPOINT_TAGNAME = "master_breakpoint";
  @NonNls private static final String SLAVE_BREAKPOINT_TAGNAME = "slave_breakpoint";
  @NonNls private static final String DEFAULT_SUSPEND_POLICY_ATTRIBUTE_NAME = "default_suspend_policy";
  @NonNls private static final String DEFAULT_CONDITION_STATE_ATTRIBUTE_NAME = "default_condition_enabled";

  @NonNls private static final String RULES_GROUP_NAME = "breakpoint_rules";
  private static final String CONVERTED_PARAM = "converted";

  private final Project myProject;
  private final Map<XBreakpoint, Breakpoint> myBreakpoints = new HashMap<XBreakpoint, Breakpoint>(); // breakpoints storage, access should be synchronized
  @Nullable private List<Breakpoint> myBreakpointsListForIteration = null; // another list for breakpoints iteration, unsynchronized access ok
  private final Map<String, String> myUIProperties = new LinkedHashMap<String, String>();

  private final StartupManager myStartupManager;

  public BreakpointManager(@NotNull Project project, @NotNull StartupManager startupManager, @NotNull DebuggerManagerImpl debuggerManager) {
    myProject = project;
    myStartupManager = startupManager;
    debuggerManager.getContextManager().addListener(new DebuggerContextListener() {
      private DebuggerSession myPreviousSession;

      @Override
      public void changeEvent(@NotNull DebuggerContextImpl newContext, int event) {
        if (event == DebuggerSession.EVENT_ATTACHED) {
          for (XBreakpoint breakpoint : getXBreakpointManager().getAllBreakpoints()) {
            if (checkAndNotifyPossiblySlowBreakpoint(breakpoint)) break;
          }
        }
        if (newContext.getDebuggerSession() != myPreviousSession || event == DebuggerSession.EVENT_DETACHED) {
          updateBreakpointsUI();
          myPreviousSession = newContext.getDebuggerSession();
        }
      }
    });
  }

  private boolean checkAndNotifyPossiblySlowBreakpoint(XBreakpoint breakpoint) {
    if (breakpoint.isEnabled() &&
        (breakpoint.getType() instanceof JavaMethodBreakpointType || breakpoint.getType() instanceof JavaWildcardMethodBreakpointType)) {
      XDebugSessionImpl.NOTIFICATION_GROUP.createNotification("Method breakpoints may dramatically slow down debugging", MessageType.WARNING).notify(myProject);
      return true;
    }
    return false;
  }

  public void init() {
    XBreakpointManager manager = XDebuggerManager.getInstance(myProject).getBreakpointManager();
    manager.addBreakpointListener(new XBreakpointAdapter<XBreakpoint<?>>() {
      @Override
      public void breakpointChanged(@NotNull XBreakpoint xBreakpoint) {
        Breakpoint breakpoint = myBreakpoints.get(xBreakpoint);
        if (breakpoint != null) {
          fireBreakpointChanged(breakpoint);
        }
      }
    });
  }

  private XBreakpointManager getXBreakpointManager() {
    return XDebuggerManager.getInstance(myProject).getBreakpointManager();
  }

  public void editBreakpoint(final Breakpoint breakpoint, final Editor editor) {
    DebuggerInvocationUtil.swingInvokeLater(myProject, new Runnable() {
      @Override
      public void run() {
        XBreakpoint xBreakpoint = breakpoint.myXBreakpoint;
        if (xBreakpoint instanceof XLineBreakpointImpl) {
          RangeHighlighter highlighter = ((XLineBreakpointImpl)xBreakpoint).getHighlighter();
          if (highlighter != null) {
            GutterIconRenderer renderer = highlighter.getGutterIconRenderer();
            if (renderer != null) {
              DebuggerSupport.getDebuggerSupport(JavaDebuggerSupport.class).getEditBreakpointAction().editBreakpoint(
                myProject, editor, breakpoint.myXBreakpoint, renderer
              );
            }
          }
        }
      }
    });
  }

  public void setBreakpointDefaults(Key<? extends Breakpoint> category, BreakpointDefaults defaults) {
    Class typeCls = null;
    if (LineBreakpoint.CATEGORY.toString().equals(category.toString())) {
      typeCls = JavaLineBreakpointType.class;
    }
    else if (MethodBreakpoint.CATEGORY.toString().equals(category.toString())) {
      typeCls = JavaMethodBreakpointType.class;
    }
    else if (FieldBreakpoint.CATEGORY.toString().equals(category.toString())) {
      typeCls = JavaFieldBreakpointType.class;
    }
    else if (ExceptionBreakpoint.CATEGORY.toString().equals(category.toString())) {
      typeCls = JavaExceptionBreakpointType.class;
    }
    if (typeCls != null) {
      XBreakpointType<XBreakpoint<?>, ?> type = XDebuggerUtil.getInstance().findBreakpointType(typeCls);
      ((XBreakpointManagerImpl)getXBreakpointManager()).getBreakpointDefaults(type).setSuspendPolicy(Breakpoint.transformSuspendPolicy(defaults.getSuspendPolicy()));
    }
  }

  @Nullable
  public RunToCursorBreakpoint addRunToCursorBreakpoint(Document document, int lineIndex, final boolean ignoreBreakpoints) {
    return RunToCursorBreakpoint.create(myProject, document, lineIndex, ignoreBreakpoints);
  }

  @Nullable
  public StepIntoBreakpoint addStepIntoBreakpoint(@NotNull BreakpointStepMethodFilter filter) {
    return StepIntoBreakpoint.create(myProject, filter);
  }

  @Nullable
  public LineBreakpoint addLineBreakpoint(Document document, int lineIndex) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (!LineBreakpoint.canAddLineBreakpoint(myProject, document, lineIndex)) {
      return null;
    }
    XLineBreakpoint xLineBreakpoint = addXLineBreakpoint(JavaLineBreakpointType.class, document, lineIndex);
    LineBreakpoint breakpoint = LineBreakpoint.create(myProject, xLineBreakpoint);
    if (breakpoint == null) {
      return null;
    }

    addBreakpoint(breakpoint);
    return breakpoint;
  }

  @Nullable
  public FieldBreakpoint addFieldBreakpoint(@NotNull Document document, int offset) {
    PsiField field = FieldBreakpoint.findField(myProject, document, offset);
    if (field == null) {
      return null;
    }

    int line = document.getLineNumber(offset);

    if (document.getLineNumber(field.getNameIdentifier().getTextOffset()) < line) {
      return null;
    }

    return addFieldBreakpoint(document, line, field.getName());
  }

  @Nullable
  public FieldBreakpoint addFieldBreakpoint(Document document, int lineIndex, String fieldName) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    XLineBreakpoint xBreakpoint = addXLineBreakpoint(JavaFieldBreakpointType.class, document, lineIndex);
    FieldBreakpoint fieldBreakpoint = FieldBreakpoint.create(myProject, fieldName, xBreakpoint);
    if (fieldBreakpoint != null) {
      addBreakpoint(fieldBreakpoint);
    }
    return fieldBreakpoint;
  }

  @NotNull
  public ExceptionBreakpoint addExceptionBreakpoint(@NotNull final String exceptionClassName, final String packageName) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    final JavaExceptionBreakpointType type = (JavaExceptionBreakpointType)XDebuggerUtil.getInstance().findBreakpointType(JavaExceptionBreakpointType.class);
    return ApplicationManager.getApplication().runWriteAction(new Computable<ExceptionBreakpoint>() {
      @Override
      public ExceptionBreakpoint compute() {
        XBreakpoint<JavaExceptionBreakpointProperties> xBreakpoint = XDebuggerManager.getInstance(myProject).getBreakpointManager()
          .addBreakpoint(type, new JavaExceptionBreakpointProperties(exceptionClassName, packageName));
        ExceptionBreakpoint breakpoint = new ExceptionBreakpoint(myProject, exceptionClassName, packageName, xBreakpoint);
        addBreakpoint(breakpoint);
        if (LOG.isDebugEnabled()) {
          LOG.debug("ExceptionBreakpoint Added");
        }
        return breakpoint;
      }
    });
  }

  @Nullable
  public MethodBreakpoint addMethodBreakpoint(Document document, int lineIndex) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    XLineBreakpoint xBreakpoint = addXLineBreakpoint(JavaMethodBreakpointType.class, document, lineIndex);
    MethodBreakpoint breakpoint = MethodBreakpoint.create(myProject, xBreakpoint);
    if (breakpoint == null) {
      return null;
    }

    addBreakpoint(breakpoint);
    return breakpoint;
  }

  private <B extends XBreakpoint<?>> XLineBreakpoint addXLineBreakpoint(Class<? extends XBreakpointType<B,?>> typeCls, Document document, final int lineIndex) {
    final XBreakpointType<B, ?> type = XDebuggerUtil.getInstance().findBreakpointType(typeCls);
    final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    return ApplicationManager.getApplication().runWriteAction(new Computable<XLineBreakpoint>() {
      @Override
      public XLineBreakpoint compute() {
        return XDebuggerManager.getInstance(myProject).getBreakpointManager()
          .addLineBreakpoint((XLineBreakpointType)type, file.getUrl(), lineIndex,
                             ((XLineBreakpointType)type).createBreakpointProperties(file, lineIndex));
      }
    });
  }

  /**
   * @param category breakpoint category, null if the category does not matter
   */
  @Nullable
  public <T extends BreakpointWithHighlighter> T findBreakpoint(final Document document, final int offset, @Nullable final Key<T> category) {
    for (final Breakpoint breakpoint : getBreakpoints()) {
      if (breakpoint instanceof BreakpointWithHighlighter && ((BreakpointWithHighlighter)breakpoint).isAt(document, offset)) {
        if (category == null || category.equals(breakpoint.getCategory())) {
          //noinspection CastConflictsWithInstanceof,unchecked
          return (T)breakpoint;
        }
      }
    }
    return null;
  }

  @Nullable
  public static Breakpoint findBreakpoint(@NotNull XBreakpoint xBreakpoint) {
    Project project = ((XBreakpointBase)xBreakpoint).getProject();
    BreakpointManager breakpointManager = DebuggerManagerEx.getInstanceEx(project).getBreakpointManager();
    return breakpointManager.myBreakpoints.get(xBreakpoint);
  }

  private HashMap<String, Element> myOriginalBreakpointsNodes = new HashMap<String, Element>();

  public void readExternal(@NotNull final Element parentNode) {
    myOriginalBreakpointsNodes.clear();
    // save old breakpoints
    for (Element element : parentNode.getChildren()) {
      myOriginalBreakpointsNodes.put(element.getName(), element.clone());
    }
    if (myProject.isOpen()) {
      doRead(parentNode);
    }
    else {
      myStartupManager.registerPostStartupActivity(new Runnable() {
        @Override
        public void run() {
          doRead(parentNode);
        }
      });
    }
  }

  private void doRead(@NotNull final Element parentNode) {
    ApplicationManager.getApplication().runReadAction(new Runnable() {
      @Override
      @SuppressWarnings({"HardCodedStringLiteral"})
      public void run() {
        final Map<String, Breakpoint> nameToBreakpointMap = new THashMap<String, Breakpoint>();
        try {
          final List groups = parentNode.getChildren();
          for (final Object group1 : groups) {
            final Element group = (Element)group1;
            if (group.getName().equals(RULES_GROUP_NAME)) {
              continue;
            }
            // skip already converted
            if (group.getAttribute(CONVERTED_PARAM) != null) {
              continue;
            }
            final String categoryName = group.getName();
            final Key<Breakpoint> breakpointCategory = BreakpointCategory.lookup(categoryName);
            final String defaultPolicy = group.getAttributeValue(DEFAULT_SUSPEND_POLICY_ATTRIBUTE_NAME);
            final boolean conditionEnabled = Boolean.parseBoolean(group.getAttributeValue(DEFAULT_CONDITION_STATE_ATTRIBUTE_NAME, "true"));
            setBreakpointDefaults(breakpointCategory, new BreakpointDefaults(defaultPolicy, conditionEnabled));
            Element anyExceptionBreakpointGroup;
            if (!AnyExceptionBreakpoint.ANY_EXCEPTION_BREAKPOINT.equals(breakpointCategory)) {
              // for compatibility with previous format
              anyExceptionBreakpointGroup = group.getChild(AnyExceptionBreakpoint.ANY_EXCEPTION_BREAKPOINT.toString());
              //final BreakpointFactory factory = BreakpointFactory.getInstance(breakpointCategory);
              //if (factory != null) {
                for (Element breakpointNode : group.getChildren("breakpoint")) {
                  //Breakpoint breakpoint = factory.createBreakpoint(myProject, breakpointNode);
                  Breakpoint breakpoint = createBreakpoint(categoryName, breakpointNode);
                  breakpoint.readExternal(breakpointNode);
                  nameToBreakpointMap.put(breakpoint.getDisplayName(), breakpoint);
                }
              //}
            }
            else {
              anyExceptionBreakpointGroup = group;
            }

            if (anyExceptionBreakpointGroup != null) {
              final Element breakpointElement = group.getChild("breakpoint");
              if (breakpointElement != null) {
                XBreakpointManager manager = XDebuggerManager.getInstance(myProject).getBreakpointManager();
                JavaExceptionBreakpointType type = (JavaExceptionBreakpointType)XDebuggerUtil.getInstance().findBreakpointType(JavaExceptionBreakpointType.class);
                XBreakpoint<JavaExceptionBreakpointProperties> xBreakpoint = manager.getDefaultBreakpoint(type);
                Breakpoint breakpoint = createJavaBreakpoint(xBreakpoint);
                breakpoint.readExternal(breakpointElement);
                addBreakpoint(breakpoint);
              }
            }
          }
        }
        catch (InvalidDataException ignored) {
        }

        final Element rulesGroup = parentNode.getChild(RULES_GROUP_NAME);
        if (rulesGroup != null) {
          final List<Element> rules = rulesGroup.getChildren("rule");
          for (Element rule : rules) {
            // skip already converted
            if (rule.getAttribute(CONVERTED_PARAM) != null) {
              continue;
            }
            final Element master = rule.getChild(MASTER_BREAKPOINT_TAGNAME);
            if (master == null) {
              continue;
            }
            final Element slave = rule.getChild(SLAVE_BREAKPOINT_TAGNAME);
            if (slave == null) {
              continue;
            }
            final Breakpoint masterBreakpoint = nameToBreakpointMap.get(master.getAttributeValue("name"));
            if (masterBreakpoint == null) {
              continue;
            }
            final Breakpoint slaveBreakpoint = nameToBreakpointMap.get(slave.getAttributeValue("name"));
            if (slaveBreakpoint == null) {
              continue;
            }

            boolean leaveEnabled = "true".equalsIgnoreCase(rule.getAttributeValue("leaveEnabled"));
            XDependentBreakpointManager dependentBreakpointManager = ((XBreakpointManagerImpl)getXBreakpointManager()).getDependentBreakpointManager();
            dependentBreakpointManager.setMasterBreakpoint(slaveBreakpoint.myXBreakpoint, masterBreakpoint.myXBreakpoint, leaveEnabled);
            //addBreakpointRule(new EnableBreakpointRule(BreakpointManager.this, masterBreakpoint, slaveBreakpoint, leaveEnabled));
          }
        }

        DebuggerInvocationUtil.invokeLater(myProject, new Runnable() {
          @Override
          public void run() {
            updateBreakpointsUI();
          }
        });
      }
    });

    myUIProperties.clear();
    final Element props = parentNode.getChild("ui_properties");
    if (props != null) {
      final List children = props.getChildren("property");
      for (Object child : children) {
        Element property = (Element)child;
        final String name = property.getAttributeValue("name");
        final String value = property.getAttributeValue("value");
        if (name != null && value != null) {
          myUIProperties.put(name, value);
        }
      }
    }
  }

  private Breakpoint createBreakpoint(String category, Element breakpointNode) throws InvalidDataException {
    XBreakpoint xBreakpoint = null;
    if (category.equals(LineBreakpoint.CATEGORY.toString())) {
      xBreakpoint = createXLineBreakpoint(JavaLineBreakpointType.class, breakpointNode);
    }
    else if (category.equals(MethodBreakpoint.CATEGORY.toString())) {
      if (breakpointNode.getAttribute("url") != null) {
        xBreakpoint = createXLineBreakpoint(JavaMethodBreakpointType.class, breakpointNode);
      }
      else {
        xBreakpoint = createXBreakpoint(JavaWildcardMethodBreakpointType.class, breakpointNode);
      }
    }
    else if (category.equals(FieldBreakpoint.CATEGORY.toString())) {
      xBreakpoint = createXLineBreakpoint(JavaFieldBreakpointType.class, breakpointNode);
    }
    else if (category.equals(ExceptionBreakpoint.CATEGORY.toString())) {
      xBreakpoint =  createXBreakpoint(JavaExceptionBreakpointType.class, breakpointNode);
    }
    if (xBreakpoint == null) {
      throw new IllegalStateException("Unknown breakpoint category " + category);
    }
    return myBreakpoints.get(xBreakpoint);
  }

  private <B extends XBreakpoint<?>> XBreakpoint createXBreakpoint(Class<? extends XBreakpointType<B, ?>> typeCls,
                                                                           Element breakpointNode) throws InvalidDataException {
    final XBreakpointType<B, ?> type = XDebuggerUtil.getInstance().findBreakpointType(typeCls);
    return ApplicationManager.getApplication().runWriteAction(new Computable<XBreakpoint>() {
      @Override
      public XBreakpoint compute() {
      return XDebuggerManager.getInstance(myProject).getBreakpointManager()
        .addBreakpoint((XBreakpointType)type, type.createProperties());
    }});
  }

  private <B extends XBreakpoint<?>> XLineBreakpoint createXLineBreakpoint(Class<? extends XBreakpointType<B, ?>> typeCls,
                                                                           Element breakpointNode) throws InvalidDataException {
    final String url = breakpointNode.getAttributeValue("url");
    VirtualFile vFile = VirtualFileManager.getInstance().findFileByUrl(url);
    if (vFile == null) {
      throw new InvalidDataException(DebuggerBundle.message("error.breakpoint.file.not.found", url));
    }
    final Document doc = FileDocumentManager.getInstance().getDocument(vFile);
    if (doc == null) {
      throw new InvalidDataException(DebuggerBundle.message("error.cannot.load.breakpoint.file", url));
    }

    final int line;
    try {
      //noinspection HardCodedStringLiteral
      line = Integer.parseInt(breakpointNode.getAttributeValue("line"));
    }
    catch (Exception e) {
      throw new InvalidDataException("Line number is invalid for breakpoint");
    }
    return addXLineBreakpoint(typeCls, doc, line);
  }

  public static Breakpoint addBreakpointInt(@NotNull XBreakpoint xBreakpoint) {
    Project project = ((XBreakpointBase)xBreakpoint).getProject();
    BreakpointManager breakpointManager = DebuggerManagerEx.getInstanceEx(project).getBreakpointManager();
    Breakpoint breakpoint = breakpointManager.createJavaBreakpoint(xBreakpoint);
    breakpointManager.addBreakpoint(breakpoint);
    return breakpoint;
  }

  //used in Fabrique
  public synchronized void addBreakpoint(@NotNull Breakpoint breakpoint) {
    myBreakpoints.put(breakpoint.myXBreakpoint, breakpoint);
    myBreakpointsListForIteration = null;
    breakpoint.updateUI();
    checkAndNotifyPossiblySlowBreakpoint(breakpoint.myXBreakpoint);
  }

  public void removeBreakpoint(@Nullable final Breakpoint breakpoint) {
    if (breakpoint == null) {
      return;
    }
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        getXBreakpointManager().removeBreakpoint(breakpoint.myXBreakpoint);
      }
    });
  }

  public static void removeBreakpointInt(@NotNull XBreakpoint xBreakpoint) {
    Project project = ((XBreakpointBase)xBreakpoint).getProject();
    BreakpointManager breakpointManager = DebuggerManagerEx.getInstanceEx(project).getBreakpointManager();
    breakpointManager.onBreakpointRemoved(xBreakpoint);
  }

  private synchronized void onBreakpointRemoved(@Nullable final XBreakpoint xBreakpoint) {
    //ApplicationManager.getApplication().assertIsDispatchThread();
    if (xBreakpoint == null) {
      return;
    }

    Breakpoint breakpoint = myBreakpoints.remove(xBreakpoint);
    if (breakpoint != null) {
      //updateBreakpointRules(breakpoint);
      myBreakpointsListForIteration = null;
    }
  }

  public void writeExternal(@NotNull final Element parentNode) {
    // restore old breakpoints
    for (Element group : myOriginalBreakpointsNodes.values()) {
      if (group.getAttribute(CONVERTED_PARAM) == null) {
        group.setAttribute(CONVERTED_PARAM, "true");
      }
      group.detach();
    }

    parentNode.addContent(myOriginalBreakpointsNodes.values());
  }

  @NotNull
  public synchronized List<Breakpoint> getBreakpoints() {
    if (myBreakpointsListForIteration == null) {
      myBreakpointsListForIteration = new ArrayList<Breakpoint>(myBreakpoints.size());

      ApplicationManager.getApplication().runReadAction(new Runnable() {
        @Override
        public void run() {
          for (XBreakpoint<?> xBreakpoint : getXBreakpointManager().getAllBreakpoints()) {
            if (isJavaType(xBreakpoint)) {
              Breakpoint breakpoint = myBreakpoints.get(xBreakpoint);
              if (breakpoint == null) {
                breakpoint = createJavaBreakpoint(xBreakpoint);
                myBreakpoints.put(xBreakpoint, breakpoint);
              }
            }
          }
        }
      });

    myBreakpointsListForIteration.addAll(myBreakpoints.values());
    }
    return myBreakpointsListForIteration;
  }

  private boolean isJavaType(XBreakpoint xBreakpoint) {
    return xBreakpoint.getType() instanceof JavaBreakpointType;
  }

  private Breakpoint createJavaBreakpoint(XBreakpoint xBreakpoint) {
    if (xBreakpoint.getType() instanceof JavaBreakpointType) {
      return ((JavaBreakpointType)xBreakpoint.getType()).createJavaBreakpoint(myProject, xBreakpoint);
    }
    throw new IllegalStateException("Unsupported breakpoint type:" + xBreakpoint.getType());
  }

  //interaction with RequestManagerImpl
  public void disableBreakpoints(@NotNull final DebugProcessImpl debugProcess) {
    final List<Breakpoint> breakpoints = getBreakpoints();
    if (!breakpoints.isEmpty()) {
      final RequestManagerImpl requestManager = debugProcess.getRequestsManager();
      for (Breakpoint breakpoint : breakpoints) {
        breakpoint.markVerified(requestManager.isVerified(breakpoint));
        requestManager.deleteRequest(breakpoint);
      }
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          updateBreakpointsUI();
        }
      });
    }
  }

  public void enableBreakpoints(final DebugProcessImpl debugProcess) {
    final List<Breakpoint> breakpoints = getBreakpoints();
    if (!breakpoints.isEmpty()) {
      for (Breakpoint breakpoint : breakpoints) {
        breakpoint.markVerified(false); // clean cached state
        breakpoint.createRequest(debugProcess);
      }
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          updateBreakpointsUI();
        }
      });
    }
  }

  public void applyThreadFilter(@NotNull final DebugProcessImpl debugProcess, @Nullable ThreadReference newFilterThread) {
    final RequestManagerImpl requestManager = debugProcess.getRequestsManager();
    final ThreadReference oldFilterThread = requestManager.getFilterThread();
    if (Comparing.equal(newFilterThread, oldFilterThread)) {
      // the filter already added
      return;
    }
    requestManager.setFilterThread(newFilterThread);
    if (newFilterThread == null || oldFilterThread != null) {
      final List<Breakpoint> breakpoints = getBreakpoints();
      for (Breakpoint breakpoint : breakpoints) {
        if (LineBreakpoint.CATEGORY.equals(breakpoint.getCategory()) || MethodBreakpoint.CATEGORY.equals(breakpoint.getCategory())) {
          requestManager.deleteRequest(breakpoint);
          breakpoint.createRequest(debugProcess);
        }
      }
    }
    else {
      // important! need to add filter to _existing_ requests, otherwise Requestor->Request mapping will be lost
      // and debugger trees will not be restored to original state
      abstract class FilterSetter <T extends EventRequest> {
         void applyFilter(@NotNull final List<T> requests, final ThreadReference thread) {
          for (T request : requests) {
            try {
              final boolean wasEnabled = request.isEnabled();
              if (wasEnabled) {
                request.disable();
              }
              addFilter(request, thread);
              if (wasEnabled) {
                request.enable();
              }
            }
            catch (InternalException e) {
              LOG.info(e);
            }
          }
        }
        protected abstract void addFilter(final T request, final ThreadReference thread);
      }

      final EventRequestManager eventRequestManager = requestManager.getVMRequestManager();

      new FilterSetter<BreakpointRequest>() {
        @Override
        protected void addFilter(@NotNull final BreakpointRequest request, final ThreadReference thread) {
          request.addThreadFilter(thread);
        }
      }.applyFilter(eventRequestManager.breakpointRequests(), newFilterThread);

      new FilterSetter<MethodEntryRequest>() {
        @Override
        protected void addFilter(@NotNull final MethodEntryRequest request, final ThreadReference thread) {
          request.addThreadFilter(thread);
        }
      }.applyFilter(eventRequestManager.methodEntryRequests(), newFilterThread);

      new FilterSetter<MethodExitRequest>() {
        @Override
        protected void addFilter(@NotNull final MethodExitRequest request, final ThreadReference thread) {
          request.addThreadFilter(thread);
        }
      }.applyFilter(eventRequestManager.methodExitRequests(), newFilterThread);
    }
  }

  public void updateAllRequests() {
    ApplicationManager.getApplication().assertIsDispatchThread();

    List<Breakpoint> breakpoints = getBreakpoints();
    for (Breakpoint breakpoint : breakpoints) {
      fireBreakpointChanged(breakpoint);
    }
  }

  public void updateBreakpointsUI() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    for (Breakpoint breakpoint : getBreakpoints()) {
      breakpoint.updateUI();
    }
  }

  public void reloadBreakpoints() {
    ApplicationManager.getApplication().assertIsDispatchThread();

    for (Breakpoint breakpoint : getBreakpoints()) {
      breakpoint.reload();
    }
  }

  public void fireBreakpointChanged(Breakpoint breakpoint) {
    breakpoint.reload();
    breakpoint.updateUI();
    RequestManagerImpl.updateRequests(breakpoint);
  }

  public void setBreakpointEnabled(@NotNull final Breakpoint breakpoint, final boolean enabled) {
    if (breakpoint.isEnabled() != enabled) {
      breakpoint.setEnabled(enabled);
    }
  }

  // copied from XDebugSessionImpl processDependencies
  public void processBreakpointHit(@NotNull final Breakpoint breakpoint) {
    XDependentBreakpointManager dependentBreakpointManager = ((XBreakpointManagerImpl)getXBreakpointManager()).getDependentBreakpointManager();
    XBreakpoint xBreakpoint = breakpoint.myXBreakpoint;
    if (!dependentBreakpointManager.isMasterOrSlave(xBreakpoint)) {
      return;
    }
    List<XBreakpoint<?>> breakpoints = dependentBreakpointManager.getSlaveBreakpoints(xBreakpoint);
    for (final XBreakpoint<?> slaveBreakpoint : breakpoints) {
      DebuggerInvocationUtil.invokeLater(myProject, new Runnable() {
        @Override
        public void run() {
          slaveBreakpoint.setEnabled(true);
        }
      });
    }

    if (dependentBreakpointManager.getMasterBreakpoint(xBreakpoint) != null && !dependentBreakpointManager.isLeaveEnabled(xBreakpoint)) {
      DebuggerInvocationUtil.invokeLater(myProject, new Runnable() {
        @Override
        public void run() {
          breakpoint.setEnabled(false);
        }
      });
      //myDebuggerManager.getBreakpointManager().getLineBreakpointManager().queueBreakpointUpdate(breakpoint);
    }
  }

  @Nullable
  public Breakpoint findMasterBreakpoint(@NotNull Breakpoint dependentBreakpoint) {
    XDependentBreakpointManager dependentBreakpointManager = ((XBreakpointManagerImpl)getXBreakpointManager()).getDependentBreakpointManager();
    return myBreakpoints.get(dependentBreakpointManager.getMasterBreakpoint(dependentBreakpoint.myXBreakpoint));
  }

  public String getProperty(String name) {
    return myUIProperties.get(name);
  }
  
  public String setProperty(String name, String value) {
    return myUIProperties.put(name, value);
  }
}
