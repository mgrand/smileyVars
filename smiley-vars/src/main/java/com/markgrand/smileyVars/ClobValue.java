package com.markgrand.smileyVars;

import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Class to represent a Clob value that will be used to set the value of prepared statement parameter.
 *
 * @author Mark Grand
 */
class ClobValue extends AbstactPreparedStatementValue {
    private final Clob clob;
    private final Reader reader;
    private final Long length;

    /**
     * Constructor
     *
     * @param clob The value that this object will be used to set in a prepared statement.
     */
    ClobValue(@NotNull Clob clob) {
        this(clob, null, null);
    }

    /**
     * Constructor
     *
     * @param reader A Reader to read the value of the blob.
     */
    ClobValue(@NotNull Reader reader) {
        this(null, reader, null);
    }

    /**
     * Constructor
     *
     * @param reader A Reader to read the value of the blob.
     * @param length The length of the blob.
     */
    ClobValue(@NotNull Reader reader, @NotNull Long length) {
        this(null, reader, length);
    }

    /**
     * Constructor
     *
     * @param clob The value that this object will be used to set in a prepared statement.
     * @param reader A Reader to read the value of the blob.
     * @param length The length of the blob.
     */
    private ClobValue(Clob clob, Reader reader, Long length) {
        this.clob = clob;
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
        if (clob != null) {
            pstmt.setClob(i, clob);
        } else if (length == null) {
            pstmt.setClob(i, reader);
        } else {
            pstmt.setClob(i, reader, length);
        }
    }
}
