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
     * Create a template for ANSI-compliant SQL. ANSI SQL is a standard that all relational databases conform to to some
     * extent. This type of template is best when you are planning to write SQL that is portable between different
     * database engines or if you are working with a relational database for which there is no specific template
     * creation method.
     * <p></p>
     * ANSI templates ignore any smileyVars that are inside of string literals, quoted identifiers or comments. Note
     * that ANSI templates recognize nested comments like this:
     * <code>&#47;* This is &#47;* all one *&#47; big comment *&#47;</code>
     *
     * @param sql The template body.
     * @return the template.
     */
    @SuppressWarnings("unused")
    public static SmileyVarsTemplate ansiTemplate(String sql) {
        return new SmileyVarsTemplate(sql, Tokenizer.builder().configureForAnsi(), ValueFormatterRegistry.ansiInstance());
    }

     /**
     * Create a template for PostgreSQL SQL. This includes support for boolean smileyVars values. It is also able to
     * parse dollar sign delimited string literals and escaped string literals.
     *
     * @param sql The template body.
     * @return the template.
     */
    @SuppressWarnings("unused")
    public static SmileyVarsTemplate postgresqlTemplate(String sql) {
        return new SmileyVarsTemplate(sql, Tokenizer.builder().configureForPostgresql(), ValueFormatterRegistry.postgresqlInstance());
    }

    /**
     * Create a template for Oracle SQL. This is able to parse Oracle delimited string literals.  Like Oracle, it does
     * not recognize nested comments.
     *
     * @param sql The template body.
     * @return the template.
     */
    @SuppressWarnings("unused")
    public static SmileyVarsTemplate oracleTemplate(String sql) {
        return new SmileyVarsTemplate(sql, Tokenizer.builder().configureForOracle(), ValueFormatterRegistry.ansiInstance());
    }

    /**
     * Create a template for Transact SQL (SQL Server). Like SqlServer, it recognizes identifiers that are quoted with
     * square brackets.
     *
     * @param sql The template body.
     * @return the template.
     */
    public static SmileyVarsTemplate sqlServerTemplate(String sql) {
        return new SmileyVarsTemplate(sql, Tokenizer.builder().configureForSqlServer(), ValueFormatterRegistry.ansiInstance());
    }

    /**
     * Apply the values in the given Map to this template.
     *
     * @param values Apply the given values to this template
     * @return the template
     * @throws NoFormatterException if there is no applicable formatter registered to format a variable's value.
     */
    @SuppressWarnings("unused")
    public String apply(Map<String, Object> values) {
        Tokenizer tokenizer = builder.build(sql);
        StringBuilder sb = new StringBuilder(sql.length() * 2);
        StringBuilder segment = null;
        while (tokenizer.hasNext()) {
            Token token = tokenizer.next();
            switch (token.getTokenType()) {
                case EOF:
                    return sb.toString();
                case TEXT:
                    ((segment != null) ? segment : sb).append(token.getTokenchars());
                    break;
                case VAR:
                    segment = doVarExpansion(values, tokenizer, segment, token);
                    break;
                case SMILEY_OPEN:
                    segment = new StringBuilder();
                    break;
                case SMILEY_CLOSE:
                    if (segment!=null) {
                        sb.append(segment);
                        segment = null;
                    }
                    break;
            }
        }
        if (segment!=null) {
            sb.append(segment);
        }
        return sb.toString();
    }

    /**
     * Replace a variable with its value formatted as an SQL literal.
     *
     * @param values    A map of variable names to their assigned value.
     * @param tokenizer The tokenizer to use for getting an explicit formatter name if given.
     * @param segment   A {@link StringBuilder} that is being used to build the expansion of the template.
     * @param varToken  The token that is the variable.
     * @return segment if the variable has a value; otherwise null.
     * @throws NoFormatterException if there is no applicable formatter registered to format the variable's value.
     */
    private StringBuilder doVarExpansion(Map<String, Object> values, Tokenizer tokenizer, StringBuilder segment, Token varToken) {
        String value = getVarValue(values, tokenizer, varToken);
        if (value == null) {
            skipPastSmileyClose(tokenizer);
            return null;
        } else {
            assert segment != null;
            segment.append(value);
            return segment;
        }
    }

    /**
     * Get the formatted value of the of the given variable. The formatting used is determined by the formatter name
     * that follows the variable. Alternatively, if the variable is not followed by a formatter name, then a formatter
     * is chosen by searching for one whose {@code isAvailable()} method returns true for the value of the given
     * variable.
     *
     * @param values    A map of variable names to their assigned value.
     * @param tokenizer The tokenizer to use for getting an explicit formatter name if given.
     * @param varToken  The token that is the variable.
     * @return The value of the variable formatted as an SQL literal
     * @throws NoFormatterException if there is no applicable formatter registered to format the variable's value.
     */
    private String getVarValue(Map<String, Object> values, Tokenizer tokenizer, Token varToken) {
        String varName = varToken.getTokenchars();
        if (tokenizer.peek() == TokenType.VAR) {
            String typeName = tokenizer.next().getTokenchars();
            return formatterRegistry.format(values.get(varName), typeName);
        }
        //No explicit type given for variable, so let the formatter predicates identify the correct formatter to use.
        return formatterRegistry.format(values.get(varName));
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
