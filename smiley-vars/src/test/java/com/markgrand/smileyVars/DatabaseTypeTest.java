package com.markgrand.smileyVars;

import com.mockrunner.mock.jdbc.MockDatabaseMetaData;
import org.junit.jupiter.api.Test;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("RedundantThrows")
class DatabaseTypeTest {
    @Test
    void inferDatabaseTypeNull() {
        DatabaseMetaData metaData = new MockDatabaseMetaData();
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeCubrid() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("CUBRID");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeDb2400() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("DB2 UDB for AS/400");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeDb2390() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("DB2/390");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeDerby() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("Apache Derby");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeEnterprise() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("EnterpriseDB");
        assertEquals(DatabaseType.POSTGRESQL, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeFirebird() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("Firebird");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeH2() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("H2");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeHdb() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("HDB");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeHsql() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("HSQL Database Engine");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeInformix() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("Informix Dynamic Server");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeIngres() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("ingres");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeMaria() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("MariaDB");
        metaData.setDriverName("MariaDB");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeMySql() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("MySQL");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeOracle() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("Oracle");
        assertEquals(DatabaseType.ORACLE, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypePostgreSQL() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("PostgreSQL");
        assertEquals(DatabaseType.POSTGRESQL, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeSqlServer() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("Microsoft SQL Server");
        assertEquals(DatabaseType.SQL_SERVER, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeSybase() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("Sybase SQL Server");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeAdaptive() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("Adaptive Server Enterprise");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeAAnywhere() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("Adaptive Server Anywhere");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeSAnywhere() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("SQL Anywhere");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void inferDatabaseTypeUnknown() throws Exception {
        MockDatabaseMetaData metaData = new MockDatabaseMetaData();
        metaData.setDatabaseProductName("constable");
        assertEquals(DatabaseType.ANSI, DatabaseType.inferDatabaseType(metaData));
    }

    @Test
    void exception() throws Exception {
        DatabaseMetaData metaData = new MockDatabaseMetaData(){
            @Override
            public String getDatabaseProductName() throws SQLException {
                throw new SQLException("bogus");
            }
        };
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
