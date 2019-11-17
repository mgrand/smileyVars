package com.markgrand.smileyVars;

import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Class to represent a Reader and a length that will be used to set the value of prepared statement parameter.
 *
 * @author Mark Grand
 */
class ReaderValue extends AbstactPreparedStatementValue {
    private final Reader reader;
    private final Long length;

    /**
     * Constructor
     * @param reader The value that this object will be used to set in a prepared statement.
     */
    ReaderValue(Reader reader) {
        this.reader = reader;
        this.length = null;
    }

    /**
     * Constructor
     * @param reader The value that this object will be used to set in a prepared statement.
     * @param length The value that this object will be used to set in a prepared statement.
     */
    ReaderValue(Reader reader, int length) {
        this.reader = reader;
        this.length = Long.valueOf(length);
    }

    /**
     * Constructor
     * @param reader The value that this object will be used to set in a prepared statement.
     * @param length The value that this object will be used to set in a prepared statement.
     */
    ReaderValue(Reader reader, long length) {
        this.reader = reader;
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
            pstmt.setCharacterStream(i, reader);
        } else {
            pstmt.setCharacterStream(i, reader, length);
        }
    }
}
