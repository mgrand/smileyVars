package com.markgrand.smileyVars;

/**
 * A template for SQL that expands to leave out extraneous portions of a query.
 *
 * @author Mark Grand
 */
public class SmileyVarsTemplate {
    private final String sql;

    /**
     * Constructor
     *
     * @param sql The template body.
     */
    public SmileyVarsTemplate(String sql) {
        this.sql = sql;
    }
}
