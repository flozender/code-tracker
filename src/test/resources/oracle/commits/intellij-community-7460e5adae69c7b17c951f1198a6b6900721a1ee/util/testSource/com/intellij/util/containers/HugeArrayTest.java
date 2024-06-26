package com.intellij.util.containers;

import com.intellij.util.Assertion;
import junit.framework.Assert;
import junit.framework.TestCase;

public class HugeArrayTest extends TestCase {
  private HugeArray myArray = new HugeArray(3);
  private Assertion CHECK = new Assertion();

  public void testIndexing() {
    Assert.assertEquals(0, myArray.calcRow(0));
    Assert.assertEquals(0, myArray.calcRow(7));
    Assert.assertEquals(1, myArray.calcRow(8));
    Assert.assertEquals(0, myArray.calcColumn(0));
    Assert.assertEquals(1, myArray.calcColumn(1));
    Assert.assertEquals(7, myArray.calcColumn(7));
    Assert.assertEquals(0, myArray.calcColumn(8));
  }

  public void testSize() {
    Assert.assertEquals(0, myArray.size());
    myArray.put(0, "");
    Assert.assertEquals(1, myArray.size());
    myArray.put(10, "");
    Assert.assertEquals(11, myArray.size());
  }

  public void testToArray() {
    myArray.put(1, "a");
    myArray.put(15, "b");
    Object[] array = myArray.toArray();
    Assert.assertEquals(16, array.length);
    String[] expectedArray = new String[16];
    Assert.assertSame(expectedArray, myArray.toArray(expectedArray));
  }

  public void testToArrayOrder() {
    for (int i = 0; i < 64; i++)
      myArray.put(i, String.valueOf(i));
    String[] stringArray = (String[])myArray.toArray(new String[63]);
    for (int i = 0; i < stringArray.length; i++) {
      String value = String.valueOf(i);
      Assert.assertEquals(value, stringArray[i]);
      Assert.assertEquals(value, myArray.get(i));
    }
    Assert.assertEquals("63", myArray.toArray()[63]);
  }

  public void testAdd() {
    myArray.add("1");
    Assert.assertEquals(1, myArray.size());
    myArray.add("2");
    Assert.assertEquals(2, myArray.size());
    Assert.assertEquals("1", myArray.get(0));
    Assert.assertEquals("2", myArray.get(1));
    CHECK.compareAll(new String[]{"1", "2"}, myArray.toArray());
  }
}
