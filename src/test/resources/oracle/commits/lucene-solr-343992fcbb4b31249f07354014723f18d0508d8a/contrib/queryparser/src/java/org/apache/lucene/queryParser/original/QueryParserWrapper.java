package org.apache.lucene.queryParser.original;

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

import java.text.Collator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.core.QueryNodeException;
import org.apache.lucene.queryParser.core.config.FieldConfig;
import org.apache.lucene.queryParser.core.config.QueryConfigHandler;
import org.apache.lucene.queryParser.core.nodes.QueryNode;
import org.apache.lucene.queryParser.core.parser.SyntaxParser;
import org.apache.lucene.queryParser.core.processors.QueryNodeProcessor;
import org.apache.lucene.queryParser.original.builders.OriginalQueryBuilder;
import org.apache.lucene.queryParser.original.builders.OriginalQueryTreeBuilder;
import org.apache.lucene.queryParser.original.config.AllowLeadingWildcardAttribute;
import org.apache.lucene.queryParser.original.config.AnalyzerAttribute;
import org.apache.lucene.queryParser.original.config.MultiTermRewriteMethodAttribute;
import org.apache.lucene.queryParser.original.config.DateResolutionAttribute;
import org.apache.lucene.queryParser.original.config.DefaultOperatorAttribute;
import org.apache.lucene.queryParser.original.config.DefaultPhraseSlopAttribute;
import org.apache.lucene.queryParser.original.config.LocaleAttribute;
import org.apache.lucene.queryParser.original.config.LowercaseExpandedTermsAttribute;
import org.apache.lucene.queryParser.original.config.OriginalQueryConfigHandler;
import org.apache.lucene.queryParser.original.config.PositionIncrementsAttribute;
import org.apache.lucene.queryParser.original.config.RangeCollatorAttribute;
import org.apache.lucene.queryParser.original.parser.OriginalSyntaxParser;
import org.apache.lucene.queryParser.original.processors.OriginalQueryNodeProcessorPipeline;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Parameter;

/**
 * This class performs the query parsing using the new query parser implementation, but
 * keeps the old {@link QueryParser} API. <br/>
 * <br/>
 * This class should be used when the new query parser features are and
 * the old {@link QueryParser} API are needed at the same time. <br/>
 * 
 * @deprecated this class will be removed soon, it's a temporary class to be
 *             used along the transition from the old query parser to the new
 *             one
 */
public class QueryParserWrapper {

  /**
   * The default operator for parsing queries. Use
   * {@link QueryParserWrapper#setDefaultOperator} to change it.
   */
  static public final class Operator extends Parameter {
    private static final long serialVersionUID = 3550299139196880290L;

    private Operator(String name) {
      super(name);
    }

    static public final Operator OR = new Operator("OR");
    static public final Operator AND = new Operator("AND");
  }

  // the nested class:
  /** Alternative form of QueryParser.Operator.AND */
  public static final Operator AND_OPERATOR = Operator.AND;
  /** Alternative form of QueryParser.Operator.OR */
  public static final Operator OR_OPERATOR = Operator.OR;

  /**
   * Returns a String where those characters that QueryParser expects to be
   * escaped are escaped by a preceding <code>\</code>.
   */
  public static String escape(String s) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      // These characters are part of the query syntax and must be escaped
      if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')'
          || c == ':' || c == '^' || c == '[' || c == ']' || c == '\"'
          || c == '{' || c == '}' || c == '~' || c == '*' || c == '?'
          || c == '|' || c == '&') {
        sb.append('\\');
      }
      sb.append(c);
    }
    return sb.toString();
  }

  private SyntaxParser syntaxParser = new OriginalSyntaxParser();

  private OriginalQueryConfigHandler config;
  private OriginalQueryParserHelper qpHelper;

  private QueryNodeProcessor processorPipeline;

  private OriginalQueryBuilder builder = new OriginalQueryTreeBuilder();

  private String defaultField;

  public QueryParserWrapper(String defaultField, Analyzer analyzer) {
    this.defaultField = defaultField;
    
    this.qpHelper = new OriginalQueryParserHelper();
    
    this.config = (OriginalQueryConfigHandler) qpHelper.getQueryConfigHandler();

    this.qpHelper.setAnalyzer(analyzer);

    this.processorPipeline = new OriginalQueryNodeProcessorPipeline(this.config);

  }

  OriginalQueryParserHelper getQueryParserHelper() {
    return qpHelper;
  }
  
  public String getField() {
    return this.defaultField;
  }

  public Analyzer getAnalyzer() {

    if (this.config != null
        && this.config.hasAttribute(AnalyzerAttribute.class)) {
      return ((AnalyzerAttribute) this.config
          .getAttribute(AnalyzerAttribute.class)).getAnalyzer();
    }

    return null;

  }

  /**
   * Sets the {@link OriginalQueryBuilder} used to generate a {@link Query} object
   * from the parsed and processed query node tree.
   * 
   * @param builder
   *          the builder
   */
  public void setQueryBuilder(OriginalQueryBuilder builder) {
    this.builder = builder;
  }

  /**
   * Sets the {@link QueryNodeProcessor} used to process the query node tree
   * generated by the
   * {@link org.apache.lucene.queryParser.original.parser.OriginalSyntaxParser}.
   * 
   * @param processor
   *          the processor
   */
  public void setQueryProcessor(QueryNodeProcessor processor) {
    this.processorPipeline = processor;
    this.processorPipeline.setQueryConfigHandler(this.config);

  }

  /**
   * Sets the {@link QueryConfigHandler} used by the {@link QueryNodeProcessor}
   * set to this object.
   * 
   * @param queryConfig
   *          the query config handler
   */
  public void setQueryConfig(OriginalQueryConfigHandler queryConfig) {
    this.config = queryConfig;

    if (this.processorPipeline != null) {
      this.processorPipeline.setQueryConfigHandler(this.config);
    }

  }

  /**
   * Returns the query config handler used by this query parser
   * 
   * @return the query config handler
   */
  public QueryConfigHandler getQueryConfigHandler() {
    return this.config;
  }

  /**
   * Returns {@link QueryNodeProcessor} used to process the query node tree
   * generated by the
   * {@link org.apache.lucene.queryParser.original.parser.OriginalSyntaxParser}.
   * 
   * @return the query processor
   */
  public QueryNodeProcessor getQueryProcessor() {
    return this.processorPipeline;
  }

  public ParseException generateParseException() {
    return null;
  }

  public boolean getAllowLeadingWildcard() {

    if (this.config != null
        && this.config.hasAttribute(AllowLeadingWildcardAttribute.class)) {
      return ((AllowLeadingWildcardAttribute) this.config
          .getAttribute(AllowLeadingWildcardAttribute.class))
          .isAllowLeadingWildcard();
    }

    return false;

  }

  public MultiTermQuery.RewriteMethod getMultiTermRewriteMethod() {
    if (this.config != null
        && this.config.hasAttribute(MultiTermRewriteMethodAttribute.class)) {
      return ((MultiTermRewriteMethodAttribute) this.config
          .getAttribute(MultiTermRewriteMethodAttribute.class))
          .getMultiTermRewriteMethod();
    }

    return MultiTermQuery.CONSTANT_SCORE_AUTO_REWRITE_DEFAULT;

  }

  public Resolution getDateResolution(String fieldName) {

    if (this.config != null) {
      FieldConfig fieldConfig = this.config.getFieldConfig(fieldName);

      if (fieldConfig != null) {

        if (this.config.hasAttribute(DateResolutionAttribute.class)) {
          return ((DateResolutionAttribute) this.config
              .getAttribute(DateResolutionAttribute.class)).getDateResolution();
        }

      }

    }

    return null;

  }

  public boolean getEnablePositionIncrements() {

    if (this.config != null
        && this.config.hasAttribute(PositionIncrementsAttribute.class)) {
      return ((PositionIncrementsAttribute) this.config
          .getAttribute(PositionIncrementsAttribute.class))
          .isPositionIncrementsEnabled();
    }

    return false;

  }

  public float getFuzzyMinSim() {
    return FuzzyQuery.defaultMinSimilarity;
  }

  public int getFuzzyPrefixLength() {
    return FuzzyQuery.defaultPrefixLength;
  }

  public Locale getLocale() {

    if (this.config != null && this.config.hasAttribute(LocaleAttribute.class)) {
      return ((LocaleAttribute) this.config.getAttribute(LocaleAttribute.class))
          .getLocale();
    }

    return Locale.getDefault();

  }

  public boolean getLowercaseExpandedTerms() {

    if (this.config != null
        && this.config.hasAttribute(LowercaseExpandedTermsAttribute.class)) {
      return ((LowercaseExpandedTermsAttribute) this.config
          .getAttribute(LowercaseExpandedTermsAttribute.class))
          .isLowercaseExpandedTerms();
    }

    return true;

  }

  public int getPhraseSlop() {

    if (this.config != null
        && this.config.hasAttribute(AllowLeadingWildcardAttribute.class)) {
      return ((DefaultPhraseSlopAttribute) this.config
          .getAttribute(DefaultPhraseSlopAttribute.class))
          .getDefaultPhraseSlop();
    }

    return 0;

  }

  public Collator getRangeCollator() {

    if (this.config != null
        && this.config.hasAttribute(RangeCollatorAttribute.class)) {
      return ((RangeCollatorAttribute) this.config
          .getAttribute(RangeCollatorAttribute.class)).getRangeCollator();
    }

    return null;

  }

  public boolean getUseOldRangeQuery() {
    if (getMultiTermRewriteMethod() == MultiTermQuery.SCORING_BOOLEAN_QUERY_REWRITE) {
      return true;
    } else {
      return false;
    }
  }

  public Query parse(String query) throws ParseException {

    try {
      QueryNode queryTree = this.syntaxParser.parse(query, getField());
      queryTree = this.processorPipeline.process(queryTree);
      return (Query) this.builder.build(queryTree);

    } catch (QueryNodeException e) {
      throw new ParseException("parse exception");
    }

  }

  public void setAllowLeadingWildcard(boolean allowLeadingWildcard) {
    this.qpHelper.setAllowLeadingWildcard(allowLeadingWildcard);
  }

  public void setMultiTermRewriteMethod(MultiTermQuery.RewriteMethod method) {  
    this.qpHelper.setMultiTermRewriteMethod(method);
  }

  public void setDateResolution(Resolution dateResolution) {
    this.qpHelper.setDateResolution(dateResolution);
  }

  private Map<CharSequence, DateTools.Resolution> dateRes =  new HashMap<CharSequence, DateTools.Resolution>();
  
  public void setDateResolution(String fieldName, Resolution dateResolution) {
    dateRes.put(fieldName, dateResolution);
    this.qpHelper.setDateResolution(dateRes);
  }

  public void setDefaultOperator(Operator op) {

    this.qpHelper
        .setDefaultOperator(OR_OPERATOR.equals(op) ? org.apache.lucene.queryParser.original.config.DefaultOperatorAttribute.Operator.OR
            : org.apache.lucene.queryParser.original.config.DefaultOperatorAttribute.Operator.AND);

  }

  public Operator getDefaultOperator() {

    if (this.config != null
        && this.config.hasAttribute(DefaultOperatorAttribute.class)) {

      return (((DefaultOperatorAttribute) this.config
          .getAttribute(DefaultOperatorAttribute.class)).getOperator() == org.apache.lucene.queryParser.original.config.DefaultOperatorAttribute.Operator.AND) ? AND_OPERATOR
          : OR_OPERATOR;

    }

    return OR_OPERATOR;

  }

  public void setEnablePositionIncrements(boolean enable) {
    this.qpHelper.setEnablePositionIncrements(enable);
  }

  public void setFuzzyMinSim(float fuzzyMinSim) {
    // TODO Auto-generated method stub

  }

  public void setFuzzyPrefixLength(int fuzzyPrefixLength) {
    // TODO Auto-generated method stub

  }

  public void setLocale(Locale locale) {
    this.qpHelper.setLocale(locale);
  }

  public void setLowercaseExpandedTerms(boolean lowercaseExpandedTerms) {
    this.qpHelper.setLowercaseExpandedTerms(lowercaseExpandedTerms);
  }

  public void setPhraseSlop(int phraseSlop) {
    this.qpHelper.setDefaultPhraseSlop(phraseSlop);
  }

  public void setRangeCollator(Collator rc) {
    this.qpHelper.setRangeCollator(rc);
  }

  public void setUseOldRangeQuery(boolean useOldRangeQuery) {
    if (useOldRangeQuery) {
      setMultiTermRewriteMethod(MultiTermQuery.SCORING_BOOLEAN_QUERY_REWRITE);
    } else {
      setMultiTermRewriteMethod(MultiTermQuery.CONSTANT_SCORE_AUTO_REWRITE_DEFAULT);
    }
  }

  protected Query getPrefixQuery(String field, String termStr)
      throws ParseException {
    throw new UnsupportedOperationException();
  }

  protected Query getWildcardQuery(String field, String termStr)
      throws ParseException {
    throw new UnsupportedOperationException();
  }

  protected Query getFuzzyQuery(String field, String termStr,
      float minSimilarity) throws ParseException {
    throw new UnsupportedOperationException();
  }

  /**
   * @exception ParseException
   *              throw in overridden method to disallow
   */
  protected Query getFieldQuery(String field, String queryText)
      throws ParseException {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unchecked")
protected Query getBooleanQuery(List clauses, boolean disableCoord)
      throws ParseException {
    throw new UnsupportedOperationException();
  }

  /**
   * Base implementation delegates to {@link #getFieldQuery(String,String)}.
   * This method may be overridden, for example, to return a SpanNearQuery
   * instead of a PhraseQuery.
   * 
   * @exception ParseException
   *              throw in overridden method to disallow
   */
  protected Query getFieldQuery(String field, String queryText, int slop)
      throws ParseException {
    throw new UnsupportedOperationException();
  }

  /**
   * @exception ParseException
   *              throw in overridden method to disallow
   */
  protected Query getRangeQuery(String field, String part1, String part2,
      boolean inclusive) throws ParseException {
    throw new UnsupportedOperationException();
  }

}
