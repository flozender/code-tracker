package com.intellij.openapi.fileEditor.impl;

import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diff.DiffManager;
import com.intellij.openapi.diff.DocumentContent;
import com.intellij.openapi.diff.SimpleContent;
import com.intellij.openapi.diff.SimpleDiffRequest;
import com.intellij.openapi.diff.ex.DiffPanelOptions;
import com.intellij.openapi.diff.impl.DiffPanelImpl;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditReadOnlyListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.fileEditor.VetoDocumentReloadException;
import com.intellij.openapi.fileEditor.VetoDocumentSavingException;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.PsiExternalChangeAction;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.impl.PsiManagerConfiguration;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.ui.UIBundle;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PendingEventDispatcher;
import com.intellij.util.text.CharArrayCharSequence;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileDocumentManagerImpl extends FileDocumentManager implements ApplicationComponent, VirtualFileListener {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl");

  private static final Key<String> DETECTED_LINE_SEPARATOR_KEY = Key.create("DETECTED_LINE_SEPARATOR_KEY");
  private static final Key<String> LINE_SEPARATOR_KEY = Key.create("LINE_SEPARATOR_KEY");
  private static final Key<WeakReference<Document>> DOCUMENT_KEY = Key.create("DOCUMENT_KEY");
  private static final Key<VirtualFile> FILE_KEY = Key.create("FILE_KEY");

  private Set<Document> myUnsavedDocuments = new HashSet<Document>();
  private EditReadOnlyListener myReadOnlyListener = new MyEditReadOnlyListener();

  private ProjectEx myDummyProject = null;
  private boolean myDummyProjectInitialized = false;
  private Object myDummyProjectInitializationLock = new Object();

  private PendingEventDispatcher<FileDocumentManagerListener> myEventDispatcher = PendingEventDispatcher.create(FileDocumentManagerListener.class);
  private final PsiManagerConfiguration myPsiManagerConfiguration;
  private final ProjectManagerEx myProjectManagerEx;
  private VirtualFileManager myVirtualFileManager;


  public FileDocumentManagerImpl(VirtualFileManager virtualFileManager,
                                 PsiManagerConfiguration psiManagerConfiguration,
                                 ProjectManagerEx projectManagerEx) {
    myPsiManagerConfiguration = psiManagerConfiguration;
    myProjectManagerEx = projectManagerEx;
    myVirtualFileManager = virtualFileManager;

    myVirtualFileManager.addVirtualFileListener(this);
    this.addFileDocumentManagerListener(new TrailingSpacesStripper());
  }

  public String getComponentName() {
    return "FileDocumentManager";
  }

  public void initComponent() {
  }

  public void disposeComponent() {
    if (myDummyProject != null) {
      myDummyProject.dispose();
    }
  }

  public Document getDocument(VirtualFile file) {
    DocumentEx document = (DocumentEx)getCachedDocument(file);
    if (document == null){
      final CharSequence text = loadText(file);
      if (text == null) return null;
      document = (DocumentEx)createDocument(text);
      document.setModificationStamp(file.getModificationStamp());
      final FileType fileType = FileTypeManager.getInstance().getFileTypeByFile(file);
      document.setReadOnly(!file.isWritable() || fileType.isBinary());
      file.putUserData(DOCUMENT_KEY, new WeakReference<Document>(document));
      document.putUserData(FILE_KEY, file);
      document.addDocumentListener(
        new DocumentAdapter() {
          public void documentChanged(DocumentEvent e) {
            final Document document = e.getDocument();
            myUnsavedDocuments.add(document);
            final Runnable currentCommand = CommandProcessor.getInstance().getCurrentCommand();
            Project project = currentCommand != null ? CommandProcessor.getInstance().getCurrentCommandProject() : null;
            String lineSeparator = CodeStyleSettingsManager.getSettings(project).getLineSeparator();
            document.putUserData(LINE_SEPARATOR_KEY, lineSeparator);
          }
        }
      );
      document.addEditReadOnlyListener(myReadOnlyListener);

      try {
        fireFileContentLoaded(file, document);
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }

    return document;
  }

  private static Document createDocument(final CharSequence text) {
    return EditorFactory.getInstance().createDocument(text);
  }

  private CharSequence loadText(VirtualFile file) {
    if (file.isDirectory()) return null;
    final FileType fileType = FileTypeManager.getInstance().getFileTypeByFile(file);

    if (fileType.equals(StdFileTypes.CLASS)){
      return new CharArrayCharSequence(decompile(file));
    }

    if (fileType.isBinary()) return null;

    final String[] detectedLineSeparator = new String[1];
    final CharSequence result = LoadTextUtil.loadText(file, detectedLineSeparator);
    file.putUserData(DETECTED_LINE_SEPARATOR_KEY, detectedLineSeparator[0]);
    return result;
  }

  private char[] decompile(VirtualFile file) {
    try{
      final ProjectEx dummyProject = getDummyProject();
      PsiManager manager = PsiManager.getInstance(dummyProject);
      final String text = ClsFileImpl.decompile(manager, file);

      PsiFile mirror = manager.getElementFactory().createFileFromText("test.java", text);

      CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(dummyProject); // do not use project's code style!
      CodeStyleSettings settings = CodeStyleSettingsManager.getSettings(dummyProject);
      boolean saved = settings.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE;
      settings.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = true;
      codeStyleManager.shortenClassReferences(mirror);
      codeStyleManager.reformat(mirror);
      settings.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = saved;

      return mirror.textToCharArray();
    }
    catch(IncorrectOperationException e){
      LOG.error(e);
      return null;
    }
  }

  public Document getCachedDocument(VirtualFile file) {
    WeakReference<Document> reference = file.getUserData(DOCUMENT_KEY);
    Document document = reference != null ? (Document)reference.get() : null;

    if (document != null && isFileBecameBinary(file)){
      file.putUserData(DOCUMENT_KEY, null);
      document.putUserData(FILE_KEY, null);
      return null;
    }

    return document;
  }

  private boolean isFileBecameBinary(VirtualFile file) {
    final FileType fileType = FileTypeManager.getInstance().getFileTypeByFile(file);
    return fileType.isBinary() && !fileType.equals(StdFileTypes.CLASS);
  }

  public VirtualFile getFile(Document document) {
    return document.getUserData(FILE_KEY);
  }

  public void dropAllUnsavedDocuments() {
    if (!ApplicationManager.getApplication().isUnitTestMode()){
      throw new RuntimeException("This method is only for test mode!");
    }
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    myUnsavedDocuments.clear();
  }

  public void saveAllDocuments() {
    if (myUnsavedDocuments.isEmpty()) return;

    HashSet<Document> failedToSave = new HashSet<Document>();
    while(true){
      final Document[] unsavedDocuments = getUnsavedDocuments();

      int count = 0;
      for (Document document : unsavedDocuments) {
        if (failedToSave.contains(document)) continue;
        saveDocument(document);
        count++;
        if (myUnsavedDocuments.contains(document)) {
          failedToSave.add(document);
        }
      }

      if (count == 0) break;
    }
  }

  public void saveDocument(final Document document) {
    if (!myUnsavedDocuments.contains(document)) return;

    ApplicationManager.getApplication().runWriteAction(
      new Runnable() {
        public void run() {
          _saveDocument(document);
        }
      }
    );
  }

  private void _saveDocument(final Document document) {
    boolean commited = false;
    try{
      Writer writer = null;
      VirtualFile file = getFile(document);

      if (file == null || !file.isValid()){
        myUnsavedDocuments.remove(document);
        LOG.assertTrue(!myUnsavedDocuments.contains(document));
        return;
      }

      if (file.getModificationStamp() == document.getModificationStamp()){
        myUnsavedDocuments.remove(document);
        LOG.assertTrue(!myUnsavedDocuments.contains(document));
        return;
      }

      if (file.getTimeStamp() != file.getActualTimeStamp()){
        file.refresh(false, false);
        if (!myUnsavedDocuments.contains(document)) return;
        if (!file.isValid()) return;
      }

      try {
        myEventDispatcher.getMulticaster().beforeDocumentSaving(document);
      }
      catch (VetoDocumentSavingException e) {
        return;
      }

      LOG.assertTrue(file.isValid());

      try{
        String text = document.getText();
        String lineSeparator = getLineSeparator(document, file);
        if (!lineSeparator.equals("\n")){
          text = StringUtil.convertLineSeparators(text, lineSeparator);
        }
        writer = file.getWriter(this, document.getModificationStamp(), -1);
        writer.write(text);
        commited = true;
      }
      finally{
        if (writer != null){
          writer.close();
        }
      }
    }
    catch(IOException e){
      reportErrorOnSave(e);
      commited = false;
    }
    finally{
      if (commited){
        myUnsavedDocuments.remove(document);
        LOG.assertTrue(!myUnsavedDocuments.contains(document));
      }

    }
  }

  public static String getLineSeparator(Document document, VirtualFile file) {
    String lineSeparator = file.getUserData(DETECTED_LINE_SEPARATOR_KEY);
    if (lineSeparator == null){
      lineSeparator = document.getUserData(LINE_SEPARATOR_KEY);
    }
    return lineSeparator;
  }

  public String getLineSeparator(VirtualFile file, Project project) {
    String lineSeparator = file != null ? file.getUserData(DETECTED_LINE_SEPARATOR_KEY) : null;
    if (lineSeparator == null) {
      CodeStyleSettingsManager settingsManager = project == null
                                                 ? CodeStyleSettingsManager.getInstance()
                                                 : CodeStyleSettingsManager.getInstance(project);
      return settingsManager.getCurrentSettings().getLineSeparator();
    }
    else {
      return lineSeparator;
    }
  }


  public Document[] getUnsavedDocuments() {
    return myUnsavedDocuments.toArray(new Document[myUnsavedDocuments.size()]);
  }

  public boolean isDocumentUnsaved(Document document) {
    return myUnsavedDocuments.contains(document);
  }

  public void addFileDocumentManagerListener(FileDocumentManagerListener listener) {
    myEventDispatcher.addListener(listener);
  }

  public void removeFileDocumentManagerListener(FileDocumentManagerListener listener) {
    myEventDispatcher.removeListener(listener);
  }

  public void dispatchPendingEvents(FileDocumentManagerListener listener) {
    if (!myEventDispatcher.isDispatching()) {
      myVirtualFileManager.dispatchPendingEvent(this);
    }
    myEventDispatcher.dispatchPendingEvent(listener);
  }

  public void propertyChanged(final VirtualFilePropertyEvent event) {
    if (VirtualFile.PROP_WRITABLE.equals(event.getPropertyName())){
      final VirtualFile file = event.getFile();
      final Document document = getCachedDocument(file);
      if (document == null) return;

      ApplicationManager.getApplication().runWriteAction(
        new PsiExternalChangeAction() {
          public void run() {
            document.setReadOnly(!event.getFile().isWritable());
          }
        }
      );
      //myUnsavedDocuments.remove(document); //?
    }
  }

  public void contentsChanged(VirtualFileEvent event) {
    if (event.getRequestor() == this) return;
    final VirtualFile file = event.getFile();
    final Document document = getCachedDocument(file);
    if (document == null) {
      myEventDispatcher.getMulticaster().fileWithNoDocumentChanged(file);
      return;
    }

    long documentStamp = document.getModificationStamp();
    long oldFileStamp = event.getOldModificationStamp();
    if (documentStamp != oldFileStamp){
      LOG.info("reaload from disk?");
      LOG.info("  documentStamp:" + documentStamp);
      LOG.info("  oldFileStamp:" + oldFileStamp);

      Runnable askReloadRunnable = new Runnable() {
        public void run() {
          if (!file.isValid()) return;
          if (askReloadFromDisk(file, document)){
            reloadFromDisk(document);
          }
        }
      };
      // now can show dialogs in write actions...
      //if (!ApplicationManagerEx.getApplicationEx().isUnitTestMode()){
      //  LaterInvocatorEx.invokeLater(askReloadRunnable);
      //}
      //else{
        askReloadRunnable.run();
      //}
    }
    else{
      reloadFromDisk(document);
    }
  }

  public void reloadFromDisk(final Document document) {
    final VirtualFile file = getFile(document);
    try {
      fireBeforeFileContentReload(file, document);
    }
    catch (VetoDocumentReloadException e) {
      return;
    }
    catch (Exception e) {
      LOG.error(e);
    }

    ApplicationManager.getApplication().runWriteAction(
      new PsiExternalChangeAction() {
        public void run() {
          boolean wasWritable = document.isWritable();
          DocumentEx documentEx = (DocumentEx)document;
          documentEx.setReadOnly(false);
          documentEx.replaceText(loadText(file), file.getModificationStamp());
          documentEx.setReadOnly(!wasWritable);
        }
      }
    );
    myUnsavedDocuments.remove(document);      

    try {
      fireFileContentReloaded(file, document);
    }
    catch (Exception e) {
      LOG.error(e);
    }
  }

// Made protected for Fabrique
  protected boolean askReloadFromDisk(final VirtualFile file, final Document document) {
    String message = UIBundle.message("file.cache.conflict.message.text", file.getPresentableUrl());
    if (ApplicationManager.getApplication().isUnitTestMode()) throw new RuntimeException(message);
    final DialogBuilder builder = new DialogBuilder((Project)null);
    builder.setCenterPanel(new JLabel(message, Messages.getQuestionIcon(), SwingUtilities.TRAILING));
    builder.addOkAction().setText(UIBundle.message("file.cache.conflict.load.fs.changes.button"));
    builder.addCancelAction().setText(UIBundle.message("file.cache.conflict.keep.memory.changes.button"));
    builder.addAction(new AbstractAction(UIBundle.message("file.cache.conflict.show.difference.button")){
      public void actionPerformed(ActionEvent e) {
        String windowtitle = UIBundle.message("file.cache.conflict.for.file.dialog.title", file.getPresentableUrl());
        SimpleDiffRequest request = new SimpleDiffRequest(getDummyProject(), windowtitle);
        FileType fileType = FileTypeManager.getInstance().getFileTypeByFile(file);
        String fsContent = loadText(file).toString();
        request.setContents(new SimpleContent(fsContent, fileType),
                            new DocumentContent(myDummyProject, document, fileType));
        request.setContentTitles(UIBundle.message("file.cache.conflict.diff.content.file.system.content"),
                                 UIBundle.message("file.cache.conflict.diff.content.memory.content"));
        DialogBuilder diffBuidler = new DialogBuilder(getDummyProject());
        DiffPanelImpl diffPanel = (DiffPanelImpl)DiffManager.getInstance().createDiffPanel(diffBuidler.getWindow(), getDummyProject());
        diffPanel.getOptions().setShowSourcePolicy(DiffPanelOptions.ShowSourcePolicy.DONT_SHOW);
        diffBuidler.setCenterPanel(diffPanel.getComponent());
        diffPanel.setDiffRequest(request);
        diffBuidler.addOkAction().setText(UIBundle.message("file.cache.conflict.save.changes.button"));
        diffBuidler.addCancelAction();
        diffBuidler.setTitle(windowtitle);
        if (diffBuidler.show() == DialogWrapper.OK_EXIT_CODE)
          builder.getDialogWrapper().close(DialogWrapper.CANCEL_EXIT_CODE);
      }
    });
    //int option = Messages.showYesNoDialog(message, "File Cache Conflict", Messages.getQuestionIcon());
    builder.setTitle(UIBundle.message("file.cache.conflict.dialog.title"));
    builder.setButtonsAlignment(SwingUtilities.CENTER);
    return builder.show() == 0;
    //return option == 0;
  }

  protected void reportErrorOnSave(final IOException e) {
    // invokeLater here prevents attempt to show dialog in write action
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        Messages.showMessageDialog(
          UIBundle.message("cannot.save.file.with.error.error.message", e.getMessage()),
          UIBundle.message("cannot.save.file.dialog.title"),
          Messages.getErrorIcon()
        );
      }
    });
  }

  public void fileCreated(VirtualFileEvent event) {
  }

  public void fileDeleted(VirtualFileEvent event) {
    //todo clear document/file correspondence?
  }

  public void fileMoved(VirtualFileMoveEvent event) {
  }

  public void beforePropertyChange(VirtualFilePropertyEvent event) {
  }

  public void beforeContentsChange(VirtualFileEvent event) {
  }

  public void beforeFileDeletion(VirtualFileEvent event) {
  }

  public void beforeFileMovement(VirtualFileMoveEvent event) {
  }

  public ProjectEx getDummyProject() {
    synchronized (myDummyProjectInitializationLock) {
      if (!myDummyProjectInitialized) {
        myDummyProjectInitialized = true;

        if (myPsiManagerConfiguration.CREATE_DUMMY_PROJECT_FOR_OBFUSCATION) {
          myDummyProject = (ProjectEx)myProjectManagerEx.newProject("", false, true);
          ((StartupManagerImpl)StartupManager.getInstance(myDummyProject)).runStartupActivities();
        }
      }
    }
    return myDummyProject;
  }

  private final class MyEditReadOnlyListener implements EditReadOnlyListener {
    public void readOnlyModificationAttempt(Document document) {
      VirtualFile file = getFile(document);
      if (file == null) return;
      myVirtualFileManager.fireReadOnlyModificationAttempt(new VirtualFile[]{file});
    }
  }

  private void fireFileContentReloaded(final VirtualFile file, final Document document) {
    List<FileDocumentManagerListener> listeners = myEventDispatcher.getListeners();
    for (int i = 0; i < listeners.size(); i++) {
      FileDocumentManagerListener listener = listeners.get(i);
      try {
        listener.fileContentReloaded(file, document);
      }
      catch (AbstractMethodError e) {
        // Do nothing. Some listener just does not implement this method yet.
      }
    }
  }

  private void fireBeforeFileContentReload(final VirtualFile file, final Document document) throws VetoDocumentReloadException {
    List<FileDocumentManagerListener> listeners = myEventDispatcher.getListeners();
    for (int i = 0; i < listeners.size(); i++) {
      FileDocumentManagerListener listener = listeners.get(i);
      try {
        listener.beforeFileContentReload(file, document);
      }
      catch (AbstractMethodError e) {
        // Do nothing. Some listener just does not implement this method yet.
      }
    }
  }

  private void fireFileContentLoaded(VirtualFile file, DocumentEx document) {
    List<FileDocumentManagerListener> listeners = myEventDispatcher.getListeners();
    for (int i = 0; i < listeners.size(); i++) {
      FileDocumentManagerListener listener = listeners.get(i);
      try {
        listener.fileContentLoaded(file, document);
      }
      catch (AbstractMethodError e) {
        // Do nothing. Some listener just does not implement this method yet.
      }
    }
  }
}
