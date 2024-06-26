/*
 * Class AddToWatchAction
 * @author Jeka
 */
package com.intellij.debugger.actions;

import com.intellij.debugger.DebuggerInvocationUtil;
import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.debugger.engine.events.DebuggerContextCommandImpl;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.DebuggerPanelsManager;
import com.intellij.debugger.ui.impl.MainWatchPanel;
import com.intellij.debugger.ui.impl.WatchDebuggerTree;
import com.intellij.debugger.ui.impl.FramePanel;
import com.intellij.debugger.ui.impl.watch.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.editor.Editor;

public class AddToWatchAction extends DebuggerAction {
  public void actionPerformed(AnActionEvent e) {
    final DebuggerContextImpl debuggerContext = getDebuggerContext(e.getDataContext());

    if(debuggerContext == null) return;

    final DebuggerSession session = debuggerContext.getDebuggerSession();
    if(session == null) {
      return;
    }
    final MainWatchPanel watchPanel = DebuggerPanelsManager.getInstance(debuggerContext.getProject()).getWatchPanel();

    if(watchPanel == null) {
      return;
    }

    final DebuggerTreeNodeImpl[] selectedNodes = getSelectedNodes(e.getDataContext());

    if(selectedNodes != null && selectedNodes.length > 0) {
      debuggerContext.getDebugProcess().getManagerThread().invokeLater(new AddToWatchesCommand(debuggerContext, selectedNodes, watchPanel));
    }
    else {
      final Editor editor  = (Editor)e.getDataContext().getData(DataConstants.EDITOR);
      final TextWithImports editorText = DebuggerUtilsEx.getEditorText(editor);
      if(editorText != null) {
        doAddWatch(watchPanel, editorText, null);
      }
    }
  }

  public void update(AnActionEvent e) {
    DebuggerTreeNodeImpl[] selectedNodes = getSelectedNodes(e.getDataContext());
    boolean enabled = false;
    if (selectedNodes != null && selectedNodes.length > 0) {
      if (getPanel(e.getDataContext()) instanceof FramePanel) {
        enabled = true;
        for (DebuggerTreeNodeImpl node : selectedNodes) {
          NodeDescriptorImpl descriptor = node.getDescriptor();
          if (!(descriptor instanceof ValueDescriptorImpl)) {
            enabled = false;
            break;
          }
        }
      }
    }
    else {
      final Editor editor  = (Editor)e.getDataContext().getData(DataConstants.EDITOR);
      enabled = DebuggerUtilsEx.getEditorText(editor) != null ;
    }
    e.getPresentation().setVisible(enabled);
  }

  private static void doAddWatch(final MainWatchPanel watchPanel, final TextWithImports expression, final NodeDescriptorImpl descriptor) {
    final WatchDebuggerTree watchTree = watchPanel.getWatchTree();
    final DebuggerTreeNodeImpl node = watchTree.addWatch(expression);
    if (descriptor != null) {
      node.getDescriptor().displayAs(descriptor);
    }
    node.calcValue();
  }

  private static class AddToWatchesCommand extends DebuggerContextCommandImpl {

    private final DebuggerContextImpl myDebuggerContext;
    private final DebuggerTreeNodeImpl[] mySelectedNodes;
    private final MainWatchPanel myWatchPanel;

    public AddToWatchesCommand(DebuggerContextImpl debuggerContext, DebuggerTreeNodeImpl[] selectedNodes, MainWatchPanel watchPanel) {
      super(debuggerContext);
      myDebuggerContext = debuggerContext;
      mySelectedNodes = selectedNodes;
      myWatchPanel = watchPanel;
    }

    public void threadAction() {
      for (int idx = 0; idx < mySelectedNodes.length; idx++) {
        DebuggerTreeNodeImpl node = mySelectedNodes[idx];
        final NodeDescriptorImpl descriptor = node.getDescriptor();
        final TextWithImports expression = DebuggerTreeNodeExpression.createEvaluationText(node, myDebuggerContext);
        if (expression != null) {
          DebuggerInvocationUtil.invokeLater(myDebuggerContext.getDebuggerSession().getProject(), new Runnable() {
            public void run() {
              doAddWatch(myWatchPanel, expression, descriptor);
            }
          });
        }
      }
    }

    protected void commandCancelled() {
      DebuggerInvocationUtil.invokeLater(myDebuggerContext.getProject(), new Runnable() {
        public void run() {
          for (int idx = 0; idx < mySelectedNodes.length; idx++) {
            DebuggerTreeNodeImpl node = mySelectedNodes[idx];
            final NodeDescriptorImpl descriptor = node.getDescriptor();
            if(descriptor instanceof WatchItemDescriptor) {
              final TextWithImports expression = ((WatchItemDescriptor) descriptor).getEvaluationText();
              if(expression != null) {
                doAddWatch(myWatchPanel, expression, descriptor);
              }
            }
          }
        }
      });
    }

  }
}