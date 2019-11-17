package com.markgrand.smileyVars;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * Class to represent a Timestamp value that will be used to set the value of prepared statement parameter.
 *
 * @author Mark Grand
 */
class TimestampValue extends AbstactPreparedStatementValue {
    private final Timestamp value;
    private final Calendar calendar;

    /**
     * Constructor
     * @param value The value that this object will be used to set in a prepared statement.
     */
    TimestampValue(Timestamp value) {
        this(value, null);
    }

    /**
     * Constructor
     * @param value The value that this object will be used to set in a prepared statement.
     * @param calendar A calander to establish time zone.
     */
    TimestampValue(Timestamp value, Calendar calendar) {
        this.value = value;
        this.calendar = calendar;
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
        if (calendar == null) {
            pstmt.setTimestamp(i, value);
        } else {
            pstmt.setTimestamp(i, value, calendar);
        }
    }
}
