package org.apache.lucene.search;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

/**
 * Expert: a FieldComparator compares hits so as to determine their
 * sort order when collecting the top results with {@link
 * TopFieldCollector}.  The concrete public FieldComparator
 * classes here correspond to the SortField types.
 *
 * <p>This API is designed to achieve high performance
 * sorting, by exposing a tight interaction with {@link
 * FieldValueHitQueue} as it visits hits.  Whenever a hit is
 * competitive, it's enrolled into a virtual slot, which is
 * an int ranging from 0 to numHits-1.  The {@link
 * FieldComparator} is made aware of segment transitions
 * during searching in case any internal state it's tracking
 * needs to be recomputed during these transitions.</p>
 *
 * <p>A comparator must define these functions:</p>
 *
 * <ul>
 *
 *  <li> {@link #compare} Compare a hit at 'slot a'
 *       with hit 'slot b'.
 *
 *  <li> {@link #setBottom} This method is called by
 *       {@link FieldValueHitQueue} to notify the
 *       FieldComparator of the current weakest ("bottom")
 *       slot.  Note that this slot may not hold the weakest
 *       value according to your comparator, in cases where
 *       your comparator is not the primary one (ie, is only
 *       used to break ties from the comparators before it).
 *
 *  <li> {@link #compareBottom} Compare a new hit (docID)
 *       against the "weakest" (bottom) entry in the queue.
 *
 *  <li> {@link #setTopValue} This method is called by
 *       {@link TopFieldCollector} to notify the
 *       FieldComparator of the top most value, which is
 *       used by future calls to {@link #compareTop}.
 *
 *  <li> {@link #compareBottom} Compare a new hit (docID)
 *       against the "weakest" (bottom) entry in the queue.
 *
 *  <li> {@link #compareTop} Compare a new hit (docID)
 *       against the top value previously set by a call to
 *       {@link #setTopValue}.
 *
 *  <li> {@link #copy} Installs a new hit into the
 *       priority queue.  The {@link FieldValueHitQueue}
 *       calls this method when a new hit is competitive.
 *
 *  <li> {@link #setNextReader(AtomicReaderContext)} Invoked
 *       when the search is switching to the next segment.
 *       You may need to update internal state of the
 *       comparator, for example retrieving new values from
 *       DocValues.
 *
 *  <li> {@link #value} Return the sort value stored in
 *       the specified slot.  This is only called at the end
 *       of the search, in order to populate {@link
 *       FieldDoc#fields} when returning the top results.
 * </ul>
 *
 * @lucene.experimental
 */
public abstract class FieldComparator<T> {

  /**
   * Compare hit at slot1 with hit at slot2.
   * 
   * @param slot1 first slot to compare
   * @param slot2 second slot to compare
   * @return any N < 0 if slot2's value is sorted after
   * slot1, any N > 0 if the slot2's value is sorted before
   * slot1 and 0 if they are equal
   */
  public abstract int compare(int slot1, int slot2);

  /**
   * Set the bottom slot, ie the "weakest" (sorted last)
   * entry in the queue.  When {@link #compareBottom} is
   * called, you should compare against this slot.  This
   * will always be called before {@link #compareBottom}.
   * 
   * @param slot the currently weakest (sorted last) slot in the queue
   */
  public abstract void setBottom(final int slot);

  /**
   * Record the top value, for future calls to {@link
   * #compareTop}.  This is only called for searches that
   * use searchAfter (deep paging), and is called before any
   * calls to {@link #setNextReader}.
   */
  public abstract void setTopValue(T value);

  /**
   * Compare the bottom of the queue with this doc.  This will
   * only invoked after setBottom has been called.  This
   * should return the same result as {@link
   * #compare(int,int)}} as if bottom were slot1 and the new
   * document were slot 2.
   *    
   * <p>For a search that hits many results, this method
   * will be the hotspot (invoked by far the most
   * frequently).</p>
   * 
   * @param doc that was hit
   * @return any N < 0 if the doc's value is sorted after
   * the bottom entry (not competitive), any N > 0 if the
   * doc's value is sorted before the bottom entry and 0 if
   * they are equal.
   */
  public abstract int compareBottom(int doc) throws IOException;

  /**
   * Compare the top value with this doc.  This will
   * only invoked after setTopValue has been called.  This
   * should return the same result as {@link
   * #compare(int,int)}} as if topValue were slot1 and the new
   * document were slot 2.  This is only called for searches that
   * use searchAfter (deep paging).
   *    
   * @param doc that was hit
   * @return any N < 0 if the doc's value is sorted after
   * the bottom entry (not competitive), any N > 0 if the
   * doc's value is sorted before the bottom entry and 0 if
   * they are equal.
   */
  public abstract int compareTop(int doc) throws IOException;

  /**
   * This method is called when a new hit is competitive.
   * You should copy any state associated with this document
   * that will be required for future comparisons, into the
   * specified slot.
   * 
   * @param slot which slot to copy the hit to
   * @param doc docID relative to current reader
   */
  public abstract void copy(int slot, int doc) throws IOException;

  /**
   * Set a new {@link AtomicReaderContext}. All subsequent docIDs are relative to
   * the current reader (you must add docBase if you need to
   * map it to a top-level docID).
   * 
   * @param context current reader context
   * @return the comparator to use for this segment; most
   *   comparators can just return "this" to reuse the same
   *   comparator across segments
   * @throws IOException if there is a low-level IO error
   */
  public abstract FieldComparator<T> setNextReader(AtomicReaderContext context) throws IOException;

  /** Sets the Scorer to use in case a document's score is
   *  needed.
   * 
   * @param scorer Scorer instance that you should use to
   * obtain the current hit's score, if necessary. */
  public void setScorer(Scorer scorer) {
    // Empty implementation since most comparators don't need the score. This
    // can be overridden by those that need it.
  }
  
  /**
   * Return the actual value in the slot.
   *
   * @param slot the value
   * @return value in this slot
   */
  public abstract T value(int slot);

  /** Returns -1 if first is less than second.  Default
   *  impl to assume the type implements Comparable and
   *  invoke .compareTo; be sure to override this method if
   *  your FieldComparator's type isn't a Comparable or
   *  if your values may sometimes be null */
  @SuppressWarnings("unchecked")
  public int compareValues(T first, T second) {
    if (first == null) {
      if (second == null) {
        return 0;
      } else {
        return -1;
      }
    } else if (second == null) {
      return 1;
    } else {
      return ((Comparable<T>) first).compareTo(second);
    }
  }

  /**
   * Base FieldComparator class for numeric types
   */
  public static abstract class NumericComparator<T extends Number> extends FieldComparator<T> {
    protected final T missingValue;
    protected final String field;
    protected Bits docsWithField;
    
    public NumericComparator(String field, T missingValue) {
      this.field = field;
      this.missingValue = missingValue;
    }

    @Override
    public FieldComparator<T> setNextReader(AtomicReaderContext context) throws IOException {
      if (missingValue != null) {
        docsWithField = DocValues.getDocsWithField(context.reader(), field);
        // optimization to remove unneeded checks on the bit interface:
        if (docsWithField instanceof Bits.MatchAllBits) {
          docsWithField = null;
        }
      } else {
        docsWithField = null;
      }
      return this;
    }
  }

  /** Parses field's values as double (using {@link
   *  AtomicReader#getNumericDocValues} and sorts by ascending value */
  public static final class DoubleComparator extends NumericComparator<Double> {
    private final double[] values;
    private NumericDocValues currentReaderValues;
    private double bottom;
    private double topValue;

    DoubleComparator(int numHits, String field, Double missingValue) {
      super(field, missingValue);
      values = new double[numHits];
    }

    @Override
    public int compare(int slot1, int slot2) {
      return Double.compare(values[slot1], values[slot2]);
    }

    @Override
    public int compareBottom(int doc) {
      double v2 = Double.longBitsToDouble(currentReaderValues.get(doc));
      // Test for v2 == 0 to save Bits.get method call for
      // the common case (doc has value and value is non-zero):
      if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
        v2 = missingValue;
      }

      return Double.compare(bottom, v2);
    }

    @Override
    public void copy(int slot, int doc) {
      double v2 = Double.longBitsToDouble(currentReaderValues.get(doc));
      // Test for v2 == 0 to save Bits.get method call for
      // the common case (doc has value and value is non-zero):
      if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
        v2 = missingValue;
      }

      values[slot] = v2;
    }

    @Override
    public FieldComparator<Double> setNextReader(AtomicReaderContext context) throws IOException {
      currentReaderValues = DocValues.getNumeric(context.reader(), field);
      return super.setNextReader(context);
    }
    
    @Override
    public void setBottom(final int bottom) {
      this.bottom = values[bottom];
    }

    @Override
    public void setTopValue(Double value) {
      topValue = value;
    }

    @Override
    public Double value(int slot) {
      return Double.valueOf(values[slot]);
    }

    @Override
    public int compareTop(int doc) {
      double docValue = Double.longBitsToDouble(currentReaderValues.get(doc));
      // Test for docValue == 0 to save Bits.get method call for
      // the common case (doc has value and value is non-zero):
      if (docsWithField != null && docValue == 0 && !docsWithField.get(doc)) {
        docValue = missingValue;
      }
      return Double.compare(topValue, docValue);
    }
  }

  /** Parses field's values as float (using {@link
   *  AtomicReader#getNumericDocValues(String)} and sorts by ascending value */
  public static final class FloatComparator extends NumericComparator<Float> {
    private final float[] values;
    private NumericDocValues currentReaderValues;
    private float bottom;
    private float topValue;

    FloatComparator(int numHits, String field, Float missingValue) {
      super(field, missingValue);
      values = new float[numHits];
    }
    
    @Override
    public int compare(int slot1, int slot2) {
      return Float.compare(values[slot1], values[slot2]);
    }

    @Override
    public int compareBottom(int doc) {
      // TODO: are there sneaky non-branch ways to compute sign of float?
      float v2 = Float.intBitsToFloat((int)currentReaderValues.get(doc));
      // Test for v2 == 0 to save Bits.get method call for
      // the common case (doc has value and value is non-zero):
      if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
        v2 = missingValue;
      }

      return Float.compare(bottom, v2);
    }

    @Override
    public void copy(int slot, int doc) {
      float v2 =  Float.intBitsToFloat((int)currentReaderValues.get(doc));
      // Test for v2 == 0 to save Bits.get method call for
      // the common case (doc has value and value is non-zero):
      if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
        v2 = missingValue;
      }

      values[slot] = v2;
    }

    @Override
    public FieldComparator<Float> setNextReader(AtomicReaderContext context) throws IOException {
      currentReaderValues = DocValues.getNumeric(context.reader(), field);
      return super.setNextReader(context);
    }
    
    @Override
    public void setBottom(final int bottom) {
      this.bottom = values[bottom];
    }

    @Override
    public void setTopValue(Float value) {
      topValue = value;
    }

    @Override
    public Float value(int slot) {
      return Float.valueOf(values[slot]);
    }

    @Override
    public int compareTop(int doc) {
      float docValue = Float.intBitsToFloat((int)currentReaderValues.get(doc));
      // Test for docValue == 0 to save Bits.get method call for
      // the common case (doc has value and value is non-zero):
      if (docsWithField != null && docValue == 0 && !docsWithField.get(doc)) {
        docValue = missingValue;
      }
      return Float.compare(topValue, docValue);
    }
  }

  /** Parses field's values as int (using {@link
   *  AtomicReader#getNumericDocValues(String)} and sorts by ascending value */
  public static final class IntComparator extends NumericComparator<Integer> {
    private final int[] values;
    private NumericDocValues currentReaderValues;
    private int bottom;                           // Value of bottom of queue
    private int topValue;

    IntComparator(int numHits, String field, Integer missingValue) {
      super(field, missingValue);
      values = new int[numHits];
    }
        
    @Override
    public int compare(int slot1, int slot2) {
      return Integer.compare(values[slot1], values[slot2]);
    }

    @Override
    public int compareBottom(int doc) {
      int v2 = (int) currentReaderValues.get(doc);
      // Test for v2 == 0 to save Bits.get method call for
      // the common case (doc has value and value is non-zero):
      if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
        v2 = missingValue;
      }

      return Integer.compare(bottom, v2);
    }

    @Override
    public void copy(int slot, int doc) {
      int v2 = (int) currentReaderValues.get(doc);
      // Test for v2 == 0 to save Bits.get method call for
      // the common case (doc has value and value is non-zero):
      if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
        v2 = missingValue;
      }

      values[slot] = v2;
    }

    @Override
    public FieldComparator<Integer> setNextReader(AtomicReaderContext context) throws IOException {
      currentReaderValues = DocValues.getNumeric(context.reader(), field);
      return super.setNextReader(context);
    }
    
    @Override
    public void setBottom(final int bottom) {
      this.bottom = values[bottom];
    }

    @Override
    public void setTopValue(Integer value) {
      topValue = value;
    }

    @Override
    public Integer value(int slot) {
      return Integer.valueOf(values[slot]);
    }

    @Override
    public int compareTop(int doc) {
      int docValue = (int) currentReaderValues.get(doc);
      // Test for docValue == 0 to save Bits.get method call for
      // the common case (doc has value and value is non-zero):
      if (docsWithField != null && docValue == 0 && !docsWithField.get(doc)) {
        docValue = missingValue;
      }
      return Integer.compare(topValue, docValue);
    }
  }

  /** Parses field's values as long (using {@link
   *  AtomicReader#getNumericDocValues(String)} and sorts by ascending value */
  public static final class LongComparator extends NumericComparator<Long> {
    private final long[] values;
    private NumericDocValues currentReaderValues;
    private long bottom;
    private long topValue;

    LongComparator(int numHits, String field, Long missingValue) {
      super(field, missingValue);
      values = new long[numHits];
    }

    @Override
    public int compare(int slot1, int slot2) {
      return Long.compare(values[slot1], values[slot2]);
    }

    @Override
    public int compareBottom(int doc) {
      // TODO: there are sneaky non-branch ways to compute
      // -1/+1/0 sign
      long v2 = currentReaderValues.get(doc);
      // Test for v2 == 0 to save Bits.get method call for
      // the common case (doc has value and value is non-zero):
      if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
        v2 = missingValue;
      }

      return Long.compare(bottom, v2);
    }

    @Override
    public void copy(int slot, int doc) {
      long v2 = currentReaderValues.get(doc);
      // Test for v2 == 0 to save Bits.get method call for
      // the common case (doc has value and value is non-zero):
      if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
        v2 = missingValue;
      }

      values[slot] = v2;
    }

    @Override
    public FieldComparator<Long> setNextReader(AtomicReaderContext context) throws IOException {
      currentReaderValues = DocValues.getNumeric(context.reader(), field);
      return super.setNextReader(context);
    }
    
    @Override
    public void setBottom(final int bottom) {
      this.bottom = values[bottom];
    }

    @Override
    public void setTopValue(Long value) {
      topValue = value;
    }

    @Override
    public Long value(int slot) {
      return Long.valueOf(values[slot]);
    }

    @Override
    public int compareTop(int doc) {
      long docValue = currentReaderValues.get(doc);
      // Test for docValue == 0 to save Bits.get method call for
      // the common case (doc has value and value is non-zero):
      if (docsWithField != null && docValue == 0 && !docsWithField.get(doc)) {
        docValue = missingValue;
      }
      return Long.compare(topValue, docValue);
    }
  }

  /** Sorts by descending relevance.  NOTE: if you are
   *  sorting only by descending relevance and then
   *  secondarily by ascending docID, performance is faster
   *  using {@link TopScoreDocCollector} directly (which {@link
   *  IndexSearcher#search} uses when no {@link Sort} is
   *  specified). */
  public static final class RelevanceComparator extends FieldComparator<Float> {
    private final float[] scores;
    private float bottom;
    private Scorer scorer;
    private float topValue;

    RelevanceComparator(int numHits) {
      scores = new float[numHits];
    }

    @Override
    public int compare(int slot1, int slot2) {
      return Float.compare(scores[slot2], scores[slot1]);
    }

    @Override
    public int compareBottom(int doc) throws IOException {
      float score = scorer.score();
      assert !Float.isNaN(score);
      return Float.compare(score, bottom);
    }

    @Override
    public void copy(int slot, int doc) throws IOException {
      scores[slot] = scorer.score();
      assert !Float.isNaN(scores[slot]);
    }

    @Override
    public FieldComparator<Float> setNextReader(AtomicReaderContext context) {
      return this;
    }
    
    @Override
    public void setBottom(final int bottom) {
      this.bottom = scores[bottom];
    }

    @Override
    public void setTopValue(Float value) {
      topValue = value;
    }

    @Override
    public void setScorer(Scorer scorer) {
      // wrap with a ScoreCachingWrappingScorer so that successive calls to
      // score() will not incur score computation over and
      // over again.
      if (!(scorer instanceof ScoreCachingWrappingScorer)) {
        this.scorer = new ScoreCachingWrappingScorer(scorer);
      } else {
        this.scorer = scorer;
      }
    }
    
    @Override
    public Float value(int slot) {
      return Float.valueOf(scores[slot]);
    }

    // Override because we sort reverse of natural Float order:
    @Override
    public int compareValues(Float first, Float second) {
      // Reversed intentionally because relevance by default
      // sorts descending:
      return second.compareTo(first);
    }

    @Override
    public int compareTop(int doc) throws IOException {
      float docValue = scorer.score();
      assert !Float.isNaN(docValue);
      return Float.compare(docValue, topValue);
    }
  }

  /** Sorts by ascending docID */
  public static final class DocComparator extends FieldComparator<Integer> {
    private final int[] docIDs;
    private int docBase;
    private int bottom;
    private int topValue;

    DocComparator(int numHits) {
      docIDs = new int[numHits];
    }

    @Override
    public int compare(int slot1, int slot2) {
      // No overflow risk because docIDs are non-negative
      return docIDs[slot1] - docIDs[slot2];
    }

    @Override
    public int compareBottom(int doc) {
      // No overflow risk because docIDs are non-negative
      return bottom - (docBase + doc);
    }

    @Override
    public void copy(int slot, int doc) {
      docIDs[slot] = docBase + doc;
    }

    @Override
    public FieldComparator<Integer> setNextReader(AtomicReaderContext context) {
      // TODO: can we "map" our docIDs to the current
      // reader? saves having to then subtract on every
      // compare call
      this.docBase = context.docBase;
      return this;
    }
    
    @Override
    public void setBottom(final int bottom) {
      this.bottom = docIDs[bottom];
    }

    @Override
    public void setTopValue(Integer value) {
      topValue = value;
    }

    @Override
    public Integer value(int slot) {
      return Integer.valueOf(docIDs[slot]);
    }

    @Override
    public int compareTop(int doc) {
      int docValue = docBase + doc;
      return Integer.compare(topValue, docValue);
    }
  }
  
  /** Sorts by field's natural Term sort order, using
   *  ordinals.  This is functionally equivalent to {@link
   *  org.apache.lucene.search.FieldComparator.TermValComparator}, but it first resolves the string
   *  to their relative ordinal positions (using the index
   *  returned by {@link AtomicReader#getSortedDocValues(String)}), and
   *  does most comparisons using the ordinals.  For medium
   *  to large results, this comparator will be much faster
   *  than {@link org.apache.lucene.search.FieldComparator.TermValComparator}.  For very small
   *  result sets it may be slower. */
  public static class TermOrdValComparator extends FieldComparator<BytesRef> {
    /* Ords for each slot.
       @lucene.internal */
    final int[] ords;

    /* Values for each slot.
       @lucene.internal */
    final BytesRef[] values;

    /* Which reader last copied a value into the slot. When
       we compare two slots, we just compare-by-ord if the
       readerGen is the same; else we must compare the
       values (slower).
       @lucene.internal */
    final int[] readerGen;

    /* Gen of current reader we are on.
       @lucene.internal */
    int currentReaderGen = -1;

    /* Current reader's doc ord/values.
       @lucene.internal */
    SortedDocValues termsIndex;

    private final String field;

    /* Bottom slot, or -1 if queue isn't full yet
       @lucene.internal */
    int bottomSlot = -1;

    /* Bottom ord (same as ords[bottomSlot] once bottomSlot
       is set).  Cached for faster compares.
       @lucene.internal */
    int bottomOrd;

    /* True if current bottom slot matches the current
       reader.
       @lucene.internal */
    boolean bottomSameReader;

    /* Bottom value (same as values[bottomSlot] once
       bottomSlot is set).  Cached for faster compares.
      @lucene.internal */
    BytesRef bottomValue;

    /** Set by setTopValue. */
    BytesRef topValue;
    boolean topSameReader;
    int topOrd;

    final BytesRef tempBR = new BytesRef();

    /** -1 if missing values are sorted first, 1 if they are
     *  sorted last */
    final int missingSortCmp;
    
    /** Which ordinal to use for a missing value. */
    final int missingOrd;

    /** Creates this, sorting missing values first. */
    public TermOrdValComparator(int numHits, String field) {
      this(numHits, field, false);
    }

    /** Creates this, with control over how missing values
     *  are sorted.  Pass sortMissingLast=true to put
     *  missing values at the end. */
    public TermOrdValComparator(int numHits, String field, boolean sortMissingLast) {
      ords = new int[numHits];
      values = new BytesRef[numHits];
      readerGen = new int[numHits];
      this.field = field;
      if (sortMissingLast) {
        missingSortCmp = 1;
        missingOrd = Integer.MAX_VALUE;
      } else {
        missingSortCmp = -1;
        missingOrd = -1;
      }
    }

    @Override
    public int compare(int slot1, int slot2) {
      if (readerGen[slot1] == readerGen[slot2]) {
        return ords[slot1] - ords[slot2];
      }

      final BytesRef val1 = values[slot1];
      final BytesRef val2 = values[slot2];
      if (val1 == null) {
        if (val2 == null) {
          return 0;
        }
        return missingSortCmp;
      } else if (val2 == null) {
        return -missingSortCmp;
      }
      return val1.compareTo(val2);
    }

    @Override
    public int compareBottom(int doc) {
      assert bottomSlot != -1;
      int docOrd = termsIndex.getOrd(doc);
      if (docOrd == -1) {
        docOrd = missingOrd;
      }
      if (bottomSameReader) {
        // ord is precisely comparable, even in the equal case
        return bottomOrd - docOrd;
      } else if (bottomOrd >= docOrd) {
        // the equals case always means bottom is > doc
        // (because we set bottomOrd to the lower bound in
        // setBottom):
        return 1;
      } else {
        return -1;
      }
    }

    @Override
    public void copy(int slot, int doc) {
      int ord = termsIndex.getOrd(doc);
      if (ord == -1) {
        ord = missingOrd;
        values[slot] = null;
      } else {
        assert ord >= 0;
        if (values[slot] == null) {
          values[slot] = new BytesRef();
        }
        values[slot].copyBytes(termsIndex.lookupOrd(ord));
      }
      ords[slot] = ord;
      readerGen[slot] = currentReaderGen;
    }
    
    /** Retrieves the SortedDocValues for the field in this segment */
    protected SortedDocValues getSortedDocValues(AtomicReaderContext context, String field) throws IOException {
      return DocValues.getSorted(context.reader(), field);
    }
    
    @Override
    public FieldComparator<BytesRef> setNextReader(AtomicReaderContext context) throws IOException {
      termsIndex = getSortedDocValues(context, field);
      currentReaderGen++;

      if (topValue != null) {
        // Recompute topOrd/SameReader
        int ord = termsIndex.lookupTerm(topValue);
        if (ord >= 0) {
          topSameReader = true;
          topOrd = ord;
        } else {
          topSameReader = false;
          topOrd = -ord-2;
        }
      } else {
        topOrd = missingOrd;
        topSameReader = true;
      }
      //System.out.println("  setNextReader topOrd=" + topOrd + " topSameReader=" + topSameReader);

      if (bottomSlot != -1) {
        // Recompute bottomOrd/SameReader
        setBottom(bottomSlot);
      }

      return this;
    }
    
    @Override
    public void setBottom(final int bottom) {
      bottomSlot = bottom;

      bottomValue = values[bottomSlot];
      if (currentReaderGen == readerGen[bottomSlot]) {
        bottomOrd = ords[bottomSlot];
        bottomSameReader = true;
      } else {
        if (bottomValue == null) {
          // missingOrd is null for all segments
          assert ords[bottomSlot] == missingOrd;
          bottomOrd = missingOrd;
          bottomSameReader = true;
          readerGen[bottomSlot] = currentReaderGen;
        } else {
          final int ord = termsIndex.lookupTerm(bottomValue);
          if (ord < 0) {
            bottomOrd = -ord - 2;
            bottomSameReader = false;
          } else {
            bottomOrd = ord;
            // exact value match
            bottomSameReader = true;
            readerGen[bottomSlot] = currentReaderGen;            
            ords[bottomSlot] = bottomOrd;
          }
        }
      }
    }

    @Override
    public void setTopValue(BytesRef value) {
      // null is fine: it means the last doc of the prior
      // search was missing this value
      topValue = value;
      //System.out.println("setTopValue " + topValue);
    }

    @Override
    public BytesRef value(int slot) {
      return values[slot];
    }

    @Override
    public int compareTop(int doc) {

      int ord = termsIndex.getOrd(doc);
      if (ord == -1) {
        ord = missingOrd;
      }

      if (topSameReader) {
        // ord is precisely comparable, even in the equal
        // case
        //System.out.println("compareTop doc=" + doc + " ord=" + ord + " ret=" + (topOrd-ord));
        return topOrd - ord;
      } else if (ord <= topOrd) {
        // the equals case always means doc is < value
        // (because we set lastOrd to the lower bound)
        return 1;
      } else {
        return -1;
      }
    }

    @Override
    public int compareValues(BytesRef val1, BytesRef val2) {
      if (val1 == null) {
        if (val2 == null) {
          return 0;
        }
        return missingSortCmp;
      } else if (val2 == null) {
        return -missingSortCmp;
      }
      return val1.compareTo(val2);
    }
  }
  
  /** Sorts by field's natural Term sort order.  All
   *  comparisons are done using BytesRef.compareTo, which is
   *  slow for medium to large result sets but possibly
   *  very fast for very small results sets. */
  // TODO: should we remove this?  who really uses it?
  public static final class TermValComparator extends FieldComparator<BytesRef> {

    // sentinels, just used internally in this comparator
    private static final byte[] MISSING_BYTES = new byte[0];
    // TODO: this is seriously not good, we should nuke this comparator, or
    // instead we should represent missing as null, or use missingValue from the user...
    // but it was always this way...
    private final BytesRef MISSING_BYTESREF = new BytesRef(MISSING_BYTES);
    
    private final BytesRef[] values;
    private final BytesRef[] tempBRs;
    private BinaryDocValues docTerms;
    private Bits docsWithField;
    private final String field;
    private BytesRef bottom;
    private BytesRef topValue;

    // TODO: add missing first/last support here?

    /** Sole constructor. */
    TermValComparator(int numHits, String field) {
      values = new BytesRef[numHits];
      tempBRs = new BytesRef[numHits];
      this.field = field;
    }

    @Override
    public int compare(int slot1, int slot2) {
      final BytesRef val1 = values[slot1];
      final BytesRef val2 = values[slot2];
      return compareValues(val1, val2);
    }

    @Override
    public int compareBottom(int doc) {
      final BytesRef comparableBytes = getComparableBytes(doc, docTerms.get(doc));
      return compareValues(bottom, comparableBytes);
    }

    @Override
    public void copy(int slot, int doc) {
      final BytesRef comparableBytes = getComparableBytes(doc, docTerms.get(doc));
      if (comparableBytes == MISSING_BYTESREF) {
        values[slot] = MISSING_BYTESREF;
      } else {
        if (tempBRs[slot] == null) {
          tempBRs[slot] = new BytesRef();
        }
        values[slot] = tempBRs[slot];
        values[slot].copyBytes(comparableBytes);
      }
    }

    @Override
    public FieldComparator<BytesRef> setNextReader(AtomicReaderContext context) throws IOException {
      docTerms = DocValues.getBinary(context.reader(), field);
      docsWithField = DocValues.getDocsWithField(context.reader(), field);
      return this;
    }
    
    @Override
    public void setBottom(final int bottom) {
      this.bottom = values[bottom];
    }

    @Override
    public void setTopValue(BytesRef value) {
      if (value == null) {
        throw new IllegalArgumentException("value cannot be null");
      }
      if (value.bytes == MISSING_BYTES) {
        value = MISSING_BYTESREF;
      }
      topValue = value;
    }

    @Override
    public BytesRef value(int slot) {
      return values[slot];
    }

    @Override
    public int compareValues(BytesRef val1, BytesRef val2) {
      // missing always sorts first:
      if (val1 == MISSING_BYTESREF) {
        if (val2 == MISSING_BYTESREF) {
          return 0;
        }
        return -1;
      } else if (val2 == MISSING_BYTESREF) {
        return 1;
      }
      return val1.compareTo(val2);
    }

    @Override
    public int compareTop(int doc) {
      final BytesRef comparableBytes = getComparableBytes(doc, docTerms.get(doc));
      return compareValues(topValue, comparableBytes);
    }

    /**
     * Given a document and a term, return the term itself if it exists or
     * {@link #MISSING_BYTESREF} otherwise.
     */
    private BytesRef getComparableBytes(int doc, BytesRef term) {
      if (term.length == 0 && docsWithField.get(doc) == false) {
        return MISSING_BYTESREF;
      }
      return term;
    }
  }
}
