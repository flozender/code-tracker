package org.apache.lucene.util;

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
import java.lang.reflect.Field;

/**
 * Base class for Attributes that can be added to a 
 * {@link org.apache.lucene.util.AttributeSource}.
 * <p>
 * Attributes are used to add data in a dynamic, yet type-safe way to a source
 * of usually streamed objects, e. g. a {@link org.apache.lucene.analysis.TokenStream}.
 * <p><font color="#FF0000">
 * WARNING: The status of the new TokenStream, AttributeSource and Attributes is experimental. 
 * The APIs introduced in these classes with Lucene 2.9 might change in the future. 
 * We will make our best efforts to keep the APIs backwards-compatible.</font>
 */
public abstract class AttributeImpl implements Cloneable, Serializable {  
  /**
   * Clears the values in this Attribute and resets it to its 
   * default value.
   */
  public abstract void clear();
  
  /**
   * The default implementation of this method accesses all declared
   * fields of this object and prints the values in the following syntax:
   * 
   * <pre>
   *   public String toString() {
   *     return "start=" + startOffset + ",end=" + endOffset;
   *   }
   * </pre>
   * 
   * This method may be overridden by subclasses.
   */
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    Class clazz = this.getClass();
    Field[] fields = clazz.getDeclaredFields();
    try {
      for (int i = 0; i < fields.length; i++) {
        Field f = fields[i];
        f.setAccessible(true);
        Object value = f.get(this);
        if (value == null) {
          buffer.append(f.getName() + "=null");
        } else {
          buffer.append(f.getName() + "=" + value);
        }
        if (i < fields.length - 1) {
          buffer.append(',');
        }
      }
    } catch (IllegalAccessException e) {
      // this should never happen, because we're just accessing fields
      // from 'this'
      throw new RuntimeException(e);
    }
    
    return buffer.toString();
  }
  
  /**
   * Subclasses must implement this method and should compute
   * a hashCode similar to this:
   * <pre>
   *   public int hashCode() {
   *     int code = startOffset;
   *     code = code * 31 + endOffset;
   *     return code;
   *   }
   * </pre> 
   * 
   * see also {@link #equals(Object)}
   */
  public abstract int hashCode();
  
  /**
   * All values used for computation of {@link #hashCode()} 
   * should be checked here for equality.
   * 
   * see also {@link Object#equals(Object)}
   */
  public abstract boolean equals(Object other);
  
  /**
   * Copies the values from this Attribute into the passed-in
   * target attribute. The type of the target must match the type
   * of this attribute. 
   */
  public abstract void copyTo(AttributeImpl target);
    
  /**
   * Shallow clone. Subclasses must override this if they 
   * need to clone any members deeply,
   */
  public Object clone() {
    Object clone = null;
    try {
      clone = super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);  // shouldn't happen
    }
    return clone;
  }
}
