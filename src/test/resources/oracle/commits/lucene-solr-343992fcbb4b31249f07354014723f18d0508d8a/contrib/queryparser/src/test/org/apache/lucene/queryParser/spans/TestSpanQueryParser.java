package org.apache.lucene.queryParser.spans;

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

import javax.management.Query;

import junit.framework.TestCase;

import org.apache.lucene.queryParser.core.QueryNodeException;
import org.apache.lucene.queryParser.core.nodes.OrQueryNode;
import org.apache.lucene.queryParser.core.nodes.QueryNode;
import org.apache.lucene.queryParser.core.parser.SyntaxParser;
import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorPipeline;
import org.apache.lucene.queryParser.original.parser.OriginalSyntaxParser;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

/**
 * This test case demonstrates how the new query parser can be used.<br/>
 * <br/>
 * 
 * It tests queries likes "term", "field:term" "term1 term2" "term1 OR term2",
 * which are all already supported by the current syntax parser (
 * {@link OriginalSyntaxParser}).<br/>
 * <br/>
 * 
 * The goals is to create a new query parser that supports only the pair
 * "field:term" or a list of pairs separated or not by an OR operator, and from
 * this query generate {@link SpanQuery} objects instead of the regular
 * {@link Query} objects. Basically, every pair will be converted to a
 * {@link SpanTermQuery} object and if there are more than one pair they will be
 * grouped by an {@link OrQueryNode}.<br/>
 * <br/>
 * 
 * Another functionality that will be added is the ability to convert every
 * field defined in the query to an unique specific field.<br/>
 * <br/>
 * 
 * The query generation is divided in three different steps: parsing (syntax),
 * processing (semantic) and building.<br/>
 * <br/>
 * 
 * The parsing phase, as already mentioned will be performed by the current
 * query parser: {@link OriginalSyntaxParser}.<br/>
 * <br/>
 * 
 * The processing phase will be performed by a processor pipeline which is
 * compound by 2 processors: {@link SpansValidatorQueryNodeProcessor} and
 * {@link UniqueFieldQueryNodeProcessor}.
 * 
 * <pre>
 * 
 *   {@link SpansValidatorQueryNodeProcessor}: as it's going to use the current 
 *   query parser to parse the syntax, it will support more features than we want,
 *   this processor basically validates the query node tree generated by the parser
 *   and just let got through the elements we want, all the other elements as 
 *   wildcards, range queries, etc...if found, an exception is thrown.
 *   
 *   {@link UniqueFieldQueryNodeProcessor}: this processor will take care of reading
 *   what is the &quot;unique field&quot; from the configuration and convert every field defined
 *   in every pair to this &quot;unique field&quot;. For that, a {@link SpansQueryConfigHandler} is
 *   used, which has the {@link UniqueFieldAttribute} defined in it.
 * </pre>
 * 
 * The building phase is performed by the {@link SpansQueryTreeBuilder}, which
 * basically contains a map that defines which builder will be used to generate
 * {@link SpanQuery} objects from {@link QueryNode} objects.<br/>
 * <br/>
 * 
 * @see SpansQueryConfigHandler
 * @see SpansQueryTreeBuilder
 * @see SpansValidatorQueryNodeProcessor
 * @see SpanOrQueryNodeBuilder
 * @see SpanTermQueryNodeBuilder
 * @see OriginalSyntaxParser
 * @see UniqueFieldQueryNodeProcessor
 * @see UniqueFieldAttribute
 */
public class TestSpanQueryParser extends TestCase {

  private QueryNodeProcessorPipeline spanProcessorPipeline;

  private SpansQueryConfigHandler spanQueryConfigHandler;

  private SpansQueryTreeBuilder spansQueryTreeBuilder;

  private SyntaxParser queryParser = new OriginalSyntaxParser();

  public TestSpanQueryParser() {
    // empty constructor
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    this.spanProcessorPipeline = new QueryNodeProcessorPipeline();
    this.spanQueryConfigHandler = new SpansQueryConfigHandler();
    this.spansQueryTreeBuilder = new SpansQueryTreeBuilder();

    // set up the processor pipeline
    this.spanProcessorPipeline
        .setQueryConfigHandler(this.spanQueryConfigHandler);

    this.spanProcessorPipeline
        .addProcessor(new SpansValidatorQueryNodeProcessor());
    this.spanProcessorPipeline
        .addProcessor(new UniqueFieldQueryNodeProcessor());

  }

  public SpanQuery getSpanQuery(CharSequence query) throws QueryNodeException {
    return getSpanQuery("", query);
  }

  public SpanQuery getSpanQuery(CharSequence uniqueField, CharSequence query)
      throws QueryNodeException {
    UniqueFieldAttribute uniqueFieldAtt = (UniqueFieldAttribute) this.spanQueryConfigHandler
        .getAttribute(UniqueFieldAttribute.class);
    uniqueFieldAtt.setUniqueField(uniqueField);

    QueryNode queryTree = this.queryParser.parse(query, "defaultField");
    queryTree = this.spanProcessorPipeline.process(queryTree);

    return this.spansQueryTreeBuilder.build(queryTree);

  }

  public void testTermSpans() throws Exception {
    assertEquals(getSpanQuery("field:term").toString(), "term");
    assertEquals(getSpanQuery("term").toString(), "term");

    assertTrue(getSpanQuery("field:term") instanceof SpanTermQuery);
    assertTrue(getSpanQuery("term") instanceof SpanTermQuery);

  }

  public void testUniqueField() throws Exception {
    assertEquals(getSpanQuery("field", "term").toString(), "field:term");
    assertEquals(getSpanQuery("field", "field:term").toString(), "field:term");
    assertEquals(getSpanQuery("field", "anotherField:term").toString(),
        "field:term");

  }

  public void testOrSpans() throws Exception {
    assertEquals(getSpanQuery("term1 term2").toString(),
        "spanOr([term1, term2])");
    assertEquals(getSpanQuery("term1 OR term2").toString(),
        "spanOr([term1, term2])");

    assertTrue(getSpanQuery("term1 term2") instanceof SpanOrQuery);
    assertTrue(getSpanQuery("term1 term2") instanceof SpanOrQuery);

  }

  public void testQueryValidator() throws QueryNodeException {

    try {
      getSpanQuery("term*");
      fail("QueryNodeException was expected, wildcard queries should not be supported");

    } catch (QueryNodeException ex) {
      // expected exception
    }

    try {
      getSpanQuery("[a TO z]");
      fail("QueryNodeException was expected, range queries should not be supported");

    } catch (QueryNodeException ex) {
      // expected exception
    }

    try {
      getSpanQuery("a~0.5");
      fail("QueryNodeException was expected, boost queries should not be supported");

    } catch (QueryNodeException ex) {
      // expected exception
    }

    try {
      getSpanQuery("a^0.5");
      fail("QueryNodeException was expected, fuzzy queries should not be supported");

    } catch (QueryNodeException ex) {
      // expected exception
    }

    try {
      getSpanQuery("\"a b\"");
      fail("QueryNodeException was expected, quoted queries should not be supported");

    } catch (QueryNodeException ex) {
      // expected exception
    }

    try {
      getSpanQuery("(a b)");
      fail("QueryNodeException was expected, parenthesized queries should not be supported");

    } catch (QueryNodeException ex) {
      // expected exception
    }

    try {
      getSpanQuery("a AND b");
      fail("QueryNodeException was expected, and queries should not be supported");

    } catch (QueryNodeException ex) {
      // expected exception
    }

  }

}
