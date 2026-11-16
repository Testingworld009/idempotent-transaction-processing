package com.company.txn.qa.ui.tests;

import com.company.txn.qa.config.*;
import com.company.txn.qa.ui.pages.TransactionDashboardPage;
import com.company.txn.qa.utility.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(TestEvidenceExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class TransactionDashboardTest {
    private TransactionDashboardPage page;
    @BeforeEach void start() {
        Assumptions.assumeTrue(!FrameworkConfig.dashboardUrl().isBlank(),
                "UI BLOCKED: current repository has no dashboard; set DASHBOARD_URL after UI is deployed");
        WebDriver driver = DriverFactory.create();
        DriverManager.setDriver(driver);
        page = new TransactionDashboardPage(driver);
        page.open(FrameworkConfig.dashboardUrl());
    }
    @Test @DisplayName("UI-01 dashboard opens and heading is visible")
    void dashboardLoads() { assertTrue(page.isLoaded()); }
    @Test @DisplayName("UI-02 dashboard page title is not blank")
    void titleIsPresent() { assertFalse(page.title().isBlank()); }
    @Test @DisplayName("UI-03 dashboard URL remains on configured host")
    void urlIsConfiguredHost() { assertTrue(page.currentUrl().startsWith(FrameworkConfig.dashboardUrl())); }
    @Test @DisplayName("UI-04 transaction search control is available")
    void transactionSearchWorks() { page.searchTransaction("TXN-1001"); assertNotNull(page.resultText()); }
    @Test @DisplayName("UI-05 report navigation opens report view")
    void reportNavigationWorks() { page.openReport(); assertTrue(page.currentUrl().toLowerCase().contains("report")); }
    @Test @DisplayName("UI-06 loading indicator completes")
    void loadingCompletes() { assertTrue(page.isLoadingFinished()); }
}
