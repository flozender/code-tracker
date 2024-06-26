/*
 * Copyright (c) 2000-2004 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.util.containers;

import java.util.Arrays;
import java.util.List;

public class Queue<T> {
  private final NormalState NORMAL_STATE = new NormalState();
  private final InvertedState INVERTED_STATE = new InvertedState();

  private Object[] myArray;
  private int myFirst = 0;
  private int myLast = 0;
  private QueueState myState = NORMAL_STATE;

  public Queue(int initialCapacity) {
    myArray = new Object[initialCapacity];
  }

  public void addLast(T object) {
    int currrentSize = size();
    if (currrentSize == myArray.length) {
      myArray = myState.normalize(currrentSize * 2);
      myFirst = 0;
      myLast = currrentSize;
      myState = NORMAL_STATE;
    }
    myState.addLast(object);
  }

  public T pullFirst() {
    return myState.pullFirst();
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public int size() {
    return myState.calculateSize();
  }

  public List<T> toList() {
    return Arrays.asList(myState.normalize(size()));
  }

  private abstract class QueueState {
    public abstract T[] normalize(int capacity);

    public T pullFirst() {
      T result = (T)myArray[myFirst];
      myFirst++;
      if (myFirst == myArray.length) {
        myFirst = 0;
        myState = inverted();
      }
      return result;
    }

    public void addLast(T object) {
      myArray[myLast] = object;
      myLast++;
      if (myLast == myArray.length) {
        myState = inverted();
        myLast = 0;
      }
    }

    protected abstract QueueState inverted();

    public abstract int calculateSize();

    protected int copyFromTo(int first, int last, T[] result, int destPos) {
      int length = last - first;
      System.arraycopy(myArray, first, result, destPos, length);
      return length;
    }
  }

  private class NormalState extends QueueState {
    public T[] normalize(int capacity) {
      T[] result = (T[])new Object[capacity];
      copyFromTo(myFirst, myLast, result, 0);
      return result;
    }

    protected QueueState inverted() {
      return INVERTED_STATE;
    }

    public int calculateSize() {
      return myLast - myFirst;
    }


  }

  private class InvertedState extends QueueState {
    public T[] normalize(int capasity) {
      T[] result = (T[])new Object[capasity];
      int tailLength = copyFromTo(myFirst, myArray.length, result, 0);
      copyFromTo(0, myLast, result, tailLength);
      return result;
    }

    protected QueueState inverted() {
      return NORMAL_STATE;
    }

    public int calculateSize() {
      return myArray.length - myFirst + myLast;
    }
  }
}
