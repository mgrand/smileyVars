package com.markgrand.smileyVars;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenizerTest {
    @Test
    void empty() {
        final String sql = "";
        noBracketTest(sql);
    }

    @Test
    void justWhiteSpace() {
        final String sql = "  \t\n \r";
        noBracketTest(sql);
    }

    @Test
    void noVars() {
        final String sql = "SELECT * FROM foo";
        noBracketTest(sql);
    }

    private void noBracketTest(String sql) {
        Tokenizer tokenizer = new Tokenizer(sql);
        assertTrue(tokenizer.hasNext());
        Token token = tokenizer.next();
        assertEquals(TokenType.TEXT, token.getTokenType());
        assertEquals(sql, token.getTokenchars());
        assertFalse(tokenizer.hasNext());
    }
}