package com.markgrand.smileyVars;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Enumeration of database types that a SmileyVars can be specialized for.
 */
public enum DatabaseType {
    /**
     * Generic type of template that is not specialized for any particular type of database.
     */
    ANSI,
    /**
     * Template specialized for PostgreSQL.
     */
    POSTGRESQL,
    /**
     * Template specialized for Oracle.
     */
    ORACLE,
    /**
     * Template specialized for SQL Server.
     */
    SQL_SERVER;

    private static final Logger logger = LoggerFactory.getLogger(DatabaseType.class);

    static DatabaseType inferDatabaseType(DatabaseMetaData databaseMetaData) {
        try {
            String productName = databaseMetaData.getDatabaseProductName();
            if (productName == null) {
                return ANSI;
            }
            if ("CUBRID".equalsIgnoreCase(productName)) {
                return ANSI;
            }
            if ("DB2 UDB for AS/400".equals(productName)) {
                return ANSI;
            }
            if (productName.startsWith("DB2/")) {
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
            if ( productName.startsWith( "Microsoft SQL Server" ) ) {
                return SQL_SERVER;
            }
            if ( "Sybase SQL Server".equals( productName ) || "Adaptive Server Enterprise".equals( productName ) ) {
                return ANSI;
            }
            if ( productName.startsWith( "Adaptive Server Anywhere" ) || "SQL Anywhere".equals( productName ) ) {
                return ANSI;
            }
            logger.warn("Defaulting unknown database product " + productName + " to use ANSI templates.");
            return ANSI;
        } catch (SQLException e) {
            logger.warn("Attempt to get type of database failed", e);
            return ANSI;
        }
    }
}
