package com.intellij.openapi.wm.impl;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.FocusWatcher;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.util.containers.HashMap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
final class WindowWatcher implements PropertyChangeListener{
  private static final Logger LOG=Logger.getInstance("#com.intellij.openapi.wm.impl.WindowWatcher");
  private final Object myLock;
  private final HashMap<Window, WindowInfo> myWindow2Info;
  /**
   * Currenly focused window (window which has focused component). Can be <code>null</code> if there is no focused
   * window at all.
   */
  private Window myFocusedWindow;
  /**
   * Contains last focused window for each project.
   */
  private final HashSet myFocusedWindows;

  WindowWatcher(){
    myLock=new Object();
    myWindow2Info=new HashMap<Window, WindowInfo>();
    myFocusedWindows=new HashSet();
  }

  /**
   * This method should get notifications abount changes of focused window.
   * Only <code>focusedWindow</code> property is acceptable.
   * @throws IllegalArgumentException if property name isn't <code>focusedWindow</code>.
   */
  public final void propertyChange(final PropertyChangeEvent e){
    if(LOG.isDebugEnabled()){
      LOG.debug("enter: propertyChange("+e+")");
    }
    if(!"focusedWindow".equals(e.getPropertyName())){
      throw new IllegalArgumentException("unknown property name: "+e.getPropertyName());
    }
    synchronized(myLock){
      final Window window=(Window)e.getNewValue();
      if(window==null){
        return;
      }
      if(!myWindow2Info.containsKey(window)){
        myWindow2Info.put(window,new WindowInfo(window, true));
      }
      myFocusedWindow=window;
      final Project project = (Project)DataManager.getInstance().getDataContext(myFocusedWindow).getData(DataConstants.PROJECT);
      for (Iterator i = myFocusedWindows.iterator(); i.hasNext();) {
        final Window w = (Window)i.next();
        if (project==DataManager.getInstance().getDataContext(w).getData(DataConstants.PROJECT)) {
          i.remove();
        }
      }
      myFocusedWindows.add(myFocusedWindow);
      // Set new root frame
      final IdeFrame frame;
      if(window instanceof IdeFrame){
        frame=(IdeFrame)window;
      }else{
        frame=(IdeFrame)SwingUtilities.getAncestorOfClass(IdeFrame.class,window);
      }
      if(frame!=null){
        JOptionPane.setRootFrame(frame);
      }
    }
    if(LOG.isDebugEnabled()){
      LOG.debug("exit: propertyChange()");
    }
  }

  final void dispatchComponentEvent(final ComponentEvent e){
    final int id=e.getID();
    if(WindowEvent.WINDOW_CLOSED==id||(ComponentEvent.COMPONENT_HIDDEN==id&&e.getSource() instanceof Window)){
      dispatchHiddenOrClosed((Window)e.getSource());
    }
    // Clear obsolete reference on root frame
    if(WindowEvent.WINDOW_CLOSED==id){
      final Window window=(Window)e.getSource();
      if(JOptionPane.getRootFrame()==window){
        JOptionPane.setRootFrame(null);
      }
    }
  }

  private void dispatchHiddenOrClosed(final Window window){
    if(LOG.isDebugEnabled()){
      LOG.debug("enter: dispatchClosed("+window+")");
    }
    synchronized(myLock){
      final WindowInfo info=myWindow2Info.get(window);
      if(info!=null){
        final FocusWatcher focusWatcher=info.myFocusWatcherRef.get();
        if(focusWatcher!=null){
          focusWatcher.deinstall(window);
        }
        myWindow2Info.remove(window);
      }
    }
    // Now, we have to recalculate focused window if currently focused
    // window is being closed.
    if(myFocusedWindow==window){
      if(LOG.isDebugEnabled()){
        LOG.debug("currently active window should be closed");
      }
      myFocusedWindow=myFocusedWindow.getOwner();
      if (LOG.isDebugEnabled()) {
        LOG.debug("new active window is "+myFocusedWindow);
      }
    }
    for(Iterator i=myFocusedWindows.iterator();i.hasNext();){
      final Window activeWindow = (Window)i.next();
      if (activeWindow == window) {
        final Window newActiveWindow = activeWindow.getOwner();
        i.remove();
        if (newActiveWindow != null) {
          myFocusedWindows.add(newActiveWindow);
        }
        break;
      }
    }
    // Remove invalid infos for garbage collected windows
    for(Iterator i=myWindow2Info.values().iterator();i.hasNext();){
      final WindowInfo info=(WindowInfo)i.next();
      if(info.myFocusWatcherRef.get()==null){
        if (LOG.isDebugEnabled()) {
          LOG.debug("remove collected info");
        }
        i.remove();
      }
    }
  }

  public final Window getFocusedWindow(){
    synchronized(myLock){
      return myFocusedWindow;
    }
  }

  public final Component getFocusedComponent(final Project project){
    synchronized(myLock){
      final Window window=getFocusedWindowForProject(project);
      if(window==null){
        return null;
      }
      return getFocusedComponent(window);
    }
  }


  public final Component getFocusedComponent(final Window window){
    synchronized(myLock){
      if(window==null){
        throw new IllegalArgumentException("window is null");
      }
      final WindowInfo info=myWindow2Info.get(window);
      if(info==null){ // it means that we don't manage this window, so just return standard focus owner
        // return window.getFocusOwner();
        // TODO[vova,anton] usage of getMostRecentFocusOwner is experimental. But it seems suitable here.
        return window.getMostRecentFocusOwner();
      }
      final FocusWatcher focusWatcher=info.myFocusWatcherRef.get();
      if(focusWatcher!=null){
        final Component focusedComponent = focusWatcher.getFocusedComponent();
        if(focusedComponent != null && focusedComponent.isShowing()){
          return focusedComponent;
        }
        else{
          return null;
        }
      }else{
         // info isn't valid, i.e. window was garbage collected, so we need the remove invalid info
        // and return null
        myWindow2Info.remove(window);
        return null;
      }
    }
  }

  /**
   * @param project may be null (for example, if no projects are opened)
   */
  public final Window suggestParentWindow(final Project project){
    synchronized(myLock){
      Window window=getFocusedWindowForProject(project);
      if(window==null){
        if (project != null) {
          return WindowManagerEx.getInstanceEx().getFrame(project);
        }
        else{
          return null;
        }
      }

      LOG.assertTrue(window.isDisplayable());
      LOG.assertTrue(window.isShowing());

      while(window!=null){
        // skip all windows until found forst dialog or frame
        if(!(window instanceof Dialog)&&!(window instanceof Frame)){
          window=window.getOwner();
          continue;
        }
        // skip not visible and disposed/not shown windows
        if(!window.isDisplayable()||!window.isShowing()){
          window = window.getOwner();
          continue;
        }
        // skip windows that have not associated WindowInfo
        final WindowInfo info=myWindow2Info.get(window);
        if(info==null){
          window=window.getOwner();
          continue;
        }
        if(info.mySuggestAsParent){
          return window;
        }else{
          window=window.getOwner();
        }
      }
      return null;
    }
  }

  public final void doNotSuggestAsParent(final Window window){
    if(LOG.isDebugEnabled()){
      LOG.debug("enter: doNotSuggestAsParent("+window+")");
    }
    synchronized(myLock){
      final WindowInfo info=myWindow2Info.get(window);
      if(info==null){
        myWindow2Info.put(window,new WindowInfo(window, false));
      }else{
        info.mySuggestAsParent=false;
      }
    }
  }

  /**
   * @return active window for specified <code>project</code>. There is only one window
   * for project can be at any point of time.
   */
  private Window getFocusedWindowForProject(final Project project){
    //todo[anton,vova]: it is possible that returned wnd is not contained in myFocusedWindows; investigate
    outer: for(Iterator i=myFocusedWindows.iterator();i.hasNext();){
      Window window=(Window)i.next();
      while(!window.isDisplayable()||!window.isShowing()){ // if window isn't visible then gets its first visible ancestor
        window=window.getOwner();
        if(window==null){
          continue outer;
        }
      }
      if (project==DataManager.getInstance().getDataContext(window).getData(DataConstants.PROJECT)) {
        return window;
      }
    }
    return null;
  }

  private static final class WindowInfo {
    public final WeakReference<FocusWatcher> myFocusWatcherRef;
    public boolean mySuggestAsParent;

    public WindowInfo(final Window window,final boolean suggestAsParent){
      final FocusWatcher focusWatcher=new FocusWatcher();
      focusWatcher.install(window);
      myFocusWatcherRef=new WeakReference<FocusWatcher>(focusWatcher);
      mySuggestAsParent=suggestAsParent;
    }
  }
}
