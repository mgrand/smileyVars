package com.markgrand.smileyvars;

import com.markgrand.smileyvars.util.BiSqlConsumer;
import com.markgrand.smileyvars.util.VacuousBiSqlConsumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Date;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SmileyVars enabled version of a prepared statement. You create objects with a SmileyVars template, specify the values
 * of whichever parameters you want to specify and then run the query. It works internally by creating a {@code
 * PreparedStatement} object with the template expansion.
 * <p>The SmileyVars template language is documented at
 * <a href="https://mgrand.github.io/smileyVars/">https://mgrand.github.io/smileyVars/</a>.</p>
 * <p>All of this class's set methods return this object, so you can use a fluent coding style to configure a prepared
 * statement like this:</p>
 * <pre>
 *     String sql = "SELECT x,y FROM square WHERE 1=1 (: AND x=:x:)(: AND y=:y :)";
 *     SmileyVarsPreparedStatement svps2 = new SmileyVarsPreparedStatement(connection, sql).setInt("x", 2).setInt("y", 4);
 * </pre>
 * <p>
 * Since {@code SmileyVarsPreparedStatement} does not know which parameters you will or won't specify for the template,
 * it does not create the underlying {@code PreparedStatement} object until you try to execute the query. This means
 * that is any exceptions are going to be thrown as a result of setting a parameter or some other attribute of a {@code
 * PreparedStatement} object, they will not be thrown when make a call to set the parameter or attribute, but rather
 * when you execute the query or do something else that requires the {@code PreparedStatement} to be created.
 *
 * <p>This class uses {@link PreparedStatement} objects to implement database interactions. To the extent practical,
 * it attempts to reuse the same {@code PreparedStatement} object for operations.</p>
 *
 * <p>If two operations are done with exactly the same SmileyVars having values (it can be different values from one
 * operation to the next), then they will use the same {@code PreparedStatement} object. When different combinations of
 * SmileyVars have values then different {@code PreparedStatement} objects will be used.</p>
 *
 * <p>The underlying {@code PreparedStatement} objects are closed when this object's close method is called. It is a
 * recommended good practice to always close {@code SmileyVarsPreparedStatement} objects.</p>
 * <p>There is a related class, {@link SmileyVarsTemplate}, that expands SmileyVars templates to strings. To decide
 * which class you want to use keep these things in mind:</p>
 * <ul>
 * <li>If you want to work with SQL statements, {@link SmileyVarsTemplate} is the one to use. If you want to work with
 * prepared statements, use {@link SmileyVarsPreparedStatement}.</li>
 * <li>{@link SmileyVarsTemplate} con infer the SQL type of the values you provide or you can explicitly include the
 * type of a variable in the template body. {@link SmileyVarsPreparedStatement} requires you to use methods named for
 * Java types to specify values.</li>
 * <li>If you are working with large blobs or clobs, {@link SmileyVarsPreparedStatement} may perform better. It allows
 * you to specify large blob or clob values using write methods that send the value directly to the database without
 * having to represent it as an SQL literal.</li>
 * </ul>
 *
 * @author Mark Grand
 * @see SmileyVarsTemplate
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public class SmileyVarsPreparedStatement implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SmileyVarsPreparedStatement.class);
    @NotNull
    private final Connection connection;
    @NotNull
    private final SmileyVarsTemplate template;

    /**
     * Map SmileyVar names to BiSqlConsumer objects that set a value of a parameter in a PreparedStatement object.
     */
    private final SortedMap<String, BiSqlConsumer<PreparedStatement, Integer>> valueMap = new TreeMap<>();

    /**
     * PreparedStatement objects are collected in this map so they can be reused. The goal of the reuse is to use the
     * same PreparedStatement object for operations that are done with the same SmileyVars having values.
     */
    private final Map<BitSet, PreparedStatementTag> taggedPstmtMap = new HashMap<>();

    private boolean closed = false;
    private long changeCount = 0;

    // Fields related to prepared statement configuration values.
    private Optional<Integer> maxFieldSize = Optional.empty();
    private Optional<Integer> maxRows = Optional.empty();
    private Optional<Long> largeMaxRows = Optional.empty();
    private Optional<Integer> queryTimeout = Optional.empty();
    private Optional<String> cursorName = Optional.empty();
    private Optional<Integer> fetchDirection = Optional.empty();
    private Optional<Integer> fetchSize = Optional.empty();
    private Optional<Boolean> poolable = Optional.empty();

    /**
     * Constructor
     *
     * @param conn the connection to use for interacting with the database. This is used to determine the type of the
     *             database engine on the other end fo the connection.
     * @param sql  the SQL of the template.
     * @throws SQLException if there is a problem with the connection.
     */
    public SmileyVarsPreparedStatement(@NotNull Connection conn, @NotNull String sql) throws SQLException {
        logger.trace("Constructing SmileyVars prepared statement for {}", sql);
        connection = conn;
        template = SmileyVarsTemplate.template(conn, sql, ValueFormatterRegistry.preparedStatementInstance());
        template.getVarNames().forEach(name -> valueMap.put(name, VacuousBiSqlConsumer.getInstance()));
    }

    /**
     * Executes the SQL query in this {@code PreparedStatement} object and returns the {@code ResultSet} object
     * generated by the query.
     *
     * @return a <code>ResultSet</code> object that contains the data produced by the query; never <code>null</code>
     * @throws SQLException        if a database access error occurs; this method is called on a closed {@code
     *                             PreparedStatement} or the SQL statement does not return a {@code ResultSet} object
     * @throws SQLTimeoutException when the driver has determined that the timeout value that was specified by the
     *                             {@code setQueryTimeout} method has been exceeded and has at least attempted to cancel
     *                             the currently running {@code Statement}
     */
    public ResultSet executeQuery() throws SQLException {
        return getPreparedStatement().executeQuery();
    }

    /**
     * Executes the SQL statement in this {@code PreparedStatement} object, which must be an SQL Data Manipulation
     * Language (DML) statement, such as {@code INSERT}, {@code UPDATE} or {@code DELETE}; or an SQL statement that
     * returns nothing, such as a DDL statement.
     *
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements or (2) 0 for SQL statements
     * that return nothing
     * @throws SQLException        if a database access error occurs; this method is called on a closed {@code
     *                             PreparedStatement} or the SQL statement returns a {@code ResultSet} object
     * @throws SQLTimeoutException when the driver has determined that the timeout value that was specified by the
     *                             {@code setQueryTimeout} method has been exceeded and has at least attempted to cancel
     *                             the currently running {@code Statement}
     */
    public int executeUpdate() throws SQLException {
        return getPreparedStatement().executeUpdate();
    }

    /**
     * Sets the designated parameter to SQL <code>NULL</code>.
     *
     * <P><B>Note:</B> You must specify the parameter's SQL type.
     *
     * @param parameterName The name of the parameter.
     * @param sqlType       The SQL type code defined in <code>java.sql.Types</code>
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     */
    public SmileyVarsPreparedStatement setNull(String parameterName, int sqlType) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setNull(i, sqlType));
        return this;
    }

    /**
     * Sets the designated parameter to the given Java <code>boolean</code> value.
     *
     * @param parameterName The name of the parameter.
     * @param value         the parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     */
    public SmileyVarsPreparedStatement setBoolean(String parameterName, boolean value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setBoolean(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to the given Java <code>byte</code> value. The driver converts this to an SQL
     * <code>TINYINT</code> value when it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         the parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     */
    public SmileyVarsPreparedStatement setByte(String parameterName, byte value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setByte(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to the given Java <code>short</code> value. The driver converts this to an SQL
     * <code>SMALLINT</code> value when it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         the parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     */
    public SmileyVarsPreparedStatement setShort(String parameterName, short value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setShort(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to the given Java <code>int</code> value. The driver converts this to an SQL
     * <code>INTEGER</code> value when it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         the parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     */
    public SmileyVarsPreparedStatement setInt(String parameterName, int value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setInt(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to the given Java <code>long</code> value. The driver converts this to an SQL
     * <code>BIGINT</code> value when it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         the parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     */
    public SmileyVarsPreparedStatement setLong(String parameterName, long value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setLong(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to the given Java <code>float</code> value. The driver converts this to an SQL
     * <code>REAL</code> value when it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         the parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     */
    public SmileyVarsPreparedStatement setFloat(String parameterName, float value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setFloat(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to the given Java <code>double</code> value. The driver converts this to an SQL
     * <code>DOUBLE</code> value when it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         the parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     */
    public SmileyVarsPreparedStatement setDouble(String parameterName, double value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setDouble(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to the given <code>java.math.BigDecimal</code> value. The driver converts this to
     * an SQL <code>NUMERIC</code> value when it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         the parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     */
    public SmileyVarsPreparedStatement setBigDecimal(String parameterName, BigDecimal value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setBigDecimal(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to the given Java <code>String</code> value. The driver converts this to an SQL
     * <code>VARCHAR</code> or <code>LONGVARCHAR</code> value (depending on the argument's size relative to the
     * driver's limits on <code>VARCHAR</code> values) when it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         the parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     */
    public SmileyVarsPreparedStatement setString(String parameterName, String value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setString(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to the given Java array of bytes.  The driver converts this to an SQL {@code
     * VARBINARY} or {@code LONGVARBINARY} (depending on the argument's size relative to the driver's limits on
     * <code>VARBINARY</code> values) when it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         the parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     */
    public SmileyVarsPreparedStatement setBytes(String parameterName, byte[] value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setBytes(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Date</code> value using the default time zone of the
     * virtual machine that is running the application. The driver converts this to an SQL <code>DATE</code> value when
     * it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         the parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     */
    public SmileyVarsPreparedStatement setDate(String parameterName, Date value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setDate(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Time</code> value. The driver converts this to an SQL
     * <code>TIME</code> value when it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         the parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     */
    public SmileyVarsPreparedStatement setTime(String parameterName, Time value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setTime(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value. The driver converts this to an
     * SQL <code>TIMESTAMP</code> value when it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         the parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     */
    public SmileyVarsPreparedStatement setTimestamp(String parameterName, Timestamp value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setTimestamp(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to the given input stream, which will have the specified number of bytes. When a
     * very large ASCII value is input to a {@code LONGVARCHAR} parameter, it may be more practical to send it via a
     * {@code java.io.InputStream}. Data will be read from the stream as needed until end-of-file is reached.  The JDBC
     * driver will do any necessary conversion from ASCII to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the standard interface.
     *
     * @param parameterName The name of the parameter.
     * @param inputStream   the Java input stream that contains the ASCII parameter value
     * @param length        the number of bytes in the stream
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     */
    public SmileyVarsPreparedStatement setAsciiStream(String parameterName, InputStream inputStream, int length) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setAsciiStream(i, inputStream, length));
        return this;
    }

    /**
     * Sets the designated parameter to the given input stream, which will have the specified number of bytes. When a
     * very large binary value is input to a <code>LONGVARBINARY</code> parameter, it may be more practical to send it
     * via a {@code java.io.InputStream} object. The data will be read from the stream as needed until end-of-file is
     * reached.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the standard interface.
     *
     * @param parameterName The name of the parameter.
     * @param inputStream   the Java input stream that contains the binary parameter value
     * @param length        the number of bytes in the stream
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     */
    public SmileyVarsPreparedStatement setBinaryStream(String parameterName, InputStream inputStream, int length) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setBinaryStream(i, inputStream, length));
        return this;
    }

    /**
     * Sets the value of the designated parameter with the given object.
     * <p>
     * This method is similar to {@link #setObject(String parameterName, Object object, int targetSqlType, int
     * scaleOrLength)}, except that it assumes a scale of zero.
     *
     * @param parameterName The name of the parameter.
     * @param obj           the object containing the Object parameter value
     * @param targetSqlType the SQL type (as defined in java.sql.Types) to be sent to the database
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     * @see Types
     */
    public SmileyVarsPreparedStatement setObject(String parameterName, Object obj, int targetSqlType) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setObject(i, obj, targetSqlType));
        return this;
    }

    /**
     * <p>Sets the value of the designated parameter using the given object.
     *
     * <p>The JDBC specification specifies a standard mapping from Java <code>Object</code> types to SQL types.  The
     * given argument will be converted to the corresponding SQL type before being sent to the database.
     *
     * <p>
     * If the object is of a class implementing the interface <code>SQLData</code>, the JDBC driver should call the
     * method <code>SQLData.writeSQL</code> to write it to the SQL data stream. If, on the other hand, the object is of
     * a class implementing <code>Ref</code>, <code>Blob</code>, <code>Clob</code>, <code>NClob</code>, {@code Struct},
     * <code>java.net.URL</code>, <code>RowId</code>, <code>SQLXML</code> or <code>Array</code>, the driver should pass
     * it to the database as a value of the corresponding SQL type.
     * <p>
     * <b>Note:</b> Not all databases allow for a non-typed Null to be sent to
     * the backend. For maximum portability, the <code>setNull</code> or the
     * <code>setObject(int parameterIndex, Object x, int sqlType)</code>
     * method should be used instead of <code>setObject(int parameterIndex, Object x)</code>.
     *
     * @param parameterName The name of the parameter.
     * @param value         the object containing the Object parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template or
     *                             this object has been closed.
     */
    public SmileyVarsPreparedStatement setObject(String parameterName, Object value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setObject(i, value));
        return this;
    }

    /**
     * Executes the SQL statement in this <code>PreparedStatement</code> object, which may be any kind of SQL statement.
     * Some prepared statements return multiple results; the <code>execute</code> method handles these complex
     * statements as well as the simpler form of statements handled by the methods <code>executeQuery</code> and
     * <code>executeUpdate</code>.
     * <p>
     * The <code>execute</code> method returns a <code>boolean</code> to indicate the form of the first result.  You
     * must call either the method
     * <code>getResultSet</code> or <code>getUpdateCount</code>
     * to retrieve the result; you must call <code>getMoreResults</code> to move to any subsequent result(s).
     *
     * @return <code>true</code> if the first result is a <code>ResultSet</code>
     * object; <code>false</code> if the first result is an update count or there is no result
     * @throws SQLException        if a database access error occurs; this method is called on a closed
     *                             <code>PreparedStatement</code> or an argument is supplied to this method
     * @throws SQLTimeoutException when the driver has determined that the timeout value that was specified by the
     *                             {@code setQueryTimeout} method has been exceeded and has at least attempted to cancel
     *                             the currently running {@code Statement}
     * @see Statement#execute
     * @see Statement#getResultSet
     * @see Statement#getUpdateCount
     * @see Statement#getMoreResults
     */
    public boolean execute() throws SQLException {
        return getPreparedStatement().execute();
    }

    /**
     * Sets the designated parameter to the given <code>Reader</code> object, which is the given number of characters
     * long. When a very large UNICODE value is input to a <code>LONGVARCHAR</code> parameter, it may be more practical
     * to send it via a
     * <code>java.io.Reader</code> object. The data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will do any necessary conversion from UNICODE to the
     * database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the standard interface.
     *
     * @param parameterName The name of the parameter.
     * @param reader        the <code>java.io.Reader</code> object that contains the Unicode data
     * @param length        the number of characters in the stream
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setCharacterStream(@NotNull String parameterName, @NotNull Reader reader, int length) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setCharacterStream(i, reader, length));
        return this;
    }

    /**
     * Sets the designated parameter to the given
     * <code>REF(&lt;structured-type&gt;)</code> value.
     * The driver converts this to an SQL <code>REF</code> value when it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         an SQL <code>REF</code> value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setRef(@NotNull String parameterName, Ref value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setRef(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Blob</code> object. The driver converts this to an SQL
     * <code>BLOB</code> value when it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         a <code>Blob</code> object that maps an SQL <code>BLOB</code> value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setBlob(@NotNull String parameterName, Blob value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setBlob(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Clob</code> object. The driver converts this to an SQL
     * <code>CLOB</code> value when it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         a <code>Clob</code> object that maps an SQL <code>BLOB</code> value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setClob(@NotNull String parameterName, Clob value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setClob(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Array</code> object. The driver converts this to an
     * SQL
     * <code>ARRAY</code> value when it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         an <code>Array</code> object that maps an SQL <code>ARRAY</code> value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setArray(String parameterName, Array value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setArray(i, value));
        return this;
    }

    /**
     * Retrieves a <code>ResultSetMetaData</code> object that contains information about the columns of the
     * <code>ResultSet</code> object that will be returned when this <code>PreparedStatement</code> object is executed.
     * <p>
     * Because a <code>PreparedStatement</code> object is precompiled, it is possible to know about the
     * <code>ResultSet</code> object that it will return without having to execute it.  Consequently, it is possible to
     * invoke the method <code>getMetaData</code> on a
     * <code>PreparedStatement</code> object rather than waiting to execute
     * it and then invoking the <code>ResultSet.getMetaData</code> method on the <code>ResultSet</code> object that is
     * returned.
     * <p>
     * <B>NOTE:</B> Using this method may be expensive for some drivers due
     * to the lack of underlying DBMS support.
     *
     * @return the description of a <code>ResultSet</code> object's columns or
     * <code>null</code> if the driver cannot return a
     * <code>ResultSetMetaData</code> object
     * @throws SQLException                    if a database access error occurs or this method is called on a closed
     *                                         <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     */
    public ResultSetMetaData getMetaData() throws SQLException {
        return getPreparedStatement().getMetaData();
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Date</code> value, using the given
     * <code>Calendar</code> object.  The driver uses the <code>Calendar</code> object to construct an SQL
     * <code>DATE</code> value, which the driver then sends to the database.  With a <code>Calendar</code> object, the
     * driver can calculate the date taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterName The name of the parameter.
     * @param value         the parameter value
     * @param calendar      the <code>Calendar</code> object the driver will use to construct the date
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setDate(String parameterName, Date value, Calendar calendar) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setDate(i, value, calendar));
        return this;
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Time</code> value, using the given
     * <code>Calendar</code> object.  The driver uses the <code>Calendar</code> object to construct an SQL
     * <code>TIME</code> value, which the driver then sends to the database.  With a <code>Calendar</code> object, the
     * driver can calculate the time taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterName The name of the parameter.
     * @param time          the parameter value
     * @param calendar      the <code>Calendar</code> object the driver will use to construct the time
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setTime(String parameterName, Time time, Calendar calendar) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setTime(i, time, calendar));
        return this;
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value, using the given
     * <code>Calendar</code> object.  The driver uses the <code>Calendar</code> object to construct an SQL
     * <code>TIMESTAMP</code> value, which the driver then sends to the database.  With a
     * <code>Calendar</code> object, the driver can calculate the timestamp
     * taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterName The name of the parameter.
     * @param timestamp     the parameter value
     * @param calendar      the <code>Calendar</code> object the driver will use to construct the timestamp
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setTimestamp(String parameterName, Timestamp timestamp, Calendar calendar) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setTimestamp(i, timestamp, calendar));
        return this;
    }

    /**
     * Sets the designated parameter to SQL <code>NULL</code>. This version of the method <code>setNull</code> should be
     * used for user-defined types and REF type parameters.  Examples of user-defined types include: STRUCT, DISTINCT,
     * JAVA_OBJECT, and named array types.
     *
     * <P><B>Note:</B> To be portable, applications must give the
     * SQL type code and the fully-qualified SQL type name when specifying a NULL user-defined or REF parameter.  In the
     * case of a user-defined type the name is the type name of the parameter itself.  For a REF parameter, the name is
     * the type name of the referenced type.  If a JDBC driver does not need the type code or type name information, it
     * may ignore it.
     * <p>
     * Although it is intended for user-defined and Ref parameters, this method may be used to set a null parameter of
     * any JDBC type. If the parameter does not have a user-defined or REF type, the given typeName is ignored.
     *
     * @param parameterName The name of the parameter.
     * @param sqlType       a value from <code>java.sql.Types</code>
     * @param typeName      the fully-qualified name of an SQL user-defined type; ignored if the parameter is not a
     *                      user-defined type or REF
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setNull(String parameterName, int sqlType, String typeName) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setNull(i, sqlType, typeName));
        return this;
    }

    /**
     * Sets the designated parameter to the given <code>java.net.URL</code> value. The driver converts this to an SQL
     * <code>DATALINK</code> value when it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param url           the <code>java.net.URL</code> object to be set
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setURL(String parameterName, URL url) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setURL(i, url));
        return this;
    }

    /**
     * Retrieves the number, types and properties of this
     * <code>PreparedStatement</code> object's parameters.
     *
     * @return a <code>ParameterMetaData</code> object that contains information about the number, types and properties
     * for each parameter marker of this <code>PreparedStatement</code> object
     * @throws SQLException if a database access error occurs or this method is called on a closed
     *                      <code>PreparedStatement</code>
     */
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return getPreparedStatement().getParameterMetaData();
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.RowId</code> object. The driver converts this to a SQL
     * <code>ROWID</code> value when it sends it to the database
     *
     * @param parameterName The name of the parameter.
     * @param value         the parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setRowId(String parameterName, RowId value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setRowId(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to the given <code>String</code> object. The driver converts this to a SQL
     * <code>NCHAR</code> or
     * <code>NVARCHAR</code> or <code>LONGNVARCHAR</code> value
     * (depending on the argument's size relative to the driver's limits on <code>NVARCHAR</code> values) when it sends
     * it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         the parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setNString(String parameterName, String value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setNString(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object. The
     * <code>Reader</code> reads the data till end-of-file is reached. The
     * driver does the necessary conversion from Java character format to the national character set in the database.
     *
     * @param parameterName The name of the parameter.
     * @param reader        the <code>java.io.Reader</code> object that contains the Unicode data
     * @param length        the number of characters in the stream
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setNCharacterStream(String parameterName, Reader reader, long length) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setNCharacterStream(i, reader, length));
        return this;
    }

    /**
     * Sets the designated parameter to a <code>java.sql.NClob</code> object. The driver converts this to a SQL
     * <code>NCLOB</code> value when it sends it to the database.
     *
     * @param parameterName The name of the parameter.
     * @param value         the parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setNClob(String parameterName, NClob value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setNClob(i, value));
        return this;
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object.  The reader must contain the number of characters
     * specified by length otherwise a <code>SQLException</code> will be generated when the
     * <code>PreparedStatement</code> is executed. This method differs from the <code>setCharacterStream (int, Reader,
     * int)</code> method because it informs the driver that the parameter value should be sent to the server as a
     * <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used, the driver may have to do extra
     * work to determine whether the parameter data should be sent to the server as a <code>LONGVARCHAR</code> or a
     * <code>CLOB</code>
     *
     * @param parameterName The name of the parameter.
     * @param reader        An object that contains the data to set the parameter value to.
     * @param length        the number of characters in the parameter data.
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setClob(String parameterName, Reader reader, long length) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setClob(i, reader, length));
        return this;
    }

    /**
     * Sets the designated parameter to a <code>InputStream</code> object.  The inputStream must contain  the number of
     * characters specified by length otherwise a <code>SQLException</code> will be generated when the
     * <code>PreparedStatement</code> is executed. This method differs from the <code>setBinaryStream (int,
     * InputStream, int)</code> method because it informs the driver that the parameter value should be sent to the
     * server as a
     * <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used, the driver may have to do extra work
     * to determine whether the parameter data should be sent to the server as a <code>LONGVARBINARY</code> or a
     * <code>BLOB</code>
     *
     * @param parameterName The name of the parameter.
     * @param inputStream   An object that contains the data to set the parameter value to.
     * @param length        the number of bytes in the parameter data.
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setBlob(String parameterName, InputStream inputStream, long length) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setBlob(i, inputStream, length));
        return this;
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object.  The reader must contain  the number of characters
     * specified by length otherwise a <code>SQLException</code> will be generated when the
     * <code>PreparedStatement</code> is executed. This method differs from the <code>setCharacterStream (int, Reader,
     * int)</code> method because it informs the driver that the parameter value should be sent to the server as a
     * <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the driver may have to do extra
     * work to determine whether the parameter data should be sent to the server as a <code>LONGNVARCHAR</code> or a
     * <code>NCLOB</code>
     *
     * @param parameterName The name of the parameter.
     * @param reader        An object that contains the data to set the parameter value to.
     * @param length        the number of characters in the parameter data.
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setNClob(String parameterName, Reader reader, long length) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setNClob(i, reader, length));
        return this;
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.SQLXML</code> object. The driver converts this to an
     * SQL <code>XML</code> value when it sends it to the database.
     * <p>
     *
     * @param parameterName The name of the parameter.
     * @param value         a <code>SQLXML</code> object that maps an SQL <code>XML</code> value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setSQLXML(String parameterName, SQLXML value) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setSQLXML(i, value));
        return this;
    }

    /**
     * <p>Sets the value of the designated parameter with the given object.
     * </p>
     *
     * <p>If the second argument is an <code>InputStream</code> then the stream must contain the number of bytes
     * specified by scaleOrLength.  If the second argument is a <code>Reader</code> then the reader must contain the
     * number of characters specified by scaleOrLength. If these conditions are not true the driver will generate a
     * <code>SQLException</code> when the prepared statement is executed.</p>
     *
     * <p>The given Java object will be converted to the given targetSqlType before being sent to the database.</p>
     *
     * <p>If the object has a custom mapping (is of a class implementing the interface <code>SQLData</code>), the JDBC
     * driver should call the method <code>SQLData.writeSQL</code> to write it to the SQL data stream. If, on the other
     * hand, the object is of a class implementing <code>Ref</code>, <code>Blob</code>, <code>Clob</code>,
     * <code>NClob</code>, <code>Struct</code>, <code>java.net.URL</code>, or <code>Array</code>, the driver should
     * pass it to the database as a value of the corresponding SQL type.</p>
     *
     * <p>Note that this method may be used to pass database-specific abstract data types.</p>
     *
     * @param parameterName The name of the parameter.
     * @param object        the object containing the input parameter value
     * @param targetSqlType the SQL type (as defined in java.sql.Types) to be sent to the database. The scale argument
     *                      may further qualify this type.
     * @param scaleOrLength for <code>java.sql.Types.DECIMAL</code> or <code>java.sql.Types.NUMERIC types</code>, this
     *                      is the number of digits after the decimal point. For Java Object types
     *                      <code>InputStream</code> and <code>Reader</code>, this is the length of the data in the
     *                      stream or reader.  For all other types, this value will be ignored.
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     * @see Types
     */
    public SmileyVarsPreparedStatement setObject(String parameterName, Object object, int targetSqlType, int scaleOrLength) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setObject(i, object, targetSqlType, scaleOrLength));
        return this;
    }

    /**
     * Sets the designated parameter to the given input stream, which will have the specified number of bytes. When a
     * very large ASCII value is input to a <code>LONGVARCHAR</code> parameter, it may be more practical to send it via
     * a
     * <code>java.io.InputStream</code>. Data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will do any necessary conversion from ASCII to the
     * database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the standard interface.
     *
     * @param parameterName The name of the parameter.
     * @param inputStream   the Java input stream that contains the ASCII parameter value
     * @param length        the number of bytes in the stream
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setAsciiStream(String parameterName, InputStream inputStream, long length) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setAsciiStream(i, inputStream, length));
        return this;
    }

    /**
     * Sets the designated parameter to the given input stream, which will have the specified number of bytes. When a
     * very large binary value is input to a <code>LONGVARBINARY</code> parameter, it may be more practical to send it
     * via a
     * <code>java.io.InputStream</code> object. The data will be read from the
     * stream as needed until end-of-file is reached.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the standard interface.
     *
     * @param parameterName The name of the parameter.
     * @param inputStream   the java input stream which contains the binary parameter value
     * @param length        the number of bytes in the stream
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setBinaryStream(@NotNull String parameterName, @NotNull InputStream inputStream, long length) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setBinaryStream(i, inputStream, length));
        return this;
    }

    /**
     * Sets the designated parameter to the given <code>Reader</code> object, which is the given number of characters
     * long. When a very large UNICODE value is input to a <code>LONGVARCHAR</code> parameter, it may be more practical
     * to send it via a <code>java.io.Reader</code> object. The data will be read from the stream as needed until
     * end-of-file is reached.  The JDBC driver will do any necessary conversion from UNICODE to the database char
     * format.
     *
     * <P><B>Note:</B> This stream object can either be a standard Java Reader object or your own subclass that
     * implements the standard interface.
     *
     * @param parameterName The name of the parameter.
     * @param reader        the <code>java.io.Reader</code> object that contains the Unicode data
     * @param length        the number of characters in the stream
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setCharacterStream(String parameterName, Reader reader, long length) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setCharacterStream(i, reader, length));
        return this;
    }

    /**
     * Sets the designated parameter to the given input stream. When a very large ASCII value is input to a {@code
     * LONGVARCHAR} parameter, it may be more practical to send it via a {@code java.io.InputStream}. Data will be read
     * from the stream as needed until end-of-file is reached.  The JDBC driver will do any necessary conversion from
     * ASCII to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard Java stream object or your own subclass that
     * implements the standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if it might be more efficient to use a
     * version of <code>setAsciiStream</code> which takes a length parameter.
     *
     * @param parameterName The name of the parameter.
     * @param inputStream   the Java input stream that contains the ASCII parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setAsciiStream(String parameterName, InputStream inputStream) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setAsciiStream(i, inputStream));
        return this;
    }

    /**
     * Sets the designated parameter to the given input stream. When a very large binary value is input to a
     * <code>LONGVARBINARY</code> parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code> object. The data will be read from the
     * stream as needed until end-of-file is reached.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setBinaryStream</code> which takes a length parameter.
     *
     * @param parameterName The name of the parameter.
     * @param inputStream   the java input stream which contains the binary parameter value
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setBinaryStream(String parameterName, InputStream inputStream) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setBinaryStream(i, inputStream));
        return this;
    }

    /**
     * Sets the designated parameter to the given <code>Reader</code> object. When a very large UNICODE value is input
     * to a <code>LONGVARCHAR</code> parameter, it may be more practical to send it via a
     * <code>java.io.Reader</code> object. The data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will do any necessary conversion from UNICODE to the
     * database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setCharacterStream</code> which takes a length parameter.
     *
     * @param parameterName The name of the parameter.
     * @param reader        the <code>java.io.Reader</code> object that contains the Unicode data
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setCharacterStream(@NotNull String parameterName, @NotNull Reader reader) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setCharacterStream(i, reader));
        return this;
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object. The
     * <code>Reader</code> reads the data till end-of-file is reached. The
     * driver does the necessary conversion from Java character format to the national character set in the database.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setNCharacterStream</code> which takes a length parameter.
     *
     * @param parameterName The name of the parameter.
     * @param reader        the parameter reader
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setNCharacterStream(@NotNull String parameterName, @NotNull Reader reader) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setNCharacterStream(i, reader));
        return this;
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object. This method differs from the
     * <code>setCharacterStream (int, Reader)</code> method because it informs the driver that the parameter value
     * should be sent to the server as a <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used,
     * the driver may have to do extra work to determine whether the parameter data should be sent to the server as a
     * <code>LONGVARCHAR</code> or a <code>CLOB</code>
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setClob</code> which takes a length parameter.
     *
     * @param parameterName The name of the parameter.
     * @param reader        An object that contains the data to set the parameter value to.
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setClob(@NotNull String parameterName, @NotNull Reader reader) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setClob(i, reader));
        return this;
    }

    /**
     * Sets the designated parameter to a <code>InputStream</code> object. This method differs from the
     * <code>setBinaryStream (int, InputStream)</code> method because it informs the driver that the parameter value
     * should be sent to the server as a <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter data should be sent to the server as a
     * <code>LONGVARBINARY</code> or a <code>BLOB</code>
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setBlob</code> which takes a length parameter.
     *
     * @param parameterName The name of the parameter.
     * @param inputStream   An object that contains the data to set the parameter value to.
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setBlob(@NotNull String parameterName, @NotNull InputStream inputStream) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setBlob(i, inputStream));
        return this;
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object. This method differs from the
     * <code>setCharacterStream (int, Reader)</code> method because it informs the driver that the parameter value
     * should be sent to the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used,
     * the driver may have to do extra work to determine whether the parameter data should be sent to the server as a
     * <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setNClob</code> which takes a length parameter.
     *
     * @param parameterName The name of the parameter.
     * @param reader        An object that contains the data to set the parameter value to.
     * @return this object
     * @throws SmileyVarsException If parameterName does not correspond to a variable in the SmilelyVars template.
     */
    public SmileyVarsPreparedStatement setNClob(String parameterName, Reader reader) {
        changeWithCheckedName(parameterName, (pstmt, i) -> pstmt.setNClob(i, reader));
        return this;
    }

    /**
     * Releases the underlying <code>PreparedStatement</code> objects. This also clears all of the parameter values that
     * have been provided. It is generally good practice to close this object soon as you are finished with it to avoid
     * tying up database resources.
     *
     * @throws SQLException if a database access error occurs
     * @see #clearParameters()
     */
    @Override
    public void close() throws SQLException {
        Iterator<Map.Entry<BitSet, PreparedStatementTag>> iterator = taggedPstmtMap.entrySet().iterator();
        while (iterator.hasNext()) {
            iterator.next().getValue().getPreparedStatement().close();
            iterator.remove();
        }
        clearParameters();
        closed = true;
    }

    /**
     * Clear the value for the named SmileyVar.
     *
     * @param name The name of the SmileyVar whose value is to be cleared.
     * @return this object
     */
    public SmileyVarsPreparedStatement clearParameter(@NotNull String name) {
        ensureNotClosed();
        valueMap.replace(name, VacuousBiSqlConsumer.getInstance());
        return this;
    }

    /**
     * Determine if the named smileyVar has a value set for it.
     *
     * @param name The name of the smileyVar to check.
     * @return true of the named smileyVar has a value; otherwise false.
     */
    public boolean isParameterSet(String name) {
        ensureNotClosed();
        BiSqlConsumer<PreparedStatement, Integer> biSqlConsumer = valueMap.get(name);
        return biSqlConsumer != null && !biSqlConsumer.isVacuous();
    }

    /**
     * Clears the current parameter values immediately. This just clears the values that have been set for SmileyVars.
     * If this ios being done to release resources, call {@link #deepClearParameters()}, which also clears the parameter
     * values in the underlying {@link PreparedStatement} objects.
     *
     * @return this object
     * @see #deepClearParameters()
     */
    @SuppressWarnings("UnusedReturnValue")
    public SmileyVarsPreparedStatement clearParameters() {
        ensureNotClosed();
        valueMap.entrySet().forEach(entry -> entry.setValue(VacuousBiSqlConsumer.getInstance()));
        changeCount++;
        return this;
    }

    /**
     * Clears the current parameter values immediately. This clears both the values that have been set for SmileyVars
     * and closes the underlying {@link PreparedStatement} objects. This can be useful for releasing the resources used
     * by the current parameter values.
     * <p>This does not effect any previously set configuration values (maxRows, maxFieldSize, fetchDirection,
     * ...).</p>
     *
     * @return this object
     * @throws SQLException if a database access error occurs or this method is called on a closed
     *                      <code>PreparedStatement</code>
     * @see #clearParameters()
     */
    public SmileyVarsPreparedStatement deepClearParameters() throws SQLException {
        clearParameters();
        Iterator<PreparedStatementTag> iterator = taggedPstmtMap.values().iterator();
        while (iterator.hasNext()) {
            iterator.next().getPreparedStatement().close();
            iterator.remove();
        }
        return this;
    }

    /**
     * Retrieves the maximum number of bytes that can be returned for character and binary column values in a
     * <code>ResultSet</code> object produced by this <code>Statement</code> object. This limit applies only to
     * <code>BINARY</code>, <code>VARBINARY</code>, <code>LONGVARBINARY</code>, <code>CHAR</code>,
     * <code>VARCHAR</code>,  <code>NCHAR</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>
     * and <code>LONGVARCHAR</code> columns.  If the limit is exceeded, the excess data is silently discarded.
     *
     * @return the current column size limit for columns storing character and binary values; zero means there is no
     * limit. This value is obtained by calling the corresponding get method of the {@code PreparedStatement} object
     * that is returned by this object's {@code #getPreparedStatement} method.
     * @throws SQLException if a database access error occurs or this method is called on a closed
     *                      <code>Statement</code>.
     * @see #setMaxFieldSize
     */
    public int getMaxFieldSize() throws SQLException {
        return getPreparedStatement().getMaxFieldSize();
    }

    /**
     * Sets the limit for the maximum number of bytes that can be returned for character and binary column values in a
     * <code>ResultSet</code> object produced by this <code>Statement</code> object.
     * <p>
     * This limit applies only to <code>BINARY</code>, <code>VARBINARY</code>,
     * <code>LONGVARBINARY</code>, <code>CHAR</code>, <code>VARCHAR</code>,
     * <code>NCHAR</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code> and
     * <code>LONGVARCHAR</code> fields.  If the limit is exceeded, the excess data
     * is silently discarded. For maximum portability, use values greater than 256.
     *
     * @param max the new column size limit in bytes; zero means there is no limit
     * @return this object
     * @see #getMaxFieldSize
     */
    public SmileyVarsPreparedStatement setMaxFieldSize(int max) {
        maxFieldSize = Optional.of(max);
        changeCount++;
        return this;
    }

    /**
     * Retrieves the maximum number of rows that a <code>ResultSet</code> object produced by this <code>Statement</code>
     * object can contain.  If this limit is exceeded, the excess rows are silently dropped.
     *
     * @return the current maximum number of rows for a <code>ResultSet</code> object produced by this
     * <code>Statement</code> object; zero means there is no limit. This value is obtained by calling the corresponding
     * get method of the {@code PreparedStatement} object that is returned by this object's {@code
     * #getPreparedStatement} method.
     * @throws SQLException if a database access error occurs or this method is called on a closed
     *                      <code>Statement</code>.
     * @see #setMaxRows
     */
    public int getMaxRows() throws SQLException {
        return getPreparedStatement().getMaxRows();
    }

    /**
     * Sets the limit for the maximum number of rows that any
     * <code>ResultSet</code> object  generated by this <code>Statement</code>
     * object can contain to the given number. If the limit is exceeded, the excess rows are silently dropped.
     *
     * @param max the new max rows limit; zero means there is no limit
     * @return this object
     * @see #getMaxRows
     */
    public SmileyVarsPreparedStatement setMaxRows(int max) {
        maxRows = Optional.of(max);
        largeMaxRows = Optional.empty();
        changeCount++;
        return this;
    }

    /**
     * Retrieves the number of seconds the driver will wait for a <code>Statement</code> object to execute. If the limit
     * is exceeded, a <code>SQLException</code> is thrown.
     *
     * @return the current query timeout limit in seconds; zero means there is no limit. This value is obtained by
     * calling the corresponding get method of the {@code PreparedStatement} object that is returned by this object's
     * {@code #getPreparedStatement} method.
     * @throws SQLException if a database access error occurs or this method is called on a closed
     *                      <code>Statement</code>
     * @see #setQueryTimeout
     */
    public int getQueryTimeout() throws SQLException {
        return getPreparedStatement().getQueryTimeout();
    }

    /**
     * Sets the number of seconds the driver will wait for a
     * <code>Statement</code> object to execute to the given number of seconds.
     * By default there is no limit on the amount of time allowed for a running statement to complete. If the limit is
     * exceeded, an
     * <code>SQLTimeoutException</code> is thrown.
     * A JDBC driver must apply this limit to the <code>execute</code>,
     * <code>executeQuery</code> and <code>executeUpdate</code> methods.
     * <p>
     * <strong>Note:</strong> JDBC driver implementations may also apply this
     * limit to {@code ResultSet} methods (consult your driver vendor documentation for details).
     *
     * @param seconds the new query timeout limit in seconds; zero means there is no limit
     * @return this object
     * @see #getQueryTimeout
     */
    public SmileyVarsPreparedStatement setQueryTimeout(int seconds) {
        queryTimeout = Optional.of(seconds);
        changeCount++;
        return this;
    }

    /**
     * Retrieves the first warning reported by calls on this <code>Statement</code> object. Subsequent
     * <code>Statement</code> object warnings will be chained to this
     * <code>SQLWarning</code> object.
     *
     * <p>The warning chain is automatically cleared each time
     * a statement is (re)executed. This method may not be called on a closed
     * <code>Statement</code> object; doing so will cause an <code>SQLException</code>
     * to be thrown.
     *
     * <P><B>Note:</B> If you are processing a <code>ResultSet</code> object, any
     * warnings associated with reads on that <code>ResultSet</code> object will be chained on it rather than on the
     * <code>Statement</code> object that produced it.
     *
     * @return the first <code>SQLWarning</code> object or <code>null</code> if there are no warnings
     * @throws SQLException if a database access error occurs or this method is called on a closed {@code Statement}
     */

    public SQLWarning getWarnings() throws SQLException {
        return getPreparedStatement().getWarnings();
    }

    /**
     * Clears all the warnings reported on this <code>Statement</code> object. After a call to this method, the method
     * <code>getWarnings</code> will return <code>null</code> until a new warning is reported for this
     * <code>Statement</code> object.
     *
     * @return this object
     * @throws SQLException if a database access error occurs or this method is called on a closed
     *                      <code>Statement</code>
     */
    public SmileyVarsPreparedStatement clearWarnings() throws SQLException {
        getPreparedStatement().clearWarnings();
        return this;
    }

    /**
     * Sets the SQL cursor name to the given <code>String</code>, which will be used by subsequent
     * <code>Statement</code> object <code>execute</code> methods. This name can then be used in SQL positioned update
     * or delete statements to identify the current row in the <code>ResultSet</code> object generated by this
     * statement.  If the database does not support positioned update/delete, this method is a noop.  To insure that a
     * cursor has the proper isolation level to support updates, the cursor's <code>SELECT</code> statement should have
     * the form <code>SELECT FOR UPDATE</code>.  If  <code>FOR UPDATE</code> is not present, positioned updates may
     * fail.
     *
     * <P><B>Note:</B> By definition, the execution of positioned updates and deletes must be done by a different
     * <code>Statement</code> object than the one that generated the <code>ResultSet</code> object being used for
     * positioning. Also, cursor names must be unique within a connection.
     *
     * @param name the new cursor name, which must be unique within a connection
     * @return this object
     */
    public SmileyVarsPreparedStatement setCursorName(String name) {
        cursorName = Optional.of(name);
        changeCount++;
        return this;
    }

    /**
     * Retrieves the current result as a <code>ResultSet</code> object. This method should be called only once per
     * result.
     *
     * @return the current result as a <code>ResultSet</code> object or
     * <code>null</code> if the result is an update count or there are no more results
     * @throws SQLException if a database access error occurs or this method is called on a closed
     *                      <code>Statement</code>
     * @see #execute
     */
    public ResultSet getResultSet() throws SQLException {
        return getPreparedStatement().getResultSet();
    }

    /**
     * Retrieves the current result as an update count; if the result is a <code>ResultSet</code> object or there are no
     * more results, -1 is returned. This method should be called only once per result.
     *
     * @return the current result as an update count; -1 if the current result is a
     * <code>ResultSet</code> object or there are no more results
     * @throws SQLException if a database access error occurs or this method is called on a closed object.
     * @see #execute
     */
    public int getUpdateCount() throws SQLException {
        ensureNotClosed();
        return getPreparedStatement().getUpdateCount();
    }

    /**
     * Moves to this <code>Statement</code> object's next result, returns
     * <code>true</code> if it is a <code>ResultSet</code> object, and
     * implicitly closes any current <code>ResultSet</code> object(s) obtained with the method
     * <code>getResultSet</code>.
     *
     * <P>There are no more results when the following is true:
     * <PRE>{@code
     * // stmt is a Statement object ((stmt.getMoreResults() == false) && (stmt.getUpdateCount() == -1)) }</PRE>
     *
     * @return <code>true</code> if the next result is a <code>ResultSet</code>
     * object; <code>false</code> if it is an update count or there are no more results
     * @throws SQLException if a database access error occurs or this method is called on a closed object.
     * @see #execute
     */
    public boolean getMoreResults() throws SQLException {
        ensureNotClosed();
        return getPreparedStatement().getMoreResults();
    }

    /**
     * Adds a set of parameters to this object's batch of commands.
     *
     * @return this object;
     * @throws SQLException if a database access error occurs, the JDBC driver does not support batches or this method
     *                      is called on a closed PreparedStatement.
     * @see #clearBatch()
     */
    public SmileyVarsPreparedStatement addBatch() throws SQLException {
        ensureNotClosed();
        PreparedStatementTag ptag = getPreparedStatementTag();
        ptag.getPreparedStatement().addBatch();
        ptag.setPendingBatch(true);
        return this;
    }

    /**
     * Empties this object's batch of commands.
     *
     * @return this object;
     * @throws SQLException if a database access error occurs, the JDBC driver does not support batches or this method
     *                      is called on a closed PreparedStatement.
     * @see #addBatch()
     */
    public SmileyVarsPreparedStatement clearBatch() throws SQLException {
        ensureNotClosed();
        for (PreparedStatementTag ptag : taggedPstmtMap.values()) {
            if (ptag.isPendingBatch()) {
                ptag.getPreparedStatement().clearBatch();
                ptag.setPendingBatch(false);
            }
        }
        return this;
    }

    /**
     * Call this method after previous calls to {@link #addBatch()} to execute the values collected by those calls as a
     * batch of updates.
     *
     * @return an array of the number of rows affected by each update. The order of the elements in the array will be
     * arbitrary and generally not match the order in which sets of updates parameters were added.
     * @throws SQLException If there is a problem.
     * @see #addBatch()
     * @see #clearBatch()
     */
    public int[] executeBatch() throws SQLException {
        ensureNotClosed();
        List<int[]> resultList = new ArrayList<>();
        int resultCount = 0;
        for (PreparedStatementTag ptag : taggedPstmtMap.values()) {
            if (ptag.isPendingBatch()) {
                int[] result = ptag.getPreparedStatement().executeBatch();
                resultList.add(result);
                resultCount += result.length;
                ptag.setPendingBatch(false);
            }
        }
        return combineBatchResults(resultList, resultCount);
    }

    private int[] combineBatchResults(List<int[]> resultList, int resultCount) {
        int[] resultArray = new int[resultCount];
        int count = 0;
        for (int[] batchResult : resultList) {
            System.arraycopy(batchResult, 0, resultArray, count, batchResult.length);
            count += batchResult.length;
        }
        return resultArray;
    }

    /**
     * Retrieves the direction for fetching rows from database tables that is the default for result sets generated from
     * this <code>Statement</code> object. If this <code>Statement</code> object has not set a fetch direction by
     * calling the method <code>setFetchDirection</code>, the return value is implementation-specific.
     *
     * @return the default fetch direction for result sets generated from this <code>Statement</code> object. This value
     * is obtained by calling the corresponding get method of the {@code PreparedStatement} object * that is returned by
     * this object's {@code #getPreparedStatement} method.
     * @throws SQLException if a database access error occurs or this method is called on a closed
     *                      <code>Statement</code>
     * @see #setFetchDirection
     */
    public int getFetchDirection() throws SQLException {
        ensureNotClosed();
        return getPreparedStatement().getFetchDirection();
    }

    /**
     * Gives the driver a hint as to the direction in which rows will be processed in <code>ResultSet</code> objects
     * created using this <code>Statement</code> object.  The default value is <code>ResultSet.FETCH_FORWARD</code>.
     * <p>
     * Note that this method sets the default fetch direction for result sets generated by this <code>Statement</code>
     * object. Each result set has its own methods for getting and setting its own fetch direction.
     *
     * @param direction the initial direction for processing rows
     * @return this object
     * @see #getFetchDirection
     */
    public SmileyVarsPreparedStatement setFetchDirection(int direction) {
        fetchDirection = Optional.of(direction);
        changeCount++;
        return this;
    }

    /**
     * Retrieves the number of result set rows that is the default fetch size for <code>ResultSet</code> objects
     * generated from this <code>Statement</code> object. If this <code>Statement</code> object has not set a fetch size
     * by calling the method <code>setFetchSize</code>, the return value is implementation-specific.
     *
     * @return the default fetch size for result sets generated from this <code>Statement</code> object. This value is
     * obtained by calling the corresponding get method of the {@code PreparedStatement} object that is returned by this
     * object's {@code #getPreparedStatement} method.
     * @throws SQLException if a database access error occurs or this method is called on a closed
     *                      <code>Statement</code>
     * @see #setFetchSize
     */
    public int getFetchSize() throws SQLException {
        return getPreparedStatement().getFetchSize();
    }

    /**
     * Gives the JDBC driver a hint as to the number of rows that should be fetched from the database when more rows are
     * needed for
     * <code>ResultSet</code> objects generated by this <code>Statement</code>.
     * If the value specified is zero, then the hint is ignored. The default value is zero.
     *
     * @param rows the number of rows to fetch
     * @return this prepared statement
     * @throws SQLException if this method is called on a closed <code>Statement</code> or the condition {@code rows >=
     *                      0} is not satisfied.
     * @see #getFetchSize
     */
    public SmileyVarsPreparedStatement setFetchSize(int rows) throws SQLException {
        if (rows < 0) {
            throw new SQLException("fetchSize as specified as " + rows + ". It may not be negative");
        }
        fetchSize = Optional.of(rows);
        changeCount++;
        return this;
    }

    /**
     * Retrieves the result set concurrency for <code>ResultSet</code> objects generated by this <code>Statement</code>
     * object.
     *
     * @return either <code>ResultSet.CONCUR_READ_ONLY</code> or {@code ResultSet.CONCUR_UPDATABLE}. This value is
     * obtained by calling the corresponding get method of the {@code PreparedStatement} object that is returned by this
     * object's {@code #getPreparedStatement} method.
     * @throws SQLException if a database access error occurs or this method is called on a closed
     *                      <code>Statement</code>
     */
    public int getResultSetConcurrency() throws SQLException {
        return getPreparedStatement().getResultSetConcurrency();
    }

    /**
     * Retrieves the result set type for <code>ResultSet</code> objects generated by this <code>Statement</code>
     * object.
     *
     * @return one of {@code ResultSet.TYPE_FORWARD_ONLY}, {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     * <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>. This value is obtained by calling the corresponding get method of
     * the {@code PreparedStatement} object that is returned by this object's {@code #getPreparedStatement} method.
     * @throws SQLException if a database access error occurs or this method is called on a closed
     *                      <code>Statement</code>
     */
    public int getResultSetType() throws SQLException {
        return getPreparedStatement().getResultSetType();
    }

    /**
     * Retrieves the <code>Connection</code> object that produced this <code>Statement</code> object.
     *
     * @return the connection that produced this statement
     */
    @NotNull
    public Connection getConnection() {
        return connection;
    }

    /**
     * Moves to this <code>Statement</code> object's next result, deals with any current <code>ResultSet</code>
     * object(s) according  to the instructions specified by the given flag, and returns
     * <code>true</code> if the next result is a <code>ResultSet</code> object.
     *
     * <P>There are no more results when the following is true:
     * <PRE>{@code
     * // stmt is a Statement object ((stmt.getMoreResults(current) == false) && (stmt.getUpdateCount() == -1)) }</PRE>
     *
     * @param current one of the following <code>Statement</code> constants indicating what should happen to current
     *                <code>ResultSet</code> objects obtained using the method
     *                <code>getResultSet</code>:
     *                <code>Statement.CLOSE_CURRENT_RESULT</code>,
     *                <code>Statement.KEEP_CURRENT_RESULT</code>, or
     *                <code>Statement.CLOSE_ALL_RESULTS</code>
     * @return <code>true</code> if the next result is a <code>ResultSet</code>
     * object; <code>false</code> if it is an update count or there are no more results
     * @throws SQLException                    if a database access error occurs, this method is called on a closed
     *                                         <code>Statement</code> or the argument supplied is not one of the
     *                                         following:
     *                                         <code>Statement.CLOSE_CURRENT_RESULT</code>,
     *                                         <code>Statement.KEEP_CURRENT_RESULT</code> or
     *                                         <code>Statement.CLOSE_ALL_RESULTS</code>
     * @throws SQLFeatureNotSupportedException if
     *                                         <code>DatabaseMetaData.supportsMultipleOpenResults</code> returns
     *                                         <code>false</code> and either
     *                                         <code>Statement.KEEP_CURRENT_RESULT</code> or
     *                                         <code>Statement.CLOSE_ALL_RESULTS</code> are supplied as
     *                                         the argument.
     * @see #execute
     */
    public boolean getMoreResults(int current) throws SQLException {
        return getPreparedStatement().getMoreResults(current);
    }

    /**
     * Retrieves any auto-generated keys created as a result of executing this
     * <code>Statement</code> object. If this <code>Statement</code> object did
     * not generate any keys, an empty <code>ResultSet</code> object is returned.
     *
     * <p><B>Note:</B>If the columns which represent the auto-generated keys were not specified,
     * the JDBC driver implementation will determine the columns which best represent the auto-generated keys.
     *
     * @return a <code>ResultSet</code> object containing the auto-generated key(s) generated by the execution of this
     * <code>Statement</code> object
     * @throws SQLException                    if a database access error occurs or this method is called on a closed
     *                                         <code>Statement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.4
     */
    public ResultSet getGeneratedKeys() throws SQLException {
        return getPreparedStatement().getGeneratedKeys();
    }

    /**
     * Retrieves the result set holdability for <code>ResultSet</code> objects generated by this <code>Statement</code>
     * object.
     *
     * @return either {@code ResultSet.HOLD_CURSORS_OVER_COMMIT} or {@code ResultSet.CLOSE_CURSORS_AT_COMMIT}. This
     * value is obtained by calling the corresponding get method of the {@code PreparedStatement} object that is
     * returned by this object's {@code #getPreparedStatement} method.
     * @throws SQLException if a database access error occurs or this method is called on a closed
     *                      <code>Statement</code>
     */
    public int getResultSetHoldability() throws SQLException {
        return getPreparedStatement().getResultSetHoldability();
    }

    /**
     * Retrieves whether this object has been closed.
     *
     * @return true if this object is closed; false if it is still open
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Returns a  value indicating whether the {@code Statement} is poolable or not.
     *
     * @return {@code true} if the {@code Statement} is poolable; {@code false} otherwise.
     * @throws SQLException if this method is called on a closed <code>Statement</code>.
     * @see Statement#setPoolable(boolean) setPoolable(boolean)
     */
    public boolean isPoolable() throws SQLException {
        return getPreparedStatement().isPoolable();
    }

    /**
     * Requests that a {@code Statement} be pooled or not pooled.  The value specified is a hint to the statement pool
     * implementation indicating whether the application wants the statement to be pooled.  It is up to the statement
     * pool manager as to whether the hint is used.
     * <p>
     * The poolable value of a statement is applicable to both internal statement caches implemented by the driver and
     * external statement caches implemented by application servers and other applications.
     * <p>
     * By default, a {@code Statement} is not poolable when created, and a {@code PreparedStatement} and {@code
     * CallableStatement} are poolable when created.
     *
     * @param poolable requests that the statement be pooled if true and that the statement not be pooled if false
     * @return this object
     */
    public SmileyVarsPreparedStatement setPoolable(boolean poolable) {
        this.poolable = Optional.of(poolable);
        changeCount++;
        return this;
    }

    /**
     * Retrieves the current result as an update count; if the result is a {@code ResultSet} object or there are no more
     * results, -1 is returned. This method should be called only once per result.
     * <p>
     * This method should be used when the returned row count may exceed {@link Integer#MAX_VALUE}.
     * <p>
     * The default implementation will throw {@code UnsupportedOperationException}
     *
     * @return the current result as an update count; -1 if the current result is a <code>ResultSet</code> object or
     * there are no more results
     * @throws SQLException if a database access error occurs or this method is called on a closed {@code Statement}
     * @see #execute
     */
    long getLargeUpdateCount() throws SQLException {
        return getPreparedStatement().getLargeUpdateCount();
    }

    /**
     * Retrieves the maximum number of rows that a {@code ResultSet} object produced by this {@code Statement} object
     * can contain.  If this limit is exceeded, the excess rows are silently dropped.
     * <p>
     * This method should be used when the returned row limit may exceed {@link Integer#MAX_VALUE}.
     * <p>
     * The default implementation will return {@code 0}
     *
     * @return the current maximum number of rows for a {@code ResultSet} object produced by this {@code Statement}
     * object; zero means there is no limit. This value is obtained by calling the corresponding get method of the
     * {@code PreparedStatement} object that is returned by this object's {@code #getPreparedStatement} method.
     * @throws SQLException if a database access error occurs or this method is called on a closed {@code Statement}
     * @see #setMaxRows
     */
    long getLargeMaxRows() throws SQLException {
        return getPreparedStatement().getLargeMaxRows();
    }

    /**
     * Sets the limit for the maximum number of rows that any {@code ResultSet} object  generated by this {@code
     * Statement} object can contain to the given number. If the limit is exceeded, the excess rows are silently
     * dropped.
     * <p>
     * This method should be used when the row limit may exceed {@link Integer#MAX_VALUE}.
     * <p>
     * The default implementation will throw {@code UnsupportedOperationException}
     *
     * @param max the new max rows limit; zero means there is no limit
     * @return this object
     * @see #getMaxRows
     */
    public SmileyVarsPreparedStatement setLargeMaxRows(long max) {
        largeMaxRows = Optional.of(max);
        maxRows = Optional.empty();
        changeCount++;
        return this;
    }

    private BitSet computeParametersSignature() {
        BitSet bitSet = new BitSet(valueMap.size());
        int i = 0;
        for (Map.Entry<String, BiSqlConsumer<PreparedStatement, Integer>> entry : valueMap.entrySet()) {
            if (!entry.getValue().isVacuous()) {
                bitSet.set(i);
            }
            i += 1;
        }
        return bitSet;
    }

    private void changeWithCheckedName(String parameterName, BiSqlConsumer<PreparedStatement, Integer> setter) {
        ensureNotClosed();
        if (valueMap.containsKey(parameterName)) {
            valueMap.put(parameterName, setter);
            changeCount++;
        } else {
            throwForUnknownParameter(parameterName);
        }
    }

    private void ensureNotClosed() {
        if (closed) {
            throw new SmileyVarsException("Unable to modify a " + this.getClass().getSimpleName() + " after it is closed.");
        }
    }

    private void throwForUnknownParameter(String parameterName) {
        throw new SmileyVarsException("\"" + parameterName + "\" is not the name of a variable in " + template.getTemplateString());
    }

    /**
     * <p>Get a {@link PreparedStatement} that is configured with the SQL that is an expansion of this object's
     * SmileyVars template based on the parameters that are or are not set.  If this is the first time this {@link
     * PreparedStatement} is being used, all the of previously provided parameter values and configuration values
     * (concurrency, holdability, maxRows, ...) will be set during this method call.</p>
     * <p>{@link PreparedStatement} objects are reused if the set of parameter values that have and do not have values
     * is the same as the last time the prepared statement was used. If any parameter or configuration values have
     * changed since the last time that the prepared statement was used, this method call will update the prepared
     * statement with those changes.</p>
     * <p>The reuse of a {@link PreparedStatement} continues until a call to {@link #deepClearParameters()} or {@link
     * #close()}, which calls {@link #deepClearParameters()}.</p>
     *
     * @return a prepared statement that will be configured based the the SmileyVars template that this object was
     * created with and any parameter or configuration values that have been specified since this object's creation.
     * @throws SQLException if there is a problem creating a {@link PreparedStatement} objector this object is closed.
     */
    public PreparedStatement getPreparedStatement() throws SQLException {
        ensureNotClosed();
        return getPreparedStatementTag().getPreparedStatement();
    }

    private PreparedStatementTag getPreparedStatementTag() throws SQLException {
        BitSet signature = computeParametersSignature();
        PreparedStatementTag ptag = taggedPstmtMap.get(signature);
        if (ptag == null) {
            ptag = new PreparedStatementTag(connection.prepareStatement(template.apply(filterVacuousEntries(valueMap))));
            taggedPstmtMap.put(signature, ptag);
        }
        if (ptag.getChangeCount() != changeCount) {
            ptag.setChangeCount(changeCount);
            updatePreparedStatement(ptag);
        }
        return ptag;
    }

    private Map<String, BiSqlConsumer<PreparedStatement, Integer>> filterVacuousEntries(Map<String, BiSqlConsumer<PreparedStatement, Integer>> map) {
        Map<String, BiSqlConsumer<PreparedStatement, Integer>> filteredMap = new HashMap<>();
        map.forEach((key, value) -> {
            if (!value.isVacuous()) filteredMap.put(key, value);
        });
        return filteredMap;
    }

    private void updatePreparedStatement(PreparedStatementTag ptag) throws SQLException {
        PreparedStatement preparedStatement = ptag.getPreparedStatement();
        updatePreparedStatementConfig(preparedStatement);
        updatePreparedStatementParams(preparedStatement);
    }

    private void updatePreparedStatementParams(PreparedStatement preparedStatement) throws SQLException {
        int[] paramIndex = {1};
        template.forEachVariableInstance(name -> {
            BiSqlConsumer<PreparedStatement, Integer> setter = valueMap.get(name);
            if (!setter.isVacuous()) {
                try {
                    setter.accept(preparedStatement, paramIndex[0]);
                } catch (SQLException e) {
                    throw new SQLException("Error getting value for smileyVar named " + name, e);
                }
                paramIndex[0] += 1;
            }
        });
    }

    /**
     * Return the names of the parameters that can be specified for this object.
     *
     * @return A set containing the unique variable names in this object's template in no particular order.
     */
    public Set<String> getVarNames() {
        return template.getVarNames();
    }

    /**
     * Return the names of the parameters that have been specified for this object.
     *
     * @return A set containing the variable names in this object's template in no particular order that have been bound
     * to a parameter value.
     */
    public Set<String> getBoundVarNames() {
        return template.getVarNames().stream().filter(name -> !valueMap.get(name).isVacuous()).collect(Collectors.toSet());
    }

    private void updatePreparedStatementConfig(PreparedStatement preparedStatement) throws SQLException {
        if (maxFieldSize.isPresent()) {
            preparedStatement.setMaxFieldSize(maxFieldSize.get());
        }
        if (maxRows.isPresent()) {
            preparedStatement.setMaxRows(maxRows.get());
        } else if (largeMaxRows.isPresent()) {
            preparedStatement.setLargeMaxRows(largeMaxRows.get());
        }
        if (queryTimeout.isPresent()) {
            preparedStatement.setQueryTimeout(queryTimeout.get());
        }
        if (cursorName.isPresent()) {
            preparedStatement.setCursorName(cursorName.get());
        }
        if (fetchDirection.isPresent()) {
            //noinspection MagicConstant
            preparedStatement.setFetchDirection(fetchDirection.get());
        }
        if (fetchSize.isPresent()) {
            preparedStatement.setFetchSize(fetchSize.get());
        }
        if (poolable.isPresent()) {
            preparedStatement.setPoolable(poolable.get());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SmileyVarsPreparedStatement)) return false;
        SmileyVarsPreparedStatement that = (SmileyVarsPreparedStatement) o;
        return closed == that.closed &&
                       changeCount == that.changeCount &&
                       connection.equals(that.connection) &&
                       template.equals(that.template) &&
                       valueMap.equals(that.valueMap) &&
                       maxFieldSize.equals(that.maxFieldSize) &&
                       maxRows.equals(that.maxRows) &&
                       largeMaxRows.equals(that.largeMaxRows) &&
                       queryTimeout.equals(that.queryTimeout) &&
                       cursorName.equals(that.cursorName) &&
                       fetchDirection.equals(that.fetchDirection) &&
                       fetchSize.equals(that.fetchSize) &&
                       poolable.equals(that.poolable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connection, template, valueMap, closed, changeCount, maxFieldSize, maxRows, largeMaxRows, queryTimeout, cursorName, fetchDirection, fetchSize, poolable);
    }

    @Override
    public String toString() {
        return "SmileyVarsPreparedStatement{" +
                       "connection=" + connection +
                       ", template=" + template +
                       ", valueMap=" + valueMap +
                       ", closed=" + closed +
                       ", changeCount=" + changeCount +
                       ", maxFieldSize=" + maxFieldSize +
                       ", maxRows=" + maxRows +
                       ", largeMaxRows=" + largeMaxRows +
                       ", queryTimeout=" + queryTimeout +
                       ", cursorName=" + cursorName +
                       ", fetchDirection=" + fetchDirection +
                       ", fetchSize=" + fetchSize +
                       ", poolable=" + poolable +
                       '}';
    }

    /**
     * Tag PreparedStatement objects with a signature so that we can reuse prepared statement objects with different
     * parameter settings.
     */
    private static class PreparedStatementTag {
        private final PreparedStatement preparedStatement;
        private long changeCount;
        private boolean pendingBatch;

        PreparedStatementTag(PreparedStatement preparedStatement) {
            this.preparedStatement = preparedStatement;
            this.changeCount = -1;
        }

        PreparedStatement getPreparedStatement() {
            return preparedStatement;
        }

        long getChangeCount() {
            return changeCount;
        }

        void setChangeCount(long changeCount) {
            this.changeCount = changeCount;
        }

        public boolean isPendingBatch() {
            return pendingBatch;
        }

        public void setPendingBatch(boolean pendingBatch) {
            this.pendingBatch = pendingBatch;
        }
    }
}
