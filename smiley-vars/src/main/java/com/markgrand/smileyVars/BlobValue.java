package com.markgrand.smileyVars;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Class to represent a Blob value that will be used to set the value of prepared statement parameter.
 *
 * @author Mark Grand
 */
class BlobValue extends AbstactPreparedStatementValue {
    private final Blob blob;
    private final InputStream in;
    private final Long length;

    /**
     * Constructor
     *
     * @param blob The value that this object will be used to set in a prepared statement.
     */
    BlobValue(@NotNull Blob blob) {
        this(blob, null, null);
    }

    /**
     * Constructor
     *
     * @param in An inputstream to read the value of the blob.
     */
    BlobValue(InputStream in) {
        this(null, in, null);
    }

    /**
     * Constructor
     *
     * @param in An inputstream to read the value of the blob.
     * @param length The length of the blob.
     */
    BlobValue(InputStream in, Long length) {
        this(null, in , length);
    }

    /**
     * Constructor
     *
     * @param blob The value that this object will be used to set in a prepared statement.
     * @param in An inputstream to read the value of the blob.
     * @param length The length of the blob.
     */
    private BlobValue(Blob blob, InputStream in, Long length) {
        this.blob = blob;
        this.in = in;
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
        if (blob != null) {
            pstmt.setBlob(i, blob);
        } else if (length == null) {
            pstmt.setBlob(i, in);
        } else {
            pstmt.setBlob(i, in, length);
        }
    }
}
