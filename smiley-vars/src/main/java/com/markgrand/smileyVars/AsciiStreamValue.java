package com.markgrand.smileyVars;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Class to represent an input stream and a length that will be used to set the value of prepared statement parameter.
 *
 * @author Mark Grand
 */
class AsciiStreamValue extends AbstactPreparedStatementValue {
    private final  InputStream inputStream;
    private final Long length;

    /**
     * Constructor
     * @param inputStream The value that this object will be used to set in a prepared statement.
     */
    AsciiStreamValue(InputStream inputStream) {
        this.inputStream = inputStream;
        this.length = null;
    }

    /**
     * Constructor
     * @param inputStream The value that this object will be used to set in a prepared statement.
     * @param length The value that this object will be used to set in a prepared statement.
     */
    AsciiStreamValue(InputStream inputStream, int length) {
        this.inputStream = inputStream;
        this.length = Long.valueOf(length);
    }

    /**
     * Constructor
     * @param inputStream The value that this object will be used to set in a prepared statement.
     * @param length The value that this object will be used to set in a prepared statement.
     */
    AsciiStreamValue(InputStream inputStream, long length) {
        this.inputStream = inputStream;
        this.length = length;
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
        if (length == null) {
            pstmt.setAsciiStream(i, inputStream);
        } else {
            pstmt.setAsciiStream(i, inputStream, length);
        }
    }
}
