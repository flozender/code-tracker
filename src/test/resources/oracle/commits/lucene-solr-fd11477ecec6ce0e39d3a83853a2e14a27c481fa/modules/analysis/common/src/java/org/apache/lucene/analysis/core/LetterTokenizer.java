package org.apache.lucene.analysis.core;

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

import java.io.Reader;

import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.Version;

/**
 * A LetterTokenizer is a tokenizer that divides text at non-letters. That's to
 * say, it defines tokens as maximal strings of adjacent letters, as defined by
 * java.lang.Character.isLetter() predicate.
 * <p>
 * Note: this does a decent job for most European languages, but does a terrible
 * job for some Asian languages, where words are not separated by spaces.
 * </p>
 * <p>
 * <a name="version"/>
 * You must specify the required {@link Version} compatibility when creating
 * {@link LetterTokenizer}:
 * <ul>
 * <li>As of 3.1, {@link CharTokenizer} uses an int based API to normalize and
 * detect token characters. See {@link CharTokenizer#isTokenChar(int)} and
 * {@link CharTokenizer#normalize(int)} for details.</li>
 * </ul>
 * </p>
 */

public class LetterTokenizer extends CharTokenizer {
  
  /**
   * Construct a new LetterTokenizer.
   * 
   * @param matchVersion
   *          Lucene version to match See {@link <a href="#version">above</a>}
   * @param in
   *          the input to split up into tokens
   */
  public LetterTokenizer(Version matchVersion, Reader in) {
    super(matchVersion, in);
  }
  
  /**
   * Construct a new LetterTokenizer using a given {@link AttributeSource}.
   * 
   * @param matchVersion
   *          Lucene version to match See {@link <a href="#version">above</a>}
   * @param source
   *          the attribute source to use for this {@link Tokenizer}
   * @param in
   *          the input to split up into tokens
   */
  public LetterTokenizer(Version matchVersion, AttributeSource source, Reader in) {
    super(matchVersion, source, in);
  }
  
  /**
   * Construct a new LetterTokenizer using a given
   * {@link org.apache.lucene.util.AttributeSource.AttributeFactory}.
   * 
   * @param matchVersion
   *          Lucene version to match See {@link <a href="#version">above</a>}
   * @param factory
   *          the attribute factory to use for this {@link Tokenizer}
   * @param in
   *          the input to split up into tokens
   */
  public LetterTokenizer(Version matchVersion, AttributeFactory factory, Reader in) {
    super(matchVersion, factory, in);
  }
  
  /**
   * Construct a new LetterTokenizer.
   * 
   * @deprecated use {@link #LetterTokenizer(Version, Reader)} instead. This
   *             will be removed in Lucene 4.0.
   */
  @Deprecated
  public LetterTokenizer(Reader in) {
    super(Version.LUCENE_30, in);
  }
  
  /**
   * Construct a new LetterTokenizer using a given {@link AttributeSource}. 
   * @deprecated
   * use {@link #LetterTokenizer(Version, AttributeSource, Reader)} instead.
   * This will be removed in Lucene 4.0.
   */
  @Deprecated
  public LetterTokenizer(AttributeSource source, Reader in) {
    super(Version.LUCENE_30, source, in);
  }
  
  /**
   * Construct a new LetterTokenizer using a given
   * {@link org.apache.lucene.util.AttributeSource.AttributeFactory}.
   * 
   * @deprecated use {@link #LetterTokenizer(Version, AttributeSource.AttributeFactory, Reader)}
   *             instead. This will be removed in Lucene 4.0.
   */
  @Deprecated
  public LetterTokenizer(AttributeFactory factory, Reader in) {
    super(Version.LUCENE_30, factory, in);
  }
  
  /** Collects only characters which satisfy
   * {@link Character#isLetter(int)}.*/
  @Override
  protected boolean isTokenChar(int c) {
    return Character.isLetter(c);
  }
}
