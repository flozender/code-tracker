package com.intellij.openapi.diff.impl.highlighting;

import com.intellij.openapi.Disposeable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diff.impl.ComparisonPolicy;
import com.intellij.openapi.diff.impl.fragments.FragmentList;
import com.intellij.openapi.diff.impl.fragments.FragmentListImpl;
import com.intellij.openapi.diff.impl.fragments.LineFragment;
import com.intellij.openapi.diff.impl.processing.TextCompareProcessor;
import com.intellij.openapi.diff.impl.splitter.LineBlocks;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.Iterator;

public class SimpleDiffPanelState<DiffMarkupType extends DiffMarkup> implements Disposeable  {
  private ComparisonPolicy myComparisonPolicy = ComparisonPolicy.DEFAULT;
  protected final DiffMarkupType myAppender1;
  protected final DiffMarkupType myAppender2;
  private FragmentList myFragmentList = FragmentList.EMPTY;
  private final Project myProject;

  public SimpleDiffPanelState(DiffMarkupType diffMarkup1, DiffMarkupType diffMarkup2, Project project) {
    myAppender1 = diffMarkup1;
    myAppender2 = diffMarkup2;
    myProject = project;
  }

  public void setComparisonPolicy(ComparisonPolicy comparisonPolicy) {
    myComparisonPolicy = comparisonPolicy;
  }

  public ComparisonPolicy getComparisonPolicy() {
    return myComparisonPolicy;
  }

  public void dispose() {
    myAppender1.dispose();
    myAppender2.dispose();
  }

  private LineBlocks addMarkup(final ArrayList<LineFragment> lines) {
    resetMarkup();
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        for (Iterator<LineFragment> iterator = lines.iterator(); iterator.hasNext();) {
          LineFragment line = iterator.next();
          line.highlight(myAppender1, myAppender2, !iterator.hasNext());
        }
      }
    });
    ArrayList<LineFragment> allLineFragments = new ArrayList<LineFragment>();
    for (Iterator<LineFragment> iterator = lines.iterator(); iterator.hasNext();) {
      LineFragment lineFragment = iterator.next();
      allLineFragments.add(lineFragment);
      lineFragment.addAllDescendantsTo(allLineFragments);
    }
    myFragmentList = FragmentListImpl.fromList(allLineFragments);
    return LineBlocks.fromLineFragments(allLineFragments);
  }

  private void resetMarkup() {
  ApplicationManager.getApplication().runWriteAction(new ResetMarkupRunnable(this));
  }

  public LineBlocks updateEditors() {
    if (myAppender1.getEditor() == null || myAppender2.getEditor() == null) {
      resetMarkup();
      return LineBlocks.EMPTY;
    }

    return addMarkup(new TextCompareProcessor(myComparisonPolicy).process(myAppender1.getText(), myAppender2.getText()));
  }

  public Project getProject() { return myProject; }

  public FragmentList getFragmentList() { return myFragmentList; }

  private static class ResetMarkupRunnable implements Runnable {
    private final SimpleDiffPanelState myState;

    public ResetMarkupRunnable(SimpleDiffPanelState state) {
      myState = state;
    }

    public void run() {
      myState.myAppender1.resetHighlighters();
      myState.myAppender2.resetHighlighters();
    }
  }
}
