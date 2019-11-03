package com.markgrand.smileyVars;

import mockit.Capturing;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTypeTest {
    @Test
    void inferDatabaseTypeNull(@NotNull @Mocked DatabaseMetaData metaData) {
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeCubrid(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "CUBRID";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeDb2400(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "DB2 UDB for AS/400";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeDb2390(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "DB2/390";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeDerby(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "Apache Derby";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeEnterprise(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "EnterpriseDB";
        }};
        assertEquals(DatabaseType.POSTGRESQL, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeFirebird(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "Firebird";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeH2(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "H2";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeHdb(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "HDB";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeHsql(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "HSQL Database Engine";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeInformix(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "Informix Dynamic Server";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeIngres(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "ingres";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeMaria(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "MariaDB";
            metaData.getDriverName(); result = "MariaDB";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeMySql(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "MySQL";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeOracle(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "Oracle";
        }};
        assertEquals(DatabaseType.ORACLE, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypePostgreSQL(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "PostgreSQL";
        }};
        assertEquals(DatabaseType.POSTGRESQL, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeSqlServer(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "Microsoft SQL Server";
        }};
        assertEquals(DatabaseType.SQL_SERVER, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeSybase(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "Sybase SQL Server";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeAdaptive(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "Adaptive Server Enterprise";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeAAnywhere(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "Adaptive Server Anywhere";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeSAnywhere(@NotNull @Mocked DatabaseMetaData metaData, @NotNull @Capturing Logger logger) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "SQL Anywhere";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
        new Verifications() {{
            logger.warn(anyString); maxTimes = 0;
        }};
    }

    @Test
    void inferDatabaseTypeUnknown(@NotNull @Mocked DatabaseMetaData metaData, @NotNull @Capturing Logger logger) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "xxxxxxxx";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
        new Verifications() {{
            logger.warn(anyString); times = 1;
        }};
    }

    @Test
    void exception(@NotNull @Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = new SQLException();
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void getTokenizerBuilderAnsi() {
        Tokenizer.TokenizerBuilder builder = DatabaseType.ANSI.getTokenizerBuilder();
        assertFalse(builder.getConfig().isPostgresqlEscapeStringEnabled());
        assertFalse(builder.getConfig().isPostgresqlDollarStringEnabled());
        assertFalse(builder.getConfig().isOracleDelimitedStringEnabled());
        assertTrue(builder.getConfig().isNestedBlockCommentEnabled());
        assertFalse(builder.getConfig().isSquareBracketIdentifierQuotingEnabled());
    }

    @Test
    void getTokenizerBuilderPostgresql() {
        Tokenizer.TokenizerBuilder builder = DatabaseType.POSTGRESQL.getTokenizerBuilder();
        assertTrue(builder.getConfig().isPostgresqlEscapeStringEnabled());
        assertTrue(builder.getConfig().isPostgresqlDollarStringEnabled());
        assertFalse(builder.getConfig().isOracleDelimitedStringEnabled());
        assertTrue(builder.getConfig().isNestedBlockCommentEnabled());
        assertFalse(builder.getConfig().isSquareBracketIdentifierQuotingEnabled());
    }

    @Test
    void getTokenizerBuilderSqlServer() {
        Tokenizer.TokenizerBuilder builder = DatabaseType.SQL_SERVER.getTokenizerBuilder();
        assertFalse(builder.getConfig().isPostgresqlEscapeStringEnabled());
        assertFalse(builder.getConfig().isPostgresqlDollarStringEnabled());
        assertFalse(builder.getConfig().isOracleDelimitedStringEnabled());
        assertTrue(builder.getConfig().isNestedBlockCommentEnabled());
        assertTrue(builder.getConfig().isSquareBracketIdentifierQuotingEnabled());
    }

    @Test
    void getTokenizerBuilderOracle() {
        Tokenizer.TokenizerBuilder builder = DatabaseType.ORACLE.getTokenizerBuilder();
        assertFalse(builder.getConfig().isPostgresqlEscapeStringEnabled());
        assertFalse(builder.getConfig().isPostgresqlDollarStringEnabled());
        assertTrue(builder.getConfig().isOracleDelimitedStringEnabled());
        assertFalse(builder.getConfig().isNestedBlockCommentEnabled());
        assertFalse(builder.getConfig().isSquareBracketIdentifierQuotingEnabled());
    }

    @Test
    void getValueFormatterRegistryAnsi() {
        ValueFormatterRegistry registry = DatabaseType.ANSI.getValueFormatterRegistry();
        assertEquals("ANSI", registry.getName());
    }

    @Test
    void getValueFormatterRegistryPostgresql() {
        ValueFormatterRegistry registry = DatabaseType.POSTGRESQL.getValueFormatterRegistry();
        assertEquals("PostgreSQL", registry.getName());
    }

    @Test
    void getValueFormatterRegistryOracle() {
        ValueFormatterRegistry registry = DatabaseType.ANSI.getValueFormatterRegistry();
        assertEquals("ANSI", registry.getName());
    }

    @Test
    void getValueFormatterRegistrySqlServer() {
        ValueFormatterRegistry registry = DatabaseType.ANSI.getValueFormatterRegistry();
        assertEquals("ANSI", registry.getName());
    }
}
