package com.markgrand.smileyvars;

import com.markgrand.smileyvars.util.BiSqlConsumer;
import mockit.Mocked;
import mockit.Verifications;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class MapSetterTest {

    @Test
    void setSmileyVars(@Mocked SmileyVarsPreparedStatement svps) throws Exception {
        Map<String, BiSqlConsumer<SmileyVarsPreparedStatement, Object>> setterMap = new HashMap<>();
        setterMap.put("quantity", (ps, value)->ps.setInt("quantity", (int)value));
        setterMap.put("description", (ps, value)->ps.setString("description", (String)value));
        MapSetter mapSetter = new MapSetter(setterMap);

        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("quantity", 4);
        valueMap.put("description", "foobar");
        mapSetter.setSmileyVars(svps, valueMap);
        new Verifications() {{
           svps.clearParameters(); times = 1;
           svps.setInt("quantity", 4); times = 1;
           svps.setString("description", "foobar"); times = 1;
        }};
    }

    @Test
    void setSmileyVarsWrongName(@Mocked SmileyVarsPreparedStatement svps) {
        Map<String, BiSqlConsumer<SmileyVarsPreparedStatement, Object>> setterMap = new HashMap<>();
        setterMap.put("quantity", (ps, value)->ps.setInt("quantity", (int)value));
        setterMap.put("description", (ps, value)->ps.setString("description", (String)value));
        MapSetter mapSetter = new MapSetter(setterMap);

        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("qty", 4);
        valueMap.put("description", "foobar");
        assertThrows(SmileyVarsException.class, ()->mapSetter.setSmileyVars(svps, valueMap));
    }
}