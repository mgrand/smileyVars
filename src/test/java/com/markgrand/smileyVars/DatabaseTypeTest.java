package com.markgrand.smileyVars;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.jupiter.api.Test;

import java.sql.DatabaseMetaData;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTypeTest {
    @Test
    void inferDatabaseTypeNull(@Mocked DatabaseMetaData metaData) {
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeCubrid(@Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "CUBRID";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeDb2400(@Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "DB2 UDB for AS/400";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeDb2390(@Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "DB2/390";
        }};
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeDerby(@Mocked DatabaseMetaData metaData) throws Exception {
        new Expectations() {{
            metaData.getDatabaseProductName(); result = "Apache Derby";
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
