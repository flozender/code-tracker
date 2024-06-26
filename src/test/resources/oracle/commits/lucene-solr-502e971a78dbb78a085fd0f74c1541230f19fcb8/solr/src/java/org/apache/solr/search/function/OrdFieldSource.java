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

package org.apache.solr.search.function;

import org.apache.lucene.index.IndexReader;
import org.apache.solr.search.MutableValue;
import org.apache.solr.search.MutableValueInt;
import org.apache.solr.util.NumberUtils;

import java.io.IOException;
import java.util.Map;

/**
 * Obtains the ordinal of the field value from the default Lucene {@link org.apache.lucene.search.FieldCache} using getStringIndex().
 * <br>
 * The native lucene index order is used to assign an ordinal value for each field value.
 * <br>Field values (terms) are lexicographically ordered by unicode value, and numbered starting at 1.
 * <br>
 * Example:<br>
 *  If there were only three field values: "apple","banana","pear"
 * <br>then ord("apple")=1, ord("banana")=2, ord("pear")=3
 * <p>
 * WARNING: ord() depends on the position in an index and can thus change when other documents are inserted or deleted,
 *  or if a MultiSearcher is used.
 * <br>WARNING: as of Solr 1.4, ord() and rord() can cause excess memory use since they must use a FieldCache entry
 * at the top level reader, while sorting and function queries now use entries at the segment level.  Hence sorting
 * or using a different function query, in addition to ord()/rord() will double memory use.
 * @version $Id$
 */

public class OrdFieldSource extends ValueSource {
  protected String field;

  public OrdFieldSource(String field) {
    this.field = field;
  }

  public String description() {
    return "ord(" + field + ')';
  }


  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    return new StringIndexDocValues(this, reader, field) {
      protected String toTerm(String readableValue) {
        return readableValue;
      }
      
      public float floatVal(int doc) {
        return (float)termsIndex.getOrd(doc);
      }

      public int intVal(int doc) {
        return termsIndex.getOrd(doc);
      }

      public long longVal(int doc) {
        return (long)termsIndex.getOrd(doc);
      }

      public double doubleVal(int doc) {
        return (double)termsIndex.getOrd(doc);
      }

      public int ordVal(int doc) {
        return termsIndex.getOrd(doc);
      }

      public int numOrd() {
        return termsIndex.numOrd();
      }

      public String strVal(int doc) {
        // the string value of the ordinal, not the string itself
        return Integer.toString(termsIndex.getOrd(doc));
      }

      public String toString(int doc) {
        return description() + '=' + intVal(doc);
      }

            @Override
      public ValueFiller getValueFiller() {
        return new ValueFiller() {
          private final MutableValueInt mval = new MutableValueInt();

          @Override
          public MutableValue getValue() {
            return mval;
          }

          @Override
          public void fillValue(int doc) {
            mval.value = termsIndex.getOrd(doc);
            mval.exists = mval.value!=0;
          }
        };
      }
    };
  }

  public boolean equals(Object o) {
    return o.getClass() == OrdFieldSource.class && this.field.equals(((OrdFieldSource)o).field);
  }

  private static final int hcode = OrdFieldSource.class.hashCode();
  public int hashCode() {
    return hcode + field.hashCode();
  };

}
