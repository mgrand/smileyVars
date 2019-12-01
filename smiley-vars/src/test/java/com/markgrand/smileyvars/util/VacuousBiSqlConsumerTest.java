package com.markgrand.smileyvars.util;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class VacuousBiSqlConsumerTest {

    @Test
    void getInstance() {
        VacuousBiSqlConsumer<String, String> vbsc = VacuousBiSqlConsumer.getInstance();
        assertNotNull(vbsc);
    }

    @Test
    void accept() {
        assertThrows(SQLException.class, () -> VacuousBiSqlConsumer.getInstance().accept("", ""));
    }

    @Test
    void isVacuous() {
        assertTrue(VacuousBiSqlConsumer.getInstance().isVacuous());
    }
}