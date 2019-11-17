package com.markgrand.smileyVars.util;

import java.sql.SQLException;

/**
 * This is like BiConsumer, but with four parameters.
 */
@FunctionalInterface
public interface QuadSqlConsumer<T, U, V, W> {
    void accept(T t, U u, V v, W w) throws SQLException;
}
