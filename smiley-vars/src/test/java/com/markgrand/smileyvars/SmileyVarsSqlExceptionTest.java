package com.markgrand.smileyvars;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class SmileyVarsSqlExceptionTest {

    @Test
    void constructor() {
        assertThrows(SmileyVarsSqlException.class, () -> {
            throw new SmileyVarsSqlException("fubar", new SQLException());
        });
    }
}