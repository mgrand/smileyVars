package com.markgrand.smileyVars;

/**
 * This is thrown to indicate that SmileyVars have a value to format as an SQL literal, but no registered formatter to
 * format it.
 *
 * @see ValueFormatterRegistry
 */
@SuppressWarnings("WeakerAccess")
public class NoFormatterException extends SmileyVarsException {
    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized, and may subsequently
     * be initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *                method.
     */
    public NoFormatterException(String message) {
        super(message);
    }
}
