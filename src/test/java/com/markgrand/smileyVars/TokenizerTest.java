package com.markgrand.smileyVars;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenizerTest {
    @Test
    void empty() {
        final String sql = "";
        doTest(sql);
    }

    @Test
    void justWhiteSpace() {
        final String sql = "  \t\n \r";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void noVars() {
        final String sql = "SELECT * FROM foo";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void openParenNoColon() {
        final String sql = "SELECT count(*) FROM foo";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedClose() {
        final String sql = "SELECT count(*:) FROM foo";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedVar() {
        final String sql = "SELECT :abc FROM foo";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedAnsiStringSimple() {
        final String sql = "SELECT 'abc(:' FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedAnsiStringEmbeddedSingleQuote() {
        final String sql = "SELECT 'ab''c(:'  FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedAnsiStringUnclosed() {
        final String sql = "SELECT 'abc (: FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedIdentifierSimple() {
        final String sql = "SELECT \"abc (:\" FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedIdentifierEmbeddedDoubleQuote() {
        final String sql = "SELECT \"abc\"\" (:\" FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedIdentifierUnclosed() {
        final String sql = "SELECT 'abc (:FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unclosedBracket() {
        final String sql = "SELECT (:abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN, "(:"), makeToken(TokenType.TEXT, "abc FROM dual"));
    }

    @Test
    void unbracketedLineCommentEol() {
        final String sql = "SELECT --(:abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedLineCommentNewLine() {
        final String sql = "SELECT --(:abc FROM\ndual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedBlockComment() {
        final String sql = "SELECT /* (:\n blah blan :) */ abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedNestedBlockComment() {
        final String sql = "SELECT /* /* blah, */ (:\n blah blan :) */ abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedNestedBlockComment2() {
        final String sql = "SELECT /* /* blah, */ */ (: blah blan :) abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT /* /* blah, */ */ "), makeToken(TokenType.SMILEY_OPEN,"(:"),
                makeToken(TokenType.TEXT, " blah blan "), makeToken(TokenType.SMILEY_CLOSE, ":)"),
                makeToken(TokenType.TEXT, " abc FROM dual"));
    }

    @Test
    void unbracketedUnterminatedBlockComment() {
        final String sql = "SELECT /* (:\n blah blan :)  abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void bracketedSimple() {
        final String sql = "SELECT (: blah :foo blan :) abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN,"(:"),
                makeToken(TokenType.TEXT, " blah "), makeToken(TokenType.VAR, ":foo"),
                makeToken(TokenType.TEXT, " blan "), makeToken(TokenType.SMILEY_CLOSE, ":)"),
                makeToken(TokenType.TEXT, " abc FROM dual"));
    }

    @Test
    void bracketedLineComment() {
        final String sql = "SELECT (: blah :foo --blan :) abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN,"(:"),
                makeToken(TokenType.TEXT, " blah "), makeToken(TokenType.VAR, ":foo"),
                makeToken(TokenType.TEXT, " --blan :) abc FROM dual"));
    }

    @Test
    void bracketedString() {
        final String sql = "SELECT (: blah ':foo blan :) abc' FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN,"(:"),
                makeToken(TokenType.TEXT, " blah ':foo blan :) abc' FROM dual"));
    }

    @Test
    void bracketedLineQuotedId() {
        final String sql = "SELECT (: blah \":foo blan :) abc FROM dual\"";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN,"(:"),
                makeToken(TokenType.TEXT, " blah \":foo blan :) abc FROM dual\""));
    }

    private void doTest(String sql, Token ... tokens) {
        Tokenizer tokenizer = new Tokenizer(sql);
        for (Token thisToken : tokens) {
            assertTrue(tokenizer.hasNext());
            Token token = tokenizer.next();
            assertEquals(thisToken.getTokenType(), token.getTokenType());
            assertEquals(thisToken.getTokenchars(), token.getTokenchars());
        }
        assertFalse(tokenizer.hasNext());
    }

    private Token makeToken(TokenType tokenType, String s) {
        return new Token(tokenType, s, 0, s.length());
    }
}