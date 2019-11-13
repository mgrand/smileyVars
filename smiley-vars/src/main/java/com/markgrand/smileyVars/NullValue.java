package com.markgrand.smileyVars;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Class used to represent a null value for a prepared statement parameter
 */
class NullValue extends AbstactPstmtValue {
    private int type;

    /**
     * Constructor
     *
     * @param type A type constant from {@link java.sql.Types}.
     */
    NullValue(int type) {
        this.type = type;
    }

    /**
     * Return the type associated with this object, which should be one of the values defined in {@link
     * java.sql.Types}.
     *
     * @return
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
