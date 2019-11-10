package com.markgrand.smileyVars;

/**
 * Singleton class used to represent a null value for variables that have a null value. This is useful for setting
 * parameters of PreparedStatement and CallableStatement objects, because we need to know the type of null value.
 */
class NullValue {
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
}
