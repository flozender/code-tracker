package org.apache.lucene.analysis.path;

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

import java.io.Reader;
import java.io.StringReader;
import java.util.Random;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.CharStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;

public class TestPathHierarchyTokenizer extends BaseTokenStreamTestCase {

  public void testBasic() throws Exception {
    String path = "/a/b/c";
    PathHierarchyTokenizer t = new PathHierarchyTokenizer( new StringReader(path) );
    assertTokenStreamContents(t,
        new String[]{"/a", "/a/b", "/a/b/c"},
        new int[]{0, 0, 0},
        new int[]{2, 4, 6},
        new int[]{1, 0, 0},
        path.length());
  }

  public void testEndOfDelimiter() throws Exception {
    String path = "/a/b/c/";
    PathHierarchyTokenizer t = new PathHierarchyTokenizer( new StringReader(path) );
    assertTokenStreamContents(t,
        new String[]{"/a", "/a/b", "/a/b/c", "/a/b/c/"},
        new int[]{0, 0, 0, 0},
        new int[]{2, 4, 6, 7},
        new int[]{1, 0, 0, 0},
        path.length());
  }

  public void testStartOfChar() throws Exception {
    String path = "a/b/c";
    PathHierarchyTokenizer t = new PathHierarchyTokenizer( new StringReader(path) );
    assertTokenStreamContents(t,
        new String[]{"a", "a/b", "a/b/c"},
        new int[]{0, 0, 0},
        new int[]{1, 3, 5},
        new int[]{1, 0, 0},
        path.length());
  }

  public void testStartOfCharEndOfDelimiter() throws Exception {
    String path = "a/b/c/";
    PathHierarchyTokenizer t = new PathHierarchyTokenizer( new StringReader(path) );
    assertTokenStreamContents(t,
        new String[]{"a", "a/b", "a/b/c", "a/b/c/"},
        new int[]{0, 0, 0, 0},
        new int[]{1, 3, 5, 6},
        new int[]{1, 0, 0, 0},
        path.length());
  }

  public void testOnlyDelimiter() throws Exception {
    String path = "/";
    PathHierarchyTokenizer t = new PathHierarchyTokenizer( new StringReader(path) );
    assertTokenStreamContents(t,
        new String[]{"/"},
        new int[]{0},
        new int[]{1},
        new int[]{1},
        path.length());
  }

  public void testOnlyDelimiters() throws Exception {
    String path = "//";
    PathHierarchyTokenizer t = new PathHierarchyTokenizer( new StringReader(path) );
    assertTokenStreamContents(t,
        new String[]{"/", "//"},
        new int[]{0, 0},
        new int[]{1, 2},
        new int[]{1, 0},
        path.length());
  }

  public void testReplace() throws Exception {
    String path = "/a/b/c";
    PathHierarchyTokenizer t = new PathHierarchyTokenizer( new StringReader(path), '/', '\\' );
    assertTokenStreamContents(t,
        new String[]{"\\a", "\\a\\b", "\\a\\b\\c"},
        new int[]{0, 0, 0},
        new int[]{2, 4, 6},
        new int[]{1, 0, 0},
        path.length());
  }

  public void testWindowsPath() throws Exception {
    String path = "c:\\a\\b\\c";
    PathHierarchyTokenizer t = new PathHierarchyTokenizer( new StringReader(path), '\\', '\\' );
    assertTokenStreamContents(t,
        new String[]{"c:", "c:\\a", "c:\\a\\b", "c:\\a\\b\\c"},
        new int[]{0, 0, 0, 0},
        new int[]{2, 4, 6, 8},
        new int[]{1, 0, 0, 0},
        path.length());
  }

  public void testNormalizeWinDelimToLinuxDelim() throws Exception {
    NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
    builder.add("\\", "/");
    NormalizeCharMap normMap = builder.build();
    String path = "c:\\a\\b\\c";
    CharStream cs = new MappingCharFilter(normMap, new StringReader(path));
    PathHierarchyTokenizer t = new PathHierarchyTokenizer( cs );
    assertTokenStreamContents(t,
        new String[]{"c:", "c:/a", "c:/a/b", "c:/a/b/c"},
        new int[]{0, 0, 0, 0},
        new int[]{2, 4, 6, 8},
        new int[]{1, 0, 0, 0},
        path.length());
  }

  public void testBasicSkip() throws Exception {
    String path = "/a/b/c";
    PathHierarchyTokenizer t = new PathHierarchyTokenizer( new StringReader(path), 1 );
    assertTokenStreamContents(t,
        new String[]{"/b", "/b/c"},
        new int[]{2, 2},
        new int[]{4, 6},
        new int[]{1, 0},
        path.length());
  }

  public void testEndOfDelimiterSkip() throws Exception {
    String path = "/a/b/c/";
    PathHierarchyTokenizer t = new PathHierarchyTokenizer( new StringReader(path), 1 );
    assertTokenStreamContents(t,
        new String[]{"/b", "/b/c", "/b/c/"},
        new int[]{2, 2, 2},
        new int[]{4, 6, 7},
        new int[]{1, 0, 0},
        path.length());
  }

  public void testStartOfCharSkip() throws Exception {
    String path = "a/b/c";
    PathHierarchyTokenizer t = new PathHierarchyTokenizer( new StringReader(path), 1 );
    assertTokenStreamContents(t,
        new String[]{"/b", "/b/c"},
        new int[]{1, 1},
        new int[]{3, 5},
        new int[]{1, 0},
        path.length());
  }

  public void testStartOfCharEndOfDelimiterSkip() throws Exception {
    String path = "a/b/c/";
    PathHierarchyTokenizer t = new PathHierarchyTokenizer( new StringReader(path), 1 );
    assertTokenStreamContents(t,
        new String[]{"/b", "/b/c", "/b/c/"},
        new int[]{1, 1, 1},
        new int[]{3, 5, 6},
        new int[]{1, 0, 0},
        path.length());
  }

  public void testOnlyDelimiterSkip() throws Exception {
    String path = "/";
    PathHierarchyTokenizer t = new PathHierarchyTokenizer( new StringReader(path), 1 );
    assertTokenStreamContents(t,
        new String[]{},
        new int[]{},
        new int[]{},
        new int[]{},
        path.length());
  }

  public void testOnlyDelimitersSkip() throws Exception {
    String path = "//";
    PathHierarchyTokenizer t = new PathHierarchyTokenizer( new StringReader(path), 1 );
    assertTokenStreamContents(t,
        new String[]{"/"},
        new int[]{1},
        new int[]{2},
        new int[]{1},
        path.length());
  }
  
  /** blast some random strings through the analyzer */
  public void testRandomStrings() throws Exception {
    Analyzer a = new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        Tokenizer tokenizer = new PathHierarchyTokenizer(reader);
        return new TokenStreamComponents(tokenizer, tokenizer);
      }    
    };
    checkRandomData(random(), a, 1000*RANDOM_MULTIPLIER);
  }
  
  /** blast some random large strings through the analyzer */
  public void testRandomHugeStrings() throws Exception {
    Random random = random();
    Analyzer a = new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        Tokenizer tokenizer = new PathHierarchyTokenizer(reader);
        return new TokenStreamComponents(tokenizer, tokenizer);
      }    
    };
    checkRandomData(random, a, 100*RANDOM_MULTIPLIER, 1027);
  }
}
