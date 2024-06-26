package org.apache.lucene.analysis.tokenattributes;

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

import java.io.Serializable;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util.Attribute;

/** The positionIncrement determines the position of this token
 * relative to the previous Token in a {@link TokenStream}, used in phrase
 * searching.
 *
 * <p>The default value is one.
 *
 * <p>Some common uses for this are:<ul>
 *
 * <li>Set it to zero to put multiple terms in the same position.  This is
 * useful if, e.g., a word has multiple stems.  Searches for phrases
 * including either stem will match.  In this case, all but the first stem's
 * increment should be set to zero: the increment of the first instance
 * should be one.  Repeating a token with an increment of zero can also be
 * used to boost the scores of matches on that token.
 *
 * <li>Set it to values greater than one to inhibit exact phrase matches.
 * If, for example, one does not want phrases to match across removed stop
 * words, then one could build a stop word filter that removes stop words and
 * also sets the increment to the number of stop words removed before each
 * non-stop word.  Then exact phrase queries will only match when the terms
 * occur with no intervening stop words.
 *
 * </ul>
 * 
 * <p><font color="#FF0000">
 * WARNING: The status of the new TokenStream, AttributeSource and Attributes is experimental. 
 * The APIs introduced in these classes with Lucene 2.9 might change in the future. 
 * We will make our best efforts to keep the APIs backwards-compatible.</font>
 * 
 * @see org.apache.lucene.index.TermPositions
 */
public class PositionIncrementAttribute extends Attribute implements Cloneable, Serializable {
  private int positionIncrement = 1;
  
  /** Set the position increment. The default value is one.
   *
   * @param positionIncrement the distance from the prior term
   */
  public void setPositionIncrement(int positionIncrement) {
    if (positionIncrement < 0)
      throw new IllegalArgumentException
        ("Increment must be zero or greater: " + positionIncrement);
    this.positionIncrement = positionIncrement;
  }

  /** Returns the position increment of this Token.
   * @see #setPositionIncrement
   */
  public int getPositionIncrement() {
    return positionIncrement;
  }

  public void clear() {
    this.positionIncrement = 1;
  }
  
  public String toString() {
    return "positionIncrement=" + positionIncrement;
  }

  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    
    if (other instanceof PositionIncrementAttribute) {
      return positionIncrement == ((PositionIncrementAttribute) other).positionIncrement;
    }
 
    return false;
  }

  public int hashCode() {
    return positionIncrement;
  }
  
  public void copyTo(Attribute target) {
    PositionIncrementAttribute t = (PositionIncrementAttribute) target;
    t.setPositionIncrement(positionIncrement);
  }  

}
