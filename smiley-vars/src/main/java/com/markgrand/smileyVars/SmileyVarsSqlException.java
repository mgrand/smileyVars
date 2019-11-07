package com.markgrand.smileyVars;

/**
 * Unchecked exception that is thrown by SmileyVars when there is an {@link java.sql.SQLException}.
 */
public class SmileyVarsSqlException extends SmileyVarsException {
    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized, and may subsequently
     * be initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *                method.
     */
    SmileyVarsSqlException(String message) {
        super(message);
    }

    /**
     * Construct a new exception with the given cause.
     *
     * @param message The message.
     * @param cause   The cause.
     */
    SmileyVarsSqlException(String message, Exception cause) {
        super(message, cause);
    }
}
