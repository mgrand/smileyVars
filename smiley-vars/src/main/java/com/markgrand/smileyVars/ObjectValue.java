package com.markgrand.smileyVars;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Class to represent an Object value that will be used to set the value of prepared statement parameter.
 *
 * @author Mark Grand
 */
class ObjectValue extends AbstactPreparedStatementValue {
    private final Object object;
    private final int sqlType;
    private final Integer scaleOrLength;

    /**
     * Constructor
     *
     * @param object  The value that this object will be used to set in a prepared statement.
     * @param sqlType the SQL type (as defined in java.sql.Types) to be sent to the database
     */
    ObjectValue(Object object, int sqlType) {
        this(object, sqlType, null);
    }

    /**
     * Constructor
     *
     * @param object        The value that this object will be used to set in a prepared statement.
     * @param sqlType       the SQL type (as defined in java.sql.Types) to be sent to the database
     * @param scaleOrLength for <code>java.sql.Types.DECIMAL</code> or <code>java.sql.Types.NUMERIC types</code>, this
     *                      is the number of digits after the decimal point. For Java Object types
     *                      <code>InputStream</code> and <code>Reader</code>, this is the length of the data in the
     *                      stream or reader.  For all other types, this value will be ignored.
     */
    ObjectValue(Object object, int sqlType, Integer scaleOrLength) {
        this.object = object;
        this.sqlType = sqlType;
        this.scaleOrLength = scaleOrLength;
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
        if (scaleOrLength == null) {
            pstmt.setObject(i, object, sqlType);
        } else {
            pstmt.setObject(i, object, sqlType, scaleOrLength);
        }
    }
}
