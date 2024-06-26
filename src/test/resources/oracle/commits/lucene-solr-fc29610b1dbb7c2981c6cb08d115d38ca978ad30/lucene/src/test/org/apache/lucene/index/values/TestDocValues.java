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

import org.apache.lucene.index.values.DocValues.SortedSource;
import org.apache.lucene.index.values.DocValues.Source;
import org.apache.lucene.store.Directory;
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
    Writer w = Bytes.getWriter(dir, "test", mode, comp, fixedSize, trackBytes);
    int maxDoc = 220;
    final String[] values = new String[maxDoc];
    final int lenMin, lenMax;
    if (fixedSize) {
      lenMin = lenMax = 3 + random.nextInt(7);
    } else {
      lenMin = 1;
      lenMax = 15 + random.nextInt(6);
    }
    for (int i = 0; i < 100; i++) {
      final String s;
      if (i > 0 && random.nextInt(5) <= 2) {
        // use prior value
        s = values[2 * random.nextInt(i)];
      } else {
        s = _TestUtil.randomUnicodeString(random, lenMin, lenMax);
      }
      values[2 * i] = s;

      UnicodeUtil.UTF16toUTF8(s, 0, s.length(), bytesRef);
      w.add(2 * i, bytesRef);
    }
    w.finish(maxDoc);
    assertEquals(0, trackBytes.get());

    DocValues r = Bytes.getValues(dir, "test", mode, fixedSize, maxDoc);
    for (int iter = 0; iter < 2; iter++) {
      ValuesEnum bytesEnum = r.getEnum();
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
      DocValues.SortedSource ss;
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
          DocValues.SortedSource.LookupResult result = ss
              .getByValue(new BytesRef(values[idx]));
          assertTrue(result.found);
          assertEquals(ss.ord(idx), result.ord);
        }
      }

      // Lookup random strings:
      if (mode == Bytes.Mode.SORTED) {
        final int numValues = ss.getValueCount();
        for (int i = 0; i < 1000; i++) {
          BytesRef bytesValue = new BytesRef(_TestUtil.randomUnicodeString(
              random, lenMin, lenMax));
          SortedSource.LookupResult result = ss.getByValue(bytesValue);
          if (result.found) {
            assert result.ord > 0;
            assertTrue(bytesValue
                .bytesEquals(ss.getByOrd(result.ord, bytesRef)));
            int count = 0;
            for (int k = 0; k < 100; k++) {
              if (bytesValue.utf8ToString().equals(values[2 * k])) {
                assertEquals(ss.ord(2 * k), result.ord);
                count++;
              }
            }
            assertTrue(count > 0);
          } else {
            assert result.ord >= 0;
            if (result.ord == 0) {
              final BytesRef firstRef = ss.getByOrd(1, bytesRef);
              // random string was before our first
              assertTrue(firstRef.compareTo(bytesValue) > 0);
            } else if (result.ord == numValues) {
              final BytesRef lastRef = ss.getByOrd(numValues, bytesRef);
              // random string was after our last
              assertTrue(lastRef.compareTo(bytesValue) < 0);
            } else {
              // random string fell between two of our values
              final BytesRef before = (BytesRef) ss.getByOrd(result.ord,
                  bytesRef).clone();
              final BytesRef after = ss.getByOrd(result.ord + 1, bytesRef);
              assertTrue(before.compareTo(bytesValue) < 0);
              assertTrue(bytesValue.compareTo(after) < 0);

            }
          }
        }
      }
    }

    r.close();
    dir.close();
  }

  public void testInts() throws IOException {
    long maxV = 1;
    final int NUM_VALUES = 777 + random.nextInt(777);
    final long[] values = new long[NUM_VALUES];
    for (int rx = 1; rx < 63; rx++, maxV *= 2) {
      Directory dir = newDirectory();
      final AtomicLong trackBytes = new AtomicLong(0);
      Writer w = Ints.getWriter(dir, "test", false, trackBytes);
      for (int i = 0; i < NUM_VALUES; i++) {
        final long v = random.nextLong() % (1 + maxV);
        values[i] = v;
        w.add(i, v);
      }
      final int additionalDocs = 1 + random.nextInt(9);
      w.finish(NUM_VALUES + additionalDocs);
      assertEquals(0, trackBytes.get());


      DocValues r = Ints.getValues(dir, "test", false);
      for (int iter = 0; iter < 2; iter++) {
        Source s = getSource(r);
        for (int i = 0; i < NUM_VALUES; i++) {
          final long v = s.getInt(i);
          assertEquals("index " + i, values[i], v);
        }
      }

      for (int iter = 0; iter < 2; iter++) {
        ValuesEnum iEnum = r.getEnum();
        LongsRef ints = iEnum.getInt();
        for (int i = 0; i < NUM_VALUES; i++) {
          assertEquals(i, iEnum.nextDoc());
          assertEquals(values[i], ints.get());
        }
        if (iEnum.docID() < NUM_VALUES - 1) {
          assertEquals(NUM_VALUES - 1, iEnum.advance(NUM_VALUES - 1));
        }
        for (int i = NUM_VALUES; i < NUM_VALUES + additionalDocs; i++) {
          assertEquals(ValuesEnum.NO_MORE_DOCS, iEnum.nextDoc());
        }

        iEnum.close();
      }

      for (int iter = 0; iter < 2; iter++) {
        ValuesEnum iEnum = r.getEnum();
        LongsRef ints = iEnum.getInt();
        for (int i = 0; i < NUM_VALUES; i += 1 + random.nextInt(25)) {
          assertEquals(i, iEnum.advance(i));
          assertEquals(values[i], ints.get());
        }
        if (iEnum.docID() < NUM_VALUES - 1) {
          assertEquals(NUM_VALUES - 1, iEnum.advance(NUM_VALUES - 1));
        }
        for (int i = NUM_VALUES; i < NUM_VALUES + additionalDocs; i++) {
          assertEquals(ValuesEnum.NO_MORE_DOCS, iEnum.nextDoc());
        }

        iEnum.close();
      }
      r.close();
      dir.close();
    }
  }

  public void testFloats4() throws IOException {
    runTestFloats(4, 0.00001);
  }

  private void runTestFloats(int precision, double delta) throws IOException {
    Directory dir = newDirectory();
    final AtomicLong trackBytes = new AtomicLong(0);
    Writer w = Floats.getWriter(dir, "test", precision, trackBytes);
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

    DocValues r = Floats.getValues(dir, "test", NUM_VALUES + additionalValues);
    for (int iter = 0; iter < 2; iter++) {
      Source s = getSource(r);
      for (int i = 0; i < NUM_VALUES; i++) {
        assertEquals(values[i], s.getFloat(i), 0.0f);
      }
    }

    for (int iter = 0; iter < 2; iter++) {
      ValuesEnum fEnum = r.getEnum();
      FloatsRef floats = fEnum.getFloat();
      for (int i = 0; i < NUM_VALUES; i++) {
        assertEquals(i, fEnum.nextDoc());
        assertEquals(values[i], floats.get(), delta);
      }
      for (int i = NUM_VALUES; i < NUM_VALUES + additionalValues; i++) {
        assertEquals(ValuesEnum.NO_MORE_DOCS, fEnum.nextDoc());
      }
      fEnum.close();
    }
    for (int iter = 0; iter < 2; iter++) {
      ValuesEnum fEnum = r.getEnum();
      FloatsRef floats = fEnum.getFloat();
      for (int i = 0; i < NUM_VALUES; i += 1 + random.nextInt(25)) {
        assertEquals(i, fEnum.advance(i));
        assertEquals(values[i], floats.get(), delta);
      }
      for (int i = NUM_VALUES; i < NUM_VALUES + additionalValues; i++) {
        assertEquals(ValuesEnum.NO_MORE_DOCS, fEnum.advance(i));
      }
      fEnum.close();
    }

    r.close();
    dir.close();
  }

  public void testFloats8() throws IOException {
    runTestFloats(8, 0.0);
  }

  private Source getSource(DocValues values) throws IOException {
    // getSource uses cache internally
    return random.nextBoolean() ? values.load() : values.getSource();
  }

  private SortedSource getSortedSource(DocValues values,
      Comparator<BytesRef> comparator) throws IOException {
    // getSortedSource uses cache internally
    return random.nextBoolean() ? values.loadSorted(comparator) : values
        .getSortedSorted(comparator);
  }
}
