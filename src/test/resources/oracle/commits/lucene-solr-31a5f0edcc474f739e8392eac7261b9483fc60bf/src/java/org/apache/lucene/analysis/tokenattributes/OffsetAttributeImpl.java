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

import org.apache.lucene.util.AttributeImpl;

/**
 * The start and end character offset of a Token. 
 * 
 * <p><font color="#FF0000">
 * WARNING: The status of the new TokenStream, AttributeSource and Attributes is experimental. 
 * The APIs introduced in these classes with Lucene 2.9 might change in the future. 
 * We will make our best efforts to keep the APIs backwards-compatible.</font>
 */
public class OffsetAttributeImpl extends AttributeImpl implements OffsetAttribute, Cloneable, Serializable {
  private int startOffset;
  private int endOffset;

  /** Returns this Token's starting offset, the position of the first character
  corresponding to this token in the source text.

  Note that the difference between endOffset() and startOffset() may not be
  equal to termText.length(), as the term text may have been altered by a
  stemmer or some other filter. */
  public int startOffset() {
    return startOffset;
  }

  
  /** Set the starting and ending offset.
    @see #startOffset() and #endOffset()*/
  public void setOffset(int startOffset, int endOffset) {
    this.startOffset = startOffset;
    this.endOffset = endOffset;
  }
  

  /** Returns this Token's ending offset, one greater than the position of the
  last character corresponding to this token in the source text. The length
  of the token in the source text is (endOffset - startOffset). */
  public int endOffset() {
    return endOffset;
  }


  public void clear() {
    startOffset = 0;
    endOffset = 0;
  }
  
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    
    if (other instanceof OffsetAttributeImpl) {
      OffsetAttributeImpl o = (OffsetAttributeImpl) other;
      return o.startOffset == startOffset && o.endOffset == endOffset;
    }
    
    return false;
  }

  public int hashCode() {
    int code = startOffset;
    code = code * 31 + endOffset;
    return code;
  } 
  
  public void copyTo(AttributeImpl target) {
    OffsetAttribute t = (OffsetAttribute) target;
    t.setOffset(startOffset, endOffset);
  }  
}
