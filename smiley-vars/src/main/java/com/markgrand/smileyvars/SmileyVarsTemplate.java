package com.markgrand.smileyvars;

import com.markgrand.smileyvars.util.SqlConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * <p>SmileyVars is a lightweight template engine for SQL. It helps you avoid having to write similar SQL many times
 * because simple variations are needed.</p>
 * <p>The SmileyVars template language is documented at
 * <a href="https://mgrand.github.io/smileyVars/">https://mgrand.github.io/smileyVars/</a>.</p>
 *
 * <p>This class expands SmileyVars templates into a {@code String} that can be used with anything that consumes SQL
 * as Strings. There is a related class, {@link SmileyVarsPreparedStatement}, that uses SmileyVars templates with
 * prepared statements. To decide which class you want to use keep these things in mind:</p>
 * <ul>
 * <li>If you want to work with SQL statements, {@link SmileyVarsTemplate} is the one to use. If you want to work
 * with prepared statements, use {@link SmileyVarsPreparedStatement}.</li>
 * <li>{@link SmileyVarsTemplate} can infer the SQL type of the values you provide or you can explicitly include the
 * type of a variable in the template body. {@link SmileyVarsPreparedStatement} requires you to use methods named for
 * Java types to specify values.</li>
 * <li>If you are working with large blobs or clobs, {@link SmileyVarsPreparedStatement} may perform better. It
 * allows you to specify large blob or clob values using write methods that send the value directly to the database
 * without having to represent it as an SQL literal.</li>
 * </ul>
 *
 * @author Mark Grand
 * @see SmileyVarsPreparedStatement
 */
public class SmileyVarsTemplate {
    private static final Logger logger = LoggerFactory.getLogger(SmileyVarsTemplate.class);

    private final Tokenizer.TokenizerBuilder builder;
    @NotNull
    private final String sql;
    @NotNull
    private final ValueFormatterRegistry formatterRegistry;

    private SortedSet<String> varNames;

    /**
     * Constructor for internal use.
     *
     * @param sql               The template body.
     * @param builder           A builder to create a new tokenizer each time the template is to be expanded.
     * @param formatterRegistry The formatter registry to use for formatting SmileyVar values.
     */
    private SmileyVarsTemplate(@NotNull String sql, @NotNull Tokenizer.TokenizerBuilder builder, @NotNull ValueFormatterRegistry formatterRegistry) {
        this.builder = builder;
        this.sql = sql;
        this.formatterRegistry = formatterRegistry;
    }

    /**
     * Create a template for the given type of database, using a specified {@link ValueFormatterRegistry}.
     *
     * @param databaseType      The type of database that this template is for.
     * @param sql               The template body.
     * @param formatterRegistry The formatter registry that this template will use.
     * @return the template.
     */
    @NotNull
    private static SmileyVarsTemplate template(@NotNull DatabaseType databaseType, @NotNull String sql,
                                               @NotNull ValueFormatterRegistry formatterRegistry) {
        return new SmileyVarsTemplate(sql, databaseType.getTokenizerBuilder(), formatterRegistry);
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
        return template(databaseType, sql, databaseType.getValueFormatterRegistry());
    }

    /**
     * Create a template for the given type of database.
     *
     * @param conn a database connection to the database that the template will be used with.
     * @param sql  The template body.
     * @return the template.
     */
    @NotNull
    static SmileyVarsTemplate template(@NotNull Connection conn, @NotNull String sql,
                                       @NotNull ValueFormatterRegistry formatterRegistry) throws SQLException {
        return template(DatabaseType.inferDatabaseType(conn.getMetaData()), sql, formatterRegistry);
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
    @SuppressWarnings({"unused"})
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
    @SuppressWarnings({"unused"})
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
    @SuppressWarnings({"unused"})
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
    public String apply(@NotNull Map<String, ?> values) {
        if (logger.isDebugEnabled()) {
            logger.debug("Expanding \"{}\" with mappings: {}", sql, values);
        }
        @NotNull Tokenizer tokenizer = builder.build(sql);
        @Nullable StringBuilder segment = new StringBuilder(sql.length() * 2);
        @NotNull Deque<StringBuilder> stack = new ArrayDeque<>();
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
                case EOF:
                    // Ignore EOF
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
    private StringBuilder processVar(@NotNull Map<String, ?> values, @NotNull Tokenizer tokenizer,
                                     @Nullable StringBuilder segment, @NotNull Token token, @NotNull Deque<StringBuilder> stack) {
        if (segment != null) {
            segment = doVarExpansion(values, tokenizer, segment, token);
            if (segment == null && stack.isEmpty()) {
                throw new UnboundVariableException("No value is provided for :" + token.getTokenchars());
            }
        }
        return segment;
    }

    private StringBuilder processBracketOpen(StringBuilder segment, @org.jetbrains.annotations.NotNull Deque<StringBuilder> stack) {
        stack.push(segment);
        segment = new StringBuilder();
        return segment;
    }

    private StringBuilder processBracketClose(StringBuilder segment, @org.jetbrains.annotations.NotNull Deque<StringBuilder> stack) {
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
    private String finalizeExpansion(@Nullable StringBuilder segment, @org.jetbrains.annotations.NotNull Deque<StringBuilder> stack) {
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
    private StringBuilder doVarExpansion(@NotNull Map<String, ?> values, @NotNull Tokenizer tokenizer,
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
    private String getVarValue(@NotNull Map<String, ?> values, @NotNull Tokenizer tokenizer, @NotNull Token varToken) {
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
     * Iterate over the variable instances in this template.
     *
     * @param consumer A consumer that will be passed each variable instance in the template exactly once, in order.
     * @throws SQLException if the consumer throws an {@code SQLException}
     */
    void forEachVariableInstance(SqlConsumer<String> consumer) throws SQLException {
        @NotNull Tokenizer tokenizer = builder.build(sql);
        while (tokenizer.hasNext()) {
            Token token = tokenizer.next();
            if (TokenType.VAR.equals(token.getTokenType())) {
                consumer.accept(token.getTokenchars());
            }
        }
    }



    /**
     * Get the names of the variables in this SmileyVars template.
     *
     * @return the names of the variables as an immutable sorted Set.
     */
    @SuppressWarnings("WeakerAccess")
    public SortedSet<String> getVarNames() {
        if (varNames == null) {
            final SortedSet<String> varNameSet = new TreeSet<>();
            try {
                forEachVariableInstance(varNameSet::add);
            } catch (SQLException e) {
                throw new SmileyVarsSqlException("Unexpected SQLException from adding a variable name to a set.", e);
            }
            varNames = Collections.unmodifiableSortedSet(varNameSet);
        }
        return varNames;
    }

    /**
     * Get the String that this template is based on.
     *
     * @return the String that this template is based on.
     */
    @SuppressWarnings("WeakerAccess")
    public String getTemplateString() {
        return sql;
    }

    @Override
    public String toString() {
        return "SmileyVarsTemplate{" +
                       "sql='" + sql + '\'' +
                       ", formatterRegistry=" + formatterRegistry +
                       '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SmileyVarsTemplate)) return false;
        SmileyVarsTemplate that = (SmileyVarsTemplate) o;
        return sql.equals(that.sql) &&
                       formatterRegistry.equals(that.formatterRegistry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sql, formatterRegistry);
    }
}
