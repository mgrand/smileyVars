package com.markgrand.smileyVars.util;

import java.sql.SQLException;

/**
 * Class to use for representing a missing value in a {@link com.markgrand.smileyVars.SmileyVarsPreparedStatement}
 *
 * @param <T> the first parameter type
 * @param <U> the second parameter type
 */
public class VacuousBiSqlConsumer<T, U> implements BiSqlConsumer<T, U> {
    public static <T, U> VacuousBiSqlConsumer<T, U> getInstance() {
        return new VacuousBiSqlConsumer<>();
    }

    @Override
    public void accept(T t, U u) throws SQLException {
        throw new SQLException("The value for a required SmileyVar is not specified.");
    }

    /**
     * Return false to indicate that this object represents a missing value.
     *
     * @return false to indicate that this object represents a missing value.
     */
    @Override
    public boolean isVacuous() {
        return true;
    }
}
