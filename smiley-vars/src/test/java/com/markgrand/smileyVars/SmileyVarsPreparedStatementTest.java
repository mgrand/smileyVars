package com.markgrand.smileyVars;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class SmileyVarsPreparedStatementTest {
    private Connection h2Connection;

    @BeforeEach
    void setUp() throws Exception {
        h2Connection =  DriverManager.getConnection("jdbc:h2:mem:test", "sa", "");
        Statement stmt = h2Connection.createStatement();
        stmt.execute("CREATE TABLE SQUARE (X INT PRIMARY KEY, Y INT)");
        stmt.execute("INSERT INTO SQUARE (X,Y) VALUES (1,1);");
        stmt.execute("INSERT INTO SQUARE (X,Y) VALUES (2,4);");
        stmt.execute("INSERT INTO SQUARE (X,Y) VALUES (3,9);");
        stmt.execute("INSERT INTO SQUARE (X,Y) VALUES (4,16);");
        h2Connection.commit();
        stmt.close();
    }

    @AfterEach
    void tearDown() throws Exception {
        h2Connection.close();
    }

    @Test
    void connectionConstructor() throws Exception {
        SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "select x,y from square");
        svps.close();
    }

    @Test
    void executeQuery() throws Exception {
        SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "select x,y from square");
        ResultSet rs = svps.executeQuery();
        assertNotNull(rs);
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
        assertEquals(1, rs.getInt("y"));
        assertTrue(rs.next());
        assertEquals(2, rs.getInt("x"));
        assertEquals(4, rs.getInt(2));
        assertTrue(rs.next());
        assertEquals(3, rs.getInt("x"));
        assertEquals(9, rs.getInt(2));
        assertTrue(rs.next());
        assertEquals(4, rs.getInt("x"));
        assertEquals(16, rs.getInt(2));
        assertFalse(rs.next());
    }

    @Ignore
    @Test
    void executeUpdate() {
    }

    @Ignore
    @Test
    void setNull() {
    }

    @Ignore
    @Test
    void setBoolean() {
    }

    @Ignore
    @Test
    void setByte() {
    }

    @Ignore
    @Test
    void setShort() {
    }

    @Ignore
    @Test
    void setInt() {
    }

    @Ignore
    @Test
    void setLong() {
    }

    @Ignore
    @Test
    void setFloat() {
    }

    @Ignore
    @Test
    void setDouble() {
    }

    @Ignore
    @Test
    void setBigDecimal() {
    }

    @Ignore
    @Test
    void setString() {
    }

    @Ignore
    @Test
    void setBytes() {
    }

    @Ignore
    @Test
    void setDate() {
    }

    @Ignore
    @Test
    void setTime() {
    }

    @Ignore
    @Test
    void setTimestamp() {
    }

    @Ignore
    @Test
    void setAsciiStream() {
    }

    @Ignore
    @Test
    void setBinaryStream() {
    }

    @Ignore
    @Test
    void setObject() {
    }

    @Ignore
    @Test
    void testSetObject() {
    }

    @Ignore
    @Test
    void execute() {
    }

    @Ignore
    @Test
    void setCharacterStream() {
    }

    @Ignore
    @Test
    void setRef() {
    }

    @Ignore
    @Test
    void setBlob() {
    }

    @Ignore
    @Test
    void setClob() {
    }

    @Test
    @Ignore
    void getMetaData() {
    }

    @Ignore
    @Test
    void setArray() {
    }

    @Ignore
    @Test
    void testSetDate() {
    }

    @Ignore
    @Test
    void testSetTime() {
    }

    @Ignore
    @Test
    void testSetTimestamp() {
    }

    @Ignore
    @Test
    void testSetNull() {
    }

    @Ignore
    @Test
    void setURL() {
    }

    @Ignore
    @Test
    void getParameterMetaData() {
    }

    @Ignore
    @Test
    void setRowId() {
    }

    @Ignore
    @Test
    void setNString() {
    }

    @Ignore
    @Test
    void setNCharacterStream() {
    }

    @Ignore
    @Test
    void setNClob() {
    }

    @Ignore
    @Test
    void testSetClob() {
    }

    @Ignore
    @Test
    void testSetBlob() {
    }

    @Ignore
    @Test
    void testSetNClob() {
    }

    @Ignore
    @Test
    void setSQLXML() {
    }

    @Ignore
    @Test
    void testSetObject1() {
    }

    @Ignore
    @Test
    void testSetAsciiStream() {
    }

    @Ignore
    @Test
    void testSetBinaryStream() {
    }

    @Ignore
    @Test
    void testSetCharacterStream() {
    }

    @Ignore
    @Test
    void testSetAsciiStream1() {
    }

    @Ignore
    @Test
    void testSetBinaryStream1() {
    }

    @Ignore
    @Test
    void testSetCharacterStream1() {
    }

    @Ignore
    @Test
    void testSetNCharacterStream() {
    }

    @Ignore
    @Test
    void testSetClob1() {
    }

    @Ignore
    @Test
    void testSetBlob1() {
    }

    @Ignore
    @Test
    void testSetNClob1() {
    }

    @Ignore
    @Test
    void testExecuteQuery() {
    }

    @Ignore
    @Test
    void testExecuteUpdate() {
    }

    @Ignore
    @Test
    void close() {
    }

    @Ignore
    @Test
    void clearParameters() {
    }

    @Ignore
    @Test
    void deepClearParameters() {
    }

    @Ignore
    @Test
    void getMaxFieldSize() {
    }

    @Ignore
    @Test
    void setMaxFieldSize() {
    }

    @Ignore
    @Test
    void getMaxRows() {
    }

    @Ignore
    @Test
    void setMaxRows() {
    }

    @Ignore
    @Test
    void getQueryTimeout() {
    }

    @Ignore
    @Test
    void setQueryTimeout() {
    }

    @Ignore
    @Test
    void getWarnings() {
    }

    @Ignore
    @Test
    void clearWarnings() {
    }

    @Ignore
    @Test
    void setCursorName() {
    }

    @Ignore
    @Test
    void testExecute() {
    }

    @Ignore
    @Test
    void getResultSet() {
    }

    @Ignore
    @Test
    void getUpdateCount() {
    }

    @Ignore
    @Test
    void getMoreResults() {
    }

    @Ignore
    @Test
    void getFetchDirection() {
    }

    @Ignore
    @Test
    void setFetchDirection() {
    }

    @Ignore
    @Test
    void getFetchSize() {
    }

    @Ignore
    @Test
    void setFetchSize() {
    }

    @Ignore
    @Test
    void getResultSetConcurrency() {
    }

    @Ignore
    @Test
    void getResultSetType() {
    }

    @Ignore
    @Test
    void getConnection() {
    }

    @Ignore
    @Test
    void testGetMoreResults() {
    }

    @Ignore
    @Test
    void getGeneratedKeys() {
    }

    @Ignore
    @Test
    void testExecuteUpdate1() {
    }

    @Ignore
    @Test
    void testExecuteUpdate2() {
    }

    @Ignore
    @Test
    void testExecuteUpdate3() {
    }

    @Ignore
    @Test
    void testExecute1() {
    }

    @Ignore
    @Test
    void testExecute2() {
    }

    @Ignore
    @Test
    void testExecute3() {
    }

    @Ignore
    @Test
    void getResultSetHoldability() {
    }

    @Ignore
    @Test
    void isClosed() {
    }

    @Ignore
    @Test
    void isPoolable() {
    }

    @Ignore
    @Test
    void setPoolable() {
    }

    @Ignore
    @Test
    void getLargeUpdateCount() {
    }

    @Ignore
    @Test
    void getLargeMaxRows() {
    }

    @Ignore
    @Test
    void setLargeMaxRows() {
    }

    @Ignore
    @Test
    void executeLargeUpdate() {
    }

    @Ignore
    @Test
    void testExecuteLargeUpdate() {
    }

    @Ignore
    @Test
    void testExecuteLargeUpdate1() {
    }

    @Ignore
    @Test
    void testExecuteLargeUpdate2() {
    }

    @Ignore
    @Test
    void getPreparedStatement() {
    }
}