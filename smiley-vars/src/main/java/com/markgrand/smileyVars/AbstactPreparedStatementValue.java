package com.markgrand.smileyVars;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Abstract class for objects that capture parameter values for prepared statements.
 *
 * @author Mark Grand
 */
abstract class AbstactPreparedStatementValue {
    /**
     * Set the Parameter of the given PreparedStatement at index <i>i</i> to the value in this object.
     *
     * @param pstmt The prepared statement whose parameter is to be set.
     * @param i The index of the parameter.
     * @throws SQLException if there is a problem.
     */
    abstract void setParameter(PreparedStatement pstmt, int i) throws SQLException;
}
