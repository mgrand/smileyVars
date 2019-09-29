package com.markgrand.smileyVars;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Format objects that are values of smileyVars as SQL literals. If you want SmileyVars to format objects
 * that are instance of a class that is not a number, string or date.
 */
class ValueFormatter {
    private final Predicate<Object> appliesTo;
    private final Function<Object, String> formattingFunction;

    /**
     * Constructor
     *
     * @param predicate This should return true if, and only if, its argument is something that the formattingFunction can format.
     * @param formattingFunction If given a value that the predicate returns true for, this should return a string that
     *                           represents to given value as an SQL literal.
     */
    ValueFormatter(Predicate<Object> predicate, Function<Object, String> formattingFunction) {
        appliesTo = predicate;
        this.formattingFunction = formattingFunction;
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

    String format(Object value) {
        return formattingFunction.apply(value);
    }
}
