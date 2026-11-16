package com.company.txn.qa.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Test-only JDBC connection factory. This assessment service currently stores state in memory;
 * configure DB_URL only when a real DB/schema is provisioned for a DB integration environment.
 */
public final class DatabaseConfig {
    private DatabaseConfig() {}
    public static boolean isConfigured() {
        return FrameworkConfig.dbUrl() != null && !FrameworkConfig.dbUrl().isBlank();
    }
    public static Connection getConnection() throws SQLException {
        if (!isConfigured()) throw new IllegalStateException("DB_URL is not configured");
        try {
            Class.forName(FrameworkConfig.dbDriver());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("JDBC driver not found: " + FrameworkConfig.dbDriver(), e);
        }
        return DriverManager.getConnection(FrameworkConfig.dbUrl(),
                FrameworkConfig.dbUser(), FrameworkConfig.dbPassword());
    }
}
