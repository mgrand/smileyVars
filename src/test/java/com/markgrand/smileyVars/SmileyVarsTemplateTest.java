package com.markgrand.smileyVars;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SmileyVarsTemplateTest {

    @org.junit.jupiter.api.Test
    void ansiTemplate() {
        SmileyVarsTemplate template = SmileyVarsTemplate.ansiTemplate("Select * from foo where 1=1 (:and x=:x:)");
        assertEquals("Select * from foo where 1=1 ", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        assertEquals("Select * from foo where 1=1 and x=42", template.apply(map));
    }

    @org.junit.jupiter.api.Test
    void ansiEmbeddedQuote() {
        SmileyVarsTemplate template = SmileyVarsTemplate.ansiTemplate("Select * from foo where 1=1 (:and x=:x:)");
        Map<String, Object> map = new HashMap<>();
        map.put("x", "can't or won't");
        assertEquals("Select * from foo where 1=1 and x='can''t or won''t'", template.apply(map));
    }

    @org.junit.jupiter.api.Test
    void postgresqlTemplate() {
        SmileyVarsTemplate template = SmileyVarsTemplate.postgresqlTemplate("Select * from foo where 1=1 (:and x=:x:)");
        assertEquals("Select * from foo where 1=1 ", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        assertEquals("Select * from foo where 1=1 and x=42", template.apply(map));
    }

    @org.junit.jupiter.api.Test
    void oracleTemplate() {
        SmileyVarsTemplate template = SmileyVarsTemplate.oracleTemplate("Select * from foo where 1=1 (:and x=:x:)");
        assertEquals("Select * from foo where 1=1 ", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        assertEquals("Select * from foo where 1=1 and x=42", template.apply(map));
    }

    @org.junit.jupiter.api.Test
    void sqlServerTemplate() {
        SmileyVarsTemplate template = SmileyVarsTemplate.oracleTemplate("Select * from foo where 1=1 (:and x=:x:)");
        assertEquals("Select * from foo where 1=1 ", template.apply(new HashMap<>()));
        Map<String, Object> map = new HashMap<>();
        map.put("x", "42");
        assertEquals("Select * from foo where 1=1 and x=42", template.apply(map));
    }
}