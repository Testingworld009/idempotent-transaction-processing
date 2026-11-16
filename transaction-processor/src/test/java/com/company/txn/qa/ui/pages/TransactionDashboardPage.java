package com.company.txn.qa.ui.pages;

import org.openqa.selenium.*;

/**
 * Page Object contract for a future dashboard. Replace data-testid values with the real UI contract.
 * Current assessment source has no HTML dashboard, so UI tests are opt-in and skipped without DASHBOARD_URL.
 */
public class TransactionDashboardPage extends BasePage {
    private final By pageHeading = By.cssSelector("[data-testid='dashboard-title']");
    private final By transactionSearch = By.cssSelector("[data-testid='transaction-search']");
    private final By searchButton = By.cssSelector("[data-testid='search-submit']");
    private final By results = By.cssSelector("[data-testid='transaction-results']");
    private final By reportLink = By.cssSelector("[data-testid='report-link']");
    private final By loading = By.cssSelector("[data-testid='loading']");
    public TransactionDashboardPage(WebDriver driver) { super(driver); }
    public void open(String url) { driver.get(url); }
    public boolean isLoaded() { return waits.visible(pageHeading).isDisplayed(); }
    public void searchTransaction(String id) { type(transactionSearch, id); click(searchButton); }
    public String resultText() { return text(results); }
    public void openReport() { click(reportLink); }
    public boolean isLoadingFinished() { return waits.invisible(loading); }
}
