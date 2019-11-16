package com.markgrand.smileyVars;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Abstract class for objects that capture parameter values for prepared statements.
 *
 * @author Mark Grand
 */
abstract class AbstactPreparedStatementValue {
    void checkType(Integer type) throws SQLException {
        if (type != null) {
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
                    throw new SmileyVarsSqlException("Invalid type code that is not a java.sql.Types code: " + type);
            }
        }
    }

    /**
     * Set the Parameter of the given PreparedStatement at index <i>i</i> to the value in this object.
     *
     * @param pstmt The prepared statement whose parameter is to be set.
     * @param i The index of the parameter.
     * @throws SQLException if there is a problem.
     */
    abstract void setParameter(PreparedStatement pstmt, int i) throws SQLException;
}
