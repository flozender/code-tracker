package com.intellij.util.diff;

import junit.framework.TestCase;

/**
 * @author dyoma
 */
public class IntLCSTest extends TestCase {
  public void testDiag() {
    Diff.Change change = buildChange(new int[]{1, 2, 3}, new int[]{1, 2, 3}, 0);
    assertNull(change);
  }

  public void testOneAtBegging() {
    Diff.Change change = buildChange(new int[]{1, 2}, new int[]{1, 3}, 2);
    checkLastChange(change, 1, 1, 1, 1);
  }

  public void testOneAntEnd() {
    Diff.Change change = buildChange(new int[]{1, 3}, new int[]{2, 3}, 2);
    checkLastChange(change, 0, 0, 1, 1);
  }

  public void testOneOverAtEnd() {
    Diff.Change change = buildChange(new int[]{1, 2}, new int[]{1, 2, 3}, 1);
    checkLastChange(change, 2, 2, 1, 0);
  }

  public void testOneOverAtBegging() {
    Diff.Change change = buildChange(new int[]{1, 2, 3}, new int[]{2, 3}, 1);
    checkLastChange(change, 0, 0, 0, 1);
  }

  public void testOneTail() {
    assertEquals(1, new IntLCS(new int[]{1, 2}, new int[]{1, 2, 3}).execute());
  }

  public void testSingleMiddle() {
    Diff.Change change = buildChange(new int[]{1, 2, 3}, new int[]{4, 2, 5}, 4);
    checkChange(change, 0, 0, 1, 1);
    checkLastChange(change.link, 2, 2, 1, 1);
  }

  public void testAbsolutelyDifferent() {
    assertEquals(4, new IntLCS(new int[]{1, 2}, new int[]{3, 4}).execute());
    assertEquals(6, new IntLCS(new int[]{1, 2, 3}, new int[]{4, 5, 6}).execute());
  }

  private Diff.Change buildChange(int[] first, int[] second, int expectedNonDiags) {
    IntLCS intLCS = new IntLCS(first, second);
    assertEquals(expectedNonDiags, intLCS.execute());
    Reindexer reindexer = new Reindexer();
    reindexer.idInit(first.length, second.length);
    Diff.ChangeBuilder builder = new Diff.ChangeBuilder();
    reindexer.reindex(intLCS.getPaths(), builder);
    return builder.getFirstChange();
  }

  public static void checkChange(Diff.Change change, int line0, int line1, int inserted, int deleted) {
    assertNotNull("Expected not null change", change);
    String message = change.toString();
    assertEquals(message + " line0:", line0, change.line0);
    assertEquals(message + " line1:", line1, change.line1);
    assertEquals(message + " insert:", inserted, change.inserted);
    assertEquals(message + " delete:", deleted, change.deleted);
  }

  public static void checkLastChange(Diff.Change change, int line0, int line1, int inserted, int deleted) {
    checkChange(change, line0, line1, inserted, deleted);
    assertNull("Expected last change", change.link);
  }
}
