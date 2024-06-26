package com.intellij.util.containers;

import java.lang.ref.WeakReference;


public class WeakReferenceArrayTest extends WeaksTestCase {
  private WeakReferenceArray myCollection = new WeakReferenceArray();

  protected void setUp() throws Exception {
    super.setUp();
    assertTrue(WeakReferenceArray.PEFORM_CHECK_THREAD);
    WeakReferenceArray.PEFORM_CHECK_THREAD = false;
  }

  protected void tearDown() throws Exception {
    WeakReferenceArray.PEFORM_CHECK_THREAD = true;
    super.tearDown();
  }

  public void testCorpseCounter() {
    addElements(5);
    assertEquals(0, myCollection.getCorpseCount());
    checkSameElements();
    myHolder.remove(3);
    gc();
    assertEquals(1, myCollection.getCorpseCount());
    myCollection.remove(3);
    checkSameNotNulls();
    assertEquals(1, myCollection.getCorpseCount());
    myCollection.remove(2);
    assertEquals(2, myCollection.getCorpseCount());
    myHolder.remove(2);
    gc();
    checkSameNotNulls();
    assertEquals(2, myCollection.getCorpseCount());
  }

  public void testAddRemove() {
    myHolder.add("1");
    myHolder.add("2");
    assertEquals(0, myCollection.size());
    myCollection.add(myHolder.get(0));
    assertEquals(1, myCollection.size());
    myCollection.add(myHolder.get(1));
    checkSameElements();
    assertSame(myHolder.get(0), myCollection.remove(0));
    assertEquals(2, myCollection.size());
    assertNull(myCollection.remove(0));
  }

  public void testRemoveDead() {
    myCollection.add(new Object());
    myCollection.add(this);
    gc();
    assertEquals(2, myCollection.size());
    assertEquals(1, myCollection.getCorpseCount());
    assertNull(myCollection.remove(0));
    assertEquals(1, myCollection.getCorpseCount());
  }

  public void testCompress() {
    addElements(5);
    checkSameElements();
    myHolder.remove(0);
    myHolder.remove(0);
    gc();
    checkSameNotNulls();
    myCollection.compress(-1);
    checkSameElements();
    assertEquals(0, myCollection.getCorpseCount());
  }

  public void testCompressTrackingLastSurvived() {
    myCollection.add(new Object());
    addElement(new Object());
    myCollection.add(new Object());
    addElement(new Object());
    myCollection.add(new Object());
    gc();
    assertEquals(1, myCollection.compress(3));
  }

  public void testCompressTrackingNotLastSurvived() {
    myCollection.add(new Object());
    addElement(new Object());
    myCollection.add(new Object());
    addElement(new Object());
    gc();
    assertEquals(0, myCollection.compress(1));
  }

  public void testCompressTrackingLostIndex() {
    myCollection.add(new Object());
    addElement(new Object());
    myCollection.add(new Object());
    addElement(new Object());
    gc();
    assertEquals(-2, myCollection.compress(2));
  }

  public void testCompressTrackingLostLastIndex() {
    myCollection.add(new Object());
    addElement(new Object());
    myCollection.add(new Object());
    addElement(new Object());
    myCollection.add(new Object());
    gc();
    assertEquals(-3, myCollection.compress(4));
  }

  public void testAddingManyElement() {
    int initialCapacity = fullCollection();
    assertEquals(initialCapacity, myCollection.getCapacity());
    assertTrue(initialCapacity > 3); // Need to go on
    myHolder.remove(0);
    myHolder.remove(0);
    gc();
    checkSameNotNulls();
    addElement(new Object());
    assertEquals(initialCapacity, myCollection.getCapacity());
    checkSameElements();
  }

  public void testReduceSize() {
    int initialCapacity = fullCollection();
    assertEquals(initialCapacity, myCollection.getCapacity());
    addElement(new Object());
    int grownCapacity = 2 * initialCapacity;
    assertEquals(grownCapacity, myCollection.getCapacity());
    checkSameElements();
    myCollection.reduceCapacity(-1);
    assertEquals(grownCapacity, myCollection.getCapacity());
    myHolder.remove(0);
    gc();
    myCollection.reduceCapacity(-1);
    assertEquals(grownCapacity, myCollection.getCapacity());
    myHolder.remove(0);
    for (int i = 0; i < grownCapacity; i++) addElement(new Object());
    for (int i = 0; i < grownCapacity; i++) myHolder.remove(0);
    final int lastCapacity = myCollection.getCapacity();
    gc();
    checkSameNotNulls();
    myCollection.reduceCapacity(-1);
    assertTrue(lastCapacity > myCollection.getCapacity());
    checkSameElements();
    myHolder.clear();
    gc();
    myCollection.reduceCapacity(-1);
    assertEquals(0, myCollection.size());
    addElement(new Object());
    checkSameElements();
  }

  public void testReduceCapacityOfAliveCollection() {
    myCollection = new WeakReferenceArray(WeakReferenceArray.MINIMUM_CAPACITY * 3);
    addElement(new Object());
    myCollection.reduceCapacity(-1);
    assertEquals(0, myCollection.getCorpseCount());
    checkSameElements();
  }

  public void testRemoveAlives() {
    addElement(new Object());
    addElement(new Object());
    addElement(new Object());
    WeakReference[] references = copyReferences();
    assertTrue(references.length >= 3);
    myCollection.remove(1);
    myCollection.remove(0);
    assertEquals(2, myCollection.getCorpseCount());
    myHolder.remove(0);
    myHolder.remove(0);
    checkSameNotNulls();
    gc();
    assertEquals(2, myCollection.getCorpseCount());
  }

  private WeakReference[] copyReferences() {
    WeakReference[] references = new WeakReference[myCollection.getCapacity()];
    System.arraycopy(myCollection.getReferences(), 0, references, 0, references.length);
    return references;
  }

  public void testRemoveEnqueuedReference() {
    addElement(new Object());
    WeakReference reference = myCollection.getReferences()[0];
    assertSame(reference.get(), myHolder.get(0));
    myHolder.clear();
    reference.enqueue();
    myCollection.remove(0);
    gc();
    assertEquals(1, myCollection.getCorpseCount());
    myCollection.getCorpseCount();
    reference = null;
    gc();
    assertEquals(1, myCollection.getCorpseCount());
  }

  public void testRemoveNotEnqueuedReference() {
    addElement(new Object());
    WeakReference reference = myCollection.getReferences()[0];
    myCollection.remove(0);
    gc();
    myHolder.clear();
    reference.enqueue();
    gc();
    assertEquals(1, myCollection.getCorpseCount());
  }

  public void testOutofboundsReferences() {
    for (int i = 0; i < 400; i++) addElement(new Object());
    WeakReference[] references = copyReferences();
    for (int i = 0; i < 100; i++) references[i].clear();
    for (int i = 100; i < 395; i++) myCollection.remove(i);
    myCollection.reduceCapacity(-1);
    assertEquals(210, myCollection.getCapacity());
    assertEquals(5, myCollection.size());
    assertEquals(0, myCollection.getCorpseCount());
    for (int i = 0; i < 100; i++) assertFalse(String.valueOf(i), references[i].enqueue());

    for (int i = 0; i < myCollection.getCapacity() - 20; i++) addElement(new Object());
    for (int i = 0; i < myCollection.getCapacity() - 20; i++) myCollection.remove(i);
    myCollection.reduceCapacity(-1);
    for (int i = 0; i < 100; i++) assertFalse(references[i].enqueue());
    myCollection.reduceCapacity(-1);
  }

  public void testRemoveAliveReference() {
    Object obj = new Object();
    myCollection.add(obj);
    WeakReference[] references = copyReferences();
    assertSame(obj, references[0].get());
    assertTrue(myCollection.removeReference(0));
    assertTrue(references[0].isEnqueued());
    assertNull(references[0].get());
    assertEquals(1, myCollection.getCorpseCount());
    assertNull(myCollection.getReferences()[0]);
  }

  public void testRemoveDeadReference() {
    Object obj = new Object();
    myCollection.add(obj);
    WeakReference[] references = copyReferences();
    references[0].clear();
    references[0].enqueue();
    assertFalse(myCollection.removeReference(0));
    assertEquals(1, myCollection.getCorpseCount());
    assertNull(myCollection.getReferences()[0]);
  }

  public void testRemoveRemovedReference() {
    Object obj = new Object();
    myCollection.add(obj);
    myCollection.remove(0);
    assertEquals(1, myCollection.getCorpseCount());
    assertFalse(myCollection.removeReference(0));
    assertEquals(1, myCollection.getCorpseCount());
  }

  public void testCompacting() {
    addElements(20);
    removeEven(1);
    myCollection.reduceCapacity(-1);
    checkSameElements();

    addElements(19);
    removeEven(1);

    myCollection.compress(-1);
    checkSameElements();

    addElements(19);
    removeEven(0);
    myCollection.reduceCapacity(-1);
    checkSameElements();

    addElements(19);
    removeEven(0);
    myCollection.compress(-1);
    checkSameElements();
  }

  private void removeEven(int remainder) {
    for (int i = 0; i < myCollection.size(); i++) {
      if (i % 4 != remainder) myHolder.remove(myCollection.remove(i));
    }
  }

  private int fullCollection() {
    int initialCapacity = myCollection.getCapacity();
    for (int i = 0; i < initialCapacity; i++) {
      addElement(new Object());
    }
    return initialCapacity;
  }

  private void checkSameElements() {
    checkSameElements(myCollection);
  }

  private void checkSameNotNulls() {
    checkSameNotNulls(myCollection);
  }

  private void addElements(int count) {
    for (int i = 0; i < count; i++) {
      addElement(new Object());
    }
  }

  private void addElement(Object o) {
    addElement(o, myCollection);
  }

}
