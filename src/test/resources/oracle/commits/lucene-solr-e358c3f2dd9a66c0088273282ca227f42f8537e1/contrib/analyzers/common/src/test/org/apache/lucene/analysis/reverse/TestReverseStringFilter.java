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

package org.apache.lucene.analysis.reverse;

import java.io.StringReader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.util.Version;

public class TestReverseStringFilter extends BaseTokenStreamTestCase {
  public void testFilter() throws Exception {
    TokenStream stream = new WhitespaceTokenizer(Version.LUCENE_CURRENT, 
        new StringReader("Do have a nice day"));     // 1-4 length string
    ReverseStringFilter filter = new ReverseStringFilter(Version.LUCENE_CURRENT, stream);
    TermAttribute text = filter.getAttribute(TermAttribute.class);
    assertTrue(filter.incrementToken());
    assertEquals("oD", text.term());
    assertTrue(filter.incrementToken());
    assertEquals("evah", text.term());
    assertTrue(filter.incrementToken());
    assertEquals("a", text.term());
    assertTrue(filter.incrementToken());
    assertEquals("ecin", text.term());
    assertTrue(filter.incrementToken());
    assertEquals("yad", text.term());
    assertFalse(filter.incrementToken());
  }
  
  public void testFilterWithMark() throws Exception {
    TokenStream stream = new WhitespaceTokenizer(Version.LUCENE_CURRENT, new StringReader(
        "Do have a nice day")); // 1-4 length string
    ReverseStringFilter filter = new ReverseStringFilter(Version.LUCENE_CURRENT, stream, '\u0001');
    TermAttribute text = filter
        .getAttribute(TermAttribute.class);
    assertTrue(filter.incrementToken());
    assertEquals("\u0001oD", text.term());
    assertTrue(filter.incrementToken());
    assertEquals("\u0001evah", text.term());
    assertTrue(filter.incrementToken());
    assertEquals("\u0001a", text.term());
    assertTrue(filter.incrementToken());
    assertEquals("\u0001ecin", text.term());
    assertTrue(filter.incrementToken());
    assertEquals("\u0001yad", text.term());
    assertFalse(filter.incrementToken());
  }

  public void testReverseString() throws Exception {
    assertEquals( "A", ReverseStringFilter.reverse( "A" ) );
    assertEquals( "BA", ReverseStringFilter.reverse( "AB" ) );
    assertEquals( "CBA", ReverseStringFilter.reverse( "ABC" ) );
  }
  
  public void testReverseChar() throws Exception {
    char[] buffer = { 'A', 'B', 'C', 'D', 'E', 'F' };
    ReverseStringFilter.reverse( buffer, 2, 3 );
    assertEquals( "ABEDCF", new String( buffer ) );
  }
  
  /**
   * Test the broken 3.0 behavior, for back compat
   */
  public void testBackCompat() throws Exception {
    assertEquals("\uDF05\uD866\uDF05\uD866", ReverseStringFilter.reverse("𩬅𩬅"));
  }
  
  public void testReverseSupplementary() throws Exception {
    // supplementary at end
    assertEquals("𩬅艱鍟䇹愯瀛", ReverseStringFilter.reverse(Version.LUCENE_CURRENT, "瀛愯䇹鍟艱𩬅"));
    // supplementary at end - 1
    assertEquals("a𩬅艱鍟䇹愯瀛", ReverseStringFilter.reverse(Version.LUCENE_CURRENT, "瀛愯䇹鍟艱𩬅a"));
    // supplementary at start
    assertEquals("fedcba𩬅", ReverseStringFilter.reverse(Version.LUCENE_CURRENT, "𩬅abcdef"));
    // supplementary at start + 1
    assertEquals("fedcba𩬅z", ReverseStringFilter.reverse(Version.LUCENE_CURRENT, "z𩬅abcdef"));
    // supplementary medial
    assertEquals("gfe𩬅dcba", ReverseStringFilter.reverse(Version.LUCENE_CURRENT, "abcd𩬅efg"));
  }

  public void testReverseSupplementaryChar() throws Exception {
    // supplementary at end
    char[] buffer = "abc瀛愯䇹鍟艱𩬅".toCharArray();
    ReverseStringFilter.reverse(Version.LUCENE_CURRENT, buffer, 3, 7);
    assertEquals("abc𩬅艱鍟䇹愯瀛", new String(buffer));
    // supplementary at end - 1
    buffer = "abc瀛愯䇹鍟艱𩬅d".toCharArray();
    ReverseStringFilter.reverse(Version.LUCENE_CURRENT, buffer, 3, 8);
    assertEquals("abcd𩬅艱鍟䇹愯瀛", new String(buffer));
    // supplementary at start
    buffer = "abc𩬅瀛愯䇹鍟艱".toCharArray();
    ReverseStringFilter.reverse(Version.LUCENE_CURRENT, buffer, 3, 7);
    assertEquals("abc艱鍟䇹愯瀛𩬅", new String(buffer));
    // supplementary at start + 1
    buffer = "abcd𩬅瀛愯䇹鍟艱".toCharArray();
    ReverseStringFilter.reverse(Version.LUCENE_CURRENT, buffer, 3, 8);
    assertEquals("abc艱鍟䇹愯瀛𩬅d", new String(buffer));
    // supplementary medial
    buffer = "abc瀛愯𩬅def".toCharArray();
    ReverseStringFilter.reverse(Version.LUCENE_CURRENT, buffer, 3, 7);
    assertEquals("abcfed𩬅愯瀛", new String(buffer));
  }
}
