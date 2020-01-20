package com.markgrand.smileyvars;

import com.markgrand.smileyvars.util.BiSqlConsumer;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
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

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Set smileyVars of the given {@link SmileyVarsPreparedStatement} from the values in the valueMap. Any previously
     * set smileyVars are first cleared.
     *
     * @param svps     the {@link SmileyVarsPreparedStatement} whose parameters are to be set.
     * @param valueMap a Map containing the names and values to be set.
     * @return the {@link SmileyVarsPreparedStatement}
     * @throws SQLException        if any of the setters throw an SQLException
     * @throws SmileyVarsException If there is a problem setting the values of the smileyVars.
     */
    @SuppressWarnings("UnusedReturnValue")
    public SmileyVarsPreparedStatement setSmileyVars(@NotNull SmileyVarsPreparedStatement svps, Map<String, Object> valueMap) throws SQLException {
        svps.clearParameters();
        for (Map.Entry<String, Object> valueEntry : valueMap.entrySet()) {
            setterMap.computeIfAbsent(valueEntry.getKey(), this::throwException).accept(svps, valueEntry.getValue());
        }
        return svps;
    }

    private BiSqlConsumer<SmileyVarsPreparedStatement, Object> throwException(String name) {
        String msg = "MapSetter was not constructed with a setter for a SmileyVar named " + name
                             + "; The names that it was constructed with are " + setterMap.keySet();
        throw new SmileyVarsException(msg);
    }

    /**
     * Return a read-only view of the map that specifies the method to call for each simpleVar name.
     *
     * @return the read-only view.
     */
    public Map<String, BiSqlConsumer<SmileyVarsPreparedStatement, Object>> getSetterMap() {
        return Collections.unmodifiableMap(setterMap);
    }

    /**
     * Builder class for creating {@link MapSetter} objects.
     */
    public static class Builder {
        private Map<String, BiSqlConsumer<SmileyVarsPreparedStatement, Object>> setterMap = new HashMap<>();

        private Builder() {
        }

        /**
         * Configure the {@link MapSetter} to use {@code setBoolean} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        Builder booleanVar(@SuppressWarnings("SameParameterValue") String name) {
            setterMap.put(name, (svps, value) -> svps.setBoolean(name, (boolean) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setByte} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder byteVar(String name) {
            setterMap.put(name, (svps, value) -> svps.setByte(name, (byte) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setShort} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder shortVar(String name) {
            setterMap.put(name, (svps, value) -> svps.setShort(name, (short) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setInt} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder intVar(String name) {
            setterMap.put(name, (svps, value) -> svps.setInt(name, (int) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setLong} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder longVar(String name) {
            setterMap.put(name, (svps, value) -> svps.setLong(name, (long) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setFloat} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder floatVar(String name) {
            setterMap.put(name, (svps, value) -> svps.setFloat(name, (float) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setDouble} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder doubleVar(String name) {
            setterMap.put(name, (svps, value) -> svps.setDouble(name, (double) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setBigDecimal} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder bigDecimalVar(String name) {
            setterMap.put(name, (svps, value) -> svps.setBigDecimal(name, (BigDecimal) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setString} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder stringVar(String name) {
            setterMap.put(name, (svps, value) -> svps.setString(name, (String) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setBytes} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder bytesVar(String name) {
            setterMap.put(name, (svps, value) -> svps.setBytes(name, (byte[]) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setDate} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder dateVar(String name) {
            setterMap.put(name, (svps, value) -> svps.setDate(name, (Date) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setTime} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder timeVar(String name) {
            setterMap.put(name, (svps, value) -> svps.setTime(name, (Time) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setTimestamp} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder timestampVar(String name) {
            setterMap.put(name, (svps, value) -> svps.setTimestamp(name, (Timestamp) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setObject} to set the smileyVar with the specified name.
         *
         * @param name          The name of the smileyVar to be configured.
         * @param targetSqlType the SQL type (as defined in java.sql.Types) to be sent to the database
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder objectVar(String name, int targetSqlType) {
            setterMap.put(name, (svps, value) -> svps.setObject(name, value, targetSqlType));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setBlob} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder blobVar(String name) {
            setterMap.put(name, (svps, value) -> svps.setBlob(name, (Blob) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setClob} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder clobVar(String name) {
            setterMap.put(name, (svps, value) -> svps.setClob(name, (Clob) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setArray} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder arrayVar(String name) {
            setterMap.put(name, (svps, value) -> svps.setArray(name, (Array) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setDate} to set the smileyVar with the specified name.
         *
         * @param name     The name of the smileyVar to be configured.
         * @param calendar the <code>Calendar</code> object the driver will use to construct the date
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder dateVar(String name, Calendar calendar) {
            setterMap.put(name, (svps, value) -> svps.setDate(name, (Date) value, calendar));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setTime} to set the smileyVar with the specified name.
         *
         * @param name     The name of the smileyVar to be configured.
         * @param calendar the <code>Calendar</code> object the driver will use to construct the date
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder timeVar(String name, Calendar calendar) {
            setterMap.put(name, (svps, value) -> svps.setTime(name, (Time) value, calendar));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setTimestamp} to set the smileyVar with the specified name.
         *
         * @param name     The name of the smileyVar to be configured.
         * @param calendar the <code>Calendar</code> object the driver will use to construct the date
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder timestampVar(String name, Calendar calendar) {
            setterMap.put(name, (svps, value) -> svps.setTimestamp(name, (Timestamp) value, calendar));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setURL} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder urlVar(String name) {
            setterMap.put(name, (svps, value) -> svps.setURL(name, (URL) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setNString} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder nStringVar(String name) {
            setterMap.put(name, (svps, value) -> svps.setNString(name, (String) value));
            return this;
        }

        /**
         * Configure the {@link MapSetter} to use {@code setNClob} to set the smileyVar with the specified name.
         *
         * @param name The name of the smileyVar to be configured.
         * @return this object.
         */
        @SuppressWarnings("SameParameterValue")
        Builder nClob(String name) {
            setterMap.put(name, (svps, value) -> svps.setNClob(name, (NClob) value));
            return this;
        }

        MapSetter build() {
            return new MapSetter(setterMap);
        }
    }
}
