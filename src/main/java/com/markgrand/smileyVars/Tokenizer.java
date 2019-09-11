package com.markgrand.smileyVars;

/**
 * Iterate over the smileyVars tokens in a {@link CharSequence}
 *
 * @author Mark Grand
 */
class Tokenizer {
    private CharSequence chars;

    /**
     * Construct a {@code Tokenizer} with default configuration.
     *
     * @param chars
     */
    public Tokenizer(CharSequence chars) {
        this.chars = chars;
    }
}
