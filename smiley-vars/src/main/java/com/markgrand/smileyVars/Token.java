package com.markgrand.smileyVars;

/**
 * Represent a token in a smileyVars template
 */
class Token {
    private final TokenType tokenType;
    private final CharSequence chars;
    private final int startPosition; // Start position of token in chars (inclusive).
    private final int endPosition; // End position of token in chars (inclusive).
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
        this.startPosition = TokenType.VAR.equals(tokenType) ? startPosition + 1 : startPosition;
        this.endPosition = Math.min(endPosition, chars.length());
    }

    /**
     * Return the type of token this is.
     *
     * @return the type of token this is.
     */
    TokenType getTokenType() {
        return tokenType;
    }

    String getTokenchars() {
        if (tokenChars == null) {
            tokenChars = startPosition < endPosition ? chars.subSequence(startPosition, endPosition) : "";
        }
        return tokenChars.toString();
    }

    @Override
    public String toString() {
        return "Token[" + tokenType + ", \"" + getTokenchars() + "\"]";
    }
}