package com.markgrand.smileyvars.spring;

import com.markgrand.smileyvars.DatabaseType;
import com.markgrand.smileyvars.MapSetter;
import com.markgrand.smileyvars.SmileyVarsPreparedStatement;
import com.markgrand.smileyvars.SmileyVarsTemplate;
import com.markgrand.smileyvars.util.SqlConsumer;
import com.markgrand.smileyvars.util.SqlFunction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * This is an extension of {@linkplain JdbcTemplate} that supports <a href="https://mgrand.github.io/smileyVars/">SmileyVars
 * Templates</a>.
 *
 * @author Mark Grand
 * @see com.markgrand.smileyvars.SmileyVarsTemplate
 * @see SmileyVarsPreparedStatement
 * <p>
 * {@inheritDoc}
 */
public class SmileyVarsJdbcTemplate extends JdbcTemplate {
    private static final Logger log = LoggerFactory.getLogger(SmileyVarsJdbcTemplate.class);

    private DatabaseType databaseType;

    /**
     * Construct a new JdbcTemplate for bean usage.
     * <p>Note: The DataSource has to be set before using the instance.
     *
     * @see #setDataSource
     */
    @SuppressWarnings("WeakerAccess")
    public SmileyVarsJdbcTemplate() {
        super();
    }

    /**
     * Construct a new JdbcTemplate, given a DataSource to obtain connections from.
     * <p>Note: This will not trigger initialization of the exception translator.
     *
     * @param dataSource the JDBC DataSource to obtain connections from
     */
    @SuppressWarnings("WeakerAccess")
    public SmileyVarsJdbcTemplate(@NotNull DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Construct a new JdbcTemplate, given a DataSource to obtain connections from.
     * <p>Note: Depending on the "lazyInit" flag, initialization of the exception translator
     * will be triggered.
     *
     * @param dataSource the JDBC DataSource to obtain connections from
     * @param lazyInit   whether to lazily initialize the SQLExceptionTranslator
     */
    @SuppressWarnings("WeakerAccess")
    public SmileyVarsJdbcTemplate(@NotNull DataSource dataSource, boolean lazyInit) {
        super(dataSource, lazyInit);
    }

    /**
     * Set the JDBC DataSource to obtain connections from.
     */
    @Override
    public void setDataSource(DataSource dataSource) {
        super.setDataSource(dataSource);
        try {
            //noinspection deprecation
            databaseType = DatabaseType.inferDatabaseType(JdbcUtils.<String>extractDatabaseMetaData(dataSource, "getDatabaseProductName"));
            log.debug("DatabaseType is {}", databaseType);
        } catch (MetaDataAccessException e) {
            throw new DataAccessResourceFailureException("Error while determining type of database.", e);
        }
    }

    /**
     * Create a {@link SmileyVarsPreparedStatement} from the given SQL and pass it to the given function. Here is a
     * usage example:
     * <pre>
     * int quantity
     *    = svjt.withSmileyVars("SELECT quantity FROM inventory WHERE aisle = :aisle AND level = :level AND bin_number = :bin_number",
     *         svps -&lt; {
     *             try (ResultSet rs = svps.setInt("aisle", 4).setInt("level", 1). setInt("bin_number", 7).executeQuery()) {
     *                 return rs.getInt("quantity");
     *             }
     *         }));
     * </pre>
     *
     * @param sql          The sql to be used as a SmileyVars template.
     * @param svpsFunction A function that takes the {@link SmileyVarsPreparedStatement} created from the SQL and
     *                     returns the desired result.
     * @param <T>          The of value to be returned.
     * @return the value that is returned by the given function.
     */
    @SuppressWarnings("unused")
    public <T> T executeSmileyVars(@NotNull String sql, @NotNull SqlFunction<SmileyVarsPreparedStatement, T> svpsFunction) {
        Connection conn = DataSourceUtils.getConnection(obtainDataSource());
        try {
            log.debug("Creating SmileyVarsPreparedStatement from sql: {}", sql);
            try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(conn, sql)) {
                return svpsFunction.apply(svps);
            }
        } catch (SQLException e) {
            throw translateException("WithSmileyVarsPreparedStatement", sql, e);
        } finally {
            DataSourceUtils.releaseConnection(conn, getDataSource());
        }
    }

    /**
     * Query using a {@link SmileyVarsPreparedStatement}. A {@link SmileyVarsPreparedStatement} is created from the
     * given sql. A return value is produced by executing the sql and passing the result set to the given {@link
     * ResultSetExtractor} object. Here is a usage example:
     * <pre>
     *     String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *     List&gt;Inventory&lt; inventoryList = svjt.querySmileyVars(sql, svps-&gt; svps.setInt("aisle", 4).setInt("level", 1), rse);
     * </pre>
     *
     * @param <T> The type of object that the method is expected to produce
     * @param sql    The SQL to use for the SmileyVars template.
     * @param setter a consumer function that sets the values of variables in the SmileVars template.
     * @param rse    a callback that will extract results
     * @return the result object returned by the ResultSetExtractor
     * @throws DataAccessException if there is any problem
     */
    public <T> T querySmileyVars(@NotNull String sql, @NotNull SqlConsumer<SmileyVarsPreparedStatement> setter, @NotNull ResultSetExtractor<T> rse) {
        return executeSmileyVars(sql, (SmileyVarsPreparedStatement svps) -> {
            setter.accept(svps);
            return rse.extractData(svps.executeQuery());
        });
    }

    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given name and value
     * arrays. Execute the expanded SQL as a {@code Statement}. Take the ResultSet that is produced and use the given
     * {@link ResultSetExtractor} to produce the value that will be returned by this method. Here is a usage example:
     * <pre>
     *     String[] names = {"aisle", "level"};
     *     Integer[] values = {4, 1};
     *     String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *     List&lt;Inventory&gt; inventoryList = svjt.querySmileyVars(sql, names, values, rse);
     * </pre>
     *
     * @param sql    The string to use as the SmileyVars template body.
     * @param names  The names of the variables whose values are being specified.
     * @param values The corresponding values to use for the variables in the template body.
     * @param rse    The {@link ResultSetExtractor} to use for extracting a result from the query's result set.
     * @param <T>    The type of value to be returned.
     * @return the value produced by the {@link ResultSetExtractor}.
     * @throws DataAccessException      if there is a problem.
     * @throws IllegalArgumentException if the names and values arrays are not the same length
     */
    public <T> T querySmileyVars(@NotNull String sql, @NotNull String[] names, @NotNull Object[] values, @NotNull ResultSetExtractor<T> rse) {
        ensureEqualLengthArrays(names, values);
        Map<String, Object> valueMap = arraysToMap(names, values);
        return querySmileyVars(sql, valueMap, rse);
    }

    @NotNull
    private Map<String, Object> arraysToMap(String[] names, Object[] values) {
        Map<String, Object> valueMap = new HashMap<>();
        for (int i = 0; i < names.length; i++) {
            valueMap.put(names[i], values[i]);
        }
        return valueMap;
    }

    private void ensureEqualLengthArrays(String[] names, Object[] values) {
        if (names.length != values.length) {
            throw new IllegalArgumentException("Length of names array (" + names.length + ") and length of values array(" + values.length + ") should be the same.");
        }
    }

    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given map. Execute the
     * expanded SQL as a {@code Statement}. Take the ResultSet that is produced and use the given {@link
     * ResultSetExtractor} to produce the value that will be returned by this method. Here is a usage example:
     * <pre>
     *    Map&lt;String, Object&gt; valueMap = new HashMap&lt;&gt;();
     *    valueMap.put("aisle", 4);
     *    valueMap.put("level", 1);
     *    String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *    List&lt;Inventory&gt; inventoryList = svjt.querySmileyVars(sql, valueMap, rse);
     * </pre>
     *
     * @param sql      The string to use as the SmileyVars template body.
     * @param valueMap The values to use for the variables in the template body.
     * @param rse      The {@link ResultSetExtractor} to use for extracting a result from the query's result set.
     * @param <T>      The type of value to be returned.
     * @return the value produced by the {@link ResultSetExtractor}.
     * @throws DataAccessException if there is a problem.
     */
    public <T> T querySmileyVars(String sql, Map<String, ?> valueMap, ResultSetExtractor<T> rse) {
        return super.query(expand(sql, valueMap), rse);
    }

    @NotNull
    private String expand(@NotNull String sql, @NotNull Map<String, ?> valueMap) {
        return SmileyVarsTemplate.template(databaseType, sql).apply(valueMap);
    }

    /**
     * Query using a {@link SmileyVarsPreparedStatement}. A {@code SmileyVarsPreparedStatement} is created from the
     * given sql. The {@code SmileyVarsPreparedStatement} is executed. Each row of the ResultSet that is produced is
     * passed it to the given {@link RowCallbackHandler}. Here is a usage example:
     * <pre>
     *     String sql = "SELECT item_number, quantity FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *     svjt.querySmileyVars(sql, svps-&gt; svps.setInt("aisle", 4).setInt("level", 1), rs-&gt;{
     *         processItemCount(rs.getString("item_number"), rs.getInt("quantity"));
     *     });
     * </pre>
     *
     * @param sql    The SQL to use for the SmileyVars template.
     * @param setter a consumer function that sets the values of variables in the SmileVars template.
     * @param rch    a callback that is called to process each row.
     * @throws DataAccessException if there is any problem
     */
    public void querySmileyVars(@NotNull String sql, @NotNull SqlConsumer<SmileyVarsPreparedStatement> setter, @NotNull RowCallbackHandler rch) {
        executeSmileyVars(sql, (SmileyVarsPreparedStatement svps) -> {
            setter.accept(svps);
            try (ResultSet rs = svps.executeQuery()) {
                while (rs.next()) {
                    rch.processRow(rs);
                }
            }
            return null;
        });
    }

    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given name and value
     * arrays. Execute the expanded SQL as a {@code Statement}. Take the ResultSet that is produced and process each row
     * by passing it to the given {@link RowCallbackHandler}. Here is a usage example:
     * <pre>
     *    String[] names = {"aisle", "level"};
     *    Integer[] values = {4, 1};
     *    int[] count = {0};
     *    String sql = "SELECT item_number, quantity FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *    svjt.querySmileyVars(sql, names, values, rs-$gt; {
     *        processItemCount(rs.getString("item_number"), rs.getInt("quantity"));
     *    });
     * </pre>
     *
     * @param sql    The string to use as the SmileyVars template body.
     * @param names  The names of the variables whose values are being specified.
     * @param values The corresponding values to use for the variables in the template body.
     * @param rch    The {@link RowCallbackHandler} to use for processing each row from the query's result set.
     * @throws DataAccessException      if there is a problem.
     * @throws IllegalArgumentException if the names and values arrays are not the same length
     */
    public void querySmileyVars(@NotNull String sql, @NotNull String[] names, @NotNull Object[] values, @NotNull RowCallbackHandler rch) {
        ensureEqualLengthArrays(names, values);
        Map<String, Object> valueMap = arraysToMap(names, values);
        querySmileyVars(sql, valueMap, rch);
    }

    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given map. Execute the
     * expanded SQL as a {@code Statement}. Execute the expanded SQL as a {@code Statement}. Take the ResultSet that is
     * produced and process each row by passing it to the given {@link RowCallbackHandler}. Here is a usage example:
     * <pre>
     *    Map&lt;String, Object&gt; valueMap = new HashMap&lt;&gt;();
     *    valueMap.put("aisle", 4);
     *    valueMap.put("level", 1);
     *    int[] count = {0};
     *    String sql = "SELECT item_number, quantity FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *    svjt.querySmileyVars(sql, valueMap, rs-&gt;{
     *        processItemCount(rs.getString("item_number"), rs.getInt("quantity"));
     *    });
     * </pre>
     *
     * @param sql      The string to use as the SmileyVars template body.
     * @param valueMap The values to use for the variables in the template body.
     * @param rch      The {@link RowCallbackHandler} to use for processing each row from the query's result set.
     * @throws DataAccessException if there is a problem.
     */
    public void querySmileyVars(@NotNull String sql, @NotNull Map<String, ?> valueMap, @NotNull RowCallbackHandler rch) {
        super.query(expand(sql, valueMap), rch);
    }

    /**
     * Query using a {@link SmileyVarsPreparedStatement}. A {@code SmileyVarsPreparedStatement} is created from the
     * given sql. The {@code SmileyVarsPreparedStatement} is executed. Each row of the ResultSet that is produced is
     * passed it to the given {@link RowMapper}. A list of the results returned by each {@code RowMapper} call is
     * returned. Here is a usage example:
     * <pre>
     *    List&lt;Inventory&gt; inventoryList = svjt.querySmileyVars("SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)",
     *         svps -&gt; svps.setInt("aisle", 4).setInt("level", 1),
     *         rowMapper);
     * </pre>
     *
     * @param <T> The type of object to return in the list.
     * @param sql       The SQL to use for the SmileyVars template.
     * @param setter    a consumer function that sets the values of variables in the SmileVars template.
     * @param rowMapper a callback that is called to process each row into a value.
     * @return a list of the values returned by the calls to the RowMapper.
     * @throws DataAccessException if there is any problem
     */
    @SuppressWarnings("GrazieInspection")
    public <T> List<T> querySmileyVars(@NotNull String sql, @NotNull SqlConsumer<SmileyVarsPreparedStatement> setter, @NotNull RowMapper<T> rowMapper) {
        return querySmileyVars(sql, setter, new RowMapperResultSetExtractor<>(rowMapper));
    }

    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given name and value
     * arrays. Execute the expanded SQL as a {@code Statement}. Each row of the ResultSet that is produced is passed it
     * to the given {@link RowMapper}. A list of the results returned by each {@code RowMapper} call is returned. Here
     * is a usage example:
     * <pre>
     *    String[] names = {"aisle", "level"};
     *    Integer[] values = {4, 1};
     *    String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *    List&lt;Inventory&gt; inventoryList = svjt.querySmileyVars(sql, names, values, rowMapper);
     * </pre>
     *
     * @param <T> The type of object to return in the list.
     * @param sql       The string to use as the SmileyVars template body.
     * @param names     The names of the variables whose values are being specified.
     * @param values    The corresponding values to use for the variables in the template body.
     * @param rowMapper a callback that is called to process each row into a value.
     * @return the result List, containing mapped objects
     * @throws DataAccessException      if there is a problem.
     * @throws IllegalArgumentException if the names and values arrays are not the same length
     */
    @SuppressWarnings("GrazieInspection")
    public <T> List<T> querySmileyVars(@NotNull String sql, @NotNull String[] names, @NotNull Object[] values, @NotNull RowMapper<T> rowMapper) {
        return querySmileyVars(sql, names, values, new RowMapperResultSetExtractor<>(rowMapper));
    }


    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given name and value
     * arrays. Execute the expanded SQL as a {@code Statement}. Each row of the ResultSet that is produced is passed it
     * to the given {@link RowMapper}. A list of the results returned by each {@code RowMapper} call is returned. Here
     * is a usage example:
     * <pre>
     * final String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     * List&lt;Inventory&gt; inventoryList = svjt.querySmileyVars(sql, svps -&gt; svps.setInt("aisle", 4).setInt("level", 1), rowMapper);
     * </pre>
     * Map&lt;String, Object&gt; valueMap = new HashMap&lt;&gt;(); valueMap.put("aisle", 4); valueMap.put("level", 1); String sql =
     * "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     * List&lt;Inventory&gt; inventoryList = svjt.querySmileyVars(sql, valueMap, rowMapper);
     *
     * @param <T> The type of object to be returned in the list.
     * @param sql       The string to use as the SmileyVars template body.
     * @param valueMap  The values to use for the variables in the template body.
     * @param rowMapper a callback that is called to process each row into a value.
     * @return the result object of the required type, or null in case of SQL NULL
     * @throws DataAccessException if there is a problem.
     */
    @SuppressWarnings("GrazieInspection")
    public <T> List<T> querySmileyVars(@NotNull String sql, @NotNull Map<String, Object> valueMap, @NotNull RowMapper<T> rowMapper) {
        return querySmileyVars(sql, valueMap, new RowMapperResultSetExtractor<>(rowMapper));
    }

    /**
     * Query using a {@link SmileyVarsPreparedStatement}. A {@link SmileyVarsPreparedStatement} is created from the
     * given sql. A return value is produced by executing the prepared statement and passing the result set containing a
     * single row to the given {@link RowMapper} object. Here is a usage example:
     * <pre>
     *     String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *     Inventory inventory = svjt.queryForObjectSmileyVars(sql, svps-&gt; svps.setInt("aisle", 4).setInt("level", 1).setInt("bin_number", 8), rowMapper);
     * </pre>
     *
     * @param <T> The type of object to be returned.
     * @param sql       The SQL to use for the SmileyVars template.
     * @param setter    a consumer function that sets the values of variables in the SmileVars template.
     * @param rowMapper a callback that is called to process the single row into a value.
     * @return the result object returned by the ResultSetExtractor
     * @throws DataAccessException if there is any problem
     */
    public <T> T queryForObjectSmileyVars(@NotNull String sql, @NotNull SqlConsumer<SmileyVarsPreparedStatement> setter, @NotNull RowMapper<T> rowMapper) {
        List<T> result = querySmileyVars(sql, setter, new RowMapperResultSetExtractor<>(rowMapper, 1));
        return DataAccessUtils.nullableSingleResult(result);
    }

    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given name and value
     * arrays. Execute the expanded SQL as a {@code Statement}. A return value is produced by passing result set
     * containing a single row to the given {@link RowMapper} object. Here is a usage example:
     * <pre>
     *    String[] names = {"aisle", "level", "bin_number"};
     *    Object[] values = {4, 1, 8};
     *    String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *    Inventory inventory = svjt.queryForObjectSmileyVars(sql, names, values, rowMapper);
     * </pre>
     *
     * @param <T> The type of object to be returned.
     * @param sql       The string to use as the SmileyVars template body.
     * @param names     The names of the variables whose values are being specified.
     * @param values    The corresponding values to use for the variables in the template body.
     * @param rowMapper a callback that is called to process each row into a value.
     * @return the result object of the required type, or null in case of SQL NULL
     * @throws DataAccessException      if there is a problem.
     * @throws IllegalArgumentException if the names and values arrays are not the same length
     */
    public <T> T queryForObjectSmileyVars(String sql, @NotNull String[] names, @NotNull Object[] values, @NotNull RowMapper<T> rowMapper) {
        List<T> result = querySmileyVars(sql, names, values, new RowMapperResultSetExtractor<>(rowMapper, 1));
        return DataAccessUtils.nullableSingleResult(result);
    }


    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given map. Execute the
     * expanded SQL as a {@code Statement}. A return value is produced by passing result set containing a single row to
     * the given {@link RowMapper} object. Here is a usage example:
     * <pre>
     *    Map&lt;String, Object&gt; valueMap = new HashMap&lt;&gt;();
     *    valueMap.put("aisle", 4);
     *    valueMap.put("level", 1);
     *    valueMap.put("bin_number", 8);
     *    String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *    Inventory inventory  = svjt.queryForObjectSmileyVars(sql, valueMap, rowMapper);
     * </pre>
     *
     * @param <T>         The type of object expected to be returned.
     * @param sql       The string to use as the SmileyVars template body.
     * @param valueMap  The values to use for the variables in the template body.
     * @param rowMapper a callback that is called to process each row into a value.
     * @return the result object of the required type, or null in case of SQL NULL
     * @throws DataAccessException      if there is a problem.
     * @throws IllegalArgumentException if the names and values arrays are not the same length
     */
    public <T> T queryForObjectSmileyVars(String sql, Map<String, Object> valueMap, @NotNull RowMapper<T> rowMapper) {
        List<T> result = querySmileyVars(sql, valueMap, new RowMapperResultSetExtractor<>(rowMapper, 1));
        return DataAccessUtils.nullableSingleResult(result);
    }

    /**
     * Query using a {@link SmileyVarsPreparedStatement}. A {@link SmileyVarsPreparedStatement} is created from the
     * given sql. A return value is produced by executing the prepared statement to produce result set containing a
     * single row and single column to be converted to the specified required type. Here is a usage example:
     * <pre>
     *    Integer quantity = svjt.queryForObjectSmileyVars("SELECT quantity FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)",
     *         svps -&gt; svps.setInt("aisle", 4).setInt("level", 1).setInt("bin_number", 8),
     *         Integer.class);
     * </pre>
     *
     * @param <T>         The type of object expected to be returned.
     * @param sql          The SQL to use for the SmileyVars template.
     * @param setter       a consumer function that sets the values of variables in the SmileVars template.
     * @param requiredType The type of return value to be produced.
     * @return the result object
     * @throws DataAccessException if there is any problem
     */
    public <T> T queryForObjectSmileyVars(String sql, @NotNull SqlConsumer<SmileyVarsPreparedStatement> setter, Class<T> requiredType) {
        return queryForObjectSmileyVars(sql, setter, getSingleColumnRowMapper(requiredType));
    }


    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given name and value
     * arrays. Execute the expanded SQL as a {@code Statement}. A return value is produced by passing result set
     * containing a single row and single column to be converted to the specified required type. Here is a usage
     * example:
     * <pre>
     *    String[] names = {"aisle", "level", "bin_number"};
     *    Object[] values = {4, 1, 8};
     *    String sql = "SELECT quantity FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *    Integer quantity = svjt.queryForObjectSmileyVars(sql, names, values, Integer.class);
     * </pre>
     *
     * @param <T>         The type of object expected to be returned.
     * @param sql          The string to use as the SmileyVars template body.
     * @param names        The names of the variables whose values are being specified.
     * @param values       The corresponding values to use for the variables in the template body.
     * @param requiredType The type of return value to be produced.
     * @return the result object
     * @throws DataAccessException      if there is a problem.
     * @throws IllegalArgumentException if the names and values arrays are not the same length
     */
    public <T> T queryForObjectSmileyVars(String sql, String[] names, Object[] values, Class<T> requiredType) {
        return queryForObjectSmileyVars(sql, names, values, getSingleColumnRowMapper(requiredType));
    }

    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given map. Execute the
     * expanded SQL as a {@code Statement}. A return value is produced by passing result set containing a single row and
     * single column to be converted to the specified required type. Here is a usage example:
     * <pre>
     *     Map&lt;String, Object&gt; valueMap = new HashMap&lt;&gt;();
     *     valueMap.put("aisle", 4);
     *     valueMap.put("level", 1);
     *     valueMap.put("bin_number", 8);
     *     String sql = "SELECT quantity FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *     Integer quantity = svjt.queryForObjectSmileyVars(sql, valueMap, Integer.class);
     * </pre>
     *
     * @param <T>          The type of object expected to be in the returned list.
     * @param sql          The string to use as the SmileyVars template body.
     * @param valueMap     The values to use for the variables in the template body.
     * @param requiredType The type of return value to be produced.
     * @return the result object
     * @throws DataAccessException      if there is a problem.
     * @throws IllegalArgumentException if the names and values arrays are not the same length
     */
    public <T> T queryForObjectSmileyVars(String sql, Map<String, Object> valueMap, Class<T> requiredType) {
        return queryForObjectSmileyVars(sql, valueMap, getSingleColumnRowMapper(requiredType));
    }

    /**
     * Query using a {@link SmileyVarsPreparedStatement}. A {@link SmileyVarsPreparedStatement} is created from the
     * given sql. A return value is produced by executing the prepared statement to produce result set containing a
     * single row and converting the row to a Map. Here is a usage example:
     * <pre>
     *    Map&lt;String, Object&gt; resultMap = svjt.queryForMapSmileyVars("SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)",
     *         svps -&gt; svps.setInt("aisle", 4).setInt("level", 1).setInt("bin_number", 8));
     * </pre>
     *
     * @param sql    The SQL to use for the SmileyVars template.
     * @param setter a consumer function that sets the values of variables in the SmileVars template.
     * @return A map whose keys are the value in the result row and whose values are the values from the result row.
     * @throws DataAccessException if there is any problem
     */
    public Map<String, Object> queryForMapSmileyVars(String sql, @NotNull SqlConsumer<SmileyVarsPreparedStatement> setter) {
        return queryForObjectSmileyVars(sql, setter, getColumnMapRowMapper());
    }

    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given name and value
     * arrays. Execute the expanded SQL as a {@code Statement}. A return value is produced by executing the prepared
     * statement to produce result set containing a single row and converting the row to a Map. Here is a usage
     * example:
     * <pre>
     *    String[] names = {"aisle", "level", "bin_number"};
     *    Object[] values = {4, 1, 8};
     *    String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *    Map&lt;String, Object&gt; resultMap = svjt.queryForMapSmileyVars(sql, names, values);
     * </pre>
     *
     * @param sql    The string to use as the SmileyVars template body.
     * @param names  The names of the variables whose values are being specified.
     * @param values The corresponding values to use for the variables in the template body.
     * @return A map whose keys are the value in the result row and whose values are the values from the result row.
     * @throws DataAccessException      if there is a problem.
     * @throws IllegalArgumentException if the names and values arrays are not the same length
     */
    public Map<String, Object> queryForMapSmileyVars(String sql, String[] names, Object[] values) {
        return queryForObjectSmileyVars(sql, names, values, getColumnMapRowMapper());
    }

    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given map. Execute the
     * expanded SQL as a {@code Statement}. A return value is produced by executing the prepared statement to produce
     * result set containing a single row and converting the row to a Map. Here is a usage example:
     * <pre>
     *     Map&lt;String, Object&gt; valueMap = new HashMap&lt;&gt;();
     *     valueMap.put("aisle", 4);
     *     valueMap.put("level", 1);
     *     valueMap.put("bin_number", 8);
     *     String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *     Map&lt;String, Object&gt; resultMap = svjt.queryForObjectSmileyVars(sql, valueMap);
     * </pre>
     *
     * @param sql      The string to use as the SmileyVars template body.
     * @param valueMap The values to use for the variables in the template body.
     * @return A map whose keys are the value in the result row and whose values are the values from the result row.
     * @throws DataAccessException      if there is a problem.
     * @throws IllegalArgumentException if the names and values arrays are not the same length
     */
    public Map<String, Object> queryForMapSmileyVars(String sql, Map<String, Object> valueMap) {
        return queryForObjectSmileyVars(sql, valueMap, getColumnMapRowMapper());
    }

    /**
     * Query using a {@link SmileyVarsPreparedStatement}. A {@link SmileyVarsPreparedStatement} is created from the
     * given sql. A list of return values of specified type is produced by executing the prepared statement to produce
     * result set containing a single column. Here is a usage example:
     * <pre>
     *     String sql = "SELECT item_number FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *     List&lt;String&gt; itemNumbers = svjt.queryForListSmileyVars(sql,
     *         svps -&gt; svps.setInt("aisle", 4).setInt("level", 1),
     *         String.class);
     * </pre>
     *
     * @param <T>         The type of object expected to be in the returned list.
     * @param sql         The SQL to use for the SmileyVars template.
     * @param setter      a consumer function that sets the values of variables in the SmileVars template.
     * @param elementType the required type of element in the result list (for example, {@code Integer.class})
     * @return A List whose elements are the single value in each result row.
     * @throws DataAccessException if there is any problem
     */
    public <T> List<T> queryForListSmileyVars(String sql, @NotNull SqlConsumer<SmileyVarsPreparedStatement> setter, Class<T> elementType) {
        return querySmileyVars(sql, setter, getSingleColumnRowMapper(elementType));
    }

    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given name and value
     * arrays. Execute the expanded SQL as a {@code Statement}. A list of return values of specified type is produced by
     * executing the prepared statement to produce result set containing a single column. Here is a usage example:
     * <pre>
     *    String[] names = {"aisle", "level"};
     *    Object[] values = {4, 1};
     *    String sql = "SELECT item_number FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *    List&lt;String&gt; itemNumbers = svjt.queryForListSmileyVars(sql, names, values, String.class);
     * </pre>
     *
     * @param <T>         The type of object expected to be in the returned list.
     * @param sql         The string to use as the SmileyVars template body.
     * @param names       The names of the variables whose values are being specified.
     * @param values      The corresponding values to use for the variables in the template body.
     * @param elementType the required type of element in the result list (for example, {@code Integer.class})
     * @return A List whose elements are the single value in each result row.
     * @throws DataAccessException      if there is a problem.
     * @throws IllegalArgumentException if the names and values arrays are not the same length
     */
    public <T> List<T> queryForListSmileyVars(String sql, String[] names, Object[] values, Class<T> elementType) {
        return querySmileyVars(sql, names, values, getSingleColumnRowMapper(elementType));
    }

    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given map. Execute the
     * expanded SQL as a {@code Statement}. A list of return values of specified type is produced by executing the
     * prepared statement to produce result set containing a single column. Here is a usage example:
     * <pre>
     *     Map&lt;String, Object&gt; valueMap = new HashMap&lt;&gt;();
     *     valueMap.put("aisle", 4);
     *     valueMap.put("level", 1);
     *     String sql = "SELECT item_number FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *     List&lt;String&gt; itemNumbers = svjt.queryForObjectSmileyVars(sql, valueMap, String.class);
     * </pre>
     *
     * @param <T>         The type of object expected to be in the returned list.
     * @param sql         The string to use as the SmileyVars template body.
     * @param valueMap    The values to use for the variables in the template body.
     * @param elementType the required type of element in the result list (for example, {@code Integer.class})
     * @return A List whose elements are the single value in each result row.
     * @throws DataAccessException      if there is a problem.
     * @throws IllegalArgumentException if the names and values arrays are not the same length
     */
    public <T> List<T> queryForListSmileyVars(String sql, Map<String, Object> valueMap, Class<T> elementType) {
        return querySmileyVars(sql, valueMap, getSingleColumnRowMapper(elementType));
    }

    /**
     * Query using a {@link SmileyVarsPreparedStatement}. A {@link SmileyVarsPreparedStatement} is created from the
     * given sql. The prepared statement is executed to produce a result set. A Map is created for each row. The keys of
     * these maps are the column names and the value are the values from the row. A list of these Map objects is
     * returned. Here is a usage example:
     * <pre>
     *     String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *     List&lt;Map&lt;String, Object&gt;&gt; inventoryMapList = svjt.queryForListSmileyVars(sql,
     *         svps -&gt; svps.setInt("aisle", 4).setInt("level", 1));
     * </pre>
     *
     * @param sql    The SQL to use for the SmileyVars template.
     * @param setter a consumer function that sets the values of variables in the SmileVars template.
     * @return A List that contains a Map for each returned row.
     * @throws DataAccessException if there is any problem
     */
    public List<Map<String, Object>> queryForListSmileyVars(String sql, @NotNull SqlConsumer<SmileyVarsPreparedStatement> setter) {
        return querySmileyVars(sql, setter, getColumnMapRowMapper());
    }

    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given name and value
     * arrays. Execute the expanded SQL as a {@code Statement}. A Map is created for each row. The keys of these maps
     * are the column names and the value are the values from the row. A list of these Map objects is returned. Here is
     * a usage example:
     * <pre>
     *    String[] names = {"aisle", "level"};
     *    Object[] values = {4, 1};
     *    String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *    List&lt;Map&lt;String, Object&gt;&gt; inventoryMapList = svjt.queryForListSmileyVars(sql, names, values);
     * </pre>
     *
     * @param sql    The string to use as the SmileyVars template body.
     * @param names  The names of the variables whose values are being specified.
     * @param values The corresponding values to use for the variables in the template body.
     * @return A List that contains a Map for each returned row.
     * @throws DataAccessException      if there is a problem.
     * @throws IllegalArgumentException if the names and values arrays are not the same length
     */
    public List<Map<String, Object>> queryForListSmileyVars(@NotNull String sql, @NotNull String[] names, @NotNull Object[] values) {
        return querySmileyVars(sql, names, values, getColumnMapRowMapper());
    }

    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given map. Execute the
     * expanded SQL as a {@code Statement}. A Map is created for each row. The keys of these maps are the column names
     * and the value are the values from the row. A list of these Map objects is returned. Here is a usage example:
     * <pre>
     *     Map&lt;String, Object&gt; valueMap = new HashMap&lt;&gt;();
     *     valueMap.put("aisle", 4);
     *     valueMap.put("level", 1);
     *     String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *     List&lt;Map&lt;String, Object&gt;&gt; inventoryMapList = svjt.queryForObjectSmileyVars(sql, valueMap);
     * </pre>
     *
     * @param sql      The string to use as the SmileyVars template body.
     * @param valueMap The values to use for the variables in the template body.
     * @return A List that contains a Map for each returned row.
     * @throws DataAccessException      if there is a problem.
     * @throws IllegalArgumentException if the names and values arrays are not the same length
     */
    public List<Map<String, Object>> queryForListSmileyVars(@NotNull String sql, @NotNull Map<String, Object> valueMap) {
        return querySmileyVars(sql, valueMap, getColumnMapRowMapper());
    }

    /**
     * Query using a {@link SmileyVarsPreparedStatement}. A {@link SmileyVarsPreparedStatement} is created from the
     * given sql. The prepared statement is executed to produce a result set. The result set is returned as an {@link
     * SqlRowSet} object. Here is a usage example:
     * <pre>
     *     String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *     SqlRowSet rowSet = svjt.queryForRowSetSmileyVars(sql,
     *         svps -&gt; svps.setInt("aisle", 4).setInt("level", 1));
     * </pre>
     *
     * @param sql    The SQL to use for the SmileyVars template.
     * @param setter a consumer function that sets the values of variables in the SmileVars template.
     * @return result rows as an {@link SqlRowSet} object.
     * @throws DataAccessException if there is any problem
     */
    public SqlRowSet queryForRowSetSmileyVars(@NotNull String sql, @NotNull SqlConsumer<SmileyVarsPreparedStatement> setter) {
        return querySmileyVars(sql, setter, new SqlRowSetResultSetExtractor());
    }

    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given name and value
     * arrays. Execute the expanded SQL as a {@code Statement}. Return the results an {@link SqlRowSet} object. Here is
     * a usage example:
     * <pre>
     *    String[] names = {"aisle", "level"};
     *    Object[] values = {4, 1};
     *    String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *    SqlRowSet rowSet = svjt.queryForRowSetSmileyVars(sql, names, values);
     * </pre>
     *
     * @param sql    The string to use as the SmileyVars template body.
     * @param names  The names of the variables whose values are being specified.
     * @param values The corresponding values to use for the variables in the template body.
     * @return result rows as an {@link SqlRowSet} object.
     * @throws DataAccessException      if there is a problem.
     * @throws IllegalArgumentException if the names and values arrays are not the same length
     */
    public SqlRowSet queryForRowSetSmileyVars(@NotNull String sql, @NotNull String[] names, @NotNull Object[] values) {
        return querySmileyVars(sql, names, values, new SqlRowSetResultSetExtractor());
    }

    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given map. Execute the
     * expanded SQL as a {@code Statement}. Return the results an {@link SqlRowSet} object. Here is a usage example:
     * <pre>
     *     Map&lt;String, Object&gt; valueMap = new HashMap&lt;&gt;();
     *     valueMap.put("aisle", 4);
     *     valueMap.put("level", 1);
     *     String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *     SqlRowSet rowSet = svjt.queryForObjectSmileyVars(sql, valueMap);
     * </pre>
     *
     * @param sql      The string to use as the SmileyVars template body.
     * @param valueMap The values to use for the variables in the template body.
     * @return result rows as an {@link SqlRowSet} object.
     * @throws DataAccessException      if there is a problem.
     * @throws IllegalArgumentException if the names and values arrays are not the same length
     */
    public SqlRowSet queryForRowSetSmileyVars(@NotNull String sql, @NotNull Map<String, Object> valueMap) {
        return querySmileyVars(sql, valueMap, new SqlRowSetResultSetExtractor());
    }

    /**
     * Update using a {@link SmileyVarsPreparedStatement}. A {@link SmileyVarsPreparedStatement} is created from the
     * given sql. Here is a usage example:
     * <pre>
     *     String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
     *     List&lt;Inventory&gt; inventoryList = svjt.querySmileyVars(sql, svps-&gt; svps.setInt("aisle", 4).setInt("level", 1), rse);
     * </pre>
     *
     * @param sql    The SQL to use for the SmileyVars template.
     * @param setter a consumer function that sets the values of variables in the SmileVars template.
     * @return the number of effected rows.
     * @throws DataAccessException if there is any problem
     */
    public int updateSmileyVars(@NotNull String sql, @NotNull SqlConsumer<SmileyVarsPreparedStatement> setter) {
        return update(conn -> {
            SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(conn, sql);
            setter.accept(svps);
            return svps.getPreparedStatement();
        });
    }

    /**
     * Update using a {@link SmileyVarsPreparedStatement}. A {@link SmileyVarsPreparedStatement} is created from the
     * given sql. Generated keys will be put into the given KeyHolder. Here is a usage example:
     * <pre>
     *    String sql = "UPDATE inventory SET quantity = quantity + 1 WHERE aisle=:aisle AND level=:level AND bin_number=:bin_number";
     *    int updateCount = svjt.updateSmileyVars(sql, svps -&gt; svps.setInt("aisle", 4).setInt("level", 2).setInt("bin_number", 3));
     * </pre>
     *
     * @param sql                The SQL to use for the SmileyVars template.
     * @param setter             a consumer function that sets the values of variables in the SmileVars template.
     * @param generatedKeyHolder a KeyHolder to hold the generated keys
     * @return the number of affected rows.
     * @throws DataAccessException if there is any problem
     */
    public int updateSmileyVars(@NotNull String sql,
                                @NotNull SqlConsumer<SmileyVarsPreparedStatement> setter,
                                @NotNull KeyHolder generatedKeyHolder) {
        return update(conn -> {
                    SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(conn, sql);

                    setter.accept(svps);
                    return svps.getPreparedStatement();
                },
                generatedKeyHolder);
    }

    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given name and value
     * arrays. Use the expanded SQL to update the database. Here is a usage example:
     * <pre>
     *    String[] names = {"aisle", "level", "bin_number"};
     *    Object[] values = {4, 2, 3};
     *    String sql = "UPDATE inventory SET quantity = quantity + 1 WHERE aisle=:aisle AND level=:level AND bin_number=:bin_number";
     *    int updateCount = svjt.updateSmileyVars(sql, names, values);
     * </pre>
     *
     * @param sql    The string to use as the SmileyVars template body.
     * @param names  The names of the variables whose values are being specified.
     * @param values The corresponding values to use for the variables in the template body.
     * @return the number of affected rows.
     * @throws DataAccessException      if there is a problem.
     * @throws IllegalArgumentException if the names and values arrays are not the same length
     */
    public int updateSmileyVars(@NotNull String sql, @NotNull String[] names, @NotNull Object[] values) {
        ensureEqualLengthArrays(names, values);
        return updateSmileyVars(sql, arraysToMap(names, values));
    }


    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given map. Use the
     * expanded SQL to update the database. Here is a usage example:
     * <pre>
     *     Map&lt;String, Object&gt; valueMap = new HashMap&lt;&gt;();
     *     valueMap.put("aisle", 4);
     *     valueMap.put("level", 2);
     *     valueMap.put("bin_number", 3);
     *    String sql = "UPDATE inventory SET quantity = quantity + 1 WHERE aisle=:aisle AND level=:level AND bin_number=:bin_number";
     *    int updateCount = svjt.updateSmileyVars(sql, valueMap);
     * </pre>
     *
     * @param sql      The string to use as the SmileyVars template body.
     * @param valueMap The values to use for the variables in the template body.
     * @return the number of affected rows.
     * @throws DataAccessException      if there is a problem.
     * @throws IllegalArgumentException if the names and values arrays are not the same length
     */
    public int updateSmileyVars(@NotNull String sql, @NotNull Map<String, Object> valueMap) {
        return update(expand(sql, valueMap));
    }

    /**
     * Execute a batch of updates/inserts using the given SQL as a SmileyVars template.
     *
     * @param sql       The sql to use as the body of a SmileyVars template.
     * @param mapSetter A {@link MapSetter} that specifies the methods to use for setting the parameter values in the
     *                  SmileyVars template.
     * @param valueMaps A collection of Maps that specify sets of values to be used for the batch.
     * @return an array of the number of rows affected by each update. The order of the elements in the array will be
     * arbitrary.
     */
    @NotNull
    public int[] batchUpdate(@NotNull String sql, @NotNull MapSetter mapSetter, @NotNull Collection<Map<String, Object>> valueMaps) {
        if (valueMaps.isEmpty()) {
            return new int[0];
        }
        return executeSmileyVars(sql, svps -> internalBatchUpdate(svps, mapSetter, valueMaps));
    }

    private int[] internalBatchUpdate(SmileyVarsPreparedStatement svps, MapSetter mapSetter, Collection<Map<String, Object>> valueMaps) {
        boolean batchSupported = JdbcUtils.supportsBatchUpdates(svps.getConnection());
        if (batchSupported) {
            return supportedBatchUpdate(svps, mapSetter, valueMaps);
        } else {
            return unsupportedBatchUpdate(svps, mapSetter, valueMaps);
        }
    }

    private int[] supportedBatchUpdate(SmileyVarsPreparedStatement svps, MapSetter mapSetter, Collection<Map<String, Object>> valueMaps) {
        try {
            for (Map<String, Object> valueMap : valueMaps) {
                mapSetter.setSmileyVars(svps, valueMap);
                svps.addBatch();
            }
            return svps.executeBatch();
        } catch (SQLException e) {
            throw translateException("batchUpdate", svps.toString(), e);
        }
    }

    private int[] unsupportedBatchUpdate(SmileyVarsPreparedStatement svps, MapSetter mapSetter, Collection<Map<String, Object>> valueMaps) {
        int[] rowsAffected = new int[valueMaps.size()];
        try {
            int i = 0;
            for (Map<String, Object> valueMap : valueMaps) {
                mapSetter.setSmileyVars(svps, valueMap);
                rowsAffected[i] = svps.executeUpdate();
            }
        } catch (SQLException e) {
            throw translateException("batchUpdate", svps.toString(), e);
        }
        return rowsAffected;
    }

    /**
     * Returns a string representation of the object. In general, the {@code toString} method returns a string that
     * "textually represents" this object. The result should be a concise but informative representation that is easy
     * for a person to read. It is recommended that all subclasses override this method.
     * <p>
     * The {@code toString} method for class {@code Object} returns a string consisting of the name of the class of
     * which the object is an instance, the at-sign character `{@code @}', and the unsigned hexadecimal representation
     * of the hash code of the object. In other words, this method returns a string equal to the value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return "SmileyVarsJdbcTemplate[DataSource: " + getDataSource() + "; " + super.toString() + "]";
    }
}
