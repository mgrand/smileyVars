package com.markgrand.smileyvars.util;

import java.sql.SQLException;

/**
 * This is like Consumer, but throws SqlException.
 */
@FunctionalInterface
public interface SqlConsumer<T> {
    void accept(T t) throws SQLException;
}
