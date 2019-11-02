package com.markgrand.smileyVars;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SmileyVarsTemplateTest {
    @Test
    void empty() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.SQL_SERVER, "");
        assertEquals("", template.apply(new HashMap<>()));
    }

    @Test
    void emptyAllBracketed() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.SQL_SERVER, "(: foo :bar sadfoij :)");
        assertEquals("", template.apply(new HashMap<>()));
    }

    @Test
    void justClose() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.SQL_SERVER, ":)");
        assertEquals(":)", template.apply(new HashMap<>()));
    }

    @Test
    void ansiTemplate() {
        @SuppressWarnings("deprecation")
        SmileyVarsTemplate template = SmileyVarsTemplate.ansiTemplate("Select * from foo where 1=1 (:and x=:x:)");
        assertEquals("Select * from foo where 1=1 ", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", 42);
        assertEquals("Select * from foo where 1=1 and x=42", template.apply(map));
    }

    @Test
    void ansiEmbeddedQuote() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI, "Select * from foo where 1=1 (:and x=:x:)");
        Map<String, Object> map = new HashMap<>();
        map.put("x", "can't or won't");
        assertEquals("Select * from foo where 1=1 and x='can''t or won''t'", template.apply(map));
    }

    @Test
    void postgresqlTemplate() {
        //noinspection deprecation
        SmileyVarsTemplate template = SmileyVarsTemplate.postgresqlTemplate("Select * from foo where 1=1 (:and x=:x:)");
        assertEquals("Select * from foo where 1=1 ", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", 42);
        assertEquals("Select * from foo where 1=1 and x=42", template.apply(map));
    }

    @Test
    void oracleTemplate() {
        @SuppressWarnings("deprecation")
        SmileyVarsTemplate template = SmileyVarsTemplate.oracleTemplate("Select * from foo where 1=1(: and x=:x:)(: and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and x='42' and y=39", template.apply(map));
    }

    @Test
    void twoValues() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE, "Select * from foo where 1=1(: and x=:x and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and x='42' and y=39", template.apply(map));
    }

    @Test
    void twoLeftValues() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(: and :x=x and :y=y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and '42'=x and 39=y", template.apply(map));
    }

    @Test
    void valueAndNoValue() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(: and x=:x and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        assertEquals("Select * from foo where 1=1", template.apply(map));
    }

    @Test
    void noValueAndValue() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(: and x=:x and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1", template.apply(map));
    }

    @Test
    void twoNoValue() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(: and x=:x and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        assertEquals("Select * from foo where 1=1", template.apply(map));
    }

    @Test
    void sqlServerTemplate() {
        @SuppressWarnings("deprecation")
        SmileyVarsTemplate template = SmileyVarsTemplate.sqlServerTemplate("Select * from foo where 1=1 (:and x=:x:)");
        assertEquals("Select * from foo where 1=1 ", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", 42);
        assertEquals("Select * from foo where 1=1 and x=42", template.apply(map));
    }

    @Test
    void dateAsTimestamp() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI, "Select * from foo where 1=1 (:and x=:x:)");
        Date date = new GregorianCalendar(2020, Calendar.APRIL, 18, 13, 43, 56).getTime();
        Map<String, Object> map = new HashMap<>();
        map.put("x", date);
        assertEquals("Select * from foo where 1=1 and x=TIMESTAMP '2020-04-18 13:43:56'", template.apply(map));
    }

    @Test
    void dateAsDate(@Mocked DatabaseMetaData metaData, @Mocked Connection conn, @Mocked DataSource ds) throws Exception {
        new Expectations() {{
           metaData.getDatabaseProductName();  result = "H2";
           conn.getMetaData(); result = metaData;
           ds.getConnection(); result = conn;
        }};
        SmileyVarsTemplate template = SmileyVarsTemplate.template(ds, "Select * from foo where 1=1 (:and x=:x:date:)");
        Date date = new GregorianCalendar(2020, Calendar.APRIL, 18, 13, 43, 56).getTime();
        Map<String, Object> map = new HashMap<>();
        map.put("x", date);
        assertEquals("Select * from foo where 1=1 and x=DATE '2020-04-18'", template.apply(map));
    }

    @Test
    void CalendarAsTimestamp() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where 1=1 (:and x=:x:)");
        Calendar calendar = new GregorianCalendar(2020, Calendar.FEBRUARY, 18, 13, 43, 56);
        calendar.setTimeZone(TimeZone.getTimeZone("EST"));
        Map<String, Object> map = new HashMap<>();
        map.put("x", calendar);
        assertEquals("Select * from foo where 1=1 and x=TIMESTAMP '2020-2-18 13:43:56-5:0'", template.apply(map));
        calendar.setTimeZone(TimeZone.getTimeZone("IST"));
        assertEquals("Select * from foo where 1=1 and x=TIMESTAMP '2020-2-19 0:13:56+5:30'", template.apply(map));
    }

    @Test
    void CalendarAsDate() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where 1=1 (:and x=:x:date :)");
        Calendar calendar = new GregorianCalendar(2020, Calendar.FEBRUARY, 18, 13, 43, 56);
        calendar.setTimeZone(TimeZone.getTimeZone("EST"));
        Map<String, Object> map = new HashMap<>();
        map.put("x", calendar);
        assertEquals("Select * from foo where 1=1 and x=DATE '2020-2-18' ", template.apply(map));
    }

    @Test
    void wrongFormatterType() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where 1=1 (:and x=:x:timestamp :)");
        Map<String, Object> map = new HashMap<>();
        map.put("x", "BoGuS");
        assertThrows(SmileyVarsException.class, ()->template.apply(map));
    }

    @Test
    void noFormatterType() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where 1=1 (:and x=:x :)");
        Map<String, Object> map = new HashMap<>();
        map.put("x", this);
        assertThrows(SmileyVarsException.class, ()->template.apply(map));
    }

    @Test
    void temporalAccessorAsTimestamp() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where 1=1 (:and x=:x:)");
        ZonedDateTime instant = ZonedDateTime.of(2020,2, 18, 13, 43, 56, 0, ZoneId.of("-5"));
        Map<String, Object> map = new HashMap<>();
        map.put("x", instant);
        assertEquals("Select * from foo where 1=1 and x=TIMESTAMP '2020-2-18 13:43:56-5:0'", template.apply(map));
        map.put("x", instant.withZoneSameInstant(ZoneId.of("+05:30")));
        assertEquals("Select * from foo where 1=1 and x=TIMESTAMP '2020-2-19 0:13:56+5:30'", template.apply(map));
    }

    @Test
    void temporalAccessorAsDate() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where 1=1 (:and x=:x:date:)");
        ZonedDateTime instant = ZonedDateTime.of(2020,2, 18, 13, 43, 56, 0, ZoneId.of("-5"));
        Map<String, Object> map = new HashMap<>();
        map.put("x", instant);
        assertEquals("Select * from foo where 1=1 and x=DATE '2020-2-18'", template.apply(map));
    }

    @Test
    void unclosedBracket() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where 1=1 (:and x=:x:date)");
        ZonedDateTime instant = ZonedDateTime.of(2020,2, 18, 13, 43, 56, 0, ZoneId.of("-5"));
        Map<String, Object> map = new HashMap<>();
        map.put("x", instant);
        assertEquals("Select * from foo where 1=1 and x=DATE '2020-2-18')", template.apply(map));
    }

    @Test
    void extraClosedBracket() {
        String sql = "Select * from foo where 1=1 :)";
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where 1=1 :)");
        assertEquals(sql, template.apply(new HashMap<>()));
    }

    @Test
    void unbracketedBoundVar() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where x=:x");
        Map<String, Object> map = new HashMap<>();
        map.put("x", 42);
        assertEquals("Select * from foo where x=42", template.apply(map));
    }

    @Test
    void unbracketedUnboundVar() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ANSI,"Select * from foo where x=:x");
        assertThrows(UnboundVariableException.class, ()->template.apply(new HashMap<>()));
    }

    @Disabled
    @Test
    void nestedRightBoundBrackets() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(: and x=:x(: and y=:y:):)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and x='42' and y=39", template.apply(map));
    }

    @Disabled
    @Test
    void nestedLeftBoundBrackets() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(:(: and x=:x:) and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and x='42' and y=39", template.apply(map));
    }

    @Disabled
    @Test
    void nestedRightInnerUnboundBrackets() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(: and x=:x(: and y=:y:):)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        assertEquals("Select * from foo where 1=1 and x='42'", template.apply(map));
    }

    @Disabled
    @Test
    void nestedLeftInnerUnboundBrackets() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(:(: and x=:x:) and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and y=39", template.apply(map));
    }

    @Disabled
    @Test
    void nestedRightOuterUnboundBrackets() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(: and x=:x(: and y=:y:):)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1", template.apply(map));
    }

    @Disabled
    @Test
    void nestedLeftOuterUnboundBrackets() {
        SmileyVarsTemplate template = SmileyVarsTemplate.template(DatabaseType.ORACLE,"Select * from foo where 1=1(:(: and x=:x:) and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        assertEquals("Select * from foo where 1=1", template.apply(map));
    }
}