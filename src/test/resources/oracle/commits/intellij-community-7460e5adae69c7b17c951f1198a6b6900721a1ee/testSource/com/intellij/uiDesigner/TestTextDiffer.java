package com.intellij.uiDesigner;

import junit.framework.TestCase;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class TestTextDiffer extends TestCase{
  
  public void test1() {
    test("","",-1,-1,null);
    test("abcd","abcd",-1,-1,null);
    test("abcd","aBCd",1,3,"BC");
    test("abcd","a12345d",1,3,"12345");
    test("abcd","ad",1,3,"");
    test("ad","a12345d",1,1,"12345");
    test("abcd","adf",1,4,"df");
    test("abcd","123bcd",0,1,"123");
    test("abcd","1234abcd",0,0,"1234");
    test("abcd","1234",0,4,"1234");
    test("","1234",0,0,"1234");
    test("1234","",0,4,"");
    test("1234","123",3,4,"");
    test("1234","234",0,1,"");
    test("ababa","aba",3,5,"");
    test("abbba","abba",3,4,"");
    test("abba","abbba",3,3,"b");
    test("abba","ab1ba",2,2,"1");
  }
  
  private static void test(final String oldText, final String newText, final int startOffset, final int endOffset, final String replacement) {
    final GuiEditor.ReplaceInfo replaceInfo = GuiEditor.findFragmentToChange(oldText, newText);
    assertEquals(startOffset, replaceInfo.getStartOffset());
    assertEquals(endOffset, replaceInfo.getEndOffset());
    assertEquals(replacement, replaceInfo.getReplacement());

    if (startOffset != -1 || endOffset != -1){
      assertEquals(newText, oldText.substring(0, startOffset) + replacement + oldText.substring(endOffset));
    }
  }
  
}
