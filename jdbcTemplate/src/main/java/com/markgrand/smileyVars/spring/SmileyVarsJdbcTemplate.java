package com.markgrand.smileyVars.spring;

import com.markgrand.smileyVars.DatabaseType;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.sql.DataSource;
import java.lang.ref.PhantomReference;
import java.lang.ref.WeakReference;
import java.sql.*;
import java.util.*;

/**
 * This is an extension of {@linkplain JdbcTemplate} that supports SmileyVars Templates.
 * @see com.markgrand.smileyVars.SmileyVarsTemplate
 *
 * {@inheritDoc}
 * @author Mark Grand
 */
public class SmileyVarsJdbcTemplate extends JdbcTemplate {
    Optional<DatabaseType> dbType = Optional.empty();

    public SmileyVarsJdbcTemplate() {
        super();
    }

    public SmileyVarsJdbcTemplate(DataSource dataSource) {
        super(dataSource);
        try (Connection conn = dataSource.getConnection()) {
            dbType = Optional.of(DatabaseType.inferDatabaseType(conn.getMetaData()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public SmileyVarsJdbcTemplate(DataSource dataSource, boolean lazyInit) {
        super(dataSource, lazyInit);
    }

    @Override
    public void setIgnoreWarnings(boolean ignoreWarnings) {
        super.setIgnoreWarnings(ignoreWarnings);
    }

    @Override
    public boolean isIgnoreWarnings() {
        return super.isIgnoreWarnings();
    }

    @Override
    public void setFetchSize(int fetchSize) {
        super.setFetchSize(fetchSize);
    }

    @Override
    public int getFetchSize() {
        return super.getFetchSize();
    }

    @Override
    public void setMaxRows(int maxRows) {
        super.setMaxRows(maxRows);
    }

    @Override
    public int getMaxRows() {
        return super.getMaxRows();
    }

    @Override
    public void setQueryTimeout(int queryTimeout) {
        super.setQueryTimeout(queryTimeout);
    }

    @Override
    public int getQueryTimeout() {
        return super.getQueryTimeout();
    }

    @Override
    public void setSkipResultsProcessing(boolean skipResultsProcessing) {
        super.setSkipResultsProcessing(skipResultsProcessing);
    }

    @Override
    public boolean isSkipResultsProcessing() {
        return super.isSkipResultsProcessing();
    }

    @Override
    public void setSkipUndeclaredResults(boolean skipUndeclaredResults) {
        super.setSkipUndeclaredResults(skipUndeclaredResults);
    }

    @Override
    public boolean isSkipUndeclaredResults() {
        return super.isSkipUndeclaredResults();
    }

    @Override
    public void setResultsMapCaseInsensitive(boolean resultsMapCaseInsensitive) {
        super.setResultsMapCaseInsensitive(resultsMapCaseInsensitive);
    }

    @Override
    public boolean isResultsMapCaseInsensitive() {
        return super.isResultsMapCaseInsensitive();
    }

    @Override
    public <T> T execute(ConnectionCallback<T> action) throws DataAccessException {
        return super.execute(action);
    }

    @Override
    protected Connection createConnectionProxy(Connection con) {
        return super.createConnectionProxy(con);
    }

    @Override
    public <T> T execute(StatementCallback<T> action) throws DataAccessException {
        return super.execute(action);
    }

    @Override
    public void execute(String sql) throws DataAccessException {
        super.execute(sql);
    }

    @Override
    public <T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException {
        return super.query(sql, rse);
    }

    @Override
    public void query(String sql, RowCallbackHandler rch) throws DataAccessException {
        super.query(sql, rch);
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
        return super.query(sql, rowMapper);
    }

    @Override
    public Map<String, Object> queryForMap(String sql) throws DataAccessException {
        return super.queryForMap(sql);
    }

    @Override
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper) throws DataAccessException {
        return super.queryForObject(sql, rowMapper);
    }

    @Override
    public <T> T queryForObject(String sql, Class<T> requiredType) throws DataAccessException {
        return super.queryForObject(sql, requiredType);
    }

    @Override
    public <T> List<T> queryForList(String sql, Class<T> elementType) throws DataAccessException {
        return super.queryForList(sql, elementType);
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql) throws DataAccessException {
        return super.queryForList(sql);
    }

    @Override
    public SqlRowSet queryForRowSet(String sql) throws DataAccessException {
        return super.queryForRowSet(sql);
    }

    @Override
    public int update(String sql) throws DataAccessException {
        return super.update(sql);
    }

    @Override
    public int[] batchUpdate(String... sql) throws DataAccessException {
        return super.batchUpdate(sql);
    }

    @Override
    public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) throws DataAccessException {
        return super.execute(psc, action);
    }

    @Override
    public <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException {
        return super.execute(sql, action);
    }

    @Override
    public <T> T query(PreparedStatementCreator psc, PreparedStatementSetter pss, ResultSetExtractor<T> rse) throws DataAccessException {
        return super.query(psc, pss, rse);
    }

    @Override
    public <T> T query(PreparedStatementCreator psc, ResultSetExtractor<T> rse) throws DataAccessException {
        return super.query(psc, rse);
    }

    @Override
    public <T> T query(String sql, PreparedStatementSetter pss, ResultSetExtractor<T> rse) throws DataAccessException {
        return super.query(sql, pss, rse);
    }

    @Override
    public <T> T query(String sql, Object[] args, int[] argTypes, ResultSetExtractor<T> rse) throws DataAccessException {
        return super.query(sql, args, argTypes, rse);
    }

    @Override
    public <T> T query(String sql, Object[] args, ResultSetExtractor<T> rse) throws DataAccessException {
        return super.query(sql, args, rse);
    }

    @Override
    public <T> T query(String sql, ResultSetExtractor<T> rse, Object... args) throws DataAccessException {
        return super.query(sql, rse, args);
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
    public <T> List<T> query(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException {
        return super.query(psc, rowMapper);
    }

    @Override
    public <T> List<T> query(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper) throws DataAccessException {
        return super.query(sql, pss, rowMapper);
    }

    @Override
    public <T> List<T> query(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper) throws DataAccessException {
        return super.query(sql, args, argTypes, rowMapper);
    }

    @Override
    public <T> List<T> query(String sql, Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
        return super.query(sql, args, rowMapper);
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
        return super.query(sql, rowMapper, args);
    }

    @Override
    public <T> T queryForObject(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper) throws DataAccessException {
        return super.queryForObject(sql, args, argTypes, rowMapper);
    }

    @Override
    public <T> T queryForObject(String sql, Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
        return super.queryForObject(sql, args, rowMapper);
    }

    @Override
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
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
    public int update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder) throws DataAccessException {
        return super.update(psc, generatedKeyHolder);
    }

    @Override
    public int update(String sql, PreparedStatementSetter pss) throws DataAccessException {
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
    public int[] batchUpdate(String sql, BatchPreparedStatementSetter pss) throws DataAccessException {
        return super.batchUpdate(sql, pss);
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> batchArgs) throws DataAccessException {
        return super.batchUpdate(sql, batchArgs);
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> batchArgs, int[] argTypes) throws DataAccessException {
        return super.batchUpdate(sql, batchArgs, argTypes);
    }

    @Override
    public <T> int[][] batchUpdate(String sql, Collection<T> batchArgs, int batchSize, ParameterizedPreparedStatementSetter<T> pss) throws DataAccessException {
        return super.batchUpdate(sql, batchArgs, batchSize, pss);
    }

    @Override
    public <T> T execute(CallableStatementCreator csc, CallableStatementCallback<T> action) throws DataAccessException {
        return super.execute(csc, action);
    }

    @Override
    public <T> T execute(String callString, CallableStatementCallback<T> action) throws DataAccessException {
        return super.execute(callString, action);
    }

    @Override
    public Map<String, Object> call(CallableStatementCreator csc, List<SqlParameter> declaredParameters) throws DataAccessException {
        return super.call(csc, declaredParameters);
    }

    @Override
    protected Map<String, Object> extractReturnedResults(CallableStatement cs, List<SqlParameter> updateCountParameters, List<SqlParameter> resultSetParameters, int updateCount) throws SQLException {
        return super.extractReturnedResults(cs, updateCountParameters, resultSetParameters, updateCount);
    }

    @Override
    protected Map<String, Object> extractOutputParameters(CallableStatement cs, List<SqlParameter> parameters) throws SQLException {
        return super.extractOutputParameters(cs, parameters);
    }

    @Override
    protected Map<String, Object> processResultSet(ResultSet rs, ResultSetSupportingSqlParameter param) throws SQLException {
        return super.processResultSet(rs, param);
    }

    @Override
    protected RowMapper<Map<String, Object>> getColumnMapRowMapper() {
        return super.getColumnMapRowMapper();
    }

    @Override
    protected <T> RowMapper<T> getSingleColumnRowMapper(Class<T> requiredType) {
        return super.getSingleColumnRowMapper(requiredType);
    }

    @Override
    protected Map<String, Object> createResultsMap() {
        return super.createResultsMap();
    }

    @Override
    protected void applyStatementSettings(Statement stmt) throws SQLException {
        super.applyStatementSettings(stmt);
    }

    @Override
    protected PreparedStatementSetter newArgPreparedStatementSetter(Object[] args) {
        return super.newArgPreparedStatementSetter(args);
    }

    @Override
    protected PreparedStatementSetter newArgTypePreparedStatementSetter(Object[] args, int[] argTypes) {
        return super.newArgTypePreparedStatementSetter(args, argTypes);
    }

    @Override
    protected void handleWarnings(Statement stmt) throws SQLException {
        super.handleWarnings(stmt);
    }

    @Override
    protected void handleWarnings(SQLWarning warning) throws SQLWarningException {
        super.handleWarnings(warning);
    }

    @Override
    protected DataAccessException translateException(String task, String sql, SQLException ex) {
        return super.translateException(task, sql, ex);
    }

    @Override
    public void setDataSource(DataSource dataSource) {
        super.setDataSource(dataSource);
    }

    @Override
    public DataSource getDataSource() {
        return super.getDataSource();
    }

    @Override
    protected DataSource obtainDataSource() {
        return super.obtainDataSource();
    }

    @Override
    public void setDatabaseProductName(String dbName) {
        super.setDatabaseProductName(dbName);
    }

    @Override
    public void setExceptionTranslator(SQLExceptionTranslator exceptionTranslator) {
        super.setExceptionTranslator(exceptionTranslator);
    }

    @Override
    public SQLExceptionTranslator getExceptionTranslator() {
        return super.getExceptionTranslator();
    }

    @Override
    public void setLazyInit(boolean lazyInit) {
        super.setLazyInit(lazyInit);
    }

    @Override
    public boolean isLazyInit() {
        return super.isLazyInit();
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
    }

    /**
     * Returns a hash code value for the object. This method is supported for the benefit of hash tables such as those
     * provided by {@link HashMap}.
     * <p>
     * The general contract of {@code hashCode} is:
     * <ul>
     * <li>Whenever it is invoked on the same object more than once during
     *     an execution of a Java application, the {@code hashCode} method
     *     must consistently return the same integer, provided no information
     *     used in {@code equals} comparisons on the object is modified.
     *     This integer need not remain consistent from one execution of an
     *     application to another execution of the same application.
     * <li>If two objects are equal according to the {@code equals(Object)}
     *     method, then calling the {@code hashCode} method on each of
     *     the two objects must produce the same integer result.
     * <li>It is <em>not</em> required that if two objects are unequal
     *     according to the {@link Object#equals(Object)}
     *     method, then calling the {@code hashCode} method on each of the
     *     two objects must produce distinct integer results.  However, the
     *     programmer should be aware that producing distinct integer results
     *     for unequal objects may improve the performance of hash tables.
     * </ul>
     * <p>
     * As much as is reasonably practical, the hashCode method defined by
     * class {@code Object} does return distinct integers for distinct
     * objects. (This is typically implemented by converting the internal
     * address of the object into an integer, but this implementation
     * technique is not required by the
     * Java&trade; programming language.)
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
     * <p>
     * The {@code equals} method implements an equivalence relation on non-null object references:
     * <ul>
     * <li>It is <i>reflexive</i>: for any non-null reference value
     *     {@code x}, {@code x.equals(x)} should return
     *     {@code true}.
     * <li>It is <i>symmetric</i>: for any non-null reference values
     *     {@code x} and {@code y}, {@code x.equals(y)}
     *     should return {@code true} if and only if
     *     {@code y.equals(x)} returns {@code true}.
     * <li>It is <i>transitive</i>: for any non-null reference values
     *     {@code x}, {@code y}, and {@code z}, if
     *     {@code x.equals(y)} returns {@code true} and
     *     {@code y.equals(z)} returns {@code true}, then
     *     {@code x.equals(z)} should return {@code true}.
     * <li>It is <i>consistent</i>: for any non-null reference values
     *     {@code x} and {@code y}, multiple invocations of
     *     {@code x.equals(y)} consistently return {@code true}
     *     or consistently return {@code false}, provided no
     *     information used in {@code equals} comparisons on the
     *     objects is modified.
     * <li>For any non-null reference value {@code x},
     *     {@code x.equals(null)} should return {@code false}.
     * </ul>
     * <p>
     * The {@code equals} method for class {@code Object} implements
     * the most discriminating possible equivalence relation on objects;
     * that is, for any non-null reference values {@code x} and
     * {@code y}, this method returns {@code true} if and only
     * if {@code x} and {@code y} refer to the same object
     * ({@code x == y} has the value {@code true}).
     * <p>
     * Note that it is generally necessary to override the {@code hashCode}
     * method whenever this method is overridden, so as to maintain the
     * general contract for the {@code hashCode} method, which states
     * that equal objects must have equal hash codes.
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
     * Creates and returns a copy of this object.  The precise meaning of "copy" may depend on the class of the object.
     * The general intent is that, for any object {@code x}, the expression:
     * <blockquote>
     * <pre>
     * x.clone() != x</pre></blockquote>
     * will be true, and that the expression:
     * <blockquote>
     * <pre>
     * x.clone().getClass() == x.getClass()</pre></blockquote>
     * will be {@code true}, but these are not absolute requirements. While it is typically the case that:
     * <blockquote>
     * <pre>
     * x.clone().equals(x)</pre></blockquote>
     * will be {@code true}, this is not an absolute requirement.
     * <p>
     * By convention, the returned object should be obtained by calling {@code super.clone}.  If a class and all of its
     * superclasses (except {@code Object}) obey this convention, it will be the case that {@code x.clone().getClass()
     * == x.getClass()}.
     * <p>
     * By convention, the object returned by this method should be independent of this object (which is being cloned).
     * To achieve this independence, it may be necessary to modify one or more fields of the object returned by {@code
     * super.clone} before returning it.  Typically, this means copying any mutable objects that comprise the internal
     * "deep structure" of the object being cloned and replacing the references to these objects with references to the
     * copies.  If a class contains only primitive fields or references to immutable objects, then it is usually the
     * case that no fields in the object returned by {@code super.clone} need to be modified.
     * <p>
     * The method {@code clone} for class {@code Object} performs a specific cloning operation. First, if the class of
     * this object does not implement the interface {@code Cloneable}, then a {@code CloneNotSupportedException} is
     * thrown. Note that all arrays are considered to implement the interface {@code Cloneable} and that the return type
     * of the {@code clone} method of an array type {@code T[]} is {@code T[]} where T is any reference or primitive
     * type. Otherwise, this method creates a new instance of the class of this object and initializes all its fields
     * with exactly the contents of the corresponding fields of this object, as if by assignment; the contents of the
     * fields are not themselves cloned. Thus, this method performs a "shallow copy" of this object, not a "deep copy"
     * operation.
     * <p>
     * The class {@code Object} does not itself implement the interface {@code Cloneable}, so calling the {@code clone}
     * method on an object whose class is {@code Object} will result in throwing an exception at run time.
     *
     * @return a clone of this instance.
     * @throws CloneNotSupportedException if the object's class does not support the {@code Cloneable} interface.
     *                                    Subclasses that override the {@code clone} method can also throw this
     *                                    exception to indicate that an instance cannot be cloned.
     * @see Cloneable
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
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
        return super.toString();
    }

    /**
     * Called by the garbage collector on an object when garbage collection determines that there are no more references
     * to the object. A subclass overrides the {@code finalize} method to dispose of system resources or to perform
     * other cleanup.
     * <p>
     * The general contract of {@code finalize} is that it is invoked if and when the Java&trade; virtual machine has
     * determined that there is no longer any means by which this object can be accessed by any thread that has not yet
     * died, except as a result of an action taken by the finalization of some other object or class which is ready to
     * be finalized. The {@code finalize} method may take any action, including making this object available again to
     * other threads; the usual purpose of {@code finalize}, however, is to perform cleanup actions before the object is
     * irrevocably discarded. For example, the finalize method for an object that represents an input/output connection
     * might perform explicit I/O transactions to break the connection before the object is permanently discarded.
     * <p>
     * The {@code finalize} method of class {@code Object} performs no special action; it simply returns normally.
     * Subclasses of {@code Object} may override this definition.
     * <p>
     * The Java programming language does not guarantee which thread will invoke the {@code finalize} method for any
     * given object. It is guaranteed, however, that the thread that invokes finalize will not be holding any
     * user-visible synchronization locks when finalize is invoked. If an uncaught exception is thrown by the finalize
     * method, the exception is ignored and finalization of that object terminates.
     * <p>
     * After the {@code finalize} method has been invoked for an object, no further action is taken until the Java
     * virtual machine has again determined that there is no longer any means by which this object can be accessed by
     * any thread that has not yet died, including possible actions by other objects or classes which are ready to be
     * finalized, at which point the object may be discarded.
     * <p>
     * The {@code finalize} method is never invoked more than once by a Java virtual machine for any given object.
     * <p>
     * Any exception thrown by the {@code finalize} method causes the finalization of this object to be halted, but is
     * otherwise ignored.
     *
     * @throws Throwable the {@code Exception} raised by this method
     * @jls 12.6 Finalization of Class Instances
     * @see WeakReference
     * @see PhantomReference
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
