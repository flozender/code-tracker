package com.intellij.openapi.vcs.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorGutterAction;
import com.intellij.openapi.editor.TextAnnotationGutterProvider;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.localVcs.UpToDateLineNumberProvider;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.annotate.*;
import com.intellij.openapi.vcs.impl.UpToDateLineNumberProviderImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * author: lesya
 */
public class AnnotateToggleAction extends ToggleAction {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.vcs.actions.AnnotateToggleAction");
  protected static final Key<Collection<AnnotationFieldGutter>> KEY_IN_EDITOR = Key.create("Annotations");

  public void update(AnActionEvent e) {
    e.getPresentation().setEnabled(isEnabled(VcsContextFactory.SERVICE.getInstance().createContextOn(e)));
  }

  private static boolean isEnabled(final VcsContext context) {
    VirtualFile[] selectedFiles = context.getSelectedFiles();
    if (selectedFiles == null) return false;
    if (selectedFiles.length != 1) return false;
    VirtualFile file = selectedFiles[0];
    if (file.isDirectory()) return false;
    Project project = context.getProject();
    if (project == null || project.isDisposed()) return false;
    AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
    if (vcs == null) return false;
    final AnnotationProvider annotationProvider = vcs.getAnnotationProvider();
    if (annotationProvider == null) return false;
    final FileStatus fileStatus = FileStatusManager.getInstance(project).getStatus(file);
    if (fileStatus == FileStatus.UNKNOWN || fileStatus == FileStatus.ADDED) {
      return false;
    }
    return hasTextEditor(file);
  }

  private static boolean hasTextEditor(VirtualFile selectedFile) {
    FileTypeManager fileTypeManager = FileTypeManager.getInstance();
    FileType fileType = fileTypeManager.getFileTypeByFile(selectedFile);
    return !fileType.isBinary() && fileType != StdFileTypes.GUI_DESIGNER_FORM;
  }

  public boolean isSelected(AnActionEvent e) {
    VcsContext context = VcsContextFactory.SERVICE.getInstance().createContextOn(e);
    Editor editor = context.getEditor();
    if (editor == null) return false;
    Collection annotations = editor.getUserData(KEY_IN_EDITOR);
    return annotations != null && !annotations.isEmpty();
  }

  public void setSelected(AnActionEvent e, boolean state) {
    VcsContext context = VcsContextFactory.SERVICE.getInstance().createContextOn(e);
    Editor editor = context.getEditor();
    if (!state) {
      if (editor != null) {
        editor.getGutter().closeAllAnnotations();
      }
    }
    else {
      if (editor == null) {
        VirtualFile selectedFile = context.getSelectedFile();
        FileEditor[] fileEditors = FileEditorManager.getInstance(context.getProject()).openFile(selectedFile, false);
        for (FileEditor fileEditor : fileEditors) {
          if (fileEditor instanceof TextEditor) {
            editor = ((TextEditor)fileEditor).getEditor();
          }
        }
      }

      LOG.assertTrue(editor != null);

      doAnnotate(editor, context.getProject());

    }
  }

  private static void doAnnotate(final Editor editor, final Project project) {
    final VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
    if (project == null) return;
    AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
    if (vcs == null) return;
    final AnnotationProvider annotationProvider = vcs.getAnnotationProvider();

    final Ref<FileAnnotation> fileAnnotationRef = new Ref<FileAnnotation>();
    final Ref<VcsException> exceptionRef = new Ref<VcsException>();
    boolean result = ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
      public void run() {
        try {
          fileAnnotationRef.set(annotationProvider.annotate(file));
        }
        catch (VcsException e) {
          exceptionRef.set(e);
        }
      }
    }, VcsBundle.message("retrieving.annotations"), true, project);
    if (!result) {
      return;
    }
    if (!exceptionRef.isNull()) {
      AbstractVcsHelper.getInstance(project).showErrors(Arrays.asList(exceptionRef.get()), VcsBundle.message("message.title.annotate"));
    }
    if (fileAnnotationRef.isNull()) {
      return;
    }

    doAnnotate(editor, project, file, fileAnnotationRef.get());
  }

  public static void doAnnotate(final Editor editor, final Project project, final VirtualFile file, final FileAnnotation fileAnnotation) {
    String upToDateContent = fileAnnotation.getAnnotatedContent();

    final UpToDateLineNumberProvider getUpToDateLineNumber = new UpToDateLineNumberProviderImpl(
      editor.getDocument(),
      project,
      upToDateContent);

    editor.getGutter().closeAllAnnotations();

    Collection<AnnotationFieldGutter> annotations = editor.getUserData(KEY_IN_EDITOR);
    if (annotations == null) {
      annotations = new HashSet<AnnotationFieldGutter>();
      editor.putUserData(KEY_IN_EDITOR, annotations);
    }

    final HighlightAnnotationsActions highlighting = new HighlightAnnotationsActions(project, file, fileAnnotation);
    final LineAnnotationAspect[] aspects = fileAnnotation.getAspects();
    for (LineAnnotationAspect aspect : aspects) {
      final AnnotationFieldGutter gutter = new AnnotationFieldGutter(getUpToDateLineNumber, fileAnnotation, editor, aspect, highlighting);
      if (aspect instanceof EditorGutterAction) {
        editor.getGutter().registerTextAnnotation(gutter, gutter);
      }
      else {
        editor.getGutter().registerTextAnnotation(gutter);
      }
      annotations.add(gutter);
    }
  }

  private static class AnnotationFieldGutter implements TextAnnotationGutterProvider, EditorGutterAction {
    private final UpToDateLineNumberProvider myGetUpToDateLineNumber;
    private final FileAnnotation myAnnotation;
    private final Editor myEditor;
    private LineAnnotationAspect myAspect;
    private final HighlightAnnotationsActions myHighlighting;
    private AnnotationListener myListener;

    public AnnotationFieldGutter(UpToDateLineNumberProvider getUpToDateLineNumber,
                                 FileAnnotation annotation,
                                 Editor editor,
                                 LineAnnotationAspect aspect,
                                 final HighlightAnnotationsActions highlighting) {
      myGetUpToDateLineNumber = getUpToDateLineNumber;
      myAnnotation = annotation;
      myEditor = editor;
      myAspect = aspect;
      myHighlighting = highlighting;

      myListener = new AnnotationListener() {
        public void onAnnotationChanged() {
          myEditor.getGutter().closeAllAnnotations();
        }
      };

      myAnnotation.addListener(myListener);
    }

    public String getLineText(int line, Editor editor) {
      int currentLine = myGetUpToDateLineNumber.getLineNumber(line);
      if (currentLine == UpToDateLineNumberProvider.ABSENT_LINE_NUMBER) return "";
      return myAspect.getValue(currentLine);
    }

    @Nullable
    public String getToolTip(final int line, final Editor editor) {
      int currentLine = myGetUpToDateLineNumber.getLineNumber(line);
      if (currentLine == UpToDateLineNumberProvider.ABSENT_LINE_NUMBER) return null;

      return XmlStringUtil.escapeString(myAnnotation.getToolTip(currentLine));
    }

    public void doAction(int line) {
      if (myAspect instanceof EditorGutterAction) {
        int currentLine = myGetUpToDateLineNumber.getLineNumber(line);
        if (currentLine == UpToDateLineNumberProvider.ABSENT_LINE_NUMBER) return;

        ((EditorGutterAction)myAspect).doAction(currentLine);
      }
    }

    public Cursor getCursor(final int line) {
      if (myAspect instanceof EditorGutterAction) {
        int currentLine = myGetUpToDateLineNumber.getLineNumber(line);
        if (currentLine == UpToDateLineNumberProvider.ABSENT_LINE_NUMBER) return Cursor.getDefaultCursor();

        return ((EditorGutterAction)myAspect).getCursor(currentLine);
      } else {
        return Cursor.getDefaultCursor();
      }

    }

    public EditorFontType getStyle(final int line, final Editor editor) {
      int currentLine = myGetUpToDateLineNumber.getLineNumber(line);
      if (currentLine == UpToDateLineNumberProvider.ABSENT_LINE_NUMBER) return EditorFontType.PLAIN;
      final boolean isBold = myHighlighting.isLineBold(currentLine);
      return isBold ? EditorFontType.BOLD : EditorFontType.PLAIN;
    }

    public List<AnAction> getPopupActions(final Editor editor) {
      return myHighlighting.getList();
    }

    public void gutterClosed() {
      myAnnotation.removeListener(myListener);
      myAnnotation.dispose();
      myEditor.getUserData(KEY_IN_EDITOR).remove(this);
    }
  }
}
