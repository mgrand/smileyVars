package com.markgrand.smileyVars;

import java.sql.SQLException;

/**
 * Class used to represent a null value for a prepared statement parameter
 */
class NullValue extends AbstractPreparedStatementValue {
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

    @Override
    public String toString() {
        return "null";
    }

}
