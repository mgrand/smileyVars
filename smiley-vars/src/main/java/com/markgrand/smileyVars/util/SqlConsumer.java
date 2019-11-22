package com.markgrand.smileyVars.util;

import java.sql.SQLException;

/**
 * This is like BiConsumer, but with three parameters.
 */
@FunctionalInterface
public interface SqlConsumer<T> {
    void accept(T t) throws SQLException;
}
