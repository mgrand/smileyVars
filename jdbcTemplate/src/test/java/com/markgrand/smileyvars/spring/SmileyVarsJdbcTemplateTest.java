package com.markgrand.smileyvars.spring;

import com.mockrunner.mock.jdbc.MockDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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
        assertTrue(svjt.isLazyInit());
    }

    @Test
    void constructorDataSource() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        assertEquals(mockDataSource, svjt.getDataSource());
        assertTrue(svjt.isLazyInit());
    }

    @Test
    void constructorDataSourceLazy() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource, true);
        assertEquals(mockDataSource, svjt.getDataSource());
        assertTrue(svjt.isLazyInit());
    }

    @Test
    void constructorDataSourceNotLazy() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource, false);
        assertEquals(mockDataSource, svjt.getDataSource());
        assertFalse(svjt.isLazyInit());
    }

    @Test
    void withSmileyVarsPreparedStatement() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        assertEquals(9, (int)svjt.withSmileyVarsPreparedStatement("SELECT Y from SQUARE WHERE x = :x", svps -> {
            try (ResultSet rs = svps.setInt("x", 3).executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt("y");
            }
        }));
    }

    @Test
    void withSmileyVarsPreparedStatementNoDatasource() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate();
        assertThrows(IllegalStateException.class,
                ()-> svjt.withSmileyVarsPreparedStatement("SELECT Y from SQUARE WHERE x = :x", svps -> 9));
    }
}