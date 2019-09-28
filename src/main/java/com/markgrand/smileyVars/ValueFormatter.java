package com.markgrand.smileyVars;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Format objects that are values of smileyVars as SQL literals. If you want SmileyVars to format objects
 * that are instance of a class that is not a number, string or date.
 */
public class ValueFormatter {
    final Predicate<Object> appliesTo;
    final Function<Object, String> formattingFunction;

    /**
     * Constructor
     *
     * @param predicate This should return true if, and only if, its argument is something that the formattingFunction can format.
     * @param formattingFunction If given a value that the predicate returns true for, this should return a string that
     *                           represents to given value as an SQL literal.
     */
    public ValueFormatter(Predicate<Object> predicate, Function<Object, String> formattingFunction) {
        appliesTo = predicate;
        this.formattingFunction = formattingFunction;
    }

    /**
     * Return {@code true} if the this object can formatting the given value, otherwise {@code false}.
     *
     * @param value an object to be formatted as an SQL literal.
     * @return {@code true} if the this object can formatting the given value, otherwise {@code false}.
     */
    public boolean isApplicable(Object value) {
        return appliesTo.test(value);
    }

    public String format(Object value) {
        return formattingFunction.apply(value);
    }
}
