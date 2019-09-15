package com.markgrand.smileyVars;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenizerTest {
    @Test
    void empty() {
        final String sql = "";
        Tokenizer tokenizer = new Tokenizer(sql);
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
    void operParenNoColon() {
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
        final String sql = "SELECT 'abc' FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedAnsiStringUnclosed() {
        final String sql = "SELECT 'abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedIdentifierSimple() {
        final String sql = "SELECT 'abc' FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedIdentifierUnclosed() {
        final String sql = "SELECT 'abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unclosedBracket() {
        final String sql = "SELECT (:abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN, "(:"), makeToken(TokenType.TEXT, "abc FROM dual"));
    }

    @Test
    void unbracketedLineComment() {
        final String sql = "SELECT --(:abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedBlockComment() {
        final String sql = "SELECT /* (:\n blah blan :) */ abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    private void doTest(String sql, Token ... tokens) {
        Tokenizer tokenizer = new Tokenizer(sql);
        for (int i=0; i < tokens.length; i++) {
            assertTrue(tokenizer.hasNext());
            Token token = tokenizer.next();
            assertEquals(tokens[i].getTokenType(), token.getTokenType());
            assertEquals(tokens[i].getTokenchars(), token.getTokenchars());
        }
        assertFalse(tokenizer.hasNext());
    }

    private Token makeToken(TokenType tokenType, String s) {
        return new Token(TokenType.TEXT, s, 0, s.length());
    }
}