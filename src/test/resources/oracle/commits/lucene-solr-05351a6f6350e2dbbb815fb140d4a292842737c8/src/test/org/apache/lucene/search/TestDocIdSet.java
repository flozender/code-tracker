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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util._TestUtil;

public class TestDocIdSet extends LuceneTestCase {
  public void testFilteredDocIdSet() throws Exception {
    final int maxdoc=10;
    final DocIdSet innerSet = new DocIdSet() {

        // @Override
        public DocIdSetIterator iterator() {
          return new DocIdSetIterator() {

            int docid = -1;
            
            /** @deprecated use {@link #docID()} instead. */
            public int doc() {
              return docid;
            }

            public int docID() {
              return docid;
            }
            
            /** @deprecated use {@link #nextDoc()} instead. */
            public boolean next() throws IOException {
              return nextDoc() != NO_MORE_DOCS;
            }

            //@Override
            public int nextDoc() throws IOException {
              docid++;
              return docid < maxdoc ? docid : (docid = NO_MORE_DOCS);
            }

            /** @deprecated use {@link #advance(int)} instead. */
            public boolean skipTo(int target) throws IOException {
              return advance(target) != NO_MORE_DOCS;
            }
            
            //@Override
            public int advance(int target) throws IOException {
              while (nextDoc() < target) {}
              return docid;
            }
          };
        } 
      };
	  
		
    DocIdSet filteredSet = new FilteredDocIdSet(innerSet){
        // @Override
        protected boolean match(int docid) {
          return docid%2 == 0;  //validate only even docids
        }	
      };
	  
    DocIdSetIterator iter = filteredSet.iterator();
    ArrayList/*<Integer>*/ list = new ArrayList/*<Integer>*/();
    int doc = iter.advance(3);
    if (doc != DocIdSetIterator.NO_MORE_DOCS) {
      list.add(new Integer(doc));
      while((doc = iter.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
        list.add(new Integer(doc));
      }
    }
	  
    int[] docs = new int[list.size()];
    int c=0;
    Iterator/*<Integer>*/ intIter = list.iterator();
    while(intIter.hasNext()) {
      docs[c++] = ((Integer) intIter.next()).intValue();
    }
    int[] answer = new int[]{4,6,8};
    boolean same = Arrays.equals(answer, docs);
    if (!same) {
      System.out.println("answer: "+_TestUtil.arrayToString(answer));
      System.out.println("gotten: "+_TestUtil.arrayToString(docs));
      fail();
    }
  }
}
