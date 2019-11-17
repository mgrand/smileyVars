package com.markgrand.smileyVars;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Class used to represent a null value for a prepared statement parameter
 */
class NullValue extends AbstactPreparedStatementValue {
    private final int type;
    private final String typeName;

    /**
     * Constructor
     *
     * @param type A type constant from {@link java.sql.Types}.
     * @throws SmileyVarsSqlException if the given types is not a value defined in java.sql.Types.
     */
    NullValue(int type) throws SQLException {
        this(type, null);
    }

    /**
     * Constructor
     *
     * @param type     A type constant from {@link java.sql.Types}.
     * @param typeName the fully-qualified name of an SQL user-defined type; ignored if the parameter is not a
     *                 user-defined type or REF
     * @throws SmileyVarsSqlException if the given types is not a value defined in java.sql.Types.
     */
    NullValue(int type, String typeName) throws SQLException {
        this.type = type;
        checkType(type);
        this.typeName = typeName;
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
        if (typeName == null) {
            pstmt.setNull(i, type);
        } else {
            pstmt.setNull(i, type, typeName);
        }
    }
}
