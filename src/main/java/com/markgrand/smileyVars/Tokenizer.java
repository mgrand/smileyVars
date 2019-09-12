package com.markgrand.smileyVars;

import java.util.Iterator;
import java.util.function.Supplier;

/**
 * Iterate over the smileyVars tokens in a {@link CharSequence}
 *
 * @author Mark Grand
 */
class Tokenizer implements Iterator<Token> {
    private CharSequence chars;
    private int nextPosition = 0;
    Token nextToken ;

    // Scanner to use outside of (: :)
    private Supplier<TokenType> scanUnbracketed = new Supplier<TokenType>() {
        @Override
        public TokenType get() {
            char c = nextChar();
            if (c == '(' && isNextChar(':')) {
                tokenScanner = scanBracketed;
                return TokenType.SMILEY_OPEN;
            }
            do {
                if (c == '-' && isNextChar('-')) {
                    scanToEndOfLine();
                }
                    //TODO finish this
            } while (nextPosition < chars.length());
            return null;
        }
    };

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
}
