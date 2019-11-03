package com.markgrand.smileyVars;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class TokenizerTest {
    @Test
    void empty() {
        @NotNull final String sql = "";
        doTest(sql);
    }

    @Test
    void extra() {
        @NotNull Tokenizer t = new Tokenizer("a");
        t.next();
        assertThrows(NoSuchElementException.class, t::next);
    }

    @Test
    void justWhiteSpace() {
        @NotNull final String sql = "  \t\n \r";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void noVars() {
        @NotNull final String sql = "SELECT * FROM foo";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void openParenNoColon() {
        @NotNull final String sql = "SELECT count(*) FROM foo";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedClose() {
        @NotNull final String sql = "SELECT count(*:) FROM foo";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedVar() {
        @NotNull final String sql = "SELECT :abc FROM foo";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.VAR, ":abc"), makeToken(TokenType.TEXT, " FROM foo"));
    }

    @Test
    void unbracketedAnsiStringSimple() {
        @NotNull final String sql = "SELECT 'abc(:' FROM dual";
        doTest(Tokenizer.builder().configureForAnsi().build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedAnsiStringEmbeddedSingleQuote() {
        @NotNull final String sql = "SELECT 'ab''c(:'  FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedStringEmbeddedSingleQuoteOracleLower() {
        @NotNull final String sql = "SELECT q'!ab''c(: !' FROM dual";
        doTest(Tokenizer.builder().enableOracleDelimitedString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedStringEmbeddedSingleQuoteOracleUpper() {
        @NotNull final String sql = "SELECT Q'!ab''c(: !' FROM dual";
        doTest(Tokenizer.builder().enableOracleDelimitedString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedStringEmbeddedSingleQuoteOracleParen() {
        @NotNull final String sql = "SELECT Q'(ab()''c(: )' FROM dual";
        doTest(Tokenizer.builder().enableOracleDelimitedString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedStringEmbeddedSingleQuoteOracleSquare() {
        @NotNull final String sql = "SELECT q'[ab[]''c(: ]' FROM dual";
        doTest(Tokenizer.builder().enableOracleDelimitedString(true).build(sql),
                makeToken(TokenType.TEXT, "SELECT q'[ab[]''c(: ]' FROM dual"));
    }

    @Test
    void unbracketedStringEmbeddedSingleQuoteOracleAngle() {
        @NotNull final String sql = "SELECT Q'<ab<>''c(: >' FROM dual";
        doTest(Tokenizer.builder().enableOracleDelimitedString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedStringEmbeddedSingleQuoteOracleCurly() {
        @NotNull final String sql = "SELECT Q'{ab{}''c(: }' FROM dual";
        doTest(Tokenizer.builder().enableOracleDelimitedString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedPostgresqlStringEmbeddedSingleQuoteEscapeLower() {
        @NotNull final String sql = "SELECT e'ab\\'c(:'  FROM dual";
        doTest(Tokenizer.builder().enablePostgresqlEscapeString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedPostgresqlStringEmbeddedSingleQuoteEscapeUpper() {
        @NotNull final String sql = "SELECT E'ab\\'c(:'  FROM dual";
        doTest(Tokenizer.builder().enablePostgresqlEscapeString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedDollarStringEmbeddedSingleQuoteEscapeEmpty() {
        @NotNull final String sql = "SELECT $$ab'c(:$$  FROM dual";
        doTest(Tokenizer.builder().enablePostgresqlDollarString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedDollarStringEmbeddedSingleQuoteEscapeTag() {
        @NotNull final String sql = "SELECT $!!$ab'c(:$!!$  FROM dual";
        doTest(Tokenizer.builder().enablePostgresqlDollarString(true).build(sql), makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedAnsiStringUnclosed() {
        @NotNull final String sql = "SELECT 'abc (: FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedIdentifierSimple() {
        @NotNull final String sql = "SELECT \"abc (:\" FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedIdentifierSquare() {
        @NotNull final String sql = "SELECT [abc (:] FROM dual";
        doTest(Tokenizer.builder().enableSquareBracketIdentifierQuoting(true).build(sql),
                makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedIdentifierEmbeddedDoubleQuote() {
        @NotNull final String sql = "SELECT \"abc\"\" (:\" FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedIdentifierUnclosed() {
        @NotNull final String sql = "SELECT 'abc (:FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unclosedBracket() {
        @NotNull final String sql = "SELECT (:abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN, "(:"), makeToken(TokenType.TEXT, "abc FROM dual"));
    }

    @Test
    void unbracketedLineCommentEol() {
        @NotNull final String sql = "SELECT --(:abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedLineCommentNewLine() {
        @NotNull final String sql = "SELECT --(:abc FROM\ndual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedBlockComment() {
        @NotNull final String sql = "SELECT /* (:\n blah boat :) */ abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedNestedBlockComment() {
        @NotNull final String sql = "SELECT /* /* blah, */ (:\n blah boat :) */ abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void unbracketedNestedBlockComment2() {
        @NotNull final String sql = "SELECT /* /* blah, */ */ (: blah boat :) abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT /* /* blah, */ */ "), makeToken(TokenType.SMILEY_OPEN, "(:"),
                makeToken(TokenType.TEXT, " blah boat "), makeToken(TokenType.SMILEY_CLOSE, ":)"),
                makeToken(TokenType.TEXT, " abc FROM dual"));
    }

    @Test
    void unbracketedUnnestedBlockComment() {
        @NotNull final String sql = "SELECT /* /* blah, */ (:\n blah boat :) */ abc FROM dual";
        doTest(Tokenizer.builder().enableNestedBlockComment(false).build(sql),
                makeToken(TokenType.TEXT, "SELECT /* /* blah, */ "), makeToken(TokenType.SMILEY_OPEN, "(:"),
                makeToken(TokenType.TEXT, "\n blah boat "), makeToken(TokenType.SMILEY_CLOSE, ":)"),
                makeToken(TokenType.TEXT, " */ abc FROM dual"));
    }

    @Test
    void unbracketedUnterminatedBlockComment() {
        @NotNull final String sql = "SELECT /* (:\n blah boat :)  abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, sql));
    }

    @Test
    void bracketedSimple() {
        @NotNull final String sql = "SELECT (: blah :foo boat :) abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN, "(:"),
                makeToken(TokenType.TEXT, " blah "), makeToken(TokenType.VAR, ":foo"),
                makeToken(TokenType.TEXT, " boat "), makeToken(TokenType.SMILEY_CLOSE, ":)"),
                makeToken(TokenType.TEXT, " abc FROM dual"));
    }

    @Test
    void bracketedLineComment() {
        @NotNull final String sql = "SELECT (: blah :foo --boat :) abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN, "(:"),
                makeToken(TokenType.TEXT, " blah "), makeToken(TokenType.VAR, ":foo"),
                makeToken(TokenType.TEXT, " --boat :) abc FROM dual"));
    }

    @Test
    void bracketedBlockComment() {
        @NotNull final String sql = "SELECT (: blah :foo /*boat :) abc FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN, "(:"),
                makeToken(TokenType.TEXT, " blah "), makeToken(TokenType.VAR, ":foo"),
                makeToken(TokenType.TEXT, " /*boat :) abc FROM dual"));
    }

    @Test
    void bracketedString() {
        @NotNull final String sql = "SELECT (: blah ':foo boat :) abc' FROM dual";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN, "(:"),
                makeToken(TokenType.TEXT, " blah ':foo boat :) abc' FROM dual"));
    }

    @Test
    void bracketedLineQuotedId() {
        @NotNull final String sql = "SELECT (: blah \":foo boat :) abc FROM dual\"";
        doTest(sql, makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN, "(:"),
                makeToken(TokenType.TEXT, " blah \":foo boat :) abc FROM dual\""));
    }

    @Test
    void bracketedSquareQuotedId() {
        @NotNull final String sql = "SELECT (: blah [:foo boat :) abc] FROM dual";
        doTest(Tokenizer.builder().enableSquareBracketIdentifierQuoting(true).build(sql),
                makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN, "(:"),
                makeToken(TokenType.TEXT, " blah [:foo boat :) abc] FROM dual"));
    }

    @Test
    void peek() {
        @NotNull Tokenizer tokenizer = Tokenizer.builder().enableSquareBracketIdentifierQuoting(true).build("foo");
        assertEquals(TokenType.TEXT, tokenizer.peek());
        assertEquals(TokenType.TEXT, tokenizer.next().getTokenType());
        assertEquals(TokenType.EOF, tokenizer.peek());
    }

    //TODO remove this test when nested brackets are supported
    @Test
    void unsupportedNestedBrackets() {
        @NotNull final String sql = "SELECT (: blah (:foo boat :) abc] FROM dual";
        assertThrows(UnsupportedFeatureException.class, () -> doTest(Tokenizer.builder().enableSquareBracketIdentifierQuoting(true).build(sql),
                makeToken(TokenType.TEXT, "SELECT "), makeToken(TokenType.SMILEY_OPEN, "(:"),
                makeToken(TokenType.TEXT, " blah "),
                makeToken(TokenType.SMILEY_OPEN, "(:"),
                makeToken(TokenType.TEXT, "foo boat "),
                makeToken(TokenType.TEXT, ":) abc] FROM dual")));
    }

    private void doTest(String sql, Token... tokens) {
        @NotNull Tokenizer tokenizer = new Tokenizer(sql);
        doTest(tokenizer, tokens);
    }

    private void doTest(@NotNull Tokenizer tokenizer, @NotNull Token... tokens) {
        for (@NotNull Token thisToken : tokens) {
            assertTrue(tokenizer.hasNext());
            Token token = tokenizer.next();
            assertEquals(thisToken.getTokenType(), token.getTokenType());
            assertEquals(thisToken.getTokenchars(), token.getTokenchars());
        }
        assertFalse(tokenizer.hasNext());
    }

    @NotNull
    private Token makeToken(TokenType tokenType, @NotNull String s) {
        return new Token(tokenType, s, 0, s.length());
    }
}