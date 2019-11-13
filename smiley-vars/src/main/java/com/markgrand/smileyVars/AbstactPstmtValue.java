package com.markgrand.smileyVars;

import java.sql.PreparedStatement;

/**
 * Abstract class for objects that capture parameter values for prepared statements.
 *
 * @author Mark Grand
 */
abstract class AbstactPstmtValue {
    /**
     * Set the Parameter of the given PreparedStatement at index <i>i</i> to the value in this object.
     *
     * @param pstmt The prepared statement whose parameter is to be set.
     * @param i The index of the parameter.
     */
    abstract void setParameter(PreparedStatement pstmt, int i) ;
}
