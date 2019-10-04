package com.markgrand.smileyVars;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Singleton to maintain a registry of {@link ValueFormatter} objects
 */
class ValueFormatterRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ValueFormatterRegistry.class);

    private static final ValueFormatterRegistry ansiRegistry = new ValueFormatterRegistry();
    private static final ValueFormatterRegistry postgresqlRegistry
            = new ValueFormatterRegistry().registerFormatter("boolean", Boolean.class, bool->((Boolean)bool).toString());

    private final LinkedHashMap<String, ValueFormatter> formatterMap = new LinkedHashMap<>();

    static ValueFormatterRegistry ansiInstance() { return ansiRegistry; }
    static ValueFormatterRegistry postgresqlInstance() { return postgresqlRegistry; }

    private ValueFormatterRegistry() {
        logger.debug("Registering built-in formatters.");
        registerFormatter("number", Number.class, Object::toString);
        registerFormatter("string", String.class, string->"'" + ((String)string).replace("'", "''")+ "'");
        //TODO add formatter for BitSet, Date, Time, Calendar, Timestamp, Duration, Money, unique identifier/GUID, boolean
        //TODO need to account for national character set string literals and unicode string literals.
        logger.debug("Done registering built-in formatters.");
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
    @SuppressWarnings("WeakerAccess")
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
        formatterMap.put(name, new ValueFormatter(predicate, formatter, name));
        return this;
    }

    /**
     * Format the given object as an string that is an SQL literal that represents the given object.
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
}
