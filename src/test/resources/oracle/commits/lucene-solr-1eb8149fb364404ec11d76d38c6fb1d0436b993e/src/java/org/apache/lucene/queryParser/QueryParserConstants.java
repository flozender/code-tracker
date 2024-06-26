/* Generated By:JavaCC: Do not edit this line. QueryParserConstants.java */
package org.apache.lucene.queryParser;


/**
 * Token literal values and constants.
 * Generated by org.javacc.parser.OtherFilesGen#start()
 *
 * @deprecated use the equivalent class defined in the new <tt>queryparser</tt> project, 
 * 				currently located in contrib: org.apache.lucene.queryParser.original.parser.TextParserConstants
 * 
 */
public interface QueryParserConstants {

  /** End of File. */
  int EOF = 0;
  /** RegularExpression Id. */
  int _NUM_CHAR = 1;
  /** RegularExpression Id. */
  int _ESCAPED_CHAR = 2;
  /** RegularExpression Id. */
  int _TERM_START_CHAR = 3;
  /** RegularExpression Id. */
  int _TERM_CHAR = 4;
  /** RegularExpression Id. */
  int _WHITESPACE = 5;
  /** RegularExpression Id. */
  int _QUOTED_CHAR = 6;
  /** RegularExpression Id. */
  int AND = 8;
  /** RegularExpression Id. */
  int OR = 9;
  /** RegularExpression Id. */
  int NOT = 10;
  /** RegularExpression Id. */
  int PLUS = 11;
  /** RegularExpression Id. */
  int MINUS = 12;
  /** RegularExpression Id. */
  int LPAREN = 13;
  /** RegularExpression Id. */
  int RPAREN = 14;
  /** RegularExpression Id. */
  int COLON = 15;
  /** RegularExpression Id. */
  int STAR = 16;
  /** RegularExpression Id. */
  int CARAT = 17;
  /** RegularExpression Id. */
  int QUOTED = 18;
  /** RegularExpression Id. */
  int TERM = 19;
  /** RegularExpression Id. */
  int FUZZY_SLOP = 20;
  /** RegularExpression Id. */
  int PREFIXTERM = 21;
  /** RegularExpression Id. */
  int WILDTERM = 22;
  /** RegularExpression Id. */
  int RANGEIN_START = 23;
  /** RegularExpression Id. */
  int RANGEEX_START = 24;
  /** RegularExpression Id. */
  int NUMBER = 25;
  /** RegularExpression Id. */
  int RANGEIN_TO = 26;
  /** RegularExpression Id. */
  int RANGEIN_END = 27;
  /** RegularExpression Id. */
  int RANGEIN_QUOTED = 28;
  /** RegularExpression Id. */
  int RANGEIN_GOOP = 29;
  /** RegularExpression Id. */
  int RANGEEX_TO = 30;
  /** RegularExpression Id. */
  int RANGEEX_END = 31;
  /** RegularExpression Id. */
  int RANGEEX_QUOTED = 32;
  /** RegularExpression Id. */
  int RANGEEX_GOOP = 33;

  /** Lexical state. */
  int Boost = 0;
  /** Lexical state. */
  int RangeEx = 1;
  /** Lexical state. */
  int RangeIn = 2;
  /** Lexical state. */
  int DEFAULT = 3;

  /** Literal token values. */
  String[] tokenImage = {
    "<EOF>",
    "<_NUM_CHAR>",
    "<_ESCAPED_CHAR>",
    "<_TERM_START_CHAR>",
    "<_TERM_CHAR>",
    "<_WHITESPACE>",
    "<_QUOTED_CHAR>",
    "<token of kind 7>",
    "<AND>",
    "<OR>",
    "<NOT>",
    "\"+\"",
    "\"-\"",
    "\"(\"",
    "\")\"",
    "\":\"",
    "\"*\"",
    "\"^\"",
    "<QUOTED>",
    "<TERM>",
    "<FUZZY_SLOP>",
    "<PREFIXTERM>",
    "<WILDTERM>",
    "\"[\"",
    "\"{\"",
    "<NUMBER>",
    "\"TO\"",
    "\"]\"",
    "<RANGEIN_QUOTED>",
    "<RANGEIN_GOOP>",
    "\"TO\"",
    "\"}\"",
    "<RANGEEX_QUOTED>",
    "<RANGEEX_GOOP>",
  };

}
