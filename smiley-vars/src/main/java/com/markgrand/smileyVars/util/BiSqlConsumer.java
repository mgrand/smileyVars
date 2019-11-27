package com.markgrand.smileyVars.util;

import java.sql.SQLException;

/**
 * This is like BiConsumer, but throws SqlException.
 */
@FunctionalInterface
public interface BiSqlConsumer<T, U> {
    void accept(T t, U u) throws SQLException;

    default boolean isVacuous() { return false; }
}
