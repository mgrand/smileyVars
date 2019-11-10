package com.markgrand.smileyVars;

/**
 * Singleton class used to represent a null value for variables that have a null value.
 */
class NullValue {
    static final NullValue INSTANCE = new NullValue();

    private NullValue() {}
}
