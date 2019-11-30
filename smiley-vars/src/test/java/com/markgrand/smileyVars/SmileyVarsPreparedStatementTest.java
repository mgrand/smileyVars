package com.markgrand.smileyVars;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;

class SmileyVarsPreparedStatementTest {
    private Connection h2Connection;

    @BeforeEach
    void setUp() throws Exception {
        h2Connection = DriverManager.getConnection("jdbc:h2:mem:test", "sa", "");
        Statement stmt = h2Connection.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS SQUARE (X INT PRIMARY KEY, Y INT)");
        stmt.execute("INSERT INTO SQUARE (X,Y) VALUES (-3,9);");
        stmt.execute("INSERT INTO SQUARE (X,Y) VALUES (-2,4);");
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
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "select x,y from square WHERE x > 0")) {
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
    }

    @Test
    void executeUpdate() throws Exception {
        SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "INSERT INTO SQUARE (X,Y) VALUES ( :x,:y);");
        svps.setInt("x", -1);
        svps.setInt("y", 1);
        assertEquals(1, svps.executeUpdate());
        Statement stmt = h2Connection.createStatement();
        ResultSet rs = stmt.executeQuery("Select y from square where x=-1");
        assertTrue(rs.next());
        assertEquals(1, rs.getInt("y"));
        stmt.close();
        h2Connection.rollback();
    }

    @Test
    void setNull() throws SQLException {
        SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "INSERT INTO SQUARE (X,Y) VALUES ( 0,:y)");
        svps.setNull("y", Types.INTEGER);
        assertEquals(1, svps.executeUpdate());
        Statement stmt = h2Connection.createStatement();
        ResultSet rs = stmt.executeQuery("Select y from square where x=0");
        assertTrue(rs.next());
        assertEquals(0, rs.getInt("y"));
        assertTrue(rs.wasNull());
        stmt.close();
        h2Connection.rollback();
    }

    @Test
    void testSetNull() throws Exception {
        SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "INSERT INTO SQUARE (X,Y) VALUES ( 0,:y)");
        svps.setNull("y", Types.INTEGER, "INT");
        assertEquals(1, svps.executeUpdate());
        Statement stmt = h2Connection.createStatement();
        ResultSet rs = stmt.executeQuery("Select y from square where x=0");
        assertTrue(rs.next());
        assertEquals(0, rs.getInt("y"));
        assertTrue(rs.wasNull());
        stmt.close();
        h2Connection.rollback();
    }

    @Test
    void setBooleanTrue() throws SQLException {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setBoolean("x", true);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getBoolean(1));
        }
    }

    @Test
    void setBooleanFalse() throws SQLException {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setBoolean("x", false);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertFalse(rs.getBoolean(1));
        }
    }

    @Test
    void notSet() throws SQLException {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            assertThrows(UnboundVariableException.class, svps::executeQuery);
        }
    }

    @Test
    void setByte() throws SQLException {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setByte("x", (byte) 25);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals((byte) 25, rs.getByte(1));
        }
    }

    @Test
    void setShort() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setShort("x", (short) 23456);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals((short) 23456, rs.getShort(1));
        }
    }

    @Test
    void setInt() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setInt("x", -98723456);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals(-98723456, rs.getInt(1));
        }
    }

    @Test
    void setLong() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setLong("x", -92847568723456L);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals(-92847568723456L, rs.getLong(1));
        }
    }

    @Test
    void wrongName() {
        assertThrows(SQLException.class, () -> {
            try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
                svps.setLong("bogus", -92847568723456L);
            }
        });
    }

    @Test
    void setWhileClosed() throws Exception {
        SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x");
        svps.close();
        assertThrows(SQLException.class, () -> svps.setLong("x", -92847568723456L));
    }

    @Test
    void setFloat() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setFloat("x", -234.125F);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals(-234.125F, rs.getFloat(1));
        }
    }

    @Test
    void setDouble() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setDouble("x", -1234.125);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals(-1234.125, rs.getDouble(1));
        }
    }

    @Test
    void setBigDecimal() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setBigDecimal("x", BigDecimal.valueOf(1234567890));
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals(BigDecimal.valueOf(1234567890), rs.getBigDecimal(1));
        }
    }

    @Test
    void setNullString() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setString("x", null);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertNull(rs.getString(1));
        }
    }

    @Test
    void setString() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setString("x", "Foobar");
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Foobar", rs.getString(1));
        }
    }

    @Test
    void setBytes() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setBytes("x", new byte[]{3, 27, (byte) 0xf3});
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertArrayEquals(new byte[]{3, 27, (byte) 0xf3}, rs.getBytes(1));
        }
    }

    @Test
    void setBytesNull() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setBytes("x", null);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertNull(rs.getBytes(1));
        }
    }

    @Test
    void setDate() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setDate("x", new Date(123397200000L));
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals(new Date(123397200000L), rs.getDate(1));
        }
    }

    @Test
    void testSetDate() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setDate("x", new Date(123397200000L), new GregorianCalendar());
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals(new Date(123397200000L), rs.getDate(1));
        }
    }

    @Test
    void setTime() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setTime("x", new Time(77589000));
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals(new Time(77589000), rs.getTime(1));
        }
    }

    @Test
    void testSetTime() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setTime("x", new Time(77589000), new GregorianCalendar());
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals(new Time(77589000), rs.getTime(1));
        }
    }

    @Test
    void setTimestamp() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setTimestamp("x", new Timestamp(77589000));
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals(new Timestamp(77589000), rs.getTimestamp(1));
        }
    }

    @Test
    void testSetTimestamp() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setTimestamp("x", new Timestamp(77589000), new GregorianCalendar());
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals(new Timestamp(77589000), rs.getTimestamp(1));
        }
    }

    @Test
    void setAsciiStream() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setAsciiStream("x", new ByteArrayInputStream("fubar".getBytes()));
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertArrayEquals(inputStreamToBytes(new ByteArrayInputStream("fubar".getBytes()), 5),
                    inputStreamToBytes(rs.getAsciiStream(1), 5));
        }
    }

    @Test
    void setAsciiStreamIntLength() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setAsciiStream("x", new ByteArrayInputStream("fubar".getBytes()), 5);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertArrayEquals(inputStreamToBytes(new ByteArrayInputStream("fubar".getBytes()), 5),
                    inputStreamToBytes(rs.getAsciiStream(1), 5));
        }
    }

    @Test
    void setAsciiStreamLongLength() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setAsciiStream("x", new ByteArrayInputStream("fubar".getBytes()), 5L);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertArrayEquals(inputStreamToBytes(new ByteArrayInputStream("fubar".getBytes()), 5),
                    inputStreamToBytes(rs.getAsciiStream(1), 5));
        }
    }

    private byte[] inputStreamToBytes(InputStream inputStream, int length) throws IOException {
        byte[] bytes = new byte[length];
        assertEquals(length, inputStream.read(bytes));
        return bytes;
    }

    @Test
    void setBinaryStream() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setBinaryStream("x", new ByteArrayInputStream(new byte[]{6, 32, 44}));
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertArrayEquals(inputStreamToBytes(new ByteArrayInputStream(new byte[]{6, 32, 44}), 3),
                    inputStreamToBytes(rs.getBinaryStream(1), 3));
        }
    }

    @Test
    void setBinaryStreamIntLength() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setBinaryStream("x", new ByteArrayInputStream(new byte[]{6, 32, 44}), 3);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertArrayEquals(inputStreamToBytes(new ByteArrayInputStream(new byte[]{6, 32, 44}), 3),
                    inputStreamToBytes(rs.getBinaryStream(1), 3));
        }
    }

    @Test
    void setBinaryStreamLongLength() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setBinaryStream("x", new ByteArrayInputStream(new byte[]{6, 32, 44}), 3L);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertArrayEquals(inputStreamToBytes(new ByteArrayInputStream(new byte[]{6, 32, 44}), 3),
                    inputStreamToBytes(rs.getBinaryStream(1), 3));
        }
    }

    @Test
    void setObject() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setObject("x", new Rectangle(5,7));
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals(new Rectangle(5,7), rs.getObject(1));
        }
    }

    @Test
    void setObjectWithType() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setObject("x", new Rectangle(5,7), Types.OTHER);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals(new Rectangle(5,7), rs.getObject(1));
        }
    }

    @Test
    void setObjectWithTypeAndScale() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setObject("x", new BigDecimal("3.14159265"), Types.DECIMAL, 9);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals(new BigDecimal("3.14159265"), rs.getObject(1));
        }
    }

    @Test
    void execute() throws Exception {
        try (SmileyVarsPreparedStatement svps
                     = new SmileyVarsPreparedStatement(h2Connection, "SELECT x,y FROM square WHERE 1=1 (: AND x=:x:)(: AND y=:y :)")) {
            assertTrue(svps.execute());
            ResultSet rs = svps.getResultSet();
            assertHasRows(rs, 6);
            rs.close();
            svps.setInt("y", 9);
            svps.execute();
            rs = svps.getResultSet();
            assertHasRows(rs, 2);
            assertFalse(rs.next());
            rs.close();
            svps.setInt("x", 3);
            svps.execute();
            rs = svps.getResultSet();
            assertTrue(rs.next());
            assertFalse(rs.next());
            rs.close();
            svps.clearParameter("x");
            svps.execute();
            rs = svps.getResultSet();
            assertHasRows(rs, 2);
            assertFalse(rs.next());
            rs.close();
        }
    }

    private void assertHasRows(ResultSet rs, int rowCount) throws SQLException {
        for (int i = rowCount; i > 0; i--) {
            assertTrue(rs.next());
        }
    }

    @Test
    void clearParameter() throws Exception {
        try (SmileyVarsPreparedStatement svps
                     = new SmileyVarsPreparedStatement(h2Connection, "SELECT x,y FROM square WHERE 1=1 (: AND x=:x:)(: AND y=:y :)")) {
            assertFalse(svps.clearParameter("bogus"));
            assertTrue(svps.clearParameter("y"));
            svps.setInt("y", 9);
            assertTrue(svps.clearParameter("y"));
            ResultSet rs = svps.executeQuery();
            assertHasRows(rs, 6);
            rs.close();
        }
    }

    @Test
    void setCharacterStream() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setCharacterStream("x", new StringReader("fubar"));
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals("fubar", new BufferedReader(rs.getCharacterStream(1)).readLine());
        }
    }

    @Test
    void setCharacterStreamIntLength() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setCharacterStream("x", new StringReader("fubar"), 5);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals("fubar", new BufferedReader(rs.getCharacterStream(1)).readLine());
        }
    }

    @Test
    void setCharacterStreamLongLength() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setCharacterStream("x", new StringReader("fubar"), 5L);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals("fubar", new BufferedReader(rs.getCharacterStream(1)).readLine());
        }
    }

    @Disabled
    @Test
    void setRef() {
        // H2 does not support Ref, so need to find a different way to unit test.
    }

    @Test
    void setBlob() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setBlob("x", new SerialBlob("fubar".getBytes()));
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertArrayEquals("fubar".getBytes(), rs.getBlob(1).getBytes(0, 5));
        }
    }

    @Test
    void setBlobInputstream() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setBlob("x", new ByteArrayInputStream("fubar".getBytes()));
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertArrayEquals("fubar".getBytes(), rs.getBlob(1).getBytes(1, 5));
        }
    }

    @Test
    void setBlobInputstreamLength() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setBlob("x", new ByteArrayInputStream("fubar".getBytes()), 22);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertArrayEquals("fubar".getBytes(), rs.getBlob(1).getBytes(1, 22));
        }
    }

    @Test
    void setClob() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setClob("x", new SerialClob(new char[]{'f', 'u', 'b', 'a', 'r'}));
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals("fubar", rs.getClob(1).getSubString(1, 5));
        }
    }

    @Test
    void setClobReader() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setClob("x", new StringReader("fubar"));
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals("fubar", rs.getClob(1).getSubString(1, 5));
        }
    }

    @Test
    void setClobReaderLength() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setClob("x", new StringReader("fubar"), 22);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals("fubar", rs.getClob(1).getSubString(1, 5));
        }
    }

    @Test
    void getMetaData() throws Exception {
        try (SmileyVarsPreparedStatement svps
                     = new SmileyVarsPreparedStatement(h2Connection, "SELECT x,y FROM square WHERE 1=1 (: AND x=:x:)(: AND y=:y :)")) {
            svps.setInt("y", 9);
            ResultSetMetaData resultSetMetaData = svps.getMetaData();
            assertNotNull(resultSetMetaData);
        }
    }

    @Test
    void setArray() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setArray("x", h2Connection.createArrayOf("INT", new String[]{"two", "four", "six", "eight"}));
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertArrayEquals(new String[]{"two", "four", "six", "eight"}, (String[])rs.getArray(1).getArray());
        }
    }

    @Disabled
    @Test
    void setURL() {
        // Not spported by H2
    }

    @Test
    void getParameterMetaData() throws Exception {
        try (SmileyVarsPreparedStatement svps
                     = new SmileyVarsPreparedStatement(h2Connection, "SELECT x,y FROM square WHERE 1=1 (: AND x=:x:)(: AND y=:y :)")) {
            ParameterMetaData parameterMetaData = svps.getParameterMetaData();
            assertNotNull(parameterMetaData);
        }
    }

    @Disabled
    @Test
    void setRowId() {
        // Not supported by H2
    }

    @Test
    void setNString() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setNString("x", "Grünstraße");
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Grünstraße", rs.getNString(1));
        }
    }

    @Test
    void setNCharacterStream() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setNCharacterStream("x", new StringReader("Grünstraße"));
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Grünstraße", new BufferedReader(rs.getNCharacterStream(1)).readLine());
        }
    }

    @Test
    void setNCharacterStreamLength() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setNCharacterStream("x", new StringReader("Grünstraße"), 22);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Grünstraße", new BufferedReader(rs.getNCharacterStream(1)).readLine());
        }
    }

    @Test
    void setNClob() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            NClob nClob = h2Connection.createNClob();
            nClob.setString(1,"Grünstraße");
            svps.setNClob("x", nClob);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Grünstraße", rs.getNClob(1).getSubString(1, 22));
        }
    }

    @Test
    void setNClobReader() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setNClob("x", new StringReader("Grünstraße"));
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Grünstraße", rs.getNClob(1).getSubString(1, 22));
        }
    }

    @Test
    void setNClobReaderLength() throws Exception {
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setNClob("x", new StringReader("Grünstraße"), 22);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Grünstraße", rs.getNClob(1).getSubString(1, 22));
        }
    }

    @Test
    void setSQLXML() throws Exception {
        SQLXML sqlxml = h2Connection.createSQLXML();
        sqlxml.setString("<fubar/>");
        try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(h2Connection, "SELECT :x")) {
            svps.setSQLXML("x", sqlxml);
            ResultSet rs = svps.executeQuery();
            assertTrue(rs.next());
            assertEquals("<fubar/>", rs.getSQLXML(1).getString());
        }
    }

    @Test
    void close() throws Exception {
        SmileyVarsPreparedStatement svps
                = new SmileyVarsPreparedStatement(h2Connection, "SELECT x,y FROM square WHERE 1=1 (: AND x=:x:)(: AND y=:y :)");
        svps.setInt("x", -1);
        PreparedStatement pstmt1 = svps.getPreparedStatement();
        svps.setInt("y", 1);
        PreparedStatement pstmt2 = svps.getPreparedStatement();
        svps.close();
        assertTrue(svps.isClosed());
        assertTrue(pstmt1.isClosed());
        assertTrue(pstmt2.isClosed());
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