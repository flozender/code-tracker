package com.intellij.debugger.actions;

import com.intellij.debugger.engine.events.DebuggerContextCommandImpl;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.settings.NodeRendererSettings;
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeImpl;
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl;
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl;
import com.intellij.debugger.ui.tree.render.NodeRenderer;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * User: lex
 * Date: Sep 26, 2003
 * Time: 11:05:57 PM
 */
public class ViewAsGroup extends ActionGroup{
  private static final Logger LOG = Logger.getInstance("#com.intellij.debugger.actions.ViewAsGroup");

  private AnAction[] myChildren = AnAction.EMPTY_ARRAY;

  public ViewAsGroup() {
    super(null, true);
  }

  private static class RendererAction extends ToggleAction {
    private final NodeRenderer myNodeRenderer;

    public RendererAction(NodeRenderer nodeRenderer) {
      super(nodeRenderer.getName());
      myNodeRenderer = nodeRenderer;
    }

    public boolean isSelected(AnActionEvent e) {
      DebuggerTreeNodeImpl[] nodes = DebuggerAction.getSelectedNodes(e.getDataContext());
      for (DebuggerTreeNodeImpl node : nodes) {
        if (node.getDescriptor() instanceof ValueDescriptorImpl) {
          if (((ValueDescriptorImpl)node.getDescriptor()).getLastRenderer() != myNodeRenderer) {
            return false;
          }
        }
      }
      return true;
    }

    public void setSelected(final AnActionEvent e, final boolean state) {
      final DebuggerContextImpl debuggerContext = DebuggerAction.getDebuggerContext(e.getDataContext());
      final DebuggerTreeNodeImpl[] nodes = DebuggerAction.getSelectedNodes(e.getDataContext());

      LOG.assertTrue(debuggerContext != null && nodes != null);

      debuggerContext.getDebugProcess().getManagerThread().schedule(new DebuggerContextCommandImpl(debuggerContext) {
          public void threadAction() {
            for (final DebuggerTreeNodeImpl node : nodes) {
              if (node.getDescriptor() instanceof ValueDescriptorImpl) {
                final ValueDescriptorImpl valueDescriptor = (ValueDescriptorImpl)node.getDescriptor();
                if (state) {
                  valueDescriptor.setRenderer(myNodeRenderer);
                  node.calcRepresentation();
                }
              }
            }
          }
        });
    }
  }

  public AnAction[] getChildren(@Nullable final AnActionEvent e) {
    return myChildren;
  }

  private static AnAction [] calcChildren(DebuggerTreeNodeImpl[] nodes) {
    List<AnAction> renderers = new ArrayList<AnAction>();

    List<NodeRenderer> allRenderers = NodeRendererSettings.getInstance().getAllRenderers();

    boolean anyValueDescriptor = false;

    for (NodeRenderer nodeRenderer : allRenderers) {
      boolean allApp = true;

      for (DebuggerTreeNodeImpl node : nodes) {
        NodeDescriptorImpl descriptor = node.getDescriptor();
        if (descriptor instanceof ValueDescriptorImpl) {
          anyValueDescriptor = true;
          ValueDescriptorImpl valueDescriptor = (ValueDescriptorImpl)descriptor;
          if (valueDescriptor.isValueValid() && !nodeRenderer.isApplicable(valueDescriptor.getType())) {
            allApp = false;
            break;
          }
        }
      }

      if (!anyValueDescriptor) {
        return AnAction.EMPTY_ARRAY;
      }

      if (allApp) {
        renderers.add(new RendererAction(nodeRenderer));
      }
    }

    List<AnAction> children = new ArrayList<AnAction>();
    AnAction[] viewAsActions = ((DefaultActionGroup) ActionManager.getInstance().getAction(DebuggerActions.REPRESENTATION_LIST)).getChildren(null);
    for (AnAction viewAsAction : viewAsActions) {
      if (viewAsAction instanceof AutoRendererAction) {
        if (renderers.size() > 1) {
          viewAsAction.getTemplatePresentation().setVisible(true);
          children.add(viewAsAction);
        }
      }
      else {
        children.add(viewAsAction);
      }
    }

    children.add(Separator.getInstance());
    children.addAll(renderers);

    return children.toArray(new AnAction[children.size()]);
  }

  public void update(final AnActionEvent event) {
    if(!DebuggerAction.isFirstStart(event)) return;

    final DebuggerContextImpl debuggerContext = DebuggerAction.getDebuggerContext(event.getDataContext());
    final DebuggerTreeNodeImpl[] selectedNodes = DebuggerAction.getSelectedNodes(event.getDataContext());

    debuggerContext.getDebugProcess().getManagerThread().schedule(new DebuggerContextCommandImpl(debuggerContext) {
      public void threadAction() {
        myChildren = calcChildren(selectedNodes);
        DebuggerAction.enableAction(event, myChildren.length > 0);
      }
    });
  }
}
