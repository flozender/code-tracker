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
import java.util.Random;

import org.apache.lucene.index.DocsEnum;

import com.carrotsearch.randomizedtesting.generators.RandomInts;

/** Wraps a Scorer with additional checks */
final class AssertingBulkScorer extends BulkScorer {

  public static BulkScorer wrap(Random random, BulkScorer other, int maxDoc) {
    if (other == null || other instanceof AssertingBulkScorer) {
      return other;
    }
    return new AssertingBulkScorer(random, other, maxDoc);
  }

  final Random random;
  final BulkScorer in;
  final int maxDoc;
  int max = 0;

  private AssertingBulkScorer(Random random, BulkScorer in, int maxDoc) {
    this.random = random;
    this.in = in;
    this.maxDoc = maxDoc;
  }

  public BulkScorer getIn() {
    return in;
  }

  @Override
  public long cost() {
    return in.cost();
  }

  @Override
  public void score(LeafCollector collector) throws IOException {
    assert max == 0;
    collector = new AssertingLeafCollector(random, collector, 0, DocsEnum.NO_MORE_DOCS);
    if (random.nextBoolean()) {
      try {
        final int next = score(collector, 0, DocsEnum.NO_MORE_DOCS);
        assert next == DocIdSetIterator.NO_MORE_DOCS;
      } catch (UnsupportedOperationException e) {
        in.score(collector);
      }
    } else {
      in.score(collector);
    }
  }

  @Override
  public int score(LeafCollector collector, int min, final int max) throws IOException {
    assert min >= this.max: "Scoring backward: min=" + min + " while previous max was max=" + this.max;
    assert min <= max : "max must be greater than min, got min=" + min + ", and max=" + max;
    this.max = max;
    collector = new AssertingLeafCollector(random, collector, min, max);
    final int next = in.score(collector, min, max);
    assert next >= max;
    if (max >= maxDoc || next >= maxDoc) {
      assert next == DocIdSetIterator.NO_MORE_DOCS;
      return DocIdSetIterator.NO_MORE_DOCS;
    } else {
      return RandomInts.randomIntBetween(random, max, next);
    }
  }

  @Override
  public String toString() {
    return "AssertingBulkScorer(" + in + ")";
  }

}
