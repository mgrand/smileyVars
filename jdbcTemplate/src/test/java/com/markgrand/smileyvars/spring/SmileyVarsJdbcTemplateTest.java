package com.markgrand.smileyvars.spring;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmileyVarsJdbcTemplateTest {

    @Test
    void simpleConstructor() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate();
        assertNull(svjt.getDataSource());
    }

    @Test
    void withSmileyVarsPreparedStatement() {
    }

    @Test
    void withSmileyVarsPreparedStatementNoDatasource() {
    }
}