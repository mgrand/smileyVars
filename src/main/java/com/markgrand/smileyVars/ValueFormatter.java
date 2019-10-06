package com.markgrand.smileyVars;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Format objects that are values of smileyVars as SQL literals. If you want SmileyVars to format objects that are
 * instance of a class that is not a number, string or date.
 */
class ValueFormatter {
    private final Predicate<Object> appliesTo;
    private final Function<Object, String> formattingFunction;
    private final String name;

    /**
     * Constructor
     *
     * @param predicate          This should return true if, and only if, its argument is something that the
     *                           formattingFunction can format.
     * @param formattingFunction If given a value that the predicate returns true for, this should return a string that
     *                           represents to given value as an SQL literal.
     */
    ValueFormatter(Predicate<Object> predicate, Function<Object, String> formattingFunction, String name) {
        appliesTo = predicate;
        this.formattingFunction = formattingFunction;
        this.name = name;
    }

    /**
     * Return {@code true} if the this object can formatting the given value, otherwise {@code false}.
     *
     * @param value an object to be formatted as an SQL literal.
     * @return {@code true} if the this object can formatting the given value, otherwise {@code false}.
     */
    boolean isApplicable(Object value) {
        return appliesTo.test(value);
    }

    /**
     * Format the given value as an SQL literal.
     * <p><b>Note:</b> If {@link #isApplicable(Object)} returns false for this value, then the result of calling this
     * method is undefined.</p>
     *
     * @param value The value to be formatted asn an SQL literal
     * @return the formatted string.
     */
    String format(Object value) {
        return formattingFunction.apply(value);
    }

    /**
     * Get the description of this formatter.
     *
     * @return the description.
     */
    String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ValueFormatter{" +
                       "name='" + getName() + '\'' +
                       '}';
    }
}
