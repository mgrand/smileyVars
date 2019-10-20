package com.markgrand.smileyVars;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class TokenizerTest {
    @Test
    void empty() {
        final String sql = "";
        doTest(sql);
    }
    @Test
    void extra() {
        Tokenizer t = new Tokenizer("a");
        t.next();
        assertThrows(NoSuchElementException.class, t::next);
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
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.VAR, ":abc"), makeToken(TokenType.TEXT, " FROM foo"));
    }

    @Test
    void unbracketedAnsiStringSimple() {
        final String sql = "SELECT 'abc(:' FROM dual";
        doTest(Tokenizer.builder().configureForAnsi().build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedAnsiStringEmbeddedSingleQuote() {
        final String sql = "SELECT 'ab''c(:'  FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedStringEmbeddedSingleQuoteOracleLower() {
        final String sql = "SELECT q'!ab''c(: !' FROM dual";
        doTest(Tokenizer.builder().enableOracleDelimitedString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedStringEmbeddedSingleQuoteOracleUpper() {
        final String sql = "SELECT Q'!ab''c(: !' FROM dual";
        doTest(Tokenizer.builder().enableOracleDelimitedString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedStringEmbeddedSingleQuoteOracleParen() {
        final String sql = "SELECT Q'(ab()''c(: )' FROM dual";
        doTest(Tokenizer.builder().enableOracleDelimitedString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedStringEmbeddedSingleQuoteOracleSquare() {
        final String sql = "SELECT Q'[ab[]''c(: ]' FROM dual";
        doTest(Tokenizer.builder().enableOracleDelimitedString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedStringEmbeddedSingleQuoteOracleAngle() {
        final String sql = "SELECT Q'<ab<>''c(: >' FROM dual";
        doTest(Tokenizer.builder().enableOracleDelimitedString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedStringEmbeddedSingleQuoteOracleCurly() {
        final String sql = "SELECT Q'{ab{}''c(: }' FROM dual";
        doTest(Tokenizer.builder().enableOracleDelimitedString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedPostgresqlStringEmbeddedSingleQuoteEscapeLower() {
        final String sql = "SELECT e'ab\\'c(:'  FROM dual";
        doTest(Tokenizer.builder().enablePostgresqlEscapeString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedPostgresqlStringEmbeddedSingleQuoteEscapeUpper() {
        final String sql = "SELECT E'ab\\'c(:'  FROM dual";
        doTest(Tokenizer.builder().enablePostgresqlEscapeString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedDollarStringEmbeddedSingleQuoteEscapeEmpty() {
        final String sql = "SELECT $$ab'c(:$$  FROM dual";
        doTest(Tokenizer.builder().enablePostgresqlDollarString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedDollarStringEmbeddedSingleQuoteEscapeTag() {
        final String sql = "SELECT $!!$ab'c(:$!!$  FROM dual";
        doTest(Tokenizer.builder().enablePostgresqlDollarString(true).build(sql), makeToken(TokenType.TEXT, sql));
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
    void unbracketedIdentifierSquare() {
        final String sql = "SELECT [abc (:] FROM dual";
        doTest(Tokenizer.builder().enableSquareBracketIdentifierQuoting(true).build(sql),
                makeToken(TokenType.TEXT, sql));
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
        final String sql = "SELECT /* (:\n blah boat :) */ abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedNestedBlockComment() {
        final String sql = "SELECT /* /* blah, */ (:\n blah boat :) */ abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedNestedBlockComment2() {
        final String sql = "SELECT /* /* blah, */ */ (: blah boat :) abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT /* /* blah, */ */ "), makeToken(TokenType.SMILEY_OPEN,"(:"),
                makeToken(TokenType.TEXT, " blah boat "), makeToken(TokenType.SMILEY_CLOSE, ":)"),
                makeToken(TokenType.TEXT, " abc FROM dual"));
    }

    @Test
    void unbracketedUnnestedBlockComment() {
        final String sql = "SELECT /* /* blah, */ (:\n blah boat :) */ abc FROM dual";
        doTest(Tokenizer.builder().enableNestedBlockComment(false).build(sql),
                makeToken(TokenType.TEXT, "SELECT /* /* blah, */ "), makeToken(TokenType.SMILEY_OPEN, "(:"),
                makeToken(TokenType.TEXT, "\n blah boat "), makeToken(TokenType.SMILEY_CLOSE, ":)"),
                makeToken(TokenType.TEXT, " */ abc FROM dual"));
    }

    @Test
    void unbracketedUnterminatedBlockComment() {
        final String sql = "SELECT /* (:\n blah boat :)  abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void bracketedSimple() {
        final String sql = "SELECT (: blah :foo boat :) abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN,"(:"),
                makeToken(TokenType.TEXT, " blah "), makeToken(TokenType.VAR, ":foo"),
                makeToken(TokenType.TEXT, " boat "), makeToken(TokenType.SMILEY_CLOSE, ":)"),
                makeToken(TokenType.TEXT, " abc FROM dual"));
    }

    @Test
    void bracketedLineComment() {
        final String sql = "SELECT (: blah :foo --boat :) abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN,"(:"),
                makeToken(TokenType.TEXT, " blah "), makeToken(TokenType.VAR, ":foo"),
                makeToken(TokenType.TEXT, " --boat :) abc FROM dual"));
    }

    @Test
    void bracketedBlockComment() {
        final String sql = "SELECT (: blah :foo /*boat :) abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN,"(:"),
                makeToken(TokenType.TEXT, " blah "), makeToken(TokenType.VAR, ":foo"),
                makeToken(TokenType.TEXT, " /*boat :) abc FROM dual"));
    }

    @Test
    void bracketedString() {
        final String sql = "SELECT (: blah ':foo boat :) abc' FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN,"(:"),
                makeToken(TokenType.TEXT, " blah ':foo boat :) abc' FROM dual"));
    }

    @Test
    void bracketedLineQuotedId() {
        final String sql = "SELECT (: blah \":foo boat :) abc FROM dual\"";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN,"(:"),
                makeToken(TokenType.TEXT, " blah \":foo boat :) abc FROM dual\""));
    }

    @Test
    void bracketedSquareQuotedId() {
        final String sql = "SELECT (: blah [:foo boat :) abc] FROM dual";
        doTest(Tokenizer.builder().enableSquareBracketIdentifierQuoting(true).build(sql),
                makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN,"(:"),
                makeToken(TokenType.TEXT, " blah [:foo boat :) abc] FROM dual"));
    }

    private void doTest(String sql, Token ... tokens) {
        Tokenizer tokenizer = new Tokenizer(sql);
        doTest(tokenizer, tokens);
    }

    private void doTest(Tokenizer tokenizer, Token... tokens) {
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