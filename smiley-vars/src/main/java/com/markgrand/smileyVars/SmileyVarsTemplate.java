package com.markgrand.smileyVars;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * <p>SmileyVars is a lightweight template engine for SQL. It helps you avoid having to write similar SQL many times
 * because simple variations are needed.</p>
 * <p>Suppose we have a table that tracks the content of bins in a warehouse. Suppose that bins are identified by
 * <code>aisle</code>, <code>level</code> and <code>bin_number</code>. A query to get information about the contents of
 * one bin might look like</p>
 * <pre>
 * SELECT item_number, quantity FROM bin_tbl
 * WHERE aisle=:aisle and level=:level and bin_number=:bin
 * </pre>
 * <p>The first thing that you might notice about this example is that the value to be substituted into the SQL are
 * indicated by a name prefixed by "<tt>:</tt>". If we provide the values <code>aisle=32</code>, <code>level=4</code>
 * and <code>bin=17</code> this will expand to</p>
 * <pre>
 * SELECT item_number, quantity FROM bin_tbl
 * WHERE aisle=32 and level=4 and bin_number=17
 * </pre>
 * <p>Suppose that we would like to use the same SQL even for cases were we want to retrieve multiple rows. We could
 * write</p>
 * <pre>
 * SELECT item_number, quantity FROM bin_tbl
 * WHERE aisle=:aisle (: and level=:level :) (: and bin_number=:bin :)
 * </pre>
 * <p>What we have done is to bracket two parts of the query between <tt>(:</tt> and <tt>:)</tt>. When a portion of SQL
 * is bracketed this way, if the bracketed portion contains any :<i>variables</i> and values are not supplied for all of
 * the :<i>variables</i>, then that portion of the SQL is not included in the expansion. If all of the values are
 * supplied for the above example then it will expand to exactly the same SQL as the previous example. However, if we
 * supply just the values <code>aisle=32</code> and <code>bin=17</code> with no value for <code>bin</code>, it expands
 * to</p>
 * <pre>
 * SELECT item_number, quantity FROM bin_tbl
 * WHERE aisle=32 and bin_number=17
 * </pre>
 * <p>If we supply just <code>aisle=32</code>, it expands to</p>
 * <pre>
 * SELECT item_number, quantity FROM bin_tbl
 * WHERE aisle=32
 * </pre>
 * <p>What if we wanted to also have the flexibility of not specifying <code>aisle</code>? Just bracketing that part of
 * the WHERE clause <b><i>does not work</i></b>:</p>
 * <pre>
 * SELECT item_number, quantity FROM bin_tbl
 * WHERE (: aisle=:aisle :) (: and level=:level :) (: and bin_number=:bin :)
 * </pre>
 * <p>If the first bracketed portion of this query is not in the expansion, it is not valid SQL. There is a simple
 * syntactic trick that we can use to avoid this issue. We can begin the <code>WHERE</code> clause with <code>1=1</code>
 * like this:</p>
 * <pre>
 * SELECT item_number, quantity FROM bin_tbl
 * WHERE 1=1 (: and aisle=:aisle :) (: and level=:level :) (: and bin_number=:bin :)
 * </pre>
 * <p>This form of the SQL query allows us to supply all, some or none of the
 * values and have it expand to a valid SQL query.</p>
 * <p>One thing to notice about this query is that the <code>SELECT</code> list does not include the
 * <code>aisle</code>, <code>level</code> or <code>bin_number</code> columns. Because of this, when we get the results
 * of the query, we do not know which bin result rows are associated with.</p>
 * <p>A reasonable way to solve this problem is to just add those columns to the select list like this:</p>
 * <pre>
 * SELECT item_number, quantity, aisle, level, bin_number FROM bin_tbl
 * WHERE 1=1 (: and aisle=:aisle :) (: and level=:level :) (: and bin_number=:bin :)
 * </pre>
 * <p>For more SmileyVars documentation, see <a href="https://github.com/mgrand/smileyVars">https://github.com/mgrand/smileyVars</a>.</p>
 *
 * @author Mark Grand
 */
public class SmileyVarsTemplate {
    private static final Logger logger = LoggerFactory.getLogger(SmileyVarsTemplate.class);

    private final Tokenizer.TokenizerBuilder builder;
    private final String sql;
    private final ValueFormatterRegistry formatterRegistry;

    /**
     * Constructor
     */
    private SmileyVarsTemplate(@NotNull String sql, @NotNull Tokenizer.TokenizerBuilder builder, @NotNull ValueFormatterRegistry formatterRegistry) {
        this.builder = builder;
        this.sql = sql;
        this.formatterRegistry = formatterRegistry;
    }

    /**
     * Create a template for the given type of database.
     *
     * @param databaseType The type of database that this template is for.
     * @param sql          The template body.
     * @return the template.
     */
    @SuppressWarnings("WeakerAccess")
    @NotNull
    public static SmileyVarsTemplate template(@NotNull DatabaseType databaseType, @NotNull String sql) {
        return new SmileyVarsTemplate(sql, databaseType.getTokenizerBuilder(), databaseType.getValueFormatterRegistry());
    }

    /**
     * Create a template for the type of database associated with the given data source.
     *
     * @param conn a database connection to the database that the template will be used with.
     * @param sql  The template body.
     * @return the template.
     * @throws SQLException if there is a problem using the connection to determine the type of database being used.
     */
    @SuppressWarnings("WeakerAccess")
    @NotNull
    public static SmileyVarsTemplate template(@NotNull Connection conn, @NotNull String sql) throws SQLException {
        return template(DatabaseType.inferDatabaseType(conn.getMetaData()), sql);
    }

    /**
     * Create a template for the type of database associated with the given data source.
     *
     * @param ds  the data source that the template will be used with.
     * @param sql The template body.
     * @return the template.
     * @throws SQLException if there is a problem getting a connection from the data source or if there is a problem
     *                      using the connection to determine the type of database.
     */
    @org.jetbrains.annotations.NotNull
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static SmileyVarsTemplate template(@NotNull DataSource ds, @NotNull String sql) throws SQLException {
        try (Connection conn = ds.getConnection()) {
            return template(conn, sql);
        }
    }

    /**
     * <p>Create a template for ANSI-compliant SQL. ANSI SQL is a standard that all relational databases conform to to
     * some extent. This type of template is best when you are planning to write SQL that is portable between different
     * database engines or if you are working with a relational database for which there is no specific template
     * creation method.</p>
     * <p>ANSI templates ignore any smileyVars that are inside of string literals, quoted identifiers or comments. Note
     * that ANSI templates recognize nested comments like this:</p>
     * <code>&#47;* This is &#47;* all one *&#47; big comment *&#47;</code>
     *
     * @param sql The template body.
     * @return the template.
     * @deprecated Use {@code SmileyVarsTemplate.template(DatabaseType.ANSI, sql)}
     */
    @SuppressWarnings("unused")
    @Deprecated
    @NotNull
    public static SmileyVarsTemplate ansiTemplate(@NotNull String sql) {
        return template(DatabaseType.ANSI, sql);
    }

    /**
     * Create a template for PostgreSQL SQL. This includes support for boolean smileyVars values. It is also able to
     * parse dollar sign delimited string literals and escaped string literals.
     *
     * @param sql The template body.
     * @return the template.
     * @deprecated Use {@code SmileyVarsTemplate.template(DatabaseType.POSTGRESQL, sql)}
     */
    @org.jetbrains.annotations.NotNull
    @SuppressWarnings({"unused", "WeakerAccess"})
    @Deprecated
    public static SmileyVarsTemplate postgresqlTemplate(String sql) {
        return template(DatabaseType.POSTGRESQL, sql);
    }

    /**
     * Create a template for Oracle SQL. This is able to parse Oracle delimited string literals.  Like Oracle, it does
     * not recognize nested comments.
     *
     * @param sql The template body.
     * @return the template.
     * @deprecated Use {@code SmileyVarsTemplate.template(DatabaseType.ORACLE, sql)}
     */
    @org.jetbrains.annotations.NotNull
    @SuppressWarnings({"unused", "WeakerAccess"})
    @Deprecated
    public static SmileyVarsTemplate oracleTemplate(String sql) {
        return template(DatabaseType.ORACLE, sql);
    }

    /**
     * Create a template for Transact SQL (SQL Server). Like SqlServer, it recognizes identifiers that are quoted with
     * square brackets.
     *
     * @param sql The template body.
     * @return the template.
     * @deprecated Use {@code SmileyVarsTemplate.template(DatabaseType.SQL_SERVER, sql)}
     */
    @NotNull
    @SuppressWarnings({"unused", "WeakerAccess"})
    @Deprecated
    public static SmileyVarsTemplate sqlServerTemplate(String sql) {
        return template(DatabaseType.SQL_SERVER, sql);
    }

    /**
     * Apply the values in the given Map to this template.
     *
     * @param values Apply the given values to this template
     * @return the template
     * @throws NoFormatterException        if there is no applicable formatter registered to format a variable's value.
     * @throws UnsupportedFeatureException if the template uses a smileyVars feature that is not yet supported.
     */
    @org.jetbrains.annotations.NotNull
    @SuppressWarnings("unused")
    public String apply(@NotNull Map<String, Object> values) throws UnsupportedFeatureException {
        if (logger.isDebugEnabled()) {
            logger.debug("Expanding \"" + sql + "\" with mappings: " + values);
        }
        @NotNull Tokenizer tokenizer = builder.build(sql);
        @Nullable StringBuilder segment = new StringBuilder(sql.length() * 2);
        @NotNull Stack<StringBuilder> stack = new Stack<>();
        while (tokenizer.hasNext()) {
            Token token = tokenizer.next();
            switch (token.getTokenType()) {
                case TEXT:
                    processText(segment, token);
                    break;
                case VAR:
                    segment = processVar(values, tokenizer, segment, token, stack);
                    break;
                case SMILEY_OPEN:
                    segment = processBracketOpen(segment, stack);
                    break;
                case SMILEY_CLOSE:
                    segment = processBracketClose(segment, stack);
                    break;
            }
        }
        return finalizeExpansion(segment, stack);
    }

    private void processText(@Nullable StringBuilder segment, @org.jetbrains.annotations.NotNull Token token) {
        if (segment != null) {
            segment.append(token.getTokenchars());
        }
    }

    @Nullable
    private StringBuilder processVar(@NotNull Map<String, Object> values, @NotNull Tokenizer tokenizer,
                                     @Nullable StringBuilder segment, @NotNull Token token, @NotNull Stack<StringBuilder> stack) {
        if (segment != null) {
            segment = doVarExpansion(values, tokenizer, segment, token);
            if (segment == null && stack.isEmpty()) {
                throw new UnboundVariableException("No value is provided for :" + token.getTokenchars());
            }
        }
        return segment;
    }

    private StringBuilder processBracketOpen(StringBuilder segment, @org.jetbrains.annotations.NotNull Stack<StringBuilder> stack) {
        stack.push(segment);
        segment = new StringBuilder();
        return segment;
    }

    private StringBuilder processBracketClose(StringBuilder segment, @org.jetbrains.annotations.NotNull Stack<StringBuilder> stack) {
        if (stack.isEmpty()) {
            logger.warn("SmileyVars template has an extra close bracket: {}", sql);
        } else {
            StringBuilder subSegment = segment;
            segment = stack.pop();
            if (subSegment != null) {
                segment.append(subSegment);
            }
        }
        return segment;
    }

    @org.jetbrains.annotations.NotNull
    private String finalizeExpansion(@Nullable StringBuilder segment, @org.jetbrains.annotations.NotNull Stack<StringBuilder> stack) {
        if (segment == null) {
            segment = new StringBuilder();
        }
        while (!stack.isEmpty()) {
            @Nullable StringBuilder subSegment = segment;
            segment = stack.pop();
            if (segment != null) {
                segment.append(subSegment);
            } else {
                segment = new StringBuilder();
            }
        }
        return segment.toString();
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
    @Nullable
    private StringBuilder doVarExpansion(@NotNull Map<String, Object> values, @NotNull Tokenizer tokenizer,
                                         @NotNull StringBuilder segment, @NotNull Token varToken) {

        String value = getVarValue(values, tokenizer, varToken);
        if (value == null) {
            skipToSmileyClose(tokenizer);
            return null;
        } else {
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
     * @return The value of the variable formatted as an SQL literal or null if the variable does not have a value.
     * @throws NoFormatterException if there is no applicable formatter registered to format the variable's value.
     */
    private String getVarValue(@NotNull Map<String, Object> values, @NotNull Tokenizer tokenizer, @NotNull Token varToken) {
        @NotNull String varName = varToken.getTokenchars();
        logger.debug("Formatting variable {}", varName);
        if (tokenizer.peek() == TokenType.VAR) {
            @NotNull String typeName = tokenizer.next().getTokenchars();
            logger.debug("Found type {}", typeName);
            return formatterRegistry.format(values.get(varName), typeName);
        }
        //No explicit type given for variable, so let the formatter predicates identify the correct formatter to use.
        if (values.containsKey(varName)) {
            return formatterRegistry.format(values.get(varName));
        } else {
            logger.debug("No value provided for {}", varName);
            return null;
        }
    }

    private void skipToSmileyClose(@org.jetbrains.annotations.NotNull Tokenizer tokenizer) {
        while (tokenizer.hasNext()) {
            TokenType tokenType = tokenizer.peek();
            if (TokenType.SMILEY_CLOSE.equals(tokenType) || TokenType.EOF.equals(tokenType)) {
                return;
            }
            // Ignore next token.
            tokenizer.next();
        }
    }

    /**
     * Get the names of the variables in this SmileyVars template.
     *
     * @return the name of the variables as a Set. Each call to this method will return a new Set object.
     */
    public Set<String> getVarNames() {
        final Set<String> varNames = new HashSet<>();
        @NotNull Tokenizer tokenizer = builder.build(sql);
        while (tokenizer.hasNext()) {
            Token token = tokenizer.next();
            if (TokenType.VAR.equals(token.getTokenType())) {
                varNames.add(token.getTokenchars());
            }
        }
        return varNames;
    }

    /**
     * Get the String that this template is based on.
     * @return the String that this template is based on.
     */
    public String getTemplateString() {
        return sql;
    }
}
