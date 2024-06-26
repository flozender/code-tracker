package com.intellij.openapi.diff.impl.incrementalMerge;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diff.ex.DiffFragment;
import com.intellij.openapi.diff.impl.ComparisonPolicy;
import com.intellij.openapi.diff.impl.DiffUtil;
import com.intellij.openapi.diff.impl.highlighting.FragmentSide;
import com.intellij.openapi.diff.impl.splitter.LineBlocks;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.containers.HashSet;

import java.util.*;

public class ChangeList {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.diff.impl.incrementalMerge.ChangeList");
  private static final Comparator<Change> CHANGE_ORDER = new SimpleChange.ChangeOrder(FragmentSide.SIDE1);

  private final Document[] myDocuments = new Document[2];
  private final Parent myParent;
  private final ArrayList<Listener> myListeners = new ArrayList<Listener>();
  private ArrayList<Change> myChanges;

  public ChangeList(Document base, Document version, Parent parent) {
    myDocuments[0] = base;
    myDocuments[1] = version;
    myParent = parent;
  }

  public void addListener(Listener listener) {
    myListeners.add(listener);
  }

  public void removeListener(Listener listener) {
    LOG.assertTrue(myListeners.remove(listener));
  }

  public void setChanges(ArrayList<Change> changes) {
    if (myChanges != null) {
      HashSet<Change> newChanges = new HashSet<Change>(changes);
      LOG.assertTrue(newChanges.size() == changes.size());
      for (Iterator<Change> iterator = myChanges.iterator(); iterator.hasNext();) {
        Change oldChange = iterator.next();
        if (!newChanges.contains(oldChange)) {
          iterator.remove();
          oldChange.onRemovedFromList();
        }
      }
    }
    for (Iterator<Change> iterator = changes.iterator(); iterator.hasNext();) {
      Change change = iterator.next();
      LOG.assertTrue(change.isValid());
    }
    myChanges = new ArrayList<Change>(changes);
  }

  public Project getProject() { return myParent.getProject(); }

  public List<Change> getChanges() {
    return Collections.unmodifiableList(myChanges);
  }

  public static ChangeList build(Document base, Document version, Parent parent) {
    ChangeList result = new ChangeList(base, version, parent);
    ArrayList<Change> changes = result.buildChanges();
    Collections.sort(changes, CHANGE_ORDER);
    result.setChanges(changes);
    return result;
  }

  public void setMarkup(final Editor base, final Editor version) {
    Editor[] editors = new Editor[]{base, version};
    for (Iterator<Change> iterator = myChanges.iterator(); iterator.hasNext();) {
      Change change = iterator.next();
      change.addMarkup(editors);
    }
  }

  public void updateMarkup() {
    for (Iterator<Change> iterator = myChanges.iterator(); iterator.hasNext();) {
      Change change = iterator.next();
      change.updateMarkup();
    }
  }

  public Document getDocument(FragmentSide side) {
    return myDocuments[side.getIndex()];
  }

  private ArrayList<Change> buildChanges() {
    Document base = getDocument(FragmentSide.SIDE1);
    String[] baseLines = DiffUtil.convertToLines(base.getText());
    Document version = getDocument(FragmentSide.SIDE2);
    String[] versionLines = DiffUtil.convertToLines(version.getText());
    DiffFragment[] fragments = ComparisonPolicy.DEFAULT.buildDiffFragmentsFromLines(baseLines, versionLines);
    final ArrayList<Change> result = new ArrayList<Change>();
    new DiffFragmemntsEnumerator(fragments, base, version) {
      protected void process(DiffFragment fragment) {
        if (fragment.isEqual()) return;
        Context context = getContext();
        TextRange range1 = context.createRange(FragmentSide.SIDE1);
        TextRange range2 = context.createRange(FragmentSide.SIDE2);
        result.add(new SimpleChange(ChangeType.fromDiffFragment(context.getFragment()), range1, range2, ChangeList.this));
      }
    }.execute();
    return result;
  }

  public Change getChange(int index) {
    return myChanges.get(index);
  }

  private static abstract class DiffFragmemntsEnumerator {
    private final DiffFragment[] myFragments;
    private final Context myContext;

    public DiffFragmemntsEnumerator(DiffFragment[] fragments, Document document1, Document document2) {
      myContext = new Context(document1, document2);
      myFragments = fragments;
    }

    public void execute() {
      for (int i = 0; i < myFragments.length; i++) {
        DiffFragment fragment = myFragments[i];
        myContext.myFragment = fragment;
        process(fragment);
        String text1 = fragment.getText1();
        String text2 = fragment.getText2();
        myContext.myStarts[0] += DiffUtil.getTextLength(text1);
        myContext.myStarts[1] += DiffUtil.getTextLength(text2);
        myContext.myLines[0] += countLines(text1);
        myContext.myLines[1] += countLines(text2);
      }
    }

    private int countLines(String text) {
      if (text == null) return 0;
      char[] chars = text.toCharArray();
      int counter = 0;
      for (int i = 0; i < chars.length; i++) {
        char aChar = chars[i];
        if (aChar == '\n') counter++;
      }
      return counter;
    }

    protected Context getContext() {
      return myContext;
    }

    protected abstract void process(DiffFragment fragment);
  }

  public static class Context {
    private final Document[] myDocuments = new Document[2];
    private DiffFragment myFragment;
    private final int[] myStarts = new int[]{0, 0};
    private final int[] myLines = new int[]{0, 0};

    public Context(Document document1, Document document2) {
      myDocuments[0] = document1;
      myDocuments[1] = document2;
    }

    public DiffFragment getFragment() {
      return myFragment;
    }

    public int getStart(FragmentSide side) {
      return myStarts[side.getIndex()];
    }

    public int getEnd(FragmentSide side) {
      return getStart(side) + DiffUtil.getTextLength(side.getText(myFragment));
    }

    public TextRange createRange(FragmentSide side) {
      return new TextRange(getStart(side), getEnd(side));
    }
  }

  public int getCount() {
    return myChanges.size();
  }

  public LineBlocks getLineBlocks() {
    return LineBlocks.fromChanges(myChanges);
  }

  public void remove(Change change) {
    LOG.assertTrue(myChanges.remove(change), change.toString());
    change.onRemovedFromList();
    fireOnChangeRemoved();
  }

  private void fireOnChangeRemoved() {
    Listener[] listeners = myListeners.toArray(new Listener[myListeners.size()]);
    for (int i = 0; i < listeners.length; i++) {
      Listener listener = listeners[i];
      listener.onChangeRemoved(this);
    }
  }

  public interface Parent {
    Project getProject();
  }

  public interface Listener {
    void onChangeRemoved(ChangeList source);
  }
}
