// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.execution.impl;

import com.google.common.base.CharMatcher;
import com.intellij.codeInsight.navigation.IncrementalSearchHandler;
import com.intellij.codeInsight.template.impl.editorActions.TypedActionHandlerBase;
import com.intellij.execution.ConsoleFolding;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.actions.ConsoleActionsPostProcessor;
import com.intellij.execution.actions.EOFAction;
import com.intellij.execution.filters.*;
import com.intellij.execution.filters.Filter.ResultItem;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.ObservableConsoleView;
import com.intellij.icons.AllIcons;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.OccurenceNavigator;
import com.intellij.ide.startup.StartupManagerEx;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.*;
import com.intellij.openapi.editor.actions.ScrollToTheEndToolbarAction;
import com.intellij.openapi.editor.actions.ToggleUseSoftWrapsToolbarAction;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.impl.RangeMarkerImpl;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.ui.UIUtil;
import gnu.trove.THashSet;
import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class ConsoleViewImpl extends JPanel implements ConsoleView, ObservableConsoleView, DataProvider, OccurenceNavigator {
  @NonNls private static final String CONSOLE_VIEW_POPUP_MENU = "ConsoleView.PopupMenu";
  private static final Logger LOG = Logger.getInstance("#com.intellij.execution.impl.ConsoleViewImpl");

  private static final int DEFAULT_FLUSH_DELAY = SystemProperties.getIntProperty("console.flush.delay.ms", 200);

  public static final Key<ConsoleViewImpl> CONSOLE_VIEW_IN_EDITOR_VIEW = Key.create("CONSOLE_VIEW_IN_EDITOR_VIEW");
  private static final Key<ConsoleViewContentType> CONTENT_TYPE = Key.create("ConsoleViewContentType");
  private static final Key<Boolean> USER_INPUT_SENT = Key.create("USER_INPUT_SENT");
  private static final Key<Boolean> MANUAL_HYPERLINK = Key.create("MANUAL_HYPERLINK");

  private static boolean ourTypedHandlerInitialized;
  private final Alarm myFlushUserInputAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
  private static final CharMatcher NEW_LINE_MATCHER = CharMatcher.anyOf("\n\r");

  private final CommandLineFolding myCommandLineFolding = new CommandLineFolding();

  private final DisposedPsiManagerCheck myPsiDisposedCheck;

  private final boolean myIsViewer;
  @NotNull
  private ConsoleState myState;

  private final Alarm mySpareTimeAlarm = new Alarm(this);

  @NotNull
  private final Alarm myHeavyAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
  private volatile int myHeavyUpdateTicket;
  private final Collection<ChangeListener> myListeners = new CopyOnWriteArraySet<>();

  private final List<AnAction> customActions = new ArrayList<>();
  /** the text from {@link #print(String, ConsoleViewContentType)} goes there and stays there until {@link #flushDeferredText()} is called */
  private final TokenBuffer myDeferredBuffer = new TokenBuffer(ConsoleBuffer.useCycleBuffer() ? ConsoleBuffer.getCycleBufferSize() : Integer.MAX_VALUE);

  private boolean myUpdateFoldingsEnabled = true;

  private EditorHyperlinkSupport myHyperlinks;
  private MyDiffContainer myJLayeredPane;
  private JPanel myMainPanel;
  private boolean myAllowHeavyFilters;
  private boolean myLastStickingToEnd;
  private boolean myCancelStickToEnd;

  private final Set<FlushRunnable> myCurrentRequests = new THashSet<>();
  private final Alarm myFlushAlarm = new Alarm(this);

  private final Project myProject;

  private boolean myOutputPaused;

  private EditorEx myEditor;

  private final Object LOCK = new Object();

  private final TIntObjectHashMap<ConsoleFolding> myFolding = new TIntObjectHashMap<>();

  private String myHelpId;

  protected final CompositeFilter myFilters;

  @NotNull
  private final InputFilter myInputMessageFilter;

  public ConsoleViewImpl(@NotNull Project project, boolean viewer) {
    this(project, GlobalSearchScope.allScope(project), viewer, true);
  }

  public ConsoleViewImpl(@NotNull final Project project,
                         @NotNull GlobalSearchScope searchScope,
                         boolean viewer,
                         boolean usePredefinedMessageFilter) {
    this(project, searchScope, viewer,
         new ConsoleState.NotStartedStated() {
           @NotNull
           @Override
           public ConsoleState attachTo(@NotNull ConsoleViewImpl console, ProcessHandler processHandler) {
             return new ConsoleViewRunningState(console, processHandler, this, true, true);
           }
         },
         usePredefinedMessageFilter);
  }

  protected ConsoleViewImpl(@NotNull final Project project,
                            @NotNull GlobalSearchScope searchScope,
                            boolean viewer,
                            @NotNull final ConsoleState initialState,
                            boolean usePredefinedMessageFilter) {
    super(new BorderLayout());
    initTypedHandler();
    myIsViewer = viewer;
    myState = initialState;
    myPsiDisposedCheck = new DisposedPsiManagerCheck(project);
    myProject = project;

    myFilters = new CompositeFilter(project, usePredefinedMessageFilter ? computeConsoleFilters(project, searchScope) : new SmartList<>());
    myFilters.setForceUseAllFilters(true);

    ConsoleInputFilterProvider[] inputFilters = Extensions.getExtensions(ConsoleInputFilterProvider.INPUT_FILTER_PROVIDERS);
    if (inputFilters.length > 0) {
      CompositeInputFilter compositeInputFilter = new CompositeInputFilter(project);
      myInputMessageFilter = compositeInputFilter;
      for (ConsoleInputFilterProvider eachProvider : inputFilters) {
        InputFilter[] filters = eachProvider.getDefaultFilters(project);
        for (InputFilter filter : filters) {
          compositeInputFilter.addFilter(filter);
        }
      }
    }
    else {
      myInputMessageFilter = (text, contentType) -> null;
    }

    project.getMessageBus().connect(this).subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
      private long myLastStamp;

      @Override
      public void enteredDumbMode() {
        if (myEditor == null) return;
        myLastStamp = myEditor.getDocument().getModificationStamp();

      }

      @Override
      public void exitDumbMode() {
        ApplicationManager.getApplication().invokeLater(() -> {
          if (myEditor == null || project.isDisposed() || DumbService.getInstance(project).isDumb()) return;

          DocumentEx document = myEditor.getDocument();
          if (myLastStamp != document.getModificationStamp()) {
            rehighlightHyperlinksAndFoldings();
          }
        });
      }
    });
    ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(EditorColorsManager.TOPIC, scheme -> {
      ApplicationManager.getApplication().assertIsDispatchThread();
      if (isDisposed() || myEditor == null) return;
      MarkupModel model = DocumentMarkupModel.forDocument(myEditor.getDocument(), project, false);
      for (RangeHighlighter tokenMarker : model.getAllHighlighters()) {
        ConsoleViewContentType contentType = tokenMarker.getUserData(CONTENT_TYPE);
        if (contentType != null && tokenMarker instanceof RangeHighlighterEx)
          ((RangeHighlighterEx)tokenMarker).setTextAttributes(contentType.getAttributes());
      }
    });
  }

  private static synchronized void initTypedHandler() {
    if (ourTypedHandlerInitialized) return;
    TypedAction typedAction = EditorActionManager.getInstance().getTypedAction();
    typedAction.setupHandler(new MyTypedHandler(typedAction.getHandler()));
    ourTypedHandlerInitialized = true;
  }

  public Editor getEditor() {
    return myEditor;
  }

  public EditorHyperlinkSupport getHyperlinks() {
    return myHyperlinks;
  }

  public void scrollToEnd() {
    if (myEditor == null) return;
    EditorUtil.scrollToTheEnd(myEditor, true);
    myCancelStickToEnd = false;
  }

  public void foldImmediately() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (!myFlushAlarm.isEmpty()) {
      cancelAllFlushRequests();
      flushDeferredText();
    }

    final FoldingModel model = myEditor.getFoldingModel();
    model.runBatchFoldingOperation(() -> {
      for (FoldRegion region : model.getAllFoldRegions()) {
        model.removeFoldRegion(region);
      }
    });
    myFolding.clear();

    updateFoldings(0, myEditor.getDocument().getLineCount() - 1);
  }

  @NotNull
  private List<Filter> computeConsoleFilters(@NotNull Project project, @NotNull GlobalSearchScope searchScope) {
    List<Filter> result = new ArrayList<>();
    for (ConsoleFilterProvider eachProvider : ConsoleFilterProvider.FILTER_PROVIDERS.getExtensions()) {
      Filter[] filters;
      if (eachProvider instanceof ConsoleDependentFilterProvider) {
        filters = ((ConsoleDependentFilterProvider)eachProvider).getDefaultFilters(this, project, searchScope);
      }
      else if (eachProvider instanceof ConsoleFilterProviderEx) {
        filters = ((ConsoleFilterProviderEx)eachProvider).getDefaultFilters(project, searchScope);
      }
      else {
        filters = eachProvider.getDefaultFilters(project);
      }
      ContainerUtil.addAll(result, filters);
    }
    return result;
  }

  @Override
  public void attachToProcess(ProcessHandler processHandler) {
    myState = myState.attachTo(this, processHandler);
  }

  @Override
  public void clear() {
    if (myEditor == null) return;
    synchronized (LOCK) {
      // real document content will be cleared on next flush;
      myDeferredBuffer.clear();
      myFolding.clear();
    }
    if (!myFlushAlarm.isDisposed()) {
      cancelAllFlushRequests();
      addFlushRequest(0, CLEAR);
      cancelHeavyAlarm();
    }
  }

  @Override
  public void scrollTo(final int offset) {
    if (myEditor == null) return;
    class ScrollRunnable extends FlushRunnable {
      private final int myOffset = offset;

      @Override
      public void doRun() {
        flushDeferredText();
        if (myEditor == null) return;
        int moveOffset = Math.min(offset, myEditor.getDocument().getTextLength());
        if (ConsoleBuffer.useCycleBuffer() && moveOffset >= myEditor.getDocument().getTextLength()) {
          moveOffset = 0;
        }
        myEditor.getCaretModel().moveToOffset(moveOffset);
        myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
      }

      @Override
      public boolean equals(Object o) {
        return super.equals(o) && myOffset == ((ScrollRunnable)o).myOffset;
      }
    }
    addFlushRequest(0, new ScrollRunnable());
  }

  public void requestScrollingToEnd() {
    if (myEditor == null) {
      return;
    }

    addFlushRequest(0, new FlushRunnable() {
      @Override
      public void doRun() {
        flushDeferredText();
        if (myEditor != null && !myFlushAlarm.isDisposed()) {
          scrollToEnd();
        }
      }
    });
  }

  private void addFlushRequest(final int millis, @NotNull FlushRunnable flushRunnable) {
    synchronized (myCurrentRequests) {
      if (!myFlushAlarm.isDisposed() && myCurrentRequests.add(flushRunnable)) {
        myFlushAlarm.addRequest(flushRunnable, millis, getStateForUpdate());
      }
    }
  }

  @Override
  public void setOutputPaused(final boolean value) {
    myOutputPaused = value;
    if (!value) {
      requestFlushImmediately();
    }
  }

  @Override
  public boolean isOutputPaused() {
    return myOutputPaused;
  }

  private boolean keepSlashR = true;
  public void setEmulateCarriageReturn(boolean emulate) {
    keepSlashR = emulate;
  }

  @Override
  public boolean hasDeferredOutput() {
    synchronized (LOCK) {
      return myDeferredBuffer.length() > 0;
    }
  }

  @Override
  public void performWhenNoDeferredOutput(@NotNull Runnable runnable) {
    //Q: implement in another way without timer?
    if (hasDeferredOutput()) {
      performLaterWhenNoDeferredOutput(runnable);
    }
    else {
      runnable.run();
    }
  }

  private void performLaterWhenNoDeferredOutput(@NotNull Runnable runnable) {
    if (mySpareTimeAlarm.isDisposed()) return;
    mySpareTimeAlarm.addRequest(
      () -> performWhenNoDeferredOutput(runnable),
      100,
      ModalityState.stateForComponent(myJLayeredPane)
    );
  }

  @Override
  @NotNull
  public JComponent getComponent() {
    if (myMainPanel == null) {
      myMainPanel = new JPanel(new BorderLayout());
      myJLayeredPane = new MyDiffContainer(myMainPanel, myFilters.getUpdateMessage());
      Disposer.register(this, myJLayeredPane);
      add(myJLayeredPane, BorderLayout.CENTER);
    }

    if (myEditor == null) {
      initConsoleEditor();
      requestFlushImmediately();
      myMainPanel.add(createCenterComponent(), BorderLayout.CENTER);
    }
    return this;
  }

  /**
   * Adds transparent (actually, non-opaque) component over console.
   * It will be as big as console. Use it to draw on console because it does not prevent user from console usage.
   *
   * @param component component to add
   */
  public final void addLayerToPane(@NotNull final JComponent component) {
    getComponent(); // Make sure component exists
    component.setOpaque(false);
    component.setVisible(true);
    myJLayeredPane.add(component, 0);
  }

  private void initConsoleEditor() {
    myEditor = createConsoleEditor();
    registerConsoleEditorActions();
    myEditor.getScrollPane().setBorder(null);
    MouseAdapter mouseListener = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        updateStickToEndState(true);
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        updateStickToEndState(false);
      }

      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.isShiftDown()) return; // ignore horizontal scrolling
        updateStickToEndState(false);
      }
    };
    myEditor.getScrollPane().addMouseWheelListener(mouseListener);
    myEditor.getScrollPane().getVerticalScrollBar().addMouseListener(mouseListener);
    myEditor.getScrollPane().getVerticalScrollBar().addMouseMotionListener(mouseListener);
    myHyperlinks = new EditorHyperlinkSupport(myEditor, myProject);
    myEditor.getScrollingModel().addVisibleAreaListener(e -> {
      // There is a possible case that the console text is populated while the console is not shown (e.g. we're debugging and
      // 'Debugger' tab is active while 'Console' is not). It's also possible that newly added text contains long lines that
      // are soft wrapped. We want to update viewport position then when the console becomes visible.
      Rectangle oldR = e.getOldRectangle();

      if (oldR != null && oldR.height <= 0 &&
          e.getNewRectangle().height > 0 &&
          isStickingToEnd()) {
        scrollToEnd();
      }
    });
  }

  private void updateStickToEndState(boolean useImmediatePosition) {
    if (myEditor == null) return;

    JScrollBar scrollBar = myEditor.getScrollPane().getVerticalScrollBar();
    int scrollBarPosition = useImmediatePosition ? scrollBar.getValue() :
                            myEditor.getScrollingModel().getVisibleAreaOnScrollingFinished().y;
    boolean vscrollAtBottom = scrollBarPosition == scrollBar.getMaximum() - scrollBar.getVisibleAmount();
    boolean stickingToEnd = isStickingToEnd();

    if (!vscrollAtBottom && stickingToEnd) {
      myCancelStickToEnd = true;
    } else if (vscrollAtBottom && !stickingToEnd) {
      scrollToEnd();
    }
  }

  @NotNull
  protected JComponent createCenterComponent() {
    return myEditor.getComponent();
  }

  @Override
  public void dispose() {
    myState = myState.dispose();
    if (myEditor != null) {
      cancelAllFlushRequests();
      mySpareTimeAlarm.cancelAllRequests();
      disposeEditor();
      synchronized (LOCK) {
        myDeferredBuffer.clear();
      }
      myEditor = null;
      myHyperlinks = null;
    }
  }

  private void cancelAllFlushRequests() {
    synchronized (myCurrentRequests) {
      myCurrentRequests.clear();
      myFlushAlarm.cancelAllRequests();
    }
  }

  @TestOnly
  public void waitAllRequests() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    Future<?> future = ApplicationManager.getApplication().executeOnPooledThread(() -> {
      while (true) {
        try {
          myFlushAlarm.waitForAllExecuted(10, TimeUnit.SECONDS);
          myFlushUserInputAlarm.waitForAllExecuted(10, TimeUnit.SECONDS);
          myFlushAlarm.waitForAllExecuted(10, TimeUnit.SECONDS);
          myFlushUserInputAlarm.waitForAllExecuted(10, TimeUnit.SECONDS);
          return;
        }
        catch (CancellationException e) {
          //try again
        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
          throw new RuntimeException(e);
        }
      }
    });
    try {
      while (true) {
        try {
          future.get(10, TimeUnit.MILLISECONDS);
          break;
        }
        catch (TimeoutException ignored) {
        }
        UIUtil.dispatchAllInvocationEvents();
      }
    }
    catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  protected void disposeEditor() {
    UIUtil.invokeAndWaitIfNeeded((Runnable)() -> {
      if (!myEditor.isDisposed()) {
        EditorFactory.getInstance().releaseEditor(myEditor);
      }
    });
  }

  @Override
  public void print(@NotNull String text, @NotNull ConsoleViewContentType contentType) {
    List<Pair<String, ConsoleViewContentType>> result = myInputMessageFilter.applyFilter(text, contentType);
    if (result == null) {
      print(text, contentType, null);
    }
    else {
      for (Pair<String, ConsoleViewContentType> pair : result) {
        if (pair.first != null) {
          print(pair.first, pair.second == null ? contentType : pair.second, null);
        }
      }
    }
  }

  protected void print(@NotNull String text, @NotNull ConsoleViewContentType contentType, @Nullable HyperlinkInfo info) {
    // optimisation: most of the strings don't contain line separators
    for (int i=0; i<text.length(); i++) {
      char c = text.charAt(i);
      if (c == '\n' || c == '\r') {
        text = StringUtil.convertLineSeparators(text, keepSlashR);
        break;
      }
    }
    synchronized (LOCK) {
      myDeferredBuffer.print(text, contentType, info);

      if (contentType == ConsoleViewContentType.USER_INPUT) {
        requestFlushImmediately();
      }
      else if (myEditor != null) {
        final boolean shouldFlushNow = myDeferredBuffer.length() >= myDeferredBuffer.getCycleBufferSize();
        addFlushRequest(shouldFlushNow ? 0 : DEFAULT_FLUSH_DELAY, FLUSH);
      }
    }
  }

  // send text which was typed in the console to the running process
  private void sendUserInput(@NotNull CharSequence typedText) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (myState.isRunning() && NEW_LINE_MATCHER.indexIn(typedText) >= 0) {
      StringBuilder textToSend = new StringBuilder();
      // compute text input from the console contents:
      // all range markers beginning from the caret offset backwards, marked as user input and not marked as already sent
      for (RangeMarker marker = findTokenMarker(myEditor.getCaretModel().getOffset()-1);
           marker != null;
           marker = ((RangeMarkerImpl)marker).findRangeMarkerBefore()) {
        ConsoleViewContentType tokenType = getTokenType(marker);
        if (tokenType != null) {
          if (tokenType != ConsoleViewContentType.USER_INPUT || marker.getUserData(USER_INPUT_SENT) == Boolean.TRUE) {
            break;
          }
          marker.putUserData(USER_INPUT_SENT, true);
          textToSend.insert(0, marker.getDocument().getText(TextRange.create(marker)));
        }
      }
      if (textToSend.length() != 0) {
        myFlushUserInputAlarm.addRequest(() -> {
          if (myState.isRunning()) {
            try {
              // this may block forever, see IDEA-54340
              myState.sendUserInput(textToSend.toString());
            }
            catch (IOException ignored) {
            }
          }
        }, 0);
      }
    }
  }

  protected ModalityState getStateForUpdate() {
    return null;
  }

  private void requestFlushImmediately() {
    if (myEditor != null) {
      addFlushRequest(0, FLUSH);
    }
  }

  /**
   * Holds number of symbols managed by the current console.
   * <p/>
   * Total number is assembled as a sum of symbols that are already pushed to the document and number of deferred symbols that
   * are awaiting to be pushed to the document.
   */
  @Override
  public int getContentSize() {
    synchronized (LOCK) {
      return (myEditor == null ? 0 : myEditor.getDocument().getTextLength())
             + myDeferredBuffer.length();
    }
  }

  @Override
  public boolean canPause() {
    return true;
  }

  public void flushDeferredText() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (isDisposed()) return;
    final boolean shouldStickToEnd = !myCancelStickToEnd && isStickingToEnd();
    myCancelStickToEnd = false; // Cancel only needs to last for one update. Next time, isStickingToEnd() will be false.

    final StringBuilder addedText;
    List<TokenBuffer.TokenInfo> deferredTokens;
    final Document document = myEditor.getDocument();

    synchronized (LOCK) {
      if (myOutputPaused) return;
      addedText = new StringBuilder(myDeferredBuffer.length());

      deferredTokens = myDeferredBuffer.drain();
      if (deferredTokens.isEmpty()) return;
      cancelHeavyAlarm();
    }

    final RangeMarker lastProcessedOutput = document.createRangeMarker(document.getTextLength(), document.getTextLength());
    final Collection<ConsoleViewContentType> contentTypes = new HashSet<>();

    CommandProcessor.getInstance().executeCommand(myProject, () -> {
      if (!shouldStickToEnd) {
        myEditor.getScrollingModel().accumulateViewportChanges();
      }
      try {
        // the text can contain one "\r" at the start meaning we should delete the last line
        boolean startsWithCR = deferredTokens.get(0) == TokenBuffer.CR_TOKEN;
        int startIndex = startsWithCR ? 1 : 0;
        for (int i = startIndex; i < deferredTokens.size(); i++) {
          TokenBuffer.TokenInfo deferredToken = deferredTokens.get(i);
          addedText.append(deferredToken.getText()); // can just append texts because \r inside these tokens were already taken care of
        }
        if (startsWithCR) {
          // remove last line if any
          if (document.getLineCount() != 0) {
            int lineStartOffset = document.getLineStartOffset(document.getLineCount() - 1);
            document.deleteString(lineStartOffset, document.getTextLength());
          }
        }
        document.insertString(document.getTextLength(), addedText);
        // add token information as range markers
        // start from the end because portion of the text can be stripped from the document beginning because of a cycle buffer
        int offset = document.getTextLength();
        int tokenLength = 0;
        for (int i = deferredTokens.size() - 1; i >= startIndex; i--) {
          TokenBuffer.TokenInfo token = deferredTokens.get(i);
          contentTypes.add(token.contentType);
          tokenLength += token.length();
          TokenBuffer.TokenInfo prevToken = i == startIndex ? null : deferredTokens.get(i - 1);
          if (prevToken != null && token.contentType == prevToken.contentType && token.getHyperlinkInfo() == prevToken.getHyperlinkInfo()) {
            // do not create highlighter yet because can merge previous token with the current
            continue;
          }
          int start = Math.max(0, offset - tokenLength);
          if (start == offset) break;
          final HyperlinkInfo info = token.getHyperlinkInfo();
          if (info != null) {
            myHyperlinks.createHyperlink(start, offset, null, info).putUserData(MANUAL_HYPERLINK, true);
          }
          createTokenRangeHighlighter(token.contentType, start, offset);
          offset = start;
          tokenLength = 0;
        }
      }
      finally {
        if (!shouldStickToEnd) {
          myEditor.getScrollingModel().flushViewportChanges();
        }
      }
    }, null, DocCommandGroupId.noneGroupId(document));
    if (!contentTypes.isEmpty()) {
      for (ChangeListener each : myListeners) {
        each.contentAdded(contentTypes);
      }
    }
    myPsiDisposedCheck.performCheck();

    int startLine = lastProcessedOutput.isValid() ? myEditor.getDocument().getLineNumber(lastProcessedOutput.getEndOffset()) : 0;
    lastProcessedOutput.dispose();
    highlightHyperlinksAndFoldings(startLine);

    if (shouldStickToEnd) {
      scrollToEnd();
    }
    sendUserInput(addedText);
  }

  private void createTokenRangeHighlighter(@NotNull ConsoleViewContentType contentType,
                                           int startOffset,
                                           int endOffset) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    TextAttributes attributes = contentType.getAttributes();
    MarkupModel model = DocumentMarkupModel.forDocument(myEditor.getDocument(), getProject(), true);
    int layer = HighlighterLayer.SYNTAX + 1; // make custom filters able to draw their text attributes over the default ones
    RangeHighlighter tokenMarker = model.addRangeHighlighter(startOffset, endOffset, layer,
                                                             attributes, HighlighterTargetArea.EXACT_RANGE);
    tokenMarker.putUserData(CONTENT_TYPE, contentType);
  }

  boolean isDisposed() {
    return myProject.isDisposed() || myEditor == null;
  }

  protected void doClear() {
    ApplicationManager.getApplication().assertIsDispatchThread();

    if (isDisposed()) return;
    final DocumentEx document = myEditor.getDocument();
    synchronized (LOCK) {
      clearHyperlinkAndFoldings();
    }
    final int documentTextLength = document.getTextLength();
    if (documentTextLength > 0) {
      CommandProcessor.getInstance().executeCommand(myProject,
         () -> DocumentUtil.executeInBulk(document, true,
         ()->document.deleteString(0, documentTextLength)), null, DocCommandGroupId.noneGroupId(document));
    }
    MarkupModel model = DocumentMarkupModel.forDocument(myEditor.getDocument(), getProject(), true);
    model.removeAllHighlighters(); // remove all empty highlighters leftovers if any
  }

  private boolean isStickingToEnd() {
    if (myEditor == null) return myLastStickingToEnd;
    Document document = myEditor.getDocument();
    int caretOffset = myEditor.getCaretModel().getOffset();
    myLastStickingToEnd = document.getLineNumber(caretOffset) >= document.getLineCount() - 1;
    return myLastStickingToEnd;
  }

  private void clearHyperlinkAndFoldings() {
    for (RangeHighlighter highlighter : myEditor.getMarkupModel().getAllHighlighters()) {
      if (highlighter.getUserData(MANUAL_HYPERLINK) == null) {
        myEditor.getMarkupModel().removeHighlighter(highlighter);
      }
    }

    myFolding.clear();
    myEditor.getFoldingModel().runBatchFoldingOperation(() -> myEditor.getFoldingModel().clearFoldRegions());

    cancelHeavyAlarm();
  }

  private void cancelHeavyAlarm() {
    if (!myHeavyAlarm.isDisposed()) {
      myHeavyAlarm.cancelAllRequests();
      ++myHeavyUpdateTicket;
    }
  }

  @Override
  public Object getData(final String dataId) {
    if (myEditor == null) {
      return null;
    }
    if (CommonDataKeys.NAVIGATABLE.is(dataId)) {
      final LogicalPosition pos = myEditor.getCaretModel().getLogicalPosition();
      final HyperlinkInfo info = myHyperlinks.getHyperlinkInfoByLineAndCol(pos.line, pos.column);
      final OpenFileDescriptor openFileDescriptor = info instanceof FileHyperlinkInfo ? ((FileHyperlinkInfo)info).getDescriptor() : null;
      if (openFileDescriptor == null || !openFileDescriptor.getFile().isValid()) {
        return null;
      }
      return openFileDescriptor;
    }

    if (CommonDataKeys.EDITOR.is(dataId)) {
      return myEditor;
    }
    if (PlatformDataKeys.HELP_ID.is(dataId)) {
      return myHelpId;
    }
    if (LangDataKeys.CONSOLE_VIEW.is(dataId)) {
      return this;
    }
    return null;
  }

  @Override
  public void setHelpId(@NotNull final String helpId) {
    myHelpId = helpId;
  }

  public void setUpdateFoldingsEnabled(boolean updateFoldingsEnabled) {
    myUpdateFoldingsEnabled = updateFoldingsEnabled;
  }

  @Override
  public void addMessageFilter(@NotNull Filter filter) {
    myFilters.addFilter(filter);
  }

  @Override
  public void printHyperlink(@NotNull final String hyperlinkText, @Nullable HyperlinkInfo info) {
    print(hyperlinkText, ConsoleViewContentType.NORMAL_OUTPUT, info);
  }

  @NotNull
  private EditorEx createConsoleEditor() {
    return ReadAction.compute(() -> {
      EditorEx editor = doCreateConsoleEditor();
      LOG.assertTrue(UndoUtil.isUndoDisabledFor(editor.getDocument()));
      editor.setContextMenuGroupId(null); // disabling default context menu
      editor.addEditorMouseListener(new EditorPopupHandler() {
        @Override
        public void invokePopup(final EditorMouseEvent event) {
          popupInvoked(event.getMouseEvent());
        }
      });

      int bufferSize = ConsoleBuffer.useCycleBuffer() ? ConsoleBuffer.getCycleBufferSize() : 0;
      editor.getDocument().setCyclicBufferSize(bufferSize);

      editor.putUserData(CONSOLE_VIEW_IN_EDITOR_VIEW, this);

      editor.getSettings().setAllowSingleLogicalLineFolding(true); // We want to fold long soft-wrapped command lines

      return editor;
    });
  }

  @NotNull
  protected EditorEx doCreateConsoleEditor() {
    return ConsoleViewUtil.setupConsoleEditor(myProject, true, false);
  }

  private void registerConsoleEditorActions() {
    Shortcut[] shortcuts = KeymapUtil.getActiveKeymapShortcuts(IdeActions.ACTION_GOTO_DECLARATION).getShortcuts();
    CustomShortcutSet shortcutSet = new CustomShortcutSet(ArrayUtil.mergeArrays(shortcuts, CommonShortcuts.ENTER.getShortcuts()));
    new HyperlinkNavigationAction().registerCustomShortcutSet(shortcutSet, myEditor.getContentComponent());


    if (!myIsViewer) {
      new EnterHandler().registerCustomShortcutSet(CommonShortcuts.ENTER, myEditor.getContentComponent());
      registerActionHandler(myEditor, IdeActions.ACTION_EDITOR_PASTE, new PasteHandler());
      registerActionHandler(myEditor, IdeActions.ACTION_EDITOR_BACKSPACE, new BackSpaceHandler());
      registerActionHandler(myEditor, IdeActions.ACTION_EDITOR_DELETE, new DeleteHandler());
      registerActionHandler(myEditor, IdeActions.ACTION_EDITOR_TAB, new TabHandler());

      registerActionHandler(myEditor, EOFAction.ACTION_ID);
    }
  }

  private static void registerActionHandler(@NotNull Editor editor, @NotNull String actionId) {
    AnAction action = ActionManager.getInstance().getAction(actionId);
    action.registerCustomShortcutSet(action.getShortcutSet(), editor.getContentComponent());
  }

  private static void registerActionHandler(@NotNull Editor editor, @NotNull String actionId, @NotNull AnAction action) {
    final Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
    final Shortcut[] shortcuts = keymap.getShortcuts(actionId);
    action.registerCustomShortcutSet(new CustomShortcutSet(shortcuts), editor.getContentComponent());
  }

  private void popupInvoked(@NotNull MouseEvent mouseEvent) {
    final ActionManager actionManager = ActionManager.getInstance();
    final HyperlinkInfo info = myHyperlinks != null ? myHyperlinks.getHyperlinkInfoByPoint(mouseEvent.getPoint()) : null;
    ActionGroup group = null;
    if (info instanceof HyperlinkWithPopupMenuInfo) {
      group = ((HyperlinkWithPopupMenuInfo)info).getPopupMenuGroup(mouseEvent);
    }
    if (group == null) {
      group = (ActionGroup)actionManager.getAction(CONSOLE_VIEW_POPUP_MENU);
    }
    final ConsoleActionsPostProcessor[] postProcessors = Extensions.getExtensions(ConsoleActionsPostProcessor.EP_NAME);
    AnAction[] result = group.getChildren(null);

    for (ConsoleActionsPostProcessor postProcessor : postProcessors) {
      result = postProcessor.postProcessPopupActions(this, result);
    }
    final DefaultActionGroup processedGroup = new DefaultActionGroup(result);
    final ActionPopupMenu menu = actionManager.createActionPopupMenu(ActionPlaces.EDITOR_POPUP, processedGroup);
    menu.getComponent().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
  }

  private void highlightHyperlinksAndFoldings(int startLine) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    boolean canHighlightHyperlinks = !myFilters.isEmpty();

    if (!canHighlightHyperlinks && myUpdateFoldingsEnabled) {
      return;
    }
    DocumentEx document = myEditor.getDocument();
    if (document.getTextLength() == 0) return;

    int endLine = Math.max(0, document.getLineCount() - 1);

    if (canHighlightHyperlinks) {
      myHyperlinks.highlightHyperlinks(myFilters, startLine, endLine);
    }

    if (myAllowHeavyFilters && myFilters.isAnyHeavy() && myFilters.shouldRunHeavy()) {
      runHeavyFilters(startLine, endLine);
    }
    if (myUpdateFoldingsEnabled) {
      updateFoldings(startLine, endLine);
    }
  }

  @SuppressWarnings("WeakerAccess") // Used in Rider
  public void rehighlightHyperlinksAndFoldings() {
    if (myEditor == null || myProject.isDisposed()) return;

    clearHyperlinkAndFoldings();
    highlightHyperlinksAndFoldings(0);
  }

  private void runHeavyFilters(int line1, int endLine) {
    final int startLine = Math.max(0, line1);

    final Document document = myEditor.getDocument();
    final int startOffset = document.getLineStartOffset(startLine);
    String text = document.getText(new TextRange(startOffset, document.getLineEndOffset(endLine)));
    final Document documentCopy = new DocumentImpl(text,true);
    documentCopy.setReadOnly(true);

    myJLayeredPane.startUpdating();
    final int currentValue = myHeavyUpdateTicket;
    myHeavyAlarm.addRequest(() -> {
        if (!myFilters.shouldRunHeavy()) return;
        try {
          myFilters.applyHeavyFilter(documentCopy, startOffset, startLine, additionalHighlight ->
              addFlushRequest(0, new FlushRunnable() {
                @Override
                public void doRun() {
                  if (myHeavyUpdateTicket != currentValue) return;
                  TextAttributes additionalAttributes = additionalHighlight.getTextAttributes(null);
                  if (additionalAttributes != null) {
                    ResultItem item = additionalHighlight.getResultItems().get(0);
                    myHyperlinks.addHighlighter(item.getHighlightStartOffset(), item.getHighlightEndOffset(), additionalAttributes);
                  }
                  else {
                    myHyperlinks.highlightHyperlinks(additionalHighlight, 0);
                  }
                }

                @Override
                public boolean equals(Object o) {
                  return this == o;
                }
              })
          );
        }
        finally {
          if (myHeavyAlarm.getActiveRequestCount() <= 1) { // only the current request
            UIUtil.invokeLaterIfNeeded(() -> myJLayeredPane.finishUpdating());
          }
        }
    }, 0);
  }

  private void updateFoldings(final int startLine, final int endLine) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myEditor.getFoldingModel().runBatchFoldingOperation(() -> {
      if (myEditor == null || myEditor.isDisposed()) {
        return;
      }
      final Document document = myEditor.getDocument();
      final CharSequence chars = document.getCharsSequence();
      for (int line = Math.max(0, startLine); line <= endLine; line++) {
        boolean flushOnly = line == endLine;
        /*
        Grep Console plugin allows to fold empty lines. We need to handle this case in a special way.

        Multiple lines are grouped into one folding, but to know when you can create the folding,
        you need a line which does not belong to that folding.
        When a new line, or a chunk of lines is printed, #addFolding is called for that lines + for an empty string
        (which basically does only one thing, gets a folding displayed).
        We do not want to process that empty string, but also we do not want to wait for another line
        which will create and display the folding - we'd see an unfolded stacktrace until another text came and flushed it.
        So therefore the condition, the last line(empty string) should still flush, but not be processed by
        com.intellij.execution.ConsoleFolding.
         */
        addFolding(document, chars, line, flushOnly);
      }
    });
  }

  private void addFolding(@NotNull Document document,
                          @NotNull CharSequence chars,
                          int line,
                          boolean flushOnly) {
    ConsoleFolding current = null;
    if (!flushOnly) {
      String commandLinePlaceholder = myCommandLineFolding.getPlaceholder(line);
      if (commandLinePlaceholder != null) {
        FoldRegion region = myEditor.getFoldingModel().addFoldRegion(document.getLineStartOffset(line), document.getLineEndOffset(line), commandLinePlaceholder);
        if (region != null) {
          region.setExpanded(false);
        }
        return;
      }
      String lineText = EditorHyperlinkSupport.getLineText(document, line, false);
      current = foldingForLine(lineText, getProject());
      if (current != null) {
        myFolding.put(line, current);
      }
    }

    // group equal foldings for previous lines into one huge folding
    final ConsoleFolding prevFolding = myFolding.get(line - 1);
    if (prevFolding != null && !prevFolding.equals(current)) {
      final int lEnd = line - 1;
      int lStart = lEnd;
      while (prevFolding.equals(myFolding.get(lStart - 1))) lStart--;

      for (int i = lStart; i <= lEnd; i++) {
        myFolding.remove(i);
      }

      List<String> toFold = new ArrayList<>(lEnd - lStart + 1);
      for (int i = lStart; i <= lEnd; i++) {
        toFold.add(EditorHyperlinkSupport.getLineText(document, i, false));
      }

      int oStart = document.getLineStartOffset(lStart);
      if (oStart > 0) oStart--;
      int oEnd = CharArrayUtil.shiftBackward(chars, document.getLineEndOffset(lEnd) - 1, " \t") + 1;

      String placeholder = prevFolding.getPlaceholderText(getProject(), toFold);
      FoldRegion region = placeholder == null ? null : myEditor.getFoldingModel().addFoldRegion(oStart, oEnd, placeholder);
      if (region != null) {
        region.setExpanded(false);
      }
    }
  }

  @Nullable
  private static ConsoleFolding foldingForLine(@NotNull String lineText, @NotNull Project project) {
    ConsoleFolding[] extensions = ConsoleFolding.EP_NAME.getExtensions();
    for (ConsoleFolding extension : extensions) {
      if (extension.shouldFoldLine(project, lineText)) {
        return extension;
      }
    }
    return null;
  }

  public static class ClearAllAction extends DumbAwareAction {
    private final ConsoleView myConsoleView;

    @SuppressWarnings("unused") // in LangActions.xml
    public ClearAllAction() {
      this(null);
    }

    ClearAllAction(ConsoleView consoleView) {
      super(ExecutionBundle.message("clear.all.from.console.action.name"), "Clear the contents of the console", AllIcons.Actions.GC);
      myConsoleView = consoleView;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      boolean enabled = myConsoleView != null && myConsoleView.getContentSize() > 0;
      if (!enabled) {
        enabled = e.getData(LangDataKeys.CONSOLE_VIEW) != null;
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor != null && editor.getDocument().getTextLength() == 0) {
          enabled = false;
        }
      }
      e.getPresentation().setEnabled(enabled);
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
      final ConsoleView consoleView = myConsoleView != null ? myConsoleView : e.getData(LangDataKeys.CONSOLE_VIEW);
      if (consoleView != null) {
        consoleView.clear();
      }
    }
  }

  // finds range marker the [offset..offset+1) belongs to
  private RangeMarker findTokenMarker(int offset) {
    RangeMarker[] marker = new RangeMarker[1];
    MarkupModelEx model = (MarkupModelEx)DocumentMarkupModel.forDocument(myEditor.getDocument(), getProject(), true);
    model.processRangeHighlightersOverlappingWith(offset, offset, m->{
      if (getTokenType(m) == null || m.getStartOffset() > offset || offset + 1 > m.getEndOffset()) return true;
      marker[0] = m;
      return false;
    });

    return marker[0];
  }

  private static ConsoleViewContentType getTokenType(@Nullable RangeMarker m) {
    return m == null ? null : m.getUserData(CONTENT_TYPE);
  }

  private static class MyTypedHandler extends TypedActionHandlerBase {
    private MyTypedHandler(final TypedActionHandler originalAction) {
      super(originalAction);
    }

    @Override
    public void execute(@NotNull final Editor editor, final char charTyped, @NotNull final DataContext dataContext) {
      final ConsoleViewImpl consoleView = editor.getUserData(CONSOLE_VIEW_IN_EDITOR_VIEW);
      if (consoleView == null || !consoleView.myState.isRunning() || consoleView.myIsViewer) {
        if (myOriginalHandler != null) {
          myOriginalHandler.execute(editor, charTyped, dataContext);
        }
        return;
      }
      final String text = String.valueOf(charTyped);
      consoleView.type(editor, text);
    }
  }

  private void type(@NotNull Editor editor, @NotNull String text) {
    flushDeferredText();
    SelectionModel selectionModel = editor.getSelectionModel();

    int lastOffset = selectionModel.hasSelection() ? selectionModel.getSelectionStart() : editor.getCaretModel().getOffset() - 1;
    RangeMarker marker = findTokenMarker(lastOffset);
    if (getTokenType(marker) != ConsoleViewContentType.USER_INPUT) {
      print(text, ConsoleViewContentType.USER_INPUT);
      moveScrollRemoveSelection(editor, editor.getDocument().getTextLength());
      return;
    }

    String textToUse = StringUtil.convertLineSeparators(text);
    int typeOffset;
    if (selectionModel.hasSelection()) {
      Document document = editor.getDocument();
      int start = selectionModel.getSelectionStart();
      int end = selectionModel.getSelectionEnd();
      document.deleteString(start, end);
      selectionModel.removeSelection();
      typeOffset = end;
    }
    else {
      typeOffset = selectionModel.hasSelection() ? selectionModel.getSelectionStart() : editor.getCaretModel().getOffset();
    }
    insertUserText(typeOffset, textToUse);
  }

  private abstract static class ConsoleAction extends AnAction implements DumbAware {
    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
      ApplicationManager.getApplication().assertIsDispatchThread();
      DataContext context = e.getDataContext();
      ConsoleViewImpl console = getRunningConsole(context);
      if (console != null) {
        execute(console, context);
      }
    }

    protected abstract void execute(@NotNull ConsoleViewImpl console, @NotNull DataContext context);

    @Override
    public void update(@NotNull final AnActionEvent e) {
      final ConsoleViewImpl console = getRunningConsole(e.getDataContext());
      e.getPresentation().setEnabled(console != null);
    }

    @Nullable
    private static ConsoleViewImpl getRunningConsole(@NotNull DataContext context) {
      final Editor editor = CommonDataKeys.EDITOR.getData(context);
      if (editor != null) {
        final ConsoleViewImpl console = editor.getUserData(CONSOLE_VIEW_IN_EDITOR_VIEW);
        if (console != null && console.myState.isRunning()) {
          return console;
        }
      }
      return null;
    }
  }

  private static class EnterHandler extends ConsoleAction {
    @Override
    public void execute(@NotNull final ConsoleViewImpl consoleView, @NotNull final DataContext context) {
      consoleView.print("\n", ConsoleViewContentType.USER_INPUT);
      consoleView.flushDeferredText();
      Editor editor = consoleView.myEditor;
      moveScrollRemoveSelection(editor, editor.getDocument().getTextLength());
    }
  }

  private static class PasteHandler extends ConsoleAction {
    @Override
    public void execute(@NotNull final ConsoleViewImpl consoleView, @NotNull final DataContext context) {
      String text = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
      if (text == null) return;
      Editor editor = consoleView.myEditor;
      consoleView.type(editor, text);
    }
  }

  private static class BackSpaceHandler extends ConsoleAction {
    @Override
    public void execute(@NotNull final ConsoleViewImpl consoleView, @NotNull final DataContext context) {
      final Editor editor = consoleView.myEditor;

      if (IncrementalSearchHandler.isHintVisible(editor)) {
        getDefaultActionHandler().execute(editor, context);
        return;
      }

      final Document document = editor.getDocument();
      final int length = document.getTextLength();
      if (length == 0) {
        return;
      }

      SelectionModel selectionModel = editor.getSelectionModel();
      if (selectionModel.hasSelection()) {
        consoleView.deleteUserText(selectionModel.getSelectionStart(),
                                   selectionModel.getSelectionEnd() - selectionModel.getSelectionStart());
      }
      else if (editor.getCaretModel().getOffset() > 0) {
        consoleView.deleteUserText(editor.getCaretModel().getOffset() - 1, 1);
      }
    }

    private static EditorActionHandler getDefaultActionHandler() {
      return EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_BACKSPACE);
    }
  }

  private static class DeleteHandler extends ConsoleAction {
    @Override
    public void execute(@NotNull final ConsoleViewImpl consoleView, @NotNull final DataContext context) {
      final Editor editor = consoleView.myEditor;

      if (IncrementalSearchHandler.isHintVisible(editor)) {
        getDefaultActionHandler().execute(editor, context);
        return;
      }

      consoleView.flushDeferredText();
      final Document document = editor.getDocument();
      final int length = document.getTextLength();
      if (length == 0) {
        return;
      }

      SelectionModel selectionModel = editor.getSelectionModel();
      if (selectionModel.hasSelection()) {
        consoleView.deleteUserText(selectionModel.getSelectionStart(),
                                   selectionModel.getSelectionEnd() - selectionModel.getSelectionStart());
      }
      else {
        consoleView.deleteUserText(editor.getCaretModel().getOffset(), 1);
      }
    }

    private static EditorActionHandler getDefaultActionHandler() {
      return EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_BACKSPACE);
    }
  }

  private static class TabHandler extends ConsoleAction {
    @Override
    protected void execute(@NotNull ConsoleViewImpl console, @NotNull DataContext context) {
      console.type(console.myEditor, "\t");
    }
  }

  @Override
  public JComponent getPreferredFocusableComponent() {
    //ensure editor created
    getComponent();
    return myEditor.getContentComponent();
  }


  // navigate up/down in stack trace
  @Override
  public boolean hasNextOccurence() {
    return calcNextOccurrence(1) != null;
  }

  @Override
  public boolean hasPreviousOccurence() {
    return calcNextOccurrence(-1) != null;
  }

  @Override
  public OccurenceInfo goNextOccurence() {
    return calcNextOccurrence(1);
  }

  @Nullable
  protected OccurenceInfo calcNextOccurrence(final int delta) {
    if (myHyperlinks == null) {
      return null;
    }

    return EditorHyperlinkSupport.getNextOccurrence(myEditor, delta, next -> {
      int offset = next.getStartOffset();
      scrollTo(offset);
      final HyperlinkInfo hyperlinkInfo = EditorHyperlinkSupport.getHyperlinkInfo(next);
      if (hyperlinkInfo instanceof BrowserHyperlinkInfo) {
        return;
      }
      if (hyperlinkInfo instanceof HyperlinkInfoBase) {
        VisualPosition position = myEditor.offsetToVisualPosition(offset);
        Point point = myEditor.visualPositionToXY(new VisualPosition(position.getLine() + 1, position.getColumn()));
        ((HyperlinkInfoBase)hyperlinkInfo).navigate(myProject, new RelativePoint(myEditor.getContentComponent(), point));
      }
      else if (hyperlinkInfo != null) {
        hyperlinkInfo.navigate(myProject);
      }
    });
  }

  @Override
  public OccurenceInfo goPreviousOccurence() {
    return calcNextOccurrence(-1);
  }

  @Override
  public String getNextOccurenceActionName() {
    return ExecutionBundle.message("down.the.stack.trace");
  }

  @Override
  public String getPreviousOccurenceActionName() {
    return ExecutionBundle.message("up.the.stack.trace");
  }

  public void addCustomConsoleAction(@NotNull AnAction action) {
    customActions.add(action);
  }

  @Override
  @NotNull
  public AnAction[] createConsoleActions() {
    //Initializing prev and next occurrences actions
    final CommonActionsManager actionsManager = CommonActionsManager.getInstance();
    final AnAction prevAction = actionsManager.createPrevOccurenceAction(this);
    prevAction.getTemplatePresentation().setText(getPreviousOccurenceActionName());
    final AnAction nextAction = actionsManager.createNextOccurenceAction(this);
    nextAction.getTemplatePresentation().setText(getNextOccurenceActionName());

    final AnAction switchSoftWrapsAction = new ToggleUseSoftWrapsToolbarAction(SoftWrapAppliancePlaces.CONSOLE) {
      @Override
      protected Editor getEditor(AnActionEvent e) {
        return myEditor;
      }
    };
    final AnAction autoScrollToTheEndAction = new ScrollToTheEndToolbarAction(myEditor);

    //Initializing custom actions
    final AnAction[] consoleActions = new AnAction[6 + customActions.size()];
    consoleActions[0] = prevAction;
    consoleActions[1] = nextAction;
    consoleActions[2] = switchSoftWrapsAction;
    consoleActions[3] = autoScrollToTheEndAction;
    consoleActions[4] = ActionManager.getInstance().getAction("Print");
    consoleActions[5] = new ClearAllAction(this);
    for (int i = 0; i < customActions.size(); ++i) {
      consoleActions[i + 6] = customActions.get(i);
    }
    ConsoleActionsPostProcessor[] postProcessors = Extensions.getExtensions(ConsoleActionsPostProcessor.EP_NAME);
    AnAction[] result = consoleActions;
    for (ConsoleActionsPostProcessor postProcessor : postProcessors) {
      result = postProcessor.postProcess(this, result);
    }
    return result;
  }

  @Override
  public void allowHeavyFilters() {
    myAllowHeavyFilters = true;
  }

  @Override
  public void addChangeListener(@NotNull final ChangeListener listener, @NotNull final Disposable parent) {
    myListeners.add(listener);
    Disposer.register(parent, () -> myListeners.remove(listener));
  }

  private void insertUserText(int offset, @NotNull String text) {
    List<Pair<String, ConsoleViewContentType>> result = myInputMessageFilter.applyFilter(text, ConsoleViewContentType.USER_INPUT);
    if (result == null) {
      doInsertUserInput(offset, text);
    }
    else {
      for (Pair<String, ConsoleViewContentType> pair : result) {
        String chunkText = pair.getFirst();
        ConsoleViewContentType chunkType = pair.getSecond();
        if (chunkType.equals(ConsoleViewContentType.USER_INPUT)) {
          doInsertUserInput(offset, chunkText);
          offset += chunkText.length();
        }
        else {
          print(chunkText, chunkType, null);
        }
      }
    }
  }

  private void doInsertUserInput(int offset, @NotNull String text) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    final Editor editor = myEditor;
    final Document document = editor.getDocument();

    int oldDocLength = document.getTextLength();
    document.insertString(offset, text);
    int newStartOffset = Math.max(0,document.getTextLength() - oldDocLength + offset - text.length()); // take care of trim document
    int newEndOffset = document.getTextLength() - oldDocLength + offset; // take care of trim document

    if (findTokenMarker(newEndOffset) == null) {
      createTokenRangeHighlighter(ConsoleViewContentType.USER_INPUT, newStartOffset, newEndOffset);
    }

    moveScrollRemoveSelection(editor, newEndOffset);
    sendUserInput(text);
  }

  private static void moveScrollRemoveSelection(@NotNull Editor editor, int offset) {
    editor.getCaretModel().moveToOffset(offset);
    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    editor.getSelectionModel().removeSelection();
  }

  private void deleteUserText(int startOffset, int length) {
    final Editor editor = myEditor;
    final Document document = editor.getDocument();

    RangeMarker marker = findTokenMarker(startOffset);
    if (getTokenType(marker) != ConsoleViewContentType.USER_INPUT) return;

    int endOffset = startOffset + length;
    if (startOffset >= 0 && endOffset >= 0 && endOffset > startOffset) {
      document.deleteString(startOffset, endOffset);
    }
    moveScrollRemoveSelection(editor, startOffset);
  }

  public boolean isRunning() {
    return myState.isRunning();
  }

  /**
   * Command line used to launch application/test from idea may be quite long.
   * Hence, it takes many visual lines during representation if soft wraps are enabled
   * or, otherwise, takes many columns and makes horizontal scrollbar thumb too small.
   * <p/>
   * Our point is to fold such long command line and represent it as a single visual line by default.
   */
  private class CommandLineFolding extends ConsoleFolding {
    /**
     * Checks if target line should be folded and returns its placeholder if the examination succeeds.
     *
     * @param line index of line to check
     * @return placeholder text if given line should be folded; {@code null} otherwise
     */
    @Nullable
    private String getPlaceholder(int line) {
      if (myEditor == null || line != 0) {
        return null;
      }

      String text = EditorHyperlinkSupport.getLineText(myEditor.getDocument(), 0, false);
      // Don't fold the first line if the line is not that big.
      if (text.length() < 1000) {
        return null;
      }

      if (!myState.isCommandLine(text)) {
        return null;
      }
      
      int index = 0;
      if (text.charAt(0) == '"') {
        index = text.indexOf('"', 1) + 1;
      }
      if (index == 0) {
        for (boolean nonWhiteSpaceFound = false; index < text.length(); index++) {
          char c = text.charAt(index);
          if (c != ' ' && c != '\t') {
            nonWhiteSpaceFound = true;
            continue;
          }
          if (nonWhiteSpaceFound) {
            break;
          }
        }
      }
      assert index <= text.length();
      return text.substring(0, index) + " ...";
    }

    @Override
    public boolean shouldFoldLine(@NotNull Project project, @NotNull String line) {
      return false;
    }

    @Override
    public String getPlaceholderText(@NotNull Project project, @NotNull List<String> lines) {
      // Is not expected to be called.
      return "<...>";
    }
  }

  private class FlushRunnable implements Runnable {
    @Override
    public final void run() {
      // flush requires UndoManger/CommandProcessor properly initialized
      if (!isDisposed() && !StartupManagerEx.getInstanceEx(myProject).startupActivityPassed()) {
        addFlushRequest(DEFAULT_FLUSH_DELAY, FLUSH);
      }
      synchronized (myCurrentRequests) {
        myCurrentRequests.remove(this);
      }
      doRun();
    }

    protected void doRun() {
      flushDeferredText();
    }

    // by default all instances of the same class are treated equal
    @Override
    public boolean equals(Object o) {
      return this == o || o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
      return getClass().hashCode();
    }
  }
  private final FlushRunnable FLUSH = new FlushRunnable();

  private final class ClearRunnable extends FlushRunnable {
    @Override
    public void doRun() {
      doClear();
    }
  }
  private final ClearRunnable CLEAR = new ClearRunnable();

  @NotNull
  public Project getProject() {
    return myProject;
  }

  private class HyperlinkNavigationAction extends DumbAwareAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      Runnable runnable = myHyperlinks.getLinkNavigationRunnable(myEditor.getCaretModel().getLogicalPosition());
      assert runnable != null;
      runnable.run();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(myHyperlinks.getLinkNavigationRunnable(myEditor.getCaretModel().getLogicalPosition()) != null);
    }
  }

  @NotNull
  public String getText() {
    return myEditor.getDocument().getText();
  }
}

