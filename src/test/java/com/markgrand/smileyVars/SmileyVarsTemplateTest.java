package com.markgrand.smileyVars;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SmileyVarsTemplateTest {

    @Test
    void ansiTemplate() {
        SmileyVarsTemplate template = SmileyVarsTemplate.ansiTemplate("Select * from foo where 1=1 (:and x=:x:)");
        assertEquals("Select * from foo where 1=1 ", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", 42);
        assertEquals("Select * from foo where 1=1 and x=42", template.apply(map));
    }

    @Test
    void ansiEmbeddedQuote() {
        SmileyVarsTemplate template = SmileyVarsTemplate.ansiTemplate("Select * from foo where 1=1 (:and x=:x:)");
        Map<String, Object> map = new HashMap<>();
        map.put("x", "can't or won't");
        assertEquals("Select * from foo where 1=1 and x='can''t or won''t'", template.apply(map));
    }

    @Test
    void postgresqlTemplate() {
        SmileyVarsTemplate template = SmileyVarsTemplate.postgresqlTemplate("Select * from foo where 1=1 (:and x=:x:)");
        assertEquals("Select * from foo where 1=1 ", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", 42);
        assertEquals("Select * from foo where 1=1 and x=42", template.apply(map));
    }

    @Test
    void oracleTemplate() {
        SmileyVarsTemplate template = SmileyVarsTemplate.oracleTemplate("Select * from foo where 1=1(: and x=:x:)(: and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and x='42' and y=39", template.apply(map));
    }

    @Test
    void twoValues() {
        SmileyVarsTemplate template = SmileyVarsTemplate.oracleTemplate("Select * from foo where 1=1(: and x=:x and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and x='42' and y=39", template.apply(map));
    }

    @Test
    void twoLeftValues() {
        SmileyVarsTemplate template = SmileyVarsTemplate.oracleTemplate("Select * from foo where 1=1(: and :x=x and :y=y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and '42'=x and 39=y", template.apply(map));
    }

    @Test
    void valueAndNoValue() {
        SmileyVarsTemplate template = SmileyVarsTemplate.oracleTemplate("Select * from foo where 1=1(: and x=:x and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        assertEquals("Select * from foo where 1=1", template.apply(map));
    }

    @Test
    void noValueAndValue() {
        SmileyVarsTemplate template = SmileyVarsTemplate.oracleTemplate("Select * from foo where 1=1(: and x=:x and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1", template.apply(map));
    }

    @Test
    void toeNoValue() {
        SmileyVarsTemplate template = SmileyVarsTemplate.oracleTemplate("Select * from foo where 1=1(: and x=:x and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        assertEquals("Select * from foo where 1=1", template.apply(map));
    }

    @Test
    void sqlServerTemplate() {
        SmileyVarsTemplate template = SmileyVarsTemplate.oracleTemplate("Select * from foo where 1=1 (:and x=:x:)");
        assertEquals("Select * from foo where 1=1 ", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", 42);
        assertEquals("Select * from foo where 1=1 and x=42", template.apply(map));
    }

    @Test
    void dateAsTimestamp() {
        SmileyVarsTemplate template = SmileyVarsTemplate.ansiTemplate("Select * from foo where 1=1 (:and x=:x:)");
        Date date = new GregorianCalendar(2020, Calendar.APRIL, 18, 13, 43, 56).getTime();
        Map<String, Object> map = new HashMap<>();
        map.put("x", date);
        assertEquals("Select * from foo where 1=1 and x=TIMESTAMP '2020-04-18 13:43:56'", template.apply(map));
    }

    @Test
    void dateAsDate() {
        SmileyVarsTemplate template = SmileyVarsTemplate.ansiTemplate("Select * from foo where 1=1 (:and x=:x:date:)");
        Date date = new GregorianCalendar(2020, Calendar.APRIL, 18, 13, 43, 56).getTime();
        Map<String, Object> map = new HashMap<>();
        map.put("x", date);
        assertEquals("Select * from foo where 1=1 and x=DATE '2020-04-18'", template.apply(map));
    }

    @Test
    void CalendarAsTimestamp() {
        SmileyVarsTemplate template = SmileyVarsTemplate.ansiTemplate("Select * from foo where 1=1 (:and x=:x:)");
        Calendar calendar = new GregorianCalendar(2020, Calendar.FEBRUARY, 18, 13, 43, 56);
        calendar.setTimeZone(TimeZone.getTimeZone("EST"));
        Map<String, Object> map = new HashMap<>();
        map.put("x", calendar);
        assertEquals("Select * from foo where 1=1 and x=TIMESTAMP '2020-2-18 13:43:56-5:0'", template.apply(map));
    }

    @Test
    void CalendarAsDate() {
        SmileyVarsTemplate template = SmileyVarsTemplate.ansiTemplate("Select * from foo where 1=1 (:and x=:x:date :)");
        Calendar calendar = new GregorianCalendar(2020, Calendar.FEBRUARY, 18, 13, 43, 56);
        calendar.setTimeZone(TimeZone.getTimeZone("EST"));
        Map<String, Object> map = new HashMap<>();
        map.put("x", calendar);
        assertEquals("Select * from foo where 1=1 and x=DATE '2020-2-18' ", template.apply(map));
    }

    @Test
    void TemporalAccessorAsTimestamp() {
        SmileyVarsTemplate template = SmileyVarsTemplate.ansiTemplate("Select * from foo where 1=1 (:and x=:x:)");
        ZonedDateTime instant = ZonedDateTime.of(2020,2, 18, 13, 43, 56, 0, ZoneId.of("-5"));
        Map<String, Object> map = new HashMap<>();
        map.put("x", instant);
        assertEquals("Select * from foo where 1=1 and x=TIMESTAMP '2020-2-18 13:43:56-5:0'", template.apply(map));
    }

    @Test
    void TemporalAccessorAsDate() {
        SmileyVarsTemplate template = SmileyVarsTemplate.ansiTemplate("Select * from foo where 1=1 (:and x=:x:date:)");
        ZonedDateTime instant = ZonedDateTime.of(2020,2, 18, 13, 43, 56, 0, ZoneId.of("-5"));
        Map<String, Object> map = new HashMap<>();
        map.put("x", instant);
        assertEquals("Select * from foo where 1=1 and x=DATE '2020-2-18'", template.apply(map));
    }

    @Test
    void unclosedBracket() {
        SmileyVarsTemplate template = SmileyVarsTemplate.ansiTemplate("Select * from foo where 1=1 (:and x=:x:date)");
        ZonedDateTime instant = ZonedDateTime.of(2020,2, 18, 13, 43, 56, 0, ZoneId.of("-5"));
        Map<String, Object> map = new HashMap<>();
        map.put("x", instant);
        assertEquals("Select * from foo where 1=1 and x=DATE '2020-2-18')", template.apply(map));
    }

    @Test
    void unbracketedBoundVar() {
        SmileyVarsTemplate template = SmileyVarsTemplate.ansiTemplate("Select * from foo where x=:x");
        Map<String, Object> map = new HashMap<>();
        map.put("x", 42);
        assertEquals("Select * from foo where x=42", template.apply(map));
    }

    @Test
    void unbracketedUnboundVar() {
        SmileyVarsTemplate template = SmileyVarsTemplate.ansiTemplate("Select * from foo where x=:x");
        assertThrows(UnboundVariableException.class, ()->template.apply(new HashMap<>()));
    }

    @Disabled
    @Test
    void nestedRightBoundBrackets() {
        SmileyVarsTemplate template = SmileyVarsTemplate.oracleTemplate("Select * from foo where 1=1(: and x=:x(: and y=:y:):)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and x='42' and y=39", template.apply(map));
    }

    @Disabled
    @Test
    void nestedLeftBoundBrackets() {
        SmileyVarsTemplate template = SmileyVarsTemplate.oracleTemplate("Select * from foo where 1=1(:(: and x=:x:) and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and x='42' and y=39", template.apply(map));
    }

    @Disabled
    @Test
    void nestedRightInnerUnboundBrackets() {
        SmileyVarsTemplate template = SmileyVarsTemplate.oracleTemplate("Select * from foo where 1=1(: and x=:x(: and y=:y:):)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        assertEquals("Select * from foo where 1=1 and x='42'", template.apply(map));
    }

    @Disabled
    @Test
    void nestedLeftInnerUnboundBrackets() {
        SmileyVarsTemplate template = SmileyVarsTemplate.oracleTemplate("Select * from foo where 1=1(:(: and x=:x:) and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1 and y=39", template.apply(map));
    }

    @Disabled
    @Test
    void nestedRightOuterUnboundBrackets() {
        SmileyVarsTemplate template = SmileyVarsTemplate.oracleTemplate("Select * from foo where 1=1(: and x=:x(: and y=:y:):)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("y", 39);
        assertEquals("Select * from foo where 1=1", template.apply(map));
    }

    @Disabled
    @Test
    void nestedLeftOuterUnboundBrackets() {
        SmileyVarsTemplate template = SmileyVarsTemplate.oracleTemplate("Select * from foo where 1=1(:(: and x=:x:) and y=:y:)");
        assertEquals("Select * from foo where 1=1", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        assertEquals("Select * from foo where 1=1", template.apply(map));
    }
}