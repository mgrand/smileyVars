package com.markgrand.smileyVars;

import java.util.Iterator;
import java.util.function.Supplier;

/**
 * Iterate over the smileyVars tokens in a {@link CharSequence}
 *
 * @author Mark Grand
 */
class Tokenizer implements Iterator<Token> {
    //TODO support nested block comments for not Oracle
    //TODO support oracleDelimitedString
    //TODO support postgresqlDollarString

    private static final TokenizerConfig DEFAULT_CONFIG = new TokenizerConfig();
    static {
        DEFAULT_CONFIG.oracleDelimitedStringEnabled = false;
        DEFAULT_CONFIG.postgresqlDollarStringEnabled = false;
        DEFAULT_CONFIG.postgresqlEscapeStringEnabled = false;
        DEFAULT_CONFIG.nestedBlockCommentEnabled = true;
    }

    private CharSequence chars;
    private int nextPosition = 0;
    private Token nextToken ;
    private TokenizerConfig config;

    /**
     * Construct a {@code Tokenizer} with default configuration.
     *
     * @param chars
     */
    Tokenizer(CharSequence chars) {
        this.chars = chars;
        config = DEFAULT_CONFIG;
        scanNextToken();
    }

    // Scanner to use outside of (: :)
    private Supplier<TokenType> scanUnbracketed = new Supplier<TokenType>() {
        @Override
        public TokenType get() {
            char c = nextChar();
            if (c == '(') {
                if (isNextChar(':')) {
                    tokenScanner = scanBracketed;
                    return TokenType.SMILEY_OPEN;
                } else {
                    c = nextChar();
                }
            }
            do {
                if (c == '-' && isNextChar('-')) {
                    scanToEndOfLine();
                } else if (c == '/' && isNextChar('*') ) {
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
                }
                c = nextChar();
            } while (nextPosition < chars.length());
            return TokenType.TEXT;
        }
    };

    private void scanOracleDelimitedString() {
        if (nextPosition < chars.length()) {
            char delimiter = adjustDelimiter(nextChar());
            while (nextPosition < chars.length()) {
                if (nextChar() == delimiter) {
                    if (!isNextChar('\'')) {
                        return;
                    }
                    nextPosition += 1; // skip over second single quote.
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
            if (c == '\'' ) {
                if (!isNextChar('\'')) {
                    return;
                }
                nextPosition += 1; // skip over second single quote.
            } else if (c == '\\') {
                if (isNextChar('\\')) {
                    nextPosition += 1;
                }
            }
        }
    }

    private void scanAnsiQuotedString() {
    }

    private void scanQuotedIdentifier() {
        while (nextPosition < chars.length()) {
            if (nextChar() == '"' ) {
                if (!isNextChar('"')) {
                    return;
                }
                nextPosition += 1; // skip over second double quote.
            }
        }
    }

    private void scanToEndOfBlockComment() {
        while (nextPosition < chars.length()) {
            if (nextChar() == '*' && isNextChar('/')) {
                return;
            }
        }
    }

    private void scanToEndOfLine() {
        while (! isNextChar('\n')) {
            nextPosition += 1;
        }
    }

    // Scanner to use inside of (: :)
    private Supplier<TokenType> scanBracketed = new Supplier<TokenType>() {
        @Override
        public TokenType get() {
            return null;
        }
    };

    private Supplier<TokenType> tokenScanner = scanUnbracketed;

    /**
     * ugly state loop
     */
    private void scanNextToken() {
        if (nextPosition < chars.length()) {
            nextToken = null;
            return; // Input is exhausted.
        }
        int tokenStart = nextPosition;
        TokenType tokenType = tokenScanner.get();
        int tokenEnd = nextPosition - 1;
        nextToken = new Token(tokenType, chars, tokenStart, tokenEnd);
    }

    private char nextChar() {
        char c = chars.charAt(nextPosition);
        nextPosition += 1;
        return c;
    }

    private boolean isNextChar(char c) {
        if (nextPosition < chars.length()) {
            return false;
        }
        if (c == chars.charAt(nextPosition)) {
            nextPosition +=1;
            return true;
        }
        return false;
    }

    /**
     * Return true if there is a next token.
     *
     * @return true if there is a next token.
     */
    @Override
    public boolean hasNext() {
        return nextToken != null;
    }

    /**
     * Get the next token.
     *
     * @return the token.
     */
    @Override
    public Token next() {
        Token token = nextToken;
        scanNextToken();
        return token;
    }

    private static class TokenizerConfig {
        boolean postgresqlEscapeStringEnabled;
        boolean postgresqlDollarStringEnabled;
        boolean oracleDelimitedStringEnabled;
        boolean nestedBlockCommentEnabled;
    }
}
