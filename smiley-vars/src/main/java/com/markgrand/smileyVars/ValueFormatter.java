package com.markgrand.smileyVars;

import com.markgrand.smileyVars.util.PreparedStatementSetter;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Format objects that are values of smileyVars as SQL literals. If you want SmileyVars to format objects that are
 * instance of a class that is not a number, string or date.
 */
class ValueFormatter {
    private final Predicate<Object> isDefaultFor;
    private final Predicate<Object> appliesTo;
    private final Function<Object, String> formattingFunction;
    private final PreparedStatementSetter preparedStatementSetter;
    private final String name;

    /**
     * Constructor
     *
     * @param isDefaultFor       Predicate that returns true if this formatter is the default formatter for the given
     *                           type of object.
     * @param appliesTo          Predicate that returns true if, and only if, its argument is something that the
     *                           formattingFunction can format.
     * @param formattingFunction If given a value that appliesTo returns true for, this should return a string that
     *                           represents to given value as an SQL literal.
     * @param name               The name of this formatter.
     */
    ValueFormatter(Predicate<Object> isDefaultFor,
                   Predicate<Object> appliesTo,
                   Function<Object, String> formattingFunction,
                   PreparedStatementSetter preparedStatementSetter,
                   String name) {
        this.isDefaultFor = isDefaultFor;
        this.appliesTo = appliesTo;
        this.formattingFunction = formattingFunction;
        this.preparedStatementSetter = preparedStatementSetter;
        this.name = name;
    }

    /**
     * Return {@code true} if this object can format the given value, otherwise {@code false}.
     *
     * @param value an object to be formatted.
     * @return {@code true} if this object can format the given value, otherwise {@code false}.
     */
    boolean isDefaultFor(Object value) { return isDefaultFor.test(value); }

    /**
     * Return {@code true} if this object can format the given value, otherwise {@code false}.
     *
     * @param value an object to be formatted.
     * @return {@code true} if the this object can formatting the given value, otherwise {@code false}.
     */
    boolean isApplicable(Object value) {
        return isDefaultFor(value)  || appliesTo.test(value);
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
     * Set a prepared statement parameter.
     *
     * @param preparedStatement The PreparedStatement whose parameter is to be set.
     * @param i                 The index of the parameter to set
     * @param value             The value to set the parameter to.
     * @throws SmileyVarsSqlException if given PreparedStatement object throws an SQLException.
     */
    void setPreparedStatementParameter(PreparedStatement preparedStatement, Integer i, Object value) {
        try {
            preparedStatementSetter.apply(preparedStatement, i, value);
        } catch (SQLException e) {
            throw new SmileyVarsSqlException("Error setting value of varaible named " + name, e);
        }
    }

    /**
     * Get the description of this formatter.
     *
     * @return the description.
     */
    @SuppressWarnings("WeakerAccess")
    String getName() {
        return name;
    }

    @NotNull
    @Override
    public String toString() {
        return "ValueFormatter{" +
                       "name='" + getName() + '\'' +
                       '}';
    }
}
