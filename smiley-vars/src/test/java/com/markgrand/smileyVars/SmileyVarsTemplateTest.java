package com.markgrand.smileyVars;

import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockDataSource;
import com.mockrunner.mock.jdbc.MockDatabaseMetaData;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SmileyVarsTemplateTest {
    @Test
    void empty() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.SQL_SERVER, "");
        assertEquals("", template.apply(new HashMap<>()));
    }

    @Test
    void emptyAllBracketed() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.SQL_SERVER, "(: foo :bar sadist :)");
        assertEquals("", template.apply(new HashMap<>()));
    }

    @Test
    void justClose() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.SQL_SERVER, ":)");
        assertEquals(":)", template.apply(new HashMap<>()));
    }

    @Test
    void ansiTemplate() {
        @NotNull @SuppressWarnings("deprecation")
        SmileyVarsTemplate template = SmileyVarsTemplate.ansiTemplate("Select * from foo where 1=1 (:and x=:x:)");
        assertEquals("Select * from foo where 1=1 ", template.apply(new HashMap<>()));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", 42);
        assertEquals("Select * from foo where 1=1 and x=42", template.apply(map));
    }

    @Test
    void preparedStatementAnsiTemplate() throws Exception {
        MockConnection conn = new MockConnection();
        conn.setMetaData(new MockDatabaseMetaData());
        @NotNull
        SmileyVarsTemplate template
                = SmileyVarsTemplate.template(conn, "Select * from foo where 1=1 (:and x=:x:) and y=:y", ValueFormatterRegistry.preparedStatementInstance());
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("y", "qwerty");
        assertEquals("Select * from foo where 1=1  and y=?", template.apply(map));
        map.put("x", 42);
        assertEquals("Select * from foo where 1=1 and x=? and y=?", template.apply(map));
    }

    @Test
    void ansiEmbeddedQuote() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI, "Select * from foo where 1=1 (:and x=:x:)");
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", "can't or won't");
        assertEquals("Select * from foo where 1=1 and x='can''t or won''t'", template.apply(map));
    }

    @Test
    void postgresqlTemplate() {
        //noinspection deprecation
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.postgresqlTemplate("Select * from foo where 1=1 (:and x=:x:)");
        assertEquals("Select * from foo where 1=1 ", template.apply(new HashMap<>()));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", 42);
        assertEquals("Select * from foo where 1=1 and x=42", template.apply(map));
    }

    @Test
    void oracleTemplate() {
        @NotNull @SuppressWarnings("deprecation")
        SmileyVarsTemplate template = SmileyVarsTemplate.oracleTemplate("Select * from foo where 1=1(: and x=:x:)(: and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and x='42' and y=39", template.apply(map));
    }

    @Test
    void twoValues() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE, "Select * from foo where 1=1(: and x=:x and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and x='42' and y=39", template.apply(map));
    }

    @Test
    void twoLeftValues() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(: and :x=x and :y=y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and '42'=x and 39=y", template.apply(map));
    }

    @Test
    void valueAndNoValue() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(: and x=:x and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        assertEquals("Select * from foo where 1=1", template.apply(map));
    }

    @Test
    void noValueAndValue() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(: and x=:x and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1", template.apply(map));
    }

    @Test
    void twoNoValue() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(: and x=:x and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        @NotNull Map<String, Object> map = new HashMap<>();
        assertEquals("Select * from foo where 1=1", template.apply(map));
    }

    @Test
    void sqlServerTemplate() {
        @NotNull @SuppressWarnings("deprecation")
        SmileyVarsTemplate template = SmileyVarsTemplate.sqlServerTemplate("Select * from foo where 1=1 (:and x=:x:)");
        assertEquals("Select * from foo where 1=1 ", template.apply(new HashMap<>()));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", 42);
        assertEquals("Select * from foo where 1=1 and x=42", template.apply(map));
    }

    @Test
    void timestamp() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI, "Select * from foo where 1=1 (:and x=:x :)");
        @NotNull Timestamp timestamp = new Timestamp(new GregorianCalendar(2020, Calendar.APRIL, 18, 13, 43, 56).getTimeInMillis());
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", timestamp);
        assertEquals("Select * from foo where 1=1 and x=TIMESTAMP '2020-04-18 13:43:56' ", template.apply(map));
    }

    @Test
    void dateAsTimestamp() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI, "Select * from foo where 1=1 (:and x=:x:timestamp :)");
        @NotNull Date date = new GregorianCalendar(2020, Calendar.APRIL, 18, 13, 43, 56).getTime();
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", date);
        assertEquals("Select * from foo where 1=1 and x=TIMESTAMP '2020-04-18 13:43:56' ", template.apply(map));
    }

    @Test
    void dateAsDate() throws Exception {
        MockDataSource ds = new MockDataSource();
        MockConnection mockConnection = new MockConnection();
        mockConnection.setMetaData(new MockDatabaseMetaData());
        ds.setupConnection(mockConnection);
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(ds, "Select * from foo where 1=1 (:and x=:x:date:)");
        @NotNull Date date = new GregorianCalendar(2020, Calendar.APRIL, 18, 13, 43, 56).getTime();
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", date);
        assertEquals("Select * from foo where 1=1 and x=DATE '2020-04-18'", template.apply(map));
    }

    @Test
    void CalendarAsTimestamp() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where 1=1 (:and x=:x:timestamp:)");
        @NotNull Calendar calendar = new GregorianCalendar(2020, Calendar.FEBRUARY, 18, 13, 43, 56);
        calendar.setTimeZone(TimeZone.getTimeZone("EST"));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", calendar);
        assertEquals("Select * from foo where 1=1 and x=TIMESTAMP '2020-2-18 13:43:56-5:0'", template.apply(map));
        calendar.setTimeZone(TimeZone.getTimeZone("IST"));
        assertEquals("Select * from foo where 1=1 and x=TIMESTAMP '2020-2-19 0:13:56+5:30'", template.apply(map));
    }

    @Test
    void CalendarAsDate() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where 1=1 (:and x=:x :)");
        @NotNull Calendar calendar = new GregorianCalendar(2020, Calendar.FEBRUARY, 18, 13, 43, 56);
        calendar.setTimeZone(TimeZone.getTimeZone("EST"));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", calendar);
        assertEquals("Select * from foo where 1=1 and x=DATE '2020-2-18' ", template.apply(map));
    }

    @Test
    void wrongFormatterType() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where 1=1 (:and x=:x:timestamp :)");
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", "BoGuS");
        assertThrows(SmileyVarsException.class, ()->template.apply(map));
    }

    @Test
    void noFormatterType() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where 1=1 (:and x=:x :)");
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", this);
        assertThrows(SmileyVarsException.class, ()->template.apply(map));
    }

    @Test
    void temporalAccessorAsTimestamp() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where 1=1 (:and x=:x:timestamp:)");
        @NotNull ZonedDateTime instant = ZonedDateTime.of(2020,2, 18, 13, 43, 56, 0, ZoneId.of("-5"));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", instant);
        assertEquals("Select * from foo where 1=1 and x=TIMESTAMP '2020-2-18 13:43:56-5:0'", template.apply(map));
        map.put("x", instant.withZoneSameInstant(ZoneId.of("+05:30")));
        assertEquals("Select * from foo where 1=1 and x=TIMESTAMP '2020-2-19 0:13:56+5:30'", template.apply(map));
    }

    @Test
    void temporalAccessorAsDate() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where 1=1 (:and x=:x:)");
        @NotNull ZonedDateTime instant = ZonedDateTime.of(2020,2, 18, 13, 43, 56, 0, ZoneId.of("-5"));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", instant);
        assertEquals("Select * from foo where 1=1 and x=DATE '2020-2-18'", template.apply(map));
    }

    @Test
    void unclosedBracket() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where 1=1 (:and x=:x:date)");
        @NotNull ZonedDateTime instant = ZonedDateTime.of(2020,2, 18, 13, 43, 56, 0, ZoneId.of("-5"));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", instant);
        assertEquals("Select * from foo where 1=1 and x=DATE '2020-2-18')", template.apply(map));
    }

    @Test
    void extraClosedBracket() {
        @NotNull String sql = "Select * from foo where 1=1 :)";
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where 1=1 :)");
        assertEquals(sql, template.apply(new HashMap<>()));
    }

    @Test
    void unbracketedBoundVar() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where x=:x");
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", 42);
        assertEquals("Select * from foo where x=42", template.apply(map));
    }

    @Test
    void unbracketedUnboundVar() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where x=:x");
        assertThrows(UnboundVariableException.class, ()->template.apply(new HashMap<>()));
    }

    @Disabled
    @Test
    void nestedRightBoundBrackets() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(: and x=:x(: and y=:y:):)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and x='42' and y=39", template.apply(map));
    }

    @Disabled
    @Test
    void nestedLeftBoundBrackets() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(:(: and x=:x:) and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and x='42' and y=39", template.apply(map));
    }

    @Disabled
    @Test
    void nestedRightInnerUnboundBrackets() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(: and x=:x(: and y=:y:):)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        assertEquals("Select * from foo where 1=1 and x='42'", template.apply(map));
    }

    @Disabled
    @Test
    void nestedLeftInnerUnboundBrackets() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(:(: and x=:x:) and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and y=39", template.apply(map));
    }

    @Disabled
    @Test
    void nestedRightOuterUnboundBrackets() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(: and x=:x(: and y=:y:):)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1", template.apply(map));
    }

    @Disabled
    @Test
    void nestedLeftOuterUnboundBrackets() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(:(: and x=:x:) and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        assertEquals("Select * from foo where 1=1", template.apply(map));
    }

    @Test
    void bracketedNull() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(: and x=:n:)");
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("n", null);
        assertEquals("Select * from foo where 1=1 and x=null", template.apply(map));
    }

    @Test
    void unbracketedNull() {
        @NotNull SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1 and x=:n");
        @NotNull Map<String, Object> map = new HashMap<>();
        map.put("n", null);
        assertEquals("Select * from foo where 1=1 and x=null", template.apply(map));
    }

    @Test
    void getNoVarNames() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI, "select * from foo");
        assertEquals(0, template.getVarNames().size());
    }

    @Test
    void getVar2Names() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI, "select * from foo where x = :x and y = :y");
        assertEquals(2, template.getVarNames().size());
        assertTrue(template.getVarNames().contains("x"));
        assertTrue(template.getVarNames().contains("y"));
    }
}