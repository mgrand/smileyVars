package com.markgrand.smileyVars;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final ValueFormatterRegistry postgresqlRegistry
            = new ValueFormatterRegistry("PostgreSQL").registerFormatter("boolean", Boolean.class, bool -> ((Boolean) bool).toString());
    private static final SimpleDateFormat timestampFormatNoZone = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private volatile static LinkedHashMap<String, ValueFormatter> commonBuiltinFormatters;

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

    private final LinkedHashMap<String, ValueFormatter> formatterMap = new LinkedHashMap<>();
    private final String name;

    public String getName() {
        return name;
    }

    private ValueFormatterRegistry(String name) {
        ensureCommonBuiltinFormattersAreRegistered();
        formatterMap.putAll(commonBuiltinFormatters);
        this.name = name;
        //TODO add formatter for BitSet, Time, Calendar, Duration, Money, unique identifier/GUID
        //TODO need to account for national character set string literals and unicode string literals.
    }

    static ValueFormatterRegistry ansiInstance() {
        return ansiRegistry;
    }

    static ValueFormatterRegistry postgresqlInstance() {
        return postgresqlRegistry;
    }

    private static void registerTimestampFormatter(@SuppressWarnings("SameParameterValue") LinkedHashMap<String, ValueFormatter> registryMap) {
        final String formatterName = "timestamp";
        Predicate<Object> predicate = object -> object instanceof Date || object instanceof Calendar || object instanceof TemporalAccessor;
        Function<Object, String> formattingFunction = value -> {
            StringBuilder builder = new StringBuilder("TIMESTAMP '");
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
        registerFormatter(formatterName, predicate, formattingFunction, registryMap);
    }

    private static void registerDateFormatter(@SuppressWarnings("SameParameterValue") LinkedHashMap<String, ValueFormatter> registryMap) {
        final String formatterName = "date";
        Predicate<Object> predicate = object -> object instanceof Date || object instanceof Calendar || object instanceof TemporalAccessor;
        Function<Object, String> formattingFunction = value -> {
            StringBuilder builder = new StringBuilder("DATE '");
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
        registerFormatter(formatterName, predicate, formattingFunction, registryMap);
    }

    private static void formatTemporalAccessorAsTimestamp(TemporalAccessor accessor, StringBuilder builder) {
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

    private static void formatTemporalAccessorAsDate(TemporalAccessor accessor, StringBuilder builder) {
        builder.append(accessor.get(ChronoField.YEAR)).append('-')
                .append(accessor.get(ChronoField.MONTH_OF_YEAR)).append('-').append(accessor.get(ChronoField.DAY_OF_MONTH));
    }

    private static void formatCalendarAsTimestamp(Calendar calendar, StringBuilder builder) {
        builder.append(calendar.get(Calendar.YEAR)).append('-').append(calendar.get(Calendar.MONTH)+1).append('-').append(calendar.get(Calendar.DAY_OF_MONTH))
                .append(' ').append(calendar.get(Calendar.HOUR_OF_DAY)).append(':').append(calendar.get(Calendar.MINUTE)).append(':').append(calendar.get(Calendar.SECOND));
        int zoneOffsetMinutes = calendar.get(Calendar.ZONE_OFFSET) / (60 * 1000);
        if (zoneOffsetMinutes >= 0) {
            builder.append('+');
        }
        builder.append(zoneOffsetMinutes / 60).append(':').append(zoneOffsetMinutes % 60);
    }

    private static void formatCalendarAsDate(Calendar calendar, StringBuilder builder) {
        builder.append(calendar.get(Calendar.YEAR)).append('-').append(calendar.get(Calendar.MONTH)+1).append('-').append(calendar.get(Calendar.DAY_OF_MONTH));
    }

    private static void doTimestampFormatNoZone(Date date, StringBuilder builder) {
        synchronized (timestampFormatNoZone) {
            builder.append(timestampFormatNoZone.format(date));
        }
    }

    private static void doDateFormat(Date date, StringBuilder builder) {
        synchronized (dateFormat) {
            builder.append(dateFormat.format(date));
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static void handleInapplicableValue(String formatterName, Object value) {
        String msg = "Formatter named " + formatterName + " cannot be applied to object of class " + value.getClass().getName();
        throw new IllegalArgumentException(msg);
    }

    @SuppressWarnings("SameParameterValue")
    private static void registerStringFormatter(LinkedHashMap<String, ValueFormatter> registryMap) {
        registerFormatter("string", String.class, string -> "'" + ((String) string).replace("'", "''") + "'", registryMap);
    }

    @SuppressWarnings("SameParameterValue")
    private static void registerNumberFormatter(LinkedHashMap<String, ValueFormatter> registryMap) {
        registerFormatter("number", Number.class, Object::toString, registryMap);
    }

    /**
     * Use the given formatter to return an SQL literal that will represent that object in the SQL if the object is an
     * instance of the given class.
     *
     * @param name      The name of this formatter. If the given name is specified with a smileyVar, then this formatter
     *                  will be used to format the smileyVar's value; otherwise the formatter will be used if it is the
     *                  first one registered whose class or predicate match the value of the smileyVar.
     * @param clazz     The class that a value must be an instance of for the given formatter function to be used to
     *                  format it.
     * @param formatter A function to return a representation of an object as an SQL literal.
     * @return this object
     */
    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    ValueFormatterRegistry registerFormatter(String name, Class clazz, Function<Object, String> formatter) {
        return registerFormatter(name, clazz::isInstance, formatter);
    }

    /**
     * If the given predicate returns true when passed the value of a Smiley Var, then use the given formatter to return
     * an SQL literal that will represent that object in the SQL.
     *
     * @param name      The name of this formatter. If the given name is specified with a smileyVar, then this formatter
     *                  will be used to format the smileyVar's value; otherwise the formatter will be used if it is the
     *                  first one registered whose class or predicate match the value of the smileyVar.
     * @param predicate The predicate to determine if the formatter can be applied to a given value.
     * @param formatter A function to return a representation of an object as an SQL literal.
     * @return this object
     */
    @SuppressWarnings("WeakerAccess")
    ValueFormatterRegistry registerFormatter(String name, Predicate<Object> predicate, Function<Object, String> formatter) {
        registerFormatter(name, predicate, formatter, formatterMap);
        return this;
    }

    private static void registerFormatter(String name, Class clazz,
                                          Function<Object, String> formatter,
                                          LinkedHashMap<String, ValueFormatter> map) {
        map.put(name, new ValueFormatter(clazz::isInstance, formatter, name));
    }

    private static void registerFormatter(String name, Predicate<Object> predicate,
                                          Function<Object, String> formatter,
                                          LinkedHashMap<String, ValueFormatter> map) {
        map.put(name, new ValueFormatter(predicate, formatter, name));
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
    String format(Object value) {
        if (value == null) {
            return null;
        }
        for (ValueFormatter valueFormatter : formatterMap.values()) {
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
    String format(Object value, String formatterName) {
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
