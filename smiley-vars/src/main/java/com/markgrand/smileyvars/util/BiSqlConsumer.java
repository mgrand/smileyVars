package com.markgrand.smileyvars.util;

import java.sql.SQLException;

/**
 * This is like BiConsumer, but throws SqlException.
 */
@FunctionalInterface
public interface BiSqlConsumer<T, U> {
    /**
     * Prototype for lambdas.
     *
     * @param t the first parameter type
     * @param u the second parameter type
     * @throws SQLException if the implementation chooses to throw the exception.
     */
    @SuppressWarnings("unused")
    void accept(T t, U u) throws SQLException;

    /**
     * Return false to indicate that this object represent an assigned value for a SmileyVar.
     *
     * @return false to indicate that this object represent an assigned value for a SmileyVar.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean isVacuous() {
        return false;
    }
}
