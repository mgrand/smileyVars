package com.markgrand.smileyVars;

/**
 * Represent a token in a smileyVars template
 */
class Token {
    private TokenType tokenType;
    private CharSequence chars;
    private int startPosition; // Start position of token in chars (inclusive).
    private int endPosition; // End position of token in chars (inclusive).
    private CharSequence tokenChars = null;

    /**
     * Constructor
     *
     * @param tokenType     The type of token this is.
     * @param chars         The full {@link CharSequence} that this token is part of.
     * @param startPosition The position in {@code chars} that corresponds to the first character of this token.
     * @param endPosition   The position in {@code chars} that corresponds to the last character of this token.
     */
    Token(TokenType tokenType, CharSequence chars, int startPosition, int endPosition) {
        this.tokenType = tokenType;
        this.chars = chars;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    /**
     * Return the type of token this is.
     *
     * @return the type of token this is.
     */
    TokenType getTokenType() {
        return tokenType;
    }

    CharSequence getTokenchars() {
        if (tokenChars == null) {
            tokenChars = startPosition < endPosition ? chars.subSequence(startPosition, endPosition) : "";
        }
        return tokenChars;
    }

    @Override
    public String toString() {
        return "Token[" + tokenType + ", \"" + getTokenchars() + "\"]";
    }
}
