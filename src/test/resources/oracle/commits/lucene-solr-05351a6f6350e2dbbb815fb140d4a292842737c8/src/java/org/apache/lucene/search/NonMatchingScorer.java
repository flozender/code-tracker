package org.apache.lucene.search;

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

/** A scorer that matches no document at all. */
class NonMatchingScorer extends Scorer {
  public NonMatchingScorer() { super(null); } // no similarity used
  
  /** @deprecated use {@link #docID()} instead. */
  public int doc() { throw new UnsupportedOperationException(); }
  
  public int docID() { return NO_MORE_DOCS; }

  /** @deprecated use {@link #nextDoc()} instead. */
  public boolean next() throws IOException { return false; }
  
  public int nextDoc() throws IOException { return NO_MORE_DOCS; }

  public float score() { throw new UnsupportedOperationException(); }

  /** @deprecated use {@link #advance(int)} instead. */
  public boolean skipTo(int target) { return false; }
  
  public int advance(int target) { return NO_MORE_DOCS; }

  public Explanation explain(int doc) {
    Explanation e = new Explanation();
    e.setDescription("No document matches.");
    return e;
  }
}
 

