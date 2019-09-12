package com.markgrand.smileyVars;

import java.util.Iterator;

/**
 * Iterate over the smileyVars tokens in a {@link CharSequence}
 *
 * @author Mark Grand
 */
class Tokenizer implements Iterator<Token> {
    public static final char NUL_CHAR = '\00';

    private CharSequence chars;
    private int nextPosition = 0;
    Token nextToken ;

    /**
     * Construct a {@code Tokenizer} with default configuration.
     *
     * @param chars
     */
    Tokenizer(CharSequence chars) {
        this.chars = chars;
        scanNextToken();
    }

    /**
     * ugly state loop
     */
    private void scanNextToken() {
        if (nextPosition < chars.length()) {
            nextToken = null;
            return; // Input is exhausted.
        }
        int tokenStart = nextPosition;
        TokenType tokenType = scanNonEmptyToken();
        int tokenEnd = nextPosition - 1;
        nextToken = new Token(tokenType, chars, tokenStart, tokenEnd);
    }

    //
    private static final int START = 0;

    /**
     *
     * @return
     */
    private TokenType scanNonEmptyToken() {
        while (nextPosition < chars.length()) {
            char c = nextChar();
            switch (c) {
                case '(':
                    nextPosition += 1;
                    if (isNextChar(':')) {
                        return TokenType.SMILEY_OPEN;
                    }
                    scanText();
                    return TokenType.TEXT;

                //TODO finish this
                default:
                    break;
            }
        }
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
}
