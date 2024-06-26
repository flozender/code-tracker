package org.apache.lucene.analysis;

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

import java.util.Arrays;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.util.Version;

/**
 * A simple class that stores Strings as char[]'s in a
 * hash table.  Note that this is not a general purpose
 * class.  For example, it cannot remove items from the
 * set, nor does it resize its hash table to be smaller,
 * etc.  It is designed to be quick to test if a char[]
 * is in the set without the necessity of converting it
 * to a String first.
 * <p>You must specify the required {@link Version}
 * compatibility when creating {@link CharArraySet}:
 * <ul>
 *   <li> As of 3.1, supplementary characters are
 *       properly lowercased.</li>
 * </ul>
 * Before 3.1 supplementary characters could not be
 * lowercased correctly due to the lack of Unicode 4
 * support in JDK 1.4. To use instances of
 * {@link CharArraySet} with the behavior before Lucene
 * 3.1 pass a {@link Version} < 3.1 to the constructors.
 * <P>
 * <em>Please note:</em> This class implements {@link java.util.Set Set} but
 * does not behave like it should in all cases. The generic type is
 * {@code Set<Object>}, because you can add any object to it,
 * that has a string representation. The add methods will use
 * {@link Object#toString} and store the result using a {@code char[]}
 * buffer. The same behavior have the {@code contains()} methods.
 * The {@link #iterator()} returns an {@code Iterator<String>}.
 * For type safety also {@link #stringIterator()} is provided.
 */
public class CharArraySet extends AbstractSet<Object> {
  public static final CharArraySet EMPTY_SET = new CharArraySet(CharArrayMap.<Object>emptyMap());
  private static final Object PLACEHOLDER = new Object();
  
  private final CharArrayMap<Object> map;
  
  /**
   * Create set with enough capacity to hold startSize terms
   * 
   * @param matchVersion
   *          compatibility match version see <a href="#version">Version
   *          note</a> above for details.
   * @param startSize
   *          the initial capacity
   * @param ignoreCase
   *          <code>false</code> if and only if the set should be case sensitive
   *          otherwise <code>true</code>.
   */
  public CharArraySet(Version matchVersion, int startSize, boolean ignoreCase) {
    this(new CharArrayMap<Object>(matchVersion, startSize, ignoreCase));
  }

  /**
   * Creates a set from a Collection of objects. 
   * 
   * @param matchVersion
   *          compatibility match version see <a href="#version">Version
   *          note</a> above for details.
   * @param c
   *          a collection whose elements to be placed into the set
   * @param ignoreCase
   *          <code>false</code> if and only if the set should be case sensitive
   *          otherwise <code>true</code>.
   */
  public CharArraySet(Version matchVersion, Collection<?> c, boolean ignoreCase) {
    this(matchVersion, c.size(), ignoreCase);
    addAll(c);
  }

  /**
   * Creates a set with enough capacity to hold startSize terms
   * 
   * @param startSize
   *          the initial capacity
   * @param ignoreCase
   *          <code>false</code> if and only if the set should be case sensitive
   *          otherwise <code>true</code>.
   * @deprecated use {@link #CharArraySet(Version, int, boolean)} instead
   */
  @Deprecated
  public CharArraySet(int startSize, boolean ignoreCase) {
    this(Version.LUCENE_30, startSize, ignoreCase);
  }
  
  /**
   * Creates a set from a Collection of objects. 
   * 
   * @param c
   *          a collection whose elements to be placed into the set
   * @param ignoreCase
   *          <code>false</code> if and only if the set should be case sensitive
   *          otherwise <code>true</code>.
   * @deprecated use {@link #CharArraySet(Version, Collection, boolean)} instead         
   */  
  @Deprecated
  public CharArraySet(Collection<?> c, boolean ignoreCase) {
    this(Version.LUCENE_30, c.size(), ignoreCase);
    addAll(c);
  }
  
  /** Create set from the specified map (internal only), used also by {@link CharArrayMap#keySet()} */
  CharArraySet(final CharArrayMap<Object> map){
    this.map = map;
  }
  
  /** Clears all entries in this set. This method is supported for reusing, but not {@link Set#remove}. */
  @Override
  public void clear() {
    map.clear();
  }

  /** true if the <code>len</code> chars of <code>text</code> starting at <code>off</code>
   * are in the set */
  public boolean contains(char[] text, int off, int len) {
    return map.containsKey(text, off, len);
  }

  /** true if the <code>CharSequence</code> is in the set */
  public boolean contains(CharSequence cs) {
    return map.containsKey(cs);
  }

  @Override
  public boolean contains(Object o) {
    return map.containsKey(o);
  }

  @Override
  public boolean add(Object o) {
    return map.put(o, PLACEHOLDER) == null;
  }

  /** Add this CharSequence into the set */
  public boolean add(CharSequence text) {
    return map.put(text, PLACEHOLDER) == null;
  }
  
  /** Add this String into the set */
  public boolean add(String text) {
    return map.put(text, PLACEHOLDER) == null;
  }

  /** Add this char[] directly to the set.
   * If ignoreCase is true for this Set, the text array will be directly modified.
   * The user should never modify this text array after calling this method.
   */
  public boolean add(char[] text) {
    return map.put(text, PLACEHOLDER) == null;
  }

  @Override
  public int size() {
    return map.size();
  }
  
  /**
   * Returns an unmodifiable {@link CharArraySet}. This allows to provide
   * unmodifiable views of internal sets for "read-only" use.
   * 
   * @param set
   *          a set for which the unmodifiable set is returned.
   * @return an new unmodifiable {@link CharArraySet}.
   * @throws NullPointerException
   *           if the given set is <code>null</code>.
   */
  public static CharArraySet unmodifiableSet(CharArraySet set) {
    if (set == null)
      throw new NullPointerException("Given set is null");
    if (set == EMPTY_SET)
      return EMPTY_SET;
    if (set.map instanceof CharArrayMap.UnmodifiableCharArrayMap)
      return set;
    return new CharArraySet(CharArrayMap.unmodifiableMap(set.map));
  }

  /**
   * Returns a copy of the given set as a {@link CharArraySet}. If the given set
   * is a {@link CharArraySet} the ignoreCase property will be preserved.
   * 
   * @param set
   *          a set to copy
   * @return a copy of the given set as a {@link CharArraySet}. If the given set
   *         is a {@link CharArraySet} the ignoreCase and matchVersion property will be
   *         preserved.
   * @deprecated use {@link #copy(Version, Set)} instead.
   */
  @Deprecated
  public static CharArraySet copy(final Set<?> set) {
    if(set == EMPTY_SET)
      return EMPTY_SET;
    return copy(Version.LUCENE_30, set);
  }
  
  /**
   * Returns a copy of the given set as a {@link CharArraySet}. If the given set
   * is a {@link CharArraySet} the ignoreCase property will be preserved.
   * <p>
   * <b>Note:</b> If you intend to create a copy of another {@link CharArraySet} where
   * the {@link Version} of the source set differs from its copy
   * {@link #CharArraySet(Version, Collection, boolean)} should be used instead.
   * The {@link #copy(Version, Set)} will preserve the {@link Version} of the
   * source set it is an instance of {@link CharArraySet}.
   * </p>
   * 
   * @param matchVersion
   *          compatibility match version see <a href="#version">Version
   *          note</a> above for details. This argument will be ignored if the
   *          given set is a {@link CharArraySet}.
   * @param set
   *          a set to copy
   * @return a copy of the given set as a {@link CharArraySet}. If the given set
   *         is a {@link CharArraySet} the ignoreCase property as well as the
   *         matchVersion will be of the given set will be preserved.
   */
  public static CharArraySet copy(final Version matchVersion, final Set<?> set) {
    if(set == EMPTY_SET)
      return EMPTY_SET;
    if(set instanceof CharArraySet) {
      final CharArraySet source = (CharArraySet) set;
      return new CharArraySet(CharArrayMap.copy(source.map.matchVersion, source.map));
    }
    return new CharArraySet(matchVersion, set, false);
  }
  
  /** The Iterator<String> for this set.  Strings are constructed on the fly, so
   * use <code>nextCharArray</code> for more efficient access.
   * @deprecated Use the standard iterator, which returns {@code char[]} instances.
   */
  @Deprecated
  public class CharArraySetIterator implements Iterator<String> {
    int pos=-1;
    char[] next;
    private CharArraySetIterator() {
      goNext();
    }

    private void goNext() {
      next = null;
      pos++;
      while (pos < map.keys.length && (next=map.keys[pos]) == null) pos++;
    }

    public boolean hasNext() {
      return next != null;
    }

    /** do not modify the returned char[] */
    public char[] nextCharArray() {
      char[] ret = next;
      goNext();
      return ret;
    }

    /** Returns the next String, as a Set<String> would...
     * use nextCharArray() for better efficiency. */
    public String next() {
      return new String(nextCharArray());
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /** returns an iterator of new allocated Strings (an instance of {@link CharArraySetIterator}).
   * @deprecated Use {@link #iterator}, which returns {@code char[]} instances.
   */
  @Deprecated
  public Iterator<String> stringIterator() {
    return new CharArraySetIterator();
  }

  /** Returns an {@link Iterator} depending on the version used:
   * <ul>
   * <li>if {@code matchVersion} &ge; 3.1, it returns {@code char[]} instances in this set.</li>
   * <li>if {@code matchVersion} is 3.0 or older, it returns new
   * allocated Strings, so this method violates the Set interface.
   * It is kept this way for backwards compatibility, normally it should
   * return {@code char[]} on {@code next()}</li>
   * </ul>
   */
  @Override @SuppressWarnings("unchecked")
  public Iterator<Object> iterator() {
    // use the AbstractSet#keySet()'s iterator (to not produce endless recursion)
    return map.matchVersion.onOrAfter(Version.LUCENE_31) ?
      map.originalKeySet().iterator() : (Iterator) stringIterator();
  }
  
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("[");
    for (Object item : this) {
      if (sb.length()>1) sb.append(", ");
      if (item instanceof char[]) {
        sb.append((char[]) item);
      } else {
        sb.append(item);
      }
    }
    return sb.append(']').toString();
  }
}
