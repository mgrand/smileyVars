package com.markgrand.smileyvars;

/**
 * Types of tokens used in parsing smileyVars
 */
enum TokenType {
    /**
     * SQL text that may be copied in the expansion process.
     */
    TEXT,
    /**
     * Opening smiley bracket
     */
    SMILEY_OPEN,
    /**
     * Closing smiley bracket
     */
    SMILEY_CLOSE,
    /**
     * A substitution variable.
     */
    VAR,
    /**
     * Marks the end.
     */
    EOF
}
