package com.markgrand.smileyvars;

import java.sql.SQLException;

/**
 * Unchecked exception that is thrown by SmileyVars when there is an {@link java.sql.SQLException}.
 */
@SuppressWarnings("WeakerAccess")
public class SmileyVarsSqlException extends SmileyVarsException {
    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *                method.
     * @param e The underlying exception.
     */
    SmileyVarsSqlException(String message, SQLException e) {
        super(message, e);
    }
}
