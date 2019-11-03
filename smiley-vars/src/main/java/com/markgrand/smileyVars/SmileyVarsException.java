package com.markgrand.smileyVars;

/**
 * Superclass of all exceptions explicitly thrown by SmileyVars. You can catch all exceptions explicitly thrown by
 * SmileyVars by catching this.
 */
@SuppressWarnings("WeakerAccess")
public class SmileyVarsException extends RuntimeException {
    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *                method.
     */
    SmileyVarsException(String message) {
        super(message);
    }
}
