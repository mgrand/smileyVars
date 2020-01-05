package com.markgrand.smileyvars;

import com.markgrand.smileyvars.util.BiSqlConsumer;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Map;

/**
 * {@code MapSetter} is used to set the parameters of a {@link SmileyVarsPreparedStatement} from Maps that contain pairs
 * of column names and values. You tell a {@code MapSetter} what {@link SmileyVarsPreparedStatement} method to use to
 * set each column when the {@code MapSetter} is constructor. You do this by passing a map to the constructor that pairs
 * column names with setter methods like this:
 * <pre>
 *     Map&lt;String, BiSqlConsumer&lt;SmileyVarsPreparedStatement, Object&gt;&gt; setterMap;
 *     setterMap.put("quantity", (svps, value) -&gt; svps.setInt(svps, (int)value);
 *     setterMap.put("description", (svps, value) -&gt; svps.setString(svps, (String)value);
 *     MapSetter mapSetter = new MapSetter(setterMap);
 * </pre>
 *
 * @author Mark Grand
 */
public class MapSetter {
    private Map<String, BiSqlConsumer<SmileyVarsPreparedStatement, Object>> setterMap;

    /**
     * Constructor.  The argument to this is a map that will tell the constructed {@code MapSetter} object how to set
     * each column like this:
     * <pre>
     *     Map&lt;String, BiSqlConsumer&lt;SmileyVarsPreparedStatement, Object&gt;&gt; setterMap;
     *     setterMap.put("quantity", (svps, value) -&gt; svps.setInt(svps, (int)value);
     *     setterMap.put("description", (svps, value) -&gt; svps.setString(svps, (String)value);
     *     MapSetter mapSetter = new MapSetter(setterMap);
     * </pre>
     *
     * @param setterMap the map that pairs names with setters.
     */
    public MapSetter(@NotNull Map<String, BiSqlConsumer<SmileyVarsPreparedStatement, Object>> setterMap) {
        this.setterMap = setterMap;
    }

    /**
     * Set smileyVars of the given {@link SmileyVarsPreparedStatement} from the values in the valueMap. Any previously
     * set smileyVars are first cleared.
     *
     * @param svps the {@link SmileyVarsPreparedStatement} whose parameters are to be set.
     * @param valueMap a Map containing the names and values to be set.
     * @return the {@link SmileyVarsPreparedStatement}
     * @throws SQLException if any of the setters throw an SQLException
     * @throws SmileyVarsException If there is a problem setting the values of the smileyVars.
     */
    public SmileyVarsPreparedStatement setSmileyVars(@NotNull SmileyVarsPreparedStatement svps, Map<String, Object> valueMap) throws SQLException {
        svps.clearParameters();
        for (Map.Entry<String, Object> valueEntry: valueMap.entrySet()) {
            setterMap.computeIfAbsent(valueEntry.getKey(), nm->throwException(nm)).accept(svps, valueEntry.getValue());
        }
        return svps;
    }

    private BiSqlConsumer<SmileyVarsPreparedStatement, Object> throwException(String name) {
        String msg = "MapSetter was not constructed with a setter for a SmileyVar named " + name
                + "; The names that it was constructed with are " + setterMap.keySet();
        throw new SmileyVarsException(msg);
    }
}
