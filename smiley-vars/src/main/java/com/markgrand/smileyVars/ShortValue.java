package com.markgrand.smileyVars;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Class to represent a short value that will be used to set the value of prepared statement parameter.
 *
 * @author Mark Grand
 */
class ShortValue extends AbstactPreparedStatementValue {
    private short value;

    /**
     * Constructor
     * @param value The value that this object will be used to set in a prepared statement.
     */
    ShortValue(short value) {
        this.value = value;
    }

    /**
     * Set the Parameter of the given PreparedStatement at index <i>i</i> to the value in this object.
     *
     * @param pstmt The prepared statement whose parameter is to be set.
     * @param i     The index of the parameter.
     * @throws SQLException if there is a problem.
     */
    @Override
    void setParameter(PreparedStatement pstmt, int i) throws SQLException {
        pstmt.setShort(i, value);
    }
}