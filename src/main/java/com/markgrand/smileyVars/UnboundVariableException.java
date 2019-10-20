package com.markgrand.smileyVars;

/**
 * This is thrown to indicate that no variable has been supplied for a variable that is not inside smileyVars brackets
 * <pre>
 * (: ... :)
 * </pre>
 */
@SuppressWarnings("WeakerAccess")
public class UnboundVariableException extends SmileyVarsException {
    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized, and may subsequently
     * be initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *                method.
     */
    UnboundVariableException(String message) {
        super(message);
    }
}
