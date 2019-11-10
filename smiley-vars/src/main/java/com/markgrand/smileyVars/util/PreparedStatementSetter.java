package com.markgrand.smileyVars.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Functional interface to set the value of a prepared statement parameter. This is used to create formatters for
 * additional data types or databases.
 */
@FunctionalInterface
public interface PreparedStatementSetter {
    /**
     * Performs this operation on the given arguments.
     *
     * @param preparedStatement the prepared statement whose parameter is to be set.
     * @param i                 The index of the parameter to be set
     * @param value             The value to set the parameter to.
     * @throws SQLException if there is a problem setting the PreparedStatement parameter.
     */
    void apply(PreparedStatement preparedStatement, int i, Object value) throws SQLException;
}