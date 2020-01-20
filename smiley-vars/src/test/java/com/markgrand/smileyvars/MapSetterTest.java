package com.markgrand.smileyvars;

import com.markgrand.smileyvars.util.BiSqlConsumer;
import com.mockrunner.mock.jdbc.MockArray;
import com.mockrunner.mock.jdbc.MockBlob;
import com.mockrunner.mock.jdbc.MockClob;
import com.mockrunner.mock.jdbc.MockNClob;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapSetterTest {

    @Test
    void setSmileyVars(@Mocked SmileyVarsPreparedStatement svps) throws Exception {
        Map<String, BiSqlConsumer<SmileyVarsPreparedStatement, Object>> setterMap = new HashMap<>();
        setterMap.put("quantity", (ps, value) -> ps.setInt("quantity", (int) value));
        setterMap.put("description", (ps, value) -> ps.setString("description", (String) value));
        MapSetter mapSetter = new MapSetter(setterMap);

        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("quantity", 4);
        valueMap.put("description", "foobar");
        mapSetter.setSmileyVars(svps, valueMap);
        new Verifications() {{
            svps.clearParameters();
            times = 1;
            svps.setInt("quantity", 4);
            times = 1;
            svps.setString("description", "foobar");
            times = 1;
        }};
    }

    @Test
    void getSetterMap() {
        Map<String, BiSqlConsumer<SmileyVarsPreparedStatement, Object>> setterMap = new HashMap<>();
        setterMap.put("quantity", (ps, value) -> ps.setInt("quantity", (int) value));
        setterMap.put("description", (ps, value) -> ps.setString("description", (String) value));
        MapSetter mapSetter = new MapSetter(setterMap);
        assertEquals(setterMap, mapSetter.getSetterMap());
    }

    @Test
    void setSmileyVarsWrongName(@Mocked SmileyVarsPreparedStatement svps) {
        Map<String, BiSqlConsumer<SmileyVarsPreparedStatement, Object>> setterMap = new HashMap<>();
        setterMap.put("quantity", (ps, value) -> ps.setInt("quantity", (int) value));
        setterMap.put("description", (ps, value) -> ps.setString("description", (String) value));
        MapSetter mapSetter = new MapSetter(setterMap);

        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("qty", 4);
        valueMap.put("description", "foobar");
        assertThrows(SmileyVarsException.class, () -> mapSetter.setSmileyVars(svps, valueMap));
    }

    private Array mockArray = new MockArray(new int[]{0, 1, 2, 3});
    private Blob mockBlob = new MockBlob("Bogus".getBytes());
    private Calendar calendar = Calendar.getInstance();
    private Clob mockClob = new MockClob("eieio");
    private Date date = Date.valueOf(LocalDate.now());
    private NClob mockNClob = new MockNClob("Banjo");
    private Time time = Time.valueOf(LocalTime.now());
    private Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());

    @Test
    void builder(@Mocked SmileyVarsPreparedStatement svps) throws Exception {
        MapSetter mapSetter = MapSetter.newBuilder()
                                      .arrayVar("array")
                                      .bigDecimalVar("bigDecimal")
                                      .blobVar("blobvar")
                                      .booleanVar("boolean")
                                      .bytesVar("bytes")
                                      .byteVar("byte")
                                      .bytesVar("bytes")
                                      .clobVar("clob")
                                      .dateVar("date")
                                      .dateVar("calendarDate", calendar)
                                      .doubleVar("double")
                                      .floatVar("float")
                                      .intVar("int")
                                      .longVar("long")
                                      .nClob("nClob")
                                      .nStringVar("nString")
                                      .objectVar("object", Types.DECIMAL)
                                      .shortVar("short")
                                      .stringVar("string")
                                      .timestampVar("timestamp")
                                      .timestampVar("calendarTimestamp", calendar)
                                      .timeVar("time")
                                      .timeVar("calendarTime", calendar)
                                      .urlVar("url")
                                      .build();
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("array", mockArray);
        valueMap.put("bigDecimal", BigDecimal.valueOf(123));
        valueMap.put("blobvar", mockBlob);
        valueMap.put("boolean", true);
        valueMap.put("bytes", "BoGuS".getBytes());
        valueMap.put("byte", (byte)23);
        valueMap.put("clob", mockClob);
        valueMap.put("date", date);
        valueMap.put("calendarDate", date);
        valueMap.put("double", 123456.0625D);
        valueMap.put("float", 123.456F);
        valueMap.put("int", 19238);
        valueMap.put("long", 987654321L);
        valueMap.put("nClob", mockNClob);
        valueMap.put("nString", "nnnnnn");
        valueMap.put("object", 123456);
        valueMap.put("short", (short)12345);
        valueMap.put("string", "foobar");
        valueMap.put("timestamp", timestamp);
        valueMap.put("calendarTimestamp", timestamp);
        valueMap.put("time", time);
        valueMap.put("calendarTime", time);
        valueMap.put("url", new URL("http://example.com"));

        new Expectations() {{
            svps.setArray("array", mockArray);
            svps.setBigDecimal("bigDecimal", BigDecimal.valueOf(123));
            svps.setBlob("blobvar", mockBlob);
            svps.setBoolean("boolean", true);
            svps.setBytes("bytes", "BoGuS".getBytes());
            svps.setByte("byte", (byte)23);
            svps.setClob("clob", mockClob);
            svps.setDate("date", date);
            svps.setDate("calendarDate", date, calendar);
            svps.setDouble("double", 123456.0625D);
            svps.setFloat("float", 123.456F);
            svps.setInt("int", 19238);
            svps.setLong("long", 987654321L);
            svps.setNClob("nClob", mockNClob);
            svps.setNString("nString", "nnnnnn");
            svps.setObject("object", 123456, Types.DECIMAL);
            svps.setShort("short", (short)12345);
            svps.setString("string", "foobar");
            svps.setTimestamp("timestamp", timestamp);
            svps.setTimestamp("calendarTimestamp", timestamp, calendar);
            svps.setTime("time", time);
            svps.setTime("calendarTime", time, calendar);
            svps.setURL("url", new URL("http://example.com"));
        }};

        mapSetter.setSmileyVars(svps, valueMap);
    }
}