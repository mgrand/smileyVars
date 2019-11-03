package com.markgrand.smileyVars;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * Iterate over the smileyVars tokens in a {@link CharSequence}
 *
 * @author Mark Grand
 */
class Tokenizer implements Iterator<Token> {
    private static final TokenizerConfig DEFAULT_CONFIG = new TokenizerConfig();

    static {
        DEFAULT_CONFIG.oracleDelimitedStringEnabled = false;
        DEFAULT_CONFIG.postgresqlDollarStringEnabled = false;
        DEFAULT_CONFIG.postgresqlEscapeStringEnabled = false;
        DEFAULT_CONFIG.nestedBlockCommentEnabled = true;
    }

    private CharSequence chars;
    private int nextPosition = 0;
    private Token nextToken;
    private TokenizerConfig config;

    private Supplier<TokenType> tokenScanner;
    private final Supplier<TokenType> scanUnbracketed = new ScanUnbracketed();

    // Scanner to use inside of (: :)
    private final Supplier<TokenType> scanBracketed = () -> {
        if (isEof()) {
            return TokenType.EOF;
        }
        char c = nextChar();
        if (c == ':') {
            if (isNextChar(')')) {
                tokenScanner = scanUnbracketed;
                return TokenType.SMILEY_CLOSE;
            } else if (isNextCharIdentifierStart()) {
                scanToEndOfIdentifier();
                return TokenType.VAR;
            }
        } else if (c == '(') {
            if (isNextChar(':')) {
                //TODO Support nested brackets.
                throw new UnsupportedFeatureException("Nested brackets are not yet supported");
            }
        }
        while (true) {
            if (c == '-' && isNextChar('-')) {
                scanToEndOfLine();
            } else if (c == '/' && isNextChar('*')) {
                scanToEndOfBlockComment();
            } else if (c == '"') {
                scanQuotedIdentifier();
            } else if (c == '\'') {
                scanAnsiQuotedString();
            } else if (config.postgresqlEscapeStringEnabled && (c == 'e' || c == 'E') && isNextChar('\'')) {
                scanPostgresqlEscapeString();
            } else if (config.postgresqlDollarStringEnabled && c == '$') {
                scanPostgresqlDollarString();
            } else if (config.oracleDelimitedStringEnabled && (c == 'q' || c == 'Q') && isNextChar('\'')) {
                scanOracleDelimitedString();
            } else if (c == '[' && config.squareBracketIdentifierQuotingEnabled) {
                scanPast(']');
            }
            if (nextPosition >= chars.length()) {
                break;
            }
            c = nextChar();
            if (c == ':') {
                if (isNextChar(')') || isNextCharIdentifierStart()) {
                    nextPosition -= 2;
                    break;
                }
            } else if (c == '(') {
                if (isNextChar(':')) {
                    nextPosition -= 2;
                    break;
                }
            }

        }
        return TokenType.TEXT;
    };

    // Scanner to use outside of (: :)
    private class ScanUnbracketed implements Supplier<TokenType> {
        public TokenType get() {
            if (isEof()) {
                return TokenType.EOF;
            }
            char c = nextChar();
            if (c == '(') {
                if (isNextChar(':')) {
                    tokenScanner = scanBracketed;
                    return TokenType.SMILEY_OPEN;
                }
            } else if (c == ':' && isNextCharIdentifierStart()) {
                scanToEndOfIdentifier();
                return TokenType.VAR;
            }
            while (true) {
                if (c == '-' && isNextChar('-')) {
                    scanToEndOfLine();
                } else if (c == ':' && Character.isJavaIdentifierStart(chars.charAt(nextPosition))) {
                    nextPosition -= 1;
                    return TokenType.TEXT;
                } else if (c == '/' && isNextChar('*')) {
                    scanToEndOfBlockComment();
                } else if (c == '"') {
                    scanQuotedIdentifier();
                } else if (c == '\'') {
                    scanAnsiQuotedString();
                } else if (config.postgresqlEscapeStringEnabled && (c == 'e' || c == 'E') && isNextChar('\'')) {
                    scanPostgresqlEscapeString();
                } else if (config.postgresqlDollarStringEnabled && c == '$') {
                    scanPostgresqlDollarString();
                } else if (config.oracleDelimitedStringEnabled && (c == 'q' || c == 'Q') && isNextChar('\'')) {
                    scanOracleDelimitedString();
                } else if (c == '[' && config.squareBracketIdentifierQuotingEnabled) {
                    scanPast(']');
                }
                if (nextPosition >= chars.length()) {
                    break;
                }
                c = nextChar();
                if (c == '(') {
                    if (isNextChar(':')) {
                        nextPosition -= 2;
                        break;
                    }
                }
            }
            return TokenType.TEXT;
        }
    }

    /**
     * Construct a {@code Tokenizer} with default configuration.
     *
     * @param chars The sql/template body to be expanded.
     */
    @SuppressWarnings("unused")
    Tokenizer(CharSequence chars) {
        this(chars, DEFAULT_CONFIG);
    }

    private Tokenizer(CharSequence chars, TokenizerConfig config) {
        this.config = config;
        this.chars = chars;
        tokenScanner = scanUnbracketed;
        scanNextToken();
    }

    /**
     * Get a builder for Tokenizer objects.
     *
     * @return the builder object.
     */
    static TokenizerBuilder builder() {
        return new TokenizerBuilder();
    }

    private void scanPast(@SuppressWarnings("SameParameterValue") char c) {
        while (!isNextChar(c)) {
            nextPosition += 1;
        }
    }

    private void scanOracleDelimitedString() {
        if (nextPosition < chars.length()) {
            char delimiter = adjustDelimiter(nextChar());
            while (nextPosition < chars.length()) {
                if (nextChar() == delimiter) {
                    if (isNextChar('\'')) {
                        return;
                    }
                }
            }
        }
    }

    private char adjustDelimiter(char delimiter) {
        switch (delimiter) {
            case '(':
                delimiter = ')';
                break;
            case '<':
                delimiter = '>';
                break;
            case '{':
                delimiter = '}';
                break;
            case '[':
                delimiter = ']';
                break;
        }
        return delimiter;
    }

    private void scanPostgresqlDollarString() {
        int tagStartPosition = nextPosition;
        scanToEndOfDollarTag();
        int tagEndPosition = nextPosition - 2;
        int tagLength = tagEndPosition - tagStartPosition;
        while (nextPosition < chars.length() - tagLength - 1) {
            if (isNextChar('$')) {
                if (isMatchingTag(tagStartPosition, tagEndPosition)) {
                    return;
                }
            } else {
                nextPosition += 1;
            }
        }
    }

    private boolean isMatchingTag(int tagStartPosition, int tagEndPosition) {
        int tagLength = tagEndPosition - tagStartPosition;
        for (int i = 0; i < tagLength; i++) {
            if (chars.charAt(tagStartPosition + i) != chars.charAt(nextPosition + i)) {
                return false;
            }
        }
        return chars.charAt(nextPosition + tagLength) == '$';
    }

    private void scanToEndOfDollarTag() {
        while (nextPosition < chars.length()) {
            if (isNextChar('$')) {
                return;
            }
            nextPosition += 1;
        }
    }

    private void scanPostgresqlEscapeString() {
        while (nextPosition < chars.length()) {
            char c = nextChar();
            if (c == '\\') {
                nextPosition += 1;
            } else if (c == '\'') {
                return;
            }
        }
    }

    private void scanAnsiQuotedString() {
        while (nextPosition < chars.length()) {
            if (nextChar() == '\'') {
                if (!isNextChar('\'')) {
                    return;
                }
                nextPosition += 1; // skip over second single quote.
            }
        }
    }

    private void scanQuotedIdentifier() {
        while (nextPosition < chars.length()) {
            if (nextChar() == '"') {
                if (!isNextChar('"')) {
                    return;
                }
                nextPosition += 1; // skip over second double quote.
            }
        }
    }

    private void scanToEndOfBlockComment() {
        if (config.nestedBlockCommentEnabled) {
            scanToEndOfNestedBlockComment();
        } else {
            scanToEndOfUnnestedBlockComment();
        }
    }

    private void scanToEndOfUnnestedBlockComment() {
        while (nextPosition < chars.length()) {
            if (nextChar() == '*' && isNextChar('/')) {
                return;
            }
        }
    }

    private void scanToEndOfNestedBlockComment() {
        int count = 1;
        while (nextPosition < chars.length()) {
            char c = nextChar();
            if (c == '/') {
                if (isNextChar('*')) {
                    count += 1;
                }
            } else if (c == '*') {
                if (isNextChar('/')) {
                    count -= 1;
                    if (count == 0) {
                        return;
                    }
                }
            }
        }
    }

    private void scanToEndOfLine() {
        while (!isEof() && !isNextChar('\n')) {
            nextPosition += 1;
        }
    }

    private void scanToEndOfIdentifier() {
        //noinspection StatementWithEmptyBody
        while (isNextCharIdentifierPart()) {
        }
    }

    private void scanNextToken() {
        if (isEof()) {
            nextToken = new Token(TokenType.EOF, chars, nextPosition, chars.length());
            return; // Input is exhausted.
        }
        int tokenStart = nextPosition;
        TokenType tokenType = tokenScanner.get();
        int tokenEnd = nextPosition;
        nextToken = new Token(tokenType, chars, tokenStart, tokenEnd);
    }

    private char nextChar() {
        char c = chars.charAt(nextPosition);
        nextPosition += 1;
        return c;
    }

    private boolean isNextChar(char c) {
        if (isEof()) {
            return false;
        }
        if (c == chars.charAt(nextPosition)) {
            nextPosition += 1;
            return true;
        }
        return false;
    }

    private boolean isNextCharIdentifierStart() {
        if (isEof()) {
            return false;
        }
        if (Character.isJavaIdentifierStart(chars.charAt(nextPosition))) {
            nextPosition += 1;
            return true;
        }
        return false;
    }

    private boolean isNextCharIdentifierPart() {
        if (isEof()) {
            return false;
        }
        if (Character.isJavaIdentifierPart(chars.charAt(nextPosition))) {
            nextPosition += 1;
            return true;
        }
        return false;
    }

    private boolean isEof() {
        return nextPosition >= chars.length();
    }

    /**
     * Return true if there is a next token.
     *
     * @return true if there is a next token.
     */
    @Override
    public boolean hasNext() {
        return !nextToken.getTokenType().equals(TokenType.EOF);
    }

    /**
     * Get the next token.
     *
     * @return the token.
     */
    @Override
    public Token next() {
        Token token = nextToken;
        if (TokenType.EOF.equals(token.getTokenType())) {
            throw new NoSuchElementException("No more tokens in template body.");
        }
        scanNextToken();
        return token;
    }

    /**
     * Return the type of the next token that will be returned be {@link #next()}.
     *
     * @return the type of the next token that will be returned be {@link #next()}.
     */
    TokenType peek() {
        if (nextToken == null) {
            return TokenType.EOF;
        }
        return nextToken.getTokenType();
    }

    static class TokenizerConfig {
        boolean postgresqlEscapeStringEnabled = false;
        boolean postgresqlDollarStringEnabled = false;
        boolean oracleDelimitedStringEnabled = false;
        boolean nestedBlockCommentEnabled = false;
        boolean squareBracketIdentifierQuotingEnabled = false;

        boolean isPostgresqlEscapeStringEnabled() {
            return postgresqlEscapeStringEnabled;
        }

        boolean isPostgresqlDollarStringEnabled() {
            return postgresqlDollarStringEnabled;
        }

        boolean isOracleDelimitedStringEnabled() {
            return oracleDelimitedStringEnabled;
        }

        boolean isNestedBlockCommentEnabled() {
            return nestedBlockCommentEnabled;
        }

        boolean isSquareBracketIdentifierQuotingEnabled() {
            return squareBracketIdentifierQuotingEnabled;
        }
    }

    /**
     * Builder class for Tokenizer
     */
    static class TokenizerBuilder {
        private final TokenizerConfig config = new TokenizerConfig();

        /**
         * Constructor declared to prevent auto-creation of public constructor.
         */
        TokenizerBuilder() {
        }

        TokenizerBuilder enablePostgresqlEscapeString(boolean value) {
            config.postgresqlEscapeStringEnabled = value;
            return this;
        }

        TokenizerBuilder enablePostgresqlDollarString(boolean value) {
            config.postgresqlDollarStringEnabled = value;
            return this;
        }

        TokenizerBuilder enableOracleDelimitedString(boolean value) {
            config.oracleDelimitedStringEnabled = value;
            return this;
        }

        TokenizerBuilder enableNestedBlockComment(boolean value) {
            config.nestedBlockCommentEnabled = value;
            return this;
        }

        TokenizerBuilder enableSquareBracketIdentifierQuoting(boolean value) {
            config.squareBracketIdentifierQuotingEnabled = value;
            return this;
        }

        TokenizerBuilder configureForAnsi() {
            return enableNestedBlockComment(true).enableOracleDelimitedString(false).enablePostgresqlDollarString(false)
                           .enablePostgresqlEscapeString(false).enableSquareBracketIdentifierQuoting(false);
        }

        TokenizerBuilder configureForOracle() {
            return enableNestedBlockComment(false).enableOracleDelimitedString(true).enablePostgresqlDollarString(false)
                           .enablePostgresqlEscapeString(false).enableSquareBracketIdentifierQuoting(false);
        }

        TokenizerBuilder configureForPostgresql() {
            return enableNestedBlockComment(true).enableOracleDelimitedString(false).enablePostgresqlDollarString(true)
                           .enablePostgresqlEscapeString(true).enableSquareBracketIdentifierQuoting(false);
        }

        TokenizerBuilder configureForSqlServer() {
            return enableNestedBlockComment(true).enableOracleDelimitedString(false).enablePostgresqlDollarString(false)
                           .enablePostgresqlEscapeString(false).enableSquareBracketIdentifierQuoting(true);
        }

        Tokenizer build(CharSequence chars) {
            return new Tokenizer(chars, config);
        }

        TokenizerConfig getConfig() { return config; }
    }
}