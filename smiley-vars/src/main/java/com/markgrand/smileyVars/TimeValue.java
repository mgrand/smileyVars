package com.markgrand.smileyVars;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;

/**
 * Class to represent a Time value that will be used to set the value of prepared statement parameter.
 *
 * @author Mark Grand
 */
class TimeValue extends AbstactPreparedStatementValue {
    private Time value;

    /**
     * Constructor
     * @param value The value that this object will be used to set in a prepared statement.
     */
    TimeValue(Time value) {
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
        pstmt.setTime(i, value);
    }
}
