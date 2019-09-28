package com.markgrand.smileyVars;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton to maintain a registry of {@link ValueFormatter} objects
 */
@SuppressWarnings("WeakerAccess")
public class ValueFormatterRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ValueFormatterRegistry.class);
    private static List<ValueFormatter> formatterList = new LinkedList<>();

    // Initialize with built-in ValueFormatter objects
    static {
        logger.debug("Registering built-in formatters.");
        registerFormatter(Number.class, Object::toString);
        registerFormatter(String.class, string->"'" + ((String)string).replace("'", "''")+ "'");
        //TODO add formatter for BitSet, Date, Time, Calendar, Timestamp, Duration, Money, unique identifier/GUID, boolean
        //TODO need to account for national character set string literals and unicode string literals.
        logger.debug("Done registering built-in formatters.");
    }

    /**
     * Use the given formatter to return an SQL literal that will represent that object in the SQL if the object is an
     * instance of the given class.
     *
     * @param clazz     The class that a value must be an instance of for the given formatter function to be used to
     *                  format it.
     * @param formatter A function to return a representation of an object as an SQL literal.
     */
    public static void registerFormatter(Class clazz, Function<Object, String> formatter) {
       registerFormatter(clazz::isInstance, formatter);
    }

    /**
     * If the given predicate returns true when passed the value of a Smiley Var, then use the given formatter to return
     * an SQL literal that will represent that object in the SQL.
     *
     * @param predicate The predicate to determine if the formatter can be applied to a given value.
     * @param formatter A function to return a representation of an object as an SQL literal.
     */
    public static void registerFormatter(Predicate<Object> predicate, Function<Object, String> formatter) {
        formatterList.add(new ValueFormatter(predicate, formatter));
    }

    /**
     * Format the given object as an string that is an SQL literal that represents the given object.
     * @param value the object to be represented as an SQL literal.
     * @return the SQL literal as a String or null if the value is null.
     * @throws NoFormatterException if there is no registered applicable formatter.
     */
    public static String format(Object value) {
        if (value == null) {
            return null;
        }
        for (ValueFormatter valueFormatter : formatterList) {
            if (valueFormatter.isApplicable(value)) {
                return valueFormatter.format(value);
            }
        }
        throw new NoFormatterException("No registered formatter for value that is an instance of " + value.getClass().getName());
    }
}
