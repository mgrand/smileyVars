package com.markgrand.smileyVars.util;

import java.sql.SQLException;

public class VacuousBiSqlConsumer<T, U> implements BiSqlConsumer<T, U> {
    @Override
    public void accept(T t, U u) throws SQLException {
        throw new SQLException("The value for a required SmileyVar is not speciried.");
    }

    @Override
    public boolean isVacuous() {
        return true;
    }
}
