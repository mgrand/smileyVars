package com.markgrand.smileyvars.util;

import java.sql.SQLException;

/**
 * This is like Function, but throws SqlException.
 */
@FunctionalInterface
public interface SqlFunction<T, R> {
    R apply(T t) throws SQLException;
}
