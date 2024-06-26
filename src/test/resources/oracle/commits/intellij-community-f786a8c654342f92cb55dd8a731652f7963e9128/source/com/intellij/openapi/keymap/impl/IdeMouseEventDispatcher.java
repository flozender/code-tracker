package com.intellij.openapi.keymap.impl;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.DataConstantsEx;
import com.intellij.openapi.actionSystem.impl.PresentationFactory;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeyMapBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * TODO[vova] rewrite comments
 * Current implementation of the dispatcher is intended to filter mouse event addressed to
 * the editor. Also it allows to map middle mouse's button to some action.
 *
 * @author Vladimir Kondratyev
 */
public final class IdeMouseEventDispatcher{
  private final PresentationFactory myPresentationFactory;
  private final ArrayList myActions;

  public IdeMouseEventDispatcher(){
    myPresentationFactory=new PresentationFactory();
    myActions=new ArrayList(1);
  }

  private void fillActionsList(Component component,MouseShortcut shortcut,boolean isModalContext){
    myActions.clear();

    // here we try to find "local" shortcuts

    if(component instanceof JComponent){
      ArrayList listOfActions = (ArrayList)((JComponent)component).getClientProperty(AnAction.ourClientProperty);
      if (listOfActions != null) {
        for (int i=0; i < listOfActions.size(); i++) {
          AnAction action = (AnAction)listOfActions.get(i);
          Shortcut[] shortcuts = action.getShortcutSet().getShortcuts();
          for (int j = 0; j < shortcuts.length; j++) {
            if(shortcut.equals(shortcuts[j]) && !myActions.contains(action)){
              myActions.add(action);
            }
          }
        }
        // once we've found a proper local shortcut(s), we exit
        if (myActions.size() > 0) {
          return;
        }
      }
    }

    // search in main keymap

    Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
    String[] actionIds=keymap.getActionIds(shortcut);

    ActionManager actionManager = ActionManager.getInstance();
    for (int i = 0; i < actionIds.length; i++) {
      String actionId = actionIds[i];
      AnAction action = actionManager.getAction(actionId);
      if (action == null){
        continue;
      }
      if (isModalContext && !action.isEnabledInModalContext()){
        continue;
      }
      if (!myActions.contains(action)) {
        myActions.add(action);
      }
    }
  }

  /**
   * @return <code>true</code> if and only if the passed event is already dispatched by the
   * <code>IdeMouseEventDispatcher</code> and there is no need for any other processing of the event.
   * If the method returns <code>false</code> then it means that the event should be delivered
   * to normal event dispatching.
   */
  public boolean dispatchMouseEvent(MouseEvent e){
    if(
      e.isConsumed()||
      e.isPopupTrigger()||
      MouseEvent.MOUSE_RELEASED!=e.getID()||
      e.getClickCount()<1  ||// TODO[vova,anton] is it possible. it seems that yes! but how???
      e.getButton() == MouseEvent.NOBUTTON // See #16995. It did happen
    ){
      return false;
    }

    Component component=e.getComponent();
    if(component==null){
      throw new IllegalStateException("component cannot be null");
    }
    component=SwingUtilities.getDeepestComponentAt(component,e.getX(),e.getY());
    if(component==null){ // do nothing if component doesn't contains specified point
      return false;
    }

    MouseShortcut shortcut=new MouseShortcut(e.getButton(),e.getModifiersEx(),e.getClickCount());
    fillActionsList(component,shortcut,IdeKeyEventDispatcher.isModalContext(component));
    ActionManagerEx actionManager=ActionManagerEx.getInstanceEx();
    for(int i=0;i<myActions.size();i++){
      AnAction action=(AnAction)myActions.get(i);
      DataContext dataContext=DataManager.getInstance().getDataContext(component);
      Presentation presentation=myPresentationFactory.getPresentation(action);
      AnActionEvent actionEvent=new AnActionEvent(e,dataContext,ActionPlaces.MAIN_MENU,presentation,
                                                  ActionManager.getInstance(),
                                                  e.getModifiers());
      action.update(actionEvent);
      if(presentation.isEnabled()){
        actionManager.fireBeforeActionPerformed(action, dataContext);
        Component c = ((Component)dataContext.getData(DataConstantsEx.CONTEXT_COMPONENT));
        if (c != null && !c.isShowing()) {
          continue;
        }
        action.actionPerformed(actionEvent);
        e.consume();
      }
    }
    return false;
  }
}
