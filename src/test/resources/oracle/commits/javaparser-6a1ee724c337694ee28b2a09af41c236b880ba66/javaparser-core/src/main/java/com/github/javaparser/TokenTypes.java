package com.github.javaparser;

import static com.github.javaparser.GeneratedJavaParserConstants.*;
import static com.github.javaparser.utils.Utils.EOL;

/**
 * Complements GeneratedJavaParserConstants
 */
public class TokenTypes {
    public static boolean isWhitespace(int kind) {
        return getCategory(kind) == JavaToken.Category.WHITESPACE;
    }

    /**
     * @deprecated use isEndOfLineToken
     */
    @Deprecated
    public static boolean isEndOfLineCharacter(int kind) {
        return isEndOfLineToken(kind);
    }

    public static boolean isEndOfLineToken(int kind) {
        switch (kind) {
            case WINDOWS_EOL:
            case OLD_MAC_EOL:
            case UNIX_EOL:
                return true;
            default:
                return false;
        }
    }

    public static boolean isWhitespaceOrComment(int kind) {
        switch (getCategory(kind)) {
            case WHITESPACE:
            case COMMENT:
                return true;
            default:
                return false;
        }
    }

    public static boolean isSpaceOrTab(int kind) {
        switch (kind) {
            case SPACE:
            case TAB:
            case FORM_FEED:
            case NEXT_LINE:
            case NON_BREAKING_SPACE:
            case OGHAM_SPACE:
            case MONGOLIAN_VOWEL_SEPARATOR:
            case EN_QUAD:
            case EM_QUAD:
            case EN_SPACE:
            case EM_SPACE:
            case THREE_PER_EM_SPACE:
            case FOUR_PER_EM_SPACE:
            case SIX_PER_EM_SPACE:
            case FIGURE_SPACE:
            case PUNCTUATION_SPACE:
            case THIN_SPACE:
            case HAIR_SPACE:
            case ZERO_WIDTH_SPACE:
            case ZERO_WIDTH_NON_JOINER:
            case ZERO_WIDTH_JOINER:
            case LINE_SEPARATOR:
            case PARAGRAPH_SEPARATOR:
            case NARROW_NO_BREAK_SPACE:
            case MEDIUM_MATHEMATICAL_SPACE:
            case WORD_JOINER:
            case IDEOGRAPHIC_SPACE:
            case ZERO_WIDTH_NO_BREAK_SPACE:
                return true;
            default:
                return false;
        }
    }

    public static boolean isComment(int kind) {
        return getCategory(kind)== JavaToken.Category.COMMENT;
    }

    /** @deprecated use eolTokenKind */
    public static int eolToken() {
        return eolTokenKind();
    }

    public static int eolTokenKind() {
        if (EOL.equals("\n")) {
            return UNIX_EOL;
        }
        if (EOL.equals("\r\n")) {
            return WINDOWS_EOL;
        }
        if (EOL.equals("\r")) {
            return OLD_MAC_EOL;
        }
        throw new AssertionError("Unknown EOL character sequence");
    }

    public static int spaceTokenKind() {
        return SPACE;
    }

    /** @deprecated use spaceTokenKind */
    public static int spaceToken() {
        return spaceTokenKind();
    }

    /**
     * <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-3.5">Relevant JLS section</a>
     */
    public static JavaToken.Category getCategory(int kind) {
        switch (kind) {
            case EOF:
            case SPACE:
            case WINDOWS_EOL:
            case TAB:
            case UNIX_EOL:
            case OLD_MAC_EOL:
            case FORM_FEED:
            case NEXT_LINE:
            case NON_BREAKING_SPACE:
            case OGHAM_SPACE:
            case MONGOLIAN_VOWEL_SEPARATOR:
            case EN_QUAD:
            case EM_QUAD:
            case EN_SPACE:
            case EM_SPACE:
            case THREE_PER_EM_SPACE:
            case FOUR_PER_EM_SPACE:
            case SIX_PER_EM_SPACE:
            case FIGURE_SPACE:
            case PUNCTUATION_SPACE:
            case THIN_SPACE:
            case HAIR_SPACE:
            case ZERO_WIDTH_SPACE:
            case ZERO_WIDTH_NON_JOINER:
            case ZERO_WIDTH_JOINER:
            case LINE_SEPARATOR:
            case PARAGRAPH_SEPARATOR:
            case NARROW_NO_BREAK_SPACE:
            case MEDIUM_MATHEMATICAL_SPACE:
            case WORD_JOINER:
            case IDEOGRAPHIC_SPACE:
            case ZERO_WIDTH_NO_BREAK_SPACE:
                return JavaToken.Category.WHITESPACE;
            case SINGLE_LINE_COMMENT:
            case JAVA_DOC_COMMENT:
            case MULTI_LINE_COMMENT:
                return JavaToken.Category.COMMENT;
            case ABSTRACT:
            case ASSERT:
            case BOOLEAN:
            case BREAK:
            case BYTE:
            case CASE:
            case CATCH:
            case CHAR:
            case CLASS:
            case CONST:
            case CONTINUE:
            case _DEFAULT:
            case DO:
            case DOUBLE:
            case ELSE:
            case ENUM:
            case EXTENDS:
            case FALSE:
            case FINAL:
            case FINALLY:
            case FLOAT:
            case FOR:
            case GOTO:
            case IF:
            case IMPLEMENTS:
            case IMPORT:
            case INSTANCEOF:
            case INT:
            case INTERFACE:
            case LONG:
            case NATIVE:
            case NEW:
            case NULL:
            case PACKAGE:
            case PRIVATE:
            case PROTECTED:
            case PUBLIC:
            case RETURN:
            case SHORT:
            case STATIC:
            case STRICTFP:
            case SUPER:
            case SWITCH:
            case SYNCHRONIZED:
            case THIS:
            case THROW:
            case THROWS:
            case TRANSIENT:
            case TRUE:
            case TRY:
            case VOID:
            case VOLATILE:
            case WHILE:
            case REQUIRES:
            case TO:
            case WITH:
            case OPEN:
            case OPENS:
            case USES:
            case MODULE:
            case EXPORTS:
            case PROVIDES:
            case TRANSITIVE:
                return JavaToken.Category.KEYWORD;
            case LONG_LITERAL:
            case INTEGER_LITERAL:
            case DECIMAL_LITERAL:
            case HEX_LITERAL:
            case OCTAL_LITERAL:
            case BINARY_LITERAL:
            case FLOATING_POINT_LITERAL:
            case DECIMAL_FLOATING_POINT_LITERAL:
            case DECIMAL_EXPONENT:
            case HEXADECIMAL_FLOATING_POINT_LITERAL:
            case HEXADECIMAL_EXPONENT:
            case CHARACTER_LITERAL:
            case STRING_LITERAL:
                return JavaToken.Category.LITERAL;
            case IDENTIFIER:
            case LETTER:
            case PART_LETTER:
                return JavaToken.Category.IDENTIFIER;
            case LPAREN:
            case RPAREN:
            case LBRACE:
            case RBRACE:
            case LBRACKET:
            case RBRACKET:
            case SEMICOLON:
            case COMMA:
            case DOT:
            case AT:
                return JavaToken.Category.SEPARATOR;
            case ASSIGN:
            case LT:
            case BANG:
            case TILDE:
            case HOOK:
            case COLON:
            case EQ:
            case LE:
            case GE:
            case NE:
            case SC_OR:
            case SC_AND:
            case INCR:
            case DECR:
            case PLUS:
            case MINUS:
            case STAR:
            case SLASH:
            case BIT_AND:
            case BIT_OR:
            case XOR:
            case REM:
            case LSHIFT:
            case PLUSASSIGN:
            case MINUSASSIGN:
            case STARASSIGN:
            case SLASHASSIGN:
            case ANDASSIGN:
            case ORASSIGN:
            case XORASSIGN:
            case REMASSIGN:
            case LSHIFTASSIGN:
            case RSIGNEDSHIFTASSIGN:
            case RUNSIGNEDSHIFTASSIGN:
            case ELLIPSIS:
            case ARROW:
            case DOUBLECOLON:
            case RUNSIGNEDSHIFT:
            case RSIGNEDSHIFT:
            case GT:
                return JavaToken.Category.OPERATOR;
            default:
                throw new AssertionError("Invalid token kind " + kind);
        }
    }
}
