package com.markgrand.smileyVars;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Class used to represent a null value for a prepared statement parameter
 */
class NullValue extends AbstactPreparedStatementValue {
    private int type;

    /**
     * Constructor
     *
     * @param type A type constant from {@link java.sql.Types}.
     * @throws SQLException if the given types is not a value defined in java.sql.Types.
     */
    NullValue(int type) throws SQLException {
        this.type = type;
        checkType(type);
    }

    private void checkType(int type) throws SQLException {
        switch (type) {
            case Types.BOOLEAN:
            case Types.TINYINT:
            case Types.ARRAY:
            case Types.BIGINT:
            case Types.BINARY:
            case Types.BIT:
            case Types.BLOB:
            case Types.CHAR:
            case Types.CLOB:
            case Types.DATALINK:
            case Types.DATE:
            case Types.DECIMAL:
            case Types.DISTINCT:
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.INTEGER:
            case Types.JAVA_OBJECT:
            case Types.LONGNVARCHAR:
            case Types.LONGVARBINARY:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NCLOB:
            case Types.NULL:
            case Types.NUMERIC:
            case Types.NVARCHAR:
            case Types.OTHER:
            case Types.REAL:
            case Types.REF:
            case Types.REF_CURSOR:
            case Types.ROWID:
            case Types.SMALLINT:
            case Types.SQLXML:
            case Types.STRUCT:
            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE:
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
            case Types.VARBINARY:
            case Types.VARCHAR:
                break;

            default:
                throw new SQLException("Invalid type code that is not a java.sql.Types code: " + type);
        }
    }

    /**
     * Return the type associated with this object, which should be one of the values defined in {@link
     * java.sql.Types}.
     *
     * @return the type associate with this object.
     */
    int getType() {
        return type;
    }

    @Override
    public String toString() {
        return "null";
    }

    /**
     * Set the Parameter of the given PreparedStatement at index <i>i</i> to the value in this object.
     *
     * @param pstmt The prepared statement whose parameter is to be set.
     * @param i     The index of the parameter.
     */
    @Override
    void setParameter(PreparedStatement pstmt, int i) throws SQLException {
        pstmt.setNull(i, type);
    }
}
