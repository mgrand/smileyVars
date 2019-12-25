package com.markgrand.smileyvars.spring;

import com.markgrand.smileyvars.DatabaseType;
import com.markgrand.smileyvars.SmileyVarsPreparedStatement;
import com.markgrand.smileyvars.SmileyVarsTemplate;
import com.markgrand.smileyvars.util.SqlConsumer;
import com.markgrand.smileyvars.util.SqlFunction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class SmileyVarsJdbcTemplate extends JdbcTemplate {
    private static final Logger logger = LoggerFactory.getLogger(SmileyVarsJdbcTemplate.class);

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
    public SmileyVarsJdbcTemplate(DataSource dataSource, boolean lazyInit) {
        super(dataSource, lazyInit);
    }

    @Override
    public void setDataSource(DataSource dataSource) {
        super.setDataSource(dataSource);
        Connection conn = createConnectionProxy(DataSourceUtils.getConnection(obtainDataSource()));
        try {
            databaseType = DatabaseType.inferDatabaseType(conn.getMetaData());
            logger.debug("DatabaseType is {}", databaseType);
        } catch (SQLException e) {
            throw translateException("getMetaData", null, e);
        } finally {
            DataSourceUtils.releaseConnection(conn, getDataSource());
        }
    }

    /**
     * Create a {@link SmileyVarsPreparedStatement} fron the given SQL and pass it to the given function. Here is a
     * usage example:
     * <pre>
     * int quantity
     *    = svjt.withSmileyVars("SELECT quantity FROM inventory WHERE aisle = :aisle AND level = :level AND bin_number = :bin_number",
     *         svps -> {
     *             try (ResultSet rs = svps.setInt("aisle", 4).setInt("level", 1). setInt("bin_number", 7).executeQuery()) {
     *                 return rs.getInt("quantity");
     *             }
     *         }));
     * </pre>
     *
     * @param sql          The sql to be used as a SmileyVars template.
     * @param svpsConsumer The consumer function that the
     * @param <T>          The the of value to be returned.
     * @return the value that is returned by the given function.
     */
    @SuppressWarnings("unused")
    public <T> T withSmileyVars(String sql, SqlFunction<SmileyVarsPreparedStatement, T> svpsConsumer) {
        Connection conn = DataSourceUtils.getConnection(obtainDataSource());
        try {
            logger.debug("Creating SmileyVarsPreparedStatement from sql: {}", sql);
            try (SmileyVarsPreparedStatement svps = new SmileyVarsPreparedStatement(conn, sql)) {
                return svpsConsumer.apply(svps);
            }
        } catch (SQLException e) {
            throw translateException("WithSmileyVarsPreparedStatement", sql, e);
        } finally {
            DataSourceUtils.releaseConnection(conn, getDataSource());
        }
    }


    /**
     * Query using a {@link SmileyVarsPreparedStatement}. A {@link SmileyVarsPreparedStatement} is created from the
     * given sql.
     *
     * @param sql    The SQL to use for the SmileyVars template.
     * @param setter a consumer function that sets the values of variables in the SmileVars template.
     * @param rse    a callback that will extract results
     * @return the result object returned by the ResultSetExtractor
     * @throws DataAccessException if there is any problem
     */
    public <T> T query(@NotNull String sql, SqlConsumer<SmileyVarsPreparedStatement> setter, ResultSetExtractor<T> rse) throws DataAccessException {
        return withSmileyVars(sql, (SmileyVarsPreparedStatement svps) -> {
            setter.accept(svps);
            return rse.extractData(svps.executeQuery());
        });
    }

    /**
     * Expand the given SQL as a SmileyVars template using the variable values specified in the given map. Execute the
     * expanded SQL as a {@code Statement}. Take the ResultSet that is produced and use the given {@link
     * ResultSetExtractor} to produce the value that will be returned by this method.
     *
     * @param sql    The string to use as the SmileyVars template body.
     * @param rse    The {@link ResultSetExtractor} to use for extracting a result from the query's result set.
     * @param values The values to use for the variables in the template body.
     * @param <T>    The type of value to be returned.
     * @return the value produced by the {@link ResultSetExtractor}.
     * @throws DataAccessException if there is a problem.
     */
    public <T> T query(String sql, ResultSetExtractor<T> rse, Map<String, ?> values) throws DataAccessException {
        return super.query(SmileyVarsTemplate.template(databaseType, sql).apply(values), rse);
    }

    @Override
    public void query(PreparedStatementCreator psc, RowCallbackHandler rch) throws DataAccessException {
        super.query(psc, rch);
    }

    @Override
    public void query(String sql, PreparedStatementSetter pss, RowCallbackHandler rch) throws DataAccessException {
        super.query(sql, pss, rch);
    }

    @Override
    public void query(String sql, Object[] args, int[] argTypes, RowCallbackHandler rch) throws DataAccessException {
        super.query(sql, args, argTypes, rch);
    }

    @Override
    public void query(String sql, Object[] args, RowCallbackHandler rch) throws DataAccessException {
        super.query(sql, args, rch);
    }

    @Override
    public void query(String sql, RowCallbackHandler rch, Object... args) throws DataAccessException {
        super.query(sql, rch, args);
    }

    @Override
    public <T> List<T> query(PreparedStatementCreator psc, @NotNull RowMapper<T> rowMapper) throws DataAccessException {
        return super.query(psc, rowMapper);
    }

    @Override
    public <T> List<T> query(String sql, PreparedStatementSetter pss, @NotNull RowMapper<T> rowMapper) throws DataAccessException {
        return super.query(sql, pss, rowMapper);
    }

    @Override
    public <T> List<T> query(String sql, Object[] args, int[] argTypes, @NotNull RowMapper<T> rowMapper) throws DataAccessException {
        return super.query(sql, args, argTypes, rowMapper);
    }

    @Override
    public <T> List<T> query(String sql, Object[] args, @NotNull RowMapper<T> rowMapper) throws DataAccessException {
        return super.query(sql, args, rowMapper);
    }

    @Override
    public <T> List<T> query(String sql, @NotNull RowMapper<T> rowMapper, Object... args) throws DataAccessException {
        return super.query(sql, rowMapper, args);
    }

    @Override
    public <T> T queryForObject(String sql, Object[] args, int[] argTypes, @NotNull RowMapper<T> rowMapper) throws DataAccessException {
        return super.queryForObject(sql, args, argTypes, rowMapper);
    }

    @Override
    public <T> T queryForObject(String sql, Object[] args, @NotNull RowMapper<T> rowMapper) throws DataAccessException {
        return super.queryForObject(sql, args, rowMapper);
    }

    @Override
    public <T> T queryForObject(String sql, @NotNull RowMapper<T> rowMapper, Object... args) throws DataAccessException {
        return super.queryForObject(sql, rowMapper, args);
    }

    @Override
    public <T> T queryForObject(String sql, Object[] args, int[] argTypes, Class<T> requiredType) throws DataAccessException {
        return super.queryForObject(sql, args, argTypes, requiredType);
    }

    @Override
    public <T> T queryForObject(String sql, Object[] args, Class<T> requiredType) throws DataAccessException {
        return super.queryForObject(sql, args, requiredType);
    }

    @Override
    public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) throws DataAccessException {
        return super.queryForObject(sql, requiredType, args);
    }

    @Override
    public Map<String, Object> queryForMap(String sql, Object[] args, int[] argTypes) throws DataAccessException {
        return super.queryForMap(sql, args, argTypes);
    }

    @Override
    public Map<String, Object> queryForMap(String sql, Object... args) throws DataAccessException {
        return super.queryForMap(sql, args);
    }

    @Override
    public <T> List<T> queryForList(String sql, Object[] args, int[] argTypes, Class<T> elementType) throws DataAccessException {
        return super.queryForList(sql, args, argTypes, elementType);
    }

    @Override
    public <T> List<T> queryForList(String sql, Object[] args, Class<T> elementType) throws DataAccessException {
        return super.queryForList(sql, args, elementType);
    }

    @Override
    public <T> List<T> queryForList(String sql, Class<T> elementType, Object... args) throws DataAccessException {
        return super.queryForList(sql, elementType, args);
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Object[] args, int[] argTypes) throws DataAccessException {
        return super.queryForList(sql, args, argTypes);
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Object... args) throws DataAccessException {
        return super.queryForList(sql, args);
    }

    @Override
    public SqlRowSet queryForRowSet(String sql, Object[] args, int[] argTypes) throws DataAccessException {
        return super.queryForRowSet(sql, args, argTypes);
    }

    @Override
    public SqlRowSet queryForRowSet(String sql, Object... args) throws DataAccessException {
        return super.queryForRowSet(sql, args);
    }

    @Override
    protected int update(PreparedStatementCreator psc, PreparedStatementSetter pss) throws DataAccessException {
        return super.update(psc, pss);
    }

    @Override
    public int update(PreparedStatementCreator psc) throws DataAccessException {
        return super.update(psc);
    }

    @Override
    public int update(PreparedStatementCreator psc, @NotNull KeyHolder generatedKeyHolder) throws DataAccessException {
        return super.update(psc, generatedKeyHolder);
    }

    @Override
    public int update(@NotNull String sql, PreparedStatementSetter pss) throws DataAccessException {
        return super.update(sql, pss);
    }

    @Override
    public int update(String sql, Object[] args, int[] argTypes) throws DataAccessException {
        return super.update(sql, args, argTypes);
    }

    @Override
    public int update(String sql, Object... args) throws DataAccessException {
        return super.update(sql, args);
    }

    @Override
    public int[] batchUpdate(String sql, @NotNull BatchPreparedStatementSetter pss) throws DataAccessException {
        return super.batchUpdate(sql, pss);
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> batchArgs) throws DataAccessException {
        return super.batchUpdate(sql, batchArgs);
    }

    @Override
    public int[] batchUpdate(String sql, @NotNull List<Object[]> batchArgs, int[] argTypes) throws DataAccessException {
        return super.batchUpdate(sql, batchArgs, argTypes);
    }

    @Override
    public <T> int[][] batchUpdate(String sql, @NotNull Collection<T> batchArgs, int batchSize, ParameterizedPreparedStatementSetter<T> pss) throws DataAccessException {
        return super.batchUpdate(sql, batchArgs, batchSize, pss);
    }

    @Override
    public <T> T execute(@NotNull CallableStatementCreator csc, @NotNull CallableStatementCallback<T> action) throws DataAccessException {
        return super.execute(csc, action);
    }

    @Override
    public <T> T execute(@NotNull String callString, CallableStatementCallback<T> action) throws DataAccessException {
        return super.execute(callString, action);
    }

    @Override
    public Map<String, Object> call(CallableStatementCreator csc, @NotNull List<SqlParameter> declaredParameters) throws DataAccessException {
        return super.call(csc, declaredParameters);
    }

    @Override
    protected Map<String, Object> extractReturnedResults(CallableStatement cs, List<SqlParameter> updateCountParameters, List<SqlParameter> resultSetParameters, int updateCount) throws SQLException {
        return super.extractReturnedResults(cs, updateCountParameters, resultSetParameters, updateCount);
    }

    @Override
    protected Map<String, Object> extractOutputParameters(CallableStatement cs, @NotNull List<SqlParameter> parameters) throws SQLException {
        return super.extractOutputParameters(cs, parameters);
    }

    @Override
    protected PreparedStatementSetter newArgPreparedStatementSetter(Object[] args) {
        return super.newArgPreparedStatementSetter(args);
    }

    @Override
    protected PreparedStatementSetter newArgTypePreparedStatementSetter(Object[] args, int[] argTypes) {
        return super.newArgTypePreparedStatementSetter(args, argTypes);
    }

    /**
     * Returns a hash code value for the object. This method is supported for the benefit of hash tables such as those
     * provided by {@link HashMap}.
     *
     * @return a hash code value for this object.
     * @see Object#equals(Object)
     * @see System#identityHashCode
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise.
     * @see #hashCode()
     * @see HashMap
     */
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
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
