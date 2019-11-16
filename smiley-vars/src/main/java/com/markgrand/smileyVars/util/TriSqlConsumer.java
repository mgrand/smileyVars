package com.markgrand.smileyVars.util;

import java.sql.SQLException;

/**
 * This is like BiConsumer, but with three parameters.
 */
@FunctionalInterface
public interface TriSqlConsumer<T, U, V> {
    void accept(T t, U u, V v) throws SQLException;
}
