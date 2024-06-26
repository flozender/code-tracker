package com.intellij.ide.highlighter.custom;

import com.intellij.psi.CustomHighlighterTokenType;
import com.intellij.lexer.Lexer;
import com.intellij.psi.CustomHighlighterTokenType;
import com.intellij.psi.tree.IElementType;

/**
 * @author Yura Cangea
 * @version 1.0
 */
public class SyntaxTableLexer implements Lexer {
  private SyntaxTable table;

  private PosBufferTokenizer tokenizer;

  private IElementType tokenType;
  private int tokenStart;
  private int tokenEnd;

  private String lineComment;
  private String startComment;
  private String endComment;

  private char[] buffer;
  private int startOffset;
  private int endOffset;

  private boolean firstCall;

  public SyntaxTableLexer(SyntaxTable table) {
    this.table = table;

    tokenizer = new PosBufferTokenizer();
    tokenizer.setIgnoreCase(table.isIgnoreCase());

    tokenizer.setHexPrefix(table.getHexPrefix());
    tokenizer.setNumPostifxChars(table.getNumPostfixChars());

    tokenizer.ordinaryChar('/');
    tokenizer.ordinaryChar('.');
    tokenizer.quoteChar('"');
    tokenizer.quoteChar('\'');
    tokenizer.wordChars('_', '_');

    lineComment = table.getLineComment();
    startComment = table.getStartComment();
    endComment = table.getEndComment();
  }

  public void start(char[] buffer) {
    start(buffer, 0, buffer.length, (short) 0);
  }

  public void start(char[] buffer, int startOffset, int endOffset) {
    start(buffer, startOffset, endOffset, (short) 0);
  }

  public void start(char[] buffer, int startOffset, int endOffset,
                    int initialState) {
    this.buffer = buffer;
    this.startOffset = startOffset;
    this.endOffset = endOffset;

    tokenType = null;

    tokenizer.start(buffer, startOffset, endOffset);

    firstCall = true;
  }

  private void parseToken() {
    String st = null;
    while (true) {
      int ttype = tokenizer.nextToken();

      switch (ttype) {
        case PosBufferTokenizer.TT_EOF:
          tokenStart = endOffset;
          tokenEnd = endOffset;
          tokenType = null;
          return;

        case PosBufferTokenizer.TT_WHITESPACE:
          tokenType = CustomHighlighterTokenType.WHITESPACE;
          break;

        case PosBufferTokenizer.TT_WORD:
          st = tokenizer.sval;
          if (table.getKeywords1().contains(st)) {
            tokenType = CustomHighlighterTokenType.KEYWORD_1;
          } else if (table.getKeywords2().contains(st)) {
            tokenType = CustomHighlighterTokenType.KEYWORD_2;
          } else if (table.getKeywords3().contains(st)) {
            tokenType = CustomHighlighterTokenType.KEYWORD_3;
          } else if (table.getKeywords4().contains(st)) {
            tokenType = CustomHighlighterTokenType.KEYWORD_4;
          } else {
            tokenType = CustomHighlighterTokenType.IDENTIFIER;
          }
          break;

        case PosBufferTokenizer.TT_NUMBER:
          tokenType = CustomHighlighterTokenType.NUMBER;
          break;

        case PosBufferTokenizer.TT_QUOTE:
          tokenType = CustomHighlighterTokenType.STRING;
          break;

        default:
          // Line comment
          if (lineComment != null && !"".equals(lineComment.trim())) {
            if (ttype == lineComment.charAt(0) && tokenizer.matchString(lineComment, 1)) {
              tokenType = CustomHighlighterTokenType.LINE_COMMENT;
              tokenStart = tokenizer.getPos() - lineComment.length();
              tokenizer.skipToEol();
              tokenEnd = tokenizer.getPos();
              return;
            }
          }

          // Block comment
          if (startComment != null && !"".equals(startComment.trim())) {
            if (ttype == startComment.charAt(0) && tokenizer.matchString(startComment, 1)) {
              tokenType = CustomHighlighterTokenType.MULTI_LINE_COMMENT;
              tokenStart = tokenizer.getPos() - startComment.length();
              tokenizer.skipToStr(endComment);
              tokenEnd = tokenizer.getPos();
              return;
            }
          }
          tokenType = CustomHighlighterTokenType.CHARACTER;
          break;
      }
      tokenStart = tokenizer.startOffset;
      tokenEnd = tokenizer.endOffset;
      return;
    }
  }


  public IElementType getTokenType() {
    if (firstCall) {
      advance();
    }
    return tokenType;
  }

  public int getTokenStart() {
    return tokenStart;
  }

  public int getTokenEnd() {
    return tokenEnd;
  }

  public void advance() {
    if (firstCall) {
      firstCall = false;
    }
    parseToken();
  }

  // Number of characters to shift back from the change start to reparse.
  public int getSmartUpdateShift() {
    return 0;
  }

  public char[] getBuffer() {
    return buffer;
  }

  public int getBufferEnd() {
    return endOffset;
  }

  public int getState() {
    // No state.
    return 0;
  }

  public int getLastState() {
    return 0;
  }

  public Object clone() {
    SyntaxTableLexer lexer = new SyntaxTableLexer(table);
    lexer.start(buffer, startOffset, endOffset);

    return lexer;
  }
}
