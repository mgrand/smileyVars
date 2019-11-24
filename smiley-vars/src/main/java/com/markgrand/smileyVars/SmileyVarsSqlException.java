package com.markgrand.smileyVars;

import java.sql.SQLException;

/**
 * Unchecked exception that is thrown by SmileyVars when there is an {@link java.sql.SQLException}.
 */
@SuppressWarnings("WeakerAccess")
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

    SmileyVarsSqlException(String message, SQLException e) {
        super(message, e);
    }
}
