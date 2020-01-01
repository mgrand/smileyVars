package com.markgrand.smileyvars;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of database types that a SmileyVar can be specialized for.
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

    private static Map<String, DatabaseType> nameToDatabaseTypeMap = new HashMap<>();

    static {
        nameToDatabaseTypeMap.put("CUBRID", ANSI);
        nameToDatabaseTypeMap.put("DB2", ANSI);
        nameToDatabaseTypeMap.put("APACHE DERBY", ANSI);
        nameToDatabaseTypeMap.put("ENTERPRISEDB", POSTGRESQL);
        nameToDatabaseTypeMap.put("FIREBIRD", ANSI);
        nameToDatabaseTypeMap.put("H2", ANSI);
        nameToDatabaseTypeMap.put("HDB", ANSI);
        nameToDatabaseTypeMap.put("HSQL DATABASE ENGINE", ANSI);
        nameToDatabaseTypeMap.put("INFORMIX DYNAMIC SERVER", ANSI);
        nameToDatabaseTypeMap.put("INGRES", ANSI);
        nameToDatabaseTypeMap.put("MYSQL", ANSI);
        nameToDatabaseTypeMap.put("ORACLE", ORACLE);
        nameToDatabaseTypeMap.put("POSTGRESQL", POSTGRESQL);
        nameToDatabaseTypeMap.put("SYBASE SQL SERVER", ANSI);
        nameToDatabaseTypeMap.put("ADAPTIVE SERVER ENTERPRISE", ANSI);
        nameToDatabaseTypeMap.put("ADAPTIVE SERVER ANYWHERE", ANSI);
        nameToDatabaseTypeMap.put("SQL ANYWHERE", ANSI);
    }

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
    @NotNull
    public static DatabaseType inferDatabaseType(@NotNull DatabaseMetaData databaseMetaData) {
        try {
            return inferDatabaseType(databaseMetaData.getDatabaseProductName());
        } catch (SQLException e) {
            logger.warn("Attempt to get type of database failed", e);
            return ANSI;
        }
    }

    /**
     * Infer the type of database that this is for based on a connection's metadata.
     *
     * @param productName the productName value returned by the connection's metadata
     * @return The {@code DatabaseType} value that corresponds to the inferred type of database. If there is a problem
     * inferring the type of database, the problem is logged and {@link DatabaseType#ANSI} is returned.
     */
    @NotNull
    public static DatabaseType inferDatabaseType(@NotNull String productName) {
        DatabaseType type = nameToDatabaseTypeMap.get(productName.toUpperCase());
        if (type != null) {
            return type;
        }
        if (productName.startsWith("Microsoft SQL Server")) {
            return SQL_SERVER;
        }
        logger.warn("Defaulting unknown database product {} to use ANSI templates.", productName);
        return ANSI;
    }

    Tokenizer.TokenizerBuilder getTokenizerBuilder() {
        return tokenizerBuilder;
    }

    ValueFormatterRegistry getValueFormatterRegistry() {
        return valueFormatterRegistry;
    }
}
