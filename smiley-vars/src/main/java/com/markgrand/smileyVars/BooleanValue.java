package com.markgrand.smileyVars;

import java.sql.PreparedStatement;
import java.sql.SQLException;

class BooleanValue extends AbstactPreparedStatementValue {
    private boolean value;

    /**
     * Constructor
     * @param value The value that this object will be used to set in a prepared statement.
     */
    BooleanValue(boolean value) {
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
        pstmt.setBoolean(i, value);
    }
}
