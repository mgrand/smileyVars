package com.markgrand.smileyVars;

import java.util.Map;

/**
 * A template for SQL that expands to leave out extraneous portions of a query.
 *
 * @author Mark Grand
 */
public class SmileyVarsTemplate {
    private final Tokenizer.TokenizerBuilder builder;
    private final String sql;
    private final ValueFormatterRegistry formatterRegistry;

    /**
     * Constructor
     */
    private SmileyVarsTemplate(String sql, Tokenizer.TokenizerBuilder builder, ValueFormatterRegistry formatterRegistry) {
        this.builder = builder;
        this.sql = sql;
        this.formatterRegistry = formatterRegistry;
    }

    /**
     * Create a template for ANSI-compliant SQL
     *
     * @param sql The template body.
     * @return the template.
     */
    public static SmileyVarsTemplate ansiTemplate(String sql) {
        return new SmileyVarsTemplate(sql, Tokenizer.builder().configureForAnsi(), ValueFormatterRegistry.ansiiInstance());
    }

    /**
     * Create a template for PostgreSQL SQL
     *
     * @param sql The template body.
     * @return the template.
     */
    public static SmileyVarsTemplate postgresqlTemplate(String sql) {
        return new SmileyVarsTemplate(sql, Tokenizer.builder().configureForPostgresql(), ValueFormatterRegistry.ansiiInstance());
    }

    /**
     * Create a template for Oracle SQL
     *
     * @param sql The template body.
     * @return the template.
     */
    public static SmileyVarsTemplate oracleTemplate(String sql) {
        return new SmileyVarsTemplate(sql, Tokenizer.builder().configureForOracle(), ValueFormatterRegistry.ansiiInstance());
    }

    /**
     * Create a template for Transact SQL (SQL Server)
     *
     * @param sql The template body.
     * @return the template.
     */
    public static SmileyVarsTemplate sqlServerTemplate(String sql) {
        return new SmileyVarsTemplate(sql, Tokenizer.builder().configureForSqlServer(), ValueFormatterRegistry.ansiiInstance());
    }

    /**
     * Apply the values in the given Map to this template.
     *
     * @param values Apply the given values to this template
     * @return the template
     */
    public String apply(Map<String, Object> values) {
        Tokenizer tokenizer = builder.build(sql);
        StringBuilder sb = new StringBuilder(sql.length()*2);
        StringBuilder segment = null;
        while (tokenizer.hasNext()) {
            Token token = tokenizer.next();
            switch (token.getTokenType()) {
                case EOF:
                    return sb.toString();
                case TEXT:
                    ((segment != null)? segment : sb).append(token.getTokenchars());
                    break;
                case VAR:
                    String value = formatterRegistry.format(values.get(token.getTokenchars()));
                    if (value == null) {
                        skipPastSmileyClose(tokenizer);
                    } else {
                        assert segment != null;
                        segment.append(value);
                    }
                    break;
                case SMILEY_OPEN:
                    segment = new StringBuilder();
                    break;
                case SMILEY_CLOSE:
                    sb.append(segment);
                    segment = null;
                    break;
            }
        }
        return sb.toString();
    }

    private void skipPastSmileyClose(Tokenizer tokenizer) {
        while (tokenizer.hasNext()) {
            TokenType tokenType = tokenizer.next().getTokenType();
            if (TokenType.SMILEY_CLOSE.equals(tokenType) || TokenType.EOF.equals(tokenType)) {
                return;
            }
        }
    }
}
