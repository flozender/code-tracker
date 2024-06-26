package com.intellij.openapi.actionSystem.impl;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.DataContext;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
final class ActionPopupMenuImpl implements ActionPopupMenu{
  private MyMenu myMenu;

  public ActionPopupMenuImpl(String place,ActionGroup group){
    myMenu=new MyMenu(place,group);
  }

  public JPopupMenu getComponent(){
    return myMenu;
  }

  private static class MyMenu extends JPopupMenu{
    private String myPlace;
    private ActionGroup myGroup;
    private DataContext myContext;
    private PresentationFactory myPresentationFactory;

    public MyMenu(String place,ActionGroup group){
      myPlace=place;
      myGroup=group;
      myPresentationFactory=new PresentationFactory();
      addPopupMenuListener(new MyPopupMenuListener());
    }

    public void show(final Component component, int x, int y) {
      if(!component.isShowing()){
        //noinspection HardCodedStringLiteral
        throw new IllegalArgumentException("component must be shown on the screen");
      }

      removeAll();

      // Fill menu. Only after filling menu has non zero size.

      int x2 = Math.min(Math.max(0, x),component.getWidth()-1); // fit x into [0, width-1]
      int y2 = Math.min(Math.max(0, y),component.getHeight()-1); // fit y into [0, height-1]

      myContext = DataManager.getInstance().getDataContext(component, x2, y2);
      Utils.fillMenu(myGroup, this, myPresentationFactory, myContext, myPlace);
      if (getComponentCount() == 0) {
        return;
      }
      Dimension preferredSize=getPreferredSize();

      // Translate (x,y) into screen coordinate syetem

      int _x,_y; // these are screen coordinates of clicked point
      Point p=component.getLocationOnScreen();
      _x=p.x+x;
      _y=p.y+y;

      // Determine graphics device which contains our point

      GraphicsConfiguration targetGraphicsConfiguration=null;
      GraphicsEnvironment env=GraphicsEnvironment.getLocalGraphicsEnvironment();
      GraphicsDevice[] devices=env.getScreenDevices();
      for(int i=0;i<devices.length;i++){
        GraphicsDevice device=devices[i];
        GraphicsConfiguration graphicsConfiguration=device.getDefaultConfiguration();
        Rectangle r=graphicsConfiguration.getBounds();
        if(r.x<=_x && _x<=r.x+r.width && r.y<=_y && _y<=r.y+r.height){
          targetGraphicsConfiguration=graphicsConfiguration;
          break;
        }
      }
      if (targetGraphicsConfiguration == null && devices.length > 0) {
        targetGraphicsConfiguration = env.getDefaultScreenDevice().getDefaultConfiguration();
      }
      if(targetGraphicsConfiguration==null){
        //noinspection HardCodedStringLiteral
        throw new IllegalStateException(
          "It's impossible to determine target graphics environment for point ("+_x+","+_y+")"
        );
      }

      // Determine real client area of target graphics configuration

      Insets insets=Toolkit.getDefaultToolkit().getScreenInsets(targetGraphicsConfiguration);
      Rectangle targetRectangle=targetGraphicsConfiguration.getBounds();
      targetRectangle.x+=insets.left;
      targetRectangle.y+=insets.top;
      targetRectangle.width-=insets.left+insets.right;
      targetRectangle.height-=insets.top+insets.bottom;

      // Fit popup into targetRectangle.
      // The algorithm is the following:
      // First of all try to move menu up on its height. If menu left-top corder
      // is inside screen bounds after that, then OK. Otherwise, if menu is too high
      // (left-top corner is outside of screen bounds) then try to move menu up on
      // not visible visible area heigh.

      if(_x+preferredSize.width>targetRectangle.x+targetRectangle.width){
          x-=preferredSize.width;
      }
      if(_y+preferredSize.height>targetRectangle.y+targetRectangle.height){
        int invisibleHeight=_y+preferredSize.height-targetRectangle.y-targetRectangle.height;
        y-=invisibleHeight;
      }

      super.show(component, x, y);
    }

    private class MyPopupMenuListener implements PopupMenuListener{
      public void popupMenuCanceled(PopupMenuEvent e){
        MyMenu.this.removeAll();
      }

      public void popupMenuWillBecomeInvisible(PopupMenuEvent e){
        MyMenu.this.removeAll();
      }

      public void popupMenuWillBecomeVisible(PopupMenuEvent e){
        MyMenu.this.removeAll();
        Utils.fillMenu(myGroup, MyMenu.this, myPresentationFactory, myContext, myPlace);
      }
    }
  }
}
