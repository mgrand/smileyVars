package com.markgrand.smileyVars;

import com.markgrand.smileyVars.util.CheckedContinuation;
import com.markgrand.smileyVars.util.PreparedStatementSetter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Singleton to maintain a registry of {@link ValueFormatter} objects
 */
class ValueFormatterRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ValueFormatterRegistry.class);

    private static final ValueFormatterRegistry ansiRegistry = new ValueFormatterRegistry("ANSI");
    private static PreparedStatementSetter preparedStatementBooleanSetter =
            (preparedStatement, i, value)-> {
        if (value == null) {
            handleSqlException(()->preparedStatement.setNull(i, Types.BOOLEAN));
        } else {
            handleSqlException(() -> preparedStatement.setBoolean(i, (boolean) value));
        }
    };
    private static final ValueFormatterRegistry postgresqlRegistry
            = new ValueFormatterRegistry("PostgreSQL")
                      .registerFormatter("boolean", Boolean.class, preparedStatementBooleanSetter,
                              bool -> bool== null ? "null" : bool.toString());
    private static final SimpleDateFormat timestampFormatNoZone = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private volatile static LinkedHashMap<String, ValueFormatter> commonBuiltinFormatters;
    private final LinkedHashMap<String, ValueFormatter> formatterMap = new LinkedHashMap<>();
    private final String name;

    private ValueFormatterRegistry(String name) {
        ensureCommonBuiltinFormattersAreRegistered();
        formatterMap.putAll(commonBuiltinFormatters);
        this.name = name;
        //TODO add formatter for BitSet, Time, Calendar, Duration, Money, unique identifier/GUID
        //TODO need to account for national character set string literals and unicode string literals.
    }

    private static void ensureCommonBuiltinFormattersAreRegistered() {
        if (commonBuiltinFormatters == null) {
            synchronized (ValueFormatter.class) {
                if (commonBuiltinFormatters == null) {
                    logger.debug("Registering common formatters.");
                    commonBuiltinFormatters = new LinkedHashMap<>();
                    registerNumberFormatter(commonBuiltinFormatters);
                    registerStringFormatter(commonBuiltinFormatters);
                    registerTimestampFormatter(commonBuiltinFormatters);
                    registerDateFormatter(commonBuiltinFormatters);
                    logger.debug("Registered common formatters: " + commonBuiltinFormatters);
                }
            }
        }
    }

    @NotNull
    static ValueFormatterRegistry ansiInstance() {
        return ansiRegistry;
    }

    @NotNull
    static ValueFormatterRegistry postgresqlInstance() {
        return postgresqlRegistry;
    }

    private static void registerTimestampFormatter(@NotNull @SuppressWarnings("SameParameterValue") LinkedHashMap<String, ValueFormatter> registryMap) {
        @NotNull final String formatterName = "timestamp";
        @NotNull Predicate<Object> predicate = object -> object instanceof Date || object instanceof Calendar || object instanceof TemporalAccessor;
        @NotNull Function<Object, String> formattingFunction = value -> {
            @NotNull StringBuilder builder = new StringBuilder("TIMESTAMP '");
            if (value instanceof Date) {
                doTimestampFormatNoZone((Date) value, builder);
            } else if (value instanceof Calendar) {
                formatCalendarAsTimestamp((Calendar) value, builder);
            } else if (value instanceof TemporalAccessor) {
                formatTemporalAccessorAsTimestamp((TemporalAccessor) value, builder);
            } else {
                handleInapplicableValue(formatterName, value);
            }
            return builder.append('\'').toString();
        };
        PreparedStatementSetter preparedStatementSetter = (preparedStatement, i, value) -> {
            final Timestamp timestamp =
                    (value instanceof Date) ? new Timestamp(((Date) value).getTime())
                            : value instanceof Calendar ? new Timestamp(((Calendar) value).getTimeInMillis())
                                      : value instanceof TemporalAccessor ? new Timestamp(((TemporalAccessor) value).getLong(ChronoField.EPOCH_DAY)
                                                                                                  + ((TemporalAccessor) value).getLong(ChronoField.MILLI_OF_DAY))
                                                : handleInapplicableValue(formatterName, value);
            handleSqlException(() -> preparedStatement.setTimestamp(i, timestamp));
        };
        registerFormatter(formatterName, predicate, formattingFunction, preparedStatementSetter, registryMap);
    }

    private static void registerDateFormatter(@NotNull @SuppressWarnings("SameParameterValue") LinkedHashMap<String, ValueFormatter> registryMap) {
        @NotNull final String formatterName = "date";
        @NotNull Predicate<Object> predicate = object -> object instanceof Date || object instanceof Calendar || object instanceof TemporalAccessor;
        @NotNull Function<Object, String> formattingFunction = value -> {
            @NotNull StringBuilder builder = new StringBuilder("DATE '");
            if (value instanceof Date) {
                doDateFormat((Date) value, builder);
            } else if (value instanceof Calendar) {
                formatCalendarAsDate((Calendar) value, builder);
            } else if (value instanceof TemporalAccessor) {
                formatTemporalAccessorAsDate((TemporalAccessor) value, builder);
            } else {
                handleInapplicableValue(formatterName, value);
            }
            return builder.append('\'').toString();
        };
        PreparedStatementSetter preparedStatementSetter = (preparedStatement, i, value) -> {
            final java.sql.Date date =
                    (value instanceof java.sql.Date) ? (java.sql.Date) value
                            : value instanceof Date ? new java.sql.Date(((Date)value).getTime())
                            : value instanceof Calendar ? new java.sql.Date(((Calendar) value).getTimeInMillis())
                                      : value instanceof TemporalAccessor ? new java.sql.Date(((TemporalAccessor) value).getLong(ChronoField.EPOCH_DAY)
                                                                                             + ((TemporalAccessor) value).getLong(ChronoField.MILLI_OF_DAY))
                                                : handleInapplicableValue(formatterName, value);
            handleSqlException(()->preparedStatement.setDate(i, date));
        };
        registerFormatter(formatterName, predicate, formattingFunction, preparedStatementSetter, registryMap);
    }

    private static void formatTemporalAccessorAsTimestamp(@NotNull TemporalAccessor accessor, @NotNull StringBuilder builder) {
        builder.append(accessor.get(ChronoField.YEAR)).append('-')
                .append(accessor.get(ChronoField.MONTH_OF_YEAR)).append('-').append(accessor.get(ChronoField.DAY_OF_MONTH))
                .append(' ').append(accessor.get(ChronoField.HOUR_OF_DAY))
                .append(':').append(accessor.get(ChronoField.MINUTE_OF_HOUR)).append(':').append(accessor.get(ChronoField.SECOND_OF_MINUTE));
        int zoneOffsetMinutes = accessor.get(ChronoField.OFFSET_SECONDS) / 60;
        if (zoneOffsetMinutes >= 0) {
            builder.append('+');
        }
        builder.append(zoneOffsetMinutes / 60).append(':').append(zoneOffsetMinutes % 60);
    }

    private static void formatTemporalAccessorAsDate(@NotNull TemporalAccessor accessor, @NotNull StringBuilder builder) {
        builder.append(accessor.get(ChronoField.YEAR)).append('-')
                .append(accessor.get(ChronoField.MONTH_OF_YEAR)).append('-').append(accessor.get(ChronoField.DAY_OF_MONTH));
    }

    private static void formatCalendarAsTimestamp(@NotNull Calendar calendar, @NotNull StringBuilder builder) {
        builder.append(calendar.get(Calendar.YEAR)).append('-').append(calendar.get(Calendar.MONTH) + 1).append('-').append(calendar.get(Calendar.DAY_OF_MONTH))
                .append(' ').append(calendar.get(Calendar.HOUR_OF_DAY)).append(':').append(calendar.get(Calendar.MINUTE)).append(':').append(calendar.get(Calendar.SECOND));
        int zoneOffsetMinutes = calendar.get(Calendar.ZONE_OFFSET) / (60 * 1000);
        if (zoneOffsetMinutes >= 0) {
            builder.append('+');
        }
        builder.append(zoneOffsetMinutes / 60).append(':').append(zoneOffsetMinutes % 60);
    }

    private static void formatCalendarAsDate(@NotNull Calendar calendar, @NotNull StringBuilder builder) {
        builder.append(calendar.get(Calendar.YEAR)).append('-').append(calendar.get(Calendar.MONTH) + 1).append('-').append(calendar.get(Calendar.DAY_OF_MONTH));
    }

    private static void doTimestampFormatNoZone(Date date, @NotNull StringBuilder builder) {
        synchronized (timestampFormatNoZone) {
            builder.append(timestampFormatNoZone.format(date));
        }
    }

    private static void doDateFormat(Date date, @NotNull StringBuilder builder) {
        synchronized (dateFormat) {
            builder.append(dateFormat.format(date));
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static <T> T handleInapplicableValue(String formatterName, @NotNull Object value) {
        @NotNull String msg = "Formatter named " + formatterName + " cannot be applied to object of class " + value.getClass().getName();
        throw new IllegalArgumentException(msg);
    }

    @SuppressWarnings("SameParameterValue")
    private static void registerStringFormatter(@NotNull LinkedHashMap<String, ValueFormatter> registryMap) {
        PreparedStatementSetter preparedStatementSetter
                = (preparedStatement, i, value) -> handleSqlException(() -> preparedStatement.setString(i, (String) value));
        registerFormatter("string",
                String.class, string -> "'" + ((String) string).replace("'", "''") + "'",
                preparedStatementSetter, registryMap);
    }

    @SuppressWarnings("SameParameterValue")
    private static void registerNumberFormatter(@NotNull LinkedHashMap<String, ValueFormatter> registryMap) {
        final String formatterName = "number";
        PreparedStatementSetter preparedStatementSetter = (preparedStatement, i, value) -> {
            if (value == null) {
                handleSqlException(() -> preparedStatement.setNull(i, Types.TINYINT));
            } else if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
                handleSqlException(() -> preparedStatement.setInt(i, ((Number)value).intValue()));
            } else if (value instanceof Long) {
                handleSqlException(() -> preparedStatement.setLong(i, (Long) value));
            } else if (value instanceof BigDecimal) {
                handleSqlException(() -> preparedStatement.setBigDecimal(i, (BigDecimal) value));
            } else if (value instanceof BigInteger) {
                handleSqlException(() -> preparedStatement.setBigDecimal(i, new BigDecimal((BigInteger) value)));
            } else if (value instanceof Double) {
                handleSqlException(() -> preparedStatement.setDouble(i, (Double) value));
            } else if (value instanceof Float) {
                handleSqlException(() -> preparedStatement.setDouble(i, (Float) value));
            } else {
                handleInapplicableValue(formatterName, value);
            }
        };

        registerFormatter(formatterName, Number.class, Object::toString, preparedStatementSetter, registryMap);
    }

    private static void registerFormatter(@NotNull String name,
                                          @NotNull Class clazz,
                                          @NotNull Function<Object, String> formatter,
                                          @NotNull PreparedStatementSetter preparedStatementSetter,
                                          @NotNull LinkedHashMap<String, ValueFormatter> map) {
        map.put(name, new ValueFormatter(clazz::isInstance, formatter, preparedStatementSetter, name));
    }

    private static void registerFormatter(@NotNull String name, @NotNull Predicate<Object> predicate,
                                          @NotNull Function<Object, String> formatter,
                                          @NotNull PreparedStatementSetter preparedStatementSetter,
                                          @NotNull LinkedHashMap<String, ValueFormatter> map) {
        map.put(name, new ValueFormatter(predicate, formatter, preparedStatementSetter, name));
    }

    private static void handleSqlException(CheckedContinuation<SQLException> continuation) {
        try {
            continuation.apply();
        } catch (SQLException e) {
            throw new SmileyVarsSqlException("Error while setting a PreparedStatement parameter.", e);
        }
    }

    String getName() {
        return name;
    }

    /**
     * Use the given formatter to return an SQL literal that will represent that object in the SQL if the object is an
     * instance of the given class.
     *
     * @param name                    The name of this formatter. If the given name is specified with a smileyVar, then
     *                                this formatter will be used to format the smileyVar's value; otherwise the
     *                                formatter will be used if it is the first one registered whose class or predicate
     *                                match the value of the smileyVar.
     * @param clazz                   The class that a value must be an instance of for the given formatter function to
     *                                be used to format it.
     * @param preparedStatementSetter A function that can be used to set a prepared statement parameter. The arguments
     *                                of the function must be the prepared statement, the parameter index and the value
     *                                for the parameter.
     * @param formatter               A function to return a representation of an object as an SQL literal.
     * @return this object
     */
    @NotNull
    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    ValueFormatterRegistry registerFormatter(String name, @NotNull Class clazz,
                                             PreparedStatementSetter preparedStatementSetter,
                                             Function<Object, String> formatter) {
        return registerFormatter(name, clazz::isInstance, preparedStatementSetter, formatter);
    }

    /**
     * If the given predicate returns true when passed the value of a Smiley Var, then use the given formatter to return
     * an SQL literal that will represent that object in the SQL.
     *
     * @param name                    The name of this formatter. If the given name is specified with a smileyVar, then
     *                                this formatter will be used to format the smileyVar's value; otherwise the
     *                                formatter will be used if it is the first one registered whose class or predicate
     *                                match the value of the smileyVar.
     * @param predicate               The predicate to determine if the formatter can be applied to a given value.
     * @param preparedStatementSetter A function that can be used to set a prepared statement parameter. The arguments
     *                                of the function must be the prepared statement, the parameter index and the value
     *                                for the parameter.
     * @param formatter               A function to return a representation of an object as an SQL literal.
     * @return this object
     */
    @NotNull
    @SuppressWarnings("WeakerAccess")
    ValueFormatterRegistry registerFormatter(String name, Predicate<Object> predicate,
                                             PreparedStatementSetter preparedStatementSetter,
                                             Function<Object, String> formatter) {
        registerFormatter(name, predicate, formatter, preparedStatementSetter, formatterMap);
        return this;
    }

    /**
     * Format the given object as an string that is an SQL literal that represents the given object. The formatter used
     * to format the object will be determined by going through the list of formatters until one is found whose {@link
     * ValueFormatter#isApplicable(Object)} returns true for the given object.
     *
     * @param value the object to be represented as an SQL literal.
     * @return the SQL literal as a String or null if the value is null.
     * @throws NoFormatterException if there is no registered applicable formatter.
     */
    @Nullable String format(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        for (@NotNull ValueFormatter valueFormatter : formatterMap.values()) {
            if (valueFormatter.isApplicable(value)) {
                return valueFormatter.format(value);
            }
        }
        throw new NoFormatterException("No registered formatter for value that is an instance of " + value.getClass().getName());
    }

    /**
     * Format the given object as an string that is an SQL literal that represents the given object. The formatter used
     * to format the object will be determined by going through the list of formatters until one is found whose {@link
     * ValueFormatter#isApplicable(Object)} returns true for the given object.
     *
     * @param value the object to be represented as an SQL literal.
     * @return the SQL literal as a String or null if the value is null.
     * @throws NoFormatterException if there is no registered applicable formatter.
     */
    @Nullable String format(@Nullable Object value, String formatterName) {
        if (value == null) {
            return null;
        }
        ValueFormatter valueFormatter = formatterMap.get(formatterName);
        if (valueFormatter == null) {
            throw new NoFormatterException("No registered formatter is named " + formatterName);
        }
        if (!valueFormatter.isApplicable(value)) {
            throw new NoFormatterException("The formatter named " + formatterName + " cannot be applied to the value " + value.toString());
        }
        return valueFormatter.format(value);
    }
}
