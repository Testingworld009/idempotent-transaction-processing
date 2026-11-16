package com.company.txn.qa.db.tests;

import com.company.txn.qa.config.DatabaseConfig;
import com.company.txn.qa.utility.LogUtils;
import com.company.txn.qa.utility.TestEvidenceExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.sql.Connection;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(TestEvidenceExtension.class)
class DatabaseConnectivityTest {
    @Test void db01ConnectionCanBeEstablished() throws Exception {
        Assumptions.assumeTrue(DatabaseConfig.isConfigured(),
                "DB BLOCKED: configure DB_URL, DB_USER, DB_PASSWORD and DB_DRIVER");
        try (Connection c = DatabaseConfig.getConnection()) {
            assertTrue(c.isValid(5));
            LogUtils.log("INFO", "Database connectivity verified; database metadata URL is not logged.");
        }
    }
}
