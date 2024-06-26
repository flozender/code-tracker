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
 * Class FieldBreakpoint
 * @author Jeka
 */
package com.intellij.debugger.ui.breakpoints;

import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.VirtualMachineProxy;
import com.intellij.debugger.engine.requests.RequestManagerImpl;
import com.intellij.debugger.impl.PositionUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.sun.jdi.*;
import com.sun.jdi.event.AccessWatchpointEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.ModificationWatchpointEvent;
import com.sun.jdi.request.AccessWatchpointRequest;
import com.sun.jdi.request.ModificationWatchpointRequest;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.debugger.breakpoints.properties.JavaFieldBreakpointProperties;

import javax.swing.*;
import java.util.List;

public class FieldBreakpoint extends BreakpointWithHighlighter<JavaFieldBreakpointProperties> {
  private static final Logger LOG = Logger.getInstance("#com.intellij.debugger.ui.breakpoints.FieldBreakpoint");
  private boolean myIsStatic;

  @NonNls public static final Key<FieldBreakpoint> CATEGORY = BreakpointCategory.lookup("field_breakpoints");

  protected FieldBreakpoint(Project project, XBreakpoint breakpoint) {
    super(project, breakpoint);
  }

  private FieldBreakpoint(Project project, RangeHighlighter highlighter, @NotNull String fieldName, XBreakpoint breakpoint) {
    super(project, highlighter, breakpoint);
    setFieldName(fieldName);
  }

  public boolean isStatic() {
    return myIsStatic;
  }

  public String getFieldName() {
    return getProperties().myFieldName;
  }

  @Override
  protected Icon getDisabledIcon(boolean isMuted) {
    final Breakpoint master = DebuggerManagerEx.getInstanceEx(myProject).getBreakpointManager().findMasterBreakpoint(this);
    if (isMuted) {
      return master == null? AllIcons.Debugger.Db_muted_disabled_field_breakpoint : AllIcons.Debugger.Db_muted_dep_field_breakpoint;
    }
    else {
      return master == null? AllIcons.Debugger.Db_disabled_field_breakpoint : AllIcons.Debugger.Db_dep_field_breakpoint;
    }
  }

  @Override
  protected Icon getSetIcon(boolean isMuted) {
    return isMuted? AllIcons.Debugger.Db_muted_field_breakpoint : AllIcons.Debugger.Db_field_breakpoint;
  }

  @Override
  protected Icon getInvalidIcon(boolean isMuted) {
    return isMuted? AllIcons.Debugger.Db_muted_invalid_field_breakpoint : AllIcons.Debugger.Db_invalid_field_breakpoint;
  }

  @Override
  protected Icon getVerifiedIcon(boolean isMuted) {
    return isMuted? AllIcons.Debugger.Db_muted_verified_field_breakpoint : AllIcons.Debugger.Db_verified_field_breakpoint;
  }

  @Override
  protected Icon getVerifiedWarningsIcon(boolean isMuted) {
    return isMuted? AllIcons.Debugger.Db_muted_field_warning_breakpoint : AllIcons.Debugger.Db_field_warning_breakpoint;
  }

  @Override
  public Key<FieldBreakpoint> getCategory() {
    return CATEGORY;
  }

  public PsiField getPsiField() {
    final SourcePosition sourcePosition = getSourcePosition();
    final PsiField field = ApplicationManager.getApplication().runReadAction(new Computable<PsiField>() {
      @Override
      public PsiField compute() {
        final PsiClass psiClass = getPsiClassAt(sourcePosition);
        return psiClass != null ? psiClass.findFieldByName(getFieldName(), true) : null;
      }
    });
    if (field != null) {
      return field;
    }
    return PositionUtil.getPsiElementAt(getProject(), PsiField.class, sourcePosition);
  }

  @Override
  protected void reload(PsiFile psiFile) {
    super.reload(psiFile);
    PsiField field = PositionUtil.getPsiElementAt(getProject(), PsiField.class, getSourcePosition());
    if(field != null) {
      setFieldName(field.getName());
      myIsStatic = field.hasModifierProperty(PsiModifier.STATIC);
    }
    if (myIsStatic) {
      setInstanceFiltersEnabled(false);
    }
  }

  @Override
  public boolean moveTo(@NotNull SourcePosition position) {
    final PsiField field = PositionUtil.getPsiElementAt(getProject(), PsiField.class, position);
    return field != null && super.moveTo(SourcePosition.createFromElement(field));
  }

  @Override
  protected ObjectReference getThisObject(SuspendContextImpl context, LocatableEvent event) throws EvaluateException {
    if (event instanceof ModificationWatchpointEvent) {
      ModificationWatchpointEvent modificationEvent = (ModificationWatchpointEvent)event;
      ObjectReference reference = modificationEvent.object();
      if (reference != null) {  // non-static
        return reference;
      }
    }
    else if (event instanceof AccessWatchpointEvent) {
      AccessWatchpointEvent accessEvent = (AccessWatchpointEvent)event;
      ObjectReference reference = accessEvent.object();
      if (reference != null) { // non-static
        return reference;
      }
    }

    return super.getThisObject(context, event);
  }

  @Override
  public void createRequestForPreparedClass(DebugProcessImpl debugProcess,
                                            ReferenceType refType) {
    VirtualMachineProxy vm = debugProcess.getVirtualMachineProxy();
    try {
      Field field = refType.fieldByName(getFieldName());
      if (field == null) {
        debugProcess.getRequestsManager().setInvalid(this, DebuggerBundle.message("error.invalid.breakpoint.missing.field.in.class",
                                                                                  getFieldName(), refType.name()));
        return;
      }
      RequestManagerImpl manager = debugProcess.getRequestsManager();
      if (isWATCH_MODIFICATION() && vm.canWatchFieldModification()) {
        ModificationWatchpointRequest request = manager.createModificationWatchpointRequest(this, field);
        debugProcess.getRequestsManager().enableRequest(request);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Modification request added");
        }
      }
      if (isWATCH_ACCESS() && vm.canWatchFieldAccess()) {
        AccessWatchpointRequest request = manager.createAccessWatchpointRequest(this, field);
        debugProcess.getRequestsManager().enableRequest(request);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Access request added field = "+field.name() + "; refType = "+refType.name());
        }
      }
    }
    catch (ObjectCollectedException ex) {
      LOG.debug(ex);
    }
    catch (Exception ex) {
      LOG.debug(ex);
    }
  }

  @Override
  public String getEventMessage(final LocatableEvent event) {
    final Location location = event.location();
    final String locationQName = location.declaringType().name() + "." + location.method().name();
    String locationFileName;
    try {
      locationFileName = location.sourceName();
    }
    catch (AbsentInformationException e) {
      locationFileName = getSourcePosition().getFile().getName();
    }
    final int locationLine = location.lineNumber();

    if (event instanceof ModificationWatchpointEvent) {
      final ModificationWatchpointEvent modificationEvent = (ModificationWatchpointEvent)event;
      final ObjectReference object = modificationEvent.object();
      final Field field = modificationEvent.field();
      if (object != null) {
        return DebuggerBundle.message(
          "status.field.watchpoint.reached.modification", 
          field.declaringType().name(), 
          field.name(), 
          modificationEvent.valueCurrent(), 
          modificationEvent.valueToBe(),
          locationQName,
          locationFileName,
          locationLine,
          object.uniqueID()
        );
      }
      return DebuggerBundle.message(
        "status.static.field.watchpoint.reached.modification", 
        field.declaringType().name(), 
        field.name(), 
        modificationEvent.valueCurrent(), 
        modificationEvent.valueToBe(),
        locationQName,
        locationFileName,
        locationLine
      );
    }
    if (event instanceof AccessWatchpointEvent) {
      AccessWatchpointEvent accessEvent = (AccessWatchpointEvent)event;
      final ObjectReference object = accessEvent.object();
      final Field field = accessEvent.field();
      if (object != null) {
        return DebuggerBundle.message(
          "status.field.watchpoint.reached.access", 
          field.declaringType().name(), 
          field.name(), 
          locationQName,
          locationFileName,
          locationLine,
          object.uniqueID()
        );
      }
      return DebuggerBundle.message(
        "status.static.field.watchpoint.reached.access", 
        field.declaringType().name(), 
        field.name(),
        locationQName,
        locationFileName,
        locationLine
      );
    }
    return null;
  }

  @Override
  public String getDisplayName() {
    if(!isValid()) {
      return DebuggerBundle.message("status.breakpoint.invalid");
    }
    final String className = getClassName();
    return className != null && !className.isEmpty() ? className + "." + getFieldName() : getFieldName();
  }

  public static FieldBreakpoint create(@NotNull Project project, @NotNull Document document, int lineIndex, String fieldName, XBreakpoint xBreakpoint) {
    FieldBreakpoint breakpoint = new FieldBreakpoint(project, createHighlighter(project, document, lineIndex), fieldName, xBreakpoint);
    return (FieldBreakpoint)breakpoint.init();
  }

  @Override
  public boolean canMoveTo(final SourcePosition position) {
    return super.canMoveTo(position) && PositionUtil.getPsiElementAt(getProject(), PsiField.class, position) != null;
  }

  @Override
  public boolean isValid() {
    return super.isValid() && getPsiField() != null;
  }

  @Override
  public boolean isAt(@NotNull Document document, int offset) {
    PsiField field = findField(myProject, document, offset);
    return field == getPsiField();
  }

  protected static FieldBreakpoint create(@NotNull Project project, @NotNull Field field, ObjectReference object, XBreakpoint xBreakpoint) {
    String fieldName = field.name();
    int line = 0;
    Document document = null;
    try {
      List locations = field.declaringType().allLineLocations();
      if(!locations.isEmpty()) {
        Location location = (Location)locations.get(0);
        line = location.lineNumber();
        VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(location.sourcePath());
        if(file != null) {
          PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
          if(psiFile != null) {
            document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
          }
        }
      }
    }
    catch (AbsentInformationException e) {
      LOG.debug(e);
    }
    catch (InternalError e) {
      LOG.debug(e);
    }

    if(document == null) return null;

    FieldBreakpoint fieldBreakpoint = new FieldBreakpoint(project, createHighlighter(project, document, line), fieldName, xBreakpoint);
    if (!fieldBreakpoint.isStatic()) {
      fieldBreakpoint.addInstanceFilter(object.uniqueID());
    }
    return (FieldBreakpoint)fieldBreakpoint.init();
  }

  public static PsiField findField(Project project, Document document, int offset) {
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if(file == null) return null;
    offset = CharArrayUtil.shiftForward(document.getCharsSequence(), offset, " \t");
    PsiElement element = file.findElementAt(offset);
    if(element == null) return null;
    PsiField field = PsiTreeUtil.getParentOfType(element, PsiField.class, false);
    int line = document.getLineNumber(offset);
    if(field == null) {
      final PsiField[] fld = {null};
      XDebuggerUtil.getInstance().iterateLine(project, document, line, new Processor<PsiElement>() {
        @Override
        public boolean process(PsiElement element) {
          PsiField field = PsiTreeUtil.getParentOfType(element, PsiField.class, false);
          if(field != null) {
            fld[0] = field;
            return false;
          }
          return true;
        }
      });
      field = fld[0];
    }

    return field;
  }

  //@Override
  //public void readExternal(@NotNull Element breakpointNode) throws InvalidDataException {
  //  super.readExternal(breakpointNode);
  //  //noinspection HardCodedStringLiteral
  //  setFieldName(breakpointNode.getAttributeValue("field_name"));
  //  if(getFieldName() == null) {
  //    throw new InvalidDataException("No field name for field breakpoint");
  //  }
  //}
  //
  //@Override
  //@SuppressWarnings({"HardCodedStringLiteral"})
  //public void writeExternal(@NotNull Element parentNode) throws WriteExternalException {
  //  super.writeExternal(parentNode);
  //  parentNode.setAttribute("field_name", getFieldName());
  //}

  @Override
  public PsiElement getEvaluationElement() {
    return getPsiClass();
  }

  public boolean isWATCH_MODIFICATION() {
    return getProperties().WATCH_MODIFICATION;
  }

  public boolean isWATCH_ACCESS() {
    return getProperties().WATCH_ACCESS;
  }

  public void setFieldName(String fieldName) {
    getProperties().myFieldName = fieldName;
  }
}
