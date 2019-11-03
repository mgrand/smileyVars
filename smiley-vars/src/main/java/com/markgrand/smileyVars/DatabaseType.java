package com.markgrand.smileyVars;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Enumeration of database types that a SmileyVars can be specialized for.
 */
public enum DatabaseType {
    /**
     * Generic type of template that is not specialized for any particular type of database.
     */
    ANSI(Tokenizer.builder().configureForAnsi(), ValueFormatterRegistry.ansiInstance()),
    /**
     * Template specialized for PostgreSQL.
     */
    POSTGRESQL(Tokenizer.builder().configureForPostgresql(), ValueFormatterRegistry.postgresqlInstance()),
    /**
     * Template specialized for Oracle.
     */
    ORACLE(Tokenizer.builder().configureForOracle(), ValueFormatterRegistry.ansiInstance()),
    /**
     * Template specialized for SQL Server.
     */
    SQL_SERVER(Tokenizer.builder().configureForSqlServer(), ValueFormatterRegistry.ansiInstance());

    private static final Logger logger = LoggerFactory.getLogger(DatabaseType.class);

    private final Tokenizer.TokenizerBuilder tokenizerBuilder;
    private final ValueFormatterRegistry valueFormatterRegistry;

    DatabaseType(Tokenizer.TokenizerBuilder tokenizerBuilder, ValueFormatterRegistry valueFormatterRegistry) {
        this.tokenizerBuilder = tokenizerBuilder;
        this.valueFormatterRegistry = valueFormatterRegistry;
    }

    /**
     * Infer the type of database that this is for based on a connection's metadata.
     *
     * @param databaseMetaData the metadata
     * @return The {@code DatabaseType} value that corresponds to the inferred type of database. If there is a problem
     * inferring the type of database, the problem is logged and {@link DatabaseType#ANSI} is returned.
     */
    @org.jetbrains.annotations.NotNull
    @NotNull
    public static DatabaseType inferDatabaseType(@org.jetbrains.annotations.NotNull @NotNull  DatabaseMetaData databaseMetaData) {
        try {
            String productName = databaseMetaData.getDatabaseProductName();
            if (productName == null) {
                return ANSI;
            }
            if ("CUBRID".equalsIgnoreCase(productName)) {
                return ANSI;
            }
            if (productName.startsWith("DB2")) {
                return ANSI;
            }
            if ("Apache Derby".equals(productName)) {
                return ANSI;
            }
            if ("EnterpriseDB".equals(productName)) {
                return POSTGRESQL;
            }
            if (productName.startsWith("Firebird")) {
                return ANSI;
            }
            if ("H2".equals(productName)) {
                return ANSI;
            }
            if ("HDB".equals(productName)) {
                return ANSI;
            }
            if ("HSQL Database Engine".equals(productName)) {
                return ANSI;
            }
            if ("Informix Dynamic Server".equals(productName)) {
                return ANSI;
            }
            if ("ingres".equalsIgnoreCase(productName)) {
                return ANSI;
            }
            if (databaseMetaData.getDriverName() != null && databaseMetaData.getDriverName().startsWith("MariaDB")) {
                return ANSI;
            }
            if ("MySQL".equals(productName)) {
                return ANSI;
            }
            if ("Oracle".equals(productName)) {
                return ORACLE;
            }
            if ("PostgreSQL".equals(productName)) {
                return POSTGRESQL;
            }
            if (productName.startsWith("Microsoft SQL Server")) {
                return SQL_SERVER;
            }
            if ("Sybase SQL Server".equals(productName) || "Adaptive Server Enterprise".equals(productName)) {
                return ANSI;
            }
            if (productName.startsWith("Adaptive Server Anywhere") || "SQL Anywhere".equals(productName)) {
                return ANSI;
            }
            logger.warn("Defaulting unknown database product " + productName + " to use ANSI templates.");
            return ANSI;
        } catch (SQLException e) {
            logger.warn("Attempt to get type of database failed", e);
            return ANSI;
        }

    }

    Tokenizer.TokenizerBuilder getTokenizerBuilder() {
        return tokenizerBuilder;
    }

    ValueFormatterRegistry getValueFormatterRegistry() {
        return valueFormatterRegistry;
    }
}
