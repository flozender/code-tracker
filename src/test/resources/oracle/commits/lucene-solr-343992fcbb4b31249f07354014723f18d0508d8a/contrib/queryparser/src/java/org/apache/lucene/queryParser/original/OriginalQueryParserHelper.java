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
import java.util.Locale;
import java.util.Map;
import java.util.TooManyListenersException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.queryParser.core.QueryNodeException;
import org.apache.lucene.queryParser.core.QueryParserHelper;
import org.apache.lucene.queryParser.core.config.QueryConfigHandler;
import org.apache.lucene.queryParser.original.builders.OriginalQueryTreeBuilder;
import org.apache.lucene.queryParser.original.config.AllowLeadingWildcardAttribute;
import org.apache.lucene.queryParser.original.config.AnalyzerAttribute;
import org.apache.lucene.queryParser.original.config.DateResolutionAttribute;
import org.apache.lucene.queryParser.original.config.FieldDateResolutionMapAttribute;
import org.apache.lucene.queryParser.original.config.DefaultOperatorAttribute;
import org.apache.lucene.queryParser.original.config.DefaultPhraseSlopAttribute;
import org.apache.lucene.queryParser.original.config.FieldBoostMapAttribute;
import org.apache.lucene.queryParser.original.config.FuzzyAttribute;
import org.apache.lucene.queryParser.original.config.LocaleAttribute;
import org.apache.lucene.queryParser.original.config.LowercaseExpandedTermsAttribute;
import org.apache.lucene.queryParser.original.config.MultiFieldAttribute;
import org.apache.lucene.queryParser.original.config.MultiTermRewriteMethodAttribute;
import org.apache.lucene.queryParser.original.config.OriginalQueryConfigHandler;
import org.apache.lucene.queryParser.original.config.PositionIncrementsAttribute;
import org.apache.lucene.queryParser.original.config.RangeCollatorAttribute;
import org.apache.lucene.queryParser.original.config.DefaultOperatorAttribute.Operator;
import org.apache.lucene.queryParser.original.nodes.RangeQueryNode;
import org.apache.lucene.queryParser.original.parser.OriginalSyntaxParser;
import org.apache.lucene.queryParser.original.processors.OriginalQueryNodeProcessorPipeline;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;

/**
 * <p>
 * This class is a helper that enables users to easily use the Lucene query
 * parser.
 * </p>
 * <p>
 * To construct a Query object from a query string, use the
 * {@link #parse(String, String)} method:
 * <ul>
 * OriginalQueryParserHelper queryParserHelper = new OriginalQueryParserHelper(); <br/>
 * Query query = queryParserHelper.parse("a AND b", "defaultField");
 * </ul>
 * <p>
 * To change any configuration before parsing the query string do, for example:
 * <p/>
 * <ul>
 * // the query config handler returned by {@link OriginalQueryParserHelper} is a
 * {@link OriginalQueryConfigHandler} <br/>
 * queryParserHelper.getQueryConfigHandler().setAnalyzer(new
 * WhitespaceAnalyzer());
 * </ul>
 * <p>
 * The syntax for query strings is as follows (copied from the old QueryParser
 * javadoc):
 * <ul>
 * A Query is a series of clauses. A clause may be prefixed by:
 * <ul>
 * <li>a plus (<code>+</code>) or a minus (<code>-</code>) sign, indicating that
 * the clause is required or prohibited respectively; or
 * <li>a term followed by a colon, indicating the field to be searched. This
 * enables one to construct queries which search multiple fields.
 * </ul>
 * 
 * A clause may be either:
 * <ul>
 * <li>a term, indicating all the documents that contain this term; or
 * <li>a nested query, enclosed in parentheses. Note that this may be used with
 * a <code>+</code>/<code>-</code> prefix to require any of a set of terms.
 * </ul>
 * 
 * Thus, in BNF, the query grammar is:
 * 
 * <pre>
 *   Query  ::= ( Clause )*
 *   Clause ::= [&quot;+&quot;, &quot;-&quot;] [&lt;TERM&gt; &quot;:&quot;] ( &lt;TERM&gt; | &quot;(&quot; Query &quot;)&quot; )
 * </pre>
 * 
 * <p>
 * Examples of appropriately formatted queries can be found in the <a
 * href="../../../../../../queryparsersyntax.html">query syntax
 * documentation</a>.
 * </p>
 * </ul>
 * <p>
 * The text parser used by this helper is a {@link OriginalSyntaxParser}.
 * <p/>
 * <p>
 * The query node processor used by this helper is a
 * {@link OriginalQueryNodeProcessorPipeline}.
 * <p/>
 * <p>
 * The builder used by this helper is a {@link OriginalQueryTreeBuilder}.
 * <p/>
 * 
 * @see OriginalQueryParserHelper
 * @see OriginalQueryConfigHandler
 * @see OriginalSyntaxParser
 * @see OriginalQueryNodeProcessorPipeline
 * @see OriginalQueryTreeBuilder
 */
public class OriginalQueryParserHelper extends QueryParserHelper {

  /**
   * Constructs a {@link OriginalQueryParserHelper} object.
   */
  public OriginalQueryParserHelper() {
    super(new OriginalQueryConfigHandler(), new OriginalSyntaxParser(),
        new OriginalQueryNodeProcessorPipeline(null),
        new OriginalQueryTreeBuilder());
  }

  /**
   * Constructs a {@link OriginalQueryParserHelper} object and sets an
   * {@link Analyzer} to it. The same as:
   * 
   * <ul>
   * OriginalQueryParserHelper qp = new OriginalQueryParserHelper();
   * qp.getQueryConfigHandler().setAnalyzer(analyzer);
   * </ul>
   * 
   * @param analyzer
   *          the analyzer to be used by this query parser helper
   */
  public OriginalQueryParserHelper(Analyzer analyzer) {
    this();

    this.setAnalyzer(analyzer);
  }
  
  public String toString(){
    return "<OriginalQueryParserHelper config=\"" + this.getQueryConfigHandler() + "\"/>";
  }

  /**
   * Overrides {@link QueryParserHelper#parse(String, String)} so it casts the
   * return object to {@link Query}. For more reference about this method, check
   * {@link QueryParserHelper#parse(String, String)}.
   * 
   * @param query
   *          the query string
   * @param defaultField
   *          the default field used by the text parser
   * 
   * @return the object built from the query
   * 
   * @throws QueryNodeException
   *           if something wrong happens along the three phases
   */
  @Override
  public Query parse(String query, String defaultField)
      throws QueryNodeException {

    return (Query) super.parse(query, defaultField);

  }

  /**
   * Gets implicit operator setting, which will be either {@link Operator#AND}
   * or {@link Operator#OR}.
   */
  public Operator getDefaultOperator() {
    DefaultOperatorAttribute attr = (DefaultOperatorAttribute) getQueryConfigHandler().getAttribute(DefaultOperatorAttribute.class);
    return attr.getOperator();
  }

  /**
   * Sets the collator used to determine index term inclusion in ranges for
   * RangeQuerys.
   * <p/>
   * <strong>WARNING:</strong> Setting the rangeCollator to a non-null collator
   * using this method will cause every single index Term in the Field
   * referenced by lowerTerm and/or upperTerm to be examined. Depending on the
   * number of index Terms in this Field, the operation could be very slow.
   * 
   * @param collator
   *          the collator to use when constructing {@link RangeQueryNode}s
   */
  public void setRangeCollator(Collator collator) {
    RangeCollatorAttribute attr = (RangeCollatorAttribute) getQueryConfigHandler().getAttribute(RangeCollatorAttribute.class);
    attr.setDateResolution(collator);
  }

  /**
   * @return the collator used to determine index term inclusion in ranges for
   *         RangeQuerys.
   */
  public Collator getRangeCollator() {
    RangeCollatorAttribute attr = (RangeCollatorAttribute) getQueryConfigHandler().getAttribute(RangeCollatorAttribute.class);
    return attr.getRangeCollator();
  }

  /**
   * Sets the boolean operator of the QueryParser. In default mode (
   * {@link Operator#OR}) terms without any modifiers are considered optional:
   * for example <code>capital of Hungary</code> is equal to
   * <code>capital OR of OR Hungary</code>.<br/>
   * In {@link Operator#AND} mode terms are considered to be in conjunction: the
   * above mentioned query is parsed as <code>capital AND of AND Hungary</code>
   */
  public void setDefaultOperator(Operator operator) {
    DefaultOperatorAttribute attr = (DefaultOperatorAttribute) getQueryConfigHandler().getAttribute(DefaultOperatorAttribute.class);
    attr.setOperator(operator);
  }

  /**
   * Set to <code>true</code> to allow leading wildcard characters.
   * <p>
   * When set, <code>*</code> or <code>?</code> are allowed as the first
   * character of a PrefixQuery and WildcardQuery. Note that this can produce
   * very slow queries on big indexes.
   * <p>
   * Default: false.
   */
  public void setLowercaseExpandedTerms(boolean lowercaseExpandedTerms) {
    LowercaseExpandedTermsAttribute attr= (LowercaseExpandedTermsAttribute) getQueryConfigHandler().getAttribute(LowercaseExpandedTermsAttribute.class);
    attr.setLowercaseExpandedTerms(lowercaseExpandedTerms);
  }

  /**
   * @see #setLowercaseExpandedTerms(boolean)
   */
  public boolean getLowercaseExpandedTerms() {
    LowercaseExpandedTermsAttribute attr = (LowercaseExpandedTermsAttribute) getQueryConfigHandler().getAttribute(LowercaseExpandedTermsAttribute.class);
    return attr.isLowercaseExpandedTerms();
  }

  /**
   * Set to <code>true</code> to allow leading wildcard characters.
   * <p>
   * When set, <code>*</code> or <code>?</code> are allowed as the first
   * character of a PrefixQuery and WildcardQuery. Note that this can produce
   * very slow queries on big indexes.
   * <p>
   * Default: false.
   */
  public void setAllowLeadingWildcard(boolean allowLeadingWildcard) {
    AllowLeadingWildcardAttribute attr = (AllowLeadingWildcardAttribute) getQueryConfigHandler().getAttribute(AllowLeadingWildcardAttribute.class);
    attr.setAllowLeadingWildcard(allowLeadingWildcard);
  }

  /**
   * Set to <code>true</code> to enable position increments in result query.
   * <p>
   * When set, result phrase and multi-phrase queries will be aware of position
   * increments. Useful when e.g. a StopFilter increases the position increment
   * of the token that follows an omitted token.
   * <p>
   * Default: false.
   */
  public void setEnablePositionIncrements(boolean enabled) {
    PositionIncrementsAttribute attr = (PositionIncrementsAttribute) getQueryConfigHandler().getAttribute(PositionIncrementsAttribute.class);
    attr.setPositionIncrementsEnabled(enabled);
  }

  /**
   * @see #setEnablePositionIncrements(boolean)
   */
  public boolean getEnablePositionIncrements() {
    PositionIncrementsAttribute attr = (PositionIncrementsAttribute) getQueryConfigHandler().getAttribute(PositionIncrementsAttribute.class);
    return attr.isPositionIncrementsEnabled();
  }

  /**
   * By default, it uses
   * {@link MultiTermQuery#CONSTANT_SCORE_AUTO_REWRITE_DEFAULT} when creating a
   * prefix, wildcard and range queries. This implementation is generally
   * preferable because it a) Runs faster b) Does not have the scarcity of terms
   * unduly influence score c) avoids any {@link TooManyListenersException}
   * exception. However, if your application really needs to use the
   * old-fashioned boolean queries expansion rewriting and the above points are
   * not relevant then use this change the rewrite method.
   */
  public void setMultiTermRewriteMethod(MultiTermQuery.RewriteMethod method) {
    MultiTermRewriteMethodAttribute attr = (MultiTermRewriteMethodAttribute) getQueryConfigHandler().getAttribute(MultiTermRewriteMethodAttribute.class);
    attr.setMultiTermRewriteMethod(method);
  }

  /**
   * @see #setMultiTermRewriteMethod(org.apache.lucene.search.MultiTermQuery.RewriteMethod)
   */
  public MultiTermQuery.RewriteMethod getMultiTermRewriteMethod() {
    MultiTermRewriteMethodAttribute attr =(MultiTermRewriteMethodAttribute) getQueryConfigHandler().getAttribute(MultiTermRewriteMethodAttribute.class);    
    return attr.getMultiTermRewriteMethod();
  }

  public void setMultiFields(CharSequence[] fields) {

    if (fields == null) {
      fields = new CharSequence[0];
    }

    MultiFieldAttribute attr = (MultiFieldAttribute) getQueryConfigHandler().addAttribute(MultiFieldAttribute.class);
    attr.setFields(fields);

  }

  /**
   * Set the prefix length for fuzzy queries. Default is 0.
   * 
   * @param fuzzyPrefixLength
   *          The fuzzyPrefixLength to set.
   */
  public void setFuzzyPrefixLength(int fuzzyPrefixLength) {
    FuzzyAttribute attr = (FuzzyAttribute) getQueryConfigHandler().addAttribute(FuzzyAttribute.class);
    attr.setPrefixLength(fuzzyPrefixLength);
  }

  /**
   * Set locale used by date range parsing.
   */
  public void setLocale(Locale locale) {
    LocaleAttribute attr = (LocaleAttribute) getQueryConfigHandler().addAttribute(LocaleAttribute.class);
    attr.setLocale(locale);
  }

  /**
   * Returns current locale, allowing access by subclasses.
   */
  public Locale getLocale() {
    LocaleAttribute attr = (LocaleAttribute) getQueryConfigHandler().addAttribute(LocaleAttribute.class);
    return attr.getLocale();
  }

  /**
   * Sets the default slop for phrases. If zero, then exact phrase matches are
   * required. Default value is zero.
   */
  public void setDefaultPhraseSlop(int defaultPhraseSlop) {
    DefaultPhraseSlopAttribute attr = (DefaultPhraseSlopAttribute) getQueryConfigHandler().addAttribute(DefaultPhraseSlopAttribute.class);
    attr.setDefaultPhraseSlop(defaultPhraseSlop);
  }

  public void setAnalyzer(Analyzer analyzer) {
    AnalyzerAttribute attr= (AnalyzerAttribute) getQueryConfigHandler().getAttribute(AnalyzerAttribute.class);
    attr.setAnalyzer(analyzer);
  }
  
  public Analyzer getAnalyzer() {    
    QueryConfigHandler config = this.getQueryConfigHandler();

    if ( config.hasAttribute(AnalyzerAttribute.class)) {
      AnalyzerAttribute attr= (AnalyzerAttribute) config.getAttribute(AnalyzerAttribute.class);
      return attr.getAnalyzer();
    }

    return null;       
  }

  /**
   * @see #setAllowLeadingWildcard(boolean)
   */
  public boolean getAllowLeadingWildcard() {
    AllowLeadingWildcardAttribute attr = (AllowLeadingWildcardAttribute) getQueryConfigHandler().addAttribute(AllowLeadingWildcardAttribute.class);
    return attr.isAllowLeadingWildcard();
  }

  /**
   * Get the minimal similarity for fuzzy queries.
   */
  public float getFuzzyMinSim() {
    FuzzyAttribute attr = (FuzzyAttribute) getQueryConfigHandler().addAttribute(FuzzyAttribute.class);
    return attr.getFuzzyMinSimilarity();
  }

  /**
   * Get the prefix length for fuzzy queries.
   * 
   * @return Returns the fuzzyPrefixLength.
   */
  public int getFuzzyPrefixLength() {
    FuzzyAttribute attr = (FuzzyAttribute) getQueryConfigHandler().addAttribute(FuzzyAttribute.class);
    return attr.getPrefixLength();
  }

  /**
   * Gets the default slop for phrases.
   */
  public int getPhraseSlop() {
    DefaultPhraseSlopAttribute attr = (DefaultPhraseSlopAttribute) getQueryConfigHandler().addAttribute(DefaultPhraseSlopAttribute.class);
    return attr.getDefaultPhraseSlop();
  }

  /**
   * Set the minimum similarity for fuzzy queries. Default is defined on
   * {@link FuzzyQuery#defaultMinSimilarity}.
   */
  public void setFuzzyMinSim(float fuzzyMinSim) {
    FuzzyAttribute attr = (FuzzyAttribute) getQueryConfigHandler().addAttribute(FuzzyAttribute.class);
    attr.setFuzzyMinSimilarity(fuzzyMinSim);
  }
  
  public void setFieldsBoost(Map<CharSequence, Float> boosts) {
    FieldBoostMapAttribute attr = (FieldBoostMapAttribute) getQueryConfigHandler().addAttribute(FieldBoostMapAttribute.class);
    attr.setFieldBoostMap(boosts);
  }

  public void setDateResolution(DateTools.Resolution dateResolution) {
    DateResolutionAttribute attr = (DateResolutionAttribute) getQueryConfigHandler().addAttribute(DateResolutionAttribute.class);
    attr.setDateResolution(dateResolution);
  }

  public void setDateResolution(Map<CharSequence, DateTools.Resolution> dateRes) {
    FieldDateResolutionMapAttribute attr = (FieldDateResolutionMapAttribute) getQueryConfigHandler().addAttribute(FieldDateResolutionMapAttribute.class);
    attr.setFieldDateResolutionMap(dateRes);
  }
  
}
