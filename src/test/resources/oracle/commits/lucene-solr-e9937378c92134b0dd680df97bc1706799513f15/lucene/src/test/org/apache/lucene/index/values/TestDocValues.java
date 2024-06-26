package org.apache.lucene.index.values;

/**
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
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.index.values.IndexDocValues.SortedSource;
import org.apache.lucene.index.values.IndexDocValues.Source;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FloatsRef;
import org.apache.lucene.util.LongsRef;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.UnicodeUtil;
import org.apache.lucene.util._TestUtil;

public class TestDocValues extends LuceneTestCase {

  // TODO -- for sorted test, do our own Sort of the
  // values and verify it's identical

  public void testBytesStraight() throws IOException {
    runTestBytes(Bytes.Mode.STRAIGHT, true);
    runTestBytes(Bytes.Mode.STRAIGHT, false);
  }

  public void testBytesDeref() throws IOException {
    runTestBytes(Bytes.Mode.DEREF, true);
    runTestBytes(Bytes.Mode.DEREF, false);
  }

  public void testBytesSorted() throws IOException {
    runTestBytes(Bytes.Mode.SORTED, true);
    runTestBytes(Bytes.Mode.SORTED, false);
  }
  
  public void runTestBytes(final Bytes.Mode mode, final boolean fixedSize)
      throws IOException {

    final BytesRef bytesRef = new BytesRef();

    final Comparator<BytesRef> comp = mode == Bytes.Mode.SORTED ? BytesRef
        .getUTF8SortedAsUnicodeComparator() : null;

    Directory dir = newDirectory();
    final AtomicLong trackBytes = new AtomicLong(0);
    Writer w = Bytes.getWriter(dir, "test", mode, comp, fixedSize, trackBytes, newIOContext(random));
    int maxDoc = 220;
    final String[] values = new String[maxDoc];
    final int fixedLength = 3 + random.nextInt(7);
    for (int i = 0; i < 100; i++) {
      final String s;
      if (i > 0 && random.nextInt(5) <= 2) {
        // use prior value
        s = values[2 * random.nextInt(i)];
      } else {
        s = _TestUtil.randomFixedByteLengthUnicodeString(random, fixedSize? fixedLength : 1 + random.nextInt(39));
      }
      values[2 * i] = s;

      UnicodeUtil.UTF16toUTF8(s, 0, s.length(), bytesRef);
      w.add(2 * i, bytesRef);
    }
    w.finish(maxDoc);
    assertEquals(0, trackBytes.get());

    IndexDocValues r = Bytes.getValues(dir, "test", mode, fixedSize, maxDoc, newIOContext(random));
    for (int iter = 0; iter < 2; iter++) {
      ValuesEnum bytesEnum = getEnum(r);
      assertNotNull("enum is null", bytesEnum);
      BytesRef ref = bytesEnum.bytes();

      for (int i = 0; i < 2; i++) {
        final int idx = 2 * i;
        assertEquals("doc: " + idx, idx, bytesEnum.advance(idx));
        String utf8String = ref.utf8ToString();
        assertEquals("doc: " + idx + " lenLeft: " + values[idx].length()
            + " lenRight: " + utf8String.length(), values[idx], utf8String);
      }
      assertEquals(ValuesEnum.NO_MORE_DOCS, bytesEnum.advance(maxDoc));
      assertEquals(ValuesEnum.NO_MORE_DOCS, bytesEnum.advance(maxDoc + 1));

      bytesEnum.close();
    }

    // Verify we can load source twice:
    for (int iter = 0; iter < 2; iter++) {
      Source s;
      IndexDocValues.SortedSource ss;
      if (mode == Bytes.Mode.SORTED) {
        s = ss = getSortedSource(r, comp);
      } else {
        s = getSource(r);
        ss = null;
      }
      for (int i = 0; i < 100; i++) {
        final int idx = 2 * i;
        assertNotNull("doc " + idx + "; value=" + values[idx], s.getBytes(idx,
            bytesRef));
        assertEquals("doc " + idx, values[idx], s.getBytes(idx, bytesRef)
            .utf8ToString());
        if (ss != null) {
          assertEquals("doc " + idx, values[idx], ss.getByOrd(ss.ord(idx),
              bytesRef).utf8ToString());
         int ord = ss
              .getByValue(new BytesRef(values[idx]));
          assertTrue(ord >= 0);
          assertEquals(ss.ord(idx), ord);
        }
      }

      // Lookup random strings:
      if (mode == Bytes.Mode.SORTED) {
        final int numValues = ss.getValueCount();
        for (int i = 0; i < 1000; i++) {
          BytesRef bytesValue = new BytesRef(_TestUtil.randomFixedByteLengthUnicodeString(random, fixedSize? fixedLength : 1 + random.nextInt(39)));
          int ord = ss.getByValue(bytesValue);
          if (ord >= 0) {
            assertTrue(bytesValue
                .bytesEquals(ss.getByOrd(ord, bytesRef)));
            int count = 0;
            for (int k = 0; k < 100; k++) {
              if (bytesValue.utf8ToString().equals(values[2 * k])) {
                assertEquals(ss.ord(2 * k), ord);
                count++;
              }
            }
            assertTrue(count > 0);
          } else {
            assert ord < 0;
            int insertIndex = (-ord)-1;
            if (insertIndex == 0) {
              final BytesRef firstRef = ss.getByOrd(1, bytesRef);
              // random string was before our first
              assertTrue(firstRef.compareTo(bytesValue) > 0);
            } else if (insertIndex == numValues) {
              final BytesRef lastRef = ss.getByOrd(numValues-1, bytesRef);
              // random string was after our last
              assertTrue(lastRef.compareTo(bytesValue) < 0);
            } else {
              final BytesRef before = (BytesRef) ss.getByOrd(insertIndex-1, bytesRef)
              .clone();
              BytesRef after = ss.getByOrd(insertIndex, bytesRef);
              assertTrue(comp.compare(before, bytesValue) < 0);
              assertTrue(comp.compare(bytesValue, after) < 0);
            }
          }
        }
      }
    }

    r.close();
    dir.close();
  }

  public void testInts() throws IOException {
    long[] maxMin = new long[] { 
        Long.MIN_VALUE, Long.MAX_VALUE,
        1, Long.MAX_VALUE,
        0, Long.MAX_VALUE,
        -1, Long.MAX_VALUE,
        Long.MIN_VALUE, -1,
        random.nextInt(), random.nextInt() };
    for (int j = 0; j < maxMin.length; j+=2) {
      long maxV = 1;
      final int NUM_VALUES = 777 + random.nextInt(777);
      final long[] values = new long[NUM_VALUES];
      for (int rx = 1; rx < 63; rx++, maxV *= 2) {
        Directory dir = newDirectory();
        final AtomicLong trackBytes = new AtomicLong(0);
        Writer w = Ints.getWriter(dir, "test", false, trackBytes, newIOContext(random));
        values[0] = maxMin[j];
        w.add(0, values[0]);
        values[1] = maxMin[j+1];
        w.add(1, values[1]);
        for (int i = 2; i < NUM_VALUES; i++) {
          final long v = random.nextLong() % (1 + maxV);
          values[i] = v;
          w.add(i, v);
        }
        final int additionalDocs = 1 + random.nextInt(9);
        w.finish(NUM_VALUES + additionalDocs);
        assertEquals(0, trackBytes.get());

        IndexDocValues r = Ints.getValues(dir, "test", false, newIOContext(random));
        for (int iter = 0; iter < 2; iter++) {
          Source s = getSource(r);
          for (int i = 0; i < NUM_VALUES; i++) {
            final long v = s.getInt(i);
            assertEquals("index " + i, values[i], v);
          }
        }

        for (int iter = 0; iter < 2; iter++) {
          ValuesEnum iEnum = getEnum(r);
          LongsRef ints = iEnum.getInt();
          for (int i = 0; i < NUM_VALUES + additionalDocs; i++) {
            assertEquals(i, iEnum.nextDoc());
            if (i < NUM_VALUES) {
              assertEquals(values[i], ints.get());
            } else {
              assertEquals(0, ints.get());
            }
          }
          assertEquals(ValuesEnum.NO_MORE_DOCS, iEnum.nextDoc());
          iEnum.close();
        }

        for (int iter = 0; iter < 2; iter++) {
          ValuesEnum iEnum = getEnum(r);
          LongsRef ints = iEnum.getInt();
          for (int i = 0; i < NUM_VALUES + additionalDocs; i += 1 + random.nextInt(25)) {
            assertEquals(i, iEnum.advance(i));
            if (i < NUM_VALUES) {
              assertEquals(values[i], ints.get());
            } else {
              assertEquals(0, ints.get());
            }
          }
          assertEquals(ValuesEnum.NO_MORE_DOCS, iEnum.advance(NUM_VALUES + additionalDocs));
          iEnum.close();
        }
        r.close();
        dir.close();
      }
    }
  }

  public void testFloats4() throws IOException {
    runTestFloats(4, 0.00001);
  }

  private void runTestFloats(int precision, double delta) throws IOException {
    Directory dir = newDirectory();
    final AtomicLong trackBytes = new AtomicLong(0);
    Writer w = Floats.getWriter(dir, "test", precision, trackBytes, newIOContext(random));
    final int NUM_VALUES = 777 + random.nextInt(777);;
    final double[] values = new double[NUM_VALUES];
    for (int i = 0; i < NUM_VALUES; i++) {
      final double v = precision == 4 ? random.nextFloat() : random
          .nextDouble();
      values[i] = v;
      w.add(i, v);
    }
    final int additionalValues = 1 + random.nextInt(10);
    w.finish(NUM_VALUES + additionalValues);
    assertEquals(0, trackBytes.get());

    IndexDocValues r = Floats.getValues(dir, "test", NUM_VALUES + additionalValues, newIOContext(random));
    for (int iter = 0; iter < 2; iter++) {
      Source s = getSource(r);
      for (int i = 0; i < NUM_VALUES; i++) {
        assertEquals(values[i], s.getFloat(i), 0.0f);
      }
    }

    for (int iter = 0; iter < 2; iter++) {
      ValuesEnum fEnum = getEnum(r);
      FloatsRef floats = fEnum.getFloat();
      for (int i = 0; i < NUM_VALUES + additionalValues; i++) {
        assertEquals(i, fEnum.nextDoc());
        if (i < NUM_VALUES) {
          assertEquals(values[i], floats.get(), delta);
        } else {
          assertEquals(0.0d, floats.get(), delta);
        }
      }
      assertEquals(ValuesEnum.NO_MORE_DOCS, fEnum.nextDoc());
      fEnum.close();
    }
    for (int iter = 0; iter < 2; iter++) {
      ValuesEnum fEnum = getEnum(r);
      FloatsRef floats = fEnum.getFloat();
      for (int i = 0; i < NUM_VALUES + additionalValues; i += 1 + random.nextInt(25)) {
        assertEquals(i, fEnum.advance(i));
        if (i < NUM_VALUES) {
          assertEquals(values[i], floats.get(), delta);
        } else {
          assertEquals(0.0d, floats.get(), delta);
        }
      }
      assertEquals(ValuesEnum.NO_MORE_DOCS, fEnum.advance(NUM_VALUES + additionalValues));
      fEnum.close();
    }

    r.close();
    dir.close();
  }

  public void testFloats8() throws IOException {
    runTestFloats(8, 0.0);
  }
  
  private ValuesEnum getEnum(IndexDocValues values) throws IOException {
    return random.nextBoolean() ? values.getEnum() : getSource(values).getEnum();
  }

  private Source getSource(IndexDocValues values) throws IOException {
    // getSource uses cache internally
    return random.nextBoolean() ? values.load() : values.getSource();
  }

  private SortedSource getSortedSource(IndexDocValues values,
      Comparator<BytesRef> comparator) throws IOException {
    // getSortedSource uses cache internally
    return random.nextBoolean() ? values.loadSorted(comparator) : values
        .getSortedSorted(comparator);
  }
}
