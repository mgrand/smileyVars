package com.markgrand.smileyVars;

/**
 * This is thrown to indicate that SmileyVars feature is not yet supported.
 */
@SuppressWarnings("WeakerAccess")
public class UnsupportedFeatureException extends SmileyVarsException {
    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized, and may subsequently
     * be initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *                method.
     */
    UnsupportedFeatureException(String message) {
        super(message);
    }
}
