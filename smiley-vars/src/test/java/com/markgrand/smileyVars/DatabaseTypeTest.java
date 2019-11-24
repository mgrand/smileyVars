package com.markgrand.smileyVars;

import com.markgrand.smileyVars.testUtil.MockDatabaseMetadata;
import org.junit.jupiter.api.Test;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("RedundantThrows")
class DatabaseTypeTest {
    @Test
    void inferDatabaseTypeNull() {
        DatabaseMetaData metaData = new MockDatabaseMetadata(); 
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeCubrid() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("CUBRID");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeDb2400() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("DB2 UDB for AS/400");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeDb2390() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("DB2/390");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeDerby() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("Apache Derby");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeEnterprise() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("EnterpriseDB");
        assertEquals(DatabaseType.POSTGRESQL, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeFirebird() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("Firebird");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeH2() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("H2");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeHdb() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("HDB");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeHsql() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("HSQL Database Engine");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeInformix() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("Informix Dynamic Server");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeIngres() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("ingres");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeMaria() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("MariaDB").driverName("MariaDB");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeMySql() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("MySQL");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeOracle() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("Oracle");
        assertEquals(DatabaseType.ORACLE, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypePostgreSQL() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("PostgreSQL");
        assertEquals(DatabaseType.POSTGRESQL, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeSqlServer() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("Microsoft SQL Server");
        assertEquals(DatabaseType.SQL_SERVER, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeSybase() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("Sybase SQL Server");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeAdaptive() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("Adaptive Server Enterprise");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeAAnywhere() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("Adaptive Server Anywhere");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeSAnywhere() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("SQL Anywhere");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
//        new Verifications() {{
//            logger.warn(anyString); maxTimes = 0;
//        }};
    }

    @Test
    void inferDatabaseTypeUnknown() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().databaseProductName("constable");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
//        new Verifications() {{
//            logger.warn(anyString); times = 1;
//        }};
    }

    @Test
    void exception() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetadata().getDatabaseProductNameThrows(new SQLException("bogus"));
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
