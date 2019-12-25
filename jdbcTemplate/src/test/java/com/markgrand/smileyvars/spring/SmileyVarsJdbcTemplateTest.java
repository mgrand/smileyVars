package com.markgrand.smileyvars.spring;

import com.markgrand.smileyvars.SmileyVarsPreparedStatement;
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
        stmt.execute("CREATE TABLE IF NOT EXISTS inventory ("
                             + "aisle INT,"
                             + "level INT,"
                             + "bin_number INT,"
                             + "item_number VARCHAR(100),"
                             + "quantity INT,"
                             + "CONSTRAINT inventory_pk PRIMARY KEY (aisle, level, bin_number)"
                             + ")");
        stmt.execute("INSERT INTO inventory (aisle, level, bin_number, item_number, quantity) VALUES (4, 1, 7, 'M234', 22);");
        stmt.execute("INSERT INTO inventory (aisle, level, bin_number, item_number, quantity) VALUES (4, 1, 8, 'M8473', 31);");
        stmt.execute("INSERT INTO inventory (aisle, level, bin_number, item_number, quantity) VALUES (4, 1, 9, 'M8479', 18);");
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
        assertEquals(22, (int) svjt.withSmileyVars("SELECT quantity FROM inventory WHERE aisle = :aisle AND level = :level AND bin_number = :bin_number",
                svps -> {
                    try (ResultSet rs = svps.setInt("aisle", 4).setInt("level", 1). setInt("bin_number", 7).executeQuery()) {
                        assertTrue(rs.next());
                        return rs.getInt("quantity");
                    }
                }));
    }

    @Test
    void withSmileyVarsPreparedStatementNoDatasource() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate();
        assertThrows(IllegalStateException.class,
                () -> svjt.withSmileyVars("SELECT Y from SQUARE WHERE x = :x", (SmileyVarsPreparedStatement svps) -> 9));
    }
}