package com.markgrand.smileyvars.spring;

import com.mockrunner.mock.jdbc.MockDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class SmileyVarsJdbcTemplateTest {
    private Connection h2Connection;
    private MockDataSource mockDataSource;

    @BeforeEach
    void setUp() throws Exception {
        h2Connection = DriverManager.getConnection("jdbc:h2:mem:test", "sa", "");
        Statement stmt = h2Connection.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS SQUARE (X INT PRIMARY KEY, Y INT, COMNT VARCHAR(400))");
        stmt.execute("INSERT INTO SQUARE (X,Y) VALUES (-3,9);");
        stmt.execute("INSERT INTO SQUARE (X,Y) VALUES (-2,4);");
        stmt.execute("INSERT INTO SQUARE (X,Y) VALUES (1,1);");
        stmt.execute("INSERT INTO SQUARE (X,Y) VALUES (2,4);");
        stmt.execute("INSERT INTO SQUARE (X,Y) VALUES (3,9);");
        stmt.execute("INSERT INTO SQUARE (X,Y) VALUES (4,16);");
        h2Connection.commit();
        stmt.close();
        mockDataSource = new MockDataSource();
        mockDataSource.setupConnection(h2Connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        h2Connection.close();
    }

    @Test
    void simpleConstructor() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate();
        assertNull(svjt.getDataSource());
        svjt.setDataSource(mockDataSource);
        assertEquals(mockDataSource, svjt.getDataSource());
    }

    @Test
    void constructorDataSource() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        assertEquals(mockDataSource, svjt.getDataSource());
    }

    @Test
    void withSmileyVarsPreparedStatement() {
    }

    @Test
    void withSmileyVarsPreparedStatementNoDatasource() {
    }
}