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
package com.intellij.ui;

import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.IdeTooltip;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindow;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.FocusRequestor;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeGlassPaneUtil;
import com.intellij.openapi.wm.impl.IdeGlassPaneEx;
import com.intellij.openapi.wm.impl.content.GraphicsConfig;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.Alarm;
import com.intellij.util.IJSwingUtilities;
import com.intellij.util.Range;
import com.intellij.util.containers.HashSet;
import com.intellij.util.ui.*;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class BalloonImpl implements Disposable, Balloon, LightweightWindow, PositionTracker.Client<Balloon>, IdeTooltip.Ui {

  private MyComponent myComp;
  private JLayeredPane myLayeredPane;
  private AbstractPosition myPosition;
  private Point myTargetPoint;
  private final boolean myHideOnFrameResize;

  private final Color myBorderColor;
  private final Color myFillColor;

  private final Insets myContainerInsets = new Insets(2, 2, 2, 2);

  private boolean myLastMoveWasInsideBalloon;

  private Rectangle myForcedBounds;

  private CloseButton myCloseRec;

  private final AWTEventListener myAwtActivityListener = new AWTEventListener() {
    public void eventDispatched(final AWTEvent event) {
      if (myHideOnMouse &&
          (event.getID() == MouseEvent.MOUSE_PRESSED)) {
        final MouseEvent me = (MouseEvent)event;
        if (isInsideBalloon(me))  return;

        hide();
        return;
      }

      if (myClickHandler != null && event.getID() == MouseEvent.MOUSE_CLICKED) {
        final MouseEvent me = (MouseEvent)event;
        if (!(me.getComponent() instanceof CloseButton) && isInsideBalloon(me)) {
          myClickHandler.actionPerformed(new ActionEvent(BalloonImpl.this, ActionEvent.ACTION_PERFORMED, "click", me.getModifiersEx()));
          if (myCloseOnClick) {
            hide();
            return;
          }
        }
      }

      if (myEnableCloseButton && event.getID() == MouseEvent.MOUSE_MOVED) {
        final MouseEvent me = (MouseEvent)event;
        final boolean inside = isInsideBalloon(me);
        final boolean moveChanged = inside != myLastMoveWasInsideBalloon;
        myLastMoveWasInsideBalloon = inside;
        if (moveChanged) {
          myComp.repaintButton();
        }
      }

      if (event instanceof MouseEvent && UIUtil.isCloseClick((MouseEvent)event)) {
        hide();
        return;
      }

      if (myHideOnKey && (event.getID() == KeyEvent.KEY_PRESSED)) {
        final KeyEvent ke = (KeyEvent)event;
        if (ke.getKeyCode() != KeyEvent.VK_SHIFT && ke.getKeyCode() != KeyEvent.VK_CONTROL && ke.getKeyCode() != KeyEvent.VK_ALT && ke.getKeyCode() != KeyEvent.VK_META) {
          if (SwingUtilities.isDescendingFrom(ke.getComponent(), myComp) || ke.getComponent() == myComp) return;
          hide();
        }
      }
    }
  };
  private final long myFadeoutTime;
  private Dimension myDefaultPrefSize;
  private final ActionListener myClickHandler;
  private final boolean myCloseOnClick;

  private final CopyOnWriteArraySet<JBPopupListener> myListeners = new CopyOnWriteArraySet<JBPopupListener>();
  private boolean myVisible;
  private PositionTracker<Balloon> myTracker;
  private int myAnimationCycle = 500;

  private boolean myFadedIn;
  private boolean myFadedOut;
  private int myCalloutshift;

  private int myPositionChangeXShift;
  private int myPositionChangeYShift;
  private boolean myDialogMode;
  private IdeFocusManager myFocusManager;
  private String myTitle;
  private JLabel myTitleLabel;

  private boolean myAnimationEnabled = true;

  public boolean isInsideBalloon(MouseEvent me) {
    return isInside(new RelativePoint(me));
  }

  @Override
  public boolean isInside(RelativePoint target) {
    Component cmp = target.getOriginalComponent();

    if (!cmp.isShowing()) return true;
    if (cmp == myCloseRec) return true;
    if (SwingUtilities.isDescendingFrom(cmp, myComp) || cmp == myComp) return true;
    if (!myComp.isShowing()) return false;
    if (new Rectangle(myComp.getLocationOnScreen(), myComp.getSize()).contains(target.getScreenPoint())) return true;

    return false;
  }

  private final ComponentAdapter myComponentListener = new ComponentAdapter() {
    public void componentResized(final ComponentEvent e) {
      if (myHideOnFrameResize) {
        hide();
      }
    }
  };
  private Animator myAnimator;
  private boolean myShowPointer;

  private boolean myDisposed;
  private final JComponent myContent;
  private final boolean myHideOnMouse;
  private final boolean myHideOnKey;
  private boolean myHideOnAction;
  private final boolean myEnableCloseButton;
  private final Icon myCloseButton = IconLoader.getIcon("/general/balloonClose.png");

  public BalloonImpl(JComponent content,
                     Color borderColor,
                     Color fillColor,
                     boolean hideOnMouse,
                     boolean hideOnKey,
                     boolean hideOnAction,
                     boolean showPointer,
                     boolean enableCloseButton,
                     long fadeoutTime,
                     boolean hideOnFrameResize,
                     ActionListener clickHandler,
                     boolean closeOnClick,
                     int animationCycle,
                     int calloutShift,
                     int positioChangeXShfit,
                     int positionChangeYShift,
                     boolean dialogMode,
                     String title) {
    myBorderColor = borderColor;
    myFillColor = fillColor;
    myContent = content;
    myHideOnMouse = hideOnMouse;
    myHideOnKey = hideOnKey;
    myHideOnAction = hideOnAction;
    myShowPointer = showPointer;
    myEnableCloseButton = enableCloseButton;
    myHideOnFrameResize = hideOnFrameResize;
    myClickHandler = clickHandler;
    myCloseOnClick = closeOnClick;
    myCalloutshift = calloutShift;
    myPositionChangeXShift = positioChangeXShfit;
    myPositionChangeYShift = positionChangeYShift;
    myDialogMode = dialogMode;
    myTitle = title;

    if (!myDialogMode) {
      new AwtVisitor(content) {
        @Override
        public boolean visit(Component component) {
          if (component instanceof JLabel) {
            JLabel label = (JLabel)component;
            if (label.getDisplayedMnemonic() != '\0' || label.getDisplayedMnemonicIndex() >= 0) {
              myDialogMode = true;
              return true;
            }
          } else if (component instanceof JCheckBox) {
            JCheckBox checkBox = (JCheckBox)component;
            if (checkBox.getMnemonic() >= 0 || checkBox.getDisplayedMnemonicIndex() >= 0) {
              myDialogMode = true;
              return true;
            }
          }
          return false;
        }
      };
    }

    myFadeoutTime = fadeoutTime;
    myAnimationCycle = animationCycle;
  }

  public void show(final RelativePoint target, final Balloon.Position position) {
    AbstractPosition pos = getAbstractPositionFor(position);

    show(target, pos);
  }

  private static AbstractPosition getAbstractPositionFor(Position position) {
    AbstractPosition pos = BELOW;
    switch (position) {
      case atLeft:
        pos = AT_LEFT;
        break;
      case atRight:
        pos = AT_RIGHT;
        break;
      case below:
        pos = BELOW;
        break;
      case above:
        pos = ABOVE;
        break;
    }
    return pos;
  }

  public void show(PositionTracker<Balloon> tracker, Balloon.Position position) {
    AbstractPosition pos = BELOW;
    switch (position) {
      case atLeft:
        pos = AT_LEFT;
        break;
      case atRight:
        pos = AT_RIGHT;
        break;
      case below:
        pos = BELOW;
        break;
      case above:
        pos = ABOVE;
        break;
    }

    show(tracker, pos);
  }


  private void show(RelativePoint target, AbstractPosition position) {
    show(new PositionTracker.Static<Balloon>(target), position);
  }

  private void show(PositionTracker<Balloon> tracker, AbstractPosition position) {
    assert !myDisposed : "Balloon is already disposed";

    if (isVisible()) return;
    if (!tracker.getComponent().isShowing()) return;

    myTracker = tracker;
    myTracker.init(this);

    AbstractPosition originalPreferred = position;

    JRootPane root = null;
    JDialog dialog = IJSwingUtilities.findParentOfType(tracker.getComponent(), JDialog.class);
    if (dialog != null) {
      root = dialog.getRootPane();
    } else {
      JWindow jwindow = IJSwingUtilities.findParentOfType(tracker.getComponent(), JWindow.class);
      if (jwindow != null) {
        root = jwindow.getRootPane();
      } else {
        JFrame frame = IJSwingUtilities.findParentOfType(tracker.getComponent(), JFrame.class);
        if (frame != null) {
          root = frame.getRootPane();
        } else {
          assert false;
        }
      }
    }

    myVisible = true;

    myLayeredPane = root.getLayeredPane();
    myPosition = position;
    UIUtil.setFutureRootPane(myContent, root);

    myFocusManager = IdeFocusManager.findInstanceByComponent(myLayeredPane);
    final Ref<Component> originalFocusOwner = new Ref<Component>();
    final Ref<FocusRequestor> focusRequestor = new Ref<FocusRequestor>();
    final Ref<ActionCallback> proxyFocusRequest = new Ref<ActionCallback>(new ActionCallback.Done());

    boolean mnemonicsFix = myDialogMode && SystemInfo.isMac && Registry.is("ide.mac.inplaceDialogMnemonicsFix");
    if (mnemonicsFix) {
      final IdeGlassPaneEx glassPane = (IdeGlassPaneEx)IdeGlassPaneUtil.find(myLayeredPane);
      assert glassPane != null;

      proxyFocusRequest.set(new ActionCallback());

      myFocusManager.doWhenFocusSettlesDown(new ExpirableRunnable() {
        @Override
        public boolean isExpired() {
          return isDisposed();
        }

        @Override
        public void run() {
          IdeEventQueue.getInstance().disableInputMethods(BalloonImpl.this);
          originalFocusOwner.set(myFocusManager.getFocusOwner());
          myFocusManager.requestFocus(glassPane.getProxyComponent(), true).notify(proxyFocusRequest.get());
          focusRequestor.set(myFocusManager.getFurtherRequestor());
        }
      });
    }

    myLayeredPane.addComponentListener(myComponentListener);

    myTargetPoint = myPosition.getShiftedPoint(myTracker.recalculateLocation(this).getPoint(myLayeredPane), myCalloutshift);

    int positionChangeFix = 0;
    if (myShowPointer) {
      Rectangle rec = getRecForPosition(myPosition, true);

      if (!myPosition.isOkToHavePointer(myTargetPoint, rec, getPointerLength(myPosition), getPointerWidth(myPosition), getArc(), getNormalInset())) {
        rec = getRecForPosition(myPosition, false);

        Rectangle lp = new Rectangle(new Point(myContainerInsets.left, myContainerInsets.top), myLayeredPane.getSize());
        lp.width -= myContainerInsets.right;
        lp.height -= myContainerInsets.bottom;

        if (!lp.contains(rec)) {
          Rectangle2D currentSquare = lp.createIntersection(rec);

          double maxSquare = currentSquare.getWidth() * currentSquare.getHeight();
          AbstractPosition targetPosition = myPosition;

          for (AbstractPosition eachPosition : myPosition.getOtherPositions()) {
            Rectangle2D eachIntersection = lp.createIntersection(getRecForPosition(eachPosition, false));
            double eachSquare = eachIntersection.getWidth() * eachIntersection.getHeight();
            if (maxSquare < eachSquare) {
              maxSquare = eachSquare;
              targetPosition = eachPosition;
            }
          }

          myPosition = targetPosition;
          positionChangeFix = myPosition.getChangeShift(originalPreferred, myPositionChangeXShift, myPositionChangeYShift);
        }
      }
    }

    if (myPosition != originalPreferred) {
      myTargetPoint = myPosition.getShiftedPoint(myTracker.recalculateLocation(this).getPoint(myLayeredPane), myCalloutshift > 0 ? myCalloutshift + positionChangeFix : positionChangeFix);
    }

    createComponent();

    myComp.validate();

    Rectangle rec = myComp.getBounds();

    if (myShowPointer && !myPosition.isOkToHavePointer(myTargetPoint, rec, getPointerLength(myPosition), getPointerWidth(myPosition), getArc(), getNormalInset())) {
      myShowPointer = false;
      myComp.removeAll();
      myLayeredPane.remove(myComp);

      myForcedBounds = rec;
      createComponent();
    }

    for (JBPopupListener each : myListeners) {
      each.beforeShown(new LightweightWindowEvent(this));
    }

    runAnimation(true, myLayeredPane);

    myLayeredPane.revalidate();
    myLayeredPane.repaint();



    if (mnemonicsFix) {
      proxyFocusRequest.get().doWhenDone(new Runnable() {
        @Override
        public void run() {
          myFocusManager.requestFocus(originalFocusOwner.get(), true);
        }
      });
    }

    Toolkit.getDefaultToolkit().addAWTEventListener(myAwtActivityListener, MouseEvent.MOUSE_EVENT_MASK |
                                                                           MouseEvent.MOUSE_MOTION_EVENT_MASK |
                                                                           KeyEvent.KEY_EVENT_MASK);
    if (ApplicationManager.getApplication() != null) {
      ActionManager.getInstance().addAnActionListener(new AnActionListener.Adapter() {
                                                        @Override
                                                        public void beforeActionPerformed(AnAction action,
                                                                                          DataContext dataContext,
                                                                                          AnActionEvent event) {
                                                          if (myHideOnAction) {
                                                            hide();
                                                          }
                                                        }
                                                      }, this);
    }
  }

  private Rectangle getRecForPosition(AbstractPosition position, boolean adjust) {
    Dimension size = getContentSizeFor(position);

    Rectangle rec = new Rectangle(new Point(0, 0), size);

    position.setRecToRelativePosition(rec, myTargetPoint);

    if (adjust) {
      rec = myPosition
        .getUpdatedBounds(myLayeredPane.getSize(), myForcedBounds, rec.getSize(), myShowPointer, myTargetPoint, myContainerInsets, myCalloutshift);
    }

    return rec;
  }

  private Dimension getContentSizeFor(AbstractPosition position) {
    Insets insets = position.createBorder(this).getBorderInsets();
    if (insets == null) {
      insets = new Insets(0, 0, 0, 0);
    }

    Dimension size = myContent.getPreferredSize();
    size.width += insets.left + insets.right;
    size.height += insets.top + insets.bottom;

    return size;
  }

  private void createComponent() {
    myComp = new MyComponent(myContent, this, myShowPointer
                               ? myPosition.createBorder(this)
                               : getPointlessBorder());


    myComp.clear();
    myComp.myAlpha = 0f;


    myLayeredPane.add(myComp, JLayeredPane.POPUP_LAYER);
    myPosition.updateBounds(this);
  }


  private EmptyBorder getPointlessBorder() {
    return new EmptyBorder(getNormalInset(), getNormalInset(), getNormalInset(), getNormalInset());
  }

  public void revalidate(PositionTracker<Balloon> tracker) {
    RelativePoint newPosition = tracker.recalculateLocation(this);

    if (newPosition != null) {
      myTargetPoint = myPosition.getShiftedPoint(newPosition.getPoint(myLayeredPane), myCalloutshift);
      myPosition.updateBounds(this);
    }
  }

  public void show(JLayeredPane pane) {
    show(pane, null);
  }

  @Override
  public void showInCenterOf(JComponent component) {
    final Dimension size = component.getSize();
    show(new RelativePoint(component, new Point(size.width/2, size.height/2)), Balloon.Position.above);
  }

  public void show(JLayeredPane pane, @Nullable Rectangle bounds) {
    if (bounds != null) {
      myForcedBounds = bounds;
    }
    show(new RelativePoint(pane, new Point(0, 0)), Balloon.Position.above);
  }


  private void runAnimation(boolean forward, final JLayeredPane layeredPane) {
    if (myAnimator != null) {
      Disposer.dispose(myAnimator);
    }
    myAnimator = new Animator("Balloon", 10, myAnimationEnabled ? myAnimationCycle : 0, false, 0, 1, forward) {
      public void paintNow(final float frame, final float totalFrames, final float cycle) {
        if (myComp == null || myComp.getParent() == null) return;
        myComp.setAlpha(frame / totalFrames);
      }

      @Override
      protected void paintCycleEnd() {
        if (myComp == null || myComp.getParent() == null) return;

        if (isForward()) {
          myComp.clear();
          myComp.repaint();

          myFadedIn = true;

          startFadeoutTimer();
        }
        else {
          layeredPane.remove(myComp);
          layeredPane.revalidate();
          layeredPane.repaint();
        }
        Disposer.dispose(this);
      }

      @Override
      public void dispose() {
        super.dispose();
        myAnimator = null;
      }
    };

    myAnimator.setTakInitialDelay(false);
    myAnimator.resume();
  }

  private void startFadeoutTimer() {
    if (myFadeoutTime > 0) {
      Alarm fadeoutAlarm = new Alarm(this);
      fadeoutAlarm.addRequest(new Runnable() {
        public void run() {
          hide();
        }
      }, (int)myFadeoutTime, null);
    }
  }


  int getArc() {
    return 3;
  }

  int getPointerWidth(AbstractPosition position) {
    return position.isTopBottomPointer() ? 14 : 11;
  }

  public static int getNormalInset() {
    return 3;
  }

  static int getPointerLength(AbstractPosition position) {
    return position.isTopBottomPointer() ? 10 : 8;
  }

  public static int getPointerLength(Position position) {
    return getPointerLength((getAbstractPositionFor(position)));
  }


  public void hide() {
    Disposer.dispose(this);


    for (JBPopupListener each : myListeners) {
      each.onClosed(new LightweightWindowEvent(this));
    }

    myFadedOut = true;
  }

  public void addListener(JBPopupListener listener) {
    myListeners.add(listener);
  }

  public void dispose() {
    if (myDisposed) return;

    Disposer.dispose(this);

    myDisposed = true;

    Toolkit.getDefaultToolkit().removeAWTEventListener(myAwtActivityListener);
    if (myLayeredPane != null) {
      myLayeredPane.removeComponentListener(myComponentListener);
      runAnimation(false, myLayeredPane);
    }


    myVisible = false;

    onDisposed();
  }

  protected void onDisposed() {

  }

  public boolean isVisible() {
    return myVisible;
  }

  public void setShowPointer(final boolean show) {
    myShowPointer = show;
  }

  public Icon getCloseButton() {
    return myCloseButton;
  }

  public void setBounds(Rectangle bounds) {
    myForcedBounds = bounds;
    if (myPosition != null) {
      myPosition.updateBounds(this);
    }
  }

  public Dimension getPreferredSize() {
    if (myComp != null) {
      return myComp.getPreferredSize();
    } else {
      if (myDefaultPrefSize == null) {
        final EmptyBorder border = getPointlessBorder();
        final MyComponent c = new MyComponent(myContent, this, border);
        myDefaultPrefSize = c.getPreferredSize();
      }
      return myDefaultPrefSize;
    }
  }

  public abstract static class AbstractPosition {

    abstract EmptyBorder createBorder(final BalloonImpl balloon);


    abstract void setRecToRelativePosition(Rectangle rec, Point targetPoint);

    abstract int getChangeShift(AbstractPosition original, int xShift, int yShift);

    public void updateBounds(final BalloonImpl balloon) {
      balloon.myComp._setBounds(getUpdatedBounds(balloon.myLayeredPane.getSize(),
                                                 balloon.myForcedBounds,
                                                 balloon.myComp.getPreferredSize(),
                                                 balloon.myShowPointer,
                                                 balloon.myTargetPoint,
                                                 balloon.myContainerInsets,
                                                 balloon.myCalloutshift));
    }

    public Rectangle getUpdatedBounds(Dimension layeredPaneSize,
                                      Rectangle forcedBounds,
                                      Dimension preferredSize,
                                      boolean showPointer,
                                      Point point, Insets containerInsets, int calloutShift) {

      Rectangle bounds = forcedBounds;

      if (bounds == null) {
        Point location = showPointer
                         ? getLocation(layeredPaneSize, point, preferredSize)
                         : new Point(point.x - preferredSize.width / 2, point.y - preferredSize.height / 2);
        bounds = new Rectangle(location.x, location.y, preferredSize.width, preferredSize.height);

        ScreenUtil.moveToFit(bounds, new Rectangle(0, 0, layeredPaneSize.width, layeredPaneSize.height), containerInsets);
      }

      return bounds;
    }

    abstract Point getLocation(final Dimension containerSize, final Point targetPoint, final Dimension balloonSize);

    void paintComponent(BalloonImpl balloon, final Rectangle bounds, final Graphics2D g, Point pointTarget, JComponent component) {
      final GraphicsConfig cfg = new GraphicsConfig(g);
      cfg.setAntialiasing(true);

      Shape shape;
      if (balloon.myShowPointer) {
        shape = getPointingShape(bounds, g, pointTarget, balloon);
      }
      else {
        shape = new RoundRectangle2D.Double(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1, balloon.getArc(), balloon.getArc());
      }

      g.setColor(balloon.myFillColor);
      g.fill(shape);
      g.setColor(balloon.myBorderColor);

      if (balloon.myTitleLabel != null) {
        Rectangle titleBounds = balloon.myTitleLabel.getBounds();

        Insets inset = getTitleInsets(balloon.getNormalInset() - 1, getPointerLength(this) + 50);

        titleBounds.x -= inset.left + 1;
        titleBounds.width += (inset.left + inset.right + 50);
        titleBounds.y -= inset.top + 1;
        titleBounds.height += (inset.top + inset.bottom + 1);

        Area area = new Area(shape);
        area.intersect(new Area(titleBounds));


        Color fgColor = UIManager.getColor("Label.foreground");
        fgColor = new Color(fgColor.getRed(), fgColor.getGreen(), fgColor.getBlue(), 140);
        g.setColor(fgColor);
        g.fill(area);

        g.setColor(balloon.myBorderColor);
        g.draw(area);

        //Rectangle titleBounds = balloon.myTitleLabel.getBounds();
        //titleBounds = SwingUtilities.convertRectangle(balloon.myTitleLabel.getParent(), titleBounds, component);
        //
        //g.setColor(balloon.myBorderColor);
        //int inset  = balloon.getNormalInset();
        //g.drawLine(titleBounds.x - inset, (int)titleBounds.getMaxY(), (int)titleBounds.getMaxX() + inset, (int)titleBounds.getMaxY());
      }

      g.draw(shape);
      cfg.restore();
    }

    protected abstract Insets getTitleInsets(int normalInset, int pointerLength);

    protected abstract Shape getPointingShape(final Rectangle bounds,
                                              final Graphics2D g,
                                              final Point pointTarget,
                                              final BalloonImpl balloon);

    public boolean isOkToHavePointer(Point targetPoint, Rectangle bounds, int pointerLength, int pointerWidth, int arc, int normalInset) {
      if (bounds.x < targetPoint.x && bounds.x + bounds.width > targetPoint.x && bounds.y < targetPoint.y && bounds.y + bounds.height < targetPoint.y) return false;

      Rectangle pointless = getPointlessContentRec(bounds, pointerLength);

      int size = getDistanceToTarget(pointless, targetPoint);
      if (size < pointerLength) return false;

      Range<Integer> balloonRange;
      Range<Integer> pointerRange;
      if (isTopBottomPointer()) {
        balloonRange = new Range<Integer>(bounds.x + arc, bounds.x + bounds.width - arc * 2);
        pointerRange = new Range<Integer>(targetPoint.x - pointerWidth / 2, targetPoint.x + pointerWidth / 2);
      } else {
        balloonRange = new Range<Integer>(bounds.y + arc, bounds.y + bounds.height - arc * 2);
        pointerRange = new Range<Integer>(targetPoint.y - pointerWidth / 2, targetPoint.y + pointerWidth / 2);
      }

      return balloonRange.isWithin(pointerRange.getFrom()) && balloonRange.isWithin(pointerRange.getTo());
    }

    protected abstract int getDistanceToTarget(Rectangle rectangle, Point targetPoint);

    protected boolean isTopBottomPointer() {
      return this instanceof Below || this instanceof Above;
    }

    protected abstract Rectangle getPointlessContentRec(Rectangle bounds, int pointerLength);

    public Set<AbstractPosition> getOtherPositions() {
      HashSet<AbstractPosition> all = new HashSet<AbstractPosition>();
      all.add(BELOW);
      all.add(ABOVE);
      all.add(AT_RIGHT);
      all.add(AT_LEFT);

      all.remove(this);

      return all;
    }

    public abstract Point getShiftedPoint(Point targetPoint, int shift);
  }

  public static final AbstractPosition BELOW = new Below();
  public static final AbstractPosition ABOVE = new Above();
  public static final AbstractPosition AT_RIGHT = new AtRight();
  public static final AbstractPosition AT_LEFT = new AtLeft();


  private static class Below extends AbstractPosition {


    @Override
    public Point getShiftedPoint(Point targetPoint, int shift) {
      return new Point(targetPoint.x, targetPoint.y + shift);
    }

    @Override
    int getChangeShift(AbstractPosition original, int xShift, int yShift) {
      return original == ABOVE ? yShift : 0;
    }


    @Override
    protected int getDistanceToTarget(Rectangle rectangle, Point targetPoint) {
      return rectangle.y - targetPoint.y;
    }

    @Override
    protected Rectangle getPointlessContentRec(Rectangle bounds, int pointerLength) {
      return new Rectangle(bounds.x, bounds.y + pointerLength, bounds.width, bounds.height - pointerLength);
    }

    EmptyBorder createBorder(final BalloonImpl balloon) {
      return new EmptyBorder(balloon.getPointerLength(this) + balloon.getNormalInset(), balloon.getNormalInset(), balloon.getNormalInset(), balloon.getNormalInset());
    }

    @Override
    void setRecToRelativePosition(Rectangle rec, Point targetPoint) {
      rec.setLocation(new Point(targetPoint.x - rec.width / 2, targetPoint.y));
    }

    Point getLocation(final Dimension containerSize, final Point targetPoint, final Dimension balloonSize) {
      final Point center = UIUtil.getCenterPoint(new Rectangle(targetPoint, new Dimension(0, 0)), balloonSize);
      return new Point(center.x, targetPoint.y);
    }

    @Override
    protected Insets getTitleInsets(int normalInset, int pointerLength) {
      return new Insets(pointerLength, normalInset, normalInset, normalInset);
    }

    protected void convertBoundsToContent(final Rectangle bounds, final BalloonImpl balloon) {
      bounds.y += balloon.getPointerLength(this);
      bounds.height -= balloon.getPointerLength(this) - 1;
    }

    protected Shape getPointingShape(final Rectangle bounds, final Graphics2D g, final Point pointTarget, final BalloonImpl balloon) {
      final Shaper shaper = new Shaper(balloon, bounds, pointTarget, SwingUtilities.TOP);
      shaper.line(balloon.getPointerWidth(this) / 2, balloon.getPointerLength(this)).toRightCurve().roundRightDown().toBottomCurve().roundLeftDown()
        .toLeftCurve().roundLeftUp().toTopCurve().roundUpRight()
        .lineTo(pointTarget.x - balloon.getPointerWidth(this) / 2, shaper.getCurrent().y).lineTo(pointTarget.x, pointTarget.y);
      shaper.close();

      return shaper.getShape();
    }

    protected Shape getShape(final Rectangle bounds, final Graphics2D g, final Point pointTarget, final BalloonImpl balloon) {
      bounds.y += balloon.getPointerLength(this);
      bounds.height += balloon.getPointerLength(this);
      return new RoundRectangle2D.Double(bounds.x, bounds.y, bounds.width, bounds.height, balloon.getArc(), balloon.getArc());
    }

  }

  private static class Above extends AbstractPosition {

    @Override
    public Point getShiftedPoint(Point targetPoint, int shift) {
      return new Point(targetPoint.x, targetPoint.y + shift);
    }

    @Override
    int getChangeShift(AbstractPosition original, int xShift, int yShift) {
      return original == BELOW ? -yShift : 0;
    }

    @Override
    protected int getDistanceToTarget(Rectangle rectangle, Point targetPoint) {
      return targetPoint.y - (int)rectangle.getMaxY();
    }

    @Override
    protected Rectangle getPointlessContentRec(Rectangle bounds, int pointerLength) {
      return new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height - pointerLength);
    }

    EmptyBorder createBorder(final BalloonImpl balloon) {
      return new EmptyBorder(balloon.getNormalInset(),
                             balloon.getNormalInset(),
                             balloon.getPointerLength(this),
                             balloon.getNormalInset());
    }

    @Override
    void setRecToRelativePosition(Rectangle rec, Point targetPoint) {
      rec.setLocation(targetPoint.x - rec.width / 2, targetPoint.y - rec.height);
    }

    Point getLocation(final Dimension containerSize, final Point targetPoint, final Dimension balloonSize) {
      final Point center = UIUtil.getCenterPoint(new Rectangle(targetPoint, new Dimension(0, 0)), balloonSize);
      return new Point(center.x, targetPoint.y - balloonSize.height);
    }

    @Override
    protected Insets getTitleInsets(int normalInset, int pointerLength) {
      return new Insets(normalInset, normalInset, normalInset, normalInset);
    }

    protected void convertBoundsToContent(final Rectangle bounds, final BalloonImpl balloon) {
      bounds.height -= balloon.getPointerLength(this) - 1;
    }

    protected Shape getShape(final Rectangle bounds, final Graphics2D g, final Point pointTarget, final BalloonImpl balloon) {
      bounds.y -= balloon.getPointerLength(this);
      bounds.height -= balloon.getPointerLength(this);
      return new RoundRectangle2D.Double(bounds.x, bounds.y, bounds.width, bounds.height, balloon.getArc(), balloon.getArc());
    }

    @Override
    protected Shape getPointingShape(final Rectangle bounds, final Graphics2D g, final Point pointTarget, final BalloonImpl balloon) {
      final Shaper shaper = new Shaper(balloon, bounds, pointTarget, SwingUtilities.BOTTOM);
      shaper.line(-balloon.getPointerWidth(this) / 2, -balloon.getPointerLength(this) + 1);
      shaper.toLeftCurve().roundLeftUp().toTopCurve().roundUpRight().toRightCurve().roundRightDown().toBottomCurve().line(0, 2)
        .roundLeftDown().lineTo(pointTarget.x + balloon.getPointerWidth(this) / 2, shaper.getCurrent().y).lineTo(pointTarget.x, pointTarget.y)
        .close();


      return shaper.getShape();
    }
  }

  private static class AtRight extends AbstractPosition {

    @Override
    public Point getShiftedPoint(Point targetPoint, int shift) {
      return new Point(targetPoint.x + shift, targetPoint.y);
    }

    @Override
    int getChangeShift(AbstractPosition original, int xShift, int yShift) {
      return original == AT_LEFT ? xShift : 0;
    }

    @Override
    protected int getDistanceToTarget(Rectangle rectangle, Point targetPoint) {
      return rectangle.x - targetPoint.x;
    }

    @Override
    protected Rectangle getPointlessContentRec(Rectangle bounds, int pointerLength) {
      return new Rectangle(bounds.x + pointerLength, bounds.y, bounds.width - pointerLength, bounds.height);
    }

    EmptyBorder createBorder(final BalloonImpl balloon) {
      return new EmptyBorder(balloon.getNormalInset(), balloon.getPointerLength(this) + balloon.getNormalInset(), balloon.getNormalInset(), balloon.getNormalInset());
    }

    @Override
    void setRecToRelativePosition(Rectangle rec, Point targetPoint) {
      rec.setLocation(targetPoint.x, targetPoint.y - rec.height / 2);
    }

    Point getLocation(final Dimension containerSize, final Point targetPoint, final Dimension balloonSize) {
      final Point center = UIUtil.getCenterPoint(new Rectangle(targetPoint, new Dimension(0, 0)), balloonSize);
      return new Point(targetPoint.x, center.y);
    }

    @Override
    protected Insets getTitleInsets(int normalInset, int pointerLength) {
      return new Insets(normalInset, pointerLength, normalInset, normalInset);
    }

    @Override
    protected Shape getPointingShape(final Rectangle bounds, final Graphics2D g, final Point pointTarget, final BalloonImpl balloon) {
      final Shaper shaper = new Shaper(balloon, bounds, pointTarget, SwingUtilities.LEFT);
      shaper.line(balloon.getPointerLength(this), -balloon.getPointerWidth(this) / 2).toTopCurve().roundUpRight().toRightCurve().roundRightDown()
        .toBottomCurve().roundLeftDown().toLeftCurve().roundLeftUp()
        .lineTo(shaper.getCurrent().x, pointTarget.y + balloon.getPointerWidth(this) / 2).lineTo(pointTarget.x, pointTarget.y).close();

      return shaper.getShape();
    }

    protected void convertBoundsToContent(final Rectangle bounds, final BalloonImpl balloon) {
      bounds.x += balloon.getPointerLength(this);
      bounds.width -= balloon.getPointerLength(this);
    }

    protected Shape getShape(final Rectangle bounds, final Graphics2D g, final Point pointTarget, final BalloonImpl balloon) {
      bounds.x += balloon.getPointerLength(this);
      bounds.width -= balloon.getPointerLength(this);
      return new RoundRectangle2D.Double(bounds.x, bounds.y, bounds.width, bounds.height, balloon.getArc(), balloon.getArc());
    }
  }

  private static class AtLeft extends AbstractPosition {

    @Override
    public Point getShiftedPoint(Point targetPoint, int shift) {
      return new Point(targetPoint.x - shift, targetPoint.y);
    }

    @Override
    int getChangeShift(AbstractPosition original, int xShift, int yShift) {
      return original == AT_RIGHT ? -xShift : 0;
    }


    @Override
    protected int getDistanceToTarget(Rectangle rectangle, Point targetPoint) {
      return targetPoint.x - (int)rectangle.getMaxX();
    }

    @Override
    protected Rectangle getPointlessContentRec(Rectangle bounds, int pointerLength) {
      return new Rectangle(bounds.x, bounds.y, bounds.width - pointerLength, bounds.height);
    }

    EmptyBorder createBorder(final BalloonImpl balloon) {
      return new EmptyBorder(balloon.getNormalInset(), balloon.getNormalInset(), balloon.getNormalInset(), balloon.getPointerLength(this) + balloon.getNormalInset());
    }

    @Override
    void setRecToRelativePosition(Rectangle rec, Point targetPoint) {
      rec.setLocation(targetPoint.x - rec.width, targetPoint.y - rec.height / 2);
    }

    Point getLocation(final Dimension containerSize, final Point targetPoint, final Dimension balloonSize) {
      final Point center = UIUtil.getCenterPoint(new Rectangle(targetPoint, new Dimension(0, 0)), balloonSize);
      return new Point(targetPoint.x - balloonSize.width, center.y);
    }

    @Override
    protected Insets getTitleInsets(int normalInset, int pointerLength) {
      return new Insets(normalInset, pointerLength, normalInset, normalInset);
    }

    protected void convertBoundsToContent(final Rectangle bounds, final BalloonImpl balloon) {
      bounds.width -= balloon.getPointerLength(this);
    }

    @Override
    protected Shape getPointingShape(final Rectangle bounds, final Graphics2D g, final Point pointTarget, final BalloonImpl balloon) {
      final Shaper shaper = new Shaper(balloon, bounds, pointTarget, SwingUtilities.RIGHT);
      shaper.line(-balloon.getPointerLength(this), balloon.getPointerWidth(this) / 2);
      shaper.toBottomCurve().roundLeftDown().toLeftCurve().roundLeftUp().toTopCurve().roundUpRight().toRightCurve().roundRightDown()
        .lineTo(shaper.getCurrent().x, pointTarget.y - balloon.getPointerWidth(this) / 2).lineTo(pointTarget.x, pointTarget.y).close();
      return shaper.getShape();
    }

    protected Shape getShape(final Rectangle bounds, final Graphics2D g, final Point pointTarget, final BalloonImpl balloon) {
      bounds.width -= balloon.getPointerLength(this);
      return new RoundRectangle2D.Double(bounds.x, bounds.y, bounds.width, bounds.height, balloon.getArc(), balloon.getArc());
    }
  }

  private class CloseButton extends NonOpaquePanel {

    private BaseButtonBehavior myButton;

    private CloseButton() {
      myButton = new BaseButtonBehavior(this, TimedDeadzone.NULL) {
        protected void execute(MouseEvent e) {
          if (!myEnableCloseButton) return;
          //noinspection SSBasedInspection
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              BalloonImpl.this.hide();
            }
          });
        }
      };

      if (!myEnableCloseButton) {
        setVisible(false);
      }
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);

      if (!myEnableCloseButton) return;

      if (getWidth() > 0 && myLastMoveWasInsideBalloon) {
        final boolean pressed = myButton.isPressedByMouse();
        getCloseButton().paintIcon(this, g, (pressed ? 1 : 0), (pressed ? 1 : 0));
      }
    }
  }

  private class MyComponent extends JPanel {

    private BufferedImage myImage;
    private float myAlpha;
    private final BalloonImpl myBalloon;

    private final JComponent myContent;

    private MyComponent(JComponent content, BalloonImpl balloon, EmptyBorder shapeBorder) {
      setOpaque(false);
      setLayout(null);
      myBalloon = balloon;

      setFocusCycleRoot(true);
      putClientProperty(Balloon.KEY, BalloonImpl.this);

      myContent = new JPanel(new BorderLayout(2, 2));
      Wrapper contentWrapper = new Wrapper(content);
      if (myTitle != null) {
        myTitleLabel = new JLabel(myTitle, JLabel.CENTER);
        myTitleLabel.setForeground(UIManager.getColor("List.background"));
        myTitleLabel.setBorder(new EmptyBorder(0, 4, 0, 4));
        myContent.add(myTitleLabel, BorderLayout.NORTH);
        contentWrapper.setBorder(new EmptyBorder(1, 1, 1, 1));
      }
      myContent.add(contentWrapper, BorderLayout.CENTER);
      myContent.setBorder(shapeBorder);
      myContent.setOpaque(false);

      add(myContent);

      myCloseRec = new CloseButton();
    }

    public void clear() {
      myImage = null;
      myAlpha = -1;
    }

    @Override
    public void doLayout() {
      Insets insets = getInsets();
      if (insets == null) {
        insets = new Insets(0, 0, 0, 0);
      }

      myContent.setBounds(insets.left, insets.top, getWidth() - insets.left - insets.right, getHeight() - insets.top - insets.bottom);
    }

    @Override
    public Dimension getPreferredSize() {
      return addInsets(myContent.getPreferredSize());
    }

    @Override
    public Dimension getMinimumSize() {
      return addInsets(myContent.getMinimumSize());
    }

    private Dimension addInsets(Dimension size) {
      final Insets insets = getInsets();
      if (insets != null) {
        size.width += (insets.left + insets.right);
        size.height += (insets.top + insets.bottom);
      }

      return size;
    }

    @Override
    protected void paintComponent(final Graphics g) {
      super.paintComponent(g);

      final Graphics2D g2d = (Graphics2D)g;

      final Point pointTarget = SwingUtilities.convertPoint(myLayeredPane, myBalloon.myTargetPoint, this);

      Rectangle shapeBounds = myContent.getBounds();

      if (myImage == null && myAlpha != -1) {
        myImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        myBalloon.myPosition.paintComponent(myBalloon, shapeBounds, (Graphics2D)myImage.getGraphics(), pointTarget, this);
      }

      if (myImage != null && myAlpha != -1) {
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, myAlpha));

        g2d.drawImage(myImage, 0, 0, null);
      }
      else {
        myBalloon.myPosition.paintComponent(myBalloon, shapeBounds, (Graphics2D)g, pointTarget, this);
      }
    }


    @Override
    public void removeNotify() {
      super.removeNotify();


      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          if (myCloseRec != null && myCloseRec.getParent() != null) {
            Container parent = myCloseRec.getParent();
            parent.remove(myCloseRec);
            ((JComponent)parent).revalidate();
            parent.repaint();
          }
        }
      });
    }

    public void setAlpha(float alpha) {
      myAlpha = alpha;
      paintImmediately(0, 0, getWidth(), getHeight());
    }

    public void _setBounds(Rectangle bounds) {
      super.setBounds(bounds);
      if (myCloseRec.getParent() == null && getParent() != null) {
        myLayeredPane.add(myCloseRec, JLayeredPane.DRAG_LAYER);
      }

      if (isVisible() && myCloseRec.isVisible()) {
        Rectangle lpBounds = SwingUtilities.convertRectangle(getParent(), bounds, myLayeredPane);
        lpBounds = myPosition.getPointlessContentRec(lpBounds, myBalloon.getPointerLength(myPosition));

        int iconWidth = myBalloon.myCloseButton.getIconWidth();
        int iconHeight = myBalloon.myCloseButton.getIconHeight();
        Rectangle r = new Rectangle(lpBounds.x + lpBounds.width - iconWidth + (int)(iconWidth * 0.3), lpBounds.y - (int)(iconHeight * 0.3), iconWidth, iconHeight);


        myCloseRec.setBounds(r);
      }

    }

    public void repaintButton() {
      myCloseRec.repaint();
    }
  }

  private static class Shaper {
    private final GeneralPath myPath = new GeneralPath();

    Rectangle myBounds;
    private final int myTargetSide;
    private final BalloonImpl myBalloon;

    public Shaper(BalloonImpl balloon, Rectangle bounds, Point targetPoint, int targetSide) {
      myBalloon = balloon;
      myBounds = bounds;
      myTargetSide = targetSide;
      start(targetPoint);
    }

    private void start(Point start) {
      myPath.moveTo(start.x, start.y);
    }

    public Shaper roundUpRight() {
      myPath.quadTo(getCurrent().x, getCurrent().y - myBalloon.getArc(), getCurrent().x + myBalloon.getArc(),
                    getCurrent().y - myBalloon.getArc());
      return this;
    }

    public Shaper roundRightDown() {
      myPath.quadTo(getCurrent().x + myBalloon.getArc(), getCurrent().y, getCurrent().x + myBalloon.getArc(),
                    getCurrent().y + myBalloon.getArc());
      return this;
    }

    public Shaper roundLeftUp() {
      myPath.quadTo(getCurrent().x - myBalloon.getArc(), getCurrent().y, getCurrent().x - myBalloon.getArc(),
                    getCurrent().y - myBalloon.getArc());
      return this;
    }

    public Shaper roundLeftDown() {
      myPath.quadTo(getCurrent().x, getCurrent().y + myBalloon.getArc(), getCurrent().x - myBalloon.getArc(),
                    getCurrent().y + myBalloon.getArc());
      return this;
    }

    public Point getCurrent() {
      return new Point((int)myPath.getCurrentPoint().getX(), (int)myPath.getCurrentPoint().getY());
    }

    public Shaper line(final int deltaX, final int deltaY) {
      myPath.lineTo(getCurrent().x + deltaX, getCurrent().y + deltaY);
      return this;
    }

    public Shaper lineTo(final int x, final int y) {
      myPath.lineTo(x, y);
      return this;
    }


    private int getTargetDelta(int effectiveSide) {
      return effectiveSide == myTargetSide ? myBalloon.getPointerLength(myBalloon.myPosition) : 0;
    }

    public Shaper toRightCurve() {
      myPath.lineTo((int)myBounds.getMaxX() - myBalloon.getArc() - getTargetDelta(SwingUtilities.RIGHT) - 1, getCurrent().y);
      return this;
    }

    public Shaper toBottomCurve() {
      myPath.lineTo(getCurrent().x, (int)myBounds.getMaxY() - myBalloon.getArc() - getTargetDelta(SwingUtilities.BOTTOM) - 1);
      return this;
    }

    public Shaper toLeftCurve() {
      myPath.lineTo((int)myBounds.getX() + myBalloon.getArc() + getTargetDelta(SwingUtilities.LEFT), getCurrent().y);
      return this;
    }

    public Shaper toTopCurve() {
      myPath.lineTo(getCurrent().x, (int)myBounds.getY() + myBalloon.getArc() + getTargetDelta(SwingUtilities.TOP));
      return this;
    }

    public void close() {
      myPath.closePath();
    }

    public Shape getShape() {
      return myPath;
    }
  }

  public static void main(String[] args) {
    IconLoader.activate();

    final JFrame frame = new JFrame();
    frame.getContentPane().setLayout(new BorderLayout());
    final JPanel content = new JPanel(new BorderLayout());
    frame.getContentPane().add(content, BorderLayout.CENTER);


    final JTree tree = new Tree();
    content.add(tree);


    final Ref<BalloonImpl> balloon = new Ref<BalloonImpl>();

    tree.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(final MouseEvent e) {
        if (balloon.get() != null && balloon.get().isVisible()) {
          balloon.get().dispose();
        }
        else {
          //JLabel pane1 = new JLabel("Hello, world!");
          //JLabel pane2 = new JLabel("Hello, again");
          //JPanel pane = new JPanel(new BorderLayout());
          //pane.add(pane1, BorderLayout.CENTER);
          //pane.add(pane2, BorderLayout.SOUTH);

          //pane.setBorder(new LineBorder(Color.blue));

          balloon.set(new BalloonImpl(new JLabel("FUCK"), Color.black, MessageType.ERROR.getPopupBackground(), true, true, true, true, true, 0, true, null, false, 500, 5, 0, 0, false, "This is the title"));
          balloon.get().setShowPointer(true);

          if (e.isShiftDown()) {
            balloon.get().show(new RelativePoint(e), BalloonImpl.ABOVE);
          }
          else if (e.isAltDown()) {
            balloon.get().show(new RelativePoint(e), BalloonImpl.BELOW);
          }
          else if (e.isMetaDown()) {
            balloon.get().show(new RelativePoint(e), BalloonImpl.AT_LEFT);
          }
          else {
            balloon.get().show(new RelativePoint(e), BalloonImpl.AT_RIGHT);
          }
        }
      }
    });

    tree.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        System.out.println(e.getPoint());
      }
    });

    frame.setBounds(300, 300, 300, 300);
    frame.show();
  }

  @Override
  public boolean wasFadedIn() {
    return myFadedIn;
  }

  @Override
  public boolean wasFadedOut() {
    return myFadedOut;
  }

  @Override
  public boolean isDisposed() {
    return myDisposed;
  }

  @Override
  public void setTitle(String title) {
    myTitleLabel.setText(title);
  }

  @Override
  public RelativePoint getShowingPoint() {
    Point p = myPosition.getShiftedPoint(myTargetPoint, myCalloutshift * -1);
    return new RelativePoint(myLayeredPane, p);
  }

  @Override
  public void setAnimationEnabled(boolean enabled) {
    myAnimationEnabled = enabled;
  }
}
