/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.codeInspection.ui.actions.suppress;

import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.ui.SuppressableInspectionTreeNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbModePermission;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SequentialModalProgressTask;
import com.intellij.util.SequentialTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * @author Dmitry Batkovich
 */
public class SuppressActionSequentialTask implements SequentialTask {
  private static final Logger LOG = Logger.getInstance(SuppressActionSequentialTask.class);

  private SuppressableInspectionTreeNode[] myNodesToSuppress;
  @NotNull private final SuppressIntentionAction mySuppressAction;
  @NotNull private final InspectionToolWrapper myWrapper;
  @NotNull private final SequentialModalProgressTask myTask;
  private int myCount = 0;

  public SuppressActionSequentialTask(@NotNull SuppressableInspectionTreeNode[] nodesToSuppress,
                                      @NotNull SuppressIntentionAction suppressAction,
                                      @NotNull InspectionToolWrapper wrapper,
                                      @NotNull SequentialModalProgressTask task) {
    myNodesToSuppress = nodesToSuppress;
    mySuppressAction = suppressAction;
    myWrapper = wrapper;
    myTask = task;
  }


  @Override
  public boolean iteration() {
    final SuppressableInspectionTreeNode node = myNodesToSuppress[myCount++];
    final ProgressIndicator indicator = myTask.getIndicator();
    if (indicator != null) {
      indicator.setFraction((double)myCount / myNodesToSuppress.length);
    }

    DumbService.allowStartingDumbModeInside(DumbModePermission.MAY_START_MODAL, () -> {
      final Pair<PsiElement, CommonProblemDescriptor> content = node.getSuppressContent();
      if (content.first != null) {
        final PsiElement element = content.first;
        RefEntity refEntity = node.getElement();
        LOG.assertTrue(refEntity != null);
        if (suppress(element, content.second, mySuppressAction, refEntity, myWrapper)) {
          node.markAsSuppressedFromView();
        }
      }
    });

    return false;
  }

  @Override
  public boolean isDone() {
    return myCount > myNodesToSuppress.length - 1;
  }

  @Override
  public void stop() {
  }

  @Override
  public void prepare() {
    final ProgressIndicator indicator = myTask.getIndicator();
    if (indicator != null) {
      indicator.setText(InspectionsBundle.message("inspection.action.suppress", myWrapper.getDisplayName()));
    }
  }

  private static boolean suppress(@NotNull final PsiElement element,
                                  @Nullable final CommonProblemDescriptor descriptor,
                                  @NotNull final SuppressIntentionAction action,
                                  @NotNull final RefEntity refEntity, InspectionToolWrapper wrapper) {
    if (action instanceof SuppressIntentionActionFromFix && !(descriptor instanceof ProblemDescriptor)) {
      LOG.info("local suppression fix for specific problem descriptor:  " + wrapper.getTool().getClass().getName());
    }
    final Project project = element.getProject();
    final PsiModificationTracker tracker = PsiManager.getInstance(project).getModificationTracker();
    ApplicationManager.getApplication().runWriteAction(() -> {
      PsiDocumentManager.getInstance(project).commitAllDocuments();
      try {
        final long startModificationCount = tracker.getModificationCount();

        PsiElement container = null;
        if (action instanceof SuppressIntentionActionFromFix) {
          container = ((SuppressIntentionActionFromFix)action).getContainer(element);
        }
        if (container == null) {
          container = element;
        }

        if (action.isAvailable(project, null, element)) {
          action.invoke(project, null, element);
        }
        if (startModificationCount != tracker.getModificationCount()) {
          final Set<GlobalInspectionContextImpl> globalInspectionContexts = ((InspectionManagerEx)InspectionManager.getInstance(element.getProject())).getRunningContexts();
          for (GlobalInspectionContextImpl context : globalInspectionContexts) {
            context.ignoreElement(wrapper.getTool(), container);
            if (descriptor != null) {
              context.getPresentation(wrapper).ignoreCurrentElementProblem(refEntity, descriptor);
            }
          }
        }
      }
      catch (IncorrectOperationException e1) {
        LOG.error(e1);
      }
    });
    return true;
  }
}
