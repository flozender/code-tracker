/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.ui.popup;

import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.UiActivity;
import com.intellij.ide.UiActivityMonitor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl;
import com.intellij.ui.*;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.speedSearch.SpeedSearch;
import com.intellij.util.Alarm;
import com.intellij.util.BooleanFunction;
import com.intellij.util.Processor;
import com.intellij.util.ui.ChildFocusWatcher;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AbstractPopup implements JBPopup {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ui.popup.AbstractPopup");

  private static final Icon   ourMacCorner        = AllIcons.General.MacCorner;
  private static final Object SUPPRESS_MAC_CORNER = new Object();
  @NonNls public static final  String SHOW_HINTS          = "ShowHints";

  private PopupComponent myPopup;
  private MyContentPanel myContent;
  private JComponent     myPreferredFocusedComponent;
  private boolean        myRequestFocus;
  private boolean        myFocusable;
  private boolean        myForcedHeavyweight;
  private boolean        myLocateWithinScreen;
  private boolean myResizable = false;
  private JPanel myHeaderPanel;
  private CaptionPanel myCaption = null;
  private JComponent myComponent;
  private String              myDimensionServiceKey = null;
  private Computable<Boolean> myCallBack            = null;
  private Project              myProject;
  private boolean              myCancelOnClickOutside;
  private Set<JBPopupListener> myListeners;
  private boolean              myUseDimServiceForXYLocation;
  private MouseChecker         myCancelOnMouseOutCallback;
  private Canceller            myMouseOutCanceller;
  private boolean              myCancelOnWindow;
  private boolean myCancelOnWindowDeactivation = true;
  private   Dimension         myForcedSize;
  private   Point             myForcedLocation;
  private   ChildFocusWatcher myFocusWatcher;
  private   boolean           myCancelKeyEnabled;
  private   boolean           myLocateByContent;
  protected FocusTrackback    myFocusTrackback;
  private   Dimension         myMinSize;
  private   ArrayList<Object> myUserData;
  private   boolean           myShadowed;
  private   boolean           myPaintShadow;

  private float myAlpha     = 0;
  private float myLastAlpha = 0;

  private MaskProvider myMaskProvider;

  private Window           myWindow;
  private boolean          myInStack;
  private MyWindowListener myWindowListener;

  private boolean myModalContext;

  private   Component[] myFocusOwners;
  private   PopupBorder myPopupBorder;
  private   Dimension   myRestoreWindowSize;
  protected Component   myOwner;
  protected Component   myRequestorComponent;
  private   boolean     myHeaderAlwaysFocusable;
  private   boolean     myMovable;
  private   JComponent  myHeaderComponent;

  protected InputEvent myDisposeEvent;

  private Runnable myFinalRunnable;
  @Nullable private BooleanFunction<KeyEvent> myKeyEventHandler;

  protected boolean myOk;

  protected final SpeedSearch mySpeedSearch = new SpeedSearch() {
    boolean searchFieldShown = false;

    @Override
    public void update() {
      mySpeedSearchPatternField.setBackground(new JTextField().getBackground());
      onSpeedSearchPatternChanged();
      mySpeedSearchPatternField.setText(getFilter());
      if (isHoldingFilter() && !searchFieldShown) {
        setHeaderComponent(mySpeedSearchPatternField);
        searchFieldShown = true;
      }
      else if (!isHoldingFilter() && searchFieldShown) {
        setHeaderComponent(null);
        searchFieldShown = false;
      }
    }

    @Override
    public void noHits() {
      mySpeedSearchPatternField.setBackground(LightColors.RED);
    }
  };

  private JTextField mySpeedSearchPatternField;
  private boolean myNativePopup;
  private boolean myMayBeParent;
  private AbstractPopup.SpeedSearchKeyListener mySearchKeyListener;
  private JLabel myAdComponent;
  private boolean myDisposed;

  private UiActivity myActivityKey;
  private Disposable myProjectDisposable;



  AbstractPopup() {
  }

  AbstractPopup init(final Project project,
                     @NotNull final JComponent component,
                     @Nullable final JComponent preferredFocusedComponent,
                     final boolean requestFocus,
                     final boolean focusable,
                     final boolean forceHeavyweight,
                     final boolean movable,
                     final String dimensionServiceKey,
                     final boolean resizable,
                     @Nullable final String caption,
                     @Nullable final Computable<Boolean> callback,
                     final boolean cancelOnClickOutside,
                     @Nullable final Set<JBPopupListener> listeners,
                     final boolean useDimServiceForXYLocation,
                     ActiveComponent commandButton,
                     @Nullable final IconButton cancelButton,
                     @Nullable final MouseChecker cancelOnMouseOutCallback,
                     final boolean cancelOnWindow,
                     @Nullable final ActiveIcon titleIcon,
                     final boolean cancelKeyEnabled,
                     final boolean locateBycontent,
                     final boolean placeWithinScreenBounds,
                     @Nullable final Dimension minSize,
                     float alpha,
                     @Nullable MaskProvider maskProvider,
                     boolean inStack,
                     boolean modalContext,
                     @Nullable Component[] focusOwners,
                     @Nullable String adText,
                     int adTextAlignment,
                     final boolean headerAlwaysFocusable,
                     @NotNull List<Pair<ActionListener, KeyStroke>> keyboardActions,
                     Component settingsButtons,
                     @Nullable final Processor<JBPopup> pinCallback,
                     boolean mayBeParent,
                     boolean showShadow,
                     boolean cancelOnWindowDeactivation,
                     @Nullable BooleanFunction<KeyEvent> keyEventHandler)
  {
    if (requestFocus && !focusable) {
      assert false : "Incorrect argument combination: requestFocus=" + requestFocus + " focusable=" + focusable;
    }

    myActivityKey = new UiActivity.Focus("Popup:" + this);
    myProject = project;
    myComponent = component;
    myPopupBorder = PopupBorder.Factory.create(true, showShadow);
    myShadowed = showShadow;
    myPaintShadow = showShadow && !SystemInfo.isMac && !movable && !resizable && Registry.is("ide.popup.dropShadow");
    myContent = createContentPanel(resizable, myPopupBorder, isToDrawMacCorner() && resizable);
    myMayBeParent = mayBeParent;
    myCancelOnWindowDeactivation = cancelOnWindowDeactivation;

    myContent.add(component, BorderLayout.CENTER);
    if (adText != null) {
      setAdText(adText, adTextAlignment);
    }

    myCancelKeyEnabled = cancelKeyEnabled;
    myLocateByContent = locateBycontent;
    myLocateWithinScreen = placeWithinScreenBounds;
    myAlpha = alpha;
    myMaskProvider = maskProvider;
    myInStack = inStack;
    myModalContext = modalContext;
    myFocusOwners = focusOwners;
    myHeaderAlwaysFocusable = headerAlwaysFocusable;
    myMovable = movable;

    ActiveIcon actualIcon = titleIcon == null ? new ActiveIcon(EmptyIcon.ICON_0) : titleIcon;

    myHeaderPanel = new JPanel(new BorderLayout());

    if (caption != null) {
      if (!caption.isEmpty()) {
        myCaption = new TitlePanel(actualIcon.getRegular(), actualIcon.getInactive());
        ((TitlePanel)myCaption).setText(caption);
      }
      else {
        myCaption = new CaptionPanel();
      }

      if (pinCallback != null) {
        myCaption.setButtonComponent(new InplaceButton(new IconButton("Pin", AllIcons.General.AutohideOff,
                                                                      AllIcons.General.AutohideOff,
                                                                      AllIcons.General.AutohideOffInactive),
                                                       new ActionListener() {
                                                         @Override
                                                         public void actionPerformed(final ActionEvent e) {
                                                           pinCallback.process(AbstractPopup.this);
                                                         }
                                                       }));
      }
      else if (cancelButton != null) {
        myCaption.setButtonComponent(new InplaceButton(cancelButton, new ActionListener() {
          @Override
          public void actionPerformed(final ActionEvent e) {
            cancel();
          }
        }));
      }
      else if (commandButton != null) {
        myCaption.setButtonComponent(commandButton);
      }
    }
    else {
      myCaption = new CaptionPanel();
      myCaption.setBorder(null);
      myCaption.setPreferredSize(new Dimension(0, 0));
    }

    setWindowActive(myHeaderAlwaysFocusable);

    myHeaderPanel.add(myCaption, BorderLayout.NORTH);
    myContent.add(myHeaderPanel, BorderLayout.NORTH);

    myForcedHeavyweight = true;
    myResizable = resizable;
    myPreferredFocusedComponent = preferredFocusedComponent;
    myRequestFocus = requestFocus;
    myFocusable = focusable;
    myDimensionServiceKey = dimensionServiceKey;
    myCallBack = callback;
    myCancelOnClickOutside = cancelOnClickOutside;
    myCancelOnMouseOutCallback = cancelOnMouseOutCallback;
    myListeners = listeners == null ? new HashSet<JBPopupListener>() : listeners;
    myUseDimServiceForXYLocation = useDimServiceForXYLocation;
    myCancelOnWindow = cancelOnWindow;
    myMinSize = minSize;

    for (Pair<ActionListener, KeyStroke> pair : keyboardActions) {
      myContent.registerKeyboardAction(pair.getFirst(), pair.getSecond(), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    if (settingsButtons != null) {
      myCaption.addSettingsComponent(settingsButtons);
    }
    
    myKeyEventHandler = keyEventHandler;
    return this;
  }

  private void setWindowActive(boolean active) {
    boolean value = myHeaderAlwaysFocusable || active;

    if (myCaption != null) {
      myCaption.setActive(value);
    }
    myPopupBorder.setActive(value);
    myContent.repaint();
  }


  @NotNull
  protected MyContentPanel createContentPanel(final boolean resizable, PopupBorder border, boolean isToDrawMacCorner) {
    return new MyContentPanel(resizable, border, isToDrawMacCorner);
  }

  public boolean isToDrawMacCorner() {
    if (!SystemInfo.isMac || myComponent.getComponentCount() <= 0) {
      return false;
    }

    if (myComponent.getComponentCount() > 0) {
      Component component = myComponent.getComponent(0);
      if (component instanceof JComponent && Boolean.TRUE.equals(((JComponent)component).getClientProperty(SUPPRESS_MAC_CORNER))) {
        return false;
      }
    }

    return true;
  }

  public void setShowHints(boolean show) {
    final Window ancestor = SwingUtilities.getWindowAncestor(myComponent);
    if (ancestor instanceof RootPaneContainer) {
      final JRootPane rootPane = ((RootPaneContainer)ancestor).getRootPane();
      if (rootPane != null) {
        rootPane.putClientProperty(SHOW_HINTS, Boolean.valueOf(show));
      }
    }
  }

  public static void suppressMacCornerFor(JComponent popupComponent) {
    popupComponent.putClientProperty(SUPPRESS_MAC_CORNER, Boolean.TRUE);
  }


  public String getDimensionServiceKey() {
    return myDimensionServiceKey;
  }

  public void setDimensionServiceKey(@Nullable final String dimensionServiceKey) {
    myDimensionServiceKey = dimensionServiceKey;
  }

  @Override
  public void showInCenterOf(@NotNull Component aContainer) {
    final Point popupPoint = getCenterOf(aContainer, myContent);
    show(aContainer, popupPoint.x, popupPoint.y, false);
  }

  public void setAdText(@NotNull final String s) {
    setAdText(s, SwingConstants.LEFT);
  }

  @Override
  public void setAdText(@NotNull final String s, int alignment) {
    if (myAdComponent == null) {
      myAdComponent = HintUtil.createAdComponent(s, BorderFactory.createEmptyBorder(1, 5, 1, 5), alignment);
      JPanel wrapper = new JPanel(new BorderLayout()) {
        @Override
        protected void paintComponent(Graphics g) {
          g.setColor(Gray._135);
          g.drawLine(0, 0, getWidth(), 0);
          super.paintComponent(g);
        }
      };
      wrapper.setOpaque(false);
      wrapper.setBorder(new EmptyBorder(1, 0, 0, 0));
      wrapper.add(myAdComponent, BorderLayout.CENTER);
      myContent.add(wrapper, BorderLayout.SOUTH);
      pack(false, true);
    } else {
      myAdComponent.setText(s);
      myAdComponent.setHorizontalAlignment(alignment);
    }
  }

  public static Point getCenterOf(final Component aContainer, final JComponent content) {
    final JComponent component = getTargetComponent(aContainer);

    Point containerScreenPoint = component.getVisibleRect().getLocation();
    SwingUtilities.convertPointToScreen(containerScreenPoint, aContainer);

    return UIUtil.getCenterPoint(new Rectangle(containerScreenPoint, component.getVisibleRect().getSize()), content.getPreferredSize());
  }

  @Override
  public void showCenteredInCurrentWindow(@NotNull Project project) {
    Window window = null;

    Component focusedComponent = getWndManager().getFocusedComponent(project);
    if (focusedComponent != null) {
      Component parent = UIUtil.findUltimateParent(focusedComponent);
      if (parent instanceof Window) {
        window = (Window)parent;
      }
    }
    if (window == null) {
      window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
    }

    if (window != null) {
      showInCenterOf(window);
    }
  }

  @Override
  public void showUnderneathOf(@NotNull Component aComponent) {
    show(new RelativePoint(aComponent, new Point(0, aComponent.getHeight())));
  }

  @Override
  public void show(@NotNull RelativePoint aPoint) {
    final Point screenPoint = aPoint.getScreenPoint();
    show(aPoint.getComponent(), screenPoint.x, screenPoint.y, false);
  }

  @Override
  public void showInScreenCoordinates(@NotNull Component owner, @NotNull Point point) {
    show(owner, point.x, point.y, false);
  }

  @Override
  public void showInBestPositionFor(@NotNull DataContext dataContext) {
    final Editor editor = PlatformDataKeys.EDITOR.getData(dataContext);
    if (editor != null) {
      showInBestPositionFor(editor);
    }
    else {
      show(relativePointByQuickSearch(dataContext));
    }
  }

  @Override
  public void showInFocusCenter() {
    final Component focused = getWndManager().getFocusedComponent(myProject);
    if (focused != null) {
      showInCenterOf(focused);
    }
    else {
      final JFrame frame = WindowManager.getInstance().getFrame(myProject);
      showInCenterOf(frame.getRootPane());
    }
  }

  private RelativePoint relativePointByQuickSearch(final DataContext dataContext) {
    Rectangle dominantArea = PlatformDataKeys.DOMINANT_HINT_AREA_RECTANGLE.getData(dataContext);

    if (dominantArea != null) {
      final Component focusedComponent = getWndManager().getFocusedComponent(myProject);
      Window window = SwingUtilities.windowForComponent(focusedComponent);
      JLayeredPane layeredPane;
      if (window instanceof JFrame) {
        layeredPane = ((JFrame)window).getLayeredPane();
      }
      else if (window instanceof JDialog) {
        layeredPane = ((JDialog)window).getLayeredPane();
      }
      else if (window instanceof JWindow) {
        layeredPane = ((JWindow)window).getLayeredPane();
      }
      else {
        throw new IllegalStateException("cannot find parent window: project=" + myProject + "; window=" + window);
      }

      return relativePointWithDominantRectangle(layeredPane, dominantArea);
    }

    return JBPopupFactory.getInstance().guessBestPopupLocation(dataContext);
  }

  @Override
  public void showInBestPositionFor(@NotNull Editor editor) {
    assert editor.getComponent().isShowing() : "Editor must be showing on the screen";

    DataContext context = ((EditorEx)editor).getDataContext();
    Rectangle dominantArea = PlatformDataKeys.DOMINANT_HINT_AREA_RECTANGLE.getData(context);
    if (dominantArea != null && !myRequestFocus) {
      final JLayeredPane layeredPane = editor.getContentComponent().getRootPane().getLayeredPane();
      show(relativePointWithDominantRectangle(layeredPane, dominantArea));
    }
    else {
      show(guessBestPopupLocation(editor));
    }
  }

  @NotNull
  private RelativePoint guessBestPopupLocation(@NotNull Editor editor) {
    RelativePoint preferredLocation = JBPopupFactory.getInstance().guessBestPopupLocation(editor);
    if (myDimensionServiceKey == null) {
      return preferredLocation;
    }
    Dimension preferredSize = DimensionService.getInstance().getSize(myDimensionServiceKey, myProject);
    if (preferredSize == null) {
      return preferredLocation;
    }
    Rectangle preferredBounds = new Rectangle(preferredLocation.getScreenPoint(), preferredSize);
    Rectangle adjustedBounds = new Rectangle(preferredBounds);
    ScreenUtil.moveRectangleToFitTheScreen(adjustedBounds);
    if (preferredBounds.y - adjustedBounds.y <= 0) {
      return preferredLocation;
    }
    int adjustedY = preferredBounds.y - editor.getLineHeight() * 3 / 2 - preferredSize.height;
    return adjustedY >= 0 ? RelativePoint.fromScreen(new Point(preferredBounds.x, adjustedY)) : preferredLocation;
  }
  
  public void addPopupListener(JBPopupListener listener) {
    myListeners.add(listener);
  }

  private RelativePoint relativePointWithDominantRectangle(final JLayeredPane layeredPane, final Rectangle bounds) {
    Dimension preferredSize = getComponent().getPreferredSize();
    if (myDimensionServiceKey != null) {
      final Dimension dimension = DimensionService.getInstance().getSize(myDimensionServiceKey, myProject);
      if (dimension != null) {
        preferredSize = dimension;
      }
    }
    final Point leftTopCorner = new Point(bounds.x + bounds.width, bounds.y);
    final Point leftTopCornerScreen = (Point)leftTopCorner.clone();
    SwingUtilities.convertPointToScreen(leftTopCornerScreen, layeredPane);
    final RelativePoint relativePoint;
    if (!ScreenUtil.isOutsideOnTheRightOFScreen(
      new Rectangle(leftTopCornerScreen.x, leftTopCornerScreen.y, preferredSize.width, preferredSize.height))) {
      relativePoint = new RelativePoint(layeredPane, leftTopCorner);
    }
    else {
      if (bounds.x > preferredSize.width) {
        relativePoint = new RelativePoint(layeredPane, new Point(bounds.x - preferredSize.width, bounds.y));
      }
      else {
        setDimensionServiceKey(null); // going to cut width
        Rectangle screen = ScreenUtil.getScreenRectangle(leftTopCornerScreen.x, leftTopCornerScreen.y);
        final int spaceOnTheLeft = bounds.x;
        final int spaceOnTheRight = screen.x + screen.width - leftTopCornerScreen.x;
        if (spaceOnTheLeft > spaceOnTheRight) {
          relativePoint = new RelativePoint(layeredPane, new Point(0, bounds.y));
          myComponent.setPreferredSize(new Dimension(spaceOnTheLeft, Math.max(preferredSize.height, 200)));
        }
        else {
          relativePoint = new RelativePoint(layeredPane, leftTopCorner);
          myComponent.setPreferredSize(new Dimension(spaceOnTheRight, Math.max(preferredSize.height, 200)));
        }
      }
    }
    return relativePoint;
  }

  @Override
  public final void closeOk(@Nullable InputEvent e) {
    setOk(true);
    cancel(e);
  }

  @Override
  public final void cancel() {
    cancel(null);
  }

  @Override
  public void setRequestFocus(boolean requestFocus) {
    myRequestFocus = requestFocus;
  }

  @Override
  public void cancel(InputEvent e) {
    if (isDisposed()) return;

    if (myPopup != null) {
      if (!canClose()) {
        return;
      }
      storeDimensionSize(myContent.getSize());
      if (myUseDimServiceForXYLocation) {
        final JRootPane root = myComponent.getRootPane();
        if (root != null) {
          final Container popupWindow = root.getParent();
          if (popupWindow != null && popupWindow.isShowing()) {
            storeLocation(popupWindow.getLocationOnScreen());
          }
        }
      }

      if (e instanceof MouseEvent) {
        IdeEventQueue.getInstance().blockNextEvents((MouseEvent)e);
      }

      myPopup.hide(false);

      if (ApplicationManagerEx.getApplicationEx() != null) {
        StackingPopupDispatcher.getInstance().onPopupHidden(this);
      }

      if (myInStack) {
        myFocusTrackback.setForcedRestore(!myOk && myFocusable);
        myFocusTrackback.restoreFocus();
      }


      disposePopup();

      if (myListeners != null) {
        for (JBPopupListener each : myListeners) {
          each.onClosed(new LightweightWindowEvent(this, myOk));
        }
      }
    }

    Disposer.dispose(this, false);
    if (myProjectDisposable != null) {
      Disposer.dispose(myProjectDisposable);
    }
  }

  public FocusTrackback getFocusTrackback() {
    return myFocusTrackback;
  }

  private void disposePopup() {
    if (myPopup != null) {
      myPopup.hide(true);
    }
    myPopup = null;
  }

  @Override
  public boolean canClose() {
    return myCallBack == null || myCallBack.compute().booleanValue();
  }

  @Override
  public boolean isVisible() {
    return myPopup != null;
  }

  @Override
  public void show(final Component owner) {
    show(owner, -1, -1, true);
  }

  public void show(Component owner, int aScreenX, int aScreenY, final boolean considerForcedXY) {
    if (ApplicationManagerEx.getApplicationEx() != null && ApplicationManager.getApplication().isHeadlessEnvironment()) return;
    if (isDisposed()) {
      throw new IllegalStateException("Popup was already disposed. Recreate a new instance to show again");
    }

    assert ApplicationManager.getApplication().isDispatchThread();

    installWindowHook(this);
    installProjectDisposer();
    addActivity();

    final Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

    final boolean shouldShow = beforeShow();
    if (!shouldShow) {
      removeActivity();
      return;
    }

    prepareToShow();

    if (myInStack) {
      myFocusTrackback = new FocusTrackback(this, owner, true);
      myFocusTrackback.setMustBeShown(true);
    }


    Dimension sizeToSet = null;

    if (myDimensionServiceKey != null) {
      sizeToSet = DimensionService.getInstance().getSize(myDimensionServiceKey, myProject);
    }

    if (myForcedSize != null) {
      sizeToSet = myForcedSize;
    }

    if (myMinSize == null) {
      myMinSize = myContent.getMinimumSize();
    }

    if (sizeToSet == null) {
      sizeToSet = myContent.getPreferredSize();
    }

    if (sizeToSet != null) {
      sizeToSet.width = Math.max(sizeToSet.width, myMinSize.width);
      sizeToSet.height = Math.max(sizeToSet.height, myMinSize.height);

      myContent.setSize(sizeToSet);
      myContent.setPreferredSize(sizeToSet);
    }

    Point xy = new Point(aScreenX, aScreenY);
    boolean adjustXY = true;
    if (myDimensionServiceKey != null) {
      final Point storedLocation = DimensionService.getInstance().getLocation(myDimensionServiceKey, myProject);
      if (storedLocation != null) {
        xy = storedLocation;
        adjustXY = false;
      }
    }

    if (adjustXY) {
      final Insets insets = myContent.getInsets();
      if (insets != null) {
        xy.x -= insets.left;
        xy.y -= insets.top;
      }
    }

    if (considerForcedXY && myForcedLocation != null) {
      xy = myForcedLocation;
    }

    if (myLocateByContent) {
      final Dimension captionSize = myHeaderPanel.getPreferredSize();
      xy.y -= captionSize.height;
    }

    Rectangle targetBounds = new Rectangle(xy, myContent.getPreferredSize());
    Insets insets = myPopupBorder.getBorderInsets(myContent);
    if (insets != null) {
      targetBounds.x += insets.left;
      targetBounds.y += insets.top;
    }

    Rectangle original = new Rectangle(targetBounds);
    if (myLocateWithinScreen) {
      ScreenUtil.moveRectangleToFitTheScreen(targetBounds);
    }

    if (myMouseOutCanceller != null) {
      myMouseOutCanceller.myEverEntered = targetBounds.equals(original);
    }

    myOwner = IdeFrameImpl.findNearestModalComponent(owner);
    if (myOwner == null) {
      myOwner = owner;
    }

    myRequestorComponent = owner;

    boolean forcedDialog = myMayBeParent
      || SystemInfo.isMac && !(myOwner instanceof IdeFrame) && myOwner != null && myOwner.isShowing();

    PopupComponent.Factory factory = getFactory(myForcedHeavyweight || myResizable, forcedDialog);
    myNativePopup = factory.isNativePopup();
    myPopup = factory.getPopup(myOwner, myContent, targetBounds.x, targetBounds.y, this);

    if (myResizable) {
      final JRootPane root = myContent.getRootPane();
      final IdeGlassPaneImpl glass = new IdeGlassPaneImpl(root);
      root.setGlassPane(glass);

      final ResizeComponentListener resizeListener = new ResizeComponentListener(this, glass);
      glass.addMousePreprocessor(resizeListener, this);
      glass.addMouseMotionPreprocessor(resizeListener, this);
    }

    if (myCaption != null && myMovable) {
      final MoveComponentListener moveListener = new MoveComponentListener(myCaption) {
        @Override
        public void mousePressed(final MouseEvent e) {
          super.mousePressed(e);
          if (e.isConsumed()) return;

          if (UIUtil.isCloseClick(e)) {
            if (myCaption.isWithinPanel(e)) {
              cancel();
            }
          }
        }
      };
      ListenerUtil.addMouseListener(myCaption, moveListener);
      ListenerUtil.addMouseMotionListener(myCaption, moveListener);
      final MyContentPanel saved = myContent;
      Disposer.register(this, new Disposable() {
        @Override
        public void dispose() {
          ListenerUtil.removeMouseListener(saved, moveListener);
          ListenerUtil.removeMouseMotionListener(saved, moveListener);
        }
      });
    }

    for (JBPopupListener listener : myListeners) {
      listener.beforeShown(new LightweightWindowEvent(this));
    }

    myPopup.setRequestFocus(myRequestFocus);
    myPopup.show();

    final Window window = SwingUtilities.getWindowAncestor(myContent);

    myWindowListener = new MyWindowListener();
    window.addWindowListener(myWindowListener);

    if (myFocusable) {
      window.setFocusableWindowState(true);
      window.setFocusable(true);
    }

    myWindow = updateMaskAndAlpha(window);


    if (myWindow != null) {
      // dialogwrapper-based popups do this internally through peer,
      // for other popups like jdialog-based we should exclude them manually, but
      // we still have to be able to use IdeFrame as parent
      if (!myMayBeParent && !(myWindow instanceof Frame)) {
        WindowManager.getInstance().doNotSuggestAsParent(myWindow);
      }
    }


    final Runnable afterShow = new Runnable() {
      @Override
      public void run() {
        if (myPreferredFocusedComponent != null && myInStack && myFocusable) {
          myFocusTrackback.registerFocusComponent(myPreferredFocusedComponent);
        }

        removeActivity();

        afterShow();

      }
    };

    if (myRequestFocus) {
      getFocusManager().requestFocus(new FocusCommand() {
        @Override
        public ActionCallback run() {
          if (isDisposed()) {
            removeActivity();
            return new ActionCallback.Done();
          }

          _requestFocus();

          final ActionCallback result = new ActionCallback();

          final Runnable afterShowRunnable = new Runnable() {
            @Override
            public void run() {
              afterShow.run();
              result.setDone();
            }
          };
          if (myNativePopup) {
            final FocusRequestor furtherRequestor = getFocusManager().getFurtherRequestor();
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                if (isDisposed()) {
                  result.setRejected();
                  return;
                }

                furtherRequestor.requestFocus(new FocusCommand() {
                  @Override
                  public ActionCallback run() {
                    if (isDisposed()) {
                      return new ActionCallback.Rejected();
                    }

                    _requestFocus();

                    afterShowRunnable.run();

                    return new ActionCallback.Done();
                  }
                }, true).notify(result).doWhenProcessed(new Runnable() {
                  @Override
                  public void run() {
                    removeActivity();
                  }
                });
              }
            });
          } else {
            afterShowRunnable.run();
          }

          return result;
        }
      }, true).doWhenRejected(new Runnable() {
        @Override
        public void run() {
          afterShow.run();
        }
      });
    } else {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          if (isDisposed()) {
            removeActivity();
            return;
          }

          afterShow.run();
        }
      });
    }
  }

  private void installProjectDisposer() {
    final Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (c != null) {
      final DataContext context = DataManager.getInstance().getDataContext(c);
      final Project project = PlatformDataKeys.PROJECT.getData(context);
      if (project != null) {
        myProjectDisposable = new Disposable() {

          @Override
          public void dispose() {
            if (!AbstractPopup.this.isDisposed()) {
              Disposer.dispose(AbstractPopup.this);
            }
          }
        };
        Disposer.register(project, myProjectDisposable);
      }
    }
  }

  //Sometimes just after popup was shown the WINDOW_ACTIVATED cancels it
  private static void installWindowHook(final AbstractPopup popup) {
    if (popup.myCancelOnWindow) {
      popup.myCancelOnWindow = false;
      new Alarm(popup).addRequest(new Runnable() {
        @Override
        public void run() {
          popup.myCancelOnWindow = true;
        }
      }, 100);
    }
  }

  private void addActivity() {
    UiActivityMonitor.getInstance().addActivity(myActivityKey);
  }

  private void removeActivity() {
    UiActivityMonitor.getInstance().removeActivity(myActivityKey);
  }

  private void prepareToShow() {
    final MouseAdapter mouseAdapter = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        Point point = (Point)e.getPoint().clone();
        SwingUtilities.convertPointToScreen(point, e.getComponent());

        final Dimension dimension = myContent.getSize();
        dimension.height += myResizable && isToDrawMacCorner() ? ourMacCorner.getIconHeight() : 4;
        dimension.width += 4;
        Point locationOnScreen = myContent.getLocationOnScreen();
        final Rectangle bounds = new Rectangle(new Point(locationOnScreen.x - 2, locationOnScreen.y - 2), dimension);
        if (!bounds.contains(point)) {
          cancel();
        }
      }
    };
    myContent.addMouseListener(mouseAdapter);
    Disposer.register(this, new Disposable() {
      @Override
      public void dispose() {
        myContent.removeMouseListener(mouseAdapter);
      }
    });

    myContent.registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (myCancelKeyEnabled) {
          cancel();
        }
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);


    mySearchKeyListener = new SpeedSearchKeyListener();
    myContent.addKeyListener(mySearchKeyListener);

    if (myCancelOnMouseOutCallback != null || myCancelOnWindow) {
      myMouseOutCanceller = new Canceller();
      Toolkit.getDefaultToolkit().addAWTEventListener(myMouseOutCanceller, AWTEvent.MOUSE_EVENT_MASK | WindowEvent.WINDOW_ACTIVATED |
                                                                           AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }


    myFocusWatcher = new ChildFocusWatcher(myContent) {
      @Override
      protected void onFocusGained(final FocusEvent event) {
        setWindowActive(true);
      }

      @Override
      protected void onFocusLost(final FocusEvent event) {
        setWindowActive(false);
      }

    };

    mySpeedSearchPatternField = new JTextField();
    if (SystemInfo.isMac) {
      Font f = mySpeedSearchPatternField.getFont();
      mySpeedSearchPatternField.setFont(f.deriveFont(f.getStyle(), f.getSize() - 2));
    }
  }

  private Window updateMaskAndAlpha(Window window) {
    if (window == null) return window;

    final WindowManagerEx wndManager = getWndManager();
    if (wndManager == null) return window;

    if (!wndManager.isAlphaModeEnabled(window)) return window;

    if (myAlpha != myLastAlpha) {
      wndManager.setAlphaModeRatio(window, myAlpha);
      myLastAlpha = myAlpha;
    }

    if (myMaskProvider != null) {
      final Dimension size = window.getSize();
      Shape mask = myMaskProvider.getMask(size);
      wndManager.setWindowMask(window, mask);
    }

    WindowManagerEx.WindowShadowMode mode =
      myShadowed ? WindowManagerEx.WindowShadowMode.NORMAL : WindowManagerEx.WindowShadowMode.DISABLED;
    WindowManagerEx.getInstanceEx().setWindowShadow(window, mode);

    return window;
  }

  private static WindowManagerEx getWndManager() {
    return ApplicationManagerEx.getApplicationEx() != null ? WindowManagerEx.getInstanceEx() : null;
  }

  @Override
  public boolean isDisposed() {
    return myContent == null;
  }

  protected boolean beforeShow() {
    if (ApplicationManagerEx.getApplicationEx() == null) return true;
    StackingPopupDispatcher.getInstance().onPopupShown(this, myInStack);
    return true;
  }

  protected void afterShow() {
  }

  protected final boolean requestFocus() {
    if (!myFocusable) return false;

    getFocusManager().requestFocus(new FocusCommand() {
      @Override
      public ActionCallback run() {
        _requestFocus();
        return new ActionCallback.Done();
      }
    }, true);

    return true;
  }

  private void _requestFocus() {
    if (!myFocusable) return;

    if (myPreferredFocusedComponent != null) {
      myPreferredFocusedComponent.requestFocus();
    }
  }

  private IdeFocusManager getFocusManager() {
    if (myProject != null) {
      return IdeFocusManager.getInstance(myProject);
    }
    if (myOwner != null) {
      return IdeFocusManager.findInstanceByComponent(myOwner);
    }
    return IdeFocusManager.findInstance();
  }

  private static JComponent getTargetComponent(Component aComponent) {
    if (aComponent instanceof JComponent) {
      return (JComponent)aComponent;
    }
    if (aComponent instanceof RootPaneContainer) {
      return ((RootPaneContainer)aComponent).getRootPane();
    }

    LOG.error("Cannot find target for:" + aComponent);
    return null;
  }

  private PopupComponent.Factory getFactory(boolean forceHeavyweight, boolean forceDialog) {
    boolean noFocus = !myFocusable || !myRequestFocus;
    boolean cannotBeDialog = noFocus && SystemInfo.isLinux;

    if (!cannotBeDialog && (isPersistent() || forceDialog)) {
      return new PopupComponent.Factory.Dialog();
    }
    if (forceHeavyweight) {
      return new PopupComponent.Factory.AwtHeavyweight();
    }
    return new PopupComponent.Factory.AwtDefault();
  }

  @Override
  public JComponent getContent() {
    return myContent;
  }

  public void setLocation(RelativePoint p) {
    setLocation(p, myPopup);
  }

  private static void setLocation(final RelativePoint p, final PopupComponent popup) {
    if (popup == null) return;

    final Window wnd = popup.getWindow();
    assert wnd != null;

    wnd.setLocation(p.getScreenPoint());
  }

  @Override
  public void pack(boolean width, boolean height) {
    if (!isVisible() || !width && !height) return;

    Dimension size = getSize();
    Dimension prefSize = myContent.computePreferredSize();

    if (width) {
      size.width = prefSize.width;
    }

    if (height) {
      size.height = prefSize.height;
    }

    size = computeWindowSize(size);

    final Window window = SwingUtilities.getWindowAncestor(myContent);
    window.setSize(size);
  }

  public void pack() {
    final Window window = SwingUtilities.getWindowAncestor(myContent);

    window.pack();
  }

  public JComponent getComponent() {
    return myComponent;
  }

  public void setProject(Project project) {
    myProject = project;
  }


  @Override
  public void dispose() {
    if (myDisposed) {
      return;
    }
    myDisposed = true;

    Disposer.dispose(this, false);

    ApplicationManager.getApplication().assertIsDispatchThread();

    if (myPopup != null) {
      cancel(myDisposeEvent);
    }

    if (myContent != null) {
      myContent.removeAll();
      myContent.removeKeyListener(mySearchKeyListener);
    }
    myContent = null;
    myPreferredFocusedComponent = null;
    myComponent = null;
    myFocusTrackback = null;
    myCallBack = null;
    myListeners = null;

    if (myMouseOutCanceller != null) {
      final Toolkit toolkit = Toolkit.getDefaultToolkit();
      // it may happen, but have no idea how
      // http://www.jetbrains.net/jira/browse/IDEADEV-21265
      if (toolkit != null) {
        toolkit.removeAWTEventListener(myMouseOutCanceller);
      }
    }
    myMouseOutCanceller = null;

    if (myFocusWatcher != null) {
      myFocusWatcher.dispose();
      myFocusWatcher = null;
    }

    resetWindow();

    if (myFinalRunnable != null) {
      final ActionCallback typeaheadDone = new ActionCallback();
      Runnable runFinal = new Runnable() {
        @Override
        public void run() {
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              typeaheadDone.setDone();
            }
          });
          myFinalRunnable.run();
          myFinalRunnable = null;
        }
      };

      IdeFocusManager.getInstance(myProject).typeAheadUntil(typeaheadDone);
      getFocusManager().doWhenFocusSettlesDown(runFinal);
    }
  }

  private void resetWindow() {
    if (myWindow != null && getWndManager() != null) {
      getWndManager().resetWindow(myWindow);
      if (myWindowListener != null) {
        myWindow.removeWindowListener(myWindowListener);
      }

      if (myWindow instanceof JWindow) {
        ((JWindow)myWindow).getRootPane().putClientProperty(KEY, null);
      }

      myWindow = null;
      myWindowListener = null;
    }
  }

  public void storeDimensionSize(final Dimension size) {
    if (myDimensionServiceKey != null) {
      DimensionService.getInstance().setSize(myDimensionServiceKey, size, myProject);
    }
  }

  public void storeLocation(final Point xy) {
    if (myDimensionServiceKey != null) {
      DimensionService.getInstance().setLocation(myDimensionServiceKey, xy, myProject);
    }
  }

  public static class MyContentPanel extends JPanel implements DataProvider {
    private final boolean myResizable;
    private final boolean myDrawMacCorner;
    @Nullable private DataProvider myDataProvider;

    public MyContentPanel(final boolean resizable, final PopupBorder border, boolean drawMacCorner) {
      super(new BorderLayout());
      myResizable = resizable;
      myDrawMacCorner = drawMacCorner;
      setBorder(border);
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g);

      if (myResizable && myDrawMacCorner) {
        ourMacCorner.paintIcon(this, g,
                               getX() + getWidth() - ourMacCorner.getIconWidth(),
                               getY() + getHeight() - ourMacCorner.getIconHeight());
      }
    }

    public Dimension computePreferredSize() {
      if (isPreferredSizeSet()) {
        Dimension setSize = getPreferredSize();
        setPreferredSize(null);
        Dimension result = getPreferredSize();
        setPreferredSize(setSize);
        return result;
      }
      return getPreferredSize();
    }

    @Nullable
    @Override
    public Object getData(@NonNls String dataId) {
      return myDataProvider != null ? myDataProvider.getData(dataId) : null;
    }

    public void setDataProvider(@Nullable DataProvider dataProvider) {
      myDataProvider = dataProvider;
    }
  }

  public boolean isCancelOnClickOutside() {
    return myCancelOnClickOutside;
  }

  public boolean isCancelOnWindowDeactivation() {
    return myCancelOnWindowDeactivation;
  }

  public void setCancelOnWindowDeactivation(boolean cancelOnWindowDeactivation) {
    myCancelOnWindowDeactivation = cancelOnWindowDeactivation;
  }

  private class Canceller implements AWTEventListener {

    private boolean myEverEntered = false;

    @Override
    public void eventDispatched(final AWTEvent event) {
      if (event.getID() == WindowEvent.WINDOW_ACTIVATED) {
        if (myCancelOnWindow && myPopup != null && !myPopup.isPopupWindow(((WindowEvent)event).getWindow())) {
          cancel();
        }
      }
      else if (event.getID() == MouseEvent.MOUSE_ENTERED) {
        if (withinPopup(event)) {
          myEverEntered = true;
        }
      }
      else if (event.getID() == MouseEvent.MOUSE_MOVED) {
        if (myCancelOnMouseOutCallback != null && myEverEntered && !withinPopup(event)) {
          if (myCancelOnMouseOutCallback.check((MouseEvent)event)) {
            cancel();
          }
        }
      }
    }

    private boolean withinPopup(final AWTEvent event) {
      if (!myContent.isShowing()) return false;

      final MouseEvent mouse = (MouseEvent)event;
      final Point point = mouse.getPoint();
      SwingUtilities.convertPointToScreen(point, mouse.getComponent());
      return new Rectangle(myContent.getLocationOnScreen(), myContent.getSize()).contains(point);
    }
  }

  @Override
  public void setLocation(@NotNull final Point screenPoint) {
    if (myPopup == null) {
      myForcedLocation = screenPoint;
    }
    else {
      moveTo(myContent, screenPoint, myLocateByContent ? myHeaderPanel.getPreferredSize() : null);
    }
  }

  public static Window moveTo(JComponent content, Point screenPoint, final Dimension headerCorrectionSize) {
    setDefaultCursor(content);
    final Window wnd = SwingUtilities.getWindowAncestor(content);
    if (headerCorrectionSize != null) {
      screenPoint.y -= headerCorrectionSize.height;
    }
    wnd.setLocation(screenPoint);
    return wnd;
  }

  @Override
  public Point getLocationOnScreen() {
    Dimension headerCorrectionSize = myLocateByContent ? myHeaderPanel.getPreferredSize() : null;
    Point screenPoint = myContent.getLocationOnScreen();
    if (headerCorrectionSize != null) {
      screenPoint.y -= headerCorrectionSize.height;
    }

    return screenPoint;
  }


  @Override
  public void setSize(@NotNull final Dimension size) {
    setSize(size, true);
  }

  private void setSize(Dimension size, boolean adjustByContent) {
    Dimension toSet = size;
    if (myPopup == null) {
      myForcedSize = toSet;
    }
    else {
      if (adjustByContent) {
        toSet = computeWindowSize(toSet);
      }
      updateMaskAndAlpha(setSize(myContent, toSet));
    }
  }

  private Dimension computeWindowSize(Dimension size) {
    if (myAdComponent != null && myAdComponent.isShowing()) {
      size.height += myAdComponent.getPreferredSize().height + 1;
    }
    return size;
  }

  @Override
  public Dimension getSize() {
    if (myPopup != null) {
      final Window popupWindow = SwingUtilities.windowForComponent(myContent);
      return popupWindow.getSize();
    } else {
      return myForcedSize;
    }
  }

  @Override
  public void moveToFitScreen() {
    if (myPopup == null) return;
    
    final Window popupWindow = SwingUtilities.windowForComponent(myContent);
    Rectangle bounds = popupWindow.getBounds();

    ScreenUtil.moveRectangleToFitTheScreen(bounds);
    setLocation(bounds.getLocation());
    setSize(bounds.getSize(), false);
  }


  public static Window setSize(JComponent content, final Dimension size) {
    final Window popupWindow = SwingUtilities.windowForComponent(content);
    final Point location = popupWindow.getLocation();
    popupWindow.setLocation(location.x, location.y);
    Insets insets = content.getInsets();
    if (insets != null) {
      size.width += insets.left + insets.right;
      size.height += insets.top + insets.bottom;
    }
    content.setPreferredSize(size);
    popupWindow.pack();
    return popupWindow;
  }

  public static void setDefaultCursor(JComponent content) {
    final Window wnd = SwingUtilities.getWindowAncestor(content);
    if (wnd != null) {
      wnd.setCursor(Cursor.getDefaultCursor());
    }
  }

  public void setCaption(String title) {
    if (myCaption instanceof TitlePanel) {
      ((TitlePanel)myCaption).setText(title);
    }
  }

  private class MyWindowListener extends WindowAdapter {
    @Override
    public void windowClosed(final WindowEvent e) {
      resetWindow();
    }
  }

  @Override
  public boolean isPersistent() {
    return !myCancelOnClickOutside && !myCancelOnWindow;
  }

  @Override
  public boolean isNativePopup() {
    return myNativePopup;
  }

  @Override
  public void setUiVisible(final boolean visible) {
    if (myPopup != null) {
      if (visible) {
        myPopup.show();
        final Window window = getPopupWindow();
        if (window != null && myRestoreWindowSize != null) {
          window.setSize(myRestoreWindowSize);
          myRestoreWindowSize = null;
        }
      }
      else {
        final Window window = getPopupWindow();
        if (window != null) {
          myRestoreWindowSize = window.getSize();
          window.setVisible(true);
        }
      }
    }
  }

  private Window getPopupWindow() {
    return myPopup.getWindow();
  }

  public void setUserData(ArrayList<Object> userData) {
    myUserData = userData;
  }

  @Override
  public <T> T getUserData(final Class<T> userDataClass) {
    if (myUserData != null) {
      for (Object o : myUserData) {
        if (userDataClass.isInstance(o)) {
          //noinspection unchecked
          return (T)o;
        }
      }
    }
    return null;
  }

  @Override
  public boolean isModalContext() {
    return myModalContext;
  }

  @Override
  public boolean isFocused() {
    if (myComponent != null && isFocused(new Component[]{SwingUtilities.getWindowAncestor(myComponent)})) {
      return true;
    }
    return isFocused(myFocusOwners);
  }

  public static boolean isFocused(@Nullable Component[] components) {
    if (components == null) return false;

    Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

    if (owner == null) return false;

    Window wnd;
    if (owner instanceof Window) {
      wnd = (Window)owner;
    }
    else {
      wnd = SwingUtilities.getWindowAncestor(owner);
    }

    for (Component each : components) {
      if (each != null && SwingUtilities.isDescendingFrom(owner, each)) {
        Window eachWindow = each instanceof Window ? (Window)each : SwingUtilities.getWindowAncestor(each);
        if (eachWindow == wnd) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public boolean isCancelKeyEnabled() {
    return myCancelKeyEnabled;
  }

  @NotNull
  CaptionPanel getTitle() {
    return myCaption;
  }

  private void setHeaderComponent(JComponent c) {
    boolean doRevalidate = false;
    if (myHeaderComponent != null) {
      myHeaderPanel.remove(myHeaderComponent);
      myHeaderPanel.add(myCaption, BorderLayout.NORTH);
      myHeaderComponent = null;
      doRevalidate = true;
    }

    if (c != null) {
      myHeaderPanel.remove(myCaption);
      myHeaderPanel.add(c, BorderLayout.NORTH);
      myHeaderComponent = c;

      final Dimension size = myContent.getSize();
      if (size.height < c.getPreferredSize().height * 2) {
        size.height += c.getPreferredSize().height;
        setSize(size);
      }

      doRevalidate = true;
    }

    if (doRevalidate) myContent.revalidate();
  }
  
  public void setWarning(@NotNull String text) {
    JBLabel label = new JBLabel(text, UIUtil.getBalloonWarningIcon(), SwingConstants.CENTER);
    label.setOpaque(true);
    Color color = HintUtil.INFORMATION_COLOR;
    label.setBackground(color);
    label.setBorder(BorderFactory.createLineBorder(color, 3));
    myHeaderPanel.add(label, BorderLayout.SOUTH);
  }

  @Override
  public void addListener(final JBPopupListener listener) {
    myListeners.add(listener);
  }

  @Override
  public void removeListener(final JBPopupListener listener) {
    myListeners.remove(listener);
  }

  protected void onSpeedSearchPatternChanged() {
  }

  @Override
  public Component getOwner() {
    return myRequestorComponent;
  }

  @Override
  public void setMinimumSize(Dimension size) {
    myMinSize = size;
  }

  public Runnable getFinalRunnable() {
    return myFinalRunnable;
  }

  @Override
  public void setFinalRunnable(Runnable finalRunnable) {
    myFinalRunnable = finalRunnable;
  }

  public void setOk(boolean ok) {
    myOk = ok;
  }

  @Override
  public void setDataProvider(@NotNull DataProvider dataProvider) {
    if (myContent != null) {
      myContent.setDataProvider(dataProvider);
    }
  }

  @Override
  public boolean dispatchKeyEvent(@NotNull KeyEvent e) {
    BooleanFunction<KeyEvent> handler = myKeyEventHandler;
    return handler != null && handler.fun(e);
  }

  private class SpeedSearchKeyListener implements KeyListener {
    @Override
    public void keyTyped(final KeyEvent e) {
      mySpeedSearch.process(e);
    }

    @Override
    public void keyPressed(final KeyEvent e) {
      mySpeedSearch.process(e);
    }

    @Override
    public void keyReleased(final KeyEvent e) {
      mySpeedSearch.process(e);
    }
  }

  @NotNull
  public Dimension getHeaderPreferredSize() {
    return myHeaderPanel.getPreferredSize();
  }
  @NotNull
  public Dimension getFooterPreferredSize() {
    return myAdComponent == null ? new Dimension(0,0) : myAdComponent.getPreferredSize();
  }
}
